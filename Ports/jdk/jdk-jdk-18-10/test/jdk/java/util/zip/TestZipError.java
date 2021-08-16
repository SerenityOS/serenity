/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4615343
 * @summary Check that ZipError is thrown instead of InternalError when
 * iterating entries of an invalid zip file
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class TestZipError {
    public static void realMain(String[] args) throws Throwable {
        // Causing a ZipError is hard, especially on non-Windows systems.  See
        // comments below.
        String osName = System.getProperty("os.name");
        if (!System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        String fileName = "error4615343.zip";
        File f = new File(fileName);
        f.delete();
        ZipOutputStream zos;
        ZipEntry ze;

        // Create a zip file with two entries.
        zos = new ZipOutputStream(new FileOutputStream(f));
        ze = new ZipEntry("one");
        zos.putNextEntry(ze);
        zos.write("hello".getBytes());
        zos.closeEntry();
        ze = new ZipEntry("two");
        zos.putNextEntry(ze);
        zos.write("world".getBytes());
        zos.closeEntry();
        zos.close();

        // Open the ZipFile.  This will read the zip file's central
        // directory into in-memory data structures.
        ZipFile zf = new ZipFile(fileName);

        // Delete the file; of course this does not change the in-memory data
        // structures that represent the central directory!
        f.delete();

        // Re-create zip file, with different entries than earlier.  However,
        // recall that we have in-memory information about the central
        // directory of the file at its previous state.
        zos = new ZipOutputStream(new FileOutputStream(f));
        ze = new ZipEntry("uno");
        zos.putNextEntry(ze);
        zos.write("hola".getBytes());
        zos.closeEntry();
        zos.close();

        // Iterate zip file's contents.  On Windows, this will result in a
        // ZipError, because the data in the file differs from the in-memory
        // central directory information we read earlier.
        Enumeration<? extends ZipEntry> entries = zf.entries();
        try {
            while (entries.hasMoreElements()) {
                ze = entries.nextElement();
                zf.getInputStream(ze).readAllBytes();
            }
            fail("Did not get expected exception");
        } catch (ZipException e) {
            pass();
        } catch (InternalError e) {
            fail("Caught InternalError instead of expected ZipError");
        } catch (Throwable t) {
            unexpected(t);
        } finally {
            zf.close();
            f.delete();
        }
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
