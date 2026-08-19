package com.gdxsoft.easyweb.exiftool.read;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.tables.NikonTables;

/**
 * MakerNotes dispatcher, the Java analogue of {@code @Image::ExifTool::MakerNotes::Main}.
 * Locates the vendor-specific sub-directory inside a 0x927c MakerNote value based on
 * the header signature and camera make.
 */
public final class MakerNotes {

    private static final byte[] NIKON_SIG = {'N', 'i', 'k', 'o', 'n', 0, 2};

    /**
     * Description of a located maker note sub-directory.
     *
     * @param ifdBase     base used to locate the IFD itself (absolute file offset)
     * @param byteOrder   maker note byte order, or null to keep the current one
     * @param ifd0Offset  offset of IFD0 relative to {@code ifdBase}
     * @param valueBase   base for in-IFD value offsets (absolute file offset)
     * @param table       vendor tag table
     */
    public record MakerNoteInfo(int ifdBase, ByteOrder byteOrder, int ifd0Offset, int valueBase, TagTable table) {}

    private MakerNotes() {}

    /**
     * Locate the maker note IFD inside the value at {@code valuePtr} of {@code size} bytes.
     *
     * @param exifTiffBase absolute offset of the EXIF TIFF header (the base for
     *                     Canon maker note value offsets)
     */
    public static MakerNoteInfo locate(byte[] data, int valuePtr, int size, String make, int exifTiffBase) {
        if (startsWith(data, valuePtr, NIKON_SIG)) {
            // Nikon: 7-byte signature + 3-byte version + TIFF header ("MM"/"II", magic, IFD0 offset)
            //   offsets are relative to the TIFF header at valuePtr + 10
            ByteOrder order = data[valuePtr + 10] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            int base = valuePtr + 10;
            int ifd0 = Binary.get32u(data, valuePtr + 14, order);
            return new MakerNoteInfo(base, order, ifd0, base, NikonTables.main());
        }
        if (startsWith(data, valuePtr, new byte[]{'F', 'U', 'J', 'I', 'F', 'I', 'L', 'M'})) {
            // FujiFilm: 8-byte "FUJIFILM" header + 4-byte IFD offset hint (0x0c);
            //   IFD starts at valuePtr + 12 but in-IFD offsets are relative to valuePtr.
            //   Little-endian order.
            return new MakerNoteInfo(valuePtr + 12, ByteOrder.LITTLE_ENDIAN, 0, valuePtr,
                com.gdxsoft.easyweb.exiftool.tables.FujiTables.main());
        }
        if (make != null && make.startsWith("Canon")) {
            // Canon: plain IFD starting at the value; value offsets are relative to the
            // EXIF TIFF header (base fix from the 8-byte TIFF footer is ignored when the
            // footer offset matches the current position, which is the common case)
            return new MakerNoteInfo(valuePtr, null, 0, exifTiffBase, com.gdxsoft.easyweb.exiftool.tables.CanonTables.main());
        }
        if (startsWith(data, valuePtr, new byte[]{'K', 'D', 'K', ' ', 'I', 'N', 'F', 'O'})) {
            // Kodak type 1: 8-byte "KDK INFO" header then a binary-data directory
            // (Big-endian, offsets relative to the data start at valuePtr + 8)
            int base = valuePtr + 8;
            return new MakerNoteInfo(base, ByteOrder.BIG_ENDIAN, 0, base,
                com.gdxsoft.easyweb.exiftool.tables.KodakTables.main());
        }
        if (make != null && make.startsWith("CASIO")) {
            // Casio: plain IFD starting at the value; offsets relative to the EXIF TIFF header
            return new MakerNoteInfo(valuePtr, null, 0, exifTiffBase,
                com.gdxsoft.easyweb.exiftool.tables.CasioTables.main());
        }
        if (make != null && (make.startsWith("Minolta") || make.startsWith("Konica Minolta"))) {
            // Minolta: plain IFD starting at the value; value offsets are relative to
            // the EXIF TIFF header (no header, no base fix)
            return new MakerNoteInfo(valuePtr, null, 0, exifTiffBase,
                com.gdxsoft.easyweb.exiftool.tables.MinoltaTables.main());
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int offset, byte[] sig) {
        if (offset + sig.length > data.length) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if (data[offset + i] != sig[i]) {
                return false;
            }
        }
        return true;
    }
}
