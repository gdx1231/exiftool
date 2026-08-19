package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.ExifConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;
import com.gdxsoft.easyweb.exiftool.convert.NikonConverters;

/**
 * Nikon maker note tables, ported from {@code Image::ExifTool::Nikon}.
 * Phase 2 covers the Type 2/3 layout used by the D70 (and similar DSLRs).
 */
public final class NikonTables {

    public static final String NAME = "Image::ExifTool::Nikon::Main";

    private static final ValueConverter FLASH_MODE = new LookupConverter(Map.of(
        "0", "Did Not Fire",
        "1", "Fired, Manual",
        "3", "Not Ready",
        "7", "Fired, External",
        "8", "Fired, Commander Mode",
        "9", "Fired, TTL Mode",
        "18", "LED Light"));

    private NikonTables() {}

    public static TagTable main() {
        return MainHolder.INSTANCE;
    }

    public static TagTable previewIfd() {
        return PreviewIfdHolder.INSTANCE;
    }

    public static TagTable afInfo() {
        return AfInfoHolder.INSTANCE;
    }

    public static TagTable lensData01() {
        return LensData01Holder.INSTANCE;
    }

    public static TagTable colorBalance3() {
        return ColorBalance3Holder.INSTANCE;
    }

    public static TagTable shotInfo() {
        return ShotInfoHolder.INSTANCE;
    }

    private static final class MainHolder {
        static final TagTable INSTANCE = buildMain();
    }

