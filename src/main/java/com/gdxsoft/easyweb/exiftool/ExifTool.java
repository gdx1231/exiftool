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

import com.gdxsoft.easyweb.exiftool.read.AacParser;
import com.gdxsoft.easyweb.exiftool.read.AsfParser;
import com.gdxsoft.easyweb.exiftool.read.Binary;
import com.gdxsoft.easyweb.exiftool.read.ExifParser;
import com.gdxsoft.easyweb.exiftool.read.FlacParser;
import com.gdxsoft.easyweb.exiftool.read.GifParser;
import com.gdxsoft.easyweb.exiftool.read.JpegParser;
import com.gdxsoft.easyweb.exiftool.read.MkvParser;
import com.gdxsoft.easyweb.exiftool.read.Mp3Parser;
import com.gdxsoft.easyweb.exiftool.read.MrwParser;
import com.gdxsoft.easyweb.exiftool.read.OoxmlParser;
import com.gdxsoft.easyweb.exiftool.read.PdfParser;
import com.gdxsoft.easyweb.exiftool.read.PngParser;
import com.gdxsoft.easyweb.exiftool.read.QuickTimeParser;
import com.gdxsoft.easyweb.exiftool.read.RafParser;
import com.gdxsoft.easyweb.exiftool.read.RiffParser;
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
    private final Map<String, String> group0 = new HashMap<>();
    private final Map<String, String> group1 = new HashMap<>();
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
        group0.clear();
        group1.clear();
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
            } else if (AacParser.isAac(data)) {
                AacParser.process(this, data);
            } else if (Mp3Parser.isMp3(data)) {
                Mp3Parser.process(this, data);
            } else if (FlacParser.isFlac(data)) {
                FlacParser.process(this, data);
            } else if (AacParser.isAac(data)) {
                AacParser.process(this, data);
            } else if (MkvParser.isMkv(data)) {
                MkvParser.process(this, data);
            } else if (AsfParser.isAsf(data)) {
                AsfParser.process(this, data);
            } else if (OoxmlParser.isOoxml(data)) {
                OoxmlParser.process(this, data);
            } else if (RiffParser.isRiff(data)) {
                RiffParser.process(this, data);
            } else if (PdfParser.isPdf(data)) {
                PdfParser.process(this, data);
            } else if (QuickTimeParser.isIsoBmff(data)) {
                QuickTimeParser.process(this, data);
            } else if (ExifParser.isTiff(data)) {
                boolean bigTiff = data.length >= 4 && (data[2] == 43 || data[3] == 43);
                foundTag("FileType", bigTiff ? "BTF" : "TIFF", 1, "File", "File");
                foundTag("MIMEType", bigTiff ? "image/x-tiff-big" : "image/tiff", 1, "File", "File");
                new ExifParser(this, data, 0).processTiff();
                fixRawFileType(data);
            }
        }
        if (byteOrder != null) {
            foundTag("ExifByteOrder", byteOrder, 1, "File", "File");
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
            foundTag("FileType", "NEF", 2, "File", "File");
            foundTag("MIMEType", "image/x-nikon-nef", 2);
        } else if (make.startsWith("Canon") && data.length >= 6
            && (Binary.get16u(data, 4, ByteOrder.BIG_ENDIAN) == 16
                || Binary.get16u(data, 4, ByteOrder.LITTLE_ENDIAN) == 16)) {
            foundTag("FileType", "CR2", 2, "File", "File");
            foundTag("MIMEType", "image/x-canon-cr2", 2);
        } else if (hasTiffTag(data, 0xc612)) { // DNGVersion
            foundTag("FileType", "DNG", 2, "File", "File");
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

    /** Record a found tag with group membership. */
    public void foundTag(String name, Object v, int priority, String group0, String group1) {
        foundTag(name, v, v, priority, group0, group1);
    }

    /** Record a found tag with its raw (pre-conversion) value. */
    public void foundTag(String name, Object raw, Object display, int priority) {
        foundTag(name, raw, display, priority, null, null);
    }

    /**
     * Record a found tag with its raw value and group membership.
     *
     * @param group0 family-0 group (EXIF/IPTC/XMP/File/MakerNotes/...)
     * @param group1 family-1 group (IFD0/ExifIFD/GPS/Canon/...)
     */
    public void foundTag(String name, Object raw, Object display, int priority, String group0, String group1) {
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
            if (group0 != null) {
                this.group0.put(name, group0);
                this.group1.put(name, group1 != null ? group1 : group0);
            }
        }
        if ("Make".equals(name)) {
            make = String.valueOf(raw);
        } else if ("Model".equals(name)) {
            model = String.valueOf(raw);
        }
    }

    /** Family-0 groups (EXIF/File/IPTC/XMP/MakerNotes/...), matching {@link #imageInfo} keys. */
    public Map<String, String> getGroup0() {
        return Collections.unmodifiableMap(group0);
    }

    /** Family-1 groups (IFD0/ExifIFD/GPS/Canon/...), matching {@link #imageInfo} keys. */
    public Map<String, String> getGroup1() {
        return Collections.unmodifiableMap(group1);
    }

    /**
     * Family-2 groups (Camera/Image/Time/Location/Author/Video/File/...),
     * derived from the tag name by semantic classification rules.
     */
    public Map<String, String> getGroup2() {
        Map<String, String> g2 = new LinkedHashMap<>();
        for (String tag : value.keySet()) {
            String c = classifyFamily2(tag);
            if (c != null) {
                g2.put(tag, c);
            }
        }
        return g2;
    }

    /**
     * Family-3 groups (vendor name: Canon/Nikon/FujiFilm/...), derived from the
     * camera make for maker-note tags.
     */
    public Map<String, String> getGroup3() {
        Map<String, String> g3 = new LinkedHashMap<>();
        String vendor = make != null ? vendorShortName(make) : null;
        if (vendor != null) {
            for (Map.Entry<String, String> e : group0.entrySet()) {
                if ("MakerNotes".equals(e.getValue())) {
                    g3.put(e.getKey(), vendor);
                }
            }
        }
        return g3;
    }

    /** Semantic classification of a tag name into a family-2 category. */
    private static String classifyFamily2(String tag) {
        if (tag.startsWith("GPS")) {
            return "Location";
        }
        if (tag.equals("FileType") || tag.equals("MIMEType") || tag.equals("ExifByteOrder")
            || tag.equals("FileSize") || tag.equals("FileModifyDate")) {
            return "File";
        }
        // camera parameters first: ExposureTime / ShutterSpeed contain "Time"/"Speed"
        if (tag.startsWith("Flash") || tag.startsWith("Lens") || tag.startsWith("Focus")
            || tag.startsWith("AF") || tag.startsWith("ISO") || tag.startsWith("Exposure")
            || tag.startsWith("FNumber") || tag.startsWith("Focal") || tag.startsWith("MaxAperture")
            || tag.startsWith("MinAperture") || tag.startsWith("Aperture")
            || tag.startsWith("Shutter") || tag.startsWith("Brightness")
            || tag.startsWith("Contrast") || tag.startsWith("Saturation")
            || tag.startsWith("Sharpness") || tag.startsWith("Metering")
            || tag.startsWith("WhiteBalance") || tag.startsWith("Quality")
            || tag.startsWith("Camera") || tag.startsWith("Scene")
            || tag.startsWith("Drive") || tag.startsWith("Bracket")
            || tag.startsWith("SelfTimer") || tag.startsWith("Macro")
            || tag.startsWith("Noise") || tag.startsWith("Gain")
            || tag.startsWith("Subject") || tag.startsWith("Custom")
            || tag.startsWith("Picture") || tag.startsWith("Tone")
            || tag.startsWith("LightSource") || tag.startsWith("Sensitivity")
            || tag.startsWith("Make") || tag.startsWith("Model")
            || tag.startsWith("Serial") || tag.startsWith("ColorFilter")
            || tag.startsWith("BWFilter") || tag.startsWith("InternalFlash")
            || tag.startsWith("Firmware") || tag.startsWith("ImageStabilization")
            || tag.startsWith("ExposureComp") || tag.startsWith("AEB")
            || tag.startsWith("AEBB") || tag.startsWith("FlashExposure")
            || tag.startsWith("FlashGuide") || tag.startsWith("DigitalZoom")
            || tag.startsWith("OpticalZoom") || tag.startsWith("TotalZoom")
            || tag.startsWith("Zoom") || tag.startsWith("FocalUnits")
            || tag.startsWith("ISOSetting") || tag.startsWith("MeasuredEV")
            || tag.startsWith("TargetAperture") || tag.startsWith("SequenceNumber")
            || tag.startsWith("Interval") || tag.startsWith("FileNumber")
            || tag.startsWith("LastFileNumber") || tag.startsWith("FolderName")
            || tag.startsWith("Minolta") || tag.startsWith("Kodak")
            || tag.startsWith("CanonImageType") || tag.startsWith("ColorHue")
            || tag.startsWith("HueAdjustment") || tag.startsWith("ExposureIndex")
            || tag.startsWith("BulbDuration") || tag.startsWith("SelfTimer2")
            || tag.startsWith("NDFilter") || tag.startsWith("AutoRotate")
            || tag.startsWith("AutoExposureBracketing") || tag.startsWith("SlowShutter")
            || tag.startsWith("OpticalZoomCode") || tag.startsWith("FocusBracketing")) {
            return "Camera";
        }
        if (tag.contains("Date") || tag.contains("Time") || tag.equals("Duration")) {
            return "Time";
        }
        if (tag.equals("MajorBrand") || tag.equals("MinorVersion") || tag.equals("CompatibleBrands")
            || tag.equals("CompressorID") || tag.equals("VendorID") || tag.equals("HandlerType")
            || tag.equals("TimeScale") || tag.equals("TrackID") || tag.startsWith("Track")
            || tag.equals("MovieHeaderVersion")) {
            return "Video";
        }
        if (tag.equals("Artist") || tag.equals("Copyright") || tag.equals("Creator")
            || tag.equals("Author") || tag.equals("Title") || tag.equals("Caption-Abstract")
            || tag.startsWith("By-line") || tag.equals("Headline") || tag.equals("Keywords")
            || tag.equals("OwnerName") || tag.equals("Credit") || tag.equals("Source")
            || tag.equals("Rights") || tag.equals("Writer-Editor") || tag.equals("ObjectName")
            || tag.equals("SpecialInstructions") || tag.equals("Description")) {
            return "Author";
        }
        if (tag.contains("Width") || tag.contains("Height") || tag.contains("Size")
            || tag.equals("BitsPerSample") || tag.equals("Compression")
            || tag.equals("Resolution") || tag.startsWith("Resolution")
            || tag.equals("Orientation") || tag.startsWith("YCbCr")
            || tag.equals("ColorSpace") || tag.equals("ColorType")
            || tag.equals("Interlace") || tag.equals("EncodingProcess")
            || tag.equals("ComponentsConfiguration") || tag.equals("PhotometricInterpretation")
            || tag.equals("PlanarConfiguration") || tag.equals("Predictor")
            || tag.startsWith("Strip") || tag.startsWith("Row") || tag.startsWith("Tile")
            || tag.equals("SubfileType") || tag.startsWith("Thumbnail")
            || tag.startsWith("Preview") || tag.equals("Filter") || tag.equals("BitDepth")
            || tag.equals("ColorMode") || tag.equals("ColorComponents")
            || tag.equals("Megapixels") || tag.equals("ImageDescription")
            || tag.startsWith("KodakImage") || tag.startsWith("Sensor")
            || tag.equals("RawDepth") || tag.equals("StorageMethod")
            || tag.equals("BayerPattern") || tag.equals("HasColorMap")) {
            return "Image";
        }
        if (tag.equals("JFIFVersion") || tag.equals("GIFVersion") || tag.equals("Version")
            || tag.equals("XMPToolkit") || tag.equals("HasExtendedXMP")
            || tag.startsWith("PrintIM")) {
            return "ExifTool";
        }
        return "Other";
    }

    /** Map a camera make to a short vendor name (family 3). */
    private static String vendorShortName(String make) {
        String m = make.toUpperCase(java.util.Locale.ROOT);
        if (m.startsWith("NIKON")) {
            return "Nikon";
        }
        if (m.startsWith("CANON")) {
            return "Canon";
        }
        if (m.startsWith("FUJI")) {
            return "FujiFilm";
        }
        if (m.startsWith("KONICA MINOLTA") || m.startsWith("MINOLTA")) {
            return "Minolta";
        }
        if (m.startsWith("CASIO")) {
            return "Casio";
        }
        if (m.startsWith("EASTMAN KODAK") || m.startsWith("KODAK")) {
            return "Kodak";
        }
        if (m.startsWith("SONY")) {
            return "Sony";
        }
        return make;
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
