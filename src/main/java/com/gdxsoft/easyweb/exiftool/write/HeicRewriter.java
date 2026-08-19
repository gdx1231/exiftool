package com.gdxsoft.easyweb.exiftool.write;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.read.Binary;
import com.gdxsoft.easyweb.exiftool.read.Box;

/**
 * HEIC/AVIF rewriter: updates the embedded EXIF item referenced by the meta
 * iloc box. The item payload is "length(4) + 'Exif\0\0' + TIFF" stored in the
 * mdat box; iloc extent offsets are absolute file offsets.
 *
 * <p>Two strategies: in-place replacement when the rebuilt TIFF fits, or an
 * insertion (shifting all subsequent item data) when it grows. mdat size and
 * the affected iloc offsets/lengths are updated accordingly.
 */
public final class HeicRewriter {

    private HeicRewriter() {}

    /** One iloc item with the field positions inside the iloc box for updates. */
    private static final class Item {
        int id;
        long base;
        long extentOffset;
        long extentLength;
        int basePos;    // position of the base field in the file
        int offsetPos;  // position of the extent offset field in the file
        int lengthPos;  // position of the extent length field in the file
        long absPos() {
            return base + extentOffset;
        }
    }

    public static byte[] write(byte[] data, Map<String, Object> updates) {
        Box meta = firstTopLevel(data, "meta");
        if (meta == null) {
            return data;
        }
        Box iloc = meta.findChild(data, "iloc");
        if (iloc == null) {
            return data;
        }
        int d = iloc.dataStart;
        if (d + 8 > iloc.end()) {
            return data;
        }
        int offSize = (data[d + 4] >> 4) & 0xf;
        int lenSize = data[d + 4] & 0xf;
        int baseSize = (data[d + 5] >> 4) & 0xf;
        if (offSize != 4 || lenSize != 4) {
            return data; // unsupported iloc layout
        }
        int p = d + 6;
        int itemCount = Binary.get16u(data, p, ByteOrder.BIG_ENDIAN);
        p += 2;
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < itemCount && p + 12 <= iloc.end(); i++) {
            Item item = new Item();
            item.id = Binary.get16u(data, p, ByteOrder.BIG_ENDIAN);
            p += 2;
            p += 2; // data reference index
            item.basePos = p;
            if (baseSize > 0) {
                item.base = readUInt(data, p, baseSize);
                p += baseSize;
            }
            int extentCount = Binary.get16u(data, p, ByteOrder.BIG_ENDIAN); // fixed 2 bytes
            p += 2;
            for (int e = 0; e < extentCount && p + offSize + lenSize <= iloc.end(); e++) {
                item.offsetPos = p;
                item.extentOffset = readUInt(data, p, offSize);
                p += offSize;
                item.lengthPos = p;
                item.extentLength = readUInt(data, p, lenSize);
                p += lenSize;
            }
            items.add(item);
        }

