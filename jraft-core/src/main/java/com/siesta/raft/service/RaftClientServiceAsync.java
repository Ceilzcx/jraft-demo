package com.siesta.raft.service;

import com.siesta.raft.proto.RaftProto;
import com.siesta.raft.service.callback.PreVoteCallback;
import com.siesta.raft.service.callback.VoteCallback;

import java.util.concurrent.Future;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RaftClientServiceAsync extends RaftClientService {

    Future<RaftProto.VoteResponse> preVote(RaftProto.VoteRequest request, PreVoteCallback callback);

    Future<RaftProto.VoteResponse> requestVote(RaftProto.VoteRequest request, VoteCallback callback);
}