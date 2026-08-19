package com.gdxsoft.easyweb.exiftool.read;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gdxsoft.easyweb.exiftool.ByteOrder;
import com.gdxsoft.easyweb.exiftool.ExifTool;

/**
 * XMP (RDF/XML) metadata parser, mirroring {@code Image::ExifTool::XMP}.
 * Standard XMP segments (APP1 "xap/1.0") and extended XMP segments
 * ("xmp/extension/", possibly split into ordered fragments) are parsed.
 * Tags are mapped by their local name (dc:creator -> Creator, pdf:Author ->
 * Author, ...) with ISO 8601 dates converted to EXIF format.
 */
public final class XmpParser {

    public static final String XAP_NS = "http://ns.adobe.com/xap/1.0/\u0000";
    public static final String EXT_NS = "http://ns.adobe.com/xmp/extension/\u0000";

    private static final Pattern XMPTK = Pattern.compile("x:xmptk='([^']*)'");
    private static final Pattern ELEMENT = Pattern.compile(
        "<([A-Za-z0-9]+):([A-Za-z0-9]+)>(.*?)</\\1:\\2>", Pattern.DOTALL);
    private static final Pattern LI = Pattern.compile("<rdf:li[^>]*>(.*?)</rdf:li>", Pattern.DOTALL);
    private static final Pattern DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");

    private XmpParser() {}

    /**
     * Extract and parse all XMP from a JPEG APP1 segment payload.
     * {@code extFragments} collects extended XMP fragments (keyed by GUID) that
     * must be concatenated by offset before parsing.
     */
    public static void processSegment(ExifTool et, byte[] data, int segStart, int segEnd,
        Map<String, List<byte[]>> extFragments) {
        if (hasNs(data, segStart, segEnd, XAP_NS)) {
            int xmlStart = segStart + XAP_NS.length();
            String xml = extractXml(data, xmlStart, segEnd);
            if (xml != null) {
                process(et, xml);
            }
        } else if (hasNs(data, segStart, segEnd, EXT_NS)) {
            // extended XMP: URL + GUID(32) + offset(4) + length(4) + XML fragment
            int p = segStart + EXT_NS.length();
            if (p + 40 > segEnd) {
                return;
            }
            String guid = new String(data, p, 32, java.nio.charset.StandardCharsets.ISO_8859_1);
            int offset = Binary.get32u(data, p + 32, ByteOrder.BIG_ENDIAN);
            int fragStart = p + 40;
            List<byte[]> list = extFragments.computeIfAbsent(guid, k -> new ArrayList<>());
            // store offset-prefixed so fragments can be sorted
            byte[] frag = new byte[segEnd - fragStart + 4];
            frag[0] = (byte) (offset >> 24);
            frag[1] = (byte) (offset >> 16);
            frag[2] = (byte) (offset >> 8);
            frag[3] = (byte) offset;
            System.arraycopy(data, fragStart, frag, 4, segEnd - fragStart);
            list.add(frag);
        }
    }

    /** Parse a complete XMP document (priority 1). */
    public static void process(ExifTool et, String xml) {
        process(et, xml, 1);
    }

    private static void process(ExifTool et, String xml, int priority) {
        Matcher tk = XMPTK.matcher(xml);
        if (tk.find()) {
            et.foundTag("XMPToolkit", tk.group(1), priority, "XMP", "XMP");
        }
        Matcher m = ELEMENT.matcher(xml);
        while (m.find()) {
            String tag = m.group(2); // local name: dc:title -> Title, pdf:Author -> Author
            String body = m.group(3);
            String value;
            Matcher li = LI.matcher(body);
            if (li.find()) {
                // RDF sequence/alternative: join list items with ", "
                StringBuilder sb = new StringBuilder();
                do {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(li.group(1).trim());
                } while (li.find());
                value = sb.toString();
            } else {
                value = body.trim();
            }
            if (value.isEmpty()) {
                continue;
            }
            et.foundTag(tag, fixDate(value), priority, "XMP", "XMP");
        }
    }

    /** Concatenate collected extended-XMP fragments and parse. */
    public static void finishExtended(ExifTool et, Map<String, List<byte[]>> extFragments) {
        for (List<byte[]> fragments : extFragments.values()) {
            // the fragment containing the document start sorts first; the rest
            // follow by their declared offset (equal offsets keep insertion order)
            String startMark = "<x:xmpmeta";
            fragments.sort((a, b) -> {
                boolean aStart = contains(a, startMark);
                boolean bStart = contains(b, startMark);
                if (aStart != bStart) {
                    return aStart ? -1 : 1;
                }
                int oa = offsetOf(a);
                int ob = offsetOf(b);
                return Integer.compare(oa, ob);
            });
            StringBuilder sb = new StringBuilder();
            for (byte[] f : fragments) {
                sb.append(new String(f, 4, f.length - 4, java.nio.charset.StandardCharsets.UTF_8));
            }
            // extended XMP is lower priority than the standard XMP segment
            process(et, sb.toString(), 0);
        }
    }

    private static boolean contains(byte[] frag, String mark) {
        byte[] m = mark.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        for (int i = 4; i + m.length <= frag.length; i++) {
            boolean ok = true;
            for (int j = 0; j < m.length; j++) {
                if (frag[i + j] != m[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }

    private static int offsetOf(byte[] frag) {
        return ((frag[0] & 0xff) << 24) | ((frag[1] & 0xff) << 16)
            | ((frag[2] & 0xff) << 8) | (frag[3] & 0xff);
    }

    /** ISO 8601 "2008-10-20T19:54:15" -> "2008:10:20 19:54:15". */
    private static String fixDate(String value) {
        Matcher d = DATE.matcher(value);
        if (d.matches()) {
            return d.group(1) + ":" + d.group(2) + ":" + d.group(3) + " "
                + d.group(4) + ":" + d.group(5) + ":" + d.group(6);
        }
        return value;
    }

    private static boolean hasNs(byte[] data, int start, int end, String ns) {
        if (end - start < ns.length()) {
            return false;
        }
        byte[] n = ns.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        for (int i = 0; i < n.length; i++) {
            if (data[start + i] != n[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extractXml(byte[] data, int start, int end) {
        // XML starts at "<?xpacket" or "<x:xmpmeta"
        int s = -1;
        for (int i = start; i + 10 <= end; i++) {
            if (data[i] == '<' && (data[i + 1] == '?' || (data[i + 1] == 'x' && data[i + 2] == ':'))) {
                s = i;
                break;
            }
        }
        if (s < 0) {
            return null;
        }
        // find the end of the xmpmeta element
        int e = -1;
        for (int i = s; i + 12 <= end; i++) {
            if (data[i] == '<' && data[i + 1] == '/' && data[i + 2] == 'x' && data[i + 3] == ':'
                && data[i + 4] == 'x' && data[i + 5] == 'm' && data[i + 6] == 'p') {
                e = i;
                break;
            }
        }
        if (e < 0) {
            e = end;
        }
        return new String(data, s, e - s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
