package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for RAW formats: RAF (FujiFilm header + embedded TIFF),
 * NEF (TIFF structure, Nikon), CR2 (TIFF structure with IFD0 at offset 16,
 * Canon). Reference values from {@code exiftool -json} (ExifTool 13.59).
 */
class RawGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = RawGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void rafMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/FujiFilm.raf");

        assertEquals("RAF", info.get("FileType"));
        assertEquals("image/x-fujifilm-raf", info.get("MIMEType"));
        assertEquals("FUJIFILM", info.get("Make"));
        assertEquals("FinePix S5Pro", info.get("Model"));
        assertEquals("Horizontal (normal)", info.get("Orientation"));
        assertEquals("1/250", info.get("ExposureTime"));
        assertEquals("8.0", info.get("FNumber"));
        assertEquals("100", info.get("ISO"));
        // FujiFilm maker notes via the embedded EXIF
        assertEquals("0130", info.get("Version"));
        assertEquals("Auto", info.get("WhiteBalance"));
        assertEquals("Off", info.get("FujiFlashMode"));
    }

    @Test
    void nefMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/Nikon.nef");

        assertEquals("NEF", info.get("FileType"));
        assertEquals("image/x-nikon-nef", info.get("MIMEType"));
        assertEquals("NIKON CORPORATION", info.get("Make"));
        assertEquals("NIKON D70", info.get("Model"));
        assertEquals("Horizontal (normal)", info.get("Orientation"));
    }

    @Test
    void cr2MatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/CanonRaw.cr2");

        assertEquals("CR2", info.get("FileType"));
        assertEquals("image/x-canon-cr2", info.get("MIMEType"));
        assertEquals("Canon", info.get("Make"));
        assertEquals("Canon EOS 350D DIGITAL", info.get("Model"));
        // Canon maker notes from the embedded EXIF
        assertEquals("RAW", info.get("Quality"));
        assertEquals("EOS Mid-range", info.get("CameraType"));
    }

    @Test
    void webpEmbeddedExif() throws IOException {
        Map<String, Object> info = info("/RIFF.webp");

        assertEquals("Extended WEBP", info.get("FileType"));
        assertEquals("me", info.get("Artist"));
        assertEquals("72", info.get("XResolution"));
        assertEquals("inches", info.get("ResolutionUnit"));
        assertEquals("Centered", info.get("YCbCrPositioning"));
    }

    @Test
    void mrwMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/Minolta.mrw");

        assertEquals("MRW", info.get("FileType"));
        assertEquals("image/x-minolta-mrw", info.get("MIMEType"));
        assertEquals("27200001", info.get("FirmwareID"));
        assertEquals("2456", info.get("SensorHeight"));
        assertEquals("3272", info.get("SensorWidth"));
        assertEquals("2448", info.get("ImageHeight"));
        assertEquals("3264", info.get("ImageWidth"));
        assertEquals("12", info.get("RawDepth"));
        assertEquals("12", info.get("BitDepth"));
        assertEquals("Linear", info.get("StorageMethod"));
        assertEquals("RGGB", info.get("BayerPattern"));
        // TTW block: EXIF + Minolta maker notes
        assertEquals("Konica Minolta Camera, Inc.", info.get("Make"));
        assertEquals("DiMAGE A2", info.get("Model"));
        assertEquals("MLT0", info.get("MakerNoteVersion"));
        assertEquals("DiMAGE A2 or S414", info.get("MinoltaModelID"));
    }

    @Test
    void dngMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/DNG.dng");

        assertEquals("DNG", info.get("FileType"));
        assertEquals("image/x-adobe-dng", info.get("MIMEType"));
        assertEquals("Canon", info.get("Make"));
        assertEquals("Canon EOS 350D DIGITAL", info.get("Model"));
    }
}
