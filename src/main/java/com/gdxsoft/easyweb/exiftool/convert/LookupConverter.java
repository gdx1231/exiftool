package com.gdxsoft.easyweb.exiftool.convert;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * A lookup-table converter (the analogue of a Perl {@code PrintConv => \%hash}).
 * Keys are compared by their string form, matching Perl's numeric/string
 * equivalence; values not present in the table pass through unchanged.
 */
public final class LookupConverter implements ValueConverter {

    private final Map<String, String> lookup;

    public LookupConverter(Map<String, String> lookup) {
        this.lookup = Map.copyOf(lookup);
    }

    @Override
    public Object convert(Object value) {
        if (value == null) {
            return null;
        }
        // single-byte undef values (e.g. SceneType, FileSource) compare as their
        // numeric value, like Perl's numeric/string equivalence
        if (value instanceof byte[] b) {
            if (b.length != 1) {
                return value;
            }
            String v = lookup.get(String.valueOf(b[0] & 0xff));
            return v != null ? v : unknown(b[0] & 0xff);
        }
        String v = lookup.get(String.valueOf(value));
        return v != null ? v : unknown(value);
    }

    private static String unknown(Object value) {
        return "Unknown (" + value + ")";
    }
}