    private static TagTable buildMain() {
        TagTable t = new TagTable(NAME);
        t.add(TagInfo.builder(0x0001, "MakerNoteVersion")
            .format("undef")
            .valueConv(NikonConverters.MAKER_NOTE_VERSION_VALUE)
            .printConv(NikonConverters.MAKER_NOTE_VERSION_PRINT)
            .build());
        t.add(TagInfo.builder(0x0002, "ISO")
            .priority(0) // EXIF ISO is more reliable (Nikon Priority => 0)
            .printConv(NikonConverters.ISO_PRINT)
            .build());
        t.add(simple(0x0003, "ColorMode", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0004, "Quality", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0005, "WhiteBalance", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0006, "Sharpness", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0007, "FocusMode", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0008, "FlashSetting", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0009, "FlashType", NikonConverters.FORMAT_STRING));
        t.add(simple(0x000b, "WhiteBalanceFineTune"));
        t.add(TagInfo.builder(0x000d, "ProgramShift")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(TagInfo.builder(0x000e, "ExposureDifference")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.EXPOSURE_DIFFERENCE_PRINT)
            .build());
        t.add(TagInfo.builder(0x0011, "PreviewIFD")
            .subDirectory("PreviewIFD", previewIfd())
            .build());
        t.add(TagInfo.builder(0x0012, "FlashExposureComp")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(TagInfo.builder(0x0013, "ISOSetting")
            .printConv(NikonConverters.ISO_SETTING_PRINT)
            .build());
        t.add(simple(0x0016, "ImageBoundary"));
        t.add(TagInfo.builder(0x0017, "ExternalFlashExposureComp")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(TagInfo.builder(0x0018, "FlashExposureBracketValue")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.ONE_DECIMAL_PRINT)
            .build());
        t.add(simple(0x0019, "ExposureBracketValue"));
        t.add(simple(0x0081, "ToneComp", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0083, "LensType", NikonConverters.LENS_TYPE_PRINT));
        t.add(simple(0x0084, "Lens", NikonConverters.LENS_INFO_PRINT));
        t.add(simple(0x0087, "FlashMode", FLASH_MODE));
        t.add(TagInfo.builder(0x0088, "AFInfo")
            .subDirectory("AFInfo", afInfo())
            .build());
        t.add(simple(0x0089, "ShootingMode", NikonConverters.SHOOTING_MODE_PRINT));
        t.add(TagInfo.builder(0x008b, "LensFStops")
            .format("undef")
            .valueConv(NikonConverters.APEX_FROM_3_BYTES)
            .printConv(NikonConverters.TWO_DECIMAL_PRINT)
            .build());
        t.add(simple(0x008d, "ColorHue", NikonConverters.FORMAT_STRING));
        t.add(simple(0x0090, "LightSource", NikonConverters.FORMAT_STRING));
        t.add(TagInfo.builder(0x0091, "ShotInfo")
            .subDirectory("ShotInfo", shotInfo())
            .build());
        t.add(simple(0x0092, "HueAdjustment"));
        t.add(simple(0x0095, "NoiseReduction", NikonConverters.FORMAT_STRING));
        t.add(TagInfo.builder(0x0097, "ColorBalance")
            .subDirectory("ColorBalance", colorBalance3(), 20)
            .build());
        t.add(TagInfo.builder(0x0098, "LensData")
            .subDirectory("LensData", lensData01())
            .build());
        t.add(simple(0x009a, "SensorPixelSize", NikonConverters.SENSOR_PIXEL_SIZE_PRINT));
        t.add(simple(0x00a0, "SerialNumber", NikonConverters.FORMAT_STRING));
        t.add(simple(0x00a2, "ImageDataSize"));
        t.add(simple(0x00a7, "ShutterCount"));
        return t.register();
    }

    /** Nikon::PreviewIFD: standard IFD with EXIF-style conversions. */
    private static final class PreviewIfdHolder {
        static final TagTable INSTANCE = buildPreviewIfd();
    }

    private static TagTable buildPreviewIfd() {
        TagTable t = new TagTable("Image::ExifTool::Nikon::PreviewIFD");
        t.add(simple(0x0103, "Compression", ExifConverters.COMPRESSION));
        t.add(simple(0x011a, "XResolution"));
        t.add(simple(0x011b, "YResolution"));
        t.add(simple(0x0128, "ResolutionUnit", ExifConverters.lookup(Map.of(
            "1", "None", "2", "inches", "3", "cm"))));
        t.add(TagInfo.builder(0x0201, "PreviewImageStart").isOffset(true).build());
        t.add(simple(0x0202, "PreviewImageLength"));
        t.add(simple(0x0213, "YCbCrPositioning", ExifConverters.YCBCR_POSITIONING));
        return t.register();
    }

    /** Nikon::AFInfo: binary data directory (4 bytes for the D70). */
    private static final class AfInfoHolder {
        static final TagTable INSTANCE = buildAfInfo();
    }

    private static TagTable buildAfInfo() {
        TagTable t = new TagTable("Image::ExifTool::Nikon::AFInfo");
        t.binaryData("int8u");
        t.add(TagInfo.builder(0x0000, "AFAreaMode")
            .printConv(NikonConverters.AF_AREA_MODE)
            .build());
        t.add(TagInfo.builder(0x0001, "AFPoint")
            .printConv(NikonConverters.AF_POINT)
            .build());
        t.add(TagInfo.builder(0x0002, "AFPointsInFocus")
            .format("int16u")
            .printConv(NikonConverters.AF_POINTS_IN_FOCUS)
            .build());
        return t.register();
    }

    /** Nikon::LensData01: binary data directory (D70 = 31 bytes). */
    private static final class LensData01Holder {
        static final TagTable INSTANCE = buildLensData01();
    }

    private static TagTable buildLensData01() {
        TagTable t = new TagTable("Image::ExifTool::Nikon::LensData01");
        t.binaryData("int8u");
        t.add(TagInfo.builder(0x00, "LensDataVersion").format("string[4]").build());
        t.add(TagInfo.builder(0x04, "ExitPupilPosition")
            .valueConv(NikonConverters.EXIT_PUPIL_VALUE)
            .printConv(NikonConverters.ONE_DECIMAL_MM_PRINT)
            .build());
        t.add(TagInfo.builder(0x05, "AFAperture")
            .valueConv(NikonConverters.APERTURE_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(0x08, "FocusPosition")
            .printConv(NikonConverters.HEX_BYTE_PRINT)
            .build());
        t.add(TagInfo.builder(0x09, "FocusDistance")
            .valueConv(NikonConverters.FOCUS_DISTANCE_VALUE)
            .printConv(NikonConverters.FOCUS_DISTANCE_PRINT)
            .build());
        t.add(TagInfo.builder(0x0a, "FocalLength")
            .priority(0) // Nikon LensData FocalLength is lower priority than EXIF
            .valueConv(NikonConverters.FOCAL_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_MM_PRINT)
            .build());
        t.add(simple(0x0b, "LensIDNumber"));
        t.add(TagInfo.builder(0x0c, "LensFStops")
            .valueConv(NikonConverters.LENS_FSTOPS_VALUE)
            .printConv(NikonConverters.TWO_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(0x0d, "MinFocalLength")
            .valueConv(NikonConverters.FOCAL_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_MM_PRINT)
            .build());
        t.add(TagInfo.builder(0x0e, "MaxFocalLength")
            .valueConv(NikonConverters.FOCAL_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_MM_PRINT)
            .build());
        t.add(TagInfo.builder(0x0f, "MaxApertureAtMinFocal")
            .valueConv(NikonConverters.APERTURE_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(0x10, "MaxApertureAtMaxFocal")
            .valueConv(NikonConverters.APERTURE_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_PRINT)
            .build());
        t.add(simple(0x11, "MCUVersion"));
        t.add(TagInfo.builder(0x12, "EffectiveMaxAperture")
            .valueConv(NikonConverters.APERTURE_APEX_NIKON)
            .printConv(NikonConverters.ONE_DECIMAL_PRINT)
            .build());
        return t.register();
    }

    /** Nikon::ColorBalance3: WB_RGBGLevels at offset 0 (D70). */
    private static final class ColorBalance3Holder {
        static final TagTable INSTANCE = buildColorBalance3();
    }

    private static TagTable buildColorBalance3() {
        TagTable t = new TagTable("Image::ExifTool::Nikon::ColorBalance3");
        t.binaryData("int16u");
        t.add(TagInfo.builder(0x0000, "WB_RGBGLevels").format("int16u[4]").build());
        return t.register();
    }

    /** Nikon::ShotInfo: ShotInfoVersion (D70 = "0103"). */
    private static final class ShotInfoHolder {
        static final TagTable INSTANCE = buildShotInfo();
    }

    private static TagTable buildShotInfo() {
        TagTable t = new TagTable("Image::ExifTool::Nikon::ShotInfo");
        t.binaryData("int8u");
        t.add(TagInfo.builder(0x0000, "ShotInfoVersion").format("string[4]").build());
        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }
}
