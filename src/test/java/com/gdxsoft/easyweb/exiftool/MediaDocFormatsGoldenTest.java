package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for FLAC, AAC, Matroska (MKV), ASF/WMV and OOXML (DOCX).
 * Reference values from {@code exiftool -json} (ExifTool 13.59).
 */
class MediaDocFormatsGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = MediaDocFormatsGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void flacMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/FLAC.flac");

        assertEquals("FLAC", info.get("FileType"));
        assertEquals("audio/flac", info.get("MIMEType"));
        assertEquals("4608", info.get("BlockSizeMin"));
        assertEquals("8000", info.get("SampleRate"));
        assertEquals("2", info.get("Channels"));
        assertEquals("8", info.get("BitsPerSample"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", info.get("MD5Signature"));
        assertEquals("reference libFLAC 1.1.2 20050205", info.get("Vendor"));
    }

    @Test
    void aacMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/AAC.aac");

        assertEquals("AAC", info.get("FileType"));
        assertEquals("audio/aac", info.get("MIMEType"));
        assertEquals("Low Complexity", info.get("ProfileType"));
        assertEquals("44100", info.get("SampleRate"));
        assertEquals("2", info.get("Channels"));
    }

    @Test
    void mkvMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/Matroska.mkv");

        assertEquals("MKV", info.get("FileType"));
        assertEquals("video/x-matroska", info.get("MIMEType"));
        assertEquals("matroska", info.get("DocType"));
        assertEquals("1", info.get("DocTypeVersion"));
        assertEquals("1 ms", info.get("TimecodeScale"));
        assertEquals("libebml v0.7.8 + libmatroska v0.8.1", info.get("MuxingApp"));
        assertEquals("V_MPEG4/ISO/AVC", info.get("VideoCodecID"));
        assertEquals("704", info.get("ImageWidth"));
        assertEquals("576", info.get("ImageHeight"));
        assertEquals("2010:02:03 21:17:48Z", info.get("DateTimeOriginal"));
    }

    @Test
    void asfMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/ASF.wmv");

        assertEquals("WMV", info.get("FileType"));
        assertEquals("video/x-ms-wmv", info.get("MIMEType"));
        assertEquals("414891", info.get("FileSize"));
        assertEquals("2004:10:28 17:23:34Z", info.get("CreationDate"));
    }

    @Test
    void ooxmlMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/OOXML.docx");

        assertEquals("DOCX", info.get("FileType"));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            info.get("MIMEType"));
        assertEquals("The document title", info.get("Title"));
        assertEquals("Author: Jeff", info.get("Creator"));
        assertEquals("Jeff", info.get("LastModifiedBy"));
        assertEquals("2009:10:24 01:41:00Z", info.get("CreateDate"));
        assertEquals("1", info.get("Pages"));
        assertEquals("7", info.get("Words"));
        assertEquals("Normal", info.get("Template"));
    }
}
