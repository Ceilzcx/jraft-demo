package com.siesta.raft.rpc.service.impl;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.Endpoint;
import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.extension.SPI;
import com.siesta.raft.rpc.service.RaftClientService;
import com.siesta.raft.rpc.service.RaftServerService;
import com.siesta.raft.rpc.service.callback.PreVoteCallback;
import com.siesta.raft.rpc.service.callback.RequestVoteCallback;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hujiaofen
 * @since 22/6/2022
 */
@Slf4j
@SPI("brpc")
// todo methods can merge to a common method
public class BrpcClientServiceImpl implements RaftClientService {
    private static final Map<RaftProto.Server, RpcClient> clientMap = new ConcurrentHashMap<>(8);

    @Override
    public boolean checkConnection(RaftProto.Server server) {
        if (clientMap.containsKey(server)) {
            // todo brpc how to judge a client connect to server?
            return true;
        } else {
            return connect(server);
        }
    }

    @Override
    public boolean connect(RaftProto.Server server) {
        clientMap.computeIfAbsent(server, key -> new RpcClient(new Endpoint(server.getAddress(), server.getPort())));
        return checkConnection(server);
    }

    @Override
    public boolean disconnect(RaftProto.Server server) {
        RpcClient client = clientMap.get(server);
        if (client == null) return false;
        clientMap.remove(server);
        client.shutdown();
        return true;
    }

    @Override
    public RaftProto.AppendEntriesResponse appendEntries(RaftProto.Server server, RaftProto.AppendEntriesRequest request) {
        try {
            RpcClient client = getClient(server);
            RaftServerService raftServerService = BrpcProxy.getProxy(client, RaftServerService.class);
            return raftServerService.handleAppendEntries(request);
        } catch (ConnectException e) {
            log.error("node {} connect failed", server);
        }
        return null;
    }

    @Override
    public RaftProto.VoteResponse preVote(RaftProto.Server server, RaftProto.VoteRequest request, PreVoteCallback callback) {
        try {
            RpcClient client = getClient(server);
            RaftServerService raftServerService = BrpcProxy.getProxy(client, RaftServerService.class);
            RaftProto.VoteResponse voteResponse = raftServerService.handlePreVote(request);
            callback.success(voteResponse);
            return voteResponse;
        } catch (ConnectException e) {
            log.error("node {} connect failed", server);
            callback.fail(e);
        }
        return null;
    }

    @Override
    public RaftProto.VoteResponse requestVote(RaftProto.Server server, RaftProto.VoteRequest request, RequestVoteCallback callback) {
        try {
            RpcClient client = getClient(server);
            RaftServerService raftServerService = BrpcProxy.getProxy(client, RaftServerService.class);
            RaftProto.VoteResponse voteResponse = raftServerService.handleRequestVote(request);
            callback.success(voteResponse);
            return voteResponse;
        } catch (ConnectException e) {
            log.error("node {} connect failed", server);
            callback.fail(e);
        }
        return null;
    }

    private RpcClient getClient(RaftProto.Server server) throws ConnectException {
        RpcClient client = clientMap.get(server);
        if (client == null) {
            boolean connect = connect(server);
            if (!connect) {
                throw new ConnectException();
            }
            client = clientMap.get(server);
        }
        return client;
    }
}
