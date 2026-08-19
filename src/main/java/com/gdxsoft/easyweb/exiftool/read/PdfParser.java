package com.gdxsoft.easyweb.exiftool.read;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * PDF parser: version, the Info dictionary (Title/Author/Subject/Keywords/
 * Creator/Producer/CreationDate/ModifyDate) and page count. The Info dict is
 * located by scanning object definitions for the /Info trailer reference.
 */
public final class PdfParser {

    private static final Pattern INFO_REF = Pattern.compile("/Info\\s+(\\d+)\\s+\\d+\\s+R");
    private static final Pattern OBJ_DEF = Pattern.compile("(\\d+)\\s+\\d+\\s+obj", Pattern.MULTILINE);
    private static final Pattern INFO_DICT = Pattern.compile("<<(.*?)>>", Pattern.DOTALL);
    private static final Pattern COUNT = Pattern.compile("/Count\\s+(\\d+)");

    private PdfParser() {}

    public static boolean isPdf(byte[] data) {
        return data.length >= 5 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    public static void process(ExifTool et, byte[] data) {
        et.foundTag("FileType", "PDF", 1, "File", "File");
        et.foundTag("MIMEType", "application/pdf", 1, "File", "File");
        // embedded IPTC (8BIM resource 0x0404) and XMP documents
        parseEmbeddedIptc(et, data);
        parseEmbeddedXmp(et, data);
        String s = new String(data, 0, Math.min(data.length, 64 * 1024), StandardCharsets.ISO_8859_1);
        // version
        Matcher v = Pattern.compile("%PDF-(\\d+\\.\\d+)").matcher(s);
        if (v.find()) {
            et.foundTag("PDFVersion", v.group(1), 1, "PDF", "PDF");
        }
        // page count from /Root catalog (may be in an indirect object)
        Matcher count = COUNT.matcher(s);
        if (count.find()) {
            et.foundTag("PageCount", count.group(1), 1, "PDF", "PDF");
        }
        // Info dictionary reference
        Matcher ref = INFO_REF.matcher(s);
        if (ref.find()) {
            String objNum = ref.group(1);
            Matcher obj = OBJ_DEF.matcher(s);
            int infoPos = -1;
            while (obj.find()) {
                if (obj.group(1).equals(objNum)) {
                    infoPos = obj.start();
                    break;
                }
            }
            if (infoPos >= 0) {
                Matcher dict = INFO_DICT.matcher(s.substring(infoPos));
                if (dict.find()) {
                    parseInfoDict(et, dict.group(1));
                }
            }
        }
    }

    /** Extract key/value pairs from the Info dictionary. */
    private static void parseInfoDict(ExifTool et, String dict) {
        String[] names = {"Title", "Author", "Subject", "Keywords", "Creator", "Producer",
            "CreationDate", "ModDate"};
        for (String name : names) {
            Matcher m = Pattern.compile("/" + name + "\\s*\\((.*?)\\)").matcher(dict);
            if (m.find()) {
                String value = unescapePdfString(m.group(1));
                if ("CreationDate".equals(name) || "ModDate".equals(name)) {
                    value = convertPdfDate(value);
                }
                // Info dictionary is lower priority than the embedded XMP
                et.foundTag(name, value, 0, "PDF", "PDF");
            }
        }
    }

    /** Scan for 8BIM resource blocks and extract the IPTC/NAA (0x0404) data. */
    private static void parseEmbeddedIptc(ExifTool et, byte[] data) {
        int pos = 0;
        while (pos + 12 <= data.length) {
            int idx = indexOf(data, pos, new byte[]{'8', 'B', 'I', 'M'});
            if (idx < 0) {
                break;
            }
            if (idx + 12 <= data.length) {
                int resourceId = Binary.get16u(data, idx + 4, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
                int nameLen = data[idx + 6] & 0xff;
                // Pascal name is padded to an even length
                int sizePos = idx + 7 + nameLen + ((nameLen + 1) & 1);
                if (sizePos + 4 <= data.length) {
                    int size = Binary.get32u(data, sizePos, com.gdxsoft.easyweb.exiftool.ByteOrder.BIG_ENDIAN);
                    int dataStart = sizePos + 4;
                    if (resourceId == 0x0404 && dataStart + size <= data.length) {
                        IptcParser.process(et, data, dataStart, size);
                    }
                    pos = dataStart + size;
                    continue;
                }
            }
            pos = idx + 4;
        }
    }

    /** Scan for embedded XMP documents (<?xpacket or <x:xmpmeta). */
    private static void parseEmbeddedXmp(ExifTool et, byte[] data) {
        int pos = 0;
        while (pos < data.length) {
            int idx = indexOf(data, pos, "<?xpacket".getBytes(StandardCharsets.ISO_8859_1));
            int idx2 = indexOf(data, pos, "<x:xmpmeta".getBytes(StandardCharsets.ISO_8859_1));
            int start;
            if (idx < 0) {
                start = idx2;
            } else if (idx2 < 0) {
                start = idx;
            } else {
                start = Math.min(idx, idx2);
            }
            if (start < 0) {
                break;
            }
            int end = indexOf(data, start, "<?xpacket end".getBytes(StandardCharsets.ISO_8859_1));
            if (end < 0) {
                end = indexOf(data, start, "</x:xmpmeta>".getBytes(StandardCharsets.ISO_8859_1));
            }
            if (end < 0) {
                break;
            }
            int docEnd = Math.min(end + 40, data.length);
            String xml = new String(data, start, docEnd - start, StandardCharsets.UTF_8);
            XmpParser.process(et, xml);
            pos = docEnd;
        }
    }

    private static int indexOf(byte[] data, int from, byte[] needle) {
        outer:
        for (int i = from; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /** PDF date "D:YYYYMMDDHHmmSS+HH'mm'" -> "YYYY:MM:DD HH:mm:SS±HH:mm". */
    private static String convertPdfDate(String s) {
        Matcher m = Pattern.compile("D:(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})"
            + "([+-])(\\d{2})'(\\d{2})'?").matcher(s);
        if (m.matches()) {
            return m.group(1) + ":" + m.group(2) + ":" + m.group(3) + " "
                + m.group(4) + ":" + m.group(5) + ":" + m.group(6)
                + m.group(7) + m.group(8) + ":" + m.group(9);
        }
        return s;
    }

    /** PDF string escapes: octal and backslash sequences, parenthesized nesting. */
    private static String unescapePdfString(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '(' -> sb.append('(');
                    case ')' -> sb.append(')');
                    case '\\' -> sb.append('\\');
                    default -> {
                        if (n >= '0' && n <= '7' && i + 3 < s.length()) {
                            int octal = Integer.parseInt(s.substring(i + 1, i + 4), 8);
                            sb.append((char) octal);
                            i += 3;
                        } else {
                            sb.append(n);
                            i++;
                        }
                    }
                }
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
