package com.gdxsoft.easyweb.exiftool.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

/**
 * End-to-end CLI tests: run the Main class in a subprocess against test images.
 */
class CliTest {

    private static Path extract(String resource) throws Exception {
        try (InputStream in = CliTest.class.getResourceAsStream(resource)) {
            Path tmp = Files.createTempFile("cli", ".tmp");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        }
    }

    private static String run(String... args) throws Exception {
        String cp = System.getProperty("java.class.path");
        String[] cmd = new String[args.length + 4];
        cmd[0] = "java";
        cmd[1] = "-cp";
        cmd[2] = cp;
        cmd[3] = "com.gdxsoft.easyweb.exiftool.cli.Main";
        System.arraycopy(args, 0, cmd, 4, args.length);
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "CLI failed: " + out);
        return out;
    }

    @Test
    void shortOutputSelectedTag() throws Exception {
        Path img = extract("/NikonD70.jpg");
        String out = run("-s", "-Model", img.toString());
        assertTrue(out.contains("Model: NIKON D70"), out);
        Files.deleteIfExists(img);
    }

    @Test
    void jsonOutput() throws Exception {
        Path img = extract("/FujiFilm.jpg");
        String out = run("-json", img.toString());
        assertTrue(out.startsWith("[{"), out);
        assertTrue(out.contains("\"Make\":\"FUJIFILM\""), out);
        assertTrue(out.contains("\"Quality\":\"NORMAL \""), out);
        Files.deleteIfExists(img);
    }

    @Test
    void writeRoundTrip() throws Exception {
        Path img = extract("/Motorola.jpg");
        String w = run("-Artist=CLI Test Artist", img.toString());
        assertTrue(w.contains("1 image files updated"), w);
        String r = run("-s", "-Artist", img.toString());
        assertTrue(r.contains("Artist: CLI Test Artist"), r);
        Files.deleteIfExists(img);
    }

    @Test
    void rawOutput() throws Exception {
        Path img = extract("/NikonD70.jpg");
        String out = run("-n", "-s", "-Orientation", "-ExposureTime", img.toString());
        assertTrue(out.contains("Orientation: 1"), out);
        assertTrue(out.contains("ExposureTime: 0.01666666667"), out);
        Files.deleteIfExists(img);
    }

    @Test
    void groupOutput() throws Exception {
        Path img = extract("/Canon.jpg");
        String out = run("-G1", "-s", "-Make", "-ExposureTime", "-CameraType", img.toString());
        assertTrue(out.contains("[IFD0] Make: Canon"), out);
        assertTrue(out.contains("[ExifIFD] ExposureTime: 4"), out);
        assertTrue(out.contains("[MakerNotes] CameraType: EOS Mid-range"), out);
        String out0 = run("-G0", "-s", "-Make", img.toString());
        assertTrue(out0.contains("[EXIF] Make: Canon"), out0);
        Files.deleteIfExists(img);
    }

    @Test
    void groupFamily2And3() throws Exception {
        Path img = extract("/Canon.jpg");
        String out2 = run("-G2", "-s", "-Make", "-ExposureTime", "-CameraType", "-ExifImageWidth", img.toString());
        assertTrue(out2.contains("[Camera] Make: Canon"), out2);
        assertTrue(out2.contains("[Camera] ExposureTime: 4"), out2);
        assertTrue(out2.contains("[Camera] CameraType: EOS Mid-range"), out2);
        assertTrue(out2.contains("[Image] ExifImageWidth: 160"), out2);
        String out3 = run("-G3", "-s", "-CameraType", "-LensType", img.toString());
        assertTrue(out3.contains("[Canon] CameraType: EOS Mid-range"), out3);
        assertTrue(out3.contains("[Canon] LensType: n/a"), out3);
        Files.deleteIfExists(img);
    }

    @Test
    void noFileError() throws Exception {
        String cp = System.getProperty("java.class.path");
        Process p = new ProcessBuilder("java", "-cp", cp, "com.gdxsoft.easyweb.exiftool.cli.Main")
            .redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        assertTrue(p.waitFor() != 0, "should exit with error");
        assertTrue(out.contains("No file specified"), out);
    }
}
