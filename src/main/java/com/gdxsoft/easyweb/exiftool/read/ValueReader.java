package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifFormat;

/**
 * Value decoding shared by the EXIF IFD parser and binary-data directories.
 * Mirrors {@code ReadValue} in scalar context: single values return a
 * Number/String/byte[]; multi-value numeric formats return a space-joined
 * String (like Perl's {@code join(' ', @vals)}).
 */
public final class ValueReader {

    private ValueReader() {}

    public static Object readValue(byte[] data, int absOffset, ExifFormat format, int count, ByteOrder order) {
        if (count == 0 || absOffset < 0) {
            return "";
        }
        int avail = data.length - absOffset;
        if (avail <= 0) {
            return null;
        }
        long total = (long) count * format.size();
        if (total > avail) {
            count = (int) (avail / format.size()); // shorten count if necessary
            if (count < 1) {
                return null;
            }
        }
        int size = format.size();
        switch (format) {
            case STRING: {
                int end = absOffset;
                int limit = absOffset + count * size;
                while (end < limit && data[end] != 0) {
                    end++;
                }
                return new String(data, absOffset, end - absOffset, StandardCharsets.ISO_8859_1);
            }
            case UTF8: {
                int end = absOffset;
                int limit = absOffset + count * size;
                while (end < limit && data[end] != 0) {
                    end++;
                }
                return new String(data, absOffset, end - absOffset, StandardCharsets.UTF_8);
            }
            case UNDEF:
            case UNICODE:
            case COMPLEX:
                return Arrays.copyOfRange(data, absOffset, absOffset + count * size);
            case INT8U:
                return readInts(data, absOffset, count, size, false, order);
            case INT8S:
                return readInts(data, absOffset, count, size, true, order);
            case INT16U:
                return readInts(data, absOffset, count, size, false, order);
            case INT16S:
                return readInts(data, absOffset, count, size, true, order);
            case INT32U:
            case IFD:
                return readInts(data, absOffset, count, size, false, order);
            case INT32S:
                return readInts(data, absOffset, count, size, true, order);
            case INT64U:
                return readLongs(data, absOffset, count, order);
            case INT64S:
            case IFD64:
                return readLongs(data, absOffset, count, order);
            case FLOAT:
                return readFloats(data, absOffset, count, order);
            case DOUBLE:
                return readDoubles(data, absOffset, count, order);
            case RATIONAL64U:
                return readRationals(data, absOffset, count, false, order);
            case RATIONAL64S:
                return readRationals(data, absOffset, count, true, order);
            default:
                return Arrays.copyOfRange(data, absOffset, absOffset + count * size);
        }
    }

    private static Object readInts(byte[] data, int absOffset, int count, int size, boolean signed, ByteOrder order) {
        if (count == 1) {
            long v = readInt(data, absOffset, size, order);
            return signed ? toSigned(v, size) : v;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            long v = readInt(data, absOffset + i * size, size, order);
            sb.append(signed ? toSigned(v, size) : v);
        }
        return sb.toString();
    }

    private static long readInt(byte[] data, int absOffset, int size, ByteOrder order) {
        return switch (size) {
            case 1 -> (data[absOffset] & 0xff);
            case 2 -> Binary.get16u(data, absOffset, order);
            default -> Binary.get32u(data, absOffset, order) & 0xffffffffL;
        };
    }

    private static Object readLongs(byte[] data, int absOffset, int count, ByteOrder order) {
        if (count == 1) {
            return Binary.get64u(data, absOffset, order);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(Binary.get64u(data, absOffset + i * 8, order));
        }
        return sb.toString();
    }

    private static Object readFloats(byte[] data, int absOffset, int count, ByteOrder order) {
        if (count == 1) {
            return (double) Binary.getFloat(data, absOffset, order);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(Binary.getFloat(data, absOffset + i * 4, order));
        }
        return sb.toString();
    }

    private static Object readDoubles(byte[] data, int absOffset, int count, ByteOrder order) {
        if (count == 1) {
            return Binary.getDouble(data, absOffset, order);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            // Perl stringifies doubles with 15 significant digits (not %.10g)
            sb.append(format15(Binary.getDouble(data, absOffset + i * 8, order)));
        }
        return sb.toString();
    }

    private static String format15(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        String s = String.format("%.15g", d);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

    private static Object readRationals(byte[] data, int absOffset, int count, boolean signed, ByteOrder order) {
        double[] vals = new double[count];
        for (int i = 0; i < count; i++) {
            int off = absOffset + i * 8;
            long num;
            long den;
            if (signed) {
                num = Binary.get32s(data, off, order);
                den = Binary.get32s(data, off + 4, order);
            } else {
                num = Binary.get32u(data, off, order) & 0xffffffffL;
                den = Binary.get32u(data, off + 4, order) & 0xffffffffL;
            }
            vals[i] = den == 0 ? Double.NaN : (double) num / den;
        }
        if (count == 1) {
            return vals[0];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(com.gdxsoft.easyweb.exiftool.PerlNum.format(vals[i]));
        }
        return sb.toString();
    }

    private static long toSigned(long v, int size) {
        return switch (size) {
            case 1 -> (byte) v;
            case 2 -> (short) v;
            default -> (int) v;
        };
    }
}
