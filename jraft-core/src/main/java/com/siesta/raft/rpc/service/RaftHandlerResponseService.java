package com.siesta.raft.rpc.service;

import com.siesta.raft.entity.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftHandlerResponseService {

    void handleAppendEntriesResponse(RaftProto.AppendEntriesResponse response);

    void handlePreVoteResponse(RaftProto.VoteResponse response, RaftProto.Server server, long term);

    void handleRequestVoteResponse(RaftProto.VoteResponse response, RaftProto.Server server, long term);
}
