package com.siesta.raft.rpc.service;


import com.siesta.raft.proto.RaftProto;
import com.siesta.raft.rpc.service.callback.PreVoteCallback;
import com.siesta.raft.rpc.service.callback.VoteCallback;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftClientService {

    RaftProto.AppendEntriesResponse appendEntries(RaftProto.Server server, RaftProto.AppendEntriesRequest request);

    RaftProto.VoteResponse preVote(RaftProto.Server server, RaftProto.VoteRequest request, PreVoteCallback callback);

    RaftProto.VoteResponse requestVote(RaftProto.Server server, RaftProto.VoteRequest request, VoteCallback callback);
}
