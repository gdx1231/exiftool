package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for QuickTime / ISO BMFF formats (HEIC, MOV).
 * Reference values generated with {@code exiftool -json t/images/QuickTime.*}
 * (ExifTool 13.59).
 */
class QuickTimeGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = QuickTimeGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void heicMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/QuickTime.heic");

        assertEquals("HEIF", info.get("FileType"));
        assertEquals("image/heif", info.get("MIMEType"));
        assertEquals("High Efficiency Image Format still image (.HEIF)", info.get("MajorBrand"));
        assertEquals("0.0.0", info.get("MinorVersion"));
        assertEquals("mif1, heic, hevc", info.get("CompatibleBrands"));
        assertEquals("Picture", info.get("HandlerType"));
        assertEquals("20002", info.get("PrimaryItemReference"));
        assertEquals("1596", info.get("ImageWidth"));
        assertEquals("1064", info.get("ImageHeight"));
    }

    @Test
    void movMatchesExifToolReference() throws IOException {
        Map<String, Object> info = info("/QuickTime.mov");

        assertEquals("MOV", info.get("FileType"));
        assertEquals("video/quicktime", info.get("MIMEType"));
        assertEquals("0", info.get("MovieHeaderVersion"));
        assertEquals("2005:08:11 14:03:54", info.get("CreateDate"));
        assertEquals("2010:07:30 15:43:59", info.get("ModifyDate"));
        assertEquals("600", info.get("TimeScale"));
        assertEquals("4.97 s", info.get("Duration"));
        assertEquals("2005:08:11 14:03:54", info.get("TrackCreateDate"));
        assertEquals("1", info.get("TrackID"));
        assertEquals("4.97 s", info.get("TrackDuration"));
        assertEquals("320", info.get("ImageWidth"));
        assertEquals("240", info.get("ImageHeight"));
        assertEquals("jpeg", info.get("CompressorID"));
        assertEquals("Pentax", info.get("VendorID"));
    }
}
