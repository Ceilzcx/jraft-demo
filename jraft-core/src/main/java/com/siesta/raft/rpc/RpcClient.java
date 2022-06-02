package com.siesta.raft.rpc;

import com.siesta.raft.entity.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface RpcClient {

    boolean checkConnection(final RaftProto.Server server);

    boolean connect(final RaftProto.Server server);

    boolean disconnect(final RaftProto.Server server);
}
