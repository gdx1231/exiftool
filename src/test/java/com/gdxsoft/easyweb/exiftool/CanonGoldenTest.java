package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for Canon maker notes (Canon.jpg, EOS DIGITAL REBEL / 300D).
 * Reference values generated with {@code exiftool -json t/images/Canon.jpg}
 * (ExifTool 13.59). CanonShotInfo sub-directory is Phase 3 scope.
 */
class CanonGoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = CanonGoldenTest.class.getResourceAsStream("/Canon.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        // IFD0 / ExifIFD
        assertEquals("Canon", info.get("Make"));
        assertEquals("Canon EOS DIGITAL REBEL", info.get("Model"));
        assertEquals("Horizontal (normal)", info.get("Orientation"));
        assertEquals("180", info.get("XResolution"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("2003:12:04 06:46:52", info.get("ModifyDate"));
        assertEquals("4", info.get("ExposureTime"));
        assertEquals("14.0", info.get("FNumber"));
        assertEquals("100", info.get("ISO"));
        assertEquals("0221", info.get("ExifVersion"));
        assertEquals("Y, Cb, Cr, -", info.get("ComponentsConfiguration"));
        assertEquals("9", info.get("CompressedBitsPerPixel"));
        assertEquals("0", info.get("ExposureCompensation"));
        assertEquals("4.5", info.get("MaxApertureValue"));
        assertEquals("Center-weighted average", info.get("MeteringMode"));
        assertEquals("No Flash", info.get("Flash"));
        assertEquals("34.0 mm", info.get("FocalLength"));
        assertEquals("3443.946188", info.get("FocalPlaneXResolution"));
        assertEquals("3442.016807", info.get("FocalPlaneYResolution"));
        assertEquals("inches", info.get("FocalPlaneResolutionUnit"));
        assertEquals("One-chip color area", info.get("SensingMethod"));
        assertEquals("Digital Camera", info.get("FileSource"));
        assertEquals("Normal", info.get("CustomRendered"));
        assertEquals("Manual", info.get("ExposureMode"));
        assertEquals("Auto", info.get("WhiteBalance"));

        // ---- Canon maker notes: CameraSettings (0x0001) ----
        assertEquals("Unknown (0)", info.get("MacroMode"));
        assertEquals("Off", info.get("SelfTimer"));
        assertEquals("RAW", info.get("Quality"));
        assertEquals("Off", info.get("CanonFlashMode"));
        assertEquals("Continuous", info.get("ContinuousDrive"));
        assertEquals("Manual Focus (3)", info.get("FocusMode"));
        assertEquals("CRW+THM", info.get("RecordMode"));
        assertEquals("Large", info.get("CanonImageSize"));
        assertEquals("Manual", info.get("EasyMode"));
        assertEquals("Unknown (-1)", info.get("DigitalZoom"));
        assertEquals("+1", info.get("Contrast"));
        assertEquals("+1", info.get("Saturation"));
        assertEquals("+1", info.get("Sharpness"));
        assertEquals("n/a", info.get("CameraISO"));
        assertEquals("Center-weighted average", info.get("MeteringMode"));
        assertEquals("Not Known", info.get("FocusRange"));
        assertEquals("Manual", info.get("CanonExposureMode"));
        assertEquals("n/a", info.get("LensType"));
        assertEquals("55 mm", info.get("MaxFocalLength"));
        assertEquals("18 mm", info.get("MinFocalLength"));
        assertEquals("1/mm", info.get("FocalUnits"));
        assertEquals("4", info.get("MaxAperture"));
        assertEquals("27", info.get("MinAperture"));
        assertEquals("n/a", info.get("FlashModel"));
        assertEquals("(none)", info.get("FlashBits"));
        assertEquals("3072", info.get("ZoomSourceWidth"));
        assertEquals("3072", info.get("ZoomTargetWidth"));
        assertEquals("n/a", info.get("ManualFlashOutput"));
        assertEquals("0", info.get("ColorTone"));
        // SRAWQuality (entry 46) lies outside the 92-byte data block: not output

        // ---- CanonFocalLength (0x0002) ----
        assertEquals("23.22 mm", info.get("FocalPlaneXSize"));
        assertEquals("15.49 mm", info.get("FocalPlaneYSize"));

        // ---- Canon::Main top-level tags ----
        assertEquals("CRW:EOS DIGITAL REBEL CMOS RAW", info.get("CanonImageType"));
        assertEquals("Firmware Version 1.1.1", info.get("CanonFirmwareVersion"));
        assertEquals("0560018150", info.get("SerialNumber"));
        assertEquals("Format 1", info.get("SerialNumberFormat"));
        assertEquals("118-1861", info.get("FileNumber"));
        assertEquals("Phil Harvey", info.get("OwnerName"));
        assertEquals("EOS Digital Rebel / 300D / Kiss Digital", info.get("CanonModelID"));
        assertEquals("4480822", info.get("CanonFileLength"));
        assertEquals("0 159 7 112", info.get("ThumbnailImageValidArea"));

        // ---- CanonShotInfo (0x0004) ----
        assertEquals("100", info.get("AutoISO"));
        assertEquals("100", info.get("BaseISO"));
        assertEquals("-1.25", info.get("MeasuredEV"));
        assertEquals("14", info.get("TargetAperture"));
        assertEquals("0", info.get("ExposureCompensation"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("None", info.get("SlowShutter"));
        assertEquals("0", info.get("SequenceNumber"));
        assertEquals("n/a", info.get("OpticalZoomCode"));
        assertEquals("0", info.get("FlashGuideNumber"));
        assertEquals("0", info.get("FlashExposureComp"));
        assertEquals("Off", info.get("AutoExposureBracketing"));
        assertEquals("0", info.get("AEBBracketValue"));
        assertEquals("Camera Local Control", info.get("ControlMode"));
        assertEquals("inf", info.get("FocusDistanceUpper"));
        assertEquals("5.46 m", info.get("FocusDistanceLower"));
        assertEquals("-1.25", info.get("MeasuredEV2"));
        assertEquals("4", info.get("BulbDuration"));
        assertEquals("EOS Mid-range", info.get("CameraType"));
        assertEquals("None", info.get("AutoRotate"));
        assertEquals("n/a", info.get("NDFilter"));
        assertEquals("0", info.get("SelfTimer2"));

        // ---- CanonFileInfo (0x0093) ----
        assertEquals("Off", info.get("BracketMode"));
        assertEquals("0", info.get("BracketValue"));
        assertEquals("0", info.get("BracketShotNumber"));

        // ---- InteropIFD / IFD1 ----
        assertEquals("THM - DCF thumbnail file", info.get("InteropIndex"));
        assertEquals("3072", info.get("RelatedImageWidth"));
        assertEquals("2048", info.get("RelatedImageLength"));
    }

    @Test
    void makerNoteTagsPresent() throws IOException {
        Map<String, Object> info = info();
        assertEquals("RAW", info.get("Quality"));
        assertEquals("EOS Digital Rebel / 300D / Kiss Digital", info.get("CanonModelID"));
        assertEquals("118-1861", info.get("FileNumber"));
    }
}
