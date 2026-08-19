package com.gdxsoft.easyweb.exiftool.convert;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * PrintConv/ValueConv converters for EXIF tags, ported from
 * {@code Image::ExifTool::Exif} lookup tables and helper functions.
 */
public final class ExifConverters {

    private ExifConverters() {}

    // ------------------------------------------------------------------
    // Lookup tables (PrintConv => \%hash)
    // ------------------------------------------------------------------

    public static final ValueConverter ORIENTATION = lookup(Map.of(
        "1", "Horizontal (normal)",
        "2", "Mirror horizontal",
        "3", "Rotate 180",
        "4", "Mirror vertical",
        "5", "Mirror horizontal and rotate 270 CW",
        "6", "Rotate 90 CW",
        "7", "Mirror horizontal and rotate 90 CW",
        "8", "Rotate 270 CW"));

    public static final ValueConverter COMPRESSION = lookup(Map.ofEntries(
        Map.entry("1", "Uncompressed"),
        Map.entry("2", "CCITT 1D"),
        Map.entry("3", "T4/Group 3 Fax"),
        Map.entry("4", "T6/Group 4 Fax"),
        Map.entry("5", "LZW"),
        Map.entry("6", "JPEG (old-style)"),
        Map.entry("7", "JPEG"),
        Map.entry("8", "Adobe Deflate"),
        Map.entry("9", "JBIG B&W or VC-5"),
        Map.entry("10", "JBIG Color"),
        Map.entry("99", "JPEG"),
        Map.entry("262", "Kodak 262"),
        Map.entry("32766", "NeXt or Sony ARW Compressed 2"),
        Map.entry("32767", "Sony ARW Compressed"),
        Map.entry("32769", "Packed RAW"),
        Map.entry("32770", "Samsung SRW Compressed"),
        Map.entry("32771", "CCIRLEW"),
        Map.entry("32772", "Samsung SRW Compressed 2"),
        Map.entry("32773", "PackBits"),
        Map.entry("32809", "Thunderscan"),
        Map.entry("32867", "Kodak KDC Compressed"),
        Map.entry("32895", "IT8CTPAD"),
        Map.entry("32896", "IT8LW"),
        Map.entry("32897", "IT8MP"),
        Map.entry("32898", "IT8BL"),
        Map.entry("32908", "PixarFilm"),
        Map.entry("32909", "PixarLog"),
        Map.entry("32946", "Deflate"),
        Map.entry("32947", "DCS"),
        Map.entry("33003", "Aperio JPEG 2000 YCbCr"),
        Map.entry("33005", "Aperio JPEG 2000 RGB"),
        Map.entry("34661", "JBIG"),
        Map.entry("34676", "SGILog"),
        Map.entry("34677", "SGILog24"),
        Map.entry("34712", "JPEG 2000"),
        Map.entry("34713", "Nikon NEF Compressed"),
        Map.entry("34715", "JBIG2 TIFF FX"),
        Map.entry("34718", "Microsoft Document Imaging (MDI) Binary Level Codec"),
        Map.entry("34719", "Microsoft Document Imaging (MDI) Progressive Transform Codec"),
        Map.entry("34720", "Microsoft Document Imaging (MDI) Vector"),
        Map.entry("34887", "ESRI Lerc"),
        Map.entry("34892", "Lossy JPEG"),
        Map.entry("34925", "LZMA2"),
        Map.entry("34926", "Zstd (old)"),
        Map.entry("34927", "WebP (old)"),
        Map.entry("34933", "PNG"),
        Map.entry("34934", "JPEG XR"),
        Map.entry("50000", "Zstd"),
        Map.entry("50001", "WebP"),
        Map.entry("50002", "JPEG XL (old)"),
        Map.entry("52546", "JPEG XL"),
        Map.entry("65000", "Kodak DCR Compressed"),
        Map.entry("65535", "Pentax PEF Compressed")));

    public static final ValueConverter PHOTOMETRIC_INTERPRETATION = lookup(Map.ofEntries(
        Map.entry("0", "WhiteIsZero"),
        Map.entry("1", "BlackIsZero"),
        Map.entry("2", "RGB"),
        Map.entry("3", "RGB Palette"),
        Map.entry("4", "Transparency Mask"),
        Map.entry("5", "CMYK"),
        Map.entry("6", "YCbCr"),
        Map.entry("8", "CIELab"),
        Map.entry("9", "ICCLab"),
        Map.entry("10", "ITULab"),
        Map.entry("32803", "Color Filter Array"),
        Map.entry("32844", "Pixar LogL"),
        Map.entry("32845", "Pixar LogLuv"),
        Map.entry("32892", "Sequential Color Filter"),
        Map.entry("34892", "Linear Raw"),
        Map.entry("51177", "Depth Map"),
        Map.entry("52527", "Semantic Mask")));

