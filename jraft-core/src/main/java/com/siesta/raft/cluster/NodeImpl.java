package com.siesta.raft.cluster;

import com.siesta.raft.entity.Ballot;
import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.rpc.service.RaftClientService;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;
import com.siesta.raft.rpc.service.RaftServerService;
import com.siesta.raft.rpc.service.callback.PreVoteCallback;
import com.siesta.raft.rpc.service.callback.RequestVoteCallback;
import com.siesta.raft.storage.LogStorage;
import com.siesta.raft.utils.ConfigurationUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
@Slf4j
public class NodeImpl implements Node, RaftServerService, RaftHandlerResponseService {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = this.lock.readLock();
    private final Lock writeLock = this.lock.writeLock();

    private final RaftProto.Server localServer;
    private RaftProto.Configuration configuration;
    private NodeType nodeType;
    private RaftProto.Server leaderServer;
    private RaftProto.Server voteServer;
    private NodeOptions nodeOptions;

    private long currentTerm;

    private Ballot ballot;
    private LogStorage logStorage;
    private RaftClientService clientService;

    public NodeImpl(RaftProto.Configuration configuration, RaftProto.Server server) {
        this.configuration = configuration;
        this.localServer = server;
    }

    @Override
    public boolean init(NodeOptions options) {
        this.nodeType = NodeType.FOLLOWER;
        this.leaderServer = null;
        this.voteServer = null;
        this.ballot = new Ballot();
        this.nodeOptions = options;
        return true;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public RaftProto.Server getNodeId() {
        return localServer;
    }

    @Override
    public RaftProto.Server getLeaderId() {
        this.readLock.lock();
        try {
            return leaderServer;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public boolean isLeader() {
        this.readLock.lock();
        try {
            return this.nodeType == NodeType.LEADER;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public RaftProto.Configuration getConfiguration() {
        return configuration;
    }

    public void appendEntries() {

    }

    @Override
    public RaftProto.AppendEntriesResponse handleAppendEntries(RaftProto.AppendEntriesRequest request) {
        return null;
    }

    @Override
    public void handleAppendEntriesResponse(RaftProto.AppendEntriesResponse response) {

    }

    public void preVote() {
        long oldTerm;

        this.writeLock.lock();
        try {
            if (this.nodeType != NodeType.FOLLOWER) {
                return;
            }
            log.info("Node {} start pre vote, currentTerm: {}", this.localServer, this.currentTerm);
            // todo 判断是否在 snapshot

            oldTerm = this.currentTerm;
        } finally {
            this.writeLock.unlock();
        }


        long lastLogIndex = logStorage.getLastLogIndex();
        long lastLogTerm = logStorage.getLogEntry(lastLogIndex).getTerm();

        this.writeLock.lock();
        try {
            if (oldTerm != currentTerm) {
                return;
            }
            this.nodeType = NodeType.PRE_CANDIDATE;
            this.ballot.init(this.configuration);
            for (RaftProto.Server server : this.configuration.getServersList()) {
                if (server.equals(this.localServer)) {
                    continue;
                }
                if (!clientService.checkConnection(server)) {
                    log.warn("Node {} connect to Node {} failed", this.localServer, server);
                }
                RaftProto.VoteRequest request = RaftProto.VoteRequest.newBuilder()
                        .setTerm(this.currentTerm + 1)
                        .setServerId(localServer)
                        .setLastLogIndex(lastLogIndex)
                        .setLastLogTerm(lastLogTerm)
                        .build();
                clientService.preVote(server, request, new PreVoteCallback(this, server, this.currentTerm));
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public RaftProto.VoteResponse handlePreVote(RaftProto.VoteRequest request) {
        this.writeLock.lock();
        try {
            RaftProto.Server granted = null;
            do {
                if (!ConfigurationUtils.containsNode(this.configuration, request.getServerId())) {
                    log.warn("pre vote request invalid, configuration not contains node {}", request.getServerId());
                    break;
                }
                if (this.nodeType != NodeType.FOLLOWER) {
                    log.warn("pre vote request invalid, local node {} type is not follower", this.localServer);
                    break;
                }
                // todo 判断当前节点的leader是否可用，可用可以不进行选举

                if (request.getTerm() < this.currentTerm) {
                    log.warn("pre vote request invalid, request term = {}, current term = {}", request.getTerm(), this.currentTerm);
                    break;
                }
                long lastLogIndex = this.logStorage.getLastLogIndex();
                long lastLogTerm = this.logStorage.getLogEntry(lastLogIndex).getTerm();
                // todo log is not consistency, is ok?
                if (request.getLastLogIndex() < lastLogIndex || (request.getLastLogIndex() == lastLogIndex && request.getLastLogTerm() != lastLogTerm)) {
                    log.warn("pre vote request invalid, request lastLogIndex: {}, lastLogTerm: {}, local lastLogIndex: {}, lastLogTerm: {}",
                            request.getLastLogIndex(), request.getLastLogTerm(), lastLogIndex, lastLogTerm);
                    break;
                }
                granted = request.getServerId();
            } while (false);
            return RaftProto.VoteResponse.newBuilder()
                    .setTerm(this.currentTerm)
                    .setVoteGranted(granted)
                    .build();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void handlePreVoteResponse(RaftProto.VoteResponse response, RaftProto.Server server, long term) {
        this.writeLock.lock();
        try {
            if (this.nodeType != NodeType.PRE_CANDIDATE) {
                log.warn("pre vote response invalid, local Node {} type is not follower", this.localServer);
                return;
            }
            // 不是本次term发送的request收到的请求
            if (term != this.currentTerm) {
                log.warn("pre vote response invalid, local Node {} current term = {} term = {} ",
                        this.localServer, this.currentTerm, term);
                return;
            }
            if (response.getTerm() > this.currentTerm) {
                log.warn("pre vote response invalid, local Node {} current term = {} response term = {}",
                        this.localServer, this.currentTerm, response.getTerm());
                stepDown(response.getTerm());
                return;
            }
            if (this.localServer.equals(response.getVoteGranted()) && this.ballot.grant(server)) {
                log.info("node {} grant pre vote success", server);
                if (this.ballot.isGranted()) {
                    requestVote();
                }
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    public void requestVote() {
        long oldTerm;
        this.writeLock.lock();
        try {
            if (this.nodeType != NodeType.PRE_CANDIDATE) {
                return;
            }
            this.nodeType = NodeType.CANDIDATE;
            this.currentTerm++;
            this.voteServer = RaftProto.Server.newBuilder()
                    .setServerId(this.localServer.getServerId())
                    .setAddress(this.localServer.getAddress())
                    .setPort(this.localServer.getPort())
                    .build();
            this.leaderServer = null;
            oldTerm = this.currentTerm;
        } finally {
            this.writeLock.unlock();
        }

        long lastLogIndex = logStorage.getLastLogIndex();
        long lastLogTerm = logStorage.getLogEntry(lastLogIndex).getTerm();

        this.writeLock.lock();
        try {
            if (oldTerm != this.currentTerm) {
                return;
            }
            this.ballot.init(configuration);
            for (RaftProto.Server server : configuration.getServersList()) {
                if (server.equals(this.localServer)) {
                    continue;
                }
                if (!clientService.checkConnection(server)) {
                    log.warn("Node {} connect to Node {} failed", this.localServer, server);
                }
                RaftProto.VoteRequest request = RaftProto.VoteRequest.newBuilder()
                        .setTerm(this.currentTerm)
                        .setServerId(localServer)
                        .setLastLogIndex(lastLogIndex)
                        .setLastLogTerm(lastLogTerm)
                        .build();
                clientService.requestVote(server, request, new RequestVoteCallback(this, server, this.currentTerm));
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public RaftProto.VoteResponse handleRequestVote(RaftProto.VoteRequest request) {
        this.writeLock.lock();
        try {
            RaftProto.Server granted = null;
            do {
                if (!ConfigurationUtils.containsNode(this.configuration, request.getServerId())) {
                    log.warn("request vote request invalid, configuration not contains node {}", request.getServerId());
                    break;
                }
                if (this.nodeType != NodeType.PRE_CANDIDATE) {
                    log.warn("request vote request invalid, local node {} type is not follower", this.localServer);
                    break;
                }
                // todo 判断当前节点的leader是否可用，可用可以不进行选举

                if (request.getTerm() < this.currentTerm) {
                    log.warn("request vote request invalid, request term = {}, current term = {}", request.getTerm(), this.currentTerm);
                    break;
                }
                long lastLogIndex = this.logStorage.getLastLogIndex();
                long lastLogTerm = this.logStorage.getLogEntry(lastLogIndex).getTerm();
                // todo log is not consistency, is ok?
                if (request.getLastLogIndex() < lastLogIndex || (request.getLastLogIndex() == lastLogIndex && request.getLastLogTerm() != lastLogTerm)) {
                    log.warn("request vote request invalid, request lastLogIndex: {}, lastLogTerm: {}, local lastLogIndex: {}, lastLogTerm: {}",
                            request.getLastLogIndex(), request.getLastLogTerm(), lastLogIndex, lastLogTerm);
                    break;
                }
                if (this.voteServer != null) {
                    break;
                }
                granted = request.getServerId();
                this.voteServer = request.getServerId();
            } while (false);
            return RaftProto.VoteResponse.newBuilder()
                    .setTerm(this.currentTerm)
                    .setVoteGranted(granted)
                    .build();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void handleRequestVoteResponse(RaftProto.VoteResponse response, RaftProto.Server server, long term) {
        this.writeLock.lock();
        try {
            if (this.nodeType != NodeType.CANDIDATE) {
                log.warn("request vote response invalid, local Node {} type is not follower", this.localServer);
                return;
            }
            // 不是本次term发送的request收到的请求
            if (term != this.currentTerm) {
                log.warn("request vote response invalid, local Node {} current term = {} term = {} ",
                        this.localServer, this.currentTerm, term);
                return;
            }
            if (response.getTerm() > this.currentTerm) {
                log.warn("request vote response invalid, local Node {} current term = {} response term = {}",
                        this.localServer, this.currentTerm, response.getTerm());
                stepDown(response.getTerm());
                return;
            }
            if (this.localServer.equals(response.getVoteGranted()) && this.ballot.grant(server)) {
                log.info("node {} grant request vote success", server);
                if (this.ballot.isGranted()) {
                    becomeLeader();
                }
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private void stepDown(long term) {

    }

    private void becomeLeader() {
        this.nodeType = NodeType.LEADER;
    }
}
