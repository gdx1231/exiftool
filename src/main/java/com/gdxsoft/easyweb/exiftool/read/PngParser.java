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
            }
            pos += 8 + len + 4; // length + type + data + CRC
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
