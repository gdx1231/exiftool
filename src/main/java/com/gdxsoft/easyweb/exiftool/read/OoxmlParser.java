package com.gdxsoft.easyweb.exiftool.read;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * OOXML (DOCX/XLSX/PPTX) parser: the file is a ZIP archive; metadata lives in
 * docProps/core.xml (title/creator/lastModifiedBy) and docProps/app.xml
 * (pages/words). Entry data is Deflate-inflated then parsed as XML.
 */
public final class OoxmlParser {

    private OoxmlParser() {}

    public static boolean isOoxml(byte[] data) {
        return data.length >= 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4;
    }

    public static void process(ExifTool et, byte[] data) {
        // identify the sub-type from [Content_Types].xml
        String docType = sniffDocType(data);
        et.foundTag("FileType", docType, 1, "File", "File");
        et.foundTag("MIMEType", mimeOf(docType), 1, "File", "File");
        byte[] core = readZipEntry(data, "docProps/core.xml");
        if (core != null) {
            parseCoreXml(et, core);
        }
        byte[] app = readZipEntry(data, "docProps/app.xml");
        if (app != null) {
            parseAppXml(et, app);
        }
    }

    private static String sniffDocType(byte[] data) {
        byte[] types = readZipEntry(data, "[Content_Types].xml");
        if (types == null) {
            return "ZIP";
        }
        String s = new String(types, StandardCharsets.UTF_8);
        if (s.contains("wordprocessingml")) {
            return "DOCX";
        }
        if (s.contains("spreadsheetml")) {
            return "XLSX";
        }
        if (s.contains("presentationml")) {
            return "PPTX";
        }
        return "OOXML";
    }

    private static String mimeOf(String docType) {
        return switch (docType) {
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    /** Find and inflate a ZIP entry by name. */
    private static byte[] readZipEntry(byte[] data, String name) {
        int pos = 0;
        while (pos + 30 <= data.length) {
            if (data[pos] != 'P' || data[pos + 1] != 'K') {
                break;
            }
            int sig = Binary.get32u(data, pos, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            if (sig == 0x02014b50) {
                break; // central directory
            }
            if (sig != 0x04034b50) {
                pos++;
                continue;
            }
            int method = Binary.get16u(data, pos + 8, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            int csize = Binary.get32u(data, pos + 18, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            int usize = Binary.get32u(data, pos + 22, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            int nameLen = Binary.get16u(data, pos + 26, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            int extraLen = Binary.get16u(data, pos + 28, com.gdxsoft.easyweb.exiftool.ByteOrder.LITTLE_ENDIAN);
            int dataStart = pos + 30 + nameLen + extraLen;
            String entryName = new String(data, pos + 30, nameLen, StandardCharsets.UTF_8);
            if (entryName.equals(name) && dataStart + csize <= data.length) {
                byte[] raw = java.util.Arrays.copyOfRange(data, dataStart, dataStart + csize);
                if (method == 0) {
                    return raw;
                }
                if (method == 8) {
                    return inflate(raw, usize);
                }
                return null;
            }
            pos = dataStart + csize;
        }
        return null;
    }

    private static byte[] inflate(byte[] raw, int expectedSize) {
        try {
            // ZIP uses raw deflate (no zlib wrapper)
            Inflater inf = new Inflater(true);
            inf.setInput(raw);
            ByteArrayOutputStream out = new ByteArrayOutputStream(expectedSize > 0 ? expectedSize : 1024);
            byte[] buf = new byte[4096];
            while (!inf.finished() && !inf.needsInput()) {
                int n = inf.inflate(buf);
                if (n == 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
            inf.end();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** core.xml: dc:title / dc:creator / cp:lastModifiedBy. */
    private static void parseCoreXml(ExifTool et, byte[] xml) {
        String s = new String(xml, StandardCharsets.UTF_8);
        String title = textOf(s, "dc:title");
        String creator = textOf(s, "dc:creator");
        String modifier = textOf(s, "cp:lastModifiedBy");
        String created = textOf(s, "dcterms:created");
        if (title != null) {
            et.foundTag("Title", title, 1, "XML", "XML");
        }
        if (creator != null) {
            et.foundTag("Creator", creator, 1, "XML", "XML");
        }
        if (modifier != null) {
            et.foundTag("LastModifiedBy", modifier, 1, "XML", "XML");
        }
        if (created != null) {
            et.foundTag("CreateDate", convertIsoDate(created), 1, "XML", "XML");
        }
    }

    /** app.xml: Pages / Words / Company. */
    private static void parseAppXml(ExifTool et, byte[] xml) {
        String s = new String(xml, StandardCharsets.UTF_8);
        String pages = textOf(s, "Pages");
        String words = textOf(s, "Words");
        String company = textOf(s, "Company");
        String template = textOf(s, "Template");
        if (pages != null) {
            et.foundTag("Pages", pages, 1, "XML", "XML");
        }
        if (words != null) {
            et.foundTag("Words", words, 1, "XML", "XML");
        }
        if (company != null) {
            et.foundTag("Company", company, 1, "XML", "XML");
        }
        if (template != null) {
            et.foundTag("Template", template, 1, "XML", "XML");
        }
    }

    private static String textOf(String xml, String tag) {
        int start = xml.indexOf('<' + tag + '>');
        if (start < 0) {
            start = xml.indexOf('<' + tag + ' ');
        }
        if (start < 0) {
            return null;
        }
        int gt = xml.indexOf('>', start);
        if (gt < 0) {
            return null;
        }
        String endTag = "</" + tag + ">";
        int end = xml.indexOf(endTag, gt);
        if (end < 0) {
            return null;
        }
        String value = xml.substring(gt + 1, end).trim();
        return value.isEmpty() ? null : value;
    }

    private static String convertIsoDate(String s) {
        // 2010-02-03T21:17:48Z -> 2010:02:03 21:17:48Z
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})").matcher(s);
        if (m.find()) {
            return m.group(1) + ":" + m.group(2) + ":" + m.group(3) + " "
                + m.group(4) + ":" + m.group(5) + ":" + m.group(6)
                + (s.endsWith("Z") ? "Z" : "");
        }
        return s;
    }
}
