package com.siesta.raft.service.callback;

import com.siesta.raft.proto.RaftProto;
import com.siesta.raft.service.RaftServerService;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 异步requestVote回调函数
 */
public class VoteCallback extends RpcCallbackAdapter<RaftProto.VoteResponse> {

    VoteCallback(RaftServerService raftServerService) {
        super(raftServerService);
    }

    @Override
    public void success(RaftProto.VoteResponse response) {
        this.raftServerService.handleRequestVoteResponse(response);
    }
}