package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.CanonConverters;
import com.gdxsoft.easyweb.exiftool.convert.NikonConverters;

/**
 * Canon maker note tables, ported from {@code Image::ExifTool::Canon}.
 * Phase 2 covers the Main table top-level tags, CanonCameraSettings and
 * CanonFocalLength; CanonShotInfo and other sub-directories are Phase 3.
 */
public final class CanonTables {

    public static final String NAME = "Image::ExifTool::Canon::Main";

    private CanonTables() {}

    public static TagTable main() {
        return MainHolder.INSTANCE;
    }

    public static TagTable cameraSettings() {
        return CameraSettingsHolder.INSTANCE;
    }

    public static TagTable focalLength() {
        return FocalLengthHolder.INSTANCE;
    }

    public static TagTable shotInfo() {
        return ShotInfoHolder.INSTANCE;
    }

    public static TagTable fileInfo() {
        return FileInfoHolder.INSTANCE;
    }

    private static final class MainHolder {
        static final TagTable INSTANCE = buildMain();
    }

    private static TagTable buildMain() {
        TagTable t = new TagTable(NAME);
        t.add(TagInfo.builder(0x1, "CanonCameraSettings")
            .subDirectory("CanonCameraSettings", cameraSettings())
            .build());
        t.add(TagInfo.builder(0x2, "CanonFocalLength")
            .subDirectory("CanonFocalLength", focalLength())
            .build());
        // 0x3 CanonFlashInfo: Unknown in reference (not output)
        t.add(TagInfo.builder(0x4, "CanonShotInfo")
            .subDirectory("CanonShotInfo", shotInfo())
            .build());
        t.add(simple(0x6, "CanonImageType"));
        t.add(simple(0x7, "CanonFirmwareVersion"));
        t.add(simple(0x8, "FileNumber", CanonConverters.FILE_NUMBER));
        t.add(simple(0x9, "OwnerName"));
        // 0xc SerialNumber: non-D30/1D models use zero-padded 10 digits
        t.add(TagInfo.builder(0xc, "SerialNumber")
            .printConv(CanonConverters.SERIAL_NUMBER_10)
            .build());
        t.add(simple(0xe, "CanonFileLength"));
        t.add(simple(0x10, "CanonModelID", CanonConverters.CANON_MODEL_ID));
        t.add(simple(0x13, "ThumbnailImageValidArea"));
        t.add(TagInfo.builder(0x15, "SerialNumberFormat")
            .printConv(serialNumberFormatLookup())
            .build());
        t.add(TagInfo.builder(0x93, "CanonFileInfo")
            .subDirectory("CanonFileInfo", fileInfo())
            .build());
        return t.register();
    }

    /** Canon::CameraSettings: binary data directory, FIRST_ENTRY 1, int16s entries. */
    private static final class CameraSettingsHolder {
        static final TagTable INSTANCE = buildCameraSettings();
    }

