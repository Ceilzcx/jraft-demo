package com.siesta.raft.rpc.service.callback;

import com.baidu.brpc.client.RpcCallback;
import com.siesta.raft.rpc.service.RaftHandlerResponseService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
@Slf4j
public abstract class RpcCallbackAdapter<T> implements RpcCallback<T> {
    protected final RaftHandlerResponseService handlerResponseService;

    RpcCallbackAdapter(RaftHandlerResponseService handlerResponseService) {
        this.handlerResponseService = handlerResponseService;
    }

    @Override
    public void fail(Throwable e) {
        log.error(getClass().getDeclaringClass().getName() + "call back failed");
        // handleErrorResponse
    }
}