package com.gdxsoft.easyweb.exiftool.write;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifFormat;
import com.gdxsoft.easyweb.exiftool.read.Binary;

/**
 * Encodes display values back to their binary representation for writing
 * (the reverse of {@code ValueReader}). Phase 7 supports string, integer and
 * rational formats.
 */
public final class ValueEncoder {

    private ValueEncoder() {}

    /**
     * Encode a display value into raw bytes of the given format.
     *
     * @return the encoded value bytes (with the format's exact byte size for
     *         numeric formats; strings include the trailing null terminator)
     */
    public static byte[] encode(Object value, ExifFormat format, ByteOrder order) {
        switch (format) {
            case STRING: {
                String s = String.valueOf(value);
                byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
                // TIFF strings are null-terminated
                byte[] out = Arrays.copyOf(b, b.length + 1);
                if (out.length % 2 != 0) {
                    out = Arrays.copyOf(out, out.length + 1); // word-align
                }
                return out;
            }
            case INT8U:
            case INT8S:
                return new byte[]{(byte) parseLong(value)};
            case INT16U:
            case INT16S: {
                long v = parseLong(value);
                if (order == ByteOrder.LITTLE_ENDIAN) {
                    return new byte[]{(byte) v, (byte) (v >> 8)};
                }
                return new byte[]{(byte) (v >> 8), (byte) v};
            }
            case INT32U:
            case INT32S:
            case IFD: {
                long v = parseLong(value);
                if (order == ByteOrder.LITTLE_ENDIAN) {
                    return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
                }
                return new byte[]{(byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v};
            }
            case RATIONAL64U:
            case RATIONAL64S: {
                long[] numDen = parseRational(String.valueOf(value));
                if (order == ByteOrder.LITTLE_ENDIAN) {
                    return new byte[]{(byte) numDen[0], (byte) (numDen[0] >> 8), (byte) (numDen[0] >> 16),
                        (byte) (numDen[0] >> 24), (byte) numDen[1], (byte) (numDen[1] >> 8),
                        (byte) (numDen[1] >> 16), (byte) (numDen[1] >> 24)};
                }
                return new byte[]{(byte) (numDen[0] >> 24), (byte) (numDen[0] >> 16), (byte) (numDen[0] >> 8),
                    (byte) numDen[0], (byte) (numDen[1] >> 24), (byte) (numDen[1] >> 16),
                    (byte) (numDen[1] >> 8), (byte) numDen[1]};
            }
            default:
                throw new IllegalArgumentException("unsupported write format: " + format);
        }
    }

    private static long parseLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value).trim());
    }

    /** Parse "num/den" or a plain number; returns {num, den}. */
    static long[] parseRational(String s) {
        String t = s.trim();
        int slash = t.indexOf('/');
        if (slash > 0) {
            long num = Long.parseLong(t.substring(0, slash).trim());
            long den = Long.parseLong(t.substring(slash + 1).trim());
            return new long[]{num, den == 0 ? 1 : den};
        }
        // plain number: scale to a denominator of 10000 to preserve decimals
        try {
            double d = Double.parseDouble(t);
            if (d == Math.rint(d)) {
                return new long[]{(long) d, 1};
            }
            long den = 10000;
            long num = Math.round(d * den);
            // reduce by gcd
            long g = gcd(num, den);
            return new long[]{num / g, den / g};
        } catch (NumberFormatException e) {
            return new long[]{0, 1};
        }
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return Math.max(1, a);
    }

    /** Convenience: word-align the given byte array (pad with 0). */
    public static byte[] wordAlign(byte[] b) {
        return b.length % 2 == 0 ? b : Arrays.copyOf(b, b.length + 1);
    }

    /** Convenience for reading raw value bytes back (test helper). */
    static long readLong(byte[] data, int off, int size, ByteOrder order) {
        return switch (size) {
            case 1 -> data[off] & 0xff;
            case 2 -> Binary.get16u(data, off, order);
            default -> Binary.get32u(data, off, order) & 0xffffffffL;
        };
    }
}
