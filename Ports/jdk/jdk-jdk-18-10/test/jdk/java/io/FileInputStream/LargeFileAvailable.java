/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6402006 7030573 8011136
 * @summary Test if available returns correct value when reading
 *          a large file.
 * @run main/timeout=300 LargeFileAvailable
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import java.util.concurrent.TimeUnit;

public class LargeFileAvailable {
    public static void main(String args[]) throws Exception {
        // Create a temporary file in the current directory.
        // Use it to check if we have 7G available for
        // a large sparse file test. As a fallback use whatever
        // space is available, so the test can proceed.
        File file = File.createTempFile("largefile", null, new File("."));
        long spaceavailable = file.getUsableSpace();
        long filesize = Math.min(spaceavailable,  7405576182L);
        if (spaceavailable == 0L) {
            // A full disk is considered fatal.
            throw new RuntimeException("No space available for temp file.");
        }

        createLargeFile(filesize, file);

        try (FileInputStream fis = new FileInputStream(file)) {
            if (file.length() != filesize) {
                throw new RuntimeException("unexpected file size = "
                                           + file.length());
            }

            long bigSkip = Math.min(filesize/2, 3110608882L);
            long remaining = filesize;
            remaining -= skipBytes(fis, bigSkip, remaining);
            remaining -= skipBytes(fis, 10L, remaining);
            remaining -= skipBytes(fis, bigSkip, remaining);
            int expected = (remaining >= Integer.MAX_VALUE)
                           ? Integer.MAX_VALUE
                           : (remaining > 0 ? (int) remaining : 0);
            if (fis.available() != expected) {
                throw new RuntimeException("available() returns "
                        + fis.available() + " but expected " + expected);
            }
        } finally {
            file.delete();
        }

        System.out.println("Test succeeded.");
        System.out.flush();
    }

    // Skip toSkip number of bytes and expect that the available() method
    // returns avail number of bytes.
    private static long skipBytes(InputStream is, long toSkip, long avail)
            throws IOException {
        long skip = is.skip(toSkip);
        if (skip != toSkip) {
            throw new RuntimeException("skip() returns " + skip
                                       + " but expected " + toSkip);
        }
        long remaining = avail - skip;
        int expected = (remaining >= Integer.MAX_VALUE)
                       ? Integer.MAX_VALUE
                       : (remaining > 0 ? (int) remaining : 0);

        System.out.println("Skipped " + skip + " bytes, available() returns "
                           + expected + ", remaining " + remaining);
        if (is.available() != expected) {
            throw new RuntimeException("available() returns "
                    + is.available() + " but expected " + expected);
        }
        return skip;
    }

    private static void createLargeFile(long filesize,
                                        File file) throws Exception {
        // Recreate a large file as a sparse file if possible
        Files.delete(file.toPath());

        try (FileChannel fc =
             FileChannel.open(file.toPath(),
                              CREATE_NEW, WRITE, SPARSE)) {
            ByteBuffer bb = ByteBuffer.allocate(1).put((byte)1);
            bb.rewind();
            System.out.println("  Writing large file...");
            long t0 = System.nanoTime();
            int rc = fc.write(bb, filesize - 1);
            long t1 = System.nanoTime();
            System.out.printf("  Wrote large file in %d ns (%d ms) %n",
                t1 - t0, TimeUnit.NANOSECONDS.toMillis(t1 - t0));

            if (rc != 1) {
                throw new RuntimeException("Failed to write 1 byte"
                                           + " to the large file");
            }
        }
        return;
    }
}
