package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for MP3 (ID3v2.2 + MPEG frame), WAV/AVI (RIFF) and PDF.
 * Reference values from {@code exiftool -json} (ExifTool 13.59).
 */
class AudioVideoDocGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = AudioVideoDocGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void mp3MatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/MP3.mp3");

        assertEquals("MP3", info.get("FileType"));
        assertEquals("audio/mpeg", info.get("MIMEType"));
        assertEquals("2.2.0", info.get("ID3Version"));
        assertEquals("ExifTool Test", info.get("Title"));
        assertEquals("Phil Harvey", info.get("Artist"));
        assertEquals("A Composer", info.get("Composer"));
        assertEquals("Phil's Greatest Hits", info.get("Album"));
        assertEquals("1/5", info.get("Track"));
        assertEquals("1/2", info.get("PartOfSet"));
        assertEquals("2005", info.get("Year"));
        assertEquals("Testing", info.get("Genre"));
        assertEquals("My Comments", info.get("Comment"));
        assertEquals("Do-wap she-bang", info.get("Lyrics"));
        assertEquals("1", info.get("MPEGAudioVersion"));
        assertEquals("3", info.get("AudioLayer"));
        assertEquals("128 kbps", info.get("AudioBitrate"));
        assertEquals("44100", info.get("SampleRate"));
        assertEquals("Joint Stereo", info.get("ChannelMode"));
    }

    @Test
    void wavMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/RIFF.wav");

        assertEquals("WAV", info.get("FileType"));
        assertEquals("audio/x-wav", info.get("MIMEType"));
        assertEquals("Microsoft PCM", info.get("Encoding"));
        assertEquals("1", info.get("NumChannels"));
        assertEquals("7872", info.get("SampleRate"));
        assertEquals("8", info.get("BitsPerSample"));
    }

    @Test
    void aviMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/Pentax.avi");

        assertEquals("AVI", info.get("FileType"));
        assertEquals("video/x-msvideo", info.get("MIMEType"));
        assertEquals("Video", info.get("StreamType"));
        assertEquals("1280", info.get("ImageWidth"));
        assertEquals("720", info.get("ImageHeight"));
        assertEquals("24.00", info.get("FrameRate"));
    }

    @Test
    void pdfMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/PDF.pdf");

        assertEquals("PDF", info.get("FileType"));
        assertEquals("application/pdf", info.get("MIMEType"));
        assertEquals("1.3", info.get("PDFVersion"));
        assertEquals("1", info.get("PageCount"));
        assertEquals("Adobe Photoshop 7.0", info.get("Creator"));
        assertEquals("Adobe Photoshop for Macintosh", info.get("Producer"));
        assertEquals("2005:07:18 14:30:45-04:00", info.get("CreationDate"));
        assertEquals("2005:07:18 14:30:45-04:00", info.get("ModDate"));
    }
}
