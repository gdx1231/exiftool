package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * JPEG segment scanner: walks the JPEG marker structure and dispatches embedded
 * EXIF (APP1 "Exif\0\0") payloads to {@link ExifParser}.
 */
public final class JpegParser {

    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    private JpegParser() {}

    public static boolean isJpeg(byte[] data) {
        return data.length >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "JPEG", 1);
        et.foundTag("MIMEType", "image/jpeg", 1);
        int pos = 2; // skip SOI
        boolean haveSof = false;
        java.util.Map<String, java.util.List<byte[]>> extXmp = new java.util.HashMap<>();
        while (pos + 4 <= data.length) {
            if ((data[pos] & 0xff) != 0xff) {
                pos++; // skip fill bytes
                continue;
            }
            int marker = data[pos + 1] & 0xff;
            if (marker == 0xd8 || marker == 0xd9) {
                pos += 2;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                pos += 2; // standalone markers without length
                continue;
            }
            if (pos + 4 > data.length) {
                break;
            }
            int len = Binary.get16u(data, pos + 2, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
            int segStart = pos + 4;
            int segEnd = segStart + len - 2; // len includes the 2 length bytes
            if (segEnd > data.length) {
                break;
            }
            if (marker == 0xe1 && hasExifHeader(data, segStart, segEnd)) {
                // TIFF data starts after the "Exif\0\0" header
                new ExifParser(et, data, segStart + EXIF_HEADER.length).processTiff();
            } else if (marker == 0xe1
                && (hasNs(data, segStart, XmpParser.XAP_NS) || hasNs(data, segStart, XmpParser.EXT_NS))) {
                XmpParser.processSegment(et, data, segStart, segEnd, extXmp);
            } else if (marker == 0xe0 && hasJfifHeader(data, segStart, segEnd)) {
                processJfif(et, data, segStart);
            } else if (!haveSof && isSofMarker(marker) && segEnd - segStart >= 6) {
                processSof(et, data, segStart, marker);
                haveSof = true;
            }
            pos = segEnd;
        }
        XmpParser.finishExtended(et, extXmp);
    }

    /** True if the segment starts with the given namespace string. */
    private static boolean hasNs(byte[] data, int start, String ns) {
        byte[] n = ns.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        if (start + n.length > data.length) {
            return false;
        }
        for (int i = 0; i < n.length; i++) {
            if (data[start + i] != n[i]) {
                return false;
            }
        }
        return true;
    }

    private static void processJfif(ExifTool et, byte[] data, int segStart) {
        // "JFIF\0" + version(2) + units(1) + xdensity(2) + ydensity(2)
        if (data.length < segStart + 9) {
            return;
        }
        int maj = data[segStart + 5] & 0xff;
        int min = data[segStart + 6] & 0xff;
        et.foundTag("JFIFVersion", maj + "." + String.format("%02d", min), 1);
    }

    private static void processSof(ExifTool et, byte[] data, int segStart, int marker) {
        // SOF: precision(1) + height(2) + width(2) + components(1)
        int precision = data[segStart] & 0xff;
        int height = Binary.get16u(data, segStart + 1, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
        int width = Binary.get16u(data, segStart + 3, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
        int components = data[segStart + 5] & 0xff;
        et.foundTag("ImageWidth", String.valueOf(width), 1);
        et.foundTag("ImageHeight", String.valueOf(height), 1);
        et.foundTag("BitsPerSample", String.valueOf(precision), 1);
        et.foundTag("ColorComponents", String.valueOf(components), 1);
        et.foundTag("EncodingProcess",
            marker == 0xc0 ? "Baseline DCT, Huffman coding" : "Progressive DCT, Huffman coding", 1);
    }

    /** SOF markers (C0-C3, C5-C7, C9-CB, CD-CF) excluding DHT(C4)/JPG(C8)/DAC(CC). */
    private static boolean isSofMarker(int marker) {
        return (marker >= 0xc0 && marker <= 0xcf)
            && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

    private static boolean hasJfifHeader(byte[] data, int start, int end) {
        return end - start >= 5 && data[start] == 'J' && data[start + 1] == 'F'
            && data[start + 2] == 'I' && data[start + 3] == 'F' && data[start + 4] == 0;
    }

    private static boolean hasExifHeader(byte[] data, int start, int end) {
        if (end - start < EXIF_HEADER.length) {
            return false;
        }
        for (int i = 0; i < EXIF_HEADER.length; i++) {
            if (data[start + i] != EXIF_HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
