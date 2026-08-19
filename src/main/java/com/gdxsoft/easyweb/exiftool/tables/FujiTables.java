package com.gdxsoft.easyweb.exiftool.tables;

import java.util.Map;

import com.gdxsoft.easyweb.exiftool.TagInfo;
import com.gdxsoft.easyweb.exiftool.TagTable;
import com.gdxsoft.easyweb.exiftool.ValueConverter;
import com.gdxsoft.easyweb.exiftool.convert.ExifConverters;
import com.gdxsoft.easyweb.exiftool.convert.LookupConverter;

/**
 * FujiFilm maker note tables, ported from {@code Image::ExifTool::FujiFilm}.
 * Phase 4 covers the core tags used by the test image (FujiFilm.jpg).
 */
public final class FujiTables {

    public static final String NAME = "Image::ExifTool::FujiFilm::Main";

    private static final ValueConverter SHARPNESS = new LookupConverter(Map.ofEntries(
        Map.entry("0", "-4 (softest)"), Map.entry("1", "-3 (very soft)"),
        Map.entry("2", "-2 (soft)"), Map.entry("3", "0 (normal)"),
        Map.entry("4", "+2 (hard)"), Map.entry("5", "+3 (very hard)"),
        Map.entry("6", "+4 (hardest)"), Map.entry("130", "-1 (medium soft)"),
        Map.entry("132", "+1 (medium hard)"), Map.entry("32768", "Film Simulation"),
        Map.entry("65535", "n/a")));

    private static final ValueConverter WHITE_BALANCE = new LookupConverter(Map.ofEntries(
        Map.entry("0", "Auto"), Map.entry("1", "Auto (white priority)"),
        Map.entry("2", "Auto (ambiance priority)"), Map.entry("256", "Daylight"),
        Map.entry("512", "Cloudy"), Map.entry("768", "Daylight Fluorescent"),
        Map.entry("769", "Day White Fluorescent"), Map.entry("770", "White Fluorescent"),
        Map.entry("771", "Warm White Fluorescent"),
        Map.entry("772", "Living Room Warm White Fluorescent"),
        Map.entry("1024", "Incandescent"), Map.entry("1280", "Flash"),
        Map.entry("1536", "Underwater"), Map.entry("3840", "Custom"),
        Map.entry("3841", "Custom2"), Map.entry("3842", "Custom3"),
        Map.entry("3843", "Custom4"), Map.entry("3844", "Custom5"),
        Map.entry("4080", "Kelvin")));

    private static final ValueConverter FUJI_FLASH_MODE = new LookupConverter(Map.ofEntries(
        Map.entry("0", "Auto"), Map.entry("1", "On"), Map.entry("2", "Off"),
        Map.entry("3", "Red-eye reduction"), Map.entry("4", "External"),
        Map.entry("16", "Commander"), Map.entry("32768", "Not Attached"),
        Map.entry("33056", "TTL"), Map.entry("33568", "TTL Auto - Did not fire"),
        Map.entry("38976", "Manual"), Map.entry("38944", "Flash Commander"),
        Map.entry("39040", "Multi-flash"), Map.entry("43296", "1st Curtain (front)"),
        Map.entry("43552", "TTL Slow - 1st Curtain (front)"),
        Map.entry("43808", "TTL Auto - 1st Curtain (front)"),
        Map.entry("44320", "TTL - Red-eye Flash - 1st Curtain (front)"),
        Map.entry("44576", "TTL Slow - Red-eye Flash - 1st Curtain (front)"),
        Map.entry("44832", "TTL Auto - Red-eye Flash - 1st Curtain (front)"),
        Map.entry("51488", "2nd Curtain (rear)"),
        Map.entry("51744", "TTL Slow - 2nd Curtain (rear)"),
        Map.entry("52000", "TTL Auto - 2nd Curtain (rear)"),
        Map.entry("52512", "TTL - Red-eye Flash - 2nd Curtain (rear)"),
        Map.entry("52768", "TTL Slow - Red-eye Flash - 2nd Curtain (rear)"),
        Map.entry("53024", "TTL Auto - Red-eye Flash - 2nd Curtain (rear)"),
        Map.entry("59680", "High Speed Sync (HSS)")));

