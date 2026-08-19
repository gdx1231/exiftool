package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Write path round-trip tests: apply tag updates, read back, and verify the
 * new values plus preservation of untouched tags (including maker notes).
 */
class WriteRoundTripTest {

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = WriteRoundTripTest.class.getResourceAsStream(name)) {
            assertNotNull(in, name + " resource missing");
            return in.readAllBytes();
        }
    }

    @Test
    void tiffUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/ExifTool.tif"),
            Map.of("Software", "RoundTripWriter"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("RoundTripWriter", info.get("Software"));
        assertEquals("Canon", info.get("Make")); // untouched
        assertEquals("Canon EOS DIGITAL REBEL", info.get("Model"));
        assertEquals("2004:02:20 08:07:49", info.get("ModifyDate"));
        assertEquals("180", info.get("XResolution"));
        assertEquals("Chunky", info.get("PlanarConfiguration"));
        // IPTC preserved
        assertEquals("The picture caption", info.get("Caption-Abstract"));
        assertEquals("exiftool, test, picture", info.get("Keywords"));
        assertEquals("Kingston", info.get("City"));
    }

    @Test
    void jpegUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/Motorola.jpg"),
            Map.of("Software", "RoundTripWriter"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("RoundTripWriter", info.get("Software"));
        assertEquals("Motorola", info.get("Make"));
        assertEquals("XT1575", info.get("Model"));
        assertEquals("1/1961", info.get("ExposureTime"));
        assertEquals("2.0", info.get("FNumber"));
        assertEquals("2.2.0.0", info.get("GPSVersionID"));
        assertEquals("sRGB", info.get("ColorSpace"));
    }

    @Test
    void jpegWithMakerNotesPreserved() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/NikonD70.jpg"),
            Map.of("Software", "RoundTripWriter"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("RoundTripWriter", info.get("Software"));
        // EXIF
        assertEquals("1/60", info.get("ExposureTime"));
        assertEquals("Multi-segment", info.get("MeteringMode"));
        assertEquals("56.0 mm", info.get("FocalLength"));
        // Nikon maker notes intact after the layout change
        assertEquals("2.10", info.get("MakerNoteVersion"));
        assertEquals("200", info.get("ISO"));
        assertEquals("18-70mm f/3.5-4.5", info.get("Lens"));
        assertEquals("Fired, TTL Mode", info.get("FlashMode"));
        assertEquals("Center", info.get("AFPointsInFocus"));
        assertEquals("597 256 361 256", info.get("WB_RGBGLevels"));
        assertEquals("526", info.get("ShutterCount"));
        assertEquals("0101", info.get("LensDataVersion"));
        assertEquals("0.63 m", info.get("FocusDistance"));
        // file-level tags still readable
        assertEquals("JPEG", info.get("FileType"));
        assertEquals("71", info.get("ImageWidth"));
    }

    @Test
    void jpegStillReadableByStructure() throws IOException {
        // after rewriting, the JPEG must still be a valid JPEG with a readable SOF
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/Canon.jpg"),
            Map.of("Artist", "RoundTrip Artist"));
        Map<String, Object> info = et.imageInfo(out);
        assertTrue(out.length > 100, "output too small");
        assertEquals("Canon", info.get("Make"));
        assertEquals("EOS Digital Rebel / 300D / Kiss Digital", info.get("CanonModelID"));
        assertEquals("RAW", info.get("Quality"));
        assertEquals("EOS Mid-range", info.get("CameraType"));
        assertEquals("Phil Harvey", info.get("OwnerName"));
    }

    @Test
    void addNewTagToTiff() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/ExifTool.tif"),
            Map.of("Artist", "John Doe")); // Artist not present in the original
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("John Doe", info.get("Artist"));
        assertEquals("Canon", info.get("Make"));
        assertEquals("2004:02:20 08:07:49", info.get("ModifyDate"));
    }

    @Test
    void addNewTagToJpeg() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/Motorola.jpg"),
            Map.of("Artist", "Jane Doe"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("Jane Doe", info.get("Artist"));
        assertEquals("Motorola", info.get("Make"));
        assertEquals("1/1961", info.get("ExposureTime"));
    }

    @Test
    void deleteTag() throws IOException {
        ExifTool et = new ExifTool();
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("Software", null); // null deletes the tag
        byte[] out = et.writeImage(resource("/ExifTool.tif"), updates);
        Map<String, Object> info = et.imageInfo(out);

        assertEquals(null, info.get("Software"));
        assertEquals("Canon", info.get("Make"));
        assertEquals("2004:02:20 08:07:49", info.get("ModifyDate"));
    }

    @Test
    void addExifIfdTagCreatesExifIfd() throws IOException {
        ExifTool et = new ExifTool();
        // ExifTool.tif has no ExifIFD; adding ISO must create the ExifOffset
        // pointer and a new ExifIFD directory
        byte[] out = et.writeImage(resource("/ExifTool.tif"),
            Map.of("ISO", "400", "Artist", "JD"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("400", info.get("ISO"));
        assertEquals("JD", info.get("Artist"));
        assertEquals("Canon", info.get("Make")); // untouched
        assertEquals("2004:02:20 08:07:49", info.get("ModifyDate"));
    }

    @Test
    void xmpUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        // update an existing XMP tag; other XMP tags must be preserved
        byte[] out = et.writeImage(resource("/ExtendedXMP.jpg"),
            Map.of("Title", "New XMP Title"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("New XMP Title", info.get("Title"));
        assertEquals("PhilToo", info.get("Author"));
        assertEquals("Just ExifTool again", info.get("Producer"));
        assertEquals("Image::ExifTool 7.50", info.get("XMPToolkit"));
    }

    @Test
    void xmpAddToFileWithoutXmp() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/Motorola.jpg"),
            Map.of("Title", "Brand New Title"));
        Map<String, Object> info = et.imageInfo(out);

        assertEquals("Brand New Title", info.get("Title"));
        assertEquals("Motorola", info.get("Make"));
        assertEquals("1/1961", info.get("ExposureTime"));
    }

    @Test
    void webpExifUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        // existing EXIF chunk: update Artist
        byte[] out = et.writeImage(resource("/RIFF.webp"),
            Map.of("Artist", "NewWebpArtist"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("NewWebpArtist", info.get("Artist"));
        assertEquals("Extended WEBP", info.get("FileType"));
        assertEquals("1", info.get("ImageWidth"));
    }

    @Test
    void webpExifAddRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        // RIFF.webp already has EXIF; add Make (still an update to the chunk)
        byte[] out = et.writeImage(resource("/RIFF.webp"),
            Map.of("Make", "WebpMaker"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("WebpMaker", info.get("Make"));
        assertEquals("Extended WEBP", info.get("FileType"));
    }

    @Test
    void pngExifUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/png_exif.png"),
            Map.of("Artist", "NewPngArtist"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("NewPngArtist", info.get("Artist"));
        assertEquals("TestMaker", info.get("Make")); // preserved
        assertEquals("PNG", info.get("FileType"));
        assertEquals("Grayscale", info.get("ColorType")); // IHDR still readable
    }

    @Test
    void pngExifAddRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        byte[] out = et.writeImage(resource("/PNG.png"),
            Map.of("Make", "PngMaker", "Artist", "PngArtist"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("PngMaker", info.get("Make"));
        assertEquals("PngArtist", info.get("Artist"));
        assertEquals("16", info.get("ImageWidth"));
        assertEquals("Grayscale", info.get("ColorType"));
    }

    @Test
    void heicExifUpdateRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        // in-place update of the embedded EXIF item (values must not grow the TIFF)
        byte[] out = et.writeImage(resource("/heic_exif.heic"),
            Map.of("Artist", "J", "Make", "TM"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("J", info.get("Artist"));
        assertEquals("TM", info.get("Make"));
        // ISO BMFF box-level tags preserved
        assertEquals("HEIF", info.get("FileType"));
        assertEquals("High Efficiency Image Format still image (.HEIF)", info.get("MajorBrand"));
        assertEquals("1596", info.get("ImageWidth"));
        assertEquals("Picture", info.get("HandlerType"));
    }

    @Test
    void heicExifGrowRoundTrip() throws IOException {
        ExifTool et = new ExifTool();
        // growing the TIFF triggers the mdat insertion path
        byte[] out = et.writeImage(resource("/heic_exif.heic"),
            Map.of("Artist", "A much longer artist name that grows the TIFF"));
        Map<String, Object> info = et.imageInfo(out);
        assertEquals("A much longer artist name that grows the TIFF", info.get("Artist"));
        assertEquals("TestMaker", info.get("Make")); // preserved
        assertEquals("HEIF", info.get("FileType"));
        assertEquals("1596", info.get("ImageWidth"));
        assertEquals("Picture", info.get("HandlerType"));
    }
}
