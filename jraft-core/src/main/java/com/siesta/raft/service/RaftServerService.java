package com.siesta.raft.service;

import com.siesta.raft.proto.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftServerService {

    RaftProto.AppendEntriesResponse handleAppendEntries(RaftProto.AppendEntriesRequest request);

    void handleAppendEntriesResponse(RaftProto.AppendEntriesResponse response);

    RaftProto.VoteResponse handlePreVote(RaftProto.VoteRequest request);

    void handlePreVoteResponse(RaftProto.VoteResponse response);

    RaftProto.VoteResponse handleRequestVote(RaftProto.VoteRequest request);

    void handleRequestVoteResponse(RaftProto.VoteResponse response);
}
