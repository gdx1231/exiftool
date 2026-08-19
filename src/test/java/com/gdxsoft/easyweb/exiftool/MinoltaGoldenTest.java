package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for Minolta maker notes (Minolta.jpg, DiMAGE 7i).
 * Reference values generated with {@code exiftool -json t/images/Minolta.jpg}
 * (ExifTool 13.59). CameraSettings tags have PRIORITY 0, so tags that also
 * exist in EXIF (ExposureMode, ISO, ExposureTime, FNumber, ...) are overridden.
 */
class MinoltaGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = MinoltaGoldenTest.class.getResourceAsStream("/Minolta.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        assertEquals("Minolta Co., Ltd.", info.get("Make"));
        assertEquals("DiMAGE 7i", info.get("Model"));

        // Minolta maker notes
        assertEquals("MLT0", info.get("MakerNoteVersion"));
        assertEquals("Fill flash", info.get("FlashMode"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("Full", info.get("MinoltaImageSize"));
        assertEquals("Fine", info.get("MinoltaQuality"));
        assertEquals("Single", info.get("DriveMode"));
        assertEquals("Off", info.get("MacroMode"));
        assertEquals("Off", info.get("DigitalZoom"));
        assertEquals("1/3 EV", info.get("BracketStep"));
        assertEquals("0", info.get("IntervalLength"));
        assertEquals("2", info.get("IntervalNumber"));
        assertEquals("2 m", info.get("FocusDistance"));
        assertEquals("No", info.get("FlashFired"));
        assertEquals("2002:06:01", info.get("MinoltaDate"));
        assertEquals("12:37:27", info.get("MinoltaTime"));
        assertEquals("3.4", info.get("MaxAperture"));
        assertEquals("On", info.get("FileNumberMemory"));
        assertEquals("32", info.get("LastFileNumber"));
        assertEquals("1.49609375", info.get("ColorBalanceRed"));
        assertEquals("1", info.get("ColorBalanceGreen"));
        assertEquals("1.375", info.get("ColorBalanceBlue"));
        assertEquals("None", info.get("SubjectProgram"));
        assertEquals("0", info.get("FlashExposureComp"));
        assertEquals("100", info.get("ISOSetting"));
        assertEquals("DiMAGE 7i", info.get("MinoltaModelID"));
        assertEquals("Still Image", info.get("IntervalMode"));
        assertEquals("Standard Form", info.get("FolderName"));
        assertEquals("Natural color", info.get("ColorMode"));
        assertEquals("0", info.get("ColorFilter"));
        assertEquals("0", info.get("BWFilter"));
        assertEquals("No", info.get("InternalFlash"));
        assertEquals("7.5", info.get("Brightness"));
        assertEquals("1280", info.get("SpotFocusPointX"));
        assertEquals("960", info.get("SpotFocusPointY"));
        assertEquals("Left zone", info.get("WideFocusZone"));
        assertEquals("AF", info.get("FocusMode"));
        assertEquals("Wide Focus (normal)", info.get("FocusArea"));
        assertEquals("Exposure", info.get("DECPosition"));

        // Minolta::Main top-level tags
        assertEquals("2396300", info.get("CompressedImageSize"));
        assertEquals("13042", info.get("PreviewImageStart"));
        assertEquals("26", info.get("PreviewImageLength"));

        // EXIF (unchanged by MakerNote parsing)
        assertEquals("1/180", info.get("ExposureTime"));
        assertEquals("5.6", info.get("FNumber"));
        assertEquals("100", info.get("ISO"));
    }
}
