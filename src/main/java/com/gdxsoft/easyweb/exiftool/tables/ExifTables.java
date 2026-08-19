package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.ExifConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * The main EXIF tag table, ported from {@code Image::ExifTool::Exif::Main}.
 * Covers IFD0, IFD1, ExifIFD and InteropIFD tags plus the pointer tags that
 * recurse into those sub-directories.
 */
public final class ExifTables {

    public static final String NAME = "Image::ExifTool::Exif::Main";

    private static final ValueConverter RESOLUTION_UNIT = lookup(Map.of(
        "1", "None", "2", "inches", "3", "cm"));

    private static final ValueConverter EXPOSURE_PROGRAM = lookup(Map.of(
        "0", "Not Defined",
        "1", "Manual",
        "2", "Program AE",
        "3", "Aperture-priority AE",
        "4", "Shutter speed priority AE",
        "5", "Creative (Slow speed)",
        "6", "Action (High speed)",
        "7", "Portrait",
        "8", "Landscape",
        "9", "Bulb"));

    private static final ValueConverter METERING_MODE = lookup(Map.of(
        "0", "Unknown",
        "1", "Average",
        "2", "Center-weighted average",
        "3", "Spot",
        "4", "Multi-spot",
        "5", "Multi-segment",
        "6", "Partial",
        "255", "Other"));

    private static final ValueConverter COLOR_SPACE = lookup(Map.of(
        "1", "sRGB",
        "2", "Adobe RGB",
        "65535", "Uncalibrated",
        "65534", "ICC Profile",
        "65533", "Wide Gamut RGB"));

    private static final ValueConverter SENSING_METHOD = lookup(Map.of(
        "1", "Not defined",
        "2", "One-chip color area",
        "3", "Two-chip color area",
        "4", "Three-chip color area",
        "5", "Color sequential area",
        "7", "Trilinear",
        "8", "Color sequential linear"));

