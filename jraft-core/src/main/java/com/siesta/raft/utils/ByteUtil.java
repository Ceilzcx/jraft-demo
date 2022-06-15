package com.siesta.raft.utils;

import java.nio.charset.StandardCharsets;

/**
 * @author hujiaofen
 * @since 15/6/2022
 */
public class ByteUtil {

    private ByteUtil() {}

    public static byte[] strToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static void longToBytes(byte[] bytes, int start, long value) {
        bytes[start] = (byte) (value >>> 56);
        bytes[start + 1] = (byte) (value >>> 48);
        bytes[start + 2] = (byte) (value >>> 40);
        bytes[start + 3] = (byte) (value >>> 32);
        bytes[start + 4] = (byte) (value >>> 24);
        bytes[start + 5] = (byte) (value >>> 16);
        bytes[start + 6] = (byte) (value >>> 8);
        bytes[start + 7] = (byte) value;
    }

    public static long bytesToLong(byte[] bytes, int start) {
        return ((long) bytes[start] & 0xff) << 56 | ((long) bytes[start + 1] & 0xff) << 48
                | ((long) bytes[start + 2] & 0xff) << 40 | ((long) bytes[start + 3] & 0xff) << 32
                | ((long) bytes[start + 4] & 0xff) << 24 | ((long) bytes[start + 5] & 0xff) << 16
                | ((long) bytes[start + 6] & 0xff) << 8 | (long) bytes[start + 7] & 0xff;
    }

}
