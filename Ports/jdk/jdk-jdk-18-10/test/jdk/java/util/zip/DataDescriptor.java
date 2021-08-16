/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6334171
 * @summary Test that zip file's data descriptor is written correctly.
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Bug 6252735 ZipEntry contains wasteful "temporary storage" fields introduced
 * a regression.  The value of the general purpose flag bit in a LOC header is
 * written incorrectly, because it is computed on stale data.
 * <p>
 * With the bug present, zipbytes2 is written incorrectly: when the LOC
 * header is written, (flag(e) & 8) == 8.  This is correct: the data will be
 * compressed, so we don't have data length, etc. yet; that should be written
 * in the DataDescriptor after the data itself is written.  However, when the
 * ZipOutputStream that wraps zipbytes2 is closed, the data length _is_
 * available, therefore (flag(e) & 8) = 0), therefore the DataDescriptor is
 * not written.  This is why, with the bug, zipbytes1.length == sizeof(ext
 * header) + zipbytes2.length.
 * <p>
 * The result is an invalid LOC header in zipbytes2.  So when we again use
 * copyZip, we attempt to read a data length not from the LOC header but from
 * the non-existent EXT header, and at that position in the file is some
 * arbitrary and incorrect value.
 */
public class DataDescriptor {
    static void copyZip(ZipInputStream in, ZipOutputStream out) throws IOException {
        byte[] buffer = new byte[1 << 14];
        for (ZipEntry ze; (ze = in.getNextEntry()) != null; ) {
            out.putNextEntry(ze);
            // When the bug is present, it shows up here.  The second call to
            // copyZip will throw an exception while reading data.
            for (int nr; 0 < (nr = in.read(buffer)); ) {
                out.write(buffer, 0, nr);
            }
        }
        in.close();
    }

    private static void realMain(String[] args) throws Throwable {
        // Create zip output in byte array zipbytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry e = new ZipEntry("testdir/foo");
        byte[] data = "entry data".getBytes("ASCII");
        zos.putNextEntry(e);
        zos.write(data);
        zos.close();
        byte[] zipbytes1 = baos.toByteArray();
        int length1 = zipbytes1.length;
        System.out.println("zip bytes pre-copy length=" + length1);

        // Make a ZipInputStream around zipbytes, and use
        // copyZip to get a new byte array.
        ZipInputStream zis =
            new ZipInputStream(
                new ByteArrayInputStream(zipbytes1));
        baos.reset();
        zos = new ZipOutputStream(baos);
        copyZip(zis, zos);
        zos.close();
        byte[] zipbytes2 = baos.toByteArray();
        int length2 = zipbytes2.length;
        // When the bug is present, pre- and post-copy lengths are different!
        System.out.println("zip bytes post-copy length=" + length2);

        equal(length1, length2);
        check(Arrays.equals(zipbytes1, zipbytes2));

        // Now use copyZip again on the bytes resulting from the previous
        // copy.  When the bug is present, copyZip will get an exception this
        // time.
        baos.reset();
        zos = new ZipOutputStream(baos);
        copyZip(new ZipInputStream(new ByteArrayInputStream(zipbytes2)), zos);
        zos.close();
        byte[] zipbytes3 = baos.toByteArray();
        int length3 = zipbytes3.length;
        System.out.println("zip bytes post-copy length=" + length3);

        equal(length1, length3);
        check(Arrays.equals(zipbytes1, zipbytes3));
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
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
