package com.gdxsoft.easyweb.exiftool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Golden tests for XLSX (generated fixture) and PDF embedded IPTC/XMP.
 * PDF reference values from {@code exiftool -json t/images/PDF.pdf} (13.59).
 */
class XlsxPdfMetaGoldenTest {

    private static Map<String, Object> info(String resource) throws IOException {
        try (InputStream in = XlsxPdfMetaGoldenTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " resource missing");
            return new ExifTool().imageInfo(in.readAllBytes());
        }
    }

    @Test
    void xlsxMetadata() throws IOException {
        Map<String, Object> info = info("/test.xlsx");

        assertEquals("XLSX", info.get("FileType"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            info.get("MIMEType"));
        assertEquals("Spreadsheet Title", info.get("Title"));
        assertEquals("XLSX Author", info.get("Creator"));
        assertEquals("Editor", info.get("LastModifiedBy"));
        assertEquals("2024:05:01 10:30:00Z", info.get("CreateDate"));
        assertEquals("Test Co", info.get("Company"));
        assertEquals("Microsoft Excel", info.get("Application"));
    }

    @Test
    void pdfEmbeddedIptc() throws IOException {
        Map<String, Object> info = info("/PDF.pdf");

        // IPTC via the embedded 8BIM resource 0x0404
        assertEquals("A witty caption", info.get("Caption-Abstract"));
        assertEquals("I wrote it", info.get("CaptionWriter"));
        assertEquals("What instructions", info.get("SpecialInstructions"));
        assertEquals("Phil Harvey", info.get("By-line"));
        assertEquals("My Position", info.get("By-lineTitle"));
        assertEquals("Test IPTC picture", info.get("ObjectName"));
        assertEquals("ExifTool, Test, XMP", info.get("Keywords"));
        assertEquals("Copyright 2004 Phil Harvey", info.get("CopyrightNotice"));
    }

    @Test
    void pdfEmbeddedXmp() throws IOException {
        Map<String, Object> info = info("/PDF.pdf");

        // XMP from the embedded Metadata stream
        assertEquals("Test IPTC picture", info.get("Title"));
        assertEquals("Phil Harvey", info.get("Creator"));
        assertEquals("A witty caption", info.get("Description"));
        assertEquals("Copyright 2004 Phil Harvey", info.get("Rights"));
        assertEquals("https://exiftool.org/", info.get("WebStatement"));
        // PDF Info dictionary still intact
        assertEquals("1.3", info.get("PDFVersion"));
        assertEquals("Adobe Photoshop for Macintosh", info.get("Producer"));
        assertEquals("2005:07:18 14:30:45-04:00", info.get("CreationDate"));
    }
}
