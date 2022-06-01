package com.siesta.raft.rpc.service;

import com.siesta.raft.proto.RaftProto;
import com.siesta.raft.rpc.service.callback.PreVoteCallback;
import com.siesta.raft.rpc.service.callback.RequestVoteCallback;

import java.util.concurrent.Future;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftServerServiceAsync extends RaftServerService {

    Future<RaftProto.VoteResponse> handlePreVote(RaftProto.VoteRequest request, PreVoteCallback callback);

    Future<RaftProto.VoteResponse> handleRequestVote(RaftProto.VoteRequest request, RequestVoteCallback callback);
}