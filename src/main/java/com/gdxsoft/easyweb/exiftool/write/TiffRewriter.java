package com.gdxsoft.easyweb.exiftool.write;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifFormat;
import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.read.Binary;
import com.gdxsoft.easyweb.exiftool.tables.ExifTables;
import com.gdxsoft.easyweb.exiftool.tables.GpsTables;

/**
 * TIFF/EXIF IFD rewriter: rebuilds the IFD chain, preserving unmodified tags,
 * applying reverse-converted updates, and recursing into sub-directories
 * (ExifIFD, GPS, Interop, IFD1 via the next pointer).
 *
 * <p>MakerNotes are copied as opaque blocks with offset fix-up: Nikon/FujiFilm
 * in-block offsets are relative to the maker note value position (rebase by the
 * position delta); Canon offsets are relative to the TIFF header and need no
 * fix. IPTC blocks contain no offsets and are copied verbatim.
 *
 * <p>The output keeps the TIFF header at offset 0 of the returned buffer.
 */
public final class TiffRewriter {

    private static final int TAG_EXIF_OFFSET = 0x8769;
    private static final int TAG_GPS_INFO = 0x8825;
    private static final int TAG_INTEROP = 0xa005;
    private static final int TAG_MAKER_NOTE = 0x927c;
    private static final int TAG_IPTC = 0x83bb;
    private static final int TAG_XMP = 0x02bc;
    private static final int TAG_THUMBNAIL_OFFSET = 0x0201;
    private static final int TAG_THUMBNAIL_LENGTH = 0x0202;

    private final byte[] data;
    private final ByteOrder order;
    private final Map<String, Object> updates;
    /** Thumbnail image data copied from the original file. */
    private byte[] thumbnail;
    /** Camera make, used to detect Canon maker notes (no header signature). */
    private String make;

    /** One IFD entry in build form. */
    private static final class Entry {
        int tagId;
        int formatCode;
        long count;
        byte[] raw;          // encoded value bytes (full count*size)
        List<Entry> subIfd;  // rebuilt sub-directory entries (pointer tags)
        String makerNoteSig; // maker note signature (NIKON/FUJIFILM/CANON)
        int offset;          // value offset relative to TIFF header (build result)
        boolean thumbOffset; // value is an offset into the copied thumbnail block
    }

    private TiffRewriter(byte[] data, ByteOrder order, Map<String, Object> updates) {
        this.data = data;
        this.order = order;
        this.updates = updates;
    }

    /**
     * Rebuild the EXIF/TIFF structure (header + IFD0 chain) into a new buffer.
     * IFD1 (thumbnail) is rebuilt and the thumbnail image data is copied after
     * it, with ThumbnailOffset fixed up.
     */
    public static byte[] rewrite(byte[] data, ByteOrder order, Map<String, Object> updates) {
        TiffRewriter rw = new TiffRewriter(data, order, updates);
        // IFD0 location comes from the TIFF header (8 for standard files, but
        // embedded TIFF in WebP/HEIC may use another offset)
        int ifd0Offset = data.length >= 8 ? Binary.get32u(data, 4, order) : 8;
        List<Entry> ifd0 = rw.prepareIfd(ifd0Offset, true);
        int next = rw.readNextIfd(ifd0Offset);
        List<Entry> ifd1 = next != 0 ? rw.prepareIfd(next, false) : null;
        rw.extractThumbnail(ifd1, next);

        int ifd0Size = rw.measureIfd(ifd0);
        int ifd1Size = ifd1 != null ? rw.measureIfd(ifd1) : 0;
        int thumbPos = 8 + ifd0Size + ifd1Size;
        int total = thumbPos + (rw.thumbnail != null ? rw.thumbnail.length : 0);
        byte[] out = new byte[total];
        out[0] = (byte) (order == ByteOrder.LITTLE_ENDIAN ? 'I' : 'M');
        out[1] = (byte) (order == ByteOrder.LITTLE_ENDIAN ? 'I' : 'M');
        out[2] = (byte) (order == ByteOrder.LITTLE_ENDIAN ? 42 : 0);
        out[3] = (byte) (order == ByteOrder.LITTLE_ENDIAN ? 0 : 42);
        write32(out, 4, 8, order); // IFD0 offset
        rw.buildIfd(out, 8, ifd0, 0);
        if (ifd1 != null) {
            write32(out, 8 + 2 + 12 * ifd0.size(), 8 + ifd0Size, order); // IFD0 -> IFD1
            if (rw.thumbnail != null) {
                System.arraycopy(rw.thumbnail, 0, out, thumbPos, rw.thumbnail.length);
            }
            rw.buildIfd(out, 8 + ifd0Size, ifd1, rw.thumbnail != null ? thumbPos : 0);
        }
        return out;
    }

