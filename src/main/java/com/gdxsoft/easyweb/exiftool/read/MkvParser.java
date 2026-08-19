package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * Matroska (MKV) parser: EBML element walker extracting the Info segment
 * (doc type/muxing app/duration/date), track video parameters (codec/width/
 * height/frame rate) and simple tags.
 */
public final class MkvParser {

    private static final int EBML_DOCTYPE = 0x4282;
    private static final int EBML_DOCTYPE_VERSION = 0x4287;
    private static final int EBML_DOCTYPE_READ_VERSION = 0x4285;
    private static final int SEGMENT = 0x18538067;
    private static final int INFO = 0x1549A966;
    private static final int TIMECODE_SCALE = 0x2AD7B1;
    private static final int MUXING_APP = 0x4D80;
    private static final int WRITING_APP = 0x5741;
    private static final int DURATION = 0x4489;
    private static final int DATE_UTC = 0x4461;
    private static final int TRACKS = 0x1654AE6B;
    private static final int TRACK_ENTRY = 0xAE;
    private static final int TRACK_TYPE = 0x83;
    private static final int CODEC_ID = 0x86;
    private static final int VIDEO = 0xE0;
    private static final int PIXEL_WIDTH = 0xB0;
    private static final int PIXEL_HEIGHT = 0xBA;
    private static final int FRAME_RATE = 0x2383E3;
    private static final int TAGS = 0x1254C367;
    private static final int TAG = 0x7373;
    private static final int SIMPLE_TAG = 0x67C8;
    private static final int TAG_NAME = 0x45A3;
    private static final int TAG_STRING = 0x4487;

    private MkvParser() {}

