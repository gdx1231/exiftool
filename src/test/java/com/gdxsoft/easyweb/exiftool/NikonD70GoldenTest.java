package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden test for Nikon maker notes (NikonD70.jpg, Type 2/3 layout).
 * Reference values generated with {@code exiftool -json t/images/NikonD70.jpg}
 * (ExifTool 13.59). UserComment/CFAPattern decoding are not yet implemented
 * (Phase 2 scope).
 */
class NikonD70GoldenTest {

    private static Map<String, Object> info() throws IOException {
        try (InputStream in = NikonD70GoldenTest.class.getResourceAsStream("/NikonD70.jpg")) {
            assertNotNull(in, "test image resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void matchesExifToolReference() throws IOException {
        Map<String, Object> info = info();

        // IFD0
        assertEquals("NIKON CORPORATION", info.get("Make"));
        assertEquals("NIKON D70", info.get("Model"));
        assertEquals("Horizontal (normal)", info.get("Orientation"));
        assertEquals("300", info.get("XResolution"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("2005:01:14 08:57:59", info.get("ModifyDate"));
        assertEquals("Co-sited", info.get("YCbCrPositioning"));

        // File-level / JFIF / SOF tags
        assertEquals("JPEG", info.get("FileType"));
        assertEquals("image/jpeg", info.get("MIMEType"));
        assertEquals("1.01", info.get("JFIFVersion"));
        assertEquals("71", info.get("ImageWidth"));
        assertEquals("47", info.get("ImageHeight"));
        assertEquals("8", info.get("BitsPerSample"));
        assertEquals("3", info.get("ColorComponents"));
        assertEquals("Baseline DCT, Huffman coding", info.get("EncodingProcess"));

        // ExifIFD
        assertEquals("1/60", info.get("ExposureTime"));
        assertEquals("5.0", info.get("FNumber"));
        assertEquals("Aperture-priority AE", info.get("ExposureProgram"));
        assertEquals("0221", info.get("ExifVersion"));
        assertEquals("2005:01:14 08:57:59", info.get("DateTimeOriginal"));
        assertEquals("Y, Cb, Cr, -", info.get("ComponentsConfiguration"));
        assertEquals("4", info.get("CompressedBitsPerPixel"));
        assertEquals("0", info.get("ExposureCompensation"));
        assertEquals("4.4", info.get("MaxApertureValue"));
        assertEquals("Multi-segment", info.get("MeteringMode"));
        assertEquals("Auto, Fired, Return detected", info.get("Flash"));
        assertEquals("56.0 mm", info.get("FocalLength"));
        assertEquals("0100", info.get("FlashpixVersion"));
        assertEquals("Uncalibrated", info.get("ColorSpace"));
        assertEquals("3008", info.get("ExifImageWidth"));
        assertEquals("2000", info.get("ExifImageHeight"));
        assertEquals("Digital Camera", info.get("FileSource"));
        assertEquals("Directly photographed", info.get("SceneType"));
        assertEquals("Custom", info.get("CustomRendered"));
        assertEquals("Auto", info.get("ExposureMode"));
        assertEquals("1", info.get("DigitalZoomRatio"));
        assertEquals("84 mm", info.get("FocalLengthIn35mmFormat"));
        assertEquals("Standard", info.get("SceneCaptureType"));
        assertEquals("None", info.get("GainControl"));
        assertEquals("Normal", info.get("Contrast"));
        assertEquals("High", info.get("Saturation"));
        assertEquals("Hard", info.get("Sharpness"));
        assertEquals("Unknown", info.get("SubjectDistanceRange"));
        assertEquals("20", info.get("SubSecTime"));

        // InteropIFD
        assertEquals("R03 - DCF option file (Adobe RGB)", info.get("InteropIndex"));
        assertEquals("0100", info.get("InteropVersion"));

        // IFD1 (thumbnail)
        assertEquals("JPEG (old-style)", info.get("Compression"));
        assertEquals("2696", info.get("ThumbnailOffset"));
        assertEquals("28", info.get("ThumbnailLength"));

        // ---- Nikon maker notes (Type 2/3) ----
        assertEquals("2.10", info.get("MakerNoteVersion"));
        assertEquals("200", info.get("ISO")); // EXIF ISO wins over Nikon ISO (priority 0)
        assertEquals("Fine", info.get("Quality"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("AF-S", info.get("FocusMode"));
        assertEquals("Normal", info.get("FlashSetting"));
        assertEquals("Built-in,TTL", info.get("FlashType"));
        assertEquals("0", info.get("WhiteBalanceFineTune"));
        assertEquals("0", info.get("ProgramShift"));
        assertEquals("-4.9", info.get("ExposureDifference"));
        assertEquals("-5/3", info.get("FlashExposureComp"));
        assertEquals("200", info.get("ISOSetting"));
        assertEquals("0 0 3008 2000", info.get("ImageBoundary"));
        assertEquals("0", info.get("ExternalFlashExposureComp"));
        assertEquals("0.0", info.get("FlashExposureBracketValue"));
        assertEquals("0", info.get("ExposureBracketValue"));
        assertEquals("CS", info.get("ToneComp"));
        assertEquals("G", info.get("LensType"));
        assertEquals("18-70mm f/3.5-4.5", info.get("Lens"));
        assertEquals("Fired, TTL Mode", info.get("FlashMode"));
        assertEquals("Continuous", info.get("ShootingMode"));
        assertEquals("5.33", info.get("LensFStops"));
        assertEquals("Mode2", info.get("ColorHue"));
        assertEquals("0103", info.get("ShotInfoVersion"));
        assertEquals("0", info.get("HueAdjustment"));
        assertEquals("Off", info.get("NoiseReduction"));
        assertEquals("597 256 361 256", info.get("WB_RGBGLevels"));
        assertEquals("0101", info.get("LensDataVersion"));
        assertEquals("89.0 mm", info.get("ExitPupilPosition"));
        assertEquals("4.6", info.get("AFAperture"));
        assertEquals("0x21", info.get("FocusPosition"));
        assertEquals("0.63 m", info.get("FocusDistance"));
        assertEquals("127", info.get("LensIDNumber"));
        assertEquals("18.3 mm", info.get("MinFocalLength"));
        assertEquals("71.3 mm", info.get("MaxFocalLength"));
        assertEquals("3.6", info.get("MaxApertureAtMinFocal"));
        assertEquals("4.5", info.get("MaxApertureAtMaxFocal"));
        assertEquals("132", info.get("MCUVersion"));
        assertEquals("4.5", info.get("EffectiveMaxAperture"));
        assertEquals("7.8 x 7.8 um", info.get("SensorPixelSize"));
        assertEquals("No= 20025585", info.get("SerialNumber"));
        assertEquals("2361498", info.get("ImageDataSize"));
        assertEquals("526", info.get("ShutterCount"));

        // PreviewIFD (0x0011 sub-directory)
        assertEquals("JPEG (old-style)", info.get("Compression"));
        assertEquals("2466", info.get("PreviewImageStart"));
        assertEquals("26", info.get("PreviewImageLength"));

        // AFInfo (0x0088 binary sub-directory)
        assertEquals("Single Area", info.get("AFAreaMode"));
        assertEquals("Center", info.get("AFPoint"));
        assertEquals("Center", info.get("AFPointsInFocus"));

        // ExifIFD text/binary tags
        assertEquals("curve: fotogenics point and shoot", info.get("UserComment"));
        assertEquals("[Blue,Green][Green,Red]", info.get("CFAPattern"));
    }

    @Test
    void makerNoteTagsPresent() throws IOException {
        Map<String, Object> info = info();
        // spot-check that maker note dispatch ran
        assertEquals("2.10", info.get("MakerNoteVersion"));
        assertEquals("18-70mm f/3.5-4.5", info.get("Lens"));
        assertEquals("526", info.get("ShutterCount"));
    }
}
