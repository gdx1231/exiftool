package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * Casio maker note tables, ported from {@code Image::ExifTool::Casio}.
 * Plain IFD starting at the value (no header); offsets relative to the
 * EXIF TIFF header.
 */
public final class CasioTables {

    public static final String NAME = "Image::ExifTool::Casio::Main";

    private static final ValueConverter RECORDING_MODE = lookup(Map.of(
        "1", "Single Shutter", "2", "Panorama", "3", "Night Scene", "4", "Portrait",
        "5", "Landscape", "7", "Panorama", "10", "Night Scene", "15", "Portrait",
        "16", "Landscape"));

    private static final ValueConverter QUALITY = lookup(Map.of(
        "1", "Economy", "2", "Normal", "3", "Fine"));

    private static final ValueConverter FOCUS_MODE = lookup(Map.of(
        "2", "Macro", "3", "Auto", "4", "Manual", "5", "Infinity", "7", "Spot AF"));

    private static final ValueConverter FLASH_MODE = lookup(Map.of(
        "1", "Auto", "2", "On", "3", "Off", "4", "Red-eye Reduction", "5", "Red-eye Reduction"));

    private static final ValueConverter FLASH_INTENSITY = lookup(Map.of(
        "11", "Weak", "12", "Low", "13", "Normal", "14", "High", "15", "Strong"));

    private static final ValueConverter WHITE_BALANCE = lookup(Map.of(
        "1", "Auto", "2", "Tungsten", "3", "Daylight", "4", "Fluorescent",
        "5", "Shade", "129", "Manual"));

    private static final ValueConverter DIGITAL_ZOOM = lookup(Map.of(
        "65536", "Off", "65537", "2x", "104857", "1.6x", "131072", "2x", "209715", "3.2x"));

    private static final ValueConverter LEVEL = lookup(Map.of(
        "0", "Normal", "1", "Soft", "2", "Hard", "16", "Normal"));

    private CasioTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);
        t.add(simple(0x0001, "RecordingMode", RECORDING_MODE));
        t.add(simple(0x0002, "Quality", QUALITY));
        t.add(simple(0x0003, "FocusMode", FOCUS_MODE));
        t.add(simple(0x0004, "FlashMode", FLASH_MODE));
        t.add(simple(0x0005, "FlashIntensity", FLASH_INTENSITY));
        t.add(TagInfo.builder(0x0006, "ObjectDistance")
            .printConv(v -> v instanceof Number n ? com.gdxsoft.easyweb.exiftool.PerlNum.format(n.doubleValue() / 1000) + " m" : v)
            .build());
        t.add(simple(0x0007, "WhiteBalance", WHITE_BALANCE));
        t.add(simple(0x000a, "DigitalZoom", DIGITAL_ZOOM));
        t.add(simple(0x000b, "Sharpness", LEVEL));
        t.add(simple(0x000c, "Contrast", LEVEL));
        t.add(simple(0x000d, "Saturation", LEVEL));
        t.add(simple(0x0014, "ISO"));
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
