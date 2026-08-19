package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for Casio maker notes (Casio.jpg, QV-3000EX).
 * Reference values generated with {@code exiftool -json t/images/Casio.jpg}
 * (ExifTool 13.59).
 */
class CasioGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = CasioGoldenTest.class.getResourceAsStream("/Casio.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("CASIO", info.get("Make"));
        assertEquals("QV-3000EX", info.get("Model"));

        assertEquals("Single Shutter", info.get("RecordingMode"));
        assertEquals("Fine", info.get("Quality"));
        assertEquals("Auto", info.get("FocusMode"));
        assertEquals("Red-eye Reduction", info.get("FlashMode"));
        assertEquals("Normal", info.get("FlashIntensity"));
        assertEquals("2.5 m", info.get("ObjectDistance"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("Off", info.get("DigitalZoom"));
        assertEquals("Normal", info.get("Sharpness"));
        assertEquals("Normal", info.get("Contrast"));
        assertEquals("Normal", info.get("Saturation"));
        assertEquals("64", info.get("ISO"));
    }
}
