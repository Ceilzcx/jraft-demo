package com.siesta.raft.storage;

import com.siesta.raft.entity.RaftProto;
import com.siesta.raft.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author hujiaofen
 * @since 10/6/2022
 */
@Slf4j
public class RocksDBLogStorage implements LogStorage {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private RocksDB rocksDB;
    private ReadOptions readOptions;
    private ColumnFamilyHandle confHandle;
    private ColumnFamilyHandle dataHandle;

    static {
        RocksDB.loadLibrary();
    }

    @Override
    public boolean init(LogStorageOptions options) {
        this.writeLock.lock();
        if (this.rocksDB != null) {
            log.warn("rocksdb already open");
            return true;
        }
        try (final DBOptions dbOptions = getDefaultOptions()) {
            this.readOptions = new ReadOptions();

            // column family：列族, 一个key对应一个列族; 将配置和entry区分放在不同的列族中
            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
            ColumnFamilyDescriptor confDescriptor = new ColumnFamilyDescriptor(ByteUtil.strToBytes("configuration"));
            columnFamilyDescriptors.add(confDescriptor);
            ColumnFamilyDescriptor dataDescriptor = new ColumnFamilyDescriptor(ByteUtil.strToBytes("data"));
            columnFamilyDescriptors.add(dataDescriptor);
            List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

            this.rocksDB = RocksDB.open(dbOptions, options.getPath(), columnFamilyDescriptors, columnFamilyHandles);
            assert columnFamilyHandles.size() == 2;
            this.confHandle = columnFamilyHandles.get(0);
            this.dataHandle = columnFamilyHandles.get(1);

        } catch (RocksDBException e) {
            e.printStackTrace();
        } finally {
            this.writeLock.unlock();
        }
        return false;
    }

    private DBOptions getDefaultOptions() {
        final DBOptions dbOptions = new DBOptions();
        dbOptions.setCreateIfMissing(true);
        dbOptions.setCreateMissingColumnFamilies(true);
        return dbOptions;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public long getLastLogIndex() {
        this.readLock.lock();
        try (final RocksIterator iterator = this.rocksDB.newIterator(this.dataHandle, this.readOptions)) {
            iterator.seekToLast();
            if (iterator.isValid()) {
                return ByteUtil.bytesToLong(iterator.key(), 0);
            }
            return 0L;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public RaftProto.LogEntry getLogEntry(long index) {
        return null;
    }

    @Override
    public boolean appendEntry(RaftProto.LogEntry logEntry) {
        return false;
    }

    @Override
    public int appendEntries(List<RaftProto.LogEntry> logEntries) {
        return 0;
    }
}
