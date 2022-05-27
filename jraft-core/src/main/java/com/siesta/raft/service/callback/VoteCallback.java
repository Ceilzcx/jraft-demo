package com.siesta.raft.service.callback;

import com.baidu.brpc.client.RpcCallback;
import com.siesta.raft.proto.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步requestVote回调函数
 */
public class VoteCallback implements RpcCallback<RaftProto.VoteRequest> {

    @Override
    public void success(RaftProto.VoteRequest response) {

    }

    @Override
    public void fail(Throwable e) {

    }
}
