package com.gdxsoft.easyweb.exiftool.read;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifFormat;
import com.gdxsoft.easyweb.exiftool.ExifTool;
import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;

/**
 * Binary-data directory parser, the analogue of {@code ProcessBinaryData}.
 * Tag IDs are byte offsets into a fixed binary block; each entry's format
 * (e.g. "string[4]", "int16u") selects the decode, and a table-level default
 * format applies when an entry has none.
 */
public final class BinaryDataParser {

    private static final Pattern FORMAT_SPEC = Pattern.compile("^(\\w+)(?:\\[(\\d+)\\])?$");

    private BinaryDataParser() {}

    /** Process all known tags in the binary block starting at {@code base}. */
    public static void process(ExifTool et, byte[] data, int base, ByteOrder order, TagTable table, int length) {
        process(et, data, base, order, table, length, null, null);
    }

    /**
     * Process a binary block with group membership (family-0 and family-1 groups).
     */
    public static void process(ExifTool et, byte[] data, int base, ByteOrder order, TagTable table, int length,
        String group0, String group1) {
        // the entry increment is the table's default format size (Perl $increment)
        ExifFormat incrementFormat = ExifFormat.fromName(table.defaultFormat());
        if (incrementFormat == ExifFormat.NONE) {
            return;
        }
        int increment = incrementFormat.size();
        for (TagInfo info : table.tags().values()) {
            String spec = info.format() != null ? info.format() : table.defaultFormat();
            if (spec == null) {
                continue;
            }
            FormatSpec fs = parse(spec);
            if (fs == null) {
                continue;
            }
            // entry offset = tagId * increment (FIRST_ENTRY does not affect offsets)
            int entryOffset = info.tagId() * increment;
            if (entryOffset + (long) fs.count * fs.format.size() > length) {
                continue; // entry lies outside the directory data
            }
            Object raw = ValueReader.readValue(data, base + entryOffset, fs.format, fs.count, order);
            if (raw == null) {
                continue;
            }
            Object v = info.valueConv().convert(raw);
            if (v == null) {
                continue; // RawConv returned undef: tag is not recorded
            }
            Object display = info.printConv().convert(v);
            int priority = info.prioritySet() ? info.priority() : 1;
            if (group0 != null) {
                et.foundTag(info.name(), raw, display, priority, group0, group1);
            } else {
                et.foundTag(info.name(), raw, display, priority);
            }
        }
    }

    private static FormatSpec parse(String spec) {
        Matcher m = FORMAT_SPEC.matcher(spec);
        if (!m.matches()) {
            return null;
        }
        ExifFormat format = ExifFormat.fromName(m.group(1));
        if (format == ExifFormat.NONE) {
            return null;
        }
        int count = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
        return new FormatSpec(format, count);
    }

    private record FormatSpec(ExifFormat format, int count) {}
}
