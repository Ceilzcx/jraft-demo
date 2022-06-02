package com.siesta.raft.rpc.service.callback;

import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步preVote回调函数
 */
public class PreVoteCallback extends RpcCallbackAdapter<RaftProto.VoteResponse> {
    private final RaftProto.Server server;
    private final long term;

    public PreVoteCallback(RaftHandlerResponseService handlerResponseService, RaftProto.Server server, long term) {
        super(handlerResponseService);
        this.server = server;
        this.term = term;
    }

    @Override
    public void success(RaftProto.VoteResponse response) {
        this.handlerResponseService.handlePreVoteResponse(response, server, term);
    }
}