    private static final ValueConverter MACRO = new LookupConverter(Map.of(
        "0", "Off", "1", "On"));

    private static final ValueConverter FOCUS_MODE = new LookupConverter(Map.of(
        "0", "Auto", "1", "Manual", "65535", "Movie"));

    private static final ValueConverter SLOW_SYNC = new LookupConverter(Map.of(
        "0", "Off", "1", "On"));

    private static final ValueConverter PICTURE_MODE = new LookupConverter(Map.ofEntries(
        Map.entry("0", "Auto"), Map.entry("1", "Portrait"), Map.entry("2", "Landscape"),
        Map.entry("3", "Macro"), Map.entry("4", "Sports"), Map.entry("5", "Night Scene"),
        Map.entry("6", "Program AE"), Map.entry("7", "Natural Light"), Map.entry("8", "Anti-blur"),
        Map.entry("9", "Beach & Snow"), Map.entry("10", "Sunset"), Map.entry("11", "Museum"),
        Map.entry("12", "Party"), Map.entry("13", "Flower"), Map.entry("14", "Text"),
        Map.entry("15", "Natural Light & Flash"), Map.entry("16", "Beach"),
        Map.entry("17", "Snow"), Map.entry("18", "Fireworks"), Map.entry("19", "Underwater"),
        Map.entry("20", "Portrait with Skin Correction"), Map.entry("22", "Panorama"),
        Map.entry("23", "Night (tripod)"), Map.entry("24", "Pro Low-light"),
        Map.entry("25", "Pro Focus"), Map.entry("26", "Portrait 2"),
        Map.entry("27", "Dog Face Detection"), Map.entry("28", "Cat Face Detection"),
        Map.entry("48", "HDR"), Map.entry("64", "Advanced Filter"),
        Map.entry("256", "Aperture-priority AE"), Map.entry("512", "Shutter speed priority AE"),
        Map.entry("768", "Manual")));

    private static final ValueConverter AUTO_BRACKETING = new LookupConverter(Map.of(
        "0", "Off", "1", "On", "2", "No flash & flash", "6", "Pixel Shift"));

    private static final ValueConverter BLUR_WARNING = new LookupConverter(Map.of(
        "0", "None", "1", "Blur Warning"));

    private static final ValueConverter FOCUS_WARNING = new LookupConverter(Map.of(
        "0", "Good", "1", "Out of focus"));

    private static final ValueConverter EXPOSURE_WARNING = new LookupConverter(Map.of(
        "0", "Good", "1", "Bad exposure"));

    private FujiTables() {}

    public static TagTable main() {
        return MainHolder.INSTANCE;
    }

    private static final class MainHolder {
        static final TagTable INSTANCE = buildMain();
    }

    private static TagTable buildMain() {
        TagTable t = new TagTable(NAME);
        t.add(TagInfo.builder(0x0, "Version")
            .printConv(ExifConverters.UNDEF_STRING)
            .build());
        t.add(simple(0x1000, "Quality"));
        t.add(simple(0x1001, "Sharpness", SHARPNESS));
        t.add(simple(0x1002, "WhiteBalance", WHITE_BALANCE));
        t.add(simple(0x1010, "FujiFlashMode", FUJI_FLASH_MODE));
        t.add(simple(0x1011, "FlashExposureComp"));
        t.add(simple(0x1020, "Macro", MACRO));
        t.add(simple(0x1021, "FocusMode", FOCUS_MODE));
        t.add(simple(0x1030, "SlowSync", SLOW_SYNC));
        t.add(simple(0x1031, "PictureMode", PICTURE_MODE));
        t.add(simple(0x1100, "AutoBracketing", AUTO_BRACKETING));
        t.add(simple(0x1300, "BlurWarning", BLUR_WARNING));
        t.add(simple(0x1301, "FocusWarning", FOCUS_WARNING));
        t.add(simple(0x1302, "ExposureWarning", EXPOSURE_WARNING));
        return t.register();
    }

    private static TagInfo simple(int tagId, String name) {
        return TagInfo.simple(tagId, name);
    }

    private static TagInfo simple(int tagId, String name, ValueConverter conv) {
        return TagInfo.builder(tagId, name).printConv(conv).build();
    }
}
