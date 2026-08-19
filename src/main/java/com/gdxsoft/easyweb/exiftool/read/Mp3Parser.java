package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * MP3 parser: ID3v2.3/2.4 tag frames (Title/Artist/Album/Track/Lyrics/...) and
 * the MPEG audio frame header (layer/bitrate/sample rate/channel mode).
 */
public final class Mp3Parser {

    private static final Map<String, String> ID3_FRAMES = Map.ofEntries(
        Map.entry("TIT2", "Title"), Map.entry("TALB", "Album"), Map.entry("TPE1", "Artist"),
        Map.entry("TCOM", "Composer"), Map.entry("TCOP", "Copyright"),
        Map.entry("TRCK", "Track"), Map.entry("TYER", "Year"), Map.entry("TDRC", "Year"),
        Map.entry("TPOS", "PartOfSet"), Map.entry("TCON", "Genre"),
        Map.entry("COMM", "Comment"), Map.entry("TENC", "EncodedBy"),
        Map.entry("TPE2", "AlbumArtist"), Map.entry("USLT", "Lyrics"),
        Map.entry("TDAT", "Date"), Map.entry("TIT1", "Grouping"),
        // ID3v2.2 three-letter frame IDs
        Map.entry("TT2", "Title"), Map.entry("TAL", "Album"), Map.entry("TP1", "Artist"),
        Map.entry("TCM", "Composer"), Map.entry("TCR", "Copyright"),
        Map.entry("TRK", "Track"), Map.entry("TYE", "Year"), Map.entry("TPA", "PartOfSet"),
        Map.entry("TCO", "Genre"), Map.entry("COM", "Comment"), Map.entry("TEN", "EncodedBy"),
        Map.entry("TP2", "AlbumArtist"), Map.entry("ULT", "Lyrics"));

    private Mp3Parser() {}

