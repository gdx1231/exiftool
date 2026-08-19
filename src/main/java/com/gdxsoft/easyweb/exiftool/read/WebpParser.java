package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * WebP (RIFF container) parser: detects the VP8X chunk for extended WebP
 * images and extracts flags plus canvas dimensions.
 */
public final class WebpParser {

    private WebpParser() {}

    public static boolean isWebp(byte[] data) {
        return data.length >= 12
            && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
            && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }

    public static void process(ExifTool et, byte[] data) {
        int pos = 12;
        boolean extended = false;
        while (pos + 8 <= data.length) {
            String id = new String(data, pos, 4, StandardCharsets.ISO_8859_1);
            long len = Binary.get32u(data, pos + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if (pos + 8 + len > data.length) {
                break;
            }
            int d = pos + 8;
            if ("VP8X".equals(id)) {
                extended = true;
                long flags = Binary.get32u(data, d, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
                // bits (0-based): 1=Animation, 2=XMP, 3=EXIF, 4=Alpha, 5=ICC
                String[] names = {null, "Animation", "XMP", "EXIF", "Alpha", "ICC Profile"};
                List<String> set = new ArrayList<>();
                for (int i = 1; i < names.length; i++) {
                    if ((flags & (1L << i)) != 0 && names[i] != null) {
                        set.add(names[i]);
                    }
                }
                et.foundTag("WebP_Flags", String.join(", ", set), 1, "RIFF", "File");
                int w = (int) (Binary.get32u(data, d + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffL) + 1;
                int h = (int) (Binary.get32u(data, d + 6, ByteOrder.LITTLE_ENDIAN) & 0xffffffL) + 1;
                et.foundTag("ImageWidth", String.valueOf(w), 1, "RIFF", "File");
                et.foundTag("ImageHeight", String.valueOf(h), 1, "RIFF", "File");
            } else if ("EXIF".equals(id)) {
                // embedded TIFF data (may or may not have an "Exif\0\0" header)
                int tiffBase = d;
                if (len >= 6 && data[d] == 'E' && data[d + 1] == 'x' && data[d + 2] == 'i'
                    && data[d + 3] == 'f' && data[d + 4] == 0 && data[d + 5] == 0) {
                    tiffBase = d + 6;
                }
                if (ExifParser.isTiff(data) || tiffBase > 0) {
                    new ExifParser(et, data, tiffBase).processTiff();
                }
            }
            pos += 8 + len + (len & 1); // chunks are word-aligned
        }
        et.foundTag("FileType", extended ? "Extended WEBP" : "WEBP", 1, "File", "File");
        et.foundTag("MIMEType", "image/webp", 1, "File", "File");
    }
}
