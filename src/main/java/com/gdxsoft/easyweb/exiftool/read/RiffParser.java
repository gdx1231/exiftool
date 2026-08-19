package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * RIFF container parser for WAV audio (fmt chunk) and AVI video
 * (hdrl/strh + strf dimensions). Reuses the WebP chunk layout: type(4)+size(4).
 */
public final class RiffParser {

    private RiffParser() {}

    public static boolean isRiff(byte[] data) {
        return data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
    }

    public static void process(ExifTool et, byte[] data) {
        String form = new String(data, 8, 4, StandardCharsets.ISO_8859_1);
        if ("WAVE".equals(form)) {
            et.foundTag("FileType", "WAV", 1, "File", "File");
            et.foundTag("MIMEType", "audio/x-wav", 1, "File", "File");
            processWav(et, data);
        } else if ("AVI ".equals(form)) {
            et.foundTag("FileType", "AVI", 1, "File", "File");
            et.foundTag("MIMEType", "video/x-msvideo", 1, "File", "File");
            processAvi(et, data);
        } else {
            et.foundTag("FileType", "RIFF", 1, "File", "File");
            et.foundTag("MIMEType", "application/octet-stream", 1, "File", "File");
        }
    }

    /** WAV: fmt chunk holds the audio format. */
    private static void processWav(ExifTool et, byte[] data) {
        Chunk fmt = findChunk(data, "fmt ");
        if (fmt != null && fmt.size >= 16) {
            int d = fmt.dataStart;
            int format = Binary.get16u(data, d, ByteOrder.LITTLE_ENDIAN);
            int channels = Binary.get16u(data, d + 2, ByteOrder.LITTLE_ENDIAN);
            long sampleRate = Binary.get32u(data, d + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            long byteRate = Binary.get32u(data, d + 8, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            int bits = Binary.get16u(data, d + 14, ByteOrder.LITTLE_ENDIAN);
            et.foundTag("Encoding", switch (format) {
                case 1 -> "Microsoft PCM";
                case 3 -> "Microsoft float";
                case 6 -> "Microsoft A-law";
                case 7 -> "Microsoft u-law";
                default -> "Unknown (" + format + ")";
            }, 1, "RIFF", "File");
            et.foundTag("NumChannels", String.valueOf(channels), 1, "RIFF", "File");
            et.foundTag("SampleRate", String.valueOf(sampleRate), 1, "RIFF", "File");
            et.foundTag("BitsPerSample", String.valueOf(bits), 1, "RIFF", "File");
            // duration from the data chunk
            Chunk dataChunk = findChunk(data, "data");
            if (dataChunk != null && byteRate > 0) {
                double secs = dataChunk.size / (double) byteRate;
                et.foundTag("Duration", String.format("%.2f s", secs), 1, "RIFF", "File");
            }
        }
    }

    /** AVI: LIST/hdrl holds the video stream header (strh/strf may be nested). */
    private static void processAvi(ExifTool et, byte[] data) {
        Chunk list = findChunk(data, "LIST");
        if (list == null) {
            return;
        }
        // the LIST type field ("hdrl") precedes the child chunks
        int hdrlStart = list.dataStart + 4;
        int hdrlEnd = list.offset + list.size;
        Chunk strh = findNested(data, hdrlStart, hdrlEnd, "strh");
        if (strh != null && strh.size >= 40) {
            int d = strh.dataStart;
            String streamType = new String(data, d, 4, StandardCharsets.ISO_8859_1);
            long scale = Binary.get32u(data, d + 20, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            long rate = Binary.get32u(data, d + 24, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            long length = Binary.get32u(data, d + 28, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if ("vids".equals(streamType)) {
                et.foundTag("StreamType", "Video", 1, "RIFF", "File");
                if (rate > 0 && scale > 0) {
                    et.foundTag("FrameRate", String.format("%.2f", rate / (double) scale), 1, "RIFF", "File");
                    et.foundTag("Duration", String.format("%.2f s", length * scale / (double) rate), 1, "RIFF", "File");
                }
            } else if ("auds".equals(streamType)) {
                et.foundTag("StreamType", "Audio", 1, "RIFF", "File");
            }
        }
        Chunk strf = findNested(data, hdrlStart, hdrlEnd, "strf");
        if (strf != null && strf.size >= 40) {
            // BITMAPINFOHEADER: width at +4, height at +8
            int d = strf.dataStart;
            long w = Binary.get32u(data, d + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            long h = Binary.get32u(data, d + 8, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if (w > 0 && h > 0) {
                et.foundTag("ImageWidth", String.valueOf(w), 1, "RIFF", "File");
                et.foundTag("ImageHeight", String.valueOf(h), 1, "RIFF", "File");
            }
        }
    }

    private record Chunk(String type, int offset, int size, int dataStart) {}

    private static Chunk findChunk(byte[] data, String type) {
        int pos = 12;
        while (pos + 8 <= data.length) {
            String t = new String(data, pos, 4, StandardCharsets.ISO_8859_1);
            long size = Binary.get32u(data, pos + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if (pos + 8 + size > data.length) {
                break;
            }
            if (type.equals(t)) {
                return new Chunk(t, pos, (int) size, pos + 8);
            }
            pos += 8 + size + (size & 1);
        }
        return null;
    }

    /** Recursively find a chunk of the given type (descending into LISTs). */
    private static Chunk findNested(byte[] data, int start, int end, String type) {
        int pos = start;
        while (pos + 8 <= end && pos + 8 <= data.length) {
            String t = new String(data, pos, 4, StandardCharsets.ISO_8859_1);
            long size = Binary.get32u(data, pos + 4, ByteOrder.LITTLE_ENDIAN) & 0xffffffffL;
            if (pos + 8 + size > end) {
                break;
            }
            if (type.equals(t)) {
                return new Chunk(t, pos, (int) size, pos + 8);
            }
            if ("LIST".equals(t) && size >= 4) {
                Chunk found = findNested(data, pos + 12, pos + 8 + (int) size, type);
                if (found != null) {
                    return found;
                }
            }
            pos += 8 + size + (size & 1);
        }
        return null;
    }
}