    public static final ValueConverter LIGHT_SOURCE = lookup(Map.ofEntries(
        Map.entry("0", "Unknown"),
        Map.entry("1", "Daylight"),
        Map.entry("2", "Fluorescent"),
        Map.entry("3", "Tungsten (Incandescent)"),
        Map.entry("4", "Flash"),
        Map.entry("9", "Fine Weather"),
        Map.entry("10", "Cloudy"),
        Map.entry("11", "Shade"),
        Map.entry("12", "Daylight Fluorescent"),
        Map.entry("13", "Day White Fluorescent"),
        Map.entry("14", "Cool White Fluorescent"),
        Map.entry("15", "White Fluorescent"),
        Map.entry("16", "Warm White Fluorescent"),
        Map.entry("17", "Standard Light A"),
        Map.entry("18", "Standard Light B"),
        Map.entry("19", "Standard Light C"),
        Map.entry("20", "D55"),
        Map.entry("21", "D65"),
        Map.entry("22", "D75"),
        Map.entry("23", "D50"),
        Map.entry("24", "ISO Studio Tungsten"),
        Map.entry("25", "Daylight"),
        Map.entry("255", "Other")));

    public static final ValueConverter FLASH = lookup(Map.ofEntries(
        Map.entry("0", "No Flash"),
        Map.entry("1", "Fired"),
        Map.entry("5", "Fired, Return not detected"),
        Map.entry("7", "Fired, Return detected"),
        Map.entry("8", "On, Did not fire"),
        Map.entry("9", "On, Fired"),
        Map.entry("13", "On, Return not detected"),
        Map.entry("15", "On, Return detected"),
        Map.entry("16", "Off, Did not fire"),
        Map.entry("20", "Off, Did not fire, Return not detected"),
        Map.entry("24", "Auto, Did not fire"),
        Map.entry("25", "Auto, Fired"),
        Map.entry("29", "Auto, Fired, Return not detected"),
        Map.entry("31", "Auto, Fired, Return detected"),
        Map.entry("32", "No flash function"),
        Map.entry("48", "Off, No flash function"),
        Map.entry("65", "Fired, Red-eye reduction"),
        Map.entry("69", "Fired, Red-eye reduction, Return not detected"),
        Map.entry("71", "Fired, Red-eye reduction, Return detected"),
        Map.entry("73", "On, Red-eye reduction"),
        Map.entry("77", "On, Red-eye reduction, Return not detected"),
        Map.entry("79", "On, Red-eye reduction, Return detected"),
        Map.entry("80", "Off, Red-eye reduction"),
        Map.entry("88", "Auto, Did not fire, Red-eye reduction"),
        Map.entry("89", "Auto, Fired, Red-eye reduction"),
        Map.entry("93", "Auto, Fired, Red-eye reduction, Return not detected"),
        Map.entry("95", "Auto, Fired, Red-eye reduction, Return detected")));

    public static final ValueConverter YCBCR_POSITIONING = lookup(Map.of(
        "1", "Centered", "2", "Co-sited"));

    public static final ValueConverter SCENE_TYPE = lookup(Map.of(
        "1", "Directly photographed"));

    public static final ValueConverter FILE_SOURCE = lookup(Map.of(
        "1", "Film Scanner",
        "2", "Reflection Print Scanner",
        "3", "Digital Camera",
        "4", "Infrared Scanner"));

    public static final ValueConverter INTEROP_INDEX = lookup(Map.of(
        "R98", "R98 - DCF basic file (sRGB)",
        "R03", "R03 - DCF option file (Adobe RGB)",
        "THM", "THM - DCF thumbnail file"));

    public static final ValueConverter SHARPNESS = lookup(Map.of(
        "0", "Normal", "1", "Soft", "2", "Hard"));

    /** ComponentsConfiguration: split, look up each component, join with ", ". */
    public static final ValueConverter COMPONENTS_CONFIGURATION = new MultiLookupConverter(Map.of(
        "0", "-", "1", "Y", "2", "Cb", "3", "Cr", "4", "R", "5", "G", "6", "B"));

    // ------------------------------------------------------------------
    // Function converters (PrintConv => 'Image::ExifTool::Exif::...')
    // ------------------------------------------------------------------

