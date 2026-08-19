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
            return addExifItem(data, items, iloc, updates);
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
     * Add a new Exif item to a HEIC that has none: append an infe entry to iinf,
     * an entry to iloc, and a new mdat box at the end of the file. All absolute
     * offsets in iloc shift by the size growth of the meta box.
     */
    private static byte[] addExifItem(byte[] data, List<Item> items, Box iloc, Map<String, Object> updates) {
        if (!hasXmpOrExif(updates)) {
            return data;
        }
        // new item ID = max existing + 1
        int newId = 0;
        for (Item it : items) {
            newId = Math.max(newId, it.id);
        }
        newId++;

        Box meta = firstTopLevel(data, "meta");
        Box iinf = meta.findChild(data, "iinf");
        if (iinf == null) {
            return data;
        }
        // build the new Exif payload: 4-byte header len + "Exif\0\0" + TIFF
        byte[] exifTiff = TiffRewriter.rewrite(minimalTiff(), ByteOrder.BIG_ENDIAN, updates);
        int headerLen = 6;
        byte[] payload = new byte[4 + headerLen + exifTiff.length];
        writeUInt(payload, 0, headerLen, 4);
        System.arraycopy(new byte[]{'E', 'x', 'i', 'f', 0, 0}, 0, payload, 4, headerLen);
        System.arraycopy(exifTiff, 0, payload, 4 + headerLen, exifTiff.length);

        // --- layout math ---
        int ilocOldSize = iloc.size;
        int ilocNewSize = ilocOldSize + 18; // one more 18-byte item entry
        int infeSize = 4 + 4 + 4 + 2 + 2 + 4 + 5; // size + type + fullbox + id + prot + item_type + "Exif\0"
        int shiftAfterIloc = 18;
        int shiftAfterIinf = 18 + infeSize;

        // new mdat appended at the end of the new file: everything before it
        // shifts by the iloc (+18) and iinf (+infeSize) growth
        int newMdatStart = data.length + 18 + infeSize;
        long newBase = newMdatStart + 8; // payload start

        byte[] out = new byte[data.length + 18 + infeSize + 8 + payload.length];
        // 1. copy everything up to the iloc box
        System.arraycopy(data, 0, out, 0, iloc.offset);
        int pos = iloc.offset;
        // 1b. meta box size grows by the iloc and iinf growth
        writeUInt(out, meta.offset, meta.size + 18 + infeSize, 4);
        // 2. iloc with the new entry (and item_count +1)
        pos = writeIlocWithNewItem(out, pos, data, iloc, newId, newBase, payload.length, shiftAfterIinf, items);
        // 3. copy from after iloc up to iinf (shifted by 18)
        int afterIloc = iloc.offset + ilocOldSize;
        int oldIinfPos = iinf.offset;
        System.arraycopy(data, afterIloc, out, pos, oldIinfPos - afterIloc);
        pos += oldIinfPos - afterIloc;
        // 4. iinf with the new infe entry (size + infeSize, count +1)
        pos = writeIinfWithNewEntry(out, pos, data, iinf, newId, infeSize);
        // 5. copy the rest after iinf (shifted by 18 + infeSize)
        int afterIinf = iinf.offset + iinf.size;
        System.arraycopy(data, afterIinf, out, pos, data.length - afterIinf);
        pos += data.length - afterIinf;
        // 6. new mdat box
        writeUInt(out, pos, 8 + payload.length, 4);
        System.arraycopy("mdat".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), 0, out, pos + 4, 4);
        System.arraycopy(payload, 0, out, pos + 8, payload.length);
        return out;
    }

    private static boolean hasXmpOrExif(Map<String, Object> updates) {
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            if (e.getValue() != null) {
                return true;
            }
        }
        return false;
    }

    private static byte[] minimalTiff() {
        return new byte[]{'M', 'M', 0, 42, 0, 0, 0, 8};
    }

    /** Write the rebuilt iloc (old entries shifted, new entry appended). */
    private static int writeIlocWithNewItem(byte[] out, int pos, byte[] data, Box iloc,
        int newId, long newBase, int newLength, int baseShift, List<Item> items) {
        int oldSize = iloc.size;
        // copy the original iloc bytes (header + fullbox + sizes + item_count)
        int d = iloc.dataStart;
        System.arraycopy(data, iloc.offset, out, pos, d + 6 - iloc.offset);
        // item_count + 1 (2 bytes at d+6)
        int count = Binary.get16u(data, d + 6, ByteOrder.BIG_ENDIAN);
        writeUInt(out, pos + (d + 6 - iloc.offset), count + 1, 2);
        // entries start at d+8
        int p = d + 8;
        for (Item it : items) {
            // ID(2) + ref(2) + base(4) + count(2) + offset(4) + length(4)
            writeUInt(out, pos + (p - iloc.offset), it.id, 2);
            writeUInt(out, pos + (p - iloc.offset + 2), 0, 2); // data reference
            writeUInt(out, pos + (p - iloc.offset + 4), it.base + baseShift, 4);
            writeUInt(out, pos + (p - iloc.offset + 8), 1, 2); // extent count
            writeUInt(out, pos + (p - iloc.offset + 10), it.extentOffset, 4);
            writeUInt(out, pos + (p - iloc.offset + 14), it.extentLength, 4);
            p += 18;
        }
        // new entry
        writeUInt(out, pos + (p - iloc.offset), newId, 2);
        writeUInt(out, pos + (p - iloc.offset + 2), 0, 2);
        writeUInt(out, pos + (p - iloc.offset + 4), newBase, 4);
        writeUInt(out, pos + (p - iloc.offset + 8), 1, 2);
        writeUInt(out, pos + (p - iloc.offset + 10), 0, 4);
        writeUInt(out, pos + (p - iloc.offset + 14), newLength, 4);
        // iloc box size
        writeUInt(out, pos, oldSize + 18, 4);
        return pos + oldSize + 18;
    }

    /** Write the rebuilt iinf (count +1, infe entry appended). */
    private static int writeIinfWithNewEntry(byte[] out, int pos, byte[] data, Box iinf,
        int newId, int infeSize) {
        int oldSize = iinf.size;
        // copy the original iinf (header + fullbox + item_count)
        int d = iinf.dataStart;
        int version = data[d];
        System.arraycopy(data, iinf.offset, out, pos, d + 4 - iinf.offset);
        // item_count: v2 uses 4 bytes at d+4, older versions 2 bytes at d+4
        int countPos = d + 4;
        long count = version == 2
            ? (readUInt(data, countPos, 4) & 0xffffffffL)
            : (readUInt(data, countPos, 2) & 0xffffL);
        if (version == 2) {
            writeUInt(out, pos + (countPos - iinf.offset), count + 1, 4);
        } else {
            writeUInt(out, pos + (countPos - iinf.offset), count + 1, 2);
        }
        int p = d + (version == 2 ? 8 : 6);
        // copy existing infe entries
        int end = iinf.end();
        while (p + 8 < end) {
            int sz = (int) readUInt(data, p, 4);
            if (p + sz > end) {
                break;
            }
            System.arraycopy(data, p, out, pos + (p - iinf.offset), sz);
            p += sz;
        }
        // new infe: size + "infe" + fullbox(v2) + id + prot + type + "Exif\0"
        int infePos = pos + (p - iinf.offset);
        writeUInt(out, infePos, infeSize, 4);
        System.arraycopy("infe".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), 0, out, infePos + 4, 4);
        out[infePos + 8] = 2; // version
        writeUInt(out, infePos + 12, newId, 2);
        writeUInt(out, infePos + 14, 0, 2); // protection
        System.arraycopy("Exif".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), 0, out, infePos + 16, 4);
        out[infePos + 20] = 0; // name terminator
        // iinf box size
        writeUInt(out, pos, oldSize + infeSize, 4);
        return pos + oldSize + infeSize;
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
