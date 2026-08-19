package com.gdxsoft.easyweb.exiftool.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * Command-line interface, mirroring a subset of the {@code exiftool} command
 * (ExifTool 13.59). Supported options:
 *
 * <pre>
 *   exiftool file...                  list tags (Make : value format)
 *   exiftool -s file...               short output (Make: value)
 *   exiftool -json file...            JSON output
 *   exiftool -TAG file...             print only the given tag
 *   exiftool -TAG=VALUE file...       write a tag value (display string)
 *   exiftool -TAG= file...            delete a tag
 *   exiftool -h / --help              usage
 * </pre>
 */
public final class Main {

    /** Selected group family (-1 = none, 0/1/2/3 = family), used by printOne. */
    private static int groupFamily = -1;

    public static void main(String[] args) throws IOException {
        List<String> files = new ArrayList<>();
        Map<String, String> writes = new LinkedHashMap<>();
        List<String> printTags = new ArrayList<>();
        boolean shortOut = false;
        boolean jsonOut = false;
        boolean rawOut = false;

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help") || arg.equals("-help")) {
                usage();
                return;
            } else if (arg.equals("-s") || arg.equals("-s3")) {
                shortOut = true;
            } else if (arg.equals("-json") || arg.equals("-j")) {
                jsonOut = true;
            } else if (arg.equals("-n")) {
                rawOut = true;
            } else if (arg.matches("-G[0-3]?")) {
                groupFamily = arg.length() > 2 ? Integer.parseInt(arg.substring(2)) : 1;
            } else if (arg.startsWith("-") && !arg.equals("-")) {
                String opt = arg.substring(1);
                int eq = opt.indexOf('=');
                if (eq >= 0) {
                    writes.put(opt.substring(0, eq), opt.substring(eq + 1));
                } else {
                    printTags.add(opt);
                }
            } else {
                files.add(arg);
            }
        }

        if (files.isEmpty()) {
            System.err.println("Error: No file specified");
            usage();
            System.exit(1);
        }

        boolean anyError = false;
        for (String file : files) {
            try {
                File f = new File(file);
                if (!f.exists()) {
                    System.err.println("Error: File not found - " + file);
                    anyError = true;
                    continue;
                }
                byte[] data = Files.readAllBytes(f.toPath());
                if (!writes.isEmpty()) {
                    Map<String, Object> updates = new LinkedHashMap<>();
                    for (Map.Entry<String, String> w : writes.entrySet()) {
                        updates.put(w.getKey(), w.getValue().isEmpty() ? null : w.getValue());
                    }
                    byte[] out = new ExifTool().writeImage(data, updates);
                    Files.write(f.toPath(), out);
                    System.out.println("    1 image files updated");
                    continue;
                }
                ExifTool et = new ExifTool();
                Map<String, Object> info = et.imageInfo(data);
                if (rawOut) {
                    Map<String, Object> raw = et.getRawInfo();
                    if (!printTags.isEmpty()) {
                        printSelected(raw, printTags, shortOut, null, null, null, null);
                    } else {
                        printAll(raw, shortOut, null, null, null, null);
                    }
                } else if (jsonOut) {
                    printJson(info, f.getName());
                } else if (!printTags.isEmpty()) {
                    printSelected(info, printTags, shortOut,
                        et.getGroup0(), et.getGroup1(), et.getGroup2(), et.getGroup3());
                } else {
                    printAll(info, shortOut,
                        et.getGroup0(), et.getGroup1(), et.getGroup2(), et.getGroup3());
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                anyError = true;
            }
        }
        if (anyError) {
            System.exit(1);
        }
    }

    private static void printAll(Map<String, Object> info, boolean shortOut,
        Map<String, String> group0, Map<String, String> group1,
        Map<String, String> group2, Map<String, String> group3) {
        for (Map.Entry<String, Object> e : info.entrySet()) {
            printOne(e.getKey(), e.getValue(), shortOut, group0, group1, group2, group3);
        }
    }

    private static void printSelected(Map<String, Object> info, List<String> tags, boolean shortOut,
        Map<String, String> group0, Map<String, String> group1,
        Map<String, String> group2, Map<String, String> group3) {
        for (String tag : tags) {
            Object v = info.get(tag);
            if (v != null) {
                printOne(tag, v, shortOut, group0, group1, group2, group3);
            }
        }
    }

    /** Print one tag with an optional [Group] prefix. */
    private static void printOne(String tag, Object v, boolean shortOut,
        Map<String, String> group0, Map<String, String> group1,
        Map<String, String> group2, Map<String, String> group3) {
        String prefix = "";
        if (groupFamily >= 0) {
            Map<String, String> g = switch (groupFamily) {
                case 0 -> group0;
                case 1 -> group1;
                case 2 -> group2;
                default -> group3;
            };
            String name = g != null ? g.get(tag) : null;
            if (name != null) {
                prefix = "[" + name + "] ";
            }
        }
        if (shortOut) {
            System.out.println(prefix + tag + ": " + v);
        } else {
            System.out.printf("%-30s : %s%n", prefix + tag, v);
        }
    }

    private static void printJson(Map<String, Object> info, String fileName) {
        StringBuilder sb = new StringBuilder("[{\"SourceFile\":\"" + escape(fileName) + "\"");
        for (Map.Entry<String, Object> e : info.entrySet()) {
            sb.append(",\"").append(escape(e.getKey())).append("\":").append(jsonValue(e.getValue()));
        }
        sb.append("}]");
        System.out.println(sb);
    }

    private static String jsonValue(Object v) {
        if (v instanceof Number) {
            return String.valueOf(v);
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", c));
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    private static void usage() {
        System.out.println("""
            exiftool-java (Phase 1-9 port of ExifTool 13.59)

            Usage:
              exiftool [options] file...

            Options:
              -s                short output format
              -json             output as JSON
              -n                print raw values
              -G, -G0, -G1      print group names ([EXIF]/[IFD0]/...)
              -TAG              print only the specified tag
              -TAG=VALUE        write the tag (display string; empty value deletes)
              -h, --help        show this help

            Example:
              exiftool -s -Model photo.jpg
              exiftool -G1 -Make -ExposureTime photo.jpg
              exiftool -Artist="John Doe" photo.jpg
            """);
    }
}
