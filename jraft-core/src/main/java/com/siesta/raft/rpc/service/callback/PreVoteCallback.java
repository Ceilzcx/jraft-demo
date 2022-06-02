package com.siesta.raft.rpc.service.callback;

import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步preVote回调函数
 */
public class PreVoteCallback extends RpcCallbackAdapter<RaftProto.VoteResponse> {
    private final long term;

    public PreVoteCallback(RaftHandlerResponseService handlerResponseService, long term) {
        super(handlerResponseService);
        this.term = term;
    }

    @Override
    public void success(RaftProto.VoteResponse response) {
        this.handlerResponseService.handlePreVoteResponse(response, term);
    }
}
