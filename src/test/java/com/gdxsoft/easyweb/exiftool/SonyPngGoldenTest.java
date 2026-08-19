package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for Sony (DSC-F828, PrintIM only) and PNG (file-level tags).
 * Reference values generated with {@code exiftool -json t/images/Sony.jpg}
 * and {@code exiftool -json t/images/PNG.png} (ExifTool 13.59).
 */
class SonyPngGoldenTest {

    @Test
    void sonyMatchesExifToolReference() throws IOException {
        try (InputStream in = SonyPngGoldenTest.class.getResourceAsStream("/Sony.jpg")) {
            assertNotNull(in, "Sony.jpg resource missing");
            Map<String, Object> info = new ExifTool().imageInfo(in.readAllBytes());

            assertEquals("SONY", info.get("Make"));
            assertEquals("DSC-F828", info.get("Model"));
            assertEquals("1/250", info.get("ExposureTime"));
            assertEquals("7.1", info.get("FNumber"));
            assertEquals("Multi-segment", info.get("MeteringMode"));
            assertEquals("12.5 mm", info.get("FocalLength"));
            assertEquals("sRGB", info.get("ColorSpace"));
            assertEquals("Auto bracket", info.get("ExposureMode"));
            assertEquals("0250", info.get("PrintIMVersion"));
            assertEquals("JPEG (old-style)", info.get("Compression"));
            assertEquals("8", info.get("ImageWidth"));
            assertEquals("8", info.get("ImageHeight"));
        }
    }

    @Test
    void pngMatchesExifToolReference() throws IOException {
        try (InputStream in = SonyPngGoldenTest.class.getResourceAsStream("/PNG.png")) {
            assertNotNull(in, "PNG.png resource missing");
            Map<String, Object> info = new ExifTool().imageInfo(in.readAllBytes());

            assertEquals("PNG", info.get("FileType"));
            assertEquals("image/png", info.get("MIMEType"));
            assertEquals("16", info.get("ImageWidth"));
            assertEquals("16", info.get("ImageHeight"));
            assertEquals("1", info.get("BitDepth"));
            assertEquals("Grayscale", info.get("ColorType"));
            assertEquals("Deflate/Inflate", info.get("Compression"));
            assertEquals("Adaptive", info.get("Filter"));
            assertEquals("Noninterlaced", info.get("Interlace"));
        }
    }
}
