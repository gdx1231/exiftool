package com.gdxsoft.easyweb.exiftool.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * ASF/WMV parser: top-level objects addressed by 16-byte GUIDs. Reads the File
 * Properties object (creation date/duration/size) and the Content Description
 * object (Title/Author/Copyright/Description/Rating, UTF-16LE strings).
 */
public final class AsfParser {

    private static final UUID HEADER = UUID.fromString("75B22630-668E-11CF-A6D9-00AA0062CE6C");
    private static final UUID FILE_PROPERTIES = UUID.fromString("8CABDCA1-A947-11CF-8EE4-00C00C205365");
    private static final UUID CONTENT_DESCRIPTION = UUID.fromString("75B22633-668E-11CF-A6D9-00AA0062CE6C");

    private AsfParser() {}

    public static boolean isAsf(byte[] data) {
        return data.length >= 16 && uuidAt(data, 0).equals(HEADER);
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "WMV", 1, "File", "File");
        et.foundTag("MIMEType", "video/x-ms-wmv", 1, "File", "File");
        // header object: GUID(16) + size(8) + count(4) + reserved(2); objects at 30
        long headerSize = Binary.get64u(data, 16, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        int subCount = Binary.get32u(data, 24, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        int pos = 30;
        long end = Math.min(headerSize, data.length);
        int scanned = 0;
        while (pos + 24 <= end && scanned < subCount + 10) {
            UUID guid = uuidAt(data, pos);
            long size = Binary.get64u(data, pos + 16, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            if (size < 24 || pos + size > end) {
                break;
            }
            if (guid.equals(FILE_PROPERTIES)) {
                processFileProperties(et, data, pos + 24);
            } else if (guid.equals(CONTENT_DESCRIPTION)) {
                processContentDescription(et, data, pos + 24);
            }
            pos += size;
            scanned++;
        }
    }

    private static UUID uuidAt(byte[] data, int pos) {
        // ASF GUIDs are mixed-endian: first 3 groups little-endian, last 2 big-endian
        long d0 = Binary.get32u(data, pos, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
        long d1 = Binary.get16u(data, pos + 4, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN) & 0xffffL;
        long d2 = Binary.get16u(data, pos + 6, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN) & 0xffffL;
        long msb = (d0 << 32) | (d1 << 16) | d2;
        long lsb = Binary.get64u(data, pos + 8, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
        return new UUID(msb, lsb);
    }

    /** File Properties: file size, creation date (100ns since 1601), duration. */
    private static void processFileProperties(ExifTool et, byte[] data, int d) {
        if (d + 80 > data.length) {
            return;
        }
        // FileID(16) + FileSize(8) + CreationDate(8) + DataPackets(8) + PlayDuration(8)
        long fileSize = Binary.get64u(data, d + 16, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        long creation100ns = Binary.get64u(data, d + 24, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        long playDuration100ns = Binary.get64u(data, d + 40, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        et.foundTag("FileSize", String.valueOf(fileSize), 1, "ASF", "ASF");
        // 100 ns units since 1601-01-01 -> unix seconds
        long unixSecs = creation100ns / 10_000_000 - 11644473600L;
        java.time.LocalDateTime dt = java.time.Instant.ofEpochSecond(unixSecs)
            .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
        et.foundTag("CreationDate", String.format("%04d:%02d:%02d %02d:%02d:%02dZ",
            dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
            dt.getHour(), dt.getMinute(), dt.getSecond()), 1, "ASF", "ASF");
        et.foundTag("Duration", String.format("%.2f s", playDuration100ns / 10_000_000.0), 1, "ASF", "ASF");
    }

    /** Content Description: Title/Author/Copyright/Description/Rating (UTF-16LE). */
    private static void processContentDescription(ExifTool et, byte[] data, int d) {
        if (d + 10 > data.length) {
            return;
        }
        int[] lens = new int[5];
        for (int i = 0; i < 5; i++) {
            lens[i] = Binary.get16u(data, d + i * 2, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
        }
        String[] names = {"Title", "Author", "Copyright", "Description", "Rating"};
        int p = d + 10;
        for (int i = 0; i < 5; i++) {
            if (p + lens[i] > data.length) {
                return;
            }
            if (lens[i] > 0) {
                String value = new String(data, p, lens[i], StandardCharsets.UTF_16LE);
                if (!value.isBlank()) {
                    et.foundTag(names[i], value, 1, "ASF", "ASF");
                }
            }
            p += lens[i];
        }
    }
}
