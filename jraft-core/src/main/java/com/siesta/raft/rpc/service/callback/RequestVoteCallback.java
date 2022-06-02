package com.siesta.raft.rpc.service.callback;

import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步requestVote回调函数
 */
public class RequestVoteCallback extends RpcCallbackAdapter<RaftProto.VoteResponse> {
    private final long term;

    public RequestVoteCallback(RaftHandlerResponseService handlerResponseService, long term) {
        super(handlerResponseService);
        this.term = term;
    }

    @Override
    public void success(RaftProto.VoteResponse response) {
        this.handlerResponseService.handleRequestVoteResponse(response, term);
    }
}