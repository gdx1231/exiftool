package com.gdxsoft.easyweb.exiftool.read;

import java.util.HashSet;
import java.util.Set;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifFormat;
import com.gdxsoft.easyweb.exiftool.ExifTool;
import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.tables.ExifTables;

/**
 * TIFF/EXIF IFD parser, the Java analogue of {@code Image::ExifTool::Exif::ProcessExif}.
 *
 * <p>Parses the classic TIFF structure (magic 42): IFD entry loop, value decoding
 * via {@link ValueReader}, sub-directory recursion through pointer tags
 * ({@code SubDirectory}), and the IFD0 -> IFD1 next-pointer chain. Also dispatches
 * the 0x927c MakerNote pointer to the vendor-specific parser.
 */
public final class ExifParser {

    private static final int TIFF_MAGIC = 42;
    private static final int BIGTIFF_MAGIC = 43;
    /** ExifIFD MakerNote pointer tag. */
    private static final int TAG_MAKER_NOTE = 0x927c;
    /** IFD0 XMP tag. */
    private static final int TAG_XMP = 0x02bc;

    /** Map a family-1 directory name to its family-0 group. */
    private static String group0Of(String dirName) {
        return switch (dirName) {
            case "IFD0", "IFD1", "ExifIFD", "GPS", "InteropIFD", "PreviewIFD" -> "EXIF";
            case "IPTC" -> "IPTC";
            default -> "MakerNotes"; // MakerNotes and its sub-directories
        };
    }

    private final ExifTool et;
    private final byte[] data;
    private final int tiffBase;
    private final Set<Integer> processedDirs = new HashSet<>();
    private ByteOrder order;
    /** Base for IFD-internal offsets; equals tiffBase except inside maker notes. */
    private int dirBase;
    /** True when parsing a BigTIFF structure (magic 43, 8-byte offsets). */
    private boolean bigTiff;

    public ExifParser(ExifTool et, byte[] data, int tiffBase) {
        this.et = et;
        this.data = data;
        this.tiffBase = tiffBase;
        this.dirBase = tiffBase;
    }

    /** True if data starts with a TIFF header ("II*\0"/"MM\0*") or BigTIFF ("II+\0"/"MM\0+"). */
    public static boolean isTiff(byte[] data) {
        if (data.length < 4) {
            return false;
        }
        return (data[0] == 'I' && data[1] == 'I' && data[2] == 42 && data[3] == 0)
            || (data[0] == 'M' && data[1] == 'M' && data[2] == 0 && data[3] == 42)
            || (data[0] == 'I' && data[1] == 'I' && data[2] == 43 && data[3] == 0)
            || (data[0] == 'M' && data[1] == 'M' && data[2] == 0 && data[3] == 43);
    }

    /** Parse the TIFF/BigTIFF header and the IFD0 chain starting at {@code tiffBase}. */
    public void processTiff() {
        if (data.length < tiffBase + 8) {
            return;
        }
        if (data[tiffBase] == 'I' && data[tiffBase + 1] == 'I') {
            order = ByteOrder.LITTLE_ENDIAN;
            et.setByteOrder("Little-endian (Intel, II)");
        } else if (data[tiffBase] == 'M' && data[tiffBase + 1] == 'M') {
            order = ByteOrder.BIG_ENDIAN;
            et.setByteOrder("Big-endian (Motorola, MM)");
        } else {
            return;
        }
        int magic = Binary.get16u(data, tiffBase + 2, order);
        if (magic == BIGTIFF_MAGIC) {
            // BigTIFF: magic 43 + 2 reserved bytes + 8-byte IFD0 offset
            bigTiff = true;
            long ifd0 = Binary.get64u(data, tiffBase + 8, order);
            processIFD((int) ifd0, "IFD0", ExifTables.main());
            return;
        }
        if (magic != TIFF_MAGIC) {
            return;
        }
        int ifd0 = Binary.get32u(data, tiffBase + 4, order);
        processIFD(ifd0, "IFD0", ExifTables.main());
    }

