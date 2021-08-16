/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010837 8011136
 * @summary Test if available returns correct value when skipping beyond
 *          the end of a file.
 * @author Dan Xu
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class NegativeAvailable {

    public static void main(String[] args) throws IOException {
        final int SIZE = 10;
        final int SKIP = 5;
        final int NEGATIVE_SKIP = -5;

        // Create a temporary file with size of 10 bytes.
        Path tmp = Files.createTempFile(null, null);
        try (BufferedWriter writer =
            Files.newBufferedWriter(tmp, Charset.defaultCharset())) {
            for (int i = 0; i < SIZE; i++) {
                writer.write('1');
            }
        }

        File tempFile = tmp.toFile();
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            if (tempFile.length() != SIZE) {
                throw new RuntimeException("unexpected file size = "
                                           + tempFile.length());
            }
            long space = skipBytes(fis, SKIP, SIZE);
            space = skipBytes(fis, NEGATIVE_SKIP, space);
            space = skipBytes(fis, SKIP, space);
            space = skipBytes(fis, SKIP, space);
            space = skipBytes(fis, SKIP, space);
            space = skipBytes(fis, NEGATIVE_SKIP, space);
            space = skipBytes(fis, NEGATIVE_SKIP, space);
        }
        Files.deleteIfExists(tmp);
    }

    /**
     *  Skip toSkip number of bytes and return the remaining bytes of the file.
     */
    private static long skipBytes(FileInputStream fis, int toSkip, long space)
            throws IOException {
        long skip = fis.skip(toSkip);
        if (skip != toSkip) {
            throw new RuntimeException("skip() returns " + skip
                                       + " but expected " + toSkip);
        }
        long newSpace = space - toSkip;
        long remaining = newSpace > 0 ? newSpace : 0;
        int avail = fis.available();
        if (avail != remaining) {
            throw new RuntimeException("available() returns " + avail
                                       + " but expected " + remaining);
        }

        System.out.println("Skipped " + skip + " bytes "
                           + " available() returns " + avail);
        return newSpace;
    }
}
