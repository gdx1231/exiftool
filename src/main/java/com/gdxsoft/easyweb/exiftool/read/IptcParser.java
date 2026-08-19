package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * IPTC IIM (Information Interchange Model) parser, the analogue of
 * {@code ProcessIPTC}. Scans 5-byte dataset headers ({@code 1C record dataset len16})
 * and extracts the common Application Record (record 2) datasets.
 */
public final class IptcParser {

    /** Application Record dataset ID -> tag name. */
    private static final Map<Integer, String> APP_RECORD = Map.ofEntries(
        Map.entry(0, "ApplicationRecordVersion"),
        Map.entry(5, "ObjectName"),
        Map.entry(25, "Keywords"),
        Map.entry(40, "SpecialInstructions"),
        Map.entry(55, "DateCreated"),
        Map.entry(80, "By-line"),
        Map.entry(85, "By-lineTitle"),
        Map.entry(90, "City"),
        Map.entry(92, "Sub-location"),
        Map.entry(95, "Province-State"),
        Map.entry(100, "Country-PrimaryLocationCode"),
        Map.entry(101, "Country-PrimaryLocationName"),
        Map.entry(103, "OriginalTransmissionReference"),
        Map.entry(105, "Headline"),
        Map.entry(110, "Credit"),
        Map.entry(115, "Source"),
        Map.entry(116, "CopyrightNotice"),
        Map.entry(120, "Caption-Abstract"),
        Map.entry(122, "Writer-Editor"));

    private IptcParser() {}

    public static void process(ExifTool et, byte[] data, int start, int length) {
        int pos = start;
        int end = start + length;
        // accumulate repeatable datasets (e.g. Keywords) joined with ", "
        Map<Integer, StringBuilder> multi = new LinkedHashMap<>();
        while (pos + 5 <= end) {
            int id = data[pos] & 0xff;
            int rec = data[pos + 1] & 0xff;
            int tag = data[pos + 2] & 0xff;
            int len = ((data[pos + 3] & 0xff) << 8) | (data[pos + 4] & 0xff);
            pos += 5;
            if (id != 0x1c || pos + len > end) {
                break;
            }
            if (rec == 2 && APP_RECORD.containsKey(tag)) {
                String name = APP_RECORD.get(tag);
                String value;
                if (tag == 0 && len >= 2) {
                    // ApplicationRecordVersion is a big-endian int16u
                    int v = ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
                    value = String.valueOf(v);
                } else {
                    value = decodeText(data, pos, len);
                    if (tag == 55) {
                        // DateCreated: "YYYYMMDD" -> "YYYY:MM:DD" (ExifDate)
                        value = value.replaceFirst("^(\\d{4})(\\d{2})(\\d{2})", "$1:$2:$3");
                    }
                }
                StringBuilder sb = multi.get(tag);
                if (sb == null) {
                    multi.put(tag, new StringBuilder(value));
                } else {
                    sb.append(", ").append(value);
                }
            }
            pos += len;
        }
        for (Map.Entry<Integer, StringBuilder> e : multi.entrySet()) {
            et.foundTag(APP_RECORD.get(e.getKey()), e.getValue().toString(), 1);
        }
    }

    private static String decodeText(byte[] data, int pos, int len) {
        int end = pos + len;
        int limit = end;
        for (int i = pos; i < end; i++) {
            if (data[i] == 0) {
                limit = i;
                break;
            }
        }
        return new String(data, pos, limit - pos, StandardCharsets.ISO_8859_1);
    }
}