    private void processIFD(int offset, String dirName, TagTable table) {
        int absDir = dirBase + offset;
        if (offset < 0 || absDir + 2 > data.length) {
            return;
        }
        // guard against cyclical recursion into the same directory
        if (!processedDirs.add(absDir)) {
            return;
        }

        // IFD1 and PreviewIFD are low-priority directories (Perl LOW_PRIORITY_DIR)
        boolean lowPriorityDir = "IFD1".equals(dirName) || "PreviewIFD".equals(dirName);
        // BigTIFF: 8-byte entry count, 20-byte entries, 8-byte next pointer;
        // classic: 2-byte count, 12-byte entries, 4-byte next pointer
        int entrySize = bigTiff ? 20 : 12;
        int countSize = bigTiff ? 8 : 2;
        long numEntries = bigTiff
            ? Binary.get64u(data, absDir, order)
            : (Binary.get16u(data, absDir, order) & 0xffffL);
        for (int i = 0; i < numEntries; i++) {
            int entry = absDir + countSize + entrySize * i;
            if (entry + entrySize > data.length) {
                break;
            }
            int tagId = Binary.get16u(data, entry, order);
            int formatCode = Binary.get16u(data, entry + 2, order);
            long count = bigTiff
                ? Binary.get64u(data, entry + 4, order)
                : (Binary.get32u(data, entry + 4, order) & 0xffffffffL);
            long valueOff = bigTiff
                ? Binary.get64u(data, entry + 12, order)
                : (Binary.get32u(data, entry + 8, order) & 0xffffffffL);

            TagInfo info = table.get(tagId);
            ExifFormat format = ExifFormat.fromCode(formatCode);
            if (format == ExifFormat.NONE) {
                continue;
            }
            // override the read format if the tag specifies one (Perl "Format" attribute)
            if (info != null && info.format() != null) {
                ExifFormat override = ExifFormat.fromName(info.format());
                if (override != ExifFormat.NONE && override != format) {
                    long origSize = count * format.size();
                    format = override;
                    count = Math.max(1, origSize / format.size());
                }
            }

            long size = count * format.size();
            Object raw;
            int valuePos;
            // BigTIFF values up to 8 bytes are inline (entry + 12); classic up to 4 (entry + 8)
            int inlineSize = bigTiff ? 8 : 4;
            int inlineOffset = bigTiff ? 12 : 8;
            if (size <= inlineSize) {
                valuePos = entry + inlineOffset;
                raw = ValueReader.readValue(data, entry + inlineOffset, format, (int) count, order);
            } else {
                valuePos = dirBase + (int) valueOff;
                raw = ValueReader.readValue(data, dirBase + (int) valueOff, format, (int) count, order);
            }

            // MakerNote pointer: dispatch by vendor signature/make
            if (tagId == TAG_MAKER_NOTE && "ExifIFD".equals(dirName)) {
                int mnSize = raw instanceof byte[] b ? b.length : 0;
                processMakerNote((int) valueOff, mnSize);
                continue;
            }

            // XMP (0x02BC): parse the XML document
            if (tagId == TAG_XMP && raw instanceof byte[] b) {
                String xml = new String(b, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\0+$", "");
                if (!xml.isEmpty()) {
                    XmpParser.process(et, xml);
                }
                continue;
            }

            if (info == null) {
                continue;
            }

            TagInfo.SubDirectory sub = info.subDirectory();
            if (sub != null) {
                // pointer tag: value data starts at valuePos (inline or referenced)
                TagTable subTable = sub.table() != null ? sub.table() : table;
                if ("IPTC".equals(sub.dirName())) {
                    IptcParser.process(et, data, valuePos + sub.startOffset(), (int) size);
                } else if (subTable.isBinaryData()) {
                    BinaryDataParser.process(et, data, valuePos + sub.startOffset(), order, subTable,
                        (int) size, group0Of(dirName), dirName);
                } else {
                    processIFD((int) valueOff, sub.dirName(), subTable);
                }
                continue;
            }

            Object converted = info.valueConv().convert(raw);
            if (converted == null) {
                continue; // RawConv returned undef: tag is not recorded
            }
            converted = info.printConv().convert(converted);
            if (info.isOffset() && converted instanceof Number n) {
                // IsOffset: convert relative offset to absolute file position
                converted = n.longValue() + dirBase;
            }
            int priority = info.prioritySet() ? info.priority() : (lowPriorityDir ? 0 : 1);
            et.foundTag(info.name(), raw, converted, priority, group0Of(dirName), dirName);
        }

        // next-IFD pointer (IFD0 -> IFD1 for thumbnails)
        int nextPos = absDir + countSize + entrySize * (int) numEntries;
        long nextOff = bigTiff
            ? Binary.get64u(data, nextPos, order)
            : (Binary.get32u(data, nextPos, order) & 0xffffffffL);
        if (nextOff != 0 && "IFD0".equals(dirName)) {
            processIFD((int) nextOff, "IFD1", table);
        }
    }

    /**
     * Dispatch the ExifIFD MakerNote (0x927c) value to the vendor parser.
     * {@code valueOff} is the maker note data offset relative to the current base.
     */
    private void processMakerNote(int valueOff, int size) {
        int valuePtr = dirBase + valueOff;
        MakerNotes.MakerNoteInfo info = MakerNotes.locate(data, valuePtr, size, et.getMake(), dirBase);
        if (info == null) {
            return;
        }
        ByteOrder savedOrder = order;
        int savedBase = dirBase;
        if (info.byteOrder() != null) {
            order = info.byteOrder();
        }
        dirBase = info.valueBase();
        if (info.table().isBinaryData()) {
            // binary-data maker notes (e.g. Kodak): parse as a binary directory
            BinaryDataParser.process(et, data, info.ifdBase(), order, info.table(), Integer.MAX_VALUE);
        } else {
            // IFD position expressed as an offset relative to the new value base
            int ifdOffset = info.ifdBase() - info.valueBase() + info.ifd0Offset();
            processIFD(ifdOffset, "MakerNotes", info.table());
        }
        order = savedOrder;
        dirBase = savedBase;
    }
}
