package com.siesta.raft.service.callback;

import com.baidu.brpc.client.RpcCallback;
import com.siesta.raft.service.RaftServerService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
@Slf4j
public abstract class RpcCallbackAdapter<T> implements RpcCallback<T> {
    protected final RaftServerService raftServerService;

    RpcCallbackAdapter(RaftServerService raftServerService) {
        this.raftServerService = raftServerService;
    }

    @Override
    public void fail(Throwable e) {
        log.error(getClass().getDeclaringClass().getName() + "call back failed");
        // handleErrorResponse
    }
}