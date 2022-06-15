package com.siesta.raft.utils;

import com.siesta.raft.entity.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public class ConfigurationUtil {

    private ConfigurationUtil() {}

    public static RaftProto.Server parseServer(String str) {
        return null;
    }

    public static RaftProto.Configuration parseConfiguration(String str) {
        return null;
    }

    public static boolean containsNode(RaftProto.Configuration configuration, RaftProto.Server server) {
        return configuration.getServersList().contains(server);
    }
}