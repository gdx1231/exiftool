package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.ExifConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * Minolta maker note tables, ported from {@code Image::ExifTool::Minolta}.
 * The maker note is a plain IFD (no header); in-block offsets are absolute
 * (relative to the EXIF TIFF header). CameraSettings is a binary directory
 * of int32u entries with PRIORITY 0 (less reliable than EXIF tags).
 */
public final class MinoltaTables {

    public static final String NAME = "Image::ExifTool::Minolta::Main";

    private static final ValueConverter FLASH_MODE = lookup(Map.of(
        "0", "Fill flash", "1", "Red-eye reduction", "2", "Rear flash sync",
        "3", "Wireless", "4", "Off?", "255", "Always"));

    private static final ValueConverter WHITE_BALANCE = lookup(Map.of(
        "0", "Auto", "1", "Daylight", "2", "Cloudy", "3", "Tungsten",
        "4", "Fluorescent", "5", "Flash", "6", "Custom", "7", "Black & White"));

    private static final ValueConverter IMAGE_SIZE = lookup(Map.of(
        "0", "Full", "1", "1600x1200", "2", "1280x960", "3", "640x480",
        "6", "2080x1560", "7", "2560x1920", "8", "3264x2176"));

    private static final ValueConverter QUALITY = lookup(Map.of(
        "0", "Raw", "1", "Super Fine", "2", "Fine", "3", "Standard",
        "4", "Economy", "5", "Extra Fine"));

    private static final ValueConverter DRIVE_MODE = lookup(Map.of(
        "0", "Single", "1", "Continuous", "2", "Self-timer", "4", "Bracketing",
        "5", "Interval", "6", "UHS continuous", "7", "HS continuous"));

    private static final ValueConverter MACRO_MODE = lookup(Map.of(
        "0", "Off", "1", "On"));

    private static final ValueConverter DIGITAL_ZOOM = lookup(Map.of(
        "0", "Off", "1", "Electronic magnification", "2", "2x"));

    private static final ValueConverter BRACKET_STEP = lookup(Map.of(
        "0", "1/3 EV", "1", "2/3 EV", "2", "1 EV"));

    private static final ValueConverter FLASH_FIRED = lookup(Map.of(
        "0", "No", "1", "Yes"));

    private static final ValueConverter FILE_NUMBER_MEMORY = lookup(Map.of(
        "0", "Off", "1", "On"));

    private static final ValueConverter SHARPNESS = lookup(Map.of(
        "0", "Hard", "1", "Normal", "2", "Soft"));

    private static final ValueConverter SUBJECT_PROGRAM = lookup(Map.of(
        "0", "None", "1", "Portrait", "2", "Text", "3", "Night portrait",
        "4", "Sunset", "5", "Sports action"));

    private static final ValueConverter ISO_SETTING = lookup(Map.ofEntries(
        Map.entry("0", "100"), Map.entry("1", "200"), Map.entry("2", "400"),
        Map.entry("3", "800"), Map.entry("4", "Auto"), Map.entry("5", "64"),
        Map.entry("6", "100"), Map.entry("7", "200"), Map.entry("8", "400"),
        Map.entry("9", "800"), Map.entry("10", "1600"), Map.entry("11", "3200")));

    private static final ValueConverter MODEL_ID = lookup(Map.of(
        "0", "DiMAGE 7, X1, X21 or X31", "1", "DiMAGE 5", "2", "DiMAGE S304",
        "3", "DiMAGE S404", "4", "DiMAGE 7i", "5", "DiMAGE 7Hi",
        "6", "DiMAGE A1", "7", "DiMAGE A2 or S414", "8", "DiMAGE A2"));

    private static final ValueConverter INTERVAL_MODE = lookup(Map.of(
        "0", "Still Image", "1", "Time-lapse Movie"));

    private static final ValueConverter FOLDER_NAME = lookup(Map.of(
        "0", "Standard Form", "1", "Data Form"));

    private static final ValueConverter COLOR_MODE = lookup(Map.of(
        "0", "Natural color", "1", "Black & White", "2", "Vivid color",
        "3", "Solarization", "4", "Adobe RGB"));

    private static final ValueConverter INTERNAL_FLASH = lookup(Map.of(
        "0", "No", "1", "Fired"));

    private static final ValueConverter WIDE_FOCUS_ZONE = lookup(Map.of(
        "0", "No zone", "1", "Center zone (horizontal orientation)",
        "2", "Center zone (vertical orientation)", "3", "Left zone",
        "4", "Right zone"));

    private static final ValueConverter FOCUS_MODE = lookup(Map.of(
        "0", "AF", "1", "MF"));

    private static final ValueConverter FOCUS_AREA = lookup(Map.of(
        "0", "Wide Focus (normal)", "1", "Spot Focus"));

    private static final ValueConverter DEC_POSITION = lookup(Map.of(
        "0", "Exposure", "1", "Contrast", "2", "Saturation", "3", "Filter"));

    private MinoltaTables() {}

    public static TagTable main() {
        return MainHolder.INSTANCE;
    }

    public static TagTable cameraSettings() {
        return CameraSettingsHolder.INSTANCE;
    }

    private static final class MainHolder {
        static final TagTable INSTANCE = buildMain();
    }

    private static TagTable buildMain() {
        TagTable t = new TagTable(NAME);
        t.add(TagInfo.builder(0x0000, "MakerNoteVersion")
            .printConv(ExifConverters.UNDEF_STRING)
            .build());
        t.add(TagInfo.builder(0x0003, "MinoltaCameraSettings")
            .subDirectory("MinoltaCameraSettings", cameraSettings())
            .build());
        t.add(simple(0x0040, "CompressedImageSize"));
        t.add(TagInfo.builder(0x0088, "PreviewImageStart").isOffset(true).build());
        t.add(simple(0x0089, "PreviewImageLength"));
        return t.register();
    }

