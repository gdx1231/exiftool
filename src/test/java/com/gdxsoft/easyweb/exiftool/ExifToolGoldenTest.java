package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Golden test: extracted EXIF values must match the output of the reference
 * implementation ({@code exiftool -json}) for the same image.
 *
 * <p>Reference values below were generated with:
 * {@code exiftool -json t/images/Motorola.jpg} (ExifTool 13.59).
 * MakerNote-derived tags (BuildNumber, SerialNumber, Sensor, ManufactureDate)
 * are Phase 2 scope and not expected yet.
 */
class ExifToolGoldenTest {

    private static final Set<String> PHASE2_ONLY = Set.of(
        "BuildNumber", "SerialNumber", "Sensor", "ManufactureDate");

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = ExifToolGoldenTest.class.getResourceAsStream("/Motorola.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("Motorola", info.get("Make"));
        assertEquals("XT1575", info.get("Model"));
        assertEquals("72", info.get("XResolution"));
        assertEquals("72", info.get("YResolution"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("clark_retus-user 5.1.1 LPH23.116-18 18 release-keys", info.get("Software"));
        assertEquals("2015:09:18 11:23:37", info.get("ModifyDate"));
        assertEquals("Centered", info.get("YCbCrPositioning"));

        // ExifIFD
        assertEquals("1/1961", info.get("ExposureTime"));
        assertEquals("2.0", info.get("FNumber"));
        assertEquals("Program AE", info.get("ExposureProgram"));
        assertEquals("50", info.get("ISO"));
        assertEquals("0220", info.get("ExifVersion"));
        assertEquals("2015:09:18 11:23:37", info.get("DateTimeOriginal"));
        assertEquals("2015:09:18 11:23:37", info.get("CreateDate"));
        assertEquals("Y, Cb, Cr, -", info.get("ComponentsConfiguration"));
        assertEquals("1/1924", info.get("ShutterSpeedValue"));
        assertEquals("2.0", info.get("ApertureValue"));
        assertEquals("-1", info.get("BrightnessValue"));
        assertEquals("0", info.get("ExposureCompensation"));
        assertEquals("2.0", info.get("MaxApertureValue"));
        assertEquals("Average", info.get("MeteringMode"));
        assertEquals("Off, Did not fire", info.get("Flash"));
        assertEquals("4.7 mm", info.get("FocalLength"));
        assertEquals("0100", info.get("FlashpixVersion"));
        assertEquals("sRGB", info.get("ColorSpace"));
        assertEquals("5344", info.get("ExifImageWidth"));
        assertEquals("4008", info.get("ExifImageHeight"));

        // InteropIFD
        assertEquals("R98 - DCF basic file (sRGB)", info.get("InteropIndex"));
        assertEquals("0100", info.get("InteropVersion"));

        // Misc ExifIFD
        assertEquals("Directly photographed", info.get("SceneType"));
        assertEquals("Normal", info.get("CustomRendered"));
        assertEquals("Auto", info.get("ExposureMode"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("1", info.get("DigitalZoomRatio"));
        assertEquals("Standard", info.get("SceneCaptureType"));
        assertEquals("Normal", info.get("Contrast"));
        assertEquals("Low", info.get("Saturation"));
        assertEquals("Soft", info.get("Sharpness"));

        // GPS
        assertEquals("2.2.0.0", info.get("GPSVersionID"));
        assertEquals("WGS-84", info.get("GPSMapDatum"));

        // IFD1 (thumbnail)
        assertEquals("JPEG (old-style)", info.get("Compression"));
        assertEquals("2212", info.get("ThumbnailOffset"));
        assertEquals("28", info.get("ThumbnailLength"));
    }

    @Test
    void noUnknownTagsLeak() throws IOException {
        Map<String, Object> info = info();
        // every extracted tag must be one we modeled (or a known Phase 2 placeholder)
        Set<String> allowed = Set.of(
            "FileType", "MIMEType", "ExifByteOrder", "ImageWidth", "ImageHeight",
            "BitsPerSample", "ColorComponents", "EncodingProcess",
            "Make", "Model", "XResolution", "YResolution", "ResolutionUnit",
            "Software", "ModifyDate", "YCbCrPositioning", "ExposureTime", "FNumber",
            "ExposureProgram", "ISO", "ExifVersion", "DateTimeOriginal", "CreateDate",
            "ComponentsConfiguration", "ShutterSpeedValue", "ApertureValue",
            "BrightnessValue", "ExposureCompensation", "MaxApertureValue",
            "MeteringMode", "Flash", "FocalLength", "FlashpixVersion", "ColorSpace",
            "ExifImageWidth", "ExifImageHeight", "InteropIndex", "InteropVersion",
            "SceneType", "CustomRendered", "ExposureMode", "WhiteBalance",
            "DigitalZoomRatio", "SceneCaptureType", "Contrast", "Saturation",
            "Sharpness", "GPSVersionID", "GPSMapDatum", "Compression",
            "ThumbnailOffset", "ThumbnailLength");
        for (String tag : info.keySet()) {
            assertTrue(allowed.contains(tag), "unexpected tag extracted: " + tag);
        }
    }

    @Test
    void repeatedCallIsIdempotent() throws IOException {
        ExifTool et = new ExifTool();
        try (InputStream in = ExifToolGoldenTest.class.getResourceAsStream("/Motorola.jpg")) {
            byte[] data = in.readAllBytes();
            Map<String, Object> first = et.imageInfo(data);
            Map<String, Object> second = et.imageInfo(data);
            assertEquals(first, second, "imageInfo must be re-runnable");
            assertFalse(first.isEmpty());
        }
    }
}
