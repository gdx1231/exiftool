package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.GpsConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * The GPS tag table, ported from {@code Image::ExifTool::GPS::Main}.
 */
public final class GpsTables {

    public static final String NAME = "Image::ExifTool::GPS::Main";

    private static final ValueConverter STATUS = new LookupConverter(Map.of(
        "A", "Measurement Active",
        "V", "Measurement Void"));
    private static final ValueConverter MEASURE_MODE = new LookupConverter(Map.of(
        "2", "2-Dimensional Measurement",
        "3", "3-Dimensional Measurement"));
    private static final ValueConverter SPEED_REF = new LookupConverter(Map.of(
        "K", "km/h", "M", "mph", "N", "knots"));
    private static final ValueConverter DIRECTION_REF = new LookupConverter(Map.of(
        "T", "True North", "M", "Magnetic North"));
    private static final ValueConverter DISTANCE_REF = new LookupConverter(Map.of(
        "K", "Kilometers", "M", "Miles", "N", "Nautical Miles"));
    private static final ValueConverter DIFFERENTIAL = new LookupConverter(Map.of(
        "0", "No Correction", "1", "Differential Corrected"));

    private static final class Holder {
        static final TagTable INSTANCE = build();
    }

    private GpsTables() {}

    public static TagTable main() {
        return Holder.INSTANCE;
    }

    private static TagTable build() {
        TagTable t = new TagTable(NAME);
        t.add(simple(0x0000, "GPSVersionID", GpsConverters.VERSION_ID));
        t.add(simple(0x0001, "GPSLatitudeRef", GpsConverters.LAT_REF));
        t.add(coordinate(0x0002, "GPSLatitude"));
        t.add(simple(0x0003, "GPSLongitudeRef", GpsConverters.LON_REF));
        t.add(coordinate(0x0004, "GPSLongitude"));
        t.add(simple(0x0005, "GPSAltitudeRef", GpsConverters.ALTITUDE_REF));
        t.add(simple(0x0006, "GPSAltitude", GpsConverters.ALTITUDE));
        t.add(TagInfo.builder(0x0007, "GPSTimeStamp")
            .valueConv(GpsConverters.TIME_STAMP)
            .printConv(GpsConverters.PRINT_TIME_STAMP)
            .build());
        t.add(simple(0x0008, "GPSSatellites"));
        t.add(simple(0x0009, "GPSStatus", STATUS));
        t.add(simple(0x000A, "GPSMeasureMode", MEASURE_MODE));
        t.add(simple(0x000B, "GPSDOP"));
        t.add(simple(0x000C, "GPSSpeedRef", SPEED_REF));
        t.add(simple(0x000D, "GPSSpeed"));
        t.add(simple(0x000E, "GPSTrackRef", DIRECTION_REF));
        t.add(simple(0x000F, "GPSTrack"));
        t.add(simple(0x0010, "GPSImgDirectionRef", DIRECTION_REF));
        t.add(simple(0x0011, "GPSImgDirection"));
        t.add(simple(0x0012, "GPSMapDatum"));
        t.add(simple(0x0013, "GPSDestLatitudeRef", GpsConverters.LAT_REF));
        t.add(coordinate(0x0014, "GPSDestLatitude"));
        t.add(simple(0x0015, "GPSDestLongitudeRef", GpsConverters.LON_REF));
        t.add(coordinate(0x0016, "GPSDestLongitude"));
        t.add(simple(0x0017, "GPSDestBearingRef", DIRECTION_REF));
        t.add(simple(0x0018, "GPSDestBearing"));
        t.add(simple(0x0019, "GPSDestDistanceRef", DISTANCE_REF));
        t.add(simple(0x001A, "GPSDestDistance"));
        t.add(simple(0x001B, "GPSProcessingMethod"));
        t.add(simple(0x001C, "GPSAreaInformation"));
        t.add(simple(0x001D, "GPSDateStamp"));
        t.add(simple(0x001E, "GPSDifferential", DIFFERENTIAL));
        t.add(simple(0x001F, "GPSHPositioningError"));
        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }

    /** Latitude/longitude pair: ValueConv ToDegrees + PrintConv ToDMS. */
    private static TagInfo coordinate(int tagId, String name) {
        return TagInfo.builder(tagId, name)
            .valueConv(GpsConverters.TO_DEGREES)
            .printConv(GpsConverters.TO_DMS)
            .build();
    }
}
