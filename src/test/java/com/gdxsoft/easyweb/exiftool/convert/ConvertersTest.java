package com.gdxsoft.easyweb.exiftool.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gdxsoft.easyweb.exiftool.ExifFormat;
import org.junit.jupiter.api.Test;

class ConvertersTest {

    @Test
    void exifFormatSizes() {
        assertEquals(1, ExifFormat.INT8U.size());
        assertEquals(2, ExifFormat.INT16U.size());
        assertEquals(4, ExifFormat.INT32U.size());
        assertEquals(8, ExifFormat.RATIONAL64U.size());
        assertEquals(8, ExifFormat.DOUBLE.size());
    }

    @Test
    void exifFormatLookup() {
        assertEquals(ExifFormat.INT16U, ExifFormat.fromCode(3));
        assertEquals(ExifFormat.RATIONAL64S, ExifFormat.fromName("rational64s"));
        assertEquals(ExifFormat.NONE, ExifFormat.fromCode(99));
    }

    @Test
    void exposureTimeFraction() {
        assertEquals("1/125", ExifConverters.EXPOSURE_TIME.convert(0.008));
        assertEquals("1/1961", ExifConverters.EXPOSURE_TIME.convert(0.00050994));
    }

    @Test
    void exposureTimeDecimal() {
        assertEquals("0.5", ExifConverters.EXPOSURE_TIME.convert(0.5));
        assertEquals("2", ExifConverters.EXPOSURE_TIME.convert(2.0));
        assertEquals("1.5", ExifConverters.EXPOSURE_TIME.convert(1.5));
    }

    @Test
    void fNumber() {
        assertEquals("2.0", ExifConverters.F_NUMBER.convert(2.0));
        assertEquals("5.6", ExifConverters.F_NUMBER.convert(5.6));
        assertEquals("0.95", ExifConverters.F_NUMBER.convert(0.95));
    }

    @Test
    void focalLength() {
        assertEquals("4.7 mm", ExifConverters.FOCAL_LENGTH.convert(4.7));
    }

    @Test
    void orientationLookup() {
        assertEquals("Horizontal (normal)", ExifConverters.ORIENTATION.convert(1L));
        assertEquals("Rotate 90 CW", ExifConverters.ORIENTATION.convert(6L));
        // unknown values become "Unknown (N)"
        assertEquals("Unknown (99)", ExifConverters.ORIENTATION.convert(99L));
    }

    @Test
    void singleByteUndefLookup() {
        // SceneType: single byte 0x01 -> "Directly photographed"
        assertEquals("Directly photographed", ExifConverters.SCENE_TYPE.convert(new byte[]{1}));
        assertEquals("Film Scanner", ExifConverters.FILE_SOURCE.convert(new byte[]{1}));
        assertEquals("Digital Camera", ExifConverters.FILE_SOURCE.convert(new byte[]{3}));
    }

    @Test
    void componentsConfiguration() {
        assertEquals("Y, Cb, Cr, -", ExifConverters.COMPONENTS_CONFIGURATION.convert("1 2 3 0"));
    }

    @Test
    void undefString() {
        assertEquals("0220", ExifConverters.UNDEF_STRING.convert(new byte[]{'0', '2', '2', '0'}));
        assertEquals("0232", ExifConverters.UNDEF_STRING.convert(new byte[]{'0', '2', '3', '2', 0, 0}));
    }

    @Test
    void shutterSpeedApex() {
        // APEX 10.91 -> 2^-10.91 seconds -> ~0.0005198 -> "1/1924"
        assertEquals("1/1924", ExifConverters.EXPOSURE_TIME.convert(
            ExifConverters.SHUTTER_SPEED_APEX.convert(10.91)));
    }

    @Test
    void apertureApex() {
        // APEX 2.0 -> 2^(2/2) = 2.0
        assertEquals(2.0, (Double) ExifConverters.APERTURE_APEX.convert(2.0), 1e-9);
        assertEquals("2.0", ExifConverters.APERTURE_PRINT.convert(2.0));
    }

    @Test
    void gpsToDegrees() {
        // "51 30 36" -> 51 + (30 + 36/60)/60 = 51.51
        assertEquals(51.51, (Double) GpsConverters.TO_DEGREES.convert("51 30 36"), 1e-9);
    }

    @Test
    void gpsToDms() {
        assertEquals("51 deg 30' 36.00\"", GpsConverters.TO_DMS.convert(51.51));
        assertEquals("0 deg 0' 0.00\"", GpsConverters.TO_DMS.convert(0.0));
    }

    @Test
    void gpsTimeStamp() {
        assertEquals("10:30:36", GpsConverters.TIME_STAMP.convert("10 30 36"));
        assertEquals("02:03:00", GpsConverters.TIME_STAMP.convert("2 3 0"));
    }

    @Test
    void gpsAltitude() {
        assertEquals("123.4 m", GpsConverters.ALTITUDE.convert(123.4));
    }

    @Test
    void gpsVersionId() {
        assertEquals("2.3.0.0", GpsConverters.VERSION_ID.convert("2 3 0 0"));
    }
}