    public static boolean isMkv(byte[] data) {
        return data.length >= 4 && data[0] == 0x1a && data[1] == 0x45 && data[2] == (byte) 0xdf && data[3] == (byte) 0xa3;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "MKV", 1, "File", "File");
        et.foundTag("MIMEType", "video/x-matroska", 1, "File", "File");
        walk(et, data, 0, data.length);
    }

    private static void walk(ExifTool et, byte[] data, int pos, int end) {
        while (pos < end) {
            EbmlElem elem = readElem(data, pos, end);
            if (elem == null) {
                break;
            }
            switch (elem.id) {
                case EBML_DOCTYPE -> et.foundTag("DocType", elem.asString(data), 1, "Matroska", "Matroska");
                case EBML_DOCTYPE_VERSION -> et.foundTag("DocTypeVersion", String.valueOf(elem.asLong(data)), 1, "Matroska", "Matroska");
                case EBML_DOCTYPE_READ_VERSION -> et.foundTag("DocTypeReadVersion", String.valueOf(elem.asLong(data)), 1, "Matroska", "Matroska");
                case TIMECODE_SCALE -> {
                    long ns = elem.asLong(data);
                    et.foundTag("TimecodeScale", ns / 1_000_000 + " ms", 1, "Matroska", "Matroska");
                }
                case MUXING_APP -> et.foundTag("MuxingApp", elem.asString(data), 1, "Matroska", "Matroska");
                case WRITING_APP -> et.foundTag("WritingApp", elem.asString(data), 1, "Matroska", "Matroska");
                case DURATION -> {
                    double ms = elem.asDouble(data);
                    et.foundTag("Duration", formatDuration(ms), 1, "Matroska", "Matroska");
                }
                case DATE_UTC -> {
                    // nanoseconds since 2001-01-01T00:00:00 UTC
                    long ns = elem.asLong(data);
                    long unixSecs = ns / 1_000_000_000L + 978307200L;
                    java.time.LocalDateTime dt = Instant.ofEpochSecond(unixSecs)
                        .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
                    et.foundTag("DateTimeOriginal", String.format("%04d:%02d:%02d %02d:%02d:%02dZ",
                        dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
                        dt.getHour(), dt.getMinute(), dt.getSecond()), 1, "Matroska", "Matroska");
                }
                case TRACK_TYPE -> currentTrackType = (int) elem.asLong(data);
                case CODEC_ID -> {
                    if (currentTrackType == 1) { // video track
                        et.foundTag("VideoCodecID", elem.asString(data), 1, "Matroska", "Matroska");
                    }
                }
                case PIXEL_WIDTH -> {
                    if (currentTrackType == 1) {
                        et.foundTag("ImageWidth", String.valueOf(elem.asLong(data)), 1, "Matroska", "Matroska");
                    }
                }
                case PIXEL_HEIGHT -> {
                    if (currentTrackType == 1) {
                        et.foundTag("ImageHeight", String.valueOf(elem.asLong(data)), 1, "Matroska", "Matroska");
                    }
                }
                case FRAME_RATE -> {
                    if (currentTrackType == 1) {
                        et.foundTag("VideoFrameRate", String.valueOf(elem.asLong(data)), 1, "Matroska", "Matroska");
                    }
                }
                case TAG_NAME -> currentTagName = elem.asString(data);
                case TAG_STRING -> {
                    if (currentTagName != null && !currentTagName.isBlank()) {
                        et.foundTag(currentTagName, elem.asString(data), 1, "Matroska", "Matroska");
                    }
                }
                default -> {
                    if (elem.isContainer()) {
                        walk(et, data, elem.dataStart, elem.end());
                    }
                }
            }
            pos = elem.end();
        }
    }

    private static String currentTagName;
    private static int currentTrackType = -1;

    private static String formatDuration(double ms) {
        long totalSecs = (long) (ms / 1000.0);
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    private static final class EbmlElem {
        final int id;
        final int size;
        final int dataStart;

        EbmlElem(int id, int size, int dataStart) {
            this.id = id;
            this.size = size;
            this.dataStart = dataStart;
        }

        int end() {
            return dataStart + size;
        }

        boolean isContainer() {
            return switch (id) {
                case 0x1A45DFA3, SEGMENT, INFO, TRACKS, TRACK_ENTRY, VIDEO, TAGS, TAG, SIMPLE_TAG -> true;
                default -> false;
            };
        }

        String asString(byte[] data) {
            int len = Math.min(size, data.length - dataStart);
            return new String(data, dataStart, len, StandardCharsets.UTF_8).replace("\0", "");
        }

        long asLong(byte[] data) {
            long v = 0;
            int len = Math.min(size, 8);
            for (int i = 0; i < len; i++) {
                v = (v << 8) | (data[dataStart + i] & 0xff);
            }
            return v;
        }

        double asDouble(byte[] data) {
            return size == 4 ? Float.intBitsToFloat((int) asLong(data))
                : Double.longBitsToDouble(asLong(data));
        }
    }

    /** Read an EBML element: variable-length ID and size with marker bits. */
    private static EbmlElem readElem(byte[] data, int pos, int end) {
        if (pos + 2 > end) {
            return null;
        }
        // element ID (1-4 bytes)
        int first = data[pos] & 0xff;
        int idLen = 0;
        for (int i = 0; i < 4; i++) {
            if ((first & (0x80 >> i)) != 0) {
                idLen = i + 1;
                break;
            }
        }
        if (idLen == 0) {
            return null;
        }
        if (pos + idLen > end) {
            return null;
        }
        int id = 0;
        for (int i = 0; i < idLen; i++) {
            id = (id << 8) | (data[pos + i] & 0xff);
        }
        // size (variable length; -1 = unknown, use the remaining data)
        int sp = pos + idLen;
        if (sp >= end) {
            return null;
        }
        long sizeVal = readVint(data, sp, end);
        int sizeLen = vintLen(data[sp] & 0xff);
        int dataStart = sp + sizeLen;
        if (sizeVal < 0) {
            sizeVal = end - dataStart; // unknown size: extends to the end
        }
        if (dataStart + sizeVal > end) {
            sizeVal = end - dataStart; // truncated: use the remaining data
        }
        return new EbmlElem(id, (int) sizeVal, dataStart);
    }

    private static int vintLen(int first) {
        for (int i = 0; i < 8; i++) {
            if ((first & (0x80 >> i)) != 0) {
                return i + 1;
            }
        }
        return 8;
    }

    /** Read a variable-length unsigned integer (size field); -1 for "unknown". */
    private static long readVint(byte[] data, int pos, int end) {
        int first = data[pos] & 0xff;
        int len = vintLen(first);
        if (pos + len > end) {
            return -1;
        }
        long v = first & (0xff >>> len);
        for (int i = 1; i < len; i++) {
            v = (v << 8) | (data[pos + i] & 0xff);
        }
        // all value bits set = unknown size (common for the Segment element)
        long unknown = (1L << (7 * len)) - 1;
        return v == unknown ? -1 : v;
    }
}