    private static TagTable buildCameraSettings() {
        TagTable t = new TagTable("Image::ExifTool::Canon::CameraSettings");
        t.binaryData("int16s", 1);
        t.add(simple(1, "MacroMode", CanonConverters.MACRO_MODE));
        t.add(simple(2, "SelfTimer", CanonConverters.SELF_TIMER));
        t.add(simple(3, "Quality", CanonConverters.CANON_QUALITY));
        t.add(simple(4, "CanonFlashMode", CanonConverters.CANON_FLASH_MODE));
        t.add(simple(5, "ContinuousDrive", CanonConverters.CONTINUOUS_DRIVE));
        t.add(simple(7, "FocusMode", CanonConverters.FOCUS_MODE));
        t.add(simple(9, "RecordMode", CanonConverters.RECORD_MODE));
        t.add(simple(10, "CanonImageSize", CanonConverters.CANON_IMAGE_SIZE));
        t.add(simple(11, "EasyMode", CanonConverters.EASY_MODE));
        t.add(simple(12, "DigitalZoom", CanonConverters.DIGITAL_ZOOM));
        t.add(simple(13, "Contrast", CanonConverters.PARAMETER));
        t.add(simple(14, "Saturation", CanonConverters.PARAMETER));
        t.add(simple(15, "Sharpness", CanonConverters.SHARPNESS));
        t.add(TagInfo.builder(16, "CameraISO")
            .valueConv(CanonConverters.CAMERA_ISO)
            .build());
        t.add(simple(17, "MeteringMode", CanonConverters.METERING_MODE));
        t.add(simple(18, "FocusRange", CanonConverters.FOCUS_RANGE));
        t.add(TagInfo.builder(19, "AFPoint")
            .valueConv(notValue(0L)) // RawConv: '$val==0 ? undef'
            .printConv(CanonConverters.AF_POINT)
            .build());
        t.add(simple(20, "CanonExposureMode", CanonConverters.CANON_EXPOSURE_MODE));
        t.add(TagInfo.builder(22, "LensType")
            .format("int16u")
            .valueConv(notValue(0L)) // RawConv: '$val ? ... : undef'
            .printConv(lensTypeLookup())
            .build());
        t.add(TagInfo.builder(23, "MaxFocalLength")
            .format("int16u")
            .printConv(mmPrint())
            .build());
        t.add(TagInfo.builder(24, "MinFocalLength")
            .format("int16u")
            .printConv(mmPrint())
            .build());
        t.add(simple(25, "FocalUnits", v -> v instanceof Number n ? n + "/mm" : v));
        t.add(TagInfo.builder(26, "MaxAperture")
            .valueConv(CanonConverters.CANON_APERTURE_VALUE)
            .printConv(CanonConverters.TWO_SIG_FIG_PRINT)
            .build());
        t.add(TagInfo.builder(27, "MinAperture")
            .valueConv(CanonConverters.CANON_APERTURE_VALUE)
            .printConv(CanonConverters.TWO_SIG_FIG_PRINT)
            .build());
        t.add(simple(28, "FlashModel", CanonConverters.FLASH_MODEL));
        t.add(simple(29, "FlashBits", CanonConverters.FLASH_BITS));
        t.add(TagInfo.builder(32, "FocusContinuous")
            .valueConv(notValue(-1L)) // RawConv: '$val==-1 ? undef'
            .printConv(CanonConverters.FOCUS_CONTINUOUS)
            .build());
        t.add(TagInfo.builder(33, "AESetting")
            .valueConv(notValue(-1L))
            .printConv(CanonConverters.AE_SETTING)
            .build());
        t.add(TagInfo.builder(34, "ImageStabilization")
            .valueConv(notValue(-1L))
            .printConv(CanonConverters.IMAGE_STABILIZATION)
            .build());
        t.add(simple(35, "DisplayAperture", v -> v instanceof Number n ? n.doubleValue() / 10 : v));
        t.add(simple(36, "ZoomSourceWidth"));
        t.add(simple(37, "ZoomTargetWidth"));
        t.add(TagInfo.builder(39, "SpotMeteringMode")
            .valueConv(notValue(-1L))
            .printConv(CanonConverters.SPOT_METERING_MODE)
            .build());
        t.add(TagInfo.builder(40, "PhotoEffect")
            .valueConv(notValue(-1L)) // RawConv: '$val==-1 ? undef'
            .printConv(CanonConverters.PHOTO_EFFECT)
            .build());
        t.add(simple(41, "ManualFlashOutput", CanonConverters.MANUAL_FLASH_OUTPUT));
        t.add(simple(42, "ColorTone", CanonConverters.PARAMETER));
        t.add(simple(46, "SRAWQuality", CanonConverters.SRAW_QUALITY));
        t.add(simple(50, "FocusBracketing", lookup("0", "Disable", "1", "Enable")));
        return t.register();
    }

