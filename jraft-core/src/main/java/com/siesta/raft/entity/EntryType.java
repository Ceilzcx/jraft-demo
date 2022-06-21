package com.siesta.raft.entity;

/**
 * @author hujiaofen
 * @since 17/6/2022
 */
public enum EntryType {
    ENTRY_DATA(1), ENTRY_CONFIGURATION(2);

    final private int value;

    EntryType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
