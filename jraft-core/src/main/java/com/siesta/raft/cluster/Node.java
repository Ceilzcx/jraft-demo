package com.siesta.raft.cluster;

import com.siesta.raft.Lifecycle;
import com.siesta.raft.entity.RaftProto;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * 集群的节点接口
 */
public interface Node extends Lifecycle<NodeOptions> {

    /**
     * 获取当前节点id
     * @return 节点信息
     */
    RaftProto.Server getNodeId();

    /**
     * 获取Leader节点id，集群没有Leader返回null
     * @return 节点信息
     */
    RaftProto.Server getLeaderId();

    /**
     * 判断当前节点是否为leader节点
     * 重定向使用
     */
    boolean isLeader();

    /**
     * 获取集群所有server信息，启动时配置，包括连接丢失的节点
     */
    RaftProto.Configuration getConfiguration();
}