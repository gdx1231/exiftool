package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;

/**
 * ISO Base Media File Format (ISO BMFF) box scanner: parses the top-level box
 * structure used by HEIC, MP4, MOV and related formats. Boxes are addressed by
 * absolute offset with a fixed 8-byte header (size + type).
 */
public final class Box {

    public final int size;
    public final String type;
    /** Offset of the box header (size+type). */
    public final int offset;
    /** Offset of the box payload (after the 8-byte header). */
    public final int dataStart;

    Box(int size, String type, int offset, int dataStart) {
        this.size = size;
        this.type = type;
        this.offset = offset;
        this.dataStart = dataStart;
    }

    /** End offset (exclusive) of this box in the file. */
    public int end() {
        return offset + size;
    }

    /**
     * Read a box header at {@code offset}. Returns null if there is no valid
     * header (truncated data or padding).
     */
    public static Box read(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            return null;
        }
        long size = Binary.get32u(data, offset, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
        String type = new String(data, offset + 4, 4, StandardCharsets.ISO_8859_1);
        if (size == 1) {
            // 64-bit extended size
            if (offset + 16 > data.length) {
                return null;
            }
            long size64 = Binary.get64u(data, offset + 8, ByteOrder.BIG_ENDIAN);
            if (size64 > Integer.MAX_VALUE || size64 < 16) {
                return null;
            }
            return new Box((int) size64, type, offset, offset + 16);
        }
        if (size < 8 || size == 0) {
            return null; // size 0 extends to EOF, unsupported here
        }
        if (offset + size > data.length) {
            return null;
        }
        return new Box((int) size, type, offset, offset + 8);
    }

    /** True if the box type is a container (children follow the header). */
    public static boolean isContainer(String type) {
        return switch (type) {
            case "meta", "moov", "trak", "mdia", "minf", "stbl", "udta",
                "iprp", "ipco", "iinf", "pitm", "dinf", "edts", "gmhd", "wave",
                "ilst", "tref", "mvex", "moof", "traf" -> true;
            default -> false;
        };
    }

    /**
     * Find the first child box of the given type inside this container box.
     * Returns null if not found.
     */
    public Box findChild(byte[] data, String childType) {
        if (!isContainer(type)) {
            return null;
        }
        int pos = dataStart;
        // meta is a full box: version(1) + flags(3) precede the children
        if ("meta".equals(type)) {
            pos += 4;
        }
        while (pos + 8 <= end() && pos + 8 <= data.length) {
            Box child = read(data, pos);
            if (child == null || child.end() > end()) {
                break;
            }
            if (childType.equals(child.type)) {
                return child;
            }
            pos = child.end();
        }
        return null;
    }
}
