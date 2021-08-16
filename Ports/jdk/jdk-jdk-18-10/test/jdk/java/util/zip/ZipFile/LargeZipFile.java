/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public class LargeZipFile {
    // If true, don't delete large ZIP file created for test.
    static final boolean debug = System.getProperty("debug") != null;

    static final int DATA_LEN = 1024 * 1024;
    static final int DATA_SIZE = 8;

    static long fileSize = 3L * 1024L * 1024L * 1024L; // 3GB

    static boolean userFile = false;

    static byte[] data;
    static File largeFile;
    static String lastEntryName;

    /* args can be empty, in which case check a 3 GB file which is created for
     * this test (and then deleted).  Or it can be a number, in which case
     * that designates the size of the file that's created for this test (and
     * then deleted).  Or it can be the name of a file to use for the test, in
     * which case it is *not* deleted.  Note that in this last case, the data
     * comparison might fail.
     */
    static void realMain (String[] args) throws Throwable {
        if (args.length > 0) {
            try {
                fileSize = Long.parseLong(args[0]);
                System.out.println("Testing with file of size " + fileSize);
            } catch (NumberFormatException ex) {
                largeFile = new File(args[0]);
                if (!largeFile.exists()) {
                    throw new Exception("Specified file " + args[0] + " does not exist");
                }
                userFile = true;
                System.out.println("Testing with user-provided file " + largeFile);
            }
        }
        File testDir = null;
        if (largeFile == null) {
            testDir = new File(System.getProperty("test.scratch", "."),
                                    "LargeZip");
            if (testDir.exists()) {
                if (!testDir.delete()) {
                    throw new Exception("Cannot delete already-existing test directory");
                }
            }
            check(!testDir.exists() && testDir.mkdirs());
            largeFile = new File(testDir, "largezip.zip");
            createLargeZip();
        }

        readLargeZip();

        if (!userFile && !debug) {
            check(largeFile.delete());
            check(testDir.delete());
        }
    }

    static void createLargeZip() throws Throwable {
        int iterations = DATA_LEN / DATA_SIZE;
        ByteBuffer bb = ByteBuffer.allocate(DATA_SIZE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < iterations; i++) {
            bb.putDouble(0, Math.random());
            baos.write(bb.array(), 0, DATA_SIZE);
        }
        data = baos.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(largeFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos))
        {
            long length = 0;
            while (length < fileSize) {
                ZipEntry ze = new ZipEntry("entry-" + length);
                lastEntryName = ze.getName();
                zos.putNextEntry(ze);
                zos.write(data, 0, data.length);
                zos.closeEntry();
                length = largeFile.length();
            }
            System.out.println("Last entry written is " + lastEntryName);
        }
    }

    static void readLargeZip() throws Throwable {
        try (ZipFile zipFile = new ZipFile(largeFile)) {
            ZipEntry entry = null;
            String entryName = null;
            int count = 0;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                entryName = entry.getName();
                count++;
            }
            System.out.println("Number of entries read: " + count);
            System.out.println("Last entry read is " + entryName);
            check(!entry.isDirectory());
            if (check(entryName.equals(lastEntryName))) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = zipFile.getInputStream(entry);
                byte buf[] = new byte[4096];
                int len;
                while ((len = is.read(buf)) >= 0) {
                    baos.write(buf, 0, len);
                }
                baos.close();
                is.close();
                check(Arrays.equals(data, baos.toByteArray()));
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void pass(String msg) {System.out.println(msg); passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void unexpected(Throwable t, String msg) {
        System.out.println(msg); failed++; t.printStackTrace();}
    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
