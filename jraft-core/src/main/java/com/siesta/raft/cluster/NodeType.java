package com.siesta.raft.cluster;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public enum NodeType {
    LEADER,
    PRE_CANDIDATE,
    CANDIDATE,
    FOLLOWER
}
