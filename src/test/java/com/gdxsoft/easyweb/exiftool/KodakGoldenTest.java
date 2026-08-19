package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for Kodak type 1 maker notes (Kodak.jpg, DX4900).
 * Reference values generated with {@code exiftool -json t/images/Kodak.jpg}
 * (ExifTool 13.59).
 */
class KodakGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = KodakGoldenTest.class.getResourceAsStream("/Kodak.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("EASTMAN KODAK COMPANY", info.get("Make"));
        assertEquals("KODAK DX4900 ZOOM DIGITAL CAMERA", info.get("Model"));

        assertEquals("DX4900  ", info.get("KodakModel"));
        assertEquals("Fine", info.get("Quality"));
        assertEquals("Off", info.get("BurstMode"));
        assertEquals("2448", info.get("KodakImageWidth"));
        assertEquals("1632", info.get("KodakImageHeight"));
        assertEquals("2002", info.get("YearCreated"));
        assertEquals("05:01", info.get("MonthDayCreated"));
        assertEquals("10:22:28.62", info.get("TimeCreated"));
        assertEquals("Auto", info.get("ShutterMode"));
        assertEquals("Multi-segment", info.get("MeteringMode"));
        assertEquals("0", info.get("SequenceNumber"));
        assertEquals("6.73", info.get("FNumber"));
        assertEquals("1/216", info.get("ExposureTime"));
        assertEquals("0", info.get("ExposureCompensation"));
        assertEquals("Normal", info.get("FocusMode"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("Auto", info.get("FlashMode"));
        assertEquals("No", info.get("FlashFired"));
        assertEquals("Auto", info.get("ISOSetting"));
        assertEquals("108", info.get("ISO"));
        assertEquals("1.4", info.get("TotalZoom"));
        assertEquals("Off", info.get("DateTimeStamp"));
        assertEquals("Saturated Color", info.get("ColorMode"));
        assertEquals("1", info.get("DigitalZoom"));
        assertEquals("Normal", info.get("Sharpness"));
    }
}
