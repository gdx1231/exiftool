package com.gdxsoft.easyweb.exiftool;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata describing a single tag, mirroring one entry of the Perl {@code %tagInfo} tables.
 */
public final class TagInfo {

    /** Describes a sub-directory that a pointer tag recurses into. */
    public record SubDirectory(String dirName, TagTable table, int startOffset) {

        public SubDirectory {
            startOffset = startOffset == 0 ? 0 : startOffset;
        }

        public SubDirectory(String dirName, TagTable table) {
            this(dirName, table, 0);
        }
    }

    private final int tagId;
    private final String name;
    private final String writable;
    /** Override format name for reading (the Perl {@code Format} attribute), or null. */
    private final String format;
    private final Map<Integer, String> groups;
    private final ValueConverter valueConv;
    private final ValueConverter printConv;
    private final SubDirectory subDirectory;
    private final boolean protectedTag;
    private final boolean isOffset;
    private final int priority;
    private final boolean prioritySet;

    private TagInfo(Builder b) {
        this.tagId = b.tagId;
        this.name = b.name;
        this.writable = b.writable;
        this.format = b.format;
        this.groups = b.groups == null ? Map.of() : Map.copyOf(b.groups);
        this.valueConv = b.valueConv == null ? ValueConverter.IDENTITY : b.valueConv;
        this.printConv = b.printConv == null ? ValueConverter.IDENTITY : b.printConv;
        this.subDirectory = b.subDirectory;
        this.protectedTag = b.protectedTag;
        this.isOffset = b.isOffset;
        this.priority = b.priority;
        this.prioritySet = b.prioritySet;
    }

    /** Short-form tag: {@code 0x82a6 => 'MDScalePixel'}. */
    public static TagInfo simple(int tagId, String name) {
        return new Builder(tagId, name).build();
    }

    public static Builder builder(int tagId, String name) {
        return new Builder(tagId, name);
    }

    public int tagId() {
        return tagId;
    }

    public String name() {
        return name;
    }

    /** EXIF format name this tag is writable as, or null if read-only. */
    public String writable() {
        return writable;
    }

    /** Override format name used when reading this tag, or null. */
    public String format() {
        return format;
    }

    /** Group families: 0 (File/EXIF...), 1 (IFD0/ExifIFD...), 2 (Camera/Location...). */
    public Map<Integer, String> groups() {
        return groups;
    }

    public ValueConverter valueConv() {
        return valueConv;
    }

    public ValueConverter printConv() {
        return printConv;
    }

    /** Sub-directory this pointer tag recurses into, or null. */
    public SubDirectory subDirectory() {
        return subDirectory;
    }

    public boolean protectedTag() {
        return protectedTag;
    }

    /** True if the value is an offset relative to the directory base (Perl IsOffset). */
    public boolean isOffset() {
        return isOffset;
    }

    public int priority() {
        return priority;
    }

    /** True if the priority was explicitly set on the tag definition. */
    public boolean prioritySet() {
        return prioritySet;
    }

    public static final class Builder {
        private final int tagId;
        private final String name;
        private String writable;
        private String format;
        private Map<Integer, String> groups;
        private ValueConverter valueConv;
        private ValueConverter printConv;
        private SubDirectory subDirectory;
        private boolean protectedTag;
        private boolean isOffset;
        private int priority;
        private boolean prioritySet;

        public Builder(int tagId, String name) {
            this.tagId = tagId;
            this.name = name;
        }

        public Builder writable(String writable) {
            this.writable = writable;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder group(int family, String group) {
            if (groups == null) {
                groups = new HashMap<>();
            }
            groups.put(family, group);
            return this;
        }

        public Builder valueConv(ValueConverter valueConv) {
            this.valueConv = valueConv;
            return this;
        }

        public Builder printConv(ValueConverter printConv) {
            this.printConv = printConv;
            return this;
        }

        public Builder subDirectory(String dirName, TagTable table) {
            this.subDirectory = new SubDirectory(dirName, table, 0);
            return this;
        }

        public Builder subDirectory(String dirName, TagTable table, int startOffset) {
            this.subDirectory = new SubDirectory(dirName, table, startOffset);
            return this;
        }

        public Builder protectedTag(boolean protectedTag) {
            this.protectedTag = protectedTag;
            return this;
        }

        public Builder isOffset(boolean isOffset) {
            this.isOffset = isOffset;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            this.prioritySet = true;
            return this;
        }

        public TagInfo build() {
            return new TagInfo(this);
        }
    }
}
