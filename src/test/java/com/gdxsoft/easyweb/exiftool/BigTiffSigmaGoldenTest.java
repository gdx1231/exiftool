package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for BigTIFF (magic 43) and Sigma maker notes (SD10).
 * Reference values from {@code exiftool -json} (ExifTool 13.59).
 */
class BigTiffSigmaGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = BigTiffSigmaGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void bigTiffMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/BigTIFF.btf");

        assertEquals("BTF", info.get("FileType"));
        assertEquals("image/x-tiff-big", info.get("MIMEType"));
        assertEquals("Little-endian (Intel, II)", info.get("ExifByteOrder"));
        assertEquals("8", info.get("ImageWidth"));
        assertEquals("8", info.get("ImageHeight"));
        assertEquals("8 8 8", info.get("BitsPerSample"));
        assertEquals("RGB", info.get("PhotometricInterpretation"));
        assertEquals("3", info.get("SamplesPerPixel"));
    }

    @Test
    void sigmaMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/Sigma.jpg");

        assertEquals("SIGMA", info.get("Make"));
        assertEquals("SIGMA SD10", info.get("Model"));
        assertEquals("02000019", info.get("SerialNumber"));
        assertEquals("SINGLE", info.get("DriveMode"));
        assertEquals("HI", info.get("ResolutionMode"));
        assertEquals("AF-S", info.get("AFMode"));
        assertEquals("AF", info.get("FocusSetting"));
        assertEquals("Sunlight", info.get("WhiteBalance")); // Sigma priority 2
        assertEquals("Multi-segment", info.get("MeteringMode"));
        assertEquals("24 to 70", info.get("LensFocalRange"));
        assertEquals("+0.8", info.get("ExposureCompensation"));
        assertEquals("+0.0", info.get("Contrast"));
        assertEquals("+0.4", info.get("Saturation"));
        assertEquals("+1.0", info.get("Sharpness"));
        assertEquals("+0.0", info.get("X3FillLight"));
        assertEquals("0", info.get("ColorAdjustment"));
        assertEquals("X3F Setting Mode", info.get("AdjustmentMode"));
        assertEquals("12", info.get("Quality"));
        assertEquals("2.0.4.1642 Release", info.get("Firmware"));
    }
}
