package com.gdxsoft.easyweb.exiftool;

/**
 * Perl-style number formatting. Rational values are read via RoundFloat
 * ({@code sprintf("%.10g")}), and integral values stringify without a
 * trailing ".0" (300.0 -> "300").
 */
public final class PerlNum {

    private PerlNum() {}

    /**
     * Format a double the way Perl would stringify it (RoundFloat %.10g for
     * non-integral values, plain integer for integral values).
     */
    public static String format(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        // Java's %g keeps trailing zeros ("3.500000000"); C/Perl strip them
        String s = String.format("%.10g", d);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }
}