    private static final ValueConverter CUSTOM_RENDERED = lookup(Map.of(
        "0", "Normal", "1", "Custom"));
    private static final ValueConverter EXPOSURE_MODE = lookup(Map.of(
        "0", "Auto", "1", "Manual", "2", "Auto bracket"));
    private static final ValueConverter WHITE_BALANCE = lookup(Map.of(
        "0", "Auto", "1", "Manual"));
    private static final ValueConverter SCENE_CAPTURE_TYPE = lookup(Map.of(
        "0", "Standard", "1", "Landscape", "2", "Portrait", "3", "Night"));
    private static final ValueConverter GAIN_CONTROL = lookup(Map.of(
        "0", "None", "1", "Low gain up", "2", "High gain up",
        "3", "Low gain down", "4", "High gain down"));
    private static final ValueConverter LEVEL = lookup(Map.of(
        "0", "Normal", "1", "Low", "2", "High"));
    private static final ValueConverter SUBJECT_DISTANCE_RANGE = lookup(Map.of(
        "0", "Unknown", "1", "Macro", "2", "Close", "3", "Distant"));

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private ExifTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);

        // ---- IFD0 / IFD1 (TIFF base tags) ----
        t.add(TagInfo.builder(0x00FE, "SubfileType")
            .printConv(ExifConverters.lookup(Map.of(
                "0", "Full-resolution image", "1", "Reduced-resolution image",
                "2", "Single page of multi-page image",
                "3", "Single page of multi-page reduced-resolution image",
                "4", "Transparency mask", "5", "Transparency mask of reduced-resolution image")))
            .build());
        t.add(simple(0x0100, "ImageWidth"));
        t.add(simple(0x0101, "ImageHeight"));
        t.add(simple(0x0102, "BitsPerSample"));
        t.add(simple(0x0103, "Compression", ExifConverters.COMPRESSION));
        t.add(simple(0x0106, "PhotometricInterpretation", ExifConverters.PHOTOMETRIC_INTERPRETATION));
        t.add(TagInfo.builder(0x010E, "ImageDescription").writable("string").build());
        t.add(simple(0x010F, "Make"));
        t.add(simple(0x0110, "Model"));
        t.add(simple(0x0111, "StripOffsets"));
        t.add(simple(0x0112, "Orientation", ExifConverters.ORIENTATION));
        t.add(simple(0x0115, "SamplesPerPixel"));
        t.add(simple(0x0116, "RowsPerStrip"));
        t.add(simple(0x0117, "StripByteCounts"));
        t.add(simple(0x011A, "XResolution"));
        t.add(simple(0x011B, "YResolution"));
        t.add(TagInfo.builder(0x011C, "PlanarConfiguration")
            .priority(0)
            .printConv(ExifConverters.lookup(Map.of("1", "Chunky", "2", "Planar")))
            .build());
        t.add(TagInfo.builder(0x0128, "ResolutionUnit")
            .printConv(RESOLUTION_UNIT)
            .build());
        t.add(simple(0x0129, "PageNumber"));
        t.add(simple(0x012D, "TransferFunction"));
        t.add(TagInfo.builder(0x013D, "Predictor")
            .printConv(ExifConverters.lookup(Map.of(
                "1", "None", "2", "Horizontal differencing", "3", "Floating point",
                "34892", "Horizontal difference X2", "34893", "Horizontal difference X4",
                "34894", "Floating point X2", "34895", "Floating point X4")))
            .build());
        t.add(TagInfo.builder(0x0131, "Software").writable("string").build());
        t.add(simple(0x0132, "ModifyDate"));
        t.add(TagInfo.builder(0x013B, "Artist").writable("string").build());
        t.add(simple(0x013E, "WhitePoint"));
        t.add(simple(0x013F, "PrimaryChromaticities"));
        t.add(TagInfo.builder(0x0201, "ThumbnailOffset").isOffset(true).build());
        t.add(simple(0x0202, "ThumbnailLength"));
        t.add(simple(0x0211, "YCbCrCoefficients"));
        t.add(simple(0x0212, "YCbCrSubSampling"));
        t.add(simple(0x0213, "YCbCrPositioning", ExifConverters.YCBCR_POSITIONING));
        t.add(simple(0x0214, "ReferenceBlackWhite"));
        t.add(TagInfo.builder(0x8298, "Copyright").writable("string").build());
        t.add(TagInfo.builder(0x83bb, "IPTC-NAA").subDirectory("IPTC", null).build());
        t.add(simple(0x85D8, "ModelTransform"));

        // ---- pointer tags (SubDirectory) ----
        t.add(TagInfo.builder(0x8769, "ExifOffset").subDirectory("ExifIFD", null).build());
        t.add(TagInfo.builder(0x8825, "GPSInfo").subDirectory("GPS", GpsTables.main()).build());
        t.add(TagInfo.builder(0xa005, "InteropOffset").subDirectory("InteropIFD", null).build());
        t.add(TagInfo.builder(0xc4a5, "PrintIM").subDirectory("PrintIM", PrintIMTables.main()).build());

        // ---- ExifIFD tags ----
        t.add(simple(0x829A, "ExposureTime", ExifConverters.EXPOSURE_TIME));
        t.add(simple(0x829D, "FNumber", ExifConverters.F_NUMBER));
        t.add(simple(0x8822, "ExposureProgram", EXPOSURE_PROGRAM));
        t.add(simple(0x8824, "SpectralSensitivity"));
        t.add(TagInfo.builder(0x8827, "ISO").writable("int16u").build());
        t.add(simple(0x8828, "OECF"));
        t.add(simple(0x8830, "SensitivityType"));
        t.add(simple(0x8832, "SensitivityValue"));
        t.add(simple(0x9000, "ExifVersion", ExifConverters.UNDEF_STRING));
        t.add(simple(0x9003, "DateTimeOriginal"));
        t.add(simple(0x9004, "CreateDate"));
        t.add(TagInfo.builder(0x9101, "ComponentsConfiguration")
            .format("int8u")
            .printConv(ExifConverters.COMPONENTS_CONFIGURATION)
            .build());
        t.add(simple(0x9102, "CompressedBitsPerPixel"));
        t.add(TagInfo.builder(0x9201, "ShutterSpeedValue")
            .valueConv(ExifConverters.SHUTTER_SPEED_APEX)
            .printConv(ExifConverters.EXPOSURE_TIME)
            .build());
        t.add(TagInfo.builder(0x9202, "ApertureValue")
            .valueConv(ExifConverters.APERTURE_APEX)
            .printConv(ExifConverters.APERTURE_PRINT)
            .build());
        t.add(simple(0x9203, "BrightnessValue"));
        t.add(simple(0x9204, "ExposureCompensation"));
        t.add(TagInfo.builder(0x9205, "MaxApertureValue")
            .valueConv(ExifConverters.APERTURE_APEX)
            .printConv(ExifConverters.APERTURE_PRINT)
            .build());
        t.add(simple(0x9206, "SubjectDistance"));
        t.add(simple(0x9207, "MeteringMode", METERING_MODE));
        t.add(simple(0x9208, "LightSource", ExifConverters.LIGHT_SOURCE));
        t.add(simple(0x9209, "Flash", ExifConverters.FLASH));
        t.add(simple(0x920A, "FocalLength", ExifConverters.FOCAL_LENGTH));
        t.add(simple(0x9214, "SubjectArea"));
        // 0x927C MakerNote: Phase 2 (maker note parsing)
        t.add(TagInfo.builder(0x9286, "UserComment")
            .valueConv(ExifConverters.USER_COMMENT)
            .build());
        t.add(simple(0x9290, "SubSecTime"));
        t.add(simple(0x9291, "SubSecTimeOriginal"));
        t.add(simple(0x9292, "SubSecTimeDigitized"));
        t.add(simple(0xA000, "FlashpixVersion", ExifConverters.UNDEF_STRING));
        t.add(simple(0xA001, "ColorSpace", COLOR_SPACE));
        t.add(simple(0xA002, "ExifImageWidth"));
        t.add(simple(0xA003, "ExifImageHeight"));
        t.add(simple(0xA004, "RelatedSoundFile"));
        t.add(simple(0xA20B, "FlashEnergy"));
        t.add(simple(0xA20C, "SpatialFrequencyResponse"));
        t.add(simple(0xA20E, "FocalPlaneXResolution"));
        t.add(simple(0xA20F, "FocalPlaneYResolution"));
        t.add(simple(0xA210, "FocalPlaneResolutionUnit", RESOLUTION_UNIT));
        t.add(simple(0xA214, "SubjectLocation"));
        t.add(simple(0xA215, "ExposureIndex"));
        t.add(simple(0xA217, "SensingMethod", SENSING_METHOD));
        t.add(simple(0xA300, "FileSource", ExifConverters.FILE_SOURCE));
        t.add(simple(0xA301, "SceneType", ExifConverters.SCENE_TYPE));
        t.add(TagInfo.builder(0xA302, "CFAPattern")
            .valueConv(ExifConverters.CFA_PATTERN_DECODE)
            .printConv(ExifConverters.CFA_PATTERN_PRINT)
            .build());
        t.add(simple(0xA401, "CustomRendered", CUSTOM_RENDERED));
        t.add(simple(0xA402, "ExposureMode", EXPOSURE_MODE));
        t.add(simple(0xA403, "WhiteBalance", WHITE_BALANCE));
        t.add(simple(0xA404, "DigitalZoomRatio"));
        t.add(simple(0xA405, "FocalLengthIn35mmFormat", v -> v instanceof Number n ? n + " mm" : v));
        t.add(simple(0xA406, "SceneCaptureType", SCENE_CAPTURE_TYPE));
        t.add(simple(0xA407, "GainControl", GAIN_CONTROL));
        t.add(simple(0xA408, "Contrast", LEVEL));
        t.add(simple(0xA409, "Saturation", LEVEL));
        t.add(simple(0xA40A, "Sharpness", ExifConverters.SHARPNESS));
        t.add(simple(0xA40B, "DeviceSettingDescription"));
        t.add(simple(0xA40C, "SubjectDistanceRange", SUBJECT_DISTANCE_RANGE));

        // ---- InteropIFD tags ----
        t.add(simple(0x0001, "InteropIndex", ExifConverters.INTEROP_INDEX));
        t.add(simple(0x0002, "InteropVersion", ExifConverters.UNDEF_STRING));
        t.add(simple(0x1000, "RelatedImageFileFormat"));
        t.add(simple(0x1001, "RelatedImageWidth"));
        t.add(simple(0x1002, "RelatedImageLength"));

        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }

    private static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