        // find the Exif item (payload contains "Exif")
        Item exif = null;
        for (Item it : items) {
            if (it.extentLength >= 4 && it.absPos() + 12 <= data.length
                && hasExifMarker(data, (int) it.absPos(), (int) Math.min(it.extentLength, 12))) {
                exif = it;
                break;
            }
        }
        if (exif == null) {
            return data;
        }
        byte[] newPayload = rebuildItem(data, (int) exif.absPos(), (int) exif.extentLength, updates);
        if (newPayload == null) {
            return data;
        }
        int itemData = (int) exif.absPos();
        if (newPayload.length <= exif.extentLength) {
            return inPlace(data, itemData, (int) exif.extentLength, newPayload, exif);
        }
        return grow(data, items, exif, itemData, (int) exif.extentLength, newPayload);
    }

    /** In-place replacement: overwrite the payload and fix the iloc length. */
    private static byte[] inPlace(byte[] data, int itemData, int oldLen, byte[] newPayload, Item exif) {
        byte[] out = data.clone();
        System.arraycopy(newPayload, 0, out, itemData, newPayload.length);
        for (int j = newPayload.length; j < oldLen; j++) {
            out[itemData + j] = 0;
        }
        writeUInt(out, exif.lengthPos, newPayload.length, 4);
        return out;
    }

    /**
     * Insertion: the rebuilt payload is larger, so all item data after the old
     * Exif item shifts; mdat size and affected iloc offsets are updated.
     */
    private static byte[] grow(byte[] data, List<Item> items, Item exif,
        int itemData, int oldLen, byte[] newPayload) {
        int delta = newPayload.length - oldLen;
        long oldEnd = itemData + oldLen;
        byte[] out = new byte[data.length + delta];
        System.arraycopy(data, 0, out, 0, itemData);
        System.arraycopy(newPayload, 0, out, itemData, newPayload.length);
        System.arraycopy(data, (int) oldEnd, out, itemData + newPayload.length,
            data.length - (int) oldEnd);
        // update the Exif item length
        writeUInt(out, exif.lengthPos, newPayload.length, 4);
        // shift subsequent items' extent offsets
        for (Item it : items) {
            if (it.absPos() >= oldEnd) {
                writeUInt(out, it.offsetPos, it.extentOffset + delta, 4);
            }
        }
        // update the mdat box size (find the mdat box containing the item data)
        Box mdat = findMdatContaining(data, itemData);
        if (mdat != null) {
            int mdatSizePos = mdat.offset;
            long newSize = Binary.get32u(out, mdatSizePos, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            newSize += delta;
            out[mdatSizePos] = (byte) (newSize >> 24);
            out[mdatSizePos + 1] = (byte) (newSize >> 16);
            out[mdatSizePos + 2] = (byte) (newSize >> 8);
            out[mdatSizePos + 3] = (byte) newSize;
        }
        return out;
    }

    private static Box findMdatContaining(byte[] data, int absPos) {
        int pos = 0;
        while (pos + 8 <= data.length) {
            Box b = Box.read(data, pos);
            if (b == null) {
                break;
            }
            if ("mdat".equals(b.type) && absPos >= b.offset && absPos < b.end()) {
                return b;
            }
            pos = b.end();
        }
        return null;
    }

    /** True if "Exif" appears within the first {@code len} bytes at {@code p}. */
    private static boolean hasExifMarker(byte[] data, int p, int len) {
        for (int i = p; i + 4 <= p + len; i++) {
            if (data[i] == 'E' && data[i + 1] == 'x' && data[i + 2] == 'i' && data[i + 3] == 'f') {
                return true;
            }
        }
        return false;
    }

    /** Rebuild the item payload: "length(4) + Exif\0\0 + TIFF". */
    private static byte[] rebuildItem(byte[] data, int itemData, int extentLength, Map<String, Object> updates) {
        int p = itemData;
        readUInt(data, p, 4); // header length (usually 6)
        p += 4;
        int tiffStart = -1;
        for (int i = p; i + 4 <= itemData + extentLength; i++) {
            if ((data[i] == 'I' && data[i + 1] == 'I' && data[i + 2] == 42 && data[i + 3] == 0)
                || (data[i] == 'M' && data[i + 1] == 'M' && data[i + 2] == 0 && data[i + 3] == 42)) {
                tiffStart = i;
                break;
            }
        }
        if (tiffStart < 0) {
            return null;
        }
        byte[] tiff = Arrays.copyOfRange(data, tiffStart, itemData + extentLength);
        byte[] newTiff = TiffRewriter.rewrite(tiff, orderOf(tiff), updates);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUInt(new byte[4], 0, 6, 4);
        byte[] head = {0, 0, 0, 6, 'E', 'x', 'i', 'f', 0, 0};
        out.write(head, 0, head.length);
        out.write(newTiff, 0, newTiff.length);
        return out.toByteArray();
    }

    private static ByteOrder orderOf(byte[] tiff) {
        return tiff.length >= 2 && tiff[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    private static long readUInt(byte[] data, int p, int size) {
        long v = 0;
        for (int i = 0; i < size; i++) {
            v = (v << 8) | (data[p + i] & 0xff);
        }
        return v;
    }

    private static void writeUInt(byte[] out, int p, long v, int size) {
        for (int i = size - 1; i >= 0; i--) {
            out[p + i] = (byte) v;
            v >>= 8;
        }
    }

    private static Box firstTopLevel(byte[] data, String type) {
        int pos = 0;
        while (pos + 8 <= data.length) {
            Box b = Box.read(data, pos);
            if (b == null) {
                break;
            }
            if (type.equals(b.type)) {
                return b;
            }
            pos = b.end();
        }
        return null;
    }
}
