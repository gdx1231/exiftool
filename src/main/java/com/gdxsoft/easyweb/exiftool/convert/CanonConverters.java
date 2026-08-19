package com.gdxsoft.easyweb.exiftool.convert;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * Converters for Canon maker notes, ported from {@code Image::ExifTool::Canon}
 * (CanonEv, CameraISO, PrintParameter) and the CameraSettings lookup tables.
 */
public final class CanonConverters {

    private CanonConverters() {}

    // ------------------------------------------------------------------
    // Function converters
    // ------------------------------------------------------------------

    /** CanonEv: 1/3-stop encoded EV value -> EV. */
    public static final ValueConverter CANON_EV = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return canonEv(n.longValue());
    };

    /** MaxAperture/MinAperture: exp(CanonEv(v)*ln(2)/2), PrintConv %.2g. */
    public static final ValueConverter CANON_APERTURE_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double ev = canonEv(n.longValue());
        return Math.exp(ev * Math.log(2) / 2);
    };

    public static final ValueConverter TWO_SIG_FIG_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        String s = String.format("%.2g", n.doubleValue());
        // Java's %g keeps trailing zeros ("4.0"); Perl's sprintf strips them
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    };

    /** CameraISO: lookup + EOS formula. */
    public static final ValueConverter CAMERA_ISO = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return cameraIso(n.longValue());
    };

    /** Exif::PrintParameter: 0 -> "Normal", positive -> "+N", large -> negative. */
    public static final ValueConverter PARAMETER = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if (v > 0) {
            if (v > 0xfff0) {
                return v - 0x10000;
            }
            return "+" + v;
        }
        return value;
    };

    /** Sharpness: positive values get a "+" prefix. */
    public static final ValueConverter SHARPNESS = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        return v > 0 ? "+" + v : value;
    };

    /** FileNumber: 1181861 -> "118-1861". */
    public static final ValueConverter FILE_NUMBER = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        String s = String.valueOf(n.longValue());
        return s.replaceFirst("^(\\d+)(\\d{4})$", "$1-$2");
    };

    /** SerialNumber (non-D30/1D models): zero-padded 10 digits. */
    public static final ValueConverter SERIAL_NUMBER_10 = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%010d", n.longValue());
    };

    /** MaxAperture/MinAperture PrintConv "%.2g" then plain value. */

    // ------------------------------------------------------------------
    // CameraSettings lookup tables
    // ------------------------------------------------------------------

    public static final ValueConverter MACRO_MODE = lookup(Map.of(
        "1", "Macro", "2", "Normal"));

    public static final ValueConverter SELF_TIMER = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if (v == 0) {
            return "Off";
        }
        return ((v & 0xfff) / 10.0) + " s" + ((v & 0x4000) != 0 ? ", Custom" : "");
    };

    public static final ValueConverter CANON_QUALITY = lookup(Map.of(
        "-1", "n/a", "1", "Economy", "2", "Normal", "3", "Fine",
        "4", "RAW", "5", "Superfine", "7", "CRAW",
        "130", "Light (RAW)", "131", "Standard (RAW)"));

    public static final ValueConverter CANON_FLASH_MODE = lookup(Map.of(
        "-1", "n/a", "0", "Off", "1", "Auto", "2", "On",
        "3", "Red-eye reduction", "4", "Slow-sync", "5", "Red-eye reduction (Auto)",
        "6", "Red-eye reduction (On)", "16", "External flash"));

    public static final ValueConverter CONTINUOUS_DRIVE = lookup(Map.of(
        "0", "Single", "1", "Continuous", "2", "Movie", "3", "Continuous, Speed Priority",
        "4", "Continuous, Low", "5", "Continuous, High", "6", "Silent Single",
        "8", "Continuous, High+", "9", "Single, Silent", "10", "Continuous, Silent"));

    public static final ValueConverter FOCUS_MODE = lookup(Map.ofEntries(
        Map.entry("0", "One-shot AF"), Map.entry("1", "AI Servo AF"),
        Map.entry("2", "AI Focus AF"), Map.entry("3", "Manual Focus (3)"),
        Map.entry("4", "Single"), Map.entry("5", "Continuous"),
        Map.entry("6", "Manual Focus (6)"), Map.entry("16", "Pan Focus"),
        Map.entry("256", "One-shot AF (Live View)"), Map.entry("257", "AI Servo AF (Live View)"),
        Map.entry("258", "AI Focus AF (Live View)"), Map.entry("512", "Movie Snap Focus"),
        Map.entry("519", "Movie Servo AF")));

    public static final ValueConverter RECORD_MODE = lookup(Map.ofEntries(
        Map.entry("1", "JPEG"), Map.entry("2", "CRW+THM"), Map.entry("3", "AVI+THM"),
        Map.entry("4", "TIF"), Map.entry("5", "TIF+JPEG"), Map.entry("6", "CR2"),
        Map.entry("7", "CR2+JPEG"), Map.entry("9", "MOV"), Map.entry("10", "MP4"),
        Map.entry("11", "CRM"), Map.entry("12", "CR3"), Map.entry("13", "CR3+JPEG"),
        Map.entry("14", "HIF"), Map.entry("15", "CR3+HIF")));

    public static final ValueConverter CANON_IMAGE_SIZE = lookup(Map.ofEntries(
        Map.entry("-1", "n/a"), Map.entry("0", "Large"), Map.entry("1", "Medium"),
        Map.entry("2", "Small"), Map.entry("5", "Medium 1"), Map.entry("6", "Medium 2"),
        Map.entry("7", "Medium 3"), Map.entry("8", "Postcard"), Map.entry("9", "Widescreen"),
        Map.entry("10", "Medium Widescreen"), Map.entry("14", "Small 1"),
        Map.entry("15", "Small 2"), Map.entry("16", "Small 3")));

    public static final ValueConverter EASY_MODE = lookup(Map.ofEntries(
        Map.entry("0", "Full auto"), Map.entry("1", "Manual"), Map.entry("2", "Landscape"),
        Map.entry("3", "Fast shutter"), Map.entry("4", "Slow shutter"), Map.entry("5", "Night"),
        Map.entry("6", "Gray Scale"), Map.entry("7", "Sepia"), Map.entry("8", "Portrait"),
        Map.entry("9", "Sports"), Map.entry("10", "Macro"), Map.entry("11", "Black & White"),
        Map.entry("12", "Pan focus"), Map.entry("13", "Vivid"), Map.entry("14", "Neutral"),
        Map.entry("15", "Flash Off"), Map.entry("16", "Long Shutter"), Map.entry("17", "Super Macro"),
        Map.entry("18", "Foliage"), Map.entry("19", "Indoor"), Map.entry("20", "Fireworks"),
        Map.entry("21", "Beach"), Map.entry("22", "Underwater"), Map.entry("23", "Snow"),
        Map.entry("24", "Kids & Pets"), Map.entry("25", "Night Snapshot"),
        Map.entry("26", "Digital Macro"), Map.entry("27", "My Colors"),
        Map.entry("28", "Movie Snap"), Map.entry("29", "Super Macro 2"),
        Map.entry("30", "Color Accent"), Map.entry("31", "Color Swap"), Map.entry("32", "Aquarium"),
        Map.entry("33", "ISO 3200"), Map.entry("34", "ISO 6400"),
        Map.entry("35", "Creative Light Effect"), Map.entry("36", "Easy"),
        Map.entry("37", "Quick Shot"), Map.entry("38", "Creative Auto"),
        Map.entry("39", "Zoom Blur"), Map.entry("40", "Low Light"), Map.entry("41", "Nostalgic"),
        Map.entry("42", "Super Vivid"), Map.entry("43", "Poster Effect"),
        Map.entry("44", "Face Self-timer"), Map.entry("45", "Smile"),
        Map.entry("46", "Wink Self-timer"), Map.entry("47", "Fisheye Effect"),
        Map.entry("48", "Miniature Effect"), Map.entry("49", "High-speed Burst"),
        Map.entry("50", "Best Image Selection"), Map.entry("51", "High Dynamic Range"),
        Map.entry("52", "Handheld Night Scene"), Map.entry("53", "Movie Digest"),
        Map.entry("54", "Live View Control"), Map.entry("55", "Discreet"),
        Map.entry("56", "Blur Reduction"), Map.entry("57", "Monochrome"),
        Map.entry("58", "Toy Camera Effect"), Map.entry("59", "Scene Intelligent Auto"),
        Map.entry("60", "High-speed Burst HQ"), Map.entry("61", "Smooth Skin"),
        Map.entry("62", "Soft Focus"), Map.entry("68", "Food"),
        Map.entry("84", "HDR Art Standard"), Map.entry("85", "HDR Art Vivid"),
        Map.entry("93", "HDR Art Bold"), Map.entry("257", "Spotlight"),
        Map.entry("258", "Night 2"), Map.entry("259", "Night+"),
        Map.entry("260", "Super Night"), Map.entry("261", "Sunset"),
        Map.entry("263", "Night Scene"), Map.entry("264", "Surface"),
        Map.entry("265", "Low Light 2")));

    public static final ValueConverter DIGITAL_ZOOM = lookup(Map.of(
        "0", "None", "1", "2x", "2", "4x", "3", "Other"));

    public static final ValueConverter METERING_MODE = lookup(Map.of(
        "0", "Default", "1", "Spot", "2", "Average", "3", "Evaluative",
        "4", "Partial", "5", "Center-weighted average"));

    public static final ValueConverter FOCUS_RANGE = lookup(Map.ofEntries(
        Map.entry("0", "Manual"), Map.entry("1", "Auto"), Map.entry("2", "Not Known"),
        Map.entry("3", "Macro"), Map.entry("4", "Very Close"), Map.entry("5", "Close"),
        Map.entry("6", "Middle Range"), Map.entry("7", "Far Range"), Map.entry("8", "Pan Focus"),
        Map.entry("9", "Super Macro"), Map.entry("10", "Infinity")));

    public static final ValueConverter AF_POINT = lookup(Map.of(
        "8205", "Manual AF point selection",
        "12288", "None (MF)",
        "12289", "Auto AF point selection",
        "12290", "Right",
        "12291", "Center",
        "12292", "Left",
        "16385", "Auto AF point selection",
        "16390", "Face Detect"));

    public static final ValueConverter CANON_EXPOSURE_MODE = lookup(Map.of(
        "0", "Easy", "1", "Program AE", "2", "Shutter speed priority AE",
        "3", "Aperture-priority AE", "4", "Manual", "5", "Depth-of-field AE",
        "6", "M-Dep", "7", "Bulb", "8", "Flexible-priority AE"));

    public static final ValueConverter FLASH_MODEL = lookup(Map.ofEntries(
        Map.entry("0", "n/a"), Map.entry("1", "E-TTL"), Map.entry("2", "E-TTL II"),
        Map.entry("3", "TTL"), Map.entry("4", "Auto"), Map.entry("5", "Manual"),
        Map.entry("6", "Multi"), Map.entry("7", "Off"), Map.entry("8", "PC"),
        Map.entry("9", "None"), Map.entry("10", "External")));

    public static final ValueConverter FLASH_BITS = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if (v == 0) {
            return "(none)";
        }
        return NikonConverters.decodeBits(v, Map.of(
            0, "Manual", 1, "TTL", 2, "A-TTL", 3, "E-TTL", 4, "FP sync enabled",
            7, "2nd-curtain sync used", 11, "FP sync used", 13, "Built-in", 14, "External"));
    };

    public static final ValueConverter FOCUS_CONTINUOUS = lookup(Map.of(
        "0", "Single", "1", "Continuous", "8", "Manual"));

    public static final ValueConverter AE_SETTING = lookup(Map.of(
        "0", "Normal AE", "1", "Exposure Compensation", "2", "AE Lock",
        "3", "AE Lock + Exposure Comp.", "4", "No AE"));

    public static final ValueConverter IMAGE_STABILIZATION = lookup(Map.of(
        "0", "Off", "1", "On", "2", "Shoot Only", "3", "Panning", "4", "Dynamic",
        "256", "Off (2)", "257", "On (2)", "258", "Shoot Only (2)",
        "259", "Panning (2)", "260", "Dynamic (2)"));

    public static final ValueConverter SPOT_METERING_MODE = lookup(Map.of(
        "0", "Center", "1", "AF Point"));

    public static final ValueConverter PHOTO_EFFECT = lookup(Map.of(
        "0", "Off", "1", "Vivid", "2", "Neutral", "3", "Smooth", "4", "Sepia",
        "5", "B&W", "6", "Custom", "100", "My Color Data"));

    public static final ValueConverter MANUAL_FLASH_OUTPUT = lookup(Map.of(
        "0", "n/a", "1280", "Full", "1282", "Medium", "1284", "Low",
        "32767", "n/a"));

    public static final ValueConverter SRAW_QUALITY = lookup(Map.of(
        "0", "n/a", "1", "sRAW1 (mRAW)", "2", "sRAW2 (sRAW)"));

    public static final ValueConverter FOCAL_TYPE = lookup(Map.of(
        "1", "Fixed", "2", "Zoom"));

    public static final ValueConverter SERIAL_NUMBER_FORMAT = lookup(Map.of(
        "0", "Format 1", "1", "Format 2", "2", "Format 3")); // partial

    public static final ValueConverter CANON_MODEL_ID = lookup(Map.ofEntries(
        Map.entry("2147484016", "EOS Digital Rebel / 300D / Kiss Digital"), // 0x80000170
        Map.entry("2147485200", "EOS-1D Mark II"),
        Map.entry("2147485204", "EOS-1Ds Mark II"),
        Map.entry("2147485212", "EOS-1D Mark II N"),
        Map.entry("2147485900", "EOS 5D"),
        Map.entry("2147486038", "EOS 20D"),
        Map.entry("2147486039", "EOS 350D / Digital Rebel XT / Kiss Digital N"),
        Map.entry("2147486068", "EOS 30D"),
        Map.entry("2147486069", "EOS 400D / Digital Rebel XTi / Kiss Digital X"),
        Map.entry("2147486338", "EOS-1D Mark III"),
        Map.entry("2147486339", "EOS 40D"),
        Map.entry("2147486404", "EOS-1Ds Mark III"),
        Map.entry("2147486476", "EOS 450D / Digital Rebel XSi / Kiss Digital X2"),
        Map.entry("2147486486", "EOS 50D"),
        Map.entry("2147486531", "EOS 5D Mark II"),
        Map.entry("2147486540", "EOS 7D"),
        Map.entry("2147486580", "EOS 500D / Rebel T1i / Kiss X3")));

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** CanonEv: decode 1/3-stop encoded EV value. */
    public static double canonEv(long val) {
        long sign = 1;
        if (val < 0) {
            val = -val;
            sign = -1;
        }
        long frac = val & 0x1f;
        val -= frac;
        if (frac == 0x0c) {
            frac = 0x20 / 3;
        } else if (frac == 0x14) {
            frac = 0x40 / 3;
        }
        return sign * (val + frac) / 32.0;
    }

    /** CameraISO: lookup table + EOS formula. */
    public static Object cameraIso(long val) {
        if (val == 0x7fff) {
            return null;
        }
        if ((val & 0x4000) != 0) {
            return (val & 0x3fff) == 0 ? "n/a" : val & 0x3fff;
        }
        return switch ((int) val) {
            case 0 -> "n/a";
            case 14 -> "Auto High";
            case 15 -> "Auto";
            case 16 -> 50L;
            case 17 -> 100L;
            case 18 -> 200L;
            case 19 -> 400L;
            case 20 -> 800L;
            default -> "Unknown (" + val + ")";
        };
    }

    // ------------------------------------------------------------------
    // ShotInfo / FileInfo converters
    // ------------------------------------------------------------------

    /** AutoISO: exp(v/32*ln2)*100, PrintConv %.0f. */
    public static final ValueConverter ISO_EV100_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return Math.exp(n.doubleValue() / 32 * Math.log(2)) * 100;
    };

    /** BaseISO: exp(v/32*ln2)*100/32, PrintConv %.0f. */
    public static final ValueConverter BASE_ISO_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return Math.exp(n.doubleValue() / 32 * Math.log(2)) * 100 / 32;
    };

    public static final ValueConverter ZERO_DECIMAL_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.0f", n.doubleValue());
    };

    /** MeasuredEV: v/32 + 5, PrintConv %.2f. */
    public static final ValueConverter MEASURED_EV_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return n.doubleValue() / 32 + 5;
    };

    public static final ValueConverter TWO_DECIMAL_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.2f", n.doubleValue());
    };

    /** MeasuredEV2: v/8 - 6. */
    public static final ValueConverter MEASURED_EV2_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return n.doubleValue() / 8 - 6;
    };

    /** FocusDistanceUpper/Lower: v/100, "inf" when > 655.345. */
    public static final ValueConverter FOCUS_DISTANCE_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return n.doubleValue() / 100;
    };

    public static final ValueConverter FOCUS_DISTANCE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double v = n.doubleValue();
        return v > 655.345 ? "inf" : String.format("%.2f m", v);
    };

    /** OpticalZoomCode: 8 -> "n/a". */
    public static final ValueConverter OPTICAL_ZOOM_CODE = value ->
        value instanceof Number n && n.longValue() == 8 ? "n/a" : value;

    public static final ValueConverter SLOW_SHUTTER = lookup(Map.of(
        "-1", "n/a", "0", "Off", "1", "Night Scene", "2", "On", "3", "None"));

    public static final ValueConverter AUTO_EXPOSURE_BRACKETING = lookup(Map.of(
        "-1", "On", "0", "Off", "1", "On (shot 1)", "2", "On (shot 2)", "3", "On (shot 3)"));

    public static final ValueConverter CONTROL_MODE = lookup(Map.of(
        "0", "n/a", "1", "Camera Local Control", "3", "Computer Remote Control"));

    public static final ValueConverter CAMERA_TYPE = lookup(Map.of(
        "0", "n/a", "248", "EOS High-end", "250", "Compact",
        "252", "EOS Mid-range", "255", "DV Camera"));

    public static final ValueConverter AUTO_ROTATE = lookup(Map.of(
        "-1", "n/a", "0", "None", "1", "Rotate 90 CW", "2", "Rotate 180", "3", "Rotate 270 CW"));

    public static final ValueConverter ND_FILTER = lookup(Map.of(
        "-1", "n/a", "0", "Off", "1", "On"));

    public static final ValueConverter BRACKET_MODE = lookup(Map.of(
        "0", "Off", "1", "AEB", "2", "FEB", "3", "ISO", "4", "WB"));

    /** Canon WhiteBalance lookup. */
    public static final ValueConverter WHITE_BALANCE = lookup(Map.ofEntries(
        Map.entry("0", "Auto"), Map.entry("1", "Daylight"), Map.entry("2", "Cloudy"),
        Map.entry("3", "Tungsten"), Map.entry("4", "Fluorescent"), Map.entry("5", "Flash"),
        Map.entry("6", "Custom"), Map.entry("7", "Black & White"), Map.entry("8", "Shade"),
        Map.entry("9", "Manual Temperature (Kelvin)"), Map.entry("10", "PC Set1"),
        Map.entry("11", "PC Set2"), Map.entry("12", "PC Set3")));

    private static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
