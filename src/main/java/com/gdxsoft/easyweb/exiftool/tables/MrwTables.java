package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * Minolta MRW PRD (Raw Picture Dimensions) block, ported from
 * {@code Image::ExifTool::MinoltaRaw::PRD}. Big-endian binary data.
 */
public final class MrwTables {

    private static final ValueConverter STORAGE_METHOD = new LookupConverter(Map.of(
        "82", "Padded", "89", "Linear"));

    private static final ValueConverter BAYER_PATTERN = new LookupConverter(Map.of(
        "1", "RGGB", "4", "RGBG"));

    private MrwTables() {}

    public static TagTable prd() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private static TagTable build() {
        TagTable t = new TagTable("Image::ExifTool::MinoltaRaw::PRD");
        t.binaryData("int8u");
        t.add(TagInfo.builder(0, "FirmwareID").format("string[8]").build());
        t.add(TagInfo.builder(8, "SensorHeight").format("int16u").build());
        t.add(TagInfo.builder(10, "SensorWidth").format("int16u").build());
        t.add(TagInfo.builder(12, "ImageHeight").format("int16u").build());
        t.add(TagInfo.builder(14, "ImageWidth").format("int16u").build());
        t.add(TagInfo.builder(16, "RawDepth").build());
        t.add(TagInfo.builder(17, "BitDepth").build());
        t.add(TagInfo.builder(18, "StorageMethod").printConv(STORAGE_METHOD).build());
        t.add(TagInfo.builder(23, "BayerPattern").printConv(BAYER_PATTERN).build());
        return t.register();
    }
}
