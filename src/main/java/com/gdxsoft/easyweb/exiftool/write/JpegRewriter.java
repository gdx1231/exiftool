package com.gdxsoft.easyweb.exiftool.write;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.read.Binary;
import com.gdxsoft.easyweb.exiftool.read.XmpParser;

/**
 * JPEG rewriter: rebuilds the APP1/Exif segment (via {@link TiffRewriter}) for
 * EXIF tag updates, and updates/creates the APP1/XMP segment for XMP tags.
 */
public final class JpegRewriter {

    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};
    private static final byte[] XMP_NS = "http://ns.adobe.com/xap/1.0/\u0000"
        .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

    private JpegRewriter() {}

    /**
     * Rewrite EXIF/XMP data in a JPEG file, applying the given tag updates.
     */
    public static byte[] write(byte[] data, Map<String, Object> updates) {
        Map<String, Object> exifUpdates = new HashMap<>();
        Map<String, Object> xmpUpdates = new HashMap<>();
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            if (XmpWriter.TAGS.contains(e.getKey())) {
                xmpUpdates.put(e.getKey(), e.getValue());
            } else {
                exifUpdates.put(e.getKey(), e.getValue());
            }
        }
        byte[] out = exifUpdates.isEmpty() ? data : rewriteExif(data, exifUpdates);
        if (!xmpUpdates.isEmpty()) {
            out = rewriteXmp(out, xmpUpdates);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // EXIF segment
    // ------------------------------------------------------------------

    private static byte[] rewriteExif(byte[] data, Map<String, Object> updates) {
        int pos = 2; // skip SOI
        while (pos + 4 <= data.length) {
            if ((data[pos] & 0xff) != 0xff) {
                pos++;
                continue;
            }
            int marker = data[pos + 1] & 0xff;
            if (marker == 0xd8 || marker == 0xd9 || marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                pos += 2;
                continue;
            }
            int len = Binary.get16u(data, pos + 2, ByteOrder.BIG_ENDIAN);
            int segStart = pos + 4;
            int segEnd = segStart + len - 2;
            if (segEnd > data.length) {
                break;
            }
            if (marker == 0xe1 && hasHeader(data, segStart, EXIF_HEADER)) {
                int tiffBase = segStart + EXIF_HEADER.length;
                byte[] tiff = Arrays.copyOfRange(data, tiffBase, segEnd);
                ByteOrder order = tiff.length >= 2 && tiff[0] == 'I' ? ByteOrder.LITTLE_ENDIAN
                    : ByteOrder.BIG_ENDIAN;
                byte[] newTiff = TiffRewriter.rewrite(tiff, order, updates);
                return rebuild(data, pos, segStart, 0xe1, EXIF_HEADER, newTiff);
            }
            pos = segEnd;
        }
        return data; // no EXIF segment found
    }

    // ------------------------------------------------------------------
    // XMP segment
    // ------------------------------------------------------------------

    private static byte[] rewriteXmp(byte[] data, Map<String, Object> updates) {
        int pos = 2;
        while (pos + 4 <= data.length) {
            if ((data[pos] & 0xff) != 0xff) {
                pos++;
                continue;
            }
            int marker = data[pos + 1] & 0xff;
            if (marker == 0xd8 || marker == 0xd9 || marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                pos += 2;
                continue;
            }
            int len = Binary.get16u(data, pos + 2, ByteOrder.BIG_ENDIAN);
            int segStart = pos + 4;
            int segEnd = segStart + len - 2;
            if (segEnd > data.length) {
                break;
            }
            if (marker == 0xe1 && hasHeader(data, segStart, XMP_NS)) {
                byte[] xml = Arrays.copyOfRange(data, segStart + XMP_NS.length, segEnd);
                byte[] newXml = XmpWriter.update(xml, updates);
                if (newXml != null) {
                    ByteArrayOutputStream ns = new ByteArrayOutputStream();
                    ns.write(XMP_NS, 0, XMP_NS.length);
                    ns.write(newXml, 0, newXml.length);
                    return rebuild(data, pos, segStart, 0xe1, ns.toByteArray(), new byte[0]);
                }
                return data;
            }
            pos = segEnd;
        }
        // no XMP segment: append a new APP1/XMP segment right after the SOI
        byte[] xml = XmpWriter.build(updates);
        ByteArrayOutputStream ns = new ByteArrayOutputStream();
        ns.write(XMP_NS, 0, XMP_NS.length);
        ns.write(xml, 0, xml.length);
        return insertSegment(data, 0xe1, ns.toByteArray());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Replace the segment at {@code segPos} with header+payload. */
    private static byte[] rebuild(byte[] data, int segPos, int segStart, int marker,
        byte[] header, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, segPos);
        int newLen = 2 + header.length + payload.length;
        out.write(0xff);
        out.write(marker);
        out.write((newLen >> 8) & 0xff);
        out.write(newLen & 0xff);
        out.write(header, 0, header.length);
        out.write(payload, 0, payload.length);
        int oldSegEnd = segStart + Binary.get16u(data, segPos + 2, ByteOrder.BIG_ENDIAN) - 2;
        out.write(data, oldSegEnd, data.length - oldSegEnd);
        return out.toByteArray();
    }

    /** Insert a new APP1 segment after the SOI marker. */
    private static byte[] insertSegment(byte[] data, int marker, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 64);
        out.write(data, 0, 2); // SOI
        int newLen = 2 + payload.length;
        out.write(0xff);
        out.write(marker);
        out.write((newLen >> 8) & 0xff);
        out.write(newLen & 0xff);
        out.write(payload, 0, payload.length);
        out.write(data, 2, data.length - 2);
        return out.toByteArray();
    }

    private static boolean hasHeader(byte[] data, int start, byte[] header) {
        if (start + header.length > data.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (data[start + i] != header[i]) {
                return false;
            }
        }
        return true;
    }
}
