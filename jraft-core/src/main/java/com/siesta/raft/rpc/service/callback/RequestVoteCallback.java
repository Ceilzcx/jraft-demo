package com.siesta.raft.rpc.service.callback;

import com.siesta.raft.proto.RaftProto;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步requestVote回调函数
 */
public class RequestVoteCallback extends RpcCallbackAdapter<RaftProto.VoteResponse> {

    public RequestVoteCallback(RaftHandlerResponseService handlerResponseService) {
        super(handlerResponseService);
    }

    @Override
    public void success(RaftProto.VoteResponse response) {
        this.handlerResponseService.handleRequestVoteResponse(response);
    }
}