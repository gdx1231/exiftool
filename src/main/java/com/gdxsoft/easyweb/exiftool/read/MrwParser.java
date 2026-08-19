package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;
import com.gdxsoft.easyweb.exiftool.tables.MrwTables;

/**
 * Minolta MRW (Minolta RAW) parser. Structure: "\0MRM" header + file size,
 * then blocks of "\0" + 3-byte type + 4-byte big-endian size + data.
 * The TTW block contains TIFF/EXIF data; the PRD block holds camera settings.
 */
public final class MrwParser {

    private MrwParser() {}

    public static boolean isMrw(byte[] data) {
        return data.length >= 8 && data[0] == 0 && data[1] == 'M' && data[2] == 'R' && data[3] == 'M';
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "MRW", 1, "File", "File");
        et.foundTag("MIMEType", "image/x-minolta-mrw", 1, "File", "File");
        int pos = 8; // header + file size
        while (pos + 8 <= data.length) {
            if (data[pos] != 0) {
                break;
            }
            String type = new String(data, pos + 1, 3, java.nio.charset.StandardCharsets.ISO_8859_1);
            int size = Binary.get32u(data, pos + 4, ByteOrder.BIG_ENDIAN);
            int d = pos + 8;
            if (d + size > data.length) {
                break;
            }
            switch (type) {
                case "TTW" -> {
                    if (d + 8 <= data.length) {
                        new ExifParser(et, data, d).processTiff();
                    }
                }
                case "PRD" -> BinaryDataParser.process(et, data, d, ByteOrder.BIG_ENDIAN,
                    MrwTables.prd(), size, "MRW", "PRD");
                default -> {
                    // WBG/RIF/CSA: not decoded in Phase 14
                }
            }
            pos = d + size;
        }
    }
}
