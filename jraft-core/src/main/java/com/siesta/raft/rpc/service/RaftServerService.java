package com.siesta.raft.rpc.service;

import com.siesta.raft.entity.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftServerService {

    /**
     * 节点预选举
     * 通过预选举的节点才会执行requestVote，防止节点选举一直不成功，重复选举（例：自身原因接收不到来自其他节点的消息）
     */
    RaftProto.AppendEntriesResponse handleAppendEntries(RaftProto.AppendEntriesRequest request);

    /**
     * 心跳机制 / 日志复制
     * 一致性检查（term 和 log）
     */
    RaftProto.VoteResponse handlePreVote(RaftProto.VoteRequest request);

    /**
     * 节点选举
     * candidate -> leader / follower
     */
    RaftProto.VoteResponse handleRequestVote(RaftProto.VoteRequest request);
}
