/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6423026
 * @summary Check that it is possible to open more than 2,048 zip files on
 * Windows.
 */

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ManyZipFiles {
    static final int numFiles = 3000;

    public static void realMain(String[] args) throws Throwable {
        // Linux does not yet allow opening this many files; Solaris
        // 8 requires an explicit allocation of more file descriptors
        // to succeed. Since this test is written to check for a
        // Windows capability it is much simpler to only run it
        // on that platform.
        String osName = System.getProperty("os.name");
        if (!(osName.startsWith("Windows"))) {
            return;
        }

        // Create some zip data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry ze = new ZipEntry("test");
            zos.putNextEntry(ze);
            byte[] hello = "hello, world".getBytes("ASCII");
            zos.write(hello, 0, hello.length);
            zos.closeEntry();
            zos.finish();
        }
        byte[] data = baos.toByteArray();

        ZipFile zips[] = new ZipFile[numFiles];

        try {
            // Create a directory for zip files created below
            File tmpdir = new File(
                System.getProperty("java.io.tmpdir")
                + File.separator + "ManyZipFiles");
            if (tmpdir.exists() && !tmpdir.isDirectory()) {
                fail(tmpdir.getAbsolutePath()
                     + " already exists but is not a directory");
                return;
            }
            if (!tmpdir.exists()) {
                if (!tmpdir.mkdirs()) {
                    fail("Couldn't create directory "
                         + tmpdir.getAbsolutePath() + " for test files");
                    return;
                }
            } else if (!tmpdir.canWrite()) {
                fail("Don't have write access for directory "
                     + tmpdir.getAbsolutePath() + " for test files");
                return;
            }
            tmpdir.deleteOnExit();

            // Create and then open a large number of zip files
            for (int i = 0; i < numFiles; i++) {
                File f = File.createTempFile("test", ".zip", tmpdir);
                f.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(data, 0, data.length);
                }
                try {
                    zips[i] = new ZipFile(f);
                } catch (Throwable t) {
                    fail("Failed to open zip file #" + i + " named "
                         + zips[i].getName());
                    throw t;
                }
            }
        } finally {
            // This finally block is due to bug 4171239.  On Windows, if the
            // file is still open at the end of the VM, deleteOnExit won't
            // take place.  "new ZipFile(...)" opens the zip file, so we have
            // to explicitly close those opened above.  This finally block can
            // be removed when 4171239 is fixed. See also 6357433, against which
            // 4171239 was closed as a duplicate.
            for (int i = 0; i < numFiles; i++) {
                if (zips[i] != null) {
                    try {
                        zips[i].close();
                    } catch (Throwable t) {
                        fail("At zip[" + i + "] named " + zips[i].getName()
                             + " caught " + t);
                    }
                }
            }
        }
        pass();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
