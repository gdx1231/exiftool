package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * AAC (ADTS) parser: frame header gives the profile, sample rate and channels.
 */
public final class AacParser {

    private AacParser() {}

    public static boolean isAac(byte[] data) {
        return data.length >= 7 && (data[0] & 0xff) == 0xff && (data[1] & 0xf6) == 0xf0;
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "AAC", 1, "File", "File");
        et.foundTag("MIMEType", "audio/aac", 1, "File", "File");
        int b2 = data[2] & 0xff;
        int b3 = data[3] & 0xff;
        int profile = (b2 >> 6) & 0x3;
        int sampleIdx = (b2 >> 2) & 0xf;
        int channels = ((b2 & 0x1) << 2) | (b3 >> 6);
        int[] rates = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000, 7350};
        if (sampleIdx < rates.length) {
            et.foundTag("SampleRate", String.valueOf(rates[sampleIdx]), 1, "AAC", "AAC");
        }
        et.foundTag("ProfileType", switch (profile) {
            case 1 -> "Low Complexity";
            case 2 -> "Main";
            case 3 -> "Scalable Sample Rate";
            default -> "Reserved";
        }, 1, "AAC", "AAC");
        et.foundTag("Channels", String.valueOf(channels), 1, "AAC", "AAC");
    }
}
