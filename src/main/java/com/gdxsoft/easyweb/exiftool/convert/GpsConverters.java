package com.gdxsoft.easyweb.exiftool.convert;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * Converters for GPS tags, ported from {@code Image::ExifTool::GPS}:
 * coordinate conversion (ToDegrees/ToDMS), timestamp conversion, and lookups.
 */
public final class GpsConverters {

    private static final Pattern NUMBER =
        Pattern.compile("[+-]?(?=\\d|\\.\\d)\\d*(?:\\.\\d*)?(?:[Ee][+-]?\\d+)?");

    private GpsConverters() {}

    public static final ValueConverter LAT_REF = lookup(Map.of("N", "North", "S", "South"));
    public static final ValueConverter LON_REF = lookup(Map.of("E", "East", "W", "West"));

    public static final ValueConverter ALTITUDE_REF = lookup(Map.of(
        "0", "Above Sea Level",
        "1", "Below Sea Level",
        "2", "Positive Sea Level (sea-level ref)",
        "3", "Negative Sea Level (sea-level ref)"));

    /** GPSAltitude: "$val m". */
    public static final ValueConverter ALTITUDE = value -> {
        if (value instanceof Number n) {
            return n + " m";
        }
        return value;
    };

    /** GPSVersionID: 'tr/ /./' -- "2 3 0 0" -> "2.3.0.0". */
    public static final ValueConverter VERSION_ID = value ->
        value instanceof String s ? s.replace(' ', '.') : value;

    /**
     * ToDegrees (ValueConv): parse "D M S" (space-joined rational values) into
     * decimal degrees; negative when the value itself is negative.
     */
    public static final ValueConverter TO_DEGREES = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        Matcher m = NUMBER.matcher(s);
        double[] parts = new double[3];
        int n = 0;
        while (m.find() && n < 3) {
            parts[n++] = Double.parseDouble(m.group());
        }
        if (n == 0) {
            return "";
        }
        double deg = parts[0] + ((parts[1]) + parts[2] / 60) / 60;
        if (deg < 0) {
            deg = -deg;
        }
        return deg;
    };

    /**
     * ToDMS (PrintConv, doPrintConv=1, no direction reference):
     * decimal degrees -> "51 deg 30' 36.00\"".
     */
    public static final ValueConverter TO_DMS = value -> {
        if (!(value instanceof Number n)) {
            return value;
        }
        double val = n.doubleValue();
        int d = (int) val;
        double mFloat = (val - d) * 60;
        int m = (int) mFloat;
        double s = (val - d - m / 60.0) * 3600;
        String sStr = String.format("%.2f", s);
        if (Double.parseDouble(sStr) >= 60) {
            sStr = "0.00";
            m++;
            if (m >= 60) {
                m -= 60;
                d++;
            }
        }
        return String.format("%d deg %d' %s\"", d, m, sStr);
    };

    /**
     * ConvertTimeStamp (ValueConv): "10 30 36" -> "10:30:36" (HH:MM:SS[.ss]).
     */
    public static final ValueConverter TIME_STAMP = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        String[] parts = s.trim().split("\\s+");
        double h = parts.length > 0 ? parse(parts[0]) : 0;
        double m = parts.length > 1 ? parse(parts[1]) : 0;
        double sec = parts.length > 2 ? parse(parts[2]) : 0;
        double f = (h * 60 + m) * 60 + sec;
        int hh = (int) (f / 3600);
        f -= hh * 3600;
        int mm = (int) (f / 60);
        f -= mm * 60;
        String ss = String.format("%012.9f", f);
        if (Double.parseDouble(ss) >= 60) {
            ss = "00";
            if (++mm >= 60) {
                mm -= 60;
                hh++;
            }
        } else {
            ss = trimTrailingZeros(ss);
        }
        return String.format("%02d:%02d:%s", hh, mm, ss);
    };

    /** PrintTimeStamp: no-op unless the value has decimal seconds ("10:30:36.000000000"). */
    public static final ValueConverter PRINT_TIME_STAMP = value -> {
        if (!(value instanceof String s)) {
            return value;
        }
        int idx = s.lastIndexOf(':');
        if (idx < 0 || !s.substring(idx + 1).contains(".")) {
            return s;
        }
        return s; // Phase 1: values without decimal seconds pass through unchanged
    };

    private static double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trimTrailingZeros(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        String t = s.replaceAll("0+$", "");
        t = t.endsWith(".") ? t.substring(0, t.length() - 1) : t;
        return t.isEmpty() ? "0" : t;
    }

    private static ValueConverter lookup(Map<String, String> lookup) {
        return new LookupConverter(lookup);
    }
}
