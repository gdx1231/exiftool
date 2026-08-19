package com.gdxsoft.easyweb.exiftool.write;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.read.Binary;

/**
 * PNG rewriter: replaces or creates the eXIf chunk for EXIF tag updates and the
 * iTXt chunk for XMP tag updates. Chunk payloads are a TIFF structure (possibly
 * with an "Exif\0\0" header) and RDF/XML respectively; the CRC32 is recomputed
 * over the chunk type + data.
 */
public final class PngRewriter {

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};

    private PngRewriter() {}

    public static byte[] write(byte[] data, Map<String, Object> updates) {
        // split EXIF vs XMP updates
        Map<String, Object> exifUpdates = new java.util.HashMap<>();
        Map<String, Object> xmpUpdates = new java.util.HashMap<>();
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            if (XmpWriter.TAGS.contains(e.getKey())) {
                xmpUpdates.put(e.getKey(), e.getValue());
            } else {
                exifUpdates.put(e.getKey(), e.getValue());
            }
        }
        byte[] out = data;
        if (!exifUpdates.isEmpty()) {
            int exifPos = findChunk(out, "eXIf");
            byte[] tiff = buildTiff(out, exifPos, exifUpdates);
            if (exifPos >= 0) {
                out = replaceChunk(out, exifPos, tiff);
            } else {
                out = insertChunk(out, tiff);
            }
        }
        if (!xmpUpdates.isEmpty()) {
            out = writeXmp(out, xmpUpdates);
        }
        return out;
    }

    /** PNG native text tags written as tEXt chunks (keyword = tag name). */
    private static final java.util.Set<String> PNG_TEXT_TAGS = java.util.Set.of(
        "Title", "Artist", "Author", "Copyright", "Description", "Comment",
        "Software", "CreateDate", "Disclaimer", "Warning", "Source");

    /** Update or insert text chunks for XMP-family updates. */
    private static byte[] writeXmp(byte[] data, Map<String, Object> updates) {
        try {
            // separate PNG native text tags (tEXt) from document XMP tags (iTXt)
            Map<String, Object> textUpdates = new java.util.LinkedHashMap<>();
            Map<String, Object> xmpUpdates = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Object> e : updates.entrySet()) {
                if (PNG_TEXT_TAGS.contains(e.getKey())) {
                    textUpdates.put(e.getKey(), e.getValue());
                } else {
                    xmpUpdates.put(e.getKey(), e.getValue());
                }
            }
            byte[] out = data;
            if (!textUpdates.isEmpty()) {
                out = writeTextChunks(out, textUpdates);
            }
            if (!xmpUpdates.isEmpty()) {
                out = writeXmpChunk(out, xmpUpdates);
            }
            return out;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e); // ByteArrayOutputStream never throws
        }
    }

    /** Write PNG native text tags as tEXt chunks (replace or insert). */
    private static byte[] writeTextChunks(byte[] data, Map<String, Object> updates)
        throws java.io.IOException {
        byte[] out = data;
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            if (e.getValue() == null) {
                out = deleteTextChunk(out, e.getKey());
                continue;
            }
            byte[] payload = (e.getKey() + "\0" + e.getValue())
                .getBytes(StandardCharsets.ISO_8859_1);
            int pos = findTextChunk(out, e.getKey());
            if (pos >= 0) {
                out = replaceChunk(out, pos, payload);
            } else {
                out = insertTextChunk(out, payload);
            }
        }
        return out;
    }

    /** Find a tEXt/iTXt chunk with the given keyword; -1 if absent. */
    private static int findTextChunk(byte[] data, String keyword) {
        int pos = PNG_SIG.length;
        while (pos + 8 <= data.length) {
            int len = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
            String type = new String(data, pos + 4, 4, StandardCharsets.ISO_8859_1);
            if (pos + 12 + len > data.length) {
                break;
            }
            if ("tEXt".equals(type) && len >= keyword.length() + 1) {
                int d = pos + 8;
                String kw = new String(data, d, keyword.length(), StandardCharsets.ISO_8859_1);
                if (kw.equals(keyword) && data[d + keyword.length()] == 0) {
                    return pos;
                }
            }
            pos += 12 + len;
        }
        return -1;
    }

    /** Delete a tEXt chunk with the given keyword. */
    private static byte[] deleteTextChunk(byte[] data, String keyword) {
        int pos = findTextChunk(data, keyword);
        if (pos < 0) {
            return data;
        }
        int len = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
        byte[] out = new byte[data.length - (12 + len)];
        System.arraycopy(data, 0, out, 0, pos);
        System.arraycopy(data, pos + 12 + len, out, pos, data.length - pos - 12 - len);
        return out;
    }

    /** Insert a tEXt chunk before the first IDAT chunk. */
    private static byte[] insertTextChunk(byte[] data, byte[] payload) {
        int idat = findChunk(data, "IDAT");
        if (idat < 0) {
            idat = data.length - 8; // before IEND
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, idat);
        writeChunk(out, "tEXt", payload);
        out.write(data, idat, data.length - idat);
        return out.toByteArray();
    }

    /** Update or insert the iTXt chunk holding document XMP data. */
    private static byte[] writeXmpChunk(byte[] data, Map<String, Object> updates) {
        int xmpPos = findXmpChunk(data);
        byte[] xml;
        if (xmpPos >= 0) {
            byte[] old = extractXmpXml(data, xmpPos);
            byte[] updated = XmpWriter.update(old, updates);
            xml = updated != null ? updated : XmpWriter.build(updates);
        } else {
            xml = XmpWriter.build(updates);
        }
        // iTXt payload: "XML:com.adobe.xmp\0" + flag(0) + method(0) + "\0\0" + xml
        byte[] kw = "XML:com.adobe.xmp\0".getBytes(StandardCharsets.ISO_8859_1);
        byte[] p = new byte[kw.length + 4 + xml.length];
        System.arraycopy(kw, 0, p, 0, kw.length);
        System.arraycopy(xml, 0, p, kw.length + 4, xml.length);
        if (xmpPos >= 0) {
            return replaceChunk(data, xmpPos, p);
        }
        return insertChunk(data, p);
    }

    /** Find the iTXt/tEXt chunk with keyword "XML:com.adobe.xmp"; -1 if absent. */
    private static int findXmpChunk(byte[] data) {
        int pos = PNG_SIG.length;
        while (pos + 8 <= data.length) {
            int len = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
            String type = new String(data, pos + 4, 4, StandardCharsets.ISO_8859_1);
            if (pos + 12 + len > data.length) {
                break;
            }
            if (("iTXt".equals(type) || "tEXt".equals(type)) && len >= 16) {
                int d = pos + 8;
                int kwEnd = d;
                while (kwEnd < d + len && data[kwEnd] != 0) {
                    kwEnd++;
                }
                String keyword = new String(data, d, kwEnd - d, StandardCharsets.ISO_8859_1);
                if ("XML:com.adobe.xmp".equals(keyword)) {
                    return pos;
                }
            }
            pos += 12 + len;
        }
        return -1;
    }

    /** Extract the XML text from an iTXt/tEXt XMP chunk. */
    private static byte[] extractXmpXml(byte[] data, int chunkPos) {
        int len = Binary.get32u(data, chunkPos, ByteOrder.BIG_ENDIAN);
        String type = new String(data, chunkPos + 4, 4, StandardCharsets.ISO_8859_1);
        int d = chunkPos + 8;
        int kwEnd = d;
        while (data[kwEnd] != 0) {
            kwEnd++;
        }
        int textStart = kwEnd + 1;
        if ("iTXt".equals(type)) {
            textStart += 2;
            while (data[textStart] != 0) {
                textStart++;
            }
            textStart++;
            while (data[textStart] != 0) {
                textStart++;
            }
            textStart++;
        }
        return Arrays.copyOfRange(data, textStart, d + len);
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
        // preserve the original chunk type (eXIf, tEXt, iTXt, ...)
        String type = new String(data, chunkPos + 4, 4, StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, chunkPos);
        writeChunk(out, type, payload);
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
