package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * Sigma maker note tables, ported from {@code Image::ExifTool::Sigma}.
 * Layout: 8-byte "SIGMA\0\0\0" header + 2-byte version + IFD (Little-endian,
 * offsets relative to the IFD start at valuePtr + 10).
 */
public final class SigmaTables {

    public static final String NAME = "Image::ExifTool::Sigma::Main";

    /** Strip a "Expo: " / "Cont: " style prefix (Sigma ValueConv). */
    private static ValueConverter stripPrefix(String prefix) {
        return v -> v instanceof String s ? s.replaceFirst("^" + prefix + ":\\s*", "") : v;
    }

    private static final ValueConverter METERING_MODE = new LookupConverter(Map.of(
        "8", "Multi-segment", "A", "Center-weighted average", "C", "Average", "D", "Spot"));

    private SigmaTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);
        t.add(TagInfo.builder(0x0002, "SerialNumber").build());
        t.add(simple(0x0003, "DriveMode"));
        t.add(simple(0x0004, "ResolutionMode"));
        t.add(simple(0x0005, "AFMode"));
        t.add(simple(0x0006, "FocusSetting"));
        t.add(TagInfo.builder(0x0007, "WhiteBalance").priority(2).build());
        t.add(simple(0x0009, "MeteringMode", METERING_MODE));
        t.add(simple(0x000a, "LensFocalRange"));
        t.add(simple(0x000c, "ExposureCompensation", stripPrefix("Expo")));
        t.add(simple(0x000d, "Contrast", stripPrefix("Cont")));
        t.add(simple(0x000e, "Shadow", stripPrefix("Shad")));
        t.add(simple(0x000f, "Highlight", stripPrefix("High")));
        t.add(simple(0x0010, "Saturation", stripPrefix("Satu")));
        t.add(simple(0x0011, "Sharpness", stripPrefix("Shar")));
        t.add(simple(0x0012, "X3FillLight", stripPrefix("Fill")));
        t.add(simple(0x0014, "ColorAdjustment", stripPrefix("CC")));
        t.add(simple(0x0015, "AdjustmentMode"));
        t.add(simple(0x0016, "Quality", stripPrefix("Qual")));
        t.add(simple(0x0017, "Firmware"));
        t.add(simple(0x0018, "Software"));
        t.add(TagInfo.builder(0x0019, "AutoBracket")
            .printConv(v -> v instanceof String s ? s.replaceFirst("^(\\d)of(\\d)$", "$1 of $2") : v)
            .build());
        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }
}
