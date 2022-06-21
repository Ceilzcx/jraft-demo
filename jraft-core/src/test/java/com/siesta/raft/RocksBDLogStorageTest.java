package com.siesta.raft;

import com.google.protobuf.ByteString;
import com.siesta.raft.entity.EntryType;
import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.storage.LogStorage;
import com.siesta.raft.storage.LogStorageOptions;
import com.siesta.raft.storage.RocksDBLogStorage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author hujiaofen
 * @since 21/6/2022
 */
public class RocksBDLogStorageTest {
    private LogStorage logStorage;

    @Before
    public void init() {
        this.logStorage = new RocksDBLogStorage();
        LogStorageOptions options = new LogStorageOptions();
        options.setPath("C:\\Ceilzcx\\project\\data\\raft-demo");
        this.logStorage.init(options);
    }

    @After
    public void destroy() {
        this.logStorage.shutdown();
    }

    @Test
    public void testAppendEntry() {
        RaftProto.LogEntry logEntry = RaftProto.LogEntry.newBuilder()
                .setIndex(1)
                .setTerm(1)
                .setData(ByteString.copyFromUtf8("test"))
                .setType(EntryType.ENTRY_DATA.getValue())
                .build();
        Assert.assertTrue(this.logStorage.appendEntry(logEntry));
    }

    @Test
    public void testRead() {
        RaftProto.LogEntry logEntry = this.logStorage.getLogEntry(1);
        Assert.assertEquals("test", logEntry.getData().toStringUtf8());
    }

}
