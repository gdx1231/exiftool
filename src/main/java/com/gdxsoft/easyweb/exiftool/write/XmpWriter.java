package com.gdxsoft.easyweb.exiftool.write;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XMP document writer: updates existing RDF/XML elements or generates a new
 * document from tag updates. Phase 15 writes all tags with the "xmp:" prefix
 * (the reader maps by local name, so they read back correctly).
 */
public final class XmpWriter {

    /** Tag names that belong to the XMP group (routed to the XMP segment). */
    public static final Set<String> TAGS = Set.of(
        "XMPToolkit", "HasExtendedXMP", "Author", "Creator", "Title", "Description",
        "Rights", "CreationDate", "ModDate", "Producer", "CreateDate", "CreatorTool",
        "Label", "Rating", "Subject", "Language", "MetadataDate");

    private static final String XMP_PREFIX = "<x:xmpmeta xmlns:x='adobe:ns:meta/' x:xmptk='exiftool-java'>\n"
        + "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>\n"
        + " <rdf:Description rdf:about='' xmlns:xmp='http://ns.adobe.com/xap/1.0/'>\n";
    private static final String XMP_SUFFIX = " </rdf:Description>\n</rdf:RDF>\n</x:xmpmeta>\n";

    private static final Pattern ELEMENT = Pattern.compile(
        "<([A-Za-z0-9]+):([A-Za-z0-9]+)>(.*?)</\\1:\\2>", Pattern.DOTALL);

    private XmpWriter() {}

    /**
     * Update an XMP document with the given tag values (local names).
     *
     * @return the updated document bytes, or null if there was no document to
     *         update (caller should build a new one)
     */
    public static byte[] update(byte[] xml, Map<String, Object> updates) {
        String s = new String(xml, StandardCharsets.UTF_8);
        for (Map.Entry<String, Object> u : updates.entrySet()) {
            String tag = u.getKey();
            String value = escapeXml(String.valueOf(u.getValue()));
            String replacement = "<xmp:" + tag + ">" + value + "</xmp:" + tag + ">";
            Matcher m = ELEMENT.matcher(s);
            StringBuilder sb = new StringBuilder();
            boolean found = false;
            while (m.find()) {
                if (m.group(2).equals(tag)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    found = true;
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                }
            }
            m.appendTail(sb);
            s = sb.toString();
            if (!found) {
                // insert before the closing </rdf:Description>
                int idx = s.lastIndexOf(" </rdf:Description>");
                if (idx < 0) {
                    idx = s.lastIndexOf("</rdf:Description>");
                }
                if (idx >= 0) {
                    s = s.substring(0, idx) + "  " + replacement + "\n" + s.substring(idx);
                } else {
                    return null; // not a valid XMP document
                }
            }
        }
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** Build a new minimal XMP document from tag updates. */
    public static byte[] build(Map<String, Object> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xpacket begin='\u00ef\u00bb\u00bf' id='W5M0MpCehiHzreSzNTczkc9d'?>\n");
        sb.append(XMP_PREFIX);
        for (Map.Entry<String, Object> u : updates.entrySet()) {
            sb.append("  <xmp:").append(u.getKey()).append('>')
                .append(escapeXml(String.valueOf(u.getValue())))
                .append("</xmp:").append(u.getKey()).append(">\n");
        }
        sb.append(XMP_SUFFIX);
        sb.append("<?xpacket end='w'?>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
