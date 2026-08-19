package com.gdxsoft.easyweb.exiftool.convert;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ValueConverter;

/**
 * Lookup converter for space/comma separated multi-value strings, mirroring the
 * Perl {@code OTHER} handlers that {@code split /,?\s+/}, look up each element
 * and rejoin with ", " (e.g. ComponentsConfiguration).
 */
public final class MultiLookupConverter implements ValueConverter {

    private final Map<String, String> lookup;

    public MultiLookupConverter(Map<String, String> lookup) {
        this.lookup = Map.copyOf(lookup);
    }

    @Override
    public Object convert(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return "";
        }
        String[] parts = s.split("[, ]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String v = lookup.get(p);
            sb.append(v != null ? v : "Err (" + p + ")");
        }
        return sb.toString();
    }
}