    /**
     * PrintExposureTime: seconds -> "1/125" for fast exposures, else decimal with
     * one decimal place.
     */
    public static final ValueConverter EXPOSURE_TIME = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double secs = n.doubleValue();
        if (secs < 0.25001 && secs > 0) {
            return String.format("1/%d", (int) (0.5 + 1 / secs));
        }
        String s = String.format("%.1f", secs);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    };

    /**
     * PrintFNumber: round to 1 decimal place, or 2 for values < 1.0.
     */
    public static final ValueConverter F_NUMBER = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double v = n.doubleValue();
        if (v > 0) {
            return String.format(v < 1 ? "%.2f" : "%.1f", v);
        }
        return value;
    };

    /**
     * sprintf("%.1f mm", $val) -- used by FocalLength.
     */
    public static final ValueConverter FOCAL_LENGTH = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.1f mm", n.doubleValue());
    };

    /**
     * ShutterSpeedValue ValueConv: APEX -> seconds via {@code 2**(-val)}.
     */
    public static final ValueConverter SHUTTER_SPEED_APEX = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double apex = n.doubleValue();
        return Math.abs(apex) < 100 ? Math.pow(2, -apex) : 0.0;
    };

    /**
     * ApertureValue ValueConv: APEX -> F number via {@code 2**(val/2)}.
     */
    public static final ValueConverter APERTURE_APEX = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return Math.pow(2, n.doubleValue() / 2);
    };

    /**
     * ApertureValue PrintConv: {@code sprintf("%.1f", $val)}.
     */
    public static final ValueConverter APERTURE_PRINT = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        return String.format("%.1f", n.doubleValue());
    };

    /**
     * undef/binary values -> ISO-8859-1 string with trailing nulls stripped
     * (used by ExifVersion, FlashpixVersion, InteropVersion, ...).
     */
    public static final ValueConverter UNDEF_STRING = value -> {
        if (!(value instanceof byte[] b)) {
            return value;
        }
        int len = b.length;
        while (len > 0 && b[len - 1] == 0) {
            len--;
        }
        return new String(b, 0, len, java.nio.charset.StandardCharsets.ISO_8859_1);
    };

    /**
     * UserComment RawConv (ConvertExifText): 8-byte encoding header + text.
     */
    public static final ValueConverter USER_COMMENT = value -> {
        byte[] b = value instanceof byte[] x ? x
            : value instanceof String s ? s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1) : null;
        if (b == null) {
            return value;
        }
        if (b.length < 8) {
            return new String(b, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        byte[] id = java.util.Arrays.copyOf(b, 8);
        byte[] rest = java.util.Arrays.copyOfRange(b, 8, b.length);
        // "ASCII\0\0\0" or "\0\0\0\0\0\0\0\0" (spaces allowed instead of nulls)
        if (isAsciiHeader(id)) {
            int end = indexOfNull(rest);
            String s = new String(rest, 0, end < 0 ? rest.length : end,
                java.nio.charset.StandardCharsets.ISO_8859_1);
            return s.replaceAll(" +$", "");
        }
        if (startsWith(id, "UNICODE")) {
            return new String(rest, java.nio.charset.StandardCharsets.UTF_16);
        }
        // JIS or unknown header: keep raw bytes as Latin-1
        return new String(b, java.nio.charset.StandardCharsets.ISO_8859_1);
    };

    /**
     * CFAPattern RawConv (DecodeCFAPattern): "width height values..." string.
     * Tries little-endian first, then big-endian (like Perl's byte-order swap).
     */
    public static final ValueConverter CFA_PATTERN_DECODE = value -> {
        if (!(value instanceof byte[] b) || b.length < 4) {
            return value;
        }
        int w = (b[0] & 0xff) | ((b[1] & 0xff) << 8);
        int h = (b[2] & 0xff) | ((b[3] & 0xff) << 8);
        if (2 + w * h > b.length) {
            // try swapped byte order
            w = ((b[0] & 0xff) << 8) | (b[1] & 0xff);
            h = ((b[2] & 0xff) << 8) | (b[3] & 0xff);
        }
        StringBuilder sb = new StringBuilder().append(w).append(' ').append(h);
        // 4 header bytes + w*h pixel bytes
        int end = Math.min(4 + w * h, b.length);
        for (int i = 4; i < end; i++) {
            sb.append(' ').append(b[i] & 0xff);
        }
        return sb.toString();
    };

    /**
     * CFAPattern PrintConv (PrintCFAPattern): "2 2 2 1 1 0" -> "[Blue,Green][Green,Red]".
     */
    public static final ValueConverter CFA_PATTERN_PRINT = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String[] a = s.split(" ");
        if (a.length < 2) {
            return "<truncated data>";
        }
        int w = Integer.parseInt(a[0]);
        int h = Integer.parseInt(a[1]);
        if (w == 0 || h == 0) {
            return "<zero pattern size>";
        }
        int end = 2 + w * h;
        if (end > a.length) {
            return "<invalid pattern size>";
        }
        String[] colors = {"Red", "Green", "Blue", "Cyan", "Magenta", "Yellow", "White"};
        StringBuilder sb = new StringBuilder("[");
        int pos = 2;
        for (;;) {
            int idx = Integer.parseInt(a[pos]);
            sb.append(idx < colors.length ? colors[idx] : "Unknown");
            if (++pos >= end) {
                break;
            }
            if ((pos - 2) % h != 0) {
                sb.append(',');
            } else {
                sb.append("][");
            }
        }
        return sb.append(']').toString();
    };

    private static boolean isAsciiHeader(byte[] id) {
        if (id[0] == 0) {
            // "\0\0\0\0\0\0\0\0" or spaces
            for (byte b : id) {
                if (b != 0 && b != ' ') {
                    return false;
                }
            }
            return true;
        }
        return startsWith(id, "ASCII") && (id[5] == 0 || id[5] == ' ');
    }

    private static boolean startsWith(byte[] id, String prefix) {
        byte[] p = prefix.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        if (id.length < p.length) {
            return false;
        }
        for (int i = 0; i < p.length; i++) {
            if (id[i] != p[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfNull(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    public static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
