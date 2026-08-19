package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.ExifConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * Kodak type 1 maker notes, ported from {@code Image::ExifTool::Kodak::Main}.
 * The value starts with "KDK INFO" (8 bytes) followed by a binary-data
 * directory (Big-endian, int8u increment; FIRST_ENTRY does not affect offsets).
 */
public final class KodakTables {

    public static final String NAME = "Image::ExifTool::Kodak::Main";

    private static final ValueConverter QUALITY = lookup(Map.of(
        "1", "Fine", "2", "Normal"));
    private static final ValueConverter BURST_MODE = lookup(Map.of(
        "0", "Off", "1", "On"));
    private static final ValueConverter SHUTTER_MODE = lookup(Map.of(
        "0", "Auto", "8", "Aperture Priority", "32", "Manual?"));
    private static final ValueConverter METERING_MODE = lookup(Map.of(
        "0", "Multi-segment", "1", "Center-weighted average", "2", "Spot"));
    private static final ValueConverter FOCUS_MODE = lookup(Map.of(
        "0", "Normal", "2", "Macro"));
    private static final ValueConverter WHITE_BALANCE = lookup(Map.of(
        "0", "Auto", "2", "Tungsten", "3", "Daylight", "4", "Fluorescent",
        "5", "Shade", "6", "Daylight Fluorescent"));
    private static final ValueConverter FLASH_MODE = lookup(Map.of(
        "0", "Auto", "1", "Fill Flash", "2", "Off", "3", "Red-Eye",
        "16", "Fill Flash", "32", "Off", "64", "Red-Eye?"));
    private static final ValueConverter FLASH_FIRED = lookup(Map.of(
        "0", "No", "1", "Yes"));
    private static final ValueConverter COLOR_MODE = lookup(Map.of(
        "1", "B&W", "2", "Sepia", "3", "B&W Yellow Filter", "4", "B&W Red Filter",
        "32", "Saturated Color", "64", "Neutral Color", "128", "B&W"));
    private static final ValueConverter SHARPNESS = lookup(Map.of(
        "0", "Normal", "1", "+1", "2", "-1", "3", "+2", "4", "-2", "5", "+3", "6", "-3"));

    private KodakTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);
        t.binaryData("int8u");
        t.add(TagInfo.builder(0x00, "KodakModel").format("string[8]").build());
        t.add(simple(0x09, "Quality", QUALITY));
        t.add(simple(0x0a, "BurstMode", BURST_MODE));
        t.add(TagInfo.builder(0x0c, "KodakImageWidth").format("int16u").build());
        t.add(TagInfo.builder(0x0e, "KodakImageHeight").format("int16u").build());
        t.add(TagInfo.builder(0x10, "YearCreated").format("int16u").build());
        t.add(TagInfo.builder(0x12, "MonthDayCreated")
            .format("int8u[2]")
            .printConv(v -> {
                if (!(v instanceof String s)) {
                    return v;
                }
                String[] a = s.split(" ");
                return String.format("%02d:%02d", Integer.parseInt(a[0]), Integer.parseInt(a[1]));
            })
            .build());
        t.add(TagInfo.builder(0x14, "TimeCreated")
            .format("int8u[4]")
            .printConv(v -> {
                if (!(v instanceof String s)) {
                    return v;
                }
                String[] a = s.split(" ");
                return String.format("%02d:%02d:%02d.%02d",
                    Integer.parseInt(a[0]), Integer.parseInt(a[1]),
                    Integer.parseInt(a[2]), Integer.parseInt(a[3]));
            })
            .build());
        t.add(simple(0x1b, "ShutterMode", SHUTTER_MODE));
        t.add(simple(0x1c, "MeteringMode", METERING_MODE));
        t.add(simple(0x1d, "SequenceNumber"));
        t.add(TagInfo.builder(0x1e, "FNumber")
            .format("int16u")
            .printConv(v -> v instanceof Number n ? String.format("%.2f", n.doubleValue() / 100) : v)
            .build());
        t.add(TagInfo.builder(0x20, "ExposureTime")
            .format("int32u")
            .printConv(v -> v instanceof Number n
                ? ExifConverters.EXPOSURE_TIME.convert(n.doubleValue() / 1e5) : v)
            .build());
        t.add(TagInfo.builder(0x24, "ExposureCompensation")
            .format("int16s")
            .printConv(v -> {
                if (!(v instanceof Number n)) {
                    return v;
                }
                double d = n.doubleValue() / 1000;
                return d > 0 ? "+" + d : com.gdxsoft.easyweb.exiftool.PerlNum.format(d);
            })
            .build());
        t.add(simple(0x38, "FocusMode", FOCUS_MODE));
        t.add(simple(0x40, "WhiteBalance", WHITE_BALANCE));
        t.add(simple(0x5c, "FlashMode", FLASH_MODE));
        t.add(simple(0x5d, "FlashFired", FLASH_FIRED));
        t.add(TagInfo.builder(0x5e, "ISOSetting")
            .format("int16u")
            .printConv(v -> v instanceof Number n && n.longValue() == 0 ? "Auto" : v)
            .build());
        t.add(TagInfo.builder(0x60, "ISO").format("int16u").build());
        t.add(TagInfo.builder(0x62, "TotalZoom")
            .format("int16u")
            .printConv(v -> v instanceof Number n ? com.gdxsoft.easyweb.exiftool.PerlNum.format(n.doubleValue() / 100) : v)
            .build());
        t.add(TagInfo.builder(0x64, "DateTimeStamp")
            .format("int16u")
            .printConv(v -> v instanceof Number n && n.longValue() != 0 ? "Mode " + n : "Off")
            .build());
        t.add(TagInfo.builder(0x66, "ColorMode").format("int16u").printConv(COLOR_MODE).build());
        t.add(TagInfo.builder(0x68, "DigitalZoom")
            .format("int16u")
            .printConv(v -> v instanceof Number n ? com.gdxsoft.easyweb.exiftool.PerlNum.format(n.doubleValue() / 100) : v)
            .build());
        t.add(TagInfo.builder(0x6b, "Sharpness").format("int8s").printConv(SHARPNESS).build());
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
