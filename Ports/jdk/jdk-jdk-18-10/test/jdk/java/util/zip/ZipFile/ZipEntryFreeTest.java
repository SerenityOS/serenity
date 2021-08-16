/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6907252
 * @summary ZipFileInputStream Not Thread-Safe
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run main ZipEntryFreeTest
 */

import java.io.*;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.*;
import jdk.test.lib.util.FileUtils;

public class ZipEntryFreeTest extends Thread {

    private static final int NUM_THREADS = 5;
    private static final int TEST_ITERATIONS = 5;
    private static final String ZIPFILE_NAME = "large.zip";
    private static final String ZIPENTRY_NAME = "random.txt";
    private static InputStream is = null;
    final Timer timer = new Timer();

    public static void main(String args[]) throws Exception {
        createZipFile();
        try {
            for (int i = 0; i < TEST_ITERATIONS; i++) {
               runTest();
            }
        } finally {
            FileUtils.deleteFileIfExistsWithRetry(Paths.get(ZIPFILE_NAME));
        }
    }

    private static void runTest() throws Exception {
        try (ZipFile zf = new ZipFile(new File(ZIPFILE_NAME))) {
            is = zf.getInputStream(zf.getEntry(ZIPENTRY_NAME + "_0"));
            Thread[] threadArray = new Thread[NUM_THREADS];
            for (int i = 0; i < threadArray.length; i++) {
                threadArray[i] = new ZipEntryFreeTest();
            }
            for (int i = 0; i < threadArray.length; i++) {
                threadArray[i].start();
            }
            for (int i = 0; i < threadArray.length; i++) {
                threadArray[i].join();
            }
        }
    }

    private static void createZipFile() throws Exception {
        Random rnd = new Random(1000L);
        byte[] contents = new byte[2_000_000];
        ZipEntry ze = null;

        try (ZipOutputStream zos =
            new ZipOutputStream(new FileOutputStream(ZIPFILE_NAME))) {
            // uncompressed mode seemed to tickle the crash
            zos.setMethod(ZipOutputStream.STORED);
            for (int ze_count = 0; ze_count < 10; ze_count++) {
                rnd.nextBytes(contents);
                ze = createZipEntry(contents, ze_count);
                zos.putNextEntry(ze);
                zos.write(contents, 0, contents.length);
            }
            zos.flush();
        }
    }

    private static ZipEntry createZipEntry(byte[] b, int i) {
        ZipEntry ze = new ZipEntry(ZIPENTRY_NAME + "_" + i);
        ze.setCompressedSize(b.length);
        ze.setSize(b.length);
        CRC32 crc = new CRC32();
        crc.update(b);
        ze.setCrc(crc.getValue());
        return ze;
    }

    @Override
    public void run() {
        try {
            int iteration = 0;
            TimerTask tt = (new TimerTask() {
                @Override
                public void run() {
                    try {
                        is.close();
                    } catch (Exception ex) {
                         ex.printStackTrace(System.out);
                    }
                }
            });
            timer.schedule(tt, 50);
            while (is.read() != -1 && iteration++ < 1_000) { }
        } catch (ZipException ze) {
            // ZipException now expected instead of ZIP_Read crash
            System.out.println(ze);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            timer.cancel();
        }
    }
}