    /** Canon::FocalLength: binary data directory, int16u entries. */
    private static final class FocalLengthHolder {
        static final TagTable INSTANCE = buildFocalLength();
    }

    private static TagTable buildFocalLength() {
        TagTable t = new TagTable("Image::ExifTool::Canon::FocalLength");
        t.binaryData("int16u");
        t.add(TagInfo.builder(0, "FocalType")
            .valueConv(notValue(0L)) // RawConv: '$val ? $val : undef'
            .printConv(CanonConverters.FOCAL_TYPE)
            .build());
        t.add(TagInfo.builder(1, "FocalLength")
            .priority(0) // EXIF FocalLength is more reliable
            .printConv(mmPrint())
            .build());
        t.add(TagInfo.builder(2, "FocalPlaneXSize")
            .valueConv(inchToMm())
            .printConv(twoDecimalMmPrint())
            .build());
        t.add(TagInfo.builder(3, "FocalPlaneYSize")
            .valueConv(inchToMm())
            .printConv(twoDecimalMmPrint())
            .build());
        return t.register();
    }

    /** Canon::ShotInfo: binary data directory, FIRST_ENTRY 1, int16s entries. */
    private static final class ShotInfoHolder {
        static final TagTable INSTANCE = buildShotInfo();
    }

    private static TagTable buildShotInfo() {
        TagTable t = new TagTable("Image::ExifTool::Canon::ShotInfo");
        t.binaryData("int16s", 1);
        t.add(TagInfo.builder(1, "AutoISO")
            .valueConv(CanonConverters.ISO_EV100_VALUE)
            .printConv(CanonConverters.ZERO_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(2, "BaseISO")
            .priority(0)
            .valueConv(CanonConverters.BASE_ISO_VALUE)
            .printConv(CanonConverters.ZERO_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(3, "MeasuredEV")
            .valueConv(CanonConverters.MEASURED_EV_VALUE)
            .printConv(CanonConverters.TWO_DECIMAL_PRINT)
            .build());
        t.add(TagInfo.builder(4, "TargetAperture")
            .valueConv(CanonConverters.CANON_APERTURE_VALUE)
            .printConv(CanonConverters.TWO_SIG_FIG_PRINT)
            .build());
        t.add(TagInfo.builder(6, "ExposureCompensation")
            .valueConv(CanonConverters.CANON_EV)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(simple(7, "WhiteBalance", CanonConverters.WHITE_BALANCE));
        t.add(simple(8, "SlowShutter", CanonConverters.SLOW_SHUTTER));
        t.add(simple(9, "SequenceNumber"));
        t.add(simple(10, "OpticalZoomCode", CanonConverters.OPTICAL_ZOOM_CODE));
        t.add(TagInfo.builder(13, "FlashGuideNumber")
            .valueConv(v -> v instanceof Number n ? n.doubleValue() / 32 : v)
            .build());
        t.add(TagInfo.builder(15, "FlashExposureComp")
            .valueConv(CanonConverters.CANON_EV)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(simple(16, "AutoExposureBracketing", CanonConverters.AUTO_EXPOSURE_BRACKETING));
        t.add(TagInfo.builder(17, "AEBBracketValue")
            .valueConv(CanonConverters.CANON_EV)
            .printConv(NikonConverters.FRACTION_PRINT)
            .build());
        t.add(simple(18, "ControlMode", CanonConverters.CONTROL_MODE));
        t.add(TagInfo.builder(19, "FocusDistanceUpper")
            .format("int16u")
            .valueConv(CanonConverters.FOCUS_DISTANCE_VALUE)
            .printConv(CanonConverters.FOCUS_DISTANCE_PRINT)
            .build());
        t.add(TagInfo.builder(20, "FocusDistanceLower")
            .format("int16u")
            .valueConv(CanonConverters.FOCUS_DISTANCE_VALUE)
            .printConv(CanonConverters.FOCUS_DISTANCE_PRINT)
            .build());
        t.add(TagInfo.builder(23, "MeasuredEV2")
            .valueConv(CanonConverters.MEASURED_EV2_VALUE)
            .build());
        t.add(TagInfo.builder(24, "BulbDuration")
            .valueConv(v -> v instanceof Number n ? n.doubleValue() / 10 : v)
            .build());
        t.add(simple(26, "CameraType", CanonConverters.CAMERA_TYPE));
        t.add(TagInfo.builder(27, "AutoRotate")
            .valueConv(notNegative())
            .printConv(CanonConverters.AUTO_ROTATE)
            .build());
        t.add(simple(28, "NDFilter", CanonConverters.ND_FILTER));
        t.add(TagInfo.builder(29, "SelfTimer2")
            .valueConv(notNegative())
            .printConv(v -> v instanceof Number n ? n.doubleValue() / 10 : v)
            .build());
        return t.register();
    }

    /** Canon::FileInfo: binary data directory, FIRST_ENTRY 1, int16s entries. */
    private static final class FileInfoHolder {
        static final TagTable INSTANCE = buildFileInfo();
    }

    private static TagTable buildFileInfo() {
        TagTable t = new TagTable("Image::ExifTool::Canon::FileInfo");
        t.binaryData("int16s", 1);
        t.add(simple(3, "BracketMode", CanonConverters.BRACKET_MODE));
        t.add(simple(4, "BracketValue"));
        t.add(simple(5, "BracketShotNumber"));
        t.add(TagInfo.builder(6, "RawJpgQuality")
            .valueConv(notValue(0L))
            .printConv(CanonConverters.CANON_QUALITY)
            .build());
        t.add(TagInfo.builder(7, "RawJpgSize")
            .valueConv(notNegative())
            .printConv(CanonConverters.CANON_IMAGE_SIZE)
            .build());
        t.add(TagInfo.builder(8, "LongExposureNoiseReduction2")
            .valueConv(notNegative())
            .printConv(lookup("0", "Off", "1", "On"))
            .build());
        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }

    /** ValueConv that maps the given value to null (RawConv "undef" behaviour). */
    private static ValueConverter notValue(long skip) {
        return v -> (v instanceof Number n && n.longValue() == skip) ? null : v;
    }

    /** ValueConv that drops negative values (RawConv "$val >= 0 ? $val : undef"). */
    private static ValueConverter notNegative() {
        return v -> (v instanceof Number n && n.longValue() < 0) ? null : v;
    }

    /** %canonLensTypes: Phase 2 subset (unknown values show "n/a" via lookup miss). */
    private static ValueConverter lensTypeLookup() {
        return new com.gdxsoft.easyweb.exiftool.convert.LookupConverter(Map.of(
            "-1", "n/a", "65535", "n/a", "1", "Canon EF 50mm f/1.8"));
    }

    /** 0x15 SerialNumberFormat: PrintHex lookup. */
    private static ValueConverter serialNumberFormatLookup() {
        return new com.gdxsoft.easyweb.exiftool.convert.LookupConverter(Map.of(
            "2415919104", "Format 1", // 0x90000000
            "2684354560", "Format 2")); // 0xa0000000
    }

    private static ValueConverter lookup(String... kv) {
        Map<String, String> m = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return new com.gdxsoft.easyweb.exiftool.convert.LookupConverter(m);
    }

    private static ValueConverter mmPrint() {
        return v -> v instanceof Number n ? n + " mm" : v;
    }

    /** ValueConv: focal plane size in 1/1000 inch -> mm. */
    private static ValueConverter inchToMm() {
        return v -> v instanceof Number n ? n.doubleValue() * 25.4 / 1000 : v;
    }

    private static ValueConverter twoDecimalMmPrint() {
        return v -> v instanceof Number n ? String.format("%.2f mm", n.doubleValue()) : v;
    }
}
