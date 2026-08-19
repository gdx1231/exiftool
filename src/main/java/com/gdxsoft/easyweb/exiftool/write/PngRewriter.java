package com.gdxsoft.easyweb.exiftool.write;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.read.Binary;

/**
 * PNG rewriter: replaces or creates the eXIf chunk for EXIF tag updates.
 * The chunk payload is a TIFF structure (possibly with an "Exif\0\0" header);
 * the CRC32 is recomputed over the chunk type + data.
 */
public final class PngRewriter {

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};

    private PngRewriter() {}

    public static byte[] write(byte[] data, Map<String, Object> updates) {
        int exifPos = findChunk(data, "eXIf");
        byte[] tiff = buildTiff(data, exifPos, updates);
        if (exifPos >= 0) {
            return replaceChunk(data, exifPos, tiff);
        }
        return insertChunk(data, tiff);
    }

    private static byte[] buildTiff(byte[] data, int exifPos, Map<String, Object> updates) {
        if (exifPos >= 0) {
            int d = exifPos + 8;
            int len = Binary.get32u(data, exifPos, ByteOrder.BIG_ENDIAN);
            byte[] payload = Arrays.copyOfRange(data, d, d + len);
            int tiffBase = 0;
            if (len >= 6 && payload[0] == 'E' && payload[1] == 'x' && payload[2] == 'i'
                && payload[3] == 'f' && payload[4] == 0 && payload[5] == 0) {
                tiffBase = 6;
            }
            byte[] tiff = Arrays.copyOfRange(payload, tiffBase, payload.length);
            return TiffRewriter.rewrite(tiff, orderOf(tiff), updates);
        }
        byte[] empty = {'M', 'M', 0, 42, 0, 0, 0, 8};
        return TiffRewriter.rewrite(empty, ByteOrder.BIG_ENDIAN, updates);
    }

    private static ByteOrder orderOf(byte[] tiff) {
        return tiff.length >= 2 && tiff[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    private static int findChunk(byte[] data, String id) {
        int pos = PNG_SIG.length;
        while (pos + 8 <= data.length) {
            int len = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
            String type = new String(data, pos + 4, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (pos + 12 + len > data.length) {
                break;
            }
            if (id.equals(type)) {
                return pos;
            }
            pos += 12 + len;
        }
        return -1;
    }

    private static byte[] replaceChunk(byte[] data, int chunkPos, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, chunkPos);
        writeChunk(out, "eXIf", payload);
        int oldLen = Binary.get32u(data, chunkPos, ByteOrder.BIG_ENDIAN);
        int oldEnd = chunkPos + 12 + oldLen;
        out.write(data, oldEnd, data.length - oldEnd);
        return out.toByteArray();
    }

    /** Insert the eXIf chunk right after IHDR. */
    private static byte[] insertChunk(byte[] data, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, PNG_SIG.length);
        int pos = PNG_SIG.length;
        int ihdrEnd = pos + 12 + Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
        out.write(data, PNG_SIG.length, ihdrEnd - PNG_SIG.length); // IHDR chunk
        writeChunk(out, "eXIf", payload);
        out.write(data, ihdrEnd, data.length - ihdrEnd);
        return out.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String id, byte[] payload) {
        long len = payload.length;
        out.write((byte) (len >> 24));
        out.write((byte) (len >> 16));
        out.write((byte) (len >> 8));
        out.write((byte) len);
        for (byte b : id.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) {
            out.write(b);
        }
        out.write(payload, 0, payload.length);
        // CRC over type + data
        CRC32 crc = new CRC32();
        crc.update(id.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        crc.update(payload, 0, payload.length);
        long c = crc.getValue();
        out.write((byte) (c >> 24));
        out.write((byte) (c >> 16));
        out.write((byte) (c >> 8));
        out.write((byte) c);
    }
}
