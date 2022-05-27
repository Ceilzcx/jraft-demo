package com.siesta.raft.service;


import com.siesta.raft.proto.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftClientService {

    /**
     * 心跳机制 / 日志复制
     * 一致性检查（term 和 log）
     */
    RaftProto.AppendEntriesResponse appendEntries(RaftProto.AppendEntriesRequest request);

    /**
     * 节点预选举
     * 通过预选举的节点才会执行requestVote，防止节点选举一直不成功，重复选举（例：自身原因接收不到来自其他节点的消息）
     */
    RaftProto.VoteResponse preVote(RaftProto.VoteRequest request);

    /**
     * 节点选举
     * candidate -> leader / follower
     */
    RaftProto.VoteResponse requestVote(RaftProto.VoteRequest request);
}
