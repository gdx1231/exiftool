package com.gdxsoft.easyweb.exiftool;

/**
 * Converts a raw tag value to its display value.
 * Mirrors the PrintConv stage of the ExifTool value conversion chain
 * (RawConv -> ValueConv -> PrintConv).
 */
@FunctionalInterface
public interface ValueConverter {

    /**
     * Convert a raw value to a display value.
     *
     * @param value raw value (may be a single Number/String/byte[] or an Object[] of values)
     * @return converted display value, never null
     */
    Object convert(Object value);

    /**
     * A converter that passes values through unchanged.
     */
    ValueConverter IDENTITY = value -> value;
}
