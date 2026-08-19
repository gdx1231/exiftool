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

    public ExifParser(ExifTool et, byte[] data, int tiffBase) {
        this.et = et;
        this.data = data;
        this.tiffBase = tiffBase;
        this.dirBase = tiffBase;
    }

    /** True if data starts with a TIFF header ("II*\0" or "MM\0*"). */
    public static boolean isTiff(byte[] data) {
        if (data.length < 4) {
            return false;
        }
        return (data[0] == 'I' && data[1] == 'I' && data[2] == 42 && data[3] == 0)
            || (data[0] == 'M' && data[1] == 'M' && data[2] == 0 && data[3] == 42);
    }

    /** Parse the TIFF header and the IFD0 chain starting at {@code tiffBase}. */
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
        if (Binary.get16u(data, tiffBase + 2, order) != TIFF_MAGIC) {
            return; // BigTIFF not supported in Phase 1
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
        int numEntries = Binary.get16u(data, absDir, order);
        for (int i = 0; i < numEntries; i++) {
            int entry = absDir + 2 + 12 * i;
            if (entry + 12 > data.length) {
                break;
            }
            int tagId = Binary.get16u(data, entry, order);
            int formatCode = Binary.get16u(data, entry + 2, order);
            int count = Binary.get32u(data, entry + 4, order);
            int valueOff = Binary.get32u(data, entry + 8, order);

            TagInfo info = table.get(tagId);
            ExifFormat format = ExifFormat.fromCode(formatCode);
            if (format == ExifFormat.NONE) {
                continue;
            }
            // override the read format if the tag specifies one (Perl "Format" attribute)
            if (info != null && info.format() != null) {
                ExifFormat override = ExifFormat.fromName(info.format());
                if (override != ExifFormat.NONE && override != format) {
                    long origSize = (long) count * format.size();
                    format = override;
                    count = Math.max(1, (int) (origSize / format.size()));
                }
            }

            long size = (long) count * format.size();
            Object raw;
            int valuePos;
            if (size <= 4) {
                // value is inline in the entry
                valuePos = entry + 8;
                raw = ValueReader.readValue(data, entry + 8, format, count, order);
            } else {
                valuePos = dirBase + valueOff;
                raw = ValueReader.readValue(data, dirBase + valueOff, format, count, order);
            }

            // MakerNote pointer: dispatch by vendor signature/make
            if (tagId == TAG_MAKER_NOTE && "ExifIFD".equals(dirName)) {
                int mnSize = raw instanceof byte[] b ? b.length : 0;
                processMakerNote(valueOff, mnSize);
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
                    processIFD(valueOff, sub.dirName(), subTable);
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
        int nextOff = Binary.get32u(data, absDir + 2 + 12 * numEntries, order);
        if (nextOff != 0 && "IFD0".equals(dirName)) {
            processIFD(nextOff, "IFD1", table);
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
