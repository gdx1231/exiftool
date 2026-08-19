package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for TIFF / GeoTiff / GIF file-level reading.
 * Reference values generated with {@code exiftool -json t/images/*.tif|gif}
 * (ExifTool 13.59). IPTC IIM parsing is out of scope for Phase 5.
 */
class TiffGifGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = TiffGifGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void tiffMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/ExifTool.tif");

        assertEquals("TIFF", info.get("FileType"));
        assertEquals("image/tiff", info.get("MIMEType"));
        assertEquals("Big-endian (Motorola, MM)", info.get("ExifByteOrder"));
        assertEquals("Full-resolution image", info.get("SubfileType"));
        assertEquals("160", info.get("ImageWidth"));
        assertEquals("120", info.get("ImageHeight"));
        assertEquals("8 8 8", info.get("BitsPerSample"));
        assertEquals("LZW", info.get("Compression"));
        assertEquals("RGB", info.get("PhotometricInterpretation"));
        assertEquals("The picture caption", info.get("ImageDescription"));
        assertEquals("Canon", info.get("Make"));
        assertEquals("Canon EOS DIGITAL REBEL", info.get("Model"));
        assertEquals("180", info.get("XResolution"));
        assertEquals("Chunky", info.get("PlanarConfiguration"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("GraphicConverter", info.get("Software"));
        assertEquals("2004:02:20 08:07:49", info.get("ModifyDate"));
        assertEquals("None", info.get("Predictor"));

        // IPTC IIM (0x83bb)
        assertEquals("2", info.get("ApplicationRecordVersion"));
        assertEquals("The picture caption", info.get("Caption-Abstract"));
        assertEquals("I wrote it", info.get("Writer-Editor"));
        assertEquals("no instructions", info.get("SpecialInstructions"));
        assertEquals("I'm the author", info.get("By-line"));
        assertEquals("On top", info.get("By-lineTitle"));
        assertEquals("Phil Harvey", info.get("Credit"));
        assertEquals("My camera", info.get("Source"));
        assertEquals("This is the title", info.get("ObjectName"));
        assertEquals("2004:02:20", info.get("DateCreated"));
        assertEquals("Kingston", info.get("City"));
        assertEquals("Ontario", info.get("Province-State"));
        assertEquals("Canada", info.get("Country-PrimaryLocationName"));
        assertEquals("no reference", info.get("OriginalTransmissionReference"));
        assertEquals("exiftool, test, picture", info.get("Keywords"));
        assertEquals("Copyright notice", info.get("CopyrightNotice"));
        assertEquals("headline", info.get("Headline"));
    }

    @Test
    void geoTiffMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/GeoTiff.tif");

        assertEquals("TIFF", info.get("FileType"));
        assertEquals("25", info.get("ImageWidth"));
        assertEquals("24", info.get("ImageHeight"));
        assertEquals("Uncompressed", info.get("Compression"));
        assertEquals("RGB Palette", info.get("PhotometricInterpretation"));
        assertEquals("72", info.get("XResolution"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("33.4179196429669 35.8363313794284 0 691955.165684031 "
            + "35.8363313794284 -33.4179196429669 0 2791710.99012603 0 0 0 0 0 0 0 1",
            info.get("ModelTransform"));
    }

    @Test
    void gifMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/GIF.gif");

        assertEquals("GIF", info.get("FileType"));
        assertEquals("image/gif", info.get("MIMEType"));
        assertEquals("89a", info.get("GIFVersion"));
        assertEquals("8", info.get("ImageWidth"));
        assertEquals("8", info.get("ImageHeight"));
        assertEquals("Yes", info.get("HasColorMap"));
    }

    @Test
    void webpMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/RIFF.webp");

        assertEquals("Extended WEBP", info.get("FileType"));
        assertEquals("image/webp", info.get("MIMEType"));
        assertEquals("XMP, EXIF, Alpha", info.get("WebP_Flags"));
        assertEquals("1", info.get("ImageWidth"));
        assertEquals("1", info.get("ImageHeight"));
    }
}