    private int readNextIfd(int ifdOffset) {
        if (ifdOffset + 2 > data.length) {
            return 0;
        }
        int num = Binary.get16u(data, ifdOffset, order);
        int nextPos = ifdOffset + 2 + 12 * num;
        if (nextPos + 4 > data.length) {
            return 0;
        }
        return Binary.get32u(data, nextPos, order);
    }

    /** Copy the thumbnail image data referenced by IFD1 (0x0201/0x0202). */
    private void extractThumbnail(List<Entry> ifd1, int ifd1Offset) {
        if (ifd1 == null) {
            return;
        }
        long thumbOff = -1;
        long thumbLen = 0;
        for (Entry e : ifd1) {
            if (e.tagId == TAG_THUMBNAIL_OFFSET && e.raw.length == 4) {
                thumbOff = Binary.get32u(e.raw, 0, order) & 0xffffffffL;
                e.thumbOffset = true;
            } else if (e.tagId == TAG_THUMBNAIL_LENGTH && e.raw.length == 4) {
                thumbLen = Binary.get32u(e.raw, 0, order) & 0xffffffffL;
            }
        }
        if (thumbOff >= 0 && thumbLen > 0 && thumbOff + thumbLen <= data.length) {
            thumbnail = Arrays.copyOfRange(data, (int) thumbOff, (int) thumbOff + (int) thumbLen);
        }
    }

    // ------------------------------------------------------------------
    // Preparation: read original entries, apply updates, prepare sub-directories
    // ------------------------------------------------------------------

    private List<Entry> prepareIfd(int offset, boolean isIfd0) {
        List<Entry> entries = new ArrayList<>();
        if (offset + 2 > data.length) {
            if (isIfd0) {
                addNewTags(entries);
            }
            return entries;
        }
        int num = Binary.get16u(data, offset, order);
        for (int i = 0; i < num; i++) {
            int e = offset + 2 + 12 * i;
            if (e + 12 > data.length) {
                break;
            }
            Entry entry = new Entry();
            entry.tagId = Binary.get16u(data, e, order);
            entry.formatCode = Binary.get16u(data, e + 2, order);
            entry.count = Binary.get32u(data, e + 4, order) & 0xffffffffL;
            int valueOff = Binary.get32u(data, e + 8, order); // relative to TIFF header
            ExifFormat format = ExifFormat.fromCode(entry.formatCode);
            if (format == ExifFormat.NONE) {
                continue;
            }
            long size = entry.count * format.size();
            if (size <= 4) {
                // inline in the entry
                entry.raw = Arrays.copyOfRange(data, e + 8, e + 8 + (int) size);
            } else {
                entry.raw = readValue(valueOff, size);
            }

            String updateName = tagName(entry.tagId);
            if (updateName != null && updates.containsKey(updateName) && updates.get(updateName) == null) {
                continue; // delete requested for this tag
            }

            if (entry.tagId == TAG_MAKER_NOTE) {
                entry.makerNoteSig = detectMakerNote(valueOff);
                entry.offset = valueOff; // old value position (relative to TIFF header)
            } else if (entry.tagId == TAG_EXIF_OFFSET || entry.tagId == TAG_GPS_INFO
                || entry.tagId == TAG_INTEROP) {
                entry.subIfd = prepareIfd(valueOff, false);
            } else if (entry.tagId == TAG_IPTC) {
                // opaque block, no internal offsets
            } else if (entry.tagId == TAG_XMP) {
                // XMP: rebuild the XML document from XMP tag updates
                byte[] xml = entry.raw;
                byte[] updated = rebuildXmp(xml, updates);
                if (updated != null) {
                    entry.raw = updated;
                    entry.count = updated.length / format.size();
                    if (entry.count == 0) {
                        entry.count = 1;
                    }
                }
            } else {
                applyUpdate(entry);
                if (entry.tagId == 0x010F) {
                    make = new String(entry.raw, java.nio.charset.StandardCharsets.ISO_8859_1)
                        .replaceAll("\0.*$", "");
                }
            }
            entries.add(entry);
        }
        // Phase 8: add new tags (not present) to IFD0
        if (isIfd0) {
            addNewTags(entries);
        }
        return entries;
    }

