package com.siesta.raft.cluster;

import com.siesta.raft.proto.RaftProto;
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
    private int voteId;

    private long currentTerm;

    public NodeImpl(RaftProto.Configuration configuration, RaftProto.Server server) {
        this.configuration = configuration;
        this.localServer = server;
    }

    @Override
    public boolean init(NodeOptions options) {
        return false;
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

    @Override
    public RaftProto.AppendEntriesResponse handleAppendEntries(RaftProto.AppendEntriesRequest request) {
        return null;
    }

    @Override
    public void handleAppendEntriesResponse(RaftProto.AppendEntriesResponse response) {

    }

    @Override
    public RaftProto.VoteResponse handlePreVote(RaftProto.VoteRequest request) {
        return null;
    }

    @Override
    public void handlePreVoteResponse(RaftProto.VoteResponse response) {

    }

    @Override
    public RaftProto.VoteResponse handleRequestVote(RaftProto.VoteRequest request) {
        return null;
    }

    @Override
    public void handleRequestVoteResponse(RaftProto.VoteResponse response) {

    }
}
