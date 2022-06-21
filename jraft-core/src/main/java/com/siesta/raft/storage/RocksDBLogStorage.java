package com.siesta.raft.storage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.siesta.raft.entity.EntryType;
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

    private long firstIndex = -1;

    private RocksDB rocksDB;
    private ReadOptions readOptions;
    private WriteOptions writeOptions;
    private ColumnFamilyHandle confHandle;
    private ColumnFamilyHandle dataHandle;

    static {
        RocksDB.loadLibrary();
    }

    private interface WriteBatchTemplate {
        void execute(WriteBatch writeBatch) throws RocksDBException;
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

            this.writeOptions = new WriteOptions();
            this.writeOptions.setSync(true);

            // column family：列族, 一个key对应一个列族; 将配置和entry区分放在不同的列族中
            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
            // configuration 存储的是 com.siesta.raft.entity.RaftProto.Configuration
            ColumnFamilyDescriptor confDescriptor = new ColumnFamilyDescriptor(ByteUtil.strToBytes("configuration"));
            columnFamilyDescriptors.add(confDescriptor);
            // Default column family must specified
            ColumnFamilyDescriptor dataDescriptor = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY);
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
        if (this.dataHandle != null) {
            this.dataHandle.close();
        }
        if (this.confHandle != null) {
            this.confHandle.close();
        }
        if (this.rocksDB != null) {
            this.rocksDB.close();
        }
    }

    @Override
    public long getFirstLogIndex() {
        if (this.firstIndex == -1) {
            try (RocksIterator it = this.rocksDB.newIterator(this.readOptions)) {
                it.seekToFirst();
                if (it.isValid()) {
                    this.firstIndex = ByteUtil.bytesToLong(it.key(), 0);
                }
            }
        }
        return this.firstIndex;
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
        this.readLock.lock();
        try {
            if (getFirstLogIndex() > index) {
                return null;
            }
            byte[] value = this.rocksDB.get(getKeyBytes(index));
            return RaftProto.LogEntry.parseFrom(value);
        } catch (RocksDBException | InvalidProtocolBufferException e) {
            e.printStackTrace();
        } finally {
            this.readLock.unlock();
        }
        return null;
    }

    @Override
    public boolean appendEntry(RaftProto.LogEntry logEntry) {
        if (logEntry.getType() == EntryType.ENTRY_DATA.getValue()) {
            return executeBatch(writeBatch -> addConfBatch(logEntry, writeBatch));
        } else if (logEntry.getType() == EntryType.ENTRY_CONFIGURATION.getValue()) {
            // todo other operators
            return executeBatch(writeBatch -> addDataBatch(logEntry, writeBatch));
        }
        return false;
    }

    @Override
    public int appendEntries(List<RaftProto.LogEntry> logEntries) {
        return 0;
    }

    private boolean executeBatch(final WriteBatchTemplate template) {
        this.readLock.lock();
        try (final WriteBatch writeBatch = new WriteBatch()) {
            template.execute(writeBatch);
            this.rocksDB.write(this.writeOptions, writeBatch);
            return true;
        } catch (RocksDBException e) {
            log.error("rocksdb write fail", e);
        } finally {
            this.readLock.unlock();
        }
        return false;
    }

    private void addConfBatch(final RaftProto.LogEntry logEntry, final WriteBatch writeBatch) throws RocksDBException {
        byte[] key = getKeyBytes(logEntry.getIndex());
        byte[] value = logEntry.toByteArray();
        writeBatch.put(this.confHandle, key, value);
        // todo 为什么要添加到 data column family?
        writeBatch.put(this.dataHandle, key, value);
    }

    private void addDataBatch(final RaftProto.LogEntry logEntry, final WriteBatch writeBatch) throws RocksDBException {
        byte[] key = getKeyBytes(logEntry.getIndex());
        byte[] value = logEntry.toByteArray();
        writeBatch.put(this.dataHandle, key, value);
    }

    private byte[] getKeyBytes(long l) {
        byte[] bytes = new byte[Long.BYTES];
        ByteUtil.longToBytes(bytes, 0, l);
        return bytes;
    }
}
