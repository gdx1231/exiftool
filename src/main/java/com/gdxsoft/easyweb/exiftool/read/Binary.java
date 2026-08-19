package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ByteOrder;

/**
 * Binary data access helpers, the Java analogue of ExifTool's Get16u/Get32u/Get64u/...
 * All reads take an explicit {@link ByteOrder}; no global byte order state is used.
 */
public final class Binary {

    private Binary() {}

    public static int get16u(byte[] data, int offset, ByteOrder order) {
        int b0 = data[offset] & 0xff;
        int b1 = data[offset + 1] & 0xff;
        return order == ByteOrder.LITTLE_ENDIAN ? b0 | (b1 << 8) : (b0 << 8) | b1;
    }

    public static int get32u(byte[] data, int offset, ByteOrder order) {
        int b0 = data[offset] & 0xff;
        int b1 = data[offset + 1] & 0xff;
        int b2 = data[offset + 2] & 0xff;
        int b3 = data[offset + 3] & 0xff;
        if (order == ByteOrder.LITTLE_ENDIAN) {
            return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    public static long get64u(byte[] data, int offset, ByteOrder order) {
        long a = get32u(data, offset, order) & 0xffffffffL;
        long b = get32u(data, offset + 4, order) & 0xffffffffL;
        if (order == ByteOrder.LITTLE_ENDIAN) {
            return a | (b << 32);
        }
        return (a << 32) | b;
    }

    public static short get16s(byte[] data, int offset, ByteOrder order) {
        return (short) get16u(data, offset, order);
    }

    public static int get32s(byte[] data, int offset, ByteOrder order) {
        return get32u(data, offset, order);
    }

    public static long get64s(byte[] data, int offset, ByteOrder order) {
        return get64u(data, offset, order);
    }

    public static float getFloat(byte[] data, int offset, ByteOrder order) {
        int bits = get32u(data, offset, order);
        return Float.intBitsToFloat(bits);
    }

    public static double getDouble(byte[] data, int offset, ByteOrder order) {
        long bits = get64u(data, offset, order);
        return Double.longBitsToDouble(bits);
    }
}
