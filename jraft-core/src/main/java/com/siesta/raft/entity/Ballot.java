package com.siesta.raft.entity;

import java.util.List;

/**
 * @author hujiaofen
 * @since 2/6/2022
 * vote 使用，主要用于判断是否超过半数的节点 能够选举/选举给某节点
 * todo 为什么需要sofa-jraft需要oldConf？
 */
public class Ballot {

    public static final class NotFoundServer {
        RaftProto.Server server;
        boolean isFound;

        NotFoundServer(RaftProto.Server server, boolean isFound) {
            this.server = server;
            this.isFound = isFound;
        }

    }

    private List<NotFoundServer> serverList;
    private int voteCount;

    public void init(final RaftProto.Configuration configuration) {
        this.serverList.clear();
        this.voteCount  = 0;
        for (RaftProto.Server server : configuration.getServersList()) {
            this.serverList.add(new NotFoundServer(server, false));
        }
        this.voteCount = this.serverList.size() / 2 + 1;
    }

    public void grant(RaftProto.Server server) {
        for (NotFoundServer notFoundServer : serverList) {
            if (notFoundServer.server.equals(server) && !notFoundServer.isFound) {
                this.voteCount--;
                notFoundServer.isFound = true;
            }
        }
    }

    public boolean isGranted() {
        return this.voteCount <= 0;
    }

}
