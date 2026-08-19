package com.gdxsoft.easyweb.exiftool;

/**
 * EXIF data format types, mirroring Exif.pm {@code @formatSize} / {@code @formatName} / {@code %formatNumber}.
 */
public enum ExifFormat {

    NONE(0, 0, "none"),
    INT8U(1, 1, "int8u"),
    STRING(2, 1, "string"),
    INT16U(3, 2, "int16u"),
    INT32U(4, 4, "int32u"),
    RATIONAL64U(5, 8, "rational64u"),
    INT8S(6, 1, "int8s"),
    UNDEF(7, 1, "undef"),
    INT16S(8, 2, "int16s"),
    INT32S(9, 4, "int32s"),
    RATIONAL64S(10, 8, "rational64s"),
    FLOAT(11, 4, "float"),
    DOUBLE(12, 8, "double"),
    IFD(13, 4, "ifd"),
    UNICODE(14, 2, "unicode"),
    COMPLEX(15, 8, "complex"),
    INT64U(16, 8, "int64u"),
    INT64S(17, 8, "int64s"),
    IFD64(18, 8, "ifd64"),
    UTF8(129, 1, "utf8"); // Exif 3.0

    private final int code;
    private final int size;
    private final String name;

    ExifFormat(int code, int size, String name) {
        this.code = code;
        this.size = size;
        this.name = name;
    }

    /** EXIF format code as stored in the IFD entry (2 bytes). */
    public int code() {
        return code;
    }

    /** Byte size of a single value of this format. */
    public int size() {
        return size;
    }

    /** EXIF format name (e.g. "int16u"). */
    public String formatName() {
        return name;
    }

    /** Look up a format by its EXIF code; returns {@link #NONE} for unknown codes. */
    public static ExifFormat fromCode(int code) {
        for (ExifFormat f : values()) {
            if (f.code == code) {
                return f;
            }
        }
        return NONE;
    }

    /** Look up a format by its name (e.g. "int16u"); returns {@link #NONE} if not found. */
    public static ExifFormat fromName(String name) {
        for (ExifFormat f : values()) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        return NONE;
    }
}
