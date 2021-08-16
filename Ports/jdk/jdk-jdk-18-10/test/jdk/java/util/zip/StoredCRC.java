/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4879507
 * @summary ZipInputStream does not check CRC for stored (uncompressed) files
 * @author Dave Bristor
 */

import java.io.*;
import java.util.Arrays;
import java.util.zip.*;

public class StoredCRC {
    public static void realMain(String[] args) throws Throwable {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        ZipEntry ze = new ZipEntry("test");
        ze.setMethod(ZipOutputStream.STORED);

        String writtenString = "hello, world";
        byte[] writtenData = writtenString.getBytes("ASCII");
        ze.setSize(writtenData.length);
        CRC32 crc = new CRC32();
        crc.update(writtenData);
        ze.setCrc(crc.getValue());

        zos.putNextEntry(ze);
        zos.write(writtenData, 0, writtenData.length);
        zos.close();

        byte[] data = baos.toByteArray();

        // Run with an arg to create a test file that can be used to figure
        // out what position in the data stream can be changed.
        if (args.length > 0) {
            FileOutputStream fos = new FileOutputStream("stored.zip");
            fos.write(data, 0, data.length);
            fos.close();
        } else {
            // Test that reading byte-at-a-time works
            ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(data));
            ze = zis.getNextEntry();
            int pos = 0;
            byte[] readData = new byte[256];
            try {
                int count = zis.read(readData, pos, 1);
                while (count > 0) {
                    count = zis.read(readData, ++pos, 1);
                }
                check(writtenString.equals(new String(readData, 0, pos, "ASCII")));
            } catch (Throwable t) {
                unexpected(t);
            }

            // Test that data corruption is detected.  "offset" was
            // determined to be in the entry's uncompressed data.
            data[getDataOffset(data) + 4] ^= 1;

            zis = new ZipInputStream(
                new ByteArrayInputStream(data));
            ze = zis.getNextEntry();

            try {
                zis.read(readData, 0, readData.length);
                fail("Did not catch expected ZipException" );
            } catch (ZipException ex) {
                String msg = ex.getMessage();
                check(msg != null && msg.startsWith("invalid entry CRC (expected 0x"));
            } catch (Throwable t) {
                unexpected(t);
            }
        }
    }

    public static final int getDataOffset(byte b[]) {
        final int LOCHDR = 30;       // LOC header size
        final int LOCEXT = 28;       // extra field length
        final int LOCNAM = 26;       // filename length
        int lenExt = Byte.toUnsignedInt(b[LOCEXT]) | (Byte.toUnsignedInt(b[LOCEXT + 1]) << 8);
        int lenNam = Byte.toUnsignedInt(b[LOCNAM]) | (Byte.toUnsignedInt(b[LOCNAM + 1]) << 8);
        return LOCHDR + lenExt + lenNam;
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static boolean pass() {passed++; return true;}
    static boolean fail() {failed++; Thread.dumpStack(); return false;}
    static boolean fail(String msg) {System.out.println(msg); return fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
    static boolean equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) return pass();
        else return fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
