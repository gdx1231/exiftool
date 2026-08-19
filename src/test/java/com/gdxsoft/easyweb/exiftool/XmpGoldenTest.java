package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for XMP parsing (ExtendedXMP.jpg: standard XMP with
 * HasExtendedXMP plus a split extended-XMP segment).
 * Reference values generated with {@code exiftool -json t/images/ExtendedXMP.jpg}
 * (ExifTool 13.59).
 */
class XmpGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = XmpGoldenTest.class.getResourceAsStream("/ExtendedXMP.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("JPEG", info.get("FileType"));
        assertEquals("Image::ExifTool 7.50", info.get("XMPToolkit"));
        assertEquals("04B9E48040A30A6308713BD1E4223B41", info.get("HasExtendedXMP"));
        assertEquals("PhilToo", info.get("Author"));
        assertEquals("Guess Who", info.get("Creator"));
        assertEquals("2008:10:20 19:54:15", info.get("CreationDate"));
        assertEquals("2008:10:20 19:54:15", info.get("ModDate"));
        assertEquals("Just ExifTool again", info.get("Producer"));
        assertEquals("PDF Title", info.get("Title"));
    }
}
