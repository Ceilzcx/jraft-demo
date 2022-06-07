package com.siesta.raft.cluster;

import lombok.Data;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
@Data
public class NodeOptions {
    // 触发选举的超时时间
    private int electionTimeout = 5000;
}
