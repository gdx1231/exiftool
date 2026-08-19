package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * GIF header parser: version, logical screen dimensions, color map flag.
 */
public final class GifParser {

    private GifParser() {}

    public static boolean isGif(byte[] data) {
        if (data.length < 6) {
            return false;
        }
        return startsWith(data, "GIF87a") || startsWith(data, "GIF89a");
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "GIF", 1);
        et.foundTag("MIMEType", "image/gif", 1);
        et.foundTag("GIFVersion", new String(data, 3, 3, StandardCharsets.ISO_8859_1), 1);
        if (data.length < 13) {
            return;
        }
        int width = Binary.get16u(data, 6, ByteOrder.LITTLE_ENDIAN);
        int height = Binary.get16u(data, 8, ByteOrder.LITTLE_ENDIAN);
        int flags = data[10] & 0xff;
        et.foundTag("ImageWidth", String.valueOf(width), 1);
        et.foundTag("ImageHeight", String.valueOf(height), 1);
        et.foundTag("HasColorMap", (flags & 0x80) != 0 ? "Yes" : "No", 1);
    }

    private static boolean startsWith(byte[] data, String prefix) {
        byte[] p = prefix.getBytes(StandardCharsets.ISO_8859_1);
        for (int i = 0; i < p.length; i++) {
            if (data[i] != p[i]) {
                return false;
            }
        }
        return true;
    }
}
