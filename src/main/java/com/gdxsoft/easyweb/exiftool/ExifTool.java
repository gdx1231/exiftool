package com.gdxsoft.easyweb.exiftool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.read.Binary;
import com.gdxsoft.easyweb.exiftool.read.ExifParser;
import com.gdxsoft.easyweb.exiftool.read.GifParser;
import com.gdxsoft.easyweb.exiftool.read.JpegParser;
import com.gdxsoft.easyweb.exiftool.read.MrwParser;
import com.gdxsoft.easyweb.exiftool.read.PngParser;
import com.gdxsoft.easyweb.exiftool.read.QuickTimeParser;
import com.gdxsoft.easyweb.exiftool.read.RafParser;
import com.gdxsoft.easyweb.exiftool.read.WebpParser;

/**
 * Main entry point, the Java analogue of {@code Image::ExifTool}. Phase 1 supports
 * reading EXIF metadata from JPEG and TIFF files.
 *
 * <p>Usage:
 * <pre>{@code
 * ExifTool et = new ExifTool();
 * Map<String, Object> info = et.imageInfo(new File("photo.jpg"));
 * }</pre>
 */
public final class ExifTool {

    private final Map<String, Object> value = new LinkedHashMap<>();
    private final Map<String, Object> rawValues = new LinkedHashMap<>();
    private final Map<String, Integer> priorities = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private String make;
    private String model;
    private String byteOrder;

    /**
     * Extract metadata from a file. Returns a map of tag name to display value
     * (the analogue of {@code ImageInfo}).
     */
    public Map<String, Object> imageInfo(File file) throws IOException {
        return imageInfo(Files.readAllBytes(file.toPath()));
    }

    public Map<String, Object> imageInfo(byte[] data) {
        value.clear();
        rawValues.clear();
        priorities.clear();
        warnings.clear();
        make = null;
        model = null;
        byteOrder = null;
        if (data.length >= 4) {
            if (JpegParser.isJpeg(data)) {
                JpegParser.process(this, data);
            } else if (PngParser.isPng(data)) {
                PngParser.process(this, data);
            } else if (GifParser.isGif(data)) {
                GifParser.process(this, data);
            } else if (WebpParser.isWebp(data)) {
                WebpParser.process(this, data);
            } else if (RafParser.isRaf(data)) {
                RafParser.process(this, data);
            } else if (MrwParser.isMrw(data)) {
                MrwParser.process(this, data);
            } else if (QuickTimeParser.isIsoBmff(data)) {
                QuickTimeParser.process(this, data);
            } else if (ExifParser.isTiff(data)) {
                foundTag("FileType", "TIFF", 1);
                foundTag("MIMEType", "image/tiff", 1);
                new ExifParser(this, data, 0).processTiff();
                fixRawFileType(data);
            }
        }
        if (byteOrder != null) {
            foundTag("ExifByteOrder", byteOrder, 1);
        }
        return Collections.unmodifiableMap(value);
    }

    /** Camera make (from the IFD0 Make tag), used to dispatch maker notes. */
    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    /** Record the EXIF byte order (set by the TIFF parser). */
    public void setByteOrder(String order) {
        this.byteOrder = order;
    }

    /**
     * Rewrite metadata in a file image, applying the given tag updates
     * (display values, e.g. {@code Map.of("Artist", "New Artist")}).
     *
     * @return the new file bytes
     */
    public byte[] writeImage(byte[] data, Map<String, Object> updates) {
        if (JpegParser.isJpeg(data)) {
            return com.gdxsoft.easyweb.exiftool.write.JpegRewriter.write(data, updates);
        }
        if (PngParser.isPng(data)) {
            return com.gdxsoft.easyweb.exiftool.write.PngRewriter.write(data, updates);
        }
        if (WebpParser.isWebp(data)) {
            return com.gdxsoft.easyweb.exiftool.write.WebpRewriter.write(data, updates);
        }
        if (QuickTimeParser.isIsoBmff(data)) {
            // HEIC/AVIF: embedded EXIF item in meta/mdat (no-op if not applicable)
            byte[] result = com.gdxsoft.easyweb.exiftool.write.HeicRewriter.write(data, updates);
            if (result != data) {
                return result;
            }
        }
        if (ExifParser.isTiff(data)) {
            ByteOrder order = data[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            return com.gdxsoft.easyweb.exiftool.write.TiffRewriter.rewrite(data, order, updates);
        }
        return data;
    }

    /**
     * Raw files that use the TIFF structure report a more specific FileType:
     * Nikon NEF, Canon CR2 (IFD0 at offset 16), DNG (DNGVersion tag present).
     */
    private void fixRawFileType(byte[] data) {
        String make = getMake();
        if (make == null) {
            return;
        }
        if (make.startsWith("NIKON")) {
            foundTag("FileType", "NEF", 2);
            foundTag("MIMEType", "image/x-nikon-nef", 2);
        } else if (make.startsWith("Canon") && data.length >= 6
            && (Binary.get16u(data, 4, ByteOrder.BIG_ENDIAN) == 16
                || Binary.get16u(data, 4, ByteOrder.LITTLE_ENDIAN) == 16)) {
            foundTag("FileType", "CR2", 2);
            foundTag("MIMEType", "image/x-canon-cr2", 2);
        } else if (hasTiffTag(data, 0xc612)) { // DNGVersion
            foundTag("FileType", "DNG", 2);
            foundTag("MIMEType", "image/x-adobe-dng", 2);
        }
    }

    /** Scan IFD0 for the presence of the given tag ID. */
    private boolean hasTiffTag(byte[] data, int tagId) {
        ByteOrder order = data.length > 1 && data[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        if (data.length < 8) {
            return false;
        }
        int ifd = Binary.get32u(data, 4, order);
        if (ifd + 2 > data.length) {
            return false;
        }
        int num = Binary.get16u(data, ifd, order);
        for (int i = 0; i < num; i++) {
            int e = ifd + 2 + 12 * i;
            if (e + 12 > data.length) {
                break;
            }
            if (Binary.get16u(data, e, order) == tagId) {
                return true;
            }
        }
        return false;
    }

    /** Record a found tag, mirroring FoundTag priority semantics: a higher-priority
     *  (or later same-priority) value replaces the existing one. */
    public void foundTag(String name, Object v, int priority) {
        foundTag(name, v, v, priority);
    }

    /** Record a found tag with its raw (pre-conversion) value. */
    public void foundTag(String name, Object raw, Object display, int priority) {
        if ("Model".equals(name)) {
            // Model RawConv strips trailing blanks (Exif.pm 0x0110)
            raw = String.valueOf(raw).replaceAll("\\s+$", "");
            display = String.valueOf(display).replaceAll("\\s+$", "");
        }
        Integer old = priorities.get(name);
        if (old == null || priority >= old) {
            value.put(name, formatValue(display));
            rawValues.put(name, formatValue(raw));
            priorities.put(name, priority);
        }
        if ("Make".equals(name)) {
            make = String.valueOf(raw);
        } else if ("Model".equals(name)) {
            model = String.valueOf(raw);
        }
    }

    /**
     * Raw (unconverted) values, matching {@link #imageInfo} keys. The analogue
     * of exiftool's {@code GetValue($tag, 'Raw')}.
     */
    public Map<String, Object> getRawInfo() {
        return Collections.unmodifiableMap(rawValues);
    }

    /** All values are stored in display (string) form, like GetValue('PrintConv'). */
    private static Object formatValue(Object v) {
        if (v instanceof Double d) {
            return PerlNum.format(d);
        }
        if (v instanceof Long l) {
            return String.valueOf(l);
        }
        return v;
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
