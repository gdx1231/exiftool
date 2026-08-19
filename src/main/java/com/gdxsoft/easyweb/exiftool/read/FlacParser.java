package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * FLAC parser: STREAMINFO block (sample rate/channels/bits/MD5) and the
 * Vorbis Comment block (vendor + "KEY=VALUE" entries).
 */
public final class FlacParser {

    private FlacParser() {}

    public static boolean isFlac(byte[] data) {
        return data.length >= 4 && data[0] == 'f' && data[1] == 'L' && data[2] == 'a' && data[3] == 'C';
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "FLAC", 1, "File", "File");
        et.foundTag("MIMEType", "audio/flac", 1, "File", "File");
        int pos = 4;
        while (pos + 8 <= data.length) {
            int header = data[pos] & 0xff;
            boolean last = (header & 0x80) != 0;
            int type = header & 0x7f;
            long size = ((data[pos + 1] & 0xffL) << 16) | ((data[pos + 2] & 0xffL) << 8)
                | (data[pos + 3] & 0xffL);
            int d = pos + 4;
            if (d + size > data.length) {
                break;
            }
            switch (type) {
                case 0 -> processStreamInfo(et, data, d);
                case 4 -> processVorbisComment(et, data, d, (int) size);
                default -> {
                    // skip
                }
            }
            if (last) {
                break;
            }
            pos += 4 + size;
        }
    }

    /** STREAMINFO: 34 bytes of audio parameters. */
    private static void processStreamInfo(ExifTool et, byte[] data, int d) {
        if (d + 34 > data.length) {
            return;
        }
        int minBlock = Binary.get16u(data, d, ByteOrder.BIG_ENDIAN);
        int maxBlock = Binary.get16u(data, d + 2, ByteOrder.BIG_ENDIAN);
        long minFrame = ((data[d + 4] & 0xffL) << 16) | ((data[d + 5] & 0xffL) << 8) | (data[d + 6] & 0xffL);
        long maxFrame = ((data[d + 7] & 0xffL) << 16) | ((data[d + 8] & 0xffL) << 8) | (data[d + 9] & 0xffL);
        // 20-bit sample rate, 3-bit channels-1, 5-bit bits-1, 36-bit total samples
        long b10 = data[d + 10] & 0xffL;
        long b11 = data[d + 11] & 0xffL;
        long b12 = data[d + 12] & 0xffL;
        long b13 = data[d + 13] & 0xffL;
        long b14 = data[d + 14] & 0xffL;
        long sampleRate = (b10 << 12) | (b11 << 4) | (b12 >> 4);
        int channels = (int) (((b12 >> 1) & 0x7) + 1);
        int bits = (int) ((((b12 & 0x1) << 4) | (b13 >> 4)) + 1);
        long totalSamples = ((b13 & 0x0fL) << 32) | (b14 << 24)
            | ((data[d + 15] & 0xffL) << 16) | ((data[d + 16] & 0xffL) << 8) | (data[d + 17] & 0xffL);
        StringBuilder md5 = new StringBuilder();
        for (int i = 18; i < 34; i++) {
            md5.append(String.format("%02x", data[d + i] & 0xff));
        }
        et.foundTag("BlockSizeMin", String.valueOf(minBlock), 1, "FLAC", "FLAC");
        et.foundTag("BlockSizeMax", String.valueOf(maxBlock), 1, "FLAC", "FLAC");
        et.foundTag("FrameSizeMin", String.valueOf(minFrame), 1, "FLAC", "FLAC");
        et.foundTag("FrameSizeMax", String.valueOf(maxFrame), 1, "FLAC", "FLAC");
        et.foundTag("SampleRate", String.valueOf(sampleRate), 1, "FLAC", "FLAC");
        et.foundTag("Channels", String.valueOf(channels), 1, "FLAC", "FLAC");
        et.foundTag("BitsPerSample", String.valueOf(bits), 1, "FLAC", "FLAC");
        et.foundTag("TotalSamples", String.valueOf(totalSamples), 1, "FLAC", "FLAC");
        et.foundTag("MD5Signature", md5.toString(), 1, "FLAC", "FLAC");
    }

    /** Vorbis Comment: "vorbis" + vendor + count + KEY=VALUE entries. */
    private static void processVorbisComment(ExifTool et, byte[] data, int d, int size) {
        int end = d + size;
        int p = d;
        if (p + 7 <= end && new String(data, p, 7, StandardCharsets.ISO_8859_1).equals("vorbis")) {
            p += 7;
        }
        if (p + 4 > end) {
            return;
        }
        int vendorLen = Binary.get32u(data, p, ByteOrder.LITTLE_ENDIAN);
        p += 4;
        if (p + vendorLen > end) {
            return;
        }
        String vendor = new String(data, p, vendorLen, StandardCharsets.ISO_8859_1);
        p += vendorLen;
        et.foundTag("Vendor", vendor, 1, "Vorbis", "Vorbis");
        if (p + 4 > end) {
            return;
        }
        int count = Binary.get32u(data, p, ByteOrder.LITTLE_ENDIAN);
        p += 4;
        for (int i = 0; i < count && p + 4 <= end; i++) {
            int len = Binary.get32u(data, p, ByteOrder.LITTLE_ENDIAN);
            p += 4;
            if (p + len > end) {
                break;
            }
            String entry = new String(data, p, len, StandardCharsets.UTF_8);
            int eq = entry.indexOf('=');
            if (eq > 0) {
                String key = entry.substring(0, eq);
                String value = entry.substring(eq + 1);
                // map common Vorbis comment keys to tag names
                String tag = switch (key.toUpperCase(java.util.Locale.ROOT)) {
                    case "TITLE" -> "Title";
                    case "ARTIST" -> "Artist";
                    case "ALBUM" -> "Album";
                    case "ALBUMARTIST" -> "AlbumArtist";
                    case "TRACKNUMBER" -> "Track";
                    case "TRACKTOTAL" -> "TrackTotal";
                    case "DATE", "YEAR" -> "Year";
                    case "GENRE" -> "Genre";
                    case "COMMENT" -> "Comment";
                    case "COMPOSER" -> "Composer";
                    case "COPYRIGHT" -> "Copyright";
                    case "LANGUAGE" -> "Language";
                    default -> key;
                };
                et.foundTag(tag, value, 1, "Vorbis", "Vorbis");
            }
            p += len;
        }
    }
}
