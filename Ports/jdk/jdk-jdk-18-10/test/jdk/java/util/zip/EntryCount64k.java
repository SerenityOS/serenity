/*
 * Copyright (c) 2013 Google Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary Test java.util.zip behavior with ~64k entries
 * @library /test/lib
 * @run main/othervm EntryCount64k
 * @run main/othervm -Djdk.util.zip.inhibitZip64=true EntryCount64k
 * @run main/othervm -Djdk.util.zip.inhibitZip64=false EntryCount64k
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class EntryCount64k {
    public static class Main {
        public static void main(String[] args) {
            System.out.print("Main");
        }
    }

    static final String MAIN_CLASS = "EntryCount64k$Main";
    static final String THIS_CLASS = "EntryCount64k";
    static final String[] SPECIAL_CLASSES = { MAIN_CLASS, THIS_CLASS };
    // static final String[] SPECIAL_CLASSES = { MAIN_CLASS };
    static final int SPECIAL_COUNT = 1 + SPECIAL_CLASSES.length;

    public static void main(String[] args) throws Throwable {
        for (int i = (1 << 16) - 3; i < (1 << 16) + 2; i++)
            test(i);
    }

    static void test(int entryCount) throws Throwable {
        File zipFile = new File("EntryCount64k-tmp.zip");
        zipFile.delete();

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            // Add entries to allow the zip file to be used with "java -jar"
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            for (String line : new String[] {
                     "Manifest-Version: 1.0",
                     "Main-Class: " + MAIN_CLASS,
                 })
                zos.write((line + "\n").getBytes("US-ASCII"));
            zos.closeEntry();

            String testClasses = System.getProperty("test.classes");
            for (String className : SPECIAL_CLASSES) {
                String baseName = className + ".class";
                ZipEntry ze = new ZipEntry(baseName);
                File file = new File(testClasses, baseName);
                zos.putNextEntry(ze);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }

            for (int i = SPECIAL_COUNT; i < entryCount; i++) {
                zos.putNextEntry(new ZipEntry(Integer.toString(i)));
                zos.closeEntry();
            }
        }

        String p = System.getProperty("jdk.util.zip.inhibitZip64");
        boolean tooManyEntries = entryCount >= (1 << 16) - 1;
        boolean shouldUseZip64 = tooManyEntries & !("true".equals(p));
        boolean usesZip64 = usesZip64(zipFile);
        String details = String.format
            ("entryCount=%d shouldUseZip64=%s usesZip64=%s zipSize=%d%n",
             entryCount, shouldUseZip64, usesZip64, zipFile.length());
        System.err.println(details);
        checkCanRead(zipFile, entryCount);
        if (shouldUseZip64 != usesZip64)
            throw new Error(details);
        zipFile.delete();
    }

    static boolean usesZip64(File zipFile) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(zipFile, "r");
        byte[] buf = new byte[4096];
        raf.seek(raf.length() - buf.length);
        raf.read(buf);
        for (int i = 0; i < buf.length - 4; i++) {
            // Look for ZIP64 End Header Signature
            // Phil Katz: yes, we will always remember you
            if (buf[i+0] == 'P' &&
                buf[i+1] == 'K' &&
                buf[i+2] == 6   &&
                buf[i+3] == 6)
                return true;
        }
        return false;
    }

    static void checkCanRead(File zipFile, int entryCount) throws Throwable {
        // Check ZipInputStream API
        try (FileInputStream fis = new FileInputStream(zipFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {
            for (int i = 0; i < entryCount; i++) {
                ZipEntry e = zis.getNextEntry();
                if (i >= SPECIAL_COUNT) // skip special entries
                    if (Integer.parseInt(e.getName()) != i)
                        throw new AssertionError(e.getName());
            }
            if (zis.getNextEntry() != null)
                throw new AssertionError();
        }

        // Check ZipFile API
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            for (int i = 0; i < entryCount; i++) {
                ZipEntry e = en.nextElement();
                if (i >= SPECIAL_COUNT) // skip special entries
                    if (Integer.parseInt(e.getName()) != i)
                        throw new AssertionError();
            }
            if (en.hasMoreElements()
                || (zf.size() != entryCount)
                || (zf.getEntry(Integer.toString(entryCount - 1)) == null)
                || (zf.getEntry(Integer.toString(entryCount)) != null))
                throw new AssertionError();
        }

        // Check java -jar
        String javaHome = System.getProperty("java.home");
        String java = Paths.get(javaHome, "bin", "java").toString();
        String[] cmd = { java, "-jar", zipFile.getName() };
        ProcessBuilder pb = new ProcessBuilder(cmd);
        OutputAnalyzer a = ProcessTools.executeProcess(pb);
        a.shouldHaveExitValue(0);
        a.stdoutShouldMatch("\\AMain\\Z");
        a.stderrShouldMatch("\\A\\Z");
    }
}
