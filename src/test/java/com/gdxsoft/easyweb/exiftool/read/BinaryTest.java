package com.gdxsoft.easyweb.exiftool.read;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import org.junit.jupiter.api.Test;

class BinaryTest {

    private static final byte[] DATA = {
        0x12, 0x34, 0x56, 0x78,
        (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0
    };

    @Test
    void get16uLittleEndian() {
        assertEquals(0x3412, Binary.get16u(DATA, 0, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void get16uBigEndian() {
        assertEquals(0x1234, Binary.get16u(DATA, 0, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void get32uLittleEndian() {
        assertEquals(0x78563412L, Binary.get32u(DATA, 0, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL);
    }

    @Test
    void get32uBigEndian() {
        assertEquals(0x12345678L, Binary.get32u(DATA, 0, ByteOrder.BIG_ENDIAN) & 0xffffffffL);
    }

    @Test
    void get64uLittleEndian() {
        assertEquals(0xf0debc9a78563412L, Binary.get64u(DATA, 0, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void get32sSigned() {
        assertEquals(-1698898192, Binary.get32s(DATA, 4, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void getFloat() {
        byte[] f = {0x3f, (byte) 0x80, 0x00, 0x00}; // 1.0f big-endian
        assertEquals(1.0f, Binary.getFloat(f, 0, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void getDouble() {
        byte[] d = {0x3f, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; // 1.0 big-endian
        assertEquals(1.0, Binary.getDouble(d, 0, ByteOrder.BIG_ENDIAN));
    }
}
