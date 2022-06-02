package com.siesta.raft.cluster;

import com.siesta.raft.entity.Ballot;
import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.rpc.service.RaftClientService;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;
import com.siesta.raft.rpc.service.RaftServerService;
import com.siesta.raft.rpc.service.callback.PreVoteCallback;
import com.siesta.raft.rpc.service.callback.RequestVoteCallback;
import com.siesta.raft.storage.LogStorage;
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
                clientService.preVote(server, request, new PreVoteCallback(this, this.currentTerm));
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public RaftProto.VoteResponse handlePreVote(RaftProto.VoteRequest request) {
        return null;
    }

    @Override
    public void handlePreVoteResponse(RaftProto.VoteResponse response, long term) {
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
            if (this.ballot.grant(response.getVoteGranted())) {
                log.info("node {} grant pre vote success", response.getVoteGranted());
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
                clientService.requestVote(server, request, new RequestVoteCallback(this, this.currentTerm));
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public RaftProto.VoteResponse handleRequestVote(RaftProto.VoteRequest request) {
        return null;
    }

    @Override
    public void handleRequestVoteResponse(RaftProto.VoteResponse response, long term) {
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
            if (this.ballot.grant(response.getVoteGranted())) {
                log.info("node {} grant request vote success", response.getVoteGranted());
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
