package com.gdxsoft.easyweb.exiftool.write;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.read.Binary;

/**
 * WebP (RIFF) rewriter: replaces or creates the EXIF chunk for EXIF tag
 * updates. The chunk payload is a plain TIFF structure (no "Exif\0\0" header).
 */
public final class WebpRewriter {

    private WebpRewriter() {}

    public static byte[] write(byte[] data, Map<String, Object> updates) {
        int exifChunk = findChunk(data, "EXIF");
        if (exifChunk < 0) {
            return insertChunk(data, "EXIF", buildTiff(data, updates, 0));
        }
        // existing EXIF chunk: read current TIFF (chunk data may have a header)
        int d = exifChunk + 8;
        int size = Binary.get32u(data, exifChunk, ByteOrder.LITTLE_ENDIAN);
        byte[] tiff = Arrays.copyOfRange(data, d, d + size);
        byte[] newTiff = TiffRewriter.rewrite(tiff, orderOf(tiff), updates);
        return replaceChunk(data, exifChunk, newTiff);
    }

    /** Rebuild the EXIF TIFF from the file's current embedded TIFF if present. */
    private static byte[] buildTiff(byte[] data, Map<String, Object> updates, int exifChunk) {
        if (exifChunk >= 0) {
            int d = exifChunk + 8;
            int size = Binary.get32u(data, exifChunk, ByteOrder.LITTLE_ENDIAN);
            byte[] tiff = Arrays.copyOfRange(data, d, d + size);
            return TiffRewriter.rewrite(tiff, orderOf(tiff), updates);
        }
        // no existing EXIF: build a minimal TIFF with the updates
        byte[] empty = {'M', 'M', 0, 42, 0, 0, 0, 8};
        return TiffRewriter.rewrite(empty, ByteOrder.BIG_ENDIAN, updates);
    }

    private static ByteOrder orderOf(byte[] tiff) {
        return tiff.length >= 2 && tiff[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    private static int findChunk(byte[] data, String id) {
        int pos = 12;
        while (pos + 8 <= data.length) {
            // RIFF chunk: type(4) + size(4)
            String type = new String(data, pos, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            long size = Binary.get32u(data, pos + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if (pos + 8 + size > data.length) {
                break;
            }
            if (id.equals(type)) {
                return pos;
            }
            pos += 8 + size + (size & 1);
        }
        return -1;
    }

    private static byte[] replaceChunk(byte[] data, int chunkPos, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, chunkPos);
        long oldSize = Binary.get32u(data, chunkPos + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
        writeChunk(out, "EXIF", payload);
        int oldEnd = chunkPos + 8 + (int) oldSize + (int) (oldSize & 1);
        out.write(data, oldEnd, data.length - oldEnd);
        return out.toByteArray();
    }

    private static byte[] insertChunk(byte[] data, String id, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, 12); // RIFF header + WEBP
        writeChunk(out, id, payload);
        out.write(data, 12, data.length - 12);
        byte[] result = out.toByteArray();
        // update the RIFF size field (4 bytes at offset 4)
        long riffSize = Binary.get32u(data, 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
        long newSize = riffSize + payload.length + 8;
        result[4] = (byte) newSize;
        result[5] = (byte) (newSize >> 8);
        result[6] = (byte) (newSize >> 16);
        result[7] = (byte) (newSize >> 24);
        return result;
    }

    private static void writeChunk(ByteArrayOutputStream out, String id, byte[] payload) {
        for (byte b : id.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) {
            out.write(b);
        }
        long size = payload.length;
        out.write((byte) size);
        out.write((byte) (size >> 8));
        out.write((byte) (size >> 16));
        out.write((byte) (size >> 24));
        out.write(payload, 0, payload.length);
        if ((payload.length & 1) != 0) {
            out.write(0);
        }
    }
}
