package com.gdxsoft.easyweb.exiftool;

import java.util.HashMap;
import java.util.Map;

/**
 * A tag lookup table (the Java analogue of a Perl {@code %tagInfo} table such as
 * {@code Image::ExifTool::Exif::Main}). Maps tag IDs to {@link TagInfo} and is
 * registered under a stable name so sub-directory references can resolve lazily.
 */
public final class TagTable {

    private static final Map<String, TagTable> REGISTRY = new HashMap<>();

    private final String name;
    private final Map<Integer, TagInfo> tags = new HashMap<>();
    private String defaultFormat;
    private boolean binaryData;
    private int firstEntry;

    public TagTable(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public TagTable add(TagInfo info) {
        tags.put(info.tagId(), info);
        return this;
    }

    /**
     * Mark this table as a binary-data directory (tag IDs are entry indices),
     * with the given default format used when an entry has no format attribute.
     */
    public TagTable binaryData(String defaultFormat) {
        return binaryData(defaultFormat, 0);
    }

    /**
     * As {@link #binaryData(String)} but with the index of the first entry
     * (Perl FIRST_ENTRY): entry offset = (tagId - firstEntry) * format size.
     */
    public TagTable binaryData(String defaultFormat, int firstEntry) {
        this.binaryData = true;
        this.defaultFormat = defaultFormat;
        this.firstEntry = firstEntry;
        return this;
    }

    public boolean isBinaryData() {
        return binaryData;
    }

    /** Default format for binary-data entries, or null. */
    public String defaultFormat() {
        return defaultFormat;
    }

    /** Index of the first binary-data entry (default 0). */
    public int firstEntry() {
        return firstEntry;
    }

    public TagInfo get(int tagId) {
        return tags.get(tagId);
    }

    public Map<Integer, TagInfo> tags() {
        return tags;
    }

    /**
     * Register this table globally so {@link TagInfo.SubDirectory} references can
     * resolve it by name. Returns this table for chaining.
     */
    public TagTable register() {
        synchronized (REGISTRY) {
            TagTable existing = REGISTRY.get(name);
            if (existing == null) {
                REGISTRY.put(name, this);
                return this;
            }
            return existing;
        }
    }

    public static TagTable resolve(String name) {
        synchronized (REGISTRY) {
            return REGISTRY.get(name);
        }
    }
}