    public static boolean isMp3(byte[] data) {
        if (data.length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        return data.length >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xe0) == 0xe0;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "MP3", 1, "File", "File");
        et.foundTag("MIMEType", "audio/mpeg", 1, "File", "File");
        int pos = 0;
        if (data.length >= 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            pos = parseId3v2(et, data);
        }
        parseMpegFrame(et, data, pos);
    }

    /** ID3v2 tag: "ID3" + version(2) + flags(1) + syncsafe size(4) + frames. */
    private static int parseId3v2(ExifTool et, byte[] data) {
        int major = data[3] & 0xff;
        int flags = data[5] & 0xff;
        long size = syncsafe(data, 6);
        et.foundTag("ID3Version", "2." + major + ".0", 1, "ID3", "ID3");
        et.foundTag("ID3Size", String.valueOf(size), 1, "ID3", "ID3");
        int pos = 10;
        long end = 10 + size;
        if (end > data.length) {
            end = data.length;
        }
        // skip extended header if flag set (v2.3: 4-byte size)
        if ((flags & 0x40) != 0 && major == 3 && pos + 4 <= end) {
            long extSize = Binary.get32u(data, pos, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            pos += 4 + extSize;
        }
        int idLen = major == 2 ? 3 : 4;
        int sizeLen = major == 2 ? 3 : 4;
        while (pos + idLen + sizeLen <= end) {
            String id = new String(data, pos, idLen, StandardCharsets.ISO_8859_1);
            if (id.charAt(0) == 0) {
                break;
            }
            long frameSize;
            int hdrLen;
            if (major == 2) {
                frameSize = ((data[pos + 3] & 0xff) << 16) | ((data[pos + 4] & 0xff) << 8)
                    | (data[pos + 5] & 0xff);
                hdrLen = 6;
            } else {
                frameSize = major == 4 ? syncsafe(data, pos + 4)
                    : (Binary.get32u(data, pos + 4, ByteOrder.BIG_ENDIAN) & 0xffffffffL);
                hdrLen = 10;
            }
            if (frameSize <= 0 || pos + hdrLen + frameSize > end) {
                break;
            }
            int textStart = pos + hdrLen;
            if (id.startsWith("T") || "COMM".equals(id) || "COM".equals(id)
                || "USLT".equals(id) || "ULT".equals(id)) {
                String value = decodeTextFrame(data, textStart, (int) frameSize, major);
                String tag = "COMM".equals(id) || "COM".equals(id) ? "Comment" : ID3_FRAMES.get(id);
                if (tag != null && value != null && !value.isEmpty()) {
                    et.foundTag(tag, value, 1, "ID3", "ID3");
                }
            }
            pos += hdrLen + frameSize;
        }
        return (int) end;
    }

    private static long syncsafe(byte[] data, int pos) {
        return ((data[pos] & 0x7fL) << 21) | ((data[pos + 1] & 0x7fL) << 14)
            | ((data[pos + 2] & 0x7fL) << 7) | (data[pos + 3] & 0x7fL);
    }

    /** Text frames: encoding byte + text (ISO-8859-1 or UTF-16). */
    private static String decodeTextFrame(byte[] data, int pos, int size, int major) {
        if (size < 1) {
            return null;
        }
        int enc = data[pos] & 0xff;
        int textStart = pos + 1;
        int textEnd = pos + size;
        String frameId = new String(data, pos - (major == 2 ? 6 : 10), major == 2 ? 3 : 4,
            StandardCharsets.ISO_8859_1);
        if ("COMM".equals(frameId) || "COM".equals(frameId)
            || "USLT".equals(frameId) || "ULT".equals(frameId)) {
            // language(3) + short description\0 + text
            textStart += 3;
            while (textStart < textEnd && data[textStart] != 0) {
                textStart += enc == 1 ? 2 : 1;
            }
            textStart += enc == 1 ? 2 : 1;
        }
        if (textStart >= textEnd) {
            return null;
        }
        if (enc == 1) {
            // UTF-16 with BOM
            String s = new String(data, textStart, textEnd - textStart, java.nio.charset.StandardCharsets.UTF_16);
            return s.replace("\u0000", "").trim();
        }
        return new String(data, textStart, textEnd - textStart, StandardCharsets.ISO_8859_1)
            .replace("\u0000", "").trim();
    }

    /** MPEG audio frame header (first 4 bytes after ID3). */
    private static void parseMpegFrame(ExifTool et, byte[] data, int start) {
        for (int i = start; i + 4 <= data.length && i < start + 1024; i++) {
            if ((data[i] & 0xff) == 0xff && (data[i + 1] & 0xe0) == 0xe0) {
                int b1 = data[i + 1] & 0xff;
                int b2 = data[i + 2] & 0xff;
                int b3 = data[i + 3] & 0xff;
                int versionBits = (b1 >> 3) & 0x3;
                int layerBits = (b1 >> 1) & 0x3;
                int bitrateIdx = (b2 >> 4) & 0xf;
                int sampleIdx = (b2 >> 2) & 0x3;
                int channelBits = (b3 >> 6) & 0x3;
                if (versionBits == 1 || layerBits == 0 || bitrateIdx == 0 || bitrateIdx == 15
                    || sampleIdx == 3) {
                    return;
                }
                et.foundTag("MPEGAudioVersion", String.valueOf(versionBits == 3 ? 1 : 2), 1, "MPEG", "MPEG");
                et.foundTag("AudioLayer", String.valueOf(4 - layerBits), 1, "MPEG", "MPEG");
                int[] bitrates = {32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
                if (bitrateIdx <= 14) {
                    et.foundTag("AudioBitrate", bitrates[bitrateIdx - 1] + " kbps", 1, "MPEG", "MPEG");
                }
                int[] rates = {44100, 48000, 32000};
                et.foundTag("SampleRate", String.valueOf(rates[sampleIdx]), 1, "MPEG", "MPEG");
                et.foundTag("ChannelMode", switch (channelBits) {
                    case 0 -> "Stereo";
                    case 1 -> "Joint Stereo";
                    case 2 -> "Dual Channel";
                    default -> "Single Channel";
                }, 1, "MPEG", "MPEG");
                return;
            }
        }
    }
}
