package com.gdxsoft.easyweb.exiftool;

import java.util.Map;

/** Quick manual verification: dump extracted metadata for a sample image. */
public final class DumpInfo {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: DumpInfo <image>");
            System.exit(1);
        }
        Map<String, Object> info = new ExifTool().imageInfo(new java.io.File(args[0]));
        for (Map.Entry<String, Object> e : info.entrySet()) {
            System.out.println(e.getKey() + "\t" + e.getValue());
        }
    }
}