    /** Append tags from the update map that are not already present (IFD0). */
    private void addNewTags(List<Entry> entries) {
        List<Entry> exifIfdTags = new ArrayList<>();
        // XMP document tags: collect and add a 0x02BC entry when absent
        Map<String, Object> xmpUpdates = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> u : updates.entrySet()) {
            if (u.getValue() != null && XmpWriter.TAGS.contains(u.getKey())) {
                xmpUpdates.put(u.getKey(), u.getValue());
            }
        }
        if (!xmpUpdates.isEmpty()) {
            boolean hasXmp = false;
            for (Entry e : entries) {
                if (e.tagId == TAG_XMP) {
                    hasXmp = true;
                    break;
                }
            }
            if (!hasXmp) {
                Entry entry = new Entry();
                entry.tagId = TAG_XMP;
                entry.formatCode = ExifFormat.UNDEF.code();
                byte[] xml = XmpWriter.build(xmpUpdates);
                entry.raw = java.util.Arrays.copyOf(xml, xml.length + 1);
                entry.count = entry.raw.length;
                entries.add(entry);
            }
        }
        for (Map.Entry<String, Object> u : updates.entrySet()) {
            if (u.getValue() == null) {
                continue; // deletion
            }
            Integer tagId = tagIdByName(u.getKey());
            if (tagId == null) {
                continue;
            }
            boolean present = false;
            for (Entry e : entries) {
                if (e.tagId == tagId) {
                    present = true;
                    break;
                }
            }
            if (present) {
                continue;
            }
            // tags in the ExifIFD range (0x829a..0xa40c) go to the ExifIFD directory
            if (tagId >= 0x829a && tagId <= 0xa40c && tagId != 0x83bb && tagId != 0x85d8) {
                Entry entry = buildNewEntry(tagId, u.getValue());
                if (entry != null) {
                    exifIfdTags.add(entry);
                }
                continue;
            }
            Entry ifd0Entry = buildNewEntry(tagId, u.getValue());
            if (ifd0Entry != null) {
                entries.add(ifd0Entry);
            }
        }
        if (!exifIfdTags.isEmpty()) {
            // find (or create) the ExifOffset pointer in IFD0
            Entry exifOffset = null;
            for (Entry e : entries) {
                if (e.tagId == TAG_EXIF_OFFSET) {
                    exifOffset = e;
                    break;
                }
            }
            if (exifOffset == null) {
                exifOffset = new Entry();
                exifOffset.tagId = TAG_EXIF_OFFSET;
                exifOffset.formatCode = ExifFormat.IFD.code();
                exifOffset.count = 1;
                exifOffset.raw = new byte[4];
                exifOffset.subIfd = new ArrayList<>();
                entries.add(exifOffset);
            }
            if (exifOffset.subIfd == null) {
                exifOffset.subIfd = new ArrayList<>();
            }
            exifOffset.subIfd.addAll(exifIfdTags);
        }
    }

    /** Create a new entry for a tag from the update map. */
    private Entry buildNewEntry(int tagId, Object value) {
        TagInfo info = ExifTables.main().get(tagId);
        String writable = info != null ? info.writable() : null;
        ExifFormat format = writable != null ? ExifFormat.fromName(writable) : ExifFormat.STRING;
        if (format == ExifFormat.NONE) {
            format = ExifFormat.STRING;
        }
        Entry entry = new Entry();
        entry.tagId = tagId;
        entry.formatCode = format.code();
        try {
            byte[] encoded = ValueEncoder.encode(value, format, order);
            entry.raw = encoded;
            entry.count = encoded.length / format.size();
            if (entry.count == 0) {
                entry.count = 1;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return entry;
    }

    private Integer tagIdByName(String name) {
        for (Map.Entry<Integer, TagInfo> e : ExifTables.main().tags().entrySet()) {
            if (name.equals(e.getValue().name())) {
                return e.getKey();
            }
        }
        return null;
    }

    private void applyUpdate(Entry entry) {
        String name = tagName(entry.tagId);
        if (name == null || !updates.containsKey(name)) {
            return;
        }
        Object newValue = updates.get(name);
        ExifFormat format = ExifFormat.fromCode(entry.formatCode);
        if (format == ExifFormat.NONE) {
            return;
        }
        try {
            byte[] encoded = ValueEncoder.encode(newValue, format, order);
            entry.raw = encoded;
            entry.count = encoded.length / format.size();
            if (entry.count == 0) {
                entry.count = 1;
            }
        } catch (IllegalArgumentException e) {
            // keep original value if it can't be encoded
        }
    }

    private String tagName(int tagId) {
        TagInfo info = ExifTables.main().get(tagId);
        return info != null ? info.name() : null;
    }

    /**
     * Rebuild an XMP document (0x02BC value) applying the XMP tag updates.
     * Returns null if there are no XMP updates.
     */
    private byte[] rebuildXmp(byte[] xml, Map<String, Object> updates) {
        Map<String, Object> xmpUpdates = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            if (XmpWriter.TAGS.contains(e.getKey()) && e.getValue() != null) {
                xmpUpdates.put(e.getKey(), e.getValue());
            }
        }
        if (xmpUpdates.isEmpty()) {
            return null;
        }
        String s = new String(xml, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\0+$", "");
        byte[] out = s.isEmpty() ? XmpWriter.build(xmpUpdates) : XmpWriter.update(s.getBytes(
            java.nio.charset.StandardCharsets.UTF_8), xmpUpdates);
        if (out == null) {
            return null;
        }
        return java.util.Arrays.copyOf(out, out.length + 1); // null-terminated
    }

    private String detectMakerNote(int valuePtr) {
        if (valuePtr + 7 <= data.length && data[valuePtr] == 'N' && data[valuePtr + 1] == 'i'
            && data[valuePtr + 2] == 'k' && data[valuePtr + 3] == 'o' && data[valuePtr + 4] == 'n'
            && data[valuePtr + 5] == 0 && data[valuePtr + 6] == 2) {
            return "NIKON";
        }
        if (valuePtr + 8 <= data.length && data[valuePtr] == 'F' && data[valuePtr + 1] == 'U'
            && data[valuePtr + 2] == 'J' && data[valuePtr + 3] == 'I' && data[valuePtr + 4] == 'F'
            && data[valuePtr + 5] == 'I' && data[valuePtr + 6] == 'L' && data[valuePtr + 7] == 'M') {
            return "FUJIFILM";
        }
        if (make != null && make.startsWith("Canon")) {
            return "CANON";
        }
        return null; // unknown maker note: keep as opaque block, no fix-up
    }

    private byte[] readValue(int valueOff, long size) {
        if (valueOff < 0 || valueOff + size > data.length) {
            return new byte[(int) size];
        }
        return Arrays.copyOfRange(data, valueOff, valueOff + (int) size);
    }

    // ------------------------------------------------------------------
    // Measuring and building
    // ------------------------------------------------------------------

    private int measureIfd(List<Entry> entries) {
        int size = 2 + 12 * entries.size() + 4; // count + entries + next ptr
        for (Entry e : entries) {
            if (e.subIfd != null) {
                size += measureIfd(e.subIfd);
            } else if (e.raw.length > 4) {
                size += wordAlign(e.raw.length);
            }
        }
        return size;
    }

    /**
     * Build an IFD at {@code base} (absolute offset in out). Returns the end offset.
     * Value offsets are relative to the TIFF header (offset 0 of out).
     * {@code thumbBase} is the absolute offset of the copied thumbnail block, used
     * to fix up ThumbnailOffset (0x0201).
     */
    private int buildIfd(byte[] out, int base, List<Entry> entries, int thumbBase) {
        int num = entries.size();
        write16(out, base, num); // entry count
        int valueStart = base + 2 + 12 * num;
        write32(out, valueStart, 0, order); // next-IFD pointer (fixed by caller)
        int pos = valueStart + 4;

        // value area first (offsets must be known when writing entries)
        for (Entry e : entries) {
            if (e.subIfd != null) {
                int subBase = pos;
                pos = buildIfd(out, subBase, e.subIfd, thumbBase);
                e.offset = subBase;
            } else if (e.makerNoteSig != null) {
                pos = copyMakerNotes(out, pos, e);
            } else if (e.raw.length > 4) {
                System.arraycopy(e.raw, 0, out, pos, e.raw.length);
                e.offset = pos;
                pos += wordAlign(e.raw.length);
            }
        }
        // entries
        for (int i = 0; i < num; i++) {
            Entry e = entries.get(i);
            int ent = base + 2 + 12 * i;
            write16(out, ent, e.tagId);
            write16(out, ent + 2, e.formatCode);
            write32(out, ent + 4, e.count);
            if (e.thumbOffset) {
                write32(out, ent + 8, thumbBase);
            } else if (e.raw.length <= 4 && e.subIfd == null && e.makerNoteSig == null) {
                System.arraycopy(e.raw, 0, out, ent + 8, e.raw.length);
                for (int j = e.raw.length; j < 4; j++) {
                    out[ent + 8 + j] = 0;
                }
            } else {
                write32(out, ent + 8, e.offset);
            }
        }
        return pos;
    }

    /**
     * Copy a maker note block. Nikon/FujiFilm in-block offsets are relative to a
     * base inside the block (which moves with it): no fix needed. Canon in-block
     * offsets are absolute (relative to the TIFF header), so they must be rebased
     * by the block position delta.
     */
    private int copyMakerNotes(byte[] out, int pos, Entry entry) {
        int blockLen = entry.raw.length;
        System.arraycopy(entry.raw, 0, out, pos, blockLen);
        if ("CANON".equals(entry.makerNoteSig)) {
            int delta = pos - entry.offset;
            if (delta != 0) {
                int count = Binary.get16u(out, pos, order); // Canon IFD starts at the value
                for (int i = 0; i < count; i++) {
                    int e = pos + 2 + 12 * i;
                    int formatCode = Binary.get16u(out, e + 2, order);
                    long cnt = Binary.get32u(out, e + 4, order) & 0xffffffffL;
                    ExifFormat format = ExifFormat.fromCode(formatCode);
                    if (format == ExifFormat.NONE) {
                        continue;
                    }
                    if (cnt * format.size() > 4) {
                        int off = Binary.get32u(out, e + 8, order);
                        write32(out, e + 8, off + delta, order);
                    }
                }
            }
        }
        entry.offset = pos;
        return pos + wordAlign(blockLen);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int wordAlign(int len) {
        return len + (len & 1);
    }

    private void write16(byte[] out, int off, int v) {
        write16(out, off, v, order);
    }

    private void write32(byte[] out, int off, long v) {
        write32(out, off, v, order);
    }

    private static void write16(byte[] out, int off, int v, ByteOrder order) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            out[off] = (byte) v;
            out[off + 1] = (byte) (v >> 8);
        } else {
            out[off] = (byte) (v >> 8);
            out[off + 1] = (byte) v;
        }
    }

    private static void write32(byte[] out, int off, long v, ByteOrder order) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            out[off] = (byte) v;
            out[off + 1] = (byte) (v >> 8);
            out[off + 2] = (byte) (v >> 16);
            out[off + 3] = (byte) (v >> 24);
        } else {
            out[off] = (byte) (v >> 24);
            out[off + 1] = (byte) (v >> 16);
            out[off + 2] = (byte) (v >> 8);
            out[off + 3] = (byte) v;
        }
    }
}
