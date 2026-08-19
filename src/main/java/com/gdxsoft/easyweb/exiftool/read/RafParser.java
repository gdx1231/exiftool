package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * FujiFilm RAF (Raw File Format) parser: the file starts with a
 * "FUJIFILMCCD-RAW" header; EXIF is a TIFF structure located later in the
 * file (found by scanning for the TIFF magic).
 */
public final class RafParser {

    private static final byte[] RAF_SIG = "FUJIFILMCCD-RAW".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

    private RafParser() {}

    public static boolean isRaf(byte[] data) {
        if (data.length < RAF_SIG.length) {
            return false;
        }
        for (int i = 0; i < RAF_SIG.length; i++) {
            if (data[i] != RAF_SIG[i]) {
                return false;
            }
        }
        return true;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "RAF", 1);
        et.foundTag("MIMEType", "image/x-fujifilm-raf", 1);
        int tiff = findTiff(data);
        if (tiff > 0) {
            new ExifParser(et, data, tiff).processTiff();
        }
    }

    private static int findTiff(byte[] data) {
        for (int i = 8; i + 4 <= data.length && i < 4096; i++) {
            if (data[i] == 'I' && data[i + 1] == 'I' && data[i + 2] == 42 && data[i + 3] == 0) {
                return i;
            }
            if (data[i] == 'M' && data[i + 1] == 'M' && data[i + 2] == 0 && data[i + 3] == 42) {
                return i;
            }
        }
        return -1;
    }
}
