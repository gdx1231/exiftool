package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for embedded EXIF reading in PNG (eXIf chunk) and HEIC
 * ("Exif" item in the mdat box). Test files were generated with exiftool
 * (ExifTool 13.59) from the plain PNG/HEIC fixtures.
 */
class EmbeddedExifTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = EmbeddedExifTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void pngEmbeddedExif() throws IOException {
        Map<String, Object> info = info("/png_exif.png");

        assertEquals("PNG", info.get("FileType"));
        assertEquals("TestMaker", info.get("Make"));
        assertEquals("TestArtist", info.get("Artist"));
        assertEquals("Test Desc", info.get("ImageDescription"));
        // IHDR file-level tags still present
        assertEquals("16", info.get("ImageWidth"));
        assertEquals("16", info.get("ImageHeight"));
        assertEquals("Grayscale", info.get("ColorType"));
    }

    @Test
    void heicEmbeddedExif() throws IOException {
        Map<String, Object> info = info("/heic_exif.heic");

        assertEquals("HEIF", info.get("FileType"));
        assertEquals("TestMaker", info.get("Make"));
        assertEquals("TestArtist", info.get("Artist"));
        // ISO BMFF box-level tags still present
        assertEquals("High Efficiency Image Format still image (.HEIF)", info.get("MajorBrand"));
        assertEquals("1596", info.get("ImageWidth"));
        assertEquals("1064", info.get("ImageHeight"));
        assertEquals("Picture", info.get("HandlerType"));
    }
}
