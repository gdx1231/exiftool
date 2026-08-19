package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for FujiFilm maker notes (FujiFilm.jpg, FinePix2400Zoom).
 * Reference values generated with {@code exiftool -json t/images/FujiFilm.jpg}
 * (ExifTool 13.59).
 */
class FujiFilmGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = FujiFilmGoldenTest.class.getResourceAsStream("/FujiFilm.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("FUJIFILM", info.get("Make"));
        assertEquals("FinePix2400Zoom", info.get("Model"));
        assertEquals("3.5", info.get("FNumber"));
        assertEquals("100", info.get("ISO"));
        assertEquals("Multi-segment", info.get("MeteringMode"));
        assertEquals("Fired", info.get("Flash"));
        assertEquals("6.0 mm", info.get("FocalLength"));

        // FujiFilm maker notes
        assertEquals("0130", info.get("Version"));
        assertEquals("NORMAL ", info.get("Quality"));
        assertEquals("0 (normal)", info.get("Sharpness"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("Red-eye reduction", info.get("FujiFlashMode"));
        assertEquals("0", info.get("FlashExposureComp"));
        assertEquals("Off", info.get("Macro"));
        assertEquals("Auto", info.get("FocusMode"));
        assertEquals("Off", info.get("SlowSync"));
        assertEquals("Auto", info.get("PictureMode"));
        assertEquals("Off", info.get("AutoBracketing"));
        assertEquals("None", info.get("BlurWarning"));
        assertEquals("Good", info.get("FocusWarning"));
        assertEquals("Good", info.get("ExposureWarning"));
    }
}
