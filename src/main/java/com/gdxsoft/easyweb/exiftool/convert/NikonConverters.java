package com.gdxsoft.easyweb.exiftool.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * Converters for Nikon maker notes, ported from {@code Image::ExifTool::Nikon}
 * (FormatString, LensType/ShootingMode DecodeBits handling, LensData/LensInfo
 * conversions) and {@code Image::ExifTool::Exif::PrintFraction}.
 */
public final class NikonConverters {

    private static final Pattern WORD_START_VOWEL = Pattern.compile("\\b([AEIOUY])([A-Z]+)");
    private static final Pattern WORD_WITH_VOWEL = Pattern.compile("\\b([A-Z])([A-Z]*[AEIOUY][A-Z]*)");

    private NikonConverters() {}

    // ------------------------------------------------------------------
    // Nikon::Main (Type 2/3) converters
    // ------------------------------------------------------------------

    /** Default table PRINT_CONV: trim trailing space, fix case of vowel-bearing words. */
    public static final ValueConverter FORMAT_STRING = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String out = s.replaceAll("\\s+$", "");
        if (out.matches("(?s).*[AEIOUY].*")) {
            Matcher m = WORD_START_VOWEL.matcher(out);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + m.group(2).toLowerCase()));
            }
            m.appendTail(sb);
            out = sb.toString();
            out = out.replaceAll("\\bAf\\b", "AF");
            Matcher m2 = WORD_WITH_VOWEL.matcher(out);
            StringBuilder sb2 = new StringBuilder();
            while (m2.find()) {
                m2.appendReplacement(sb2, Matcher.quoteReplacement(m2.group(1) + m2.group(2).toLowerCase()));
            }
            m2.appendTail(sb2);
            out = sb2.toString();
            out = out.replaceAll("\\bRaw\\b", "RAW");
        }
        return out;
    };

    /** 0x0001 MakerNoteVersion ValueConv: binary 4 bytes -> "0210" string. */
    public static final ValueConverter MAKER_NOTE_VERSION_VALUE = value -> {
        if (!(value instanceof byte[] b) || b.length < 1) {
            return value;
        }
        int first = b[0] & 0xff;
        if (first <= 0x09) {
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                sb.append(x & 0xff);
            }
            return sb.toString();
        }
        return new String(b, 0, b.length, java.nio.charset.StandardCharsets.ISO_8859_1);
    };

    /** 0x0001 MakerNoteVersion PrintConv: "0210" -> "2.10". */
    public static final ValueConverter MAKER_NOTE_VERSION_PRINT = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String out = s.replaceFirst("^(\\d{2})", "$1.");
        return out.replaceFirst("^0", "");
    };

    /** 0x0002 ISO PrintConv: "0 200" -> "200", "1 800" -> "Hi 800". */
    public static final ValueConverter ISO_PRINT = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String out = s.replaceFirst("^0 ", "");
        Matcher m = Pattern.compile("^1 (\\d+)").matcher(out);
        return m.matches() ? "Hi " + m.group(1) : out;
    };

    /** 0x0013 ISOSetting PrintConv: strip leading "0 ". */
    public static final ValueConverter ISO_SETTING_PRINT = value ->
        value instanceof String s ? s.replaceFirst("^0 ", "") : value;

    /** undef[4] ValueConv for ProgramShift/ExposureDifference/FlashExposureComp etc. */
    public static final ValueConverter APEX_FROM_3_BYTES = value -> {
        if (!(value instanceof byte[] b) || b.length < 3) {
            return value;
        }
        int a = b[0];
        int b2 = b[1];
        int c = b[2];
        return c != 0 ? a * (double) b2 / c : 0;
    };

    /** 0x000e ExposureDifference PrintConv: "%+.1f" (0 stays 0). */
    public static final ValueConverter EXPOSURE_DIFFERENCE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double v = n.doubleValue();
        return v == 0 ? 0 : String.format("%+.1f", v);
    };

    /** PrintFraction (Exif.pm): -1.6667 -> "-5/3". */
    public static final ValueConverter FRACTION_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return printFraction(n.doubleValue());
    };

    /** 0x0018 FlashExposureBracketValue PrintConv: "%.1f". */
    public static final ValueConverter ONE_DECIMAL_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.1f", n.doubleValue());
    };

    /** 0x0083 LensType PrintConv: DecodeBits with reordering. */
    public static final ValueConverter LENS_TYPE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if (v == 0) {
            return "AF";
        }
        String s = decodeBits(v, Map.of(
            0, "MF", 1, "D", 2, "G", 3, "VR", 4, "1", 5, "FT-1", 6, "E", 7, "AF-P"));
        s = s.replace(",", "");
        s = s.replaceAll("\\bD G\\b", "G");
        if (s.contains(" E")) {
            s = s.replace(" E", "");
            s = s.replaceFirst("^(G )?", "E ");
        }
        if (s.contains(" 1")) {
            s = s.replace(" 1", "");
            s = "1 " + s;
        }
        if (s.startsWith("FT-1 ")) {
            s = s.substring(5) + " FT-1";
        }
        return s;
    };

    /** 0x0089 ShootingMode PrintConv. */
    public static final ValueConverter SHOOTING_MODE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if ((v & 0x87) == 0) {
            return v == 0 ? "Single-Frame" : "Single-Frame, ";
        }
        return decodeBits(v, Map.of(
            0, "Continuous", 1, "Delay", 2, "PC Control", 3, "Self-timer",
            4, "Exposure Bracketing", 5, "Auto ISO", 6, "White-Balance Bracketing",
            7, "IR Control", 8, "D-Lighting Bracketing", 11, "Pre-capture"));
    };

    /** 0x009a SensorPixelSize PrintConv: "7.8 7.8" -> "7.8 x 7.8 um". */
    public static final ValueConverter SENSOR_PIXEL_SIZE_PRINT = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        Matcher m = Pattern.compile("^(\\S+) (\\S+)$").matcher(s);
        return m.matches() ? m.group(1) + " x " + m.group(2) + " um" : value;
    };

    /** 0x0084 Lens PrintConv (PrintLensInfo): "18 70 3.5 4.5" -> "18-70mm f/3.5-4.5". */
    public static final ValueConverter LENS_INFO_PRINT = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String[] vals = s.split(" ");
        if (vals.length != 4) {
            return value;
        }
        int c = 0;
        for (String v : vals) {
            if (isNumber(v) || "inf".equals(v) || "undef".equals(v)) {
                c++;
            }
        }
        if (c != 4) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(vals[0]);
        if (!vals[1].isEmpty() && !vals[1].equals(vals[0])) {
            sb.append('-').append(vals[1]);
        }
        sb.append("mm f/").append(vals[2]);
        if (!vals[3].isEmpty() && !vals[3].equals(vals[2])) {
            sb.append('-').append(vals[3]);
        }
        return sb.toString();
    };

    // ------------------------------------------------------------------
    // LensData / binary sub-directory converters
    // ------------------------------------------------------------------

    /** nikonApertureConversions: ValueConv 2**(v/24), PrintConv %.1f. */
    public static final ValueConverter APERTURE_APEX_NIKON = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return Math.pow(2, n.doubleValue() / 24);
    };

    /** nikonFocalConversions: ValueConv 5*2**(v/24), PrintConv %.1f mm. */
    public static final ValueConverter FOCAL_APEX_NIKON = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return 5 * Math.pow(2, n.doubleValue() / 24);
    };

    /** LensData ExitPupilPosition: ValueConv 2048/v, PrintConv %.1f mm. */
    public static final ValueConverter EXIT_PUPIL_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double v = n.doubleValue();
        return v != 0 ? 2048 / v : v;
    };

    /** PrintConv "%.1f mm". */
    public static final ValueConverter ONE_DECIMAL_MM_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.1f mm", n.doubleValue());
    };

    /** LensData FocusPosition PrintConv: "0x21". */
    public static final ValueConverter HEX_BYTE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("0x%02x", n.longValue() & 0xff);
    };

    /** LensData FocusDistance: ValueConv 0.01*10**(v/40), PrintConv %.2f m / "inf". */
    public static final ValueConverter FOCUS_DISTANCE_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return 0.01 * Math.pow(10, n.doubleValue() / 40);
    };

    public static final ValueConverter FOCUS_DISTANCE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double v = n.doubleValue();
        return v != 0 ? String.format("%.2f m", v) : "inf";
    };

    /** LensData LensFStops: ValueConv v/12, PrintConv %.2f. */
    public static final ValueConverter LENS_FSTOPS_VALUE = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return n.doubleValue() / 12;
    };

    public static final ValueConverter TWO_DECIMAL_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.2f", n.doubleValue());
    };

    // ------------------------------------------------------------------
    // AFInfo converters
    // ------------------------------------------------------------------

    public static final ValueConverter AF_AREA_MODE = lookup(Map.of(
        "0", "Single Area",
        "1", "Dynamic Area",
        "2", "Dynamic Area (closest subject)",
        "3", "Group Dynamic",
        "4", "Single Area (wide)",
        "5", "Dynamic Area (wide)"));

    public static final ValueConverter AF_POINT = lookup(Map.ofEntries(
        Map.entry("0", "Center"), Map.entry("1", "Top"), Map.entry("2", "Bottom"),
        Map.entry("3", "Mid-left"), Map.entry("4", "Mid-right"), Map.entry("5", "Upper-left"),
        Map.entry("6", "Upper-right"), Map.entry("7", "Lower-left"),
        Map.entry("8", "Lower-right"), Map.entry("9", "Far Left"), Map.entry("10", "Far Right")));

    /** AFPointsInFocus (11-point bit mask). */
    public static final ValueConverter AF_POINTS_IN_FOCUS = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        long v = n.longValue();
        if (v == 0) {
            return "(none)";
        }
        if (v == 0x7ff) {
            return "All 11 Points";
        }
        return decodeBits(v, Map.ofEntries(
            Map.entry(0, "Center"), Map.entry(1, "Top"), Map.entry(2, "Bottom"),
            Map.entry(3, "Mid-left"), Map.entry(4, "Mid-right"), Map.entry(5, "Upper-left"),
            Map.entry(6, "Upper-right"), Map.entry(7, "Lower-left"),
            Map.entry(8, "Lower-right"), Map.entry(9, "Far Left"), Map.entry(10, "Far Right")));
    };

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Image::ExifTool::DecodeBits for a single integer value. */
    public static String decodeBits(long value, Map<Integer, String> lookup) {
        List<String> bits = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if ((value & (1L << i)) != 0) {
                String v = lookup != null ? lookup.get(i) : null;
                bits.add(v != null ? v : "[" + i + "]");
            }
        }
        if (bits.isEmpty()) {
            return "(none)";
        }
        return String.join(lookup != null ? ", " : ",", bits);
    }

    /** Exif::PrintFraction. */
    public static String printFraction(double val) {
        val *= 1.00001;
        if (val == 0) {
            return "0";
        }
        if ((int) val / val > 0.999) {
            return String.format("%+d", (int) val);
        }
        if ((int) (val * 2) / (val * 2) > 0.999) {
            return String.format("%+d/2", (int) (val * 2));
        }
        if ((int) (val * 3) / (val * 3) > 0.999) {
            return String.format("%+d/3", (int) (val * 3));
        }
        return String.format("%+.3g", val);
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
