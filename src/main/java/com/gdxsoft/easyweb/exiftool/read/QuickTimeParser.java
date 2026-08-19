package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * QuickTime / ISO BMFF metadata parser (HEIC, MP4, MOV): extracts the ftyp
 * brands, the moov/mvhd movie header, the first track header (dimensions) and
 * the HEIC meta handler / image spatial extents.
 */
public final class QuickTimeParser {

    private static final long QUICKTIME_EPOCH = -2082844800L; // 1904-01-01 as unix seconds

    private QuickTimeParser() {}

    public static boolean isIsoBmff(byte[] data) {
        if (data.length < 12) {
            return false;
        }
        long size = Binary.get32u(data, 0, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
        if (size < 8) {
            return false;
        }
        String type = new String(data, 4, 4, StandardCharsets.ISO_8859_1);
        return "ftyp".equals(type) || "moov".equals(type) || "mdat".equals(type)
            || "free".equals(type) || "skip".equals(type) || "wide".equals(type);
    }

    public static void process(ExifTool et, byte[] data) {
        Box ftyp = firstTopLevel(data, "ftyp");
        if (ftyp != null) {
            processFtyp(et, data, ftyp);
        }
        // file type based on ftyp major brand; no ftyp -> classic QuickTime MOV
        String fileType;
        if (ftyp != null && ftyp.dataStart + 8 <= data.length) {
            String major = new String(data, ftyp.dataStart, 4, StandardCharsets.ISO_8859_1);
            fileType = switch (major) {
                case "mif1", "heic", "heix", "avif" -> "HEIF";
                case "qt  " -> "MOV";
                default -> "MP4";
            };
        } else {
            fileType = "MOV"; // moov-first files are QuickTime movies
        }
        et.foundTag("FileType", fileType, 1, "File", "File");
        et.foundTag("MIMEType", "HEIF".equals(fileType) ? "image/heif"
            : "MOV".equals(fileType) ? "video/quicktime" : "video/mp4", 1, "File", "File");

        Box meta = firstTopLevel(data, "meta");
        if (meta != null) {
            processMeta(et, data, meta);
        }
        Box moov = firstTopLevel(data, "moov");
        if (moov != null) {
            processMoov(et, data, moov);
        }
        // HEIC/AVIF embed EXIF as an "Exif" item in an mdat box (may be multiple)
        int p = 0;
        while (p + 8 <= data.length) {
            Box b = Box.read(data, p);
            if (b == null) {
                break;
            }
            if ("mdat".equals(b.type)) {
                processEmbeddedExif(et, data, b);
            }
            p = b.end();
        }
    }

    /** Scan the mdat box for an "Exif" marker followed by a TIFF header. */
    private static void processEmbeddedExif(ExifTool et, byte[] data, Box mdat) {
        int p = mdat.dataStart;
        int end = mdat.end();
        while (p + 8 < end) {
            if (data[p] == 'E' && data[p + 1] == 'x' && data[p + 2] == 'i' && data[p + 3] == 'f'
                && (isTiffHeader(data, p + 4) || isTiffHeader(data, p + 6))) {
                int tiffBase = isTiffHeader(data, p + 4) ? p + 4 : p + 6;
                new ExifParser(et, data, tiffBase).processTiff();
                return;
            }
            p++;
        }
    }

    private static boolean isTiffHeader(byte[] data, int p) {
        return p + 4 <= data.length
            && ((data[p] == 'I' && data[p + 1] == 'I' && data[p + 2] == 42 && data[p + 3] == 0)
                || (data[p] == 'M' && data[p + 1] == 'M' && data[p + 2] == 0 && data[p + 3] == 42));
    }

    private static void processFtyp(ExifTool et, byte[] data, Box ftyp) {
        if (ftyp.dataStart + 8 > data.length) {
            return;
        }
        String major = new String(data, ftyp.dataStart, 4, StandardCharsets.ISO_8859_1);
        int minor = Binary.get32u(data, ftyp.dataStart + 4, ByteOrder.BIG_ENDIAN);
        et.foundTag("MajorBrand", brandName(major), 1, "QuickTime", "File");
        et.foundTag("MinorVersion", minorVersionString(minor), 1, "QuickTime", "File");
        // compatible brands
        StringBuilder brands = new StringBuilder();
        int pos = ftyp.dataStart + 8;
        while (pos + 4 <= ftyp.end()) {
            String b = new String(data, pos, 4, StandardCharsets.ISO_8859_1);
            if (brands.length() > 0) {
                brands.append(", ");
            }
            brands.append(b.trim());
            pos += 4;
        }
        if (brands.length() > 0) {
            et.foundTag("CompatibleBrands", brands.toString(), 1, "QuickTime", "File");
        }
    }

    private static void processMeta(ExifTool et, byte[] data, Box meta) {
        Box hdlr = meta.findChild(data, "hdlr");
        if (hdlr != null && hdlr.dataStart + 12 <= meta.end()) {
            String handler = new String(data, hdlr.dataStart + 8, 4, StandardCharsets.ISO_8859_1);
            et.foundTag("HandlerType", switch (handler) {
                case "pict" -> "Picture";
                case "vide" -> "Video";
                case "soun" -> "Audio";
                case "text" -> "Text";
                default -> handler.trim();
            }, 1, "QuickTime", "File");
        }
        Box pitm = meta.findChild(data, "pitm");
        if (pitm != null && pitm.dataStart + 6 <= meta.end()) {
            long id = Binary.get32u(data, pitm.dataStart + 2, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            et.foundTag("PrimaryItemReference", String.valueOf(id), 1, "QuickTime", "File");
        }
        // image spatial extents: meta/iprp/ipco/ispe
        Box iprp = meta.findChild(data, "iprp");
        if (iprp != null) {
            Box ipco = iprp.findChild(data, "ipco");
            if (ipco != null) {
                int pos = ipco.dataStart;
                while (pos + 8 <= ipco.end()) {
                    Box b = Box.read(data, pos);
                    if (b == null) {
                        break;
                    }
                    if ("ispe".equals(b.type) && b.dataStart + 9 <= b.end()) {
                        int w = Binary.get32u(data, b.dataStart + 4, ByteOrder.BIG_ENDIAN);
                        int h = Binary.get32u(data, b.dataStart + 8, ByteOrder.BIG_ENDIAN);
                        et.foundTag("ImageWidth", String.valueOf(w), 1, "QuickTime", "File");
                        et.foundTag("ImageHeight", String.valueOf(h), 1, "QuickTime", "File");
                        break;
                    }
                    pos = b.end();
                }
            }
        }
    }

    private static void processMoov(ExifTool et, byte[] data, Box moov) {
        Box mvhd = moov.findChild(data, "mvhd");
        if (mvhd != null) {
            processMovieHeader(et, data, mvhd);
        }
        Box trak = moov.findChild(data, "trak");
        if (trak != null) {
            Box tkhd = trak.findChild(data, "tkhd");
            if (tkhd != null) {
                processTrackHeader(et, data, tkhd);
            }
            Box mdia = trak.findChild(data, "mdia");
            if (mdia != null) {
                Box minf = mdia.findChild(data, "minf");
                if (minf != null) {
                    Box stbl = minf.findChild(data, "stbl");
                    if (stbl != null) {
                        Box stsd = stbl.findChild(data, "stsd");
                        if (stsd != null) {
                            processSampleEntry(et, data, stsd);
                        }
                    }
                }
            }
        }
    }

    private static void processMovieHeader(ExifTool et, byte[] data, Box mvhd) {
        int d = mvhd.dataStart;
        int version = data[d] & 0xff;
        if (version == 0 && d + 20 <= mvhd.end()) {
            long create = Binary.get32u(data, d + 4, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long modify = Binary.get32u(data, d + 8, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long timescale = Binary.get32u(data, d + 12, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long duration = Binary.get32u(data, d + 16, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            et.foundTag("MovieHeaderVersion", String.valueOf(version), 1, "QuickTime", "File");
            et.foundTag("CreateDate", quickTimeDate(create), 1, "QuickTime", "File");
            et.foundTag("ModifyDate", quickTimeDate(modify), 1, "QuickTime", "File");
            et.foundTag("TimeScale", String.valueOf(timescale), 1, "QuickTime", "File");
            if (timescale > 0) {
                et.foundTag("Duration", String.format("%.2f s", duration / (double) timescale), 1, "QuickTime", "File");
            }
        }
    }

    private static void processTrackHeader(ExifTool et, byte[] data, Box tkhd) {
        int d = tkhd.dataStart;
        int version = data[d] & 0xff;
        if (version == 0 && d + 84 <= tkhd.end()) {
            long create = Binary.get32u(data, d + 4, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long modify = Binary.get32u(data, d + 8, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long trackId = Binary.get32u(data, d + 12, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            long duration = Binary.get32u(data, d + 20, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
            int width = (int) (Binary.get32u(data, d + 76, ByteOrder.BIG_ENDIAN) >> 16);
            int height = (int) (Binary.get32u(data, d + 80, ByteOrder.BIG_ENDIAN) >> 16);
            et.foundTag("TrackCreateDate", quickTimeDate(create), 1, "QuickTime", "File");
            et.foundTag("TrackModifyDate", quickTimeDate(modify), 1, "QuickTime", "File");
            et.foundTag("TrackID", String.valueOf(trackId), 1, "QuickTime", "File");
            et.foundTag("TrackDuration", String.format("%.2f s", duration / 600.0), 1, "QuickTime", "File");
            et.foundTag("ImageWidth", String.valueOf(width), 1, "QuickTime", "File");
            et.foundTag("ImageHeight", String.valueOf(height), 1, "QuickTime", "File");
        }
    }

    /** stsd sample entry: first entry's fourcc (CompressorID) + vendor. */
    private static void processSampleEntry(ExifTool et, byte[] data, Box stsd) {
        int d = stsd.dataStart + 8; // skip version/flags + entry count
        if (d + 24 <= stsd.end()) {
            // sample entry: size(4) + type(4) -> CompressorID
            String codec = new String(data, d + 4, 4, StandardCharsets.ISO_8859_1).trim();
            if (!codec.isEmpty()) {
                et.foundTag("CompressorID", codec, 1, "QuickTime", "File");
            }
            String vendor = new String(data, d + 20, 4, StandardCharsets.ISO_8859_1).trim();
            String vendorName = vendorName(vendor);
            if (vendorName != null) {
                et.foundTag("VendorID", vendorName, 1, "QuickTime", "File");
            }
        }
    }

    private static String vendorName(String vendor) {
        return switch (vendor) {
            case "pent" -> "Pentax";
            case "appl" -> "Apple";
            case "sony" -> "Sony";
            case "cann" -> "Canon";
            case "nikn" -> "Nikon";
            case "leic" -> "Leica";
            case "olym" -> "Olympus";
            case "sams" -> "Samsung";
            case "pana" -> "Panasonic";
            case "hkon" -> "Konica";
            case "minn" -> "Minolta";
            case "koda" -> "Kodak";
            case "fuji" -> "Fuji";
            case "prnm" -> "Polaroid";
            case "goog" -> "Google";
            default -> vendor.isBlank() ? null : vendor;
        };
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

    private static String brandName(String brand) {
        return switch (brand) {
            case "mif1" -> "High Efficiency Image Format still image (.HEIF)";
            case "heic" -> "High Efficiency Image Format still image (.HEIC)";
            case "heix" -> "High Efficiency Image Format still image (HEIF)";
            case "qt  " -> "Apple QuickTime (.MOV/QT)";
            case "isom" -> "ISO Base Media file format (MPEG-4 Part 12)";
            case "mp42" -> "MP4 v2 [ISO 14496-14]";
            case "avif" -> "AV1 Image File Format (.AVIF)";
            default -> brand.trim();
        };
    }

    private static String minorVersionString(int minor) {
        return (minor >> 16) + "." + ((minor >> 8) & 0xff) + "." + (minor & 0xff);
    }

    /** QuickTime dates are seconds since 1904-01-01. */
    private static String quickTimeDate(long seconds) {
        long unix = seconds + QUICKTIME_EPOCH;
        java.time.LocalDateTime dt = java.time.Instant.ofEpochSecond(unix)
            .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
        return String.format("%04d:%02d:%02d %02d:%02d:%02d",
            dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
            dt.getHour(), dt.getMinute(), dt.getSecond());
    }
}
