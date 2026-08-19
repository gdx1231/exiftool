package com.gdxsoft.easyweb.exiftool.tables;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;

/**
 * PrintIM table, ported from {@code Image::ExifTool::PrintIM}. The data block
 * starts with a 7-byte "PrintIM" header; PrintIMVersion lives at offset 8.
 */
public final class PrintIMTables {

    public static final String NAME = "Image::ExifTool::PrintIM::Main";

    private PrintIMTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);
        t.binaryData("int8u");
        t.add(TagInfo.builder(8, "PrintIMVersion").format("string[4]").build());
        return t.register();
    }
}