    /** Minolta::CameraSettings: binary directory, int32u entries, PRIORITY 0. */
    private static final class CameraSettingsHolder {
        static final TagTable INSTANCE = buildCameraSettings();
    }

    private static TagTable buildCameraSettings() {
        TagTable t = new TagTable("Image::ExifTool::Minolta::CameraSettings");
        t.binaryData("int32u");
        t.add(p0(1, "ExposureMode", lookup(Map.of(
            "0", "Program", "1", "Aperture Priority", "2", "Shutter Priority",
            "3", "Manual", "4", "Auto" ))));
        t.add(p0(2, "FlashMode", FLASH_MODE));
        t.add(p0(3, "WhiteBalance", WHITE_BALANCE));
        t.add(p0(4, "MinoltaImageSize", IMAGE_SIZE));
        t.add(p0(5, "MinoltaQuality", QUALITY));
        t.add(p0(6, "DriveMode", DRIVE_MODE));
        t.add(p0(7, "MeteringMode", lookup(Map.of(
            "0", "Multi-segment", "1", "Center-weighted average", "2", "Spot"))));
        t.add(p0(8, "ISO", v -> v instanceof Number n ? String.valueOf((long) (n.doubleValue() + 0.5)) : v));
        t.add(p0(9, "ExposureTime", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return ExifConverters.EXPOSURE_TIME.convert(Math.pow(2, (48 - n.doubleValue()) / 8));
        }));
        t.add(p0(10, "FNumber", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return String.format("%.1f", Math.pow(2, (n.doubleValue() - 8) / 16));
        }));
        t.add(p0(11, "MacroMode", MACRO_MODE));
        t.add(p0(12, "DigitalZoom", DIGITAL_ZOOM));
        t.add(p0(13, "ExposureCompensation", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return com.gdxsoft.easyweb.exiftool.convert.NikonConverters.printFraction(n.doubleValue() / 3 - 2);
        }));
        t.add(p0(14, "BracketStep", BRACKET_STEP));
        t.add(p0(16, "IntervalLength"));
        t.add(p0(17, "IntervalNumber"));
        t.add(p0(18, "FocalLength", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return String.format("%.1f mm", n.doubleValue() / 256);
        }));
        t.add(p0(19, "FocusDistance", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            double d = n.doubleValue() / 1000;
            return d == 0 ? "inf" : com.gdxsoft.easyweb.exiftool.PerlNum.format(d) + " m";
        }));
        t.add(p0(20, "FlashFired", FLASH_FIRED));
        t.add(p0(21, "MinoltaDate", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            long x = n.longValue();
            return String.format("%04d:%02d:%02d", x >> 16, (x >> 8) & 0xff, x & 0xff);
        }));
        t.add(p0(22, "MinoltaTime", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            long x = n.longValue();
            return String.format("%02d:%02d:%02d", x >> 16, (x >> 8) & 0xff, x & 0xff);
        }));
        t.add(p0(23, "MaxAperture", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return String.format("%.1f", Math.pow(2, (n.doubleValue() - 8) / 16));
        }));
        t.add(p0(26, "FileNumberMemory", FILE_NUMBER_MEMORY));
        t.add(p0(27, "LastFileNumber"));
        t.add(p0(28, "ColorBalanceRed", divide256()));
        t.add(p0(29, "ColorBalanceGreen", divide256()));
        t.add(p0(30, "ColorBalanceBlue", divide256()));
        t.add(p0(33, "Sharpness", SHARPNESS));
        t.add(p0(34, "SubjectProgram", SUBJECT_PROGRAM));
        t.add(p0(35, "FlashExposureComp", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return com.gdxsoft.easyweb.exiftool.convert.NikonConverters.printFraction((n.doubleValue() - 6) / 3);
        }));
        t.add(p0(36, "ISOSetting", ISO_SETTING));
        t.add(p0(37, "MinoltaModelID", MODEL_ID));
        t.add(p0(38, "IntervalMode", INTERVAL_MODE));
        t.add(p0(39, "FolderName", FOLDER_NAME));
        t.add(p0(40, "ColorMode", COLOR_MODE));
        t.add(p0(41, "ColorFilter", v -> v instanceof Number n ? n.longValue() - 3 : v));
        t.add(p0(42, "BWFilter"));
        t.add(p0(43, "InternalFlash", INTERNAL_FLASH));
        t.add(p0(44, "Brightness", v -> {
            if (!(v instanceof Number n)) {
                return v;
            }
            return com.gdxsoft.easyweb.exiftool.PerlNum.format(n.doubleValue() / 8 - 6);
        }));
        t.add(p0(45, "SpotFocusPointX"));
        t.add(p0(46, "SpotFocusPointY"));
        t.add(p0(47, "WideFocusZone", WIDE_FOCUS_ZONE));
        t.add(p0(48, "FocusMode", FOCUS_MODE));
        t.add(p0(49, "FocusArea", FOCUS_AREA));
        t.add(p0(50, "DECPosition", DEC_POSITION));
        return t.register();
    }

    /** CameraSettings tags have PRIORITY 0 (less reliable than EXIF). */
    private static TagInfo p0(int tagId, String name) {
        return TagInfo.builder(tagId, name).priority(0).build();
    }

    private static TagInfo p0(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).priority(0).printConv(conv).build();
    }

    private static ValueConverter divide256() {
        return v -> v instanceof Number n ? com.gdxsoft.easyweb.exiftool.PerlNum.format(n.doubleValue() / 256) : v;
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
