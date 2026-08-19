package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * PNG chunk scanner: parses the IHDR header for image dimensions, bit depth,
 * color type and interlacing. Phase 4 covers the file-level tags only.
 */
public final class PngParser {

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};

    private PngParser() {}

    public static boolean isPng(byte[] data) {
        if (data.length < PNG_SIG.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIG.length; i++) {
            if (data[i] != PNG_SIG[i]) {
                return false;
            }
        }
        return true;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "PNG", 1, "File", "File");
        et.foundTag("MIMEType", "image/png", 1, "File", "File");
        int pos = PNG_SIG.length;
        while (pos + 8 <= data.length) {
            int len = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN);
            if (pos + 8 + len > data.length) {
                break;
            }
            String type = new String(data, pos + 4, 4, StandardCharsets.ISO_8859_1);
            int dataStart = pos + 8;
            if ("IHDR".equals(type) && len >= 13) {
                processIhdr(et, data, dataStart);
            } else if ("eXIf".equals(type)) {
                // EXIF data: TIFF structure, possibly with an "Exif\0\0" header
                int tiffBase = dataStart;
                if (len >= 6 && data[dataStart] == 'E' && data[dataStart + 1] == 'x'
                    && data[dataStart + 2] == 'i' && data[dataStart + 3] == 'f'
                    && data[dataStart + 4] == 0 && data[dataStart + 5] == 0) {
                    tiffBase = dataStart + 6;
                }
                new ExifParser(et, data, tiffBase).processTiff();
            } else if ("iTXt".equals(type) || "tEXt".equals(type) || "zTXt".equals(type)) {
                processTextChunk(et, data, dataStart, len, type);
            }
            pos += 8 + len + 4; // length + type + data + CRC
        }
    }

    /** PNG text tags whose tEXt keyword maps to a tag name. */
    private static final java.util.Map<String, String> TEXT_TAGS = java.util.Map.ofEntries(
        java.util.Map.entry("Title", "Title"), java.util.Map.entry("Artist", "Artist"),
        java.util.Map.entry("Author", "Author"), java.util.Map.entry("Copyright", "Copyright"),
        java.util.Map.entry("Description", "Description"), java.util.Map.entry("Comment", "Comment"),
        java.util.Map.entry("Software", "Software"), java.util.Map.entry("Creation Time", "CreateDate"),
        java.util.Map.entry("Disclaimer", "Disclaimer"), java.util.Map.entry("Warning", "Warning"),
        java.util.Map.entry("Source", "Source"));

    /** Parse a text chunk: keyword + text (iTXt has extra header fields). */
    private static void processTextChunk(ExifTool et, byte[] data, int dataStart, int len, String type) {
        int kwEnd = dataStart;
        while (kwEnd < dataStart + len && data[kwEnd] != 0) {
            kwEnd++;
        }
        String keyword = new String(data, dataStart, kwEnd - dataStart, StandardCharsets.ISO_8859_1);
        if ("XML:com.adobe.xmp".equals(keyword)) {
            int textStart = kwEnd + 1;
            if ("iTXt".equals(type)) {
                // iTXt: keyword\0 + flag(1) + method(1) + lang\0 + translated\0 + text
                textStart += 2;
                while (textStart < dataStart + len && data[textStart] != 0) {
                    textStart++;
                }
                textStart++;
                while (textStart < dataStart + len && data[textStart] != 0) {
                    textStart++;
                }
                textStart++;
            }
            String xml = new String(data, textStart, dataStart + len - textStart, StandardCharsets.UTF_8);
            XmpParser.process(et, xml);
            return;
        }
        // plain text tag (tEXt or uncompressed iTXt)
        String tag = TEXT_TAGS.get(keyword);
        if (tag == null) {
            return;
        }
        int textStart = kwEnd + 1;
        if ("iTXt".equals(type)) {
            textStart += 2;
            while (textStart < dataStart + len && data[textStart] != 0) {
                textStart++;
            }
            textStart++;
            while (textStart < dataStart + len && data[textStart] != 0) {
                textStart++;
            }
            textStart++;
        }
        String text = new String(data, textStart, dataStart + len - textStart,
            StandardCharsets.ISO_8859_1);
        if (!text.isEmpty()) {
            et.foundTag(tag, text, 1, "PNG", "PNG");
        }
    }

    private static void processIhdr(ExifTool et, byte[] data, int d) {
        int width = Binary.get32u(data, d, ByteOrder.BIG_ENDIAN);
        int height = Binary.get32u(data, d + 4, ByteOrder.BIG_ENDIAN);
        int bitDepth = data[d + 8] & 0xff;
        int colorType = data[d + 9] & 0xff;
        int interlace = data[d + 12] & 0xff;
        et.foundTag("ImageWidth", String.valueOf(width), 1, "PNG", "File");
        et.foundTag("ImageHeight", String.valueOf(height), 1, "PNG", "File");
        et.foundTag("BitDepth", String.valueOf(bitDepth), 1, "PNG", "File");
        et.foundTag("ColorType", switch (colorType) {
            case 0 -> "Grayscale";
            case 2 -> "RGB";
            case 3 -> "Palette";
            case 4 -> "GrayAlpha";
            case 6 -> "RGBA";
            default -> "Unknown (" + colorType + ")";
        }, 1, "PNG", "File");
        et.foundTag("Compression", "Deflate/Inflate", 1, "PNG", "File");
        et.foundTag("Filter", "Adaptive", 1, "PNG", "File");
        et.foundTag("Interlace", interlace == 0 ? "Noninterlaced" : "Interlaced", 1, "PNG", "File");
    }
}
