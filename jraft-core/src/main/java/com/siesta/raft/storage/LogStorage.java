package com.siesta.raft.storage;

import com.siesta.raft.Lifecycle;
import com.siesta.raft.entity.RaftProto;

import java.util.List;

/**
 * @author hujiaofen
 * @since 27/5/2022
 * log存储和获取接口
 */
public interface LogStorage extends Lifecycle<LogStorageOptions> {

    long getLastLogIndex();

    RaftProto.LogEntry getLogEntry(long index);

    boolean appendEntry(RaftProto.LogEntry logEntry);

    int appendEntries(List<RaftProto.LogEntry> logEntries);
}