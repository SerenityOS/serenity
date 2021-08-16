/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 8190753 8011146
 * @summary Verify that using zip filesystem for opening an outputstream for a zip entry whose
 * compressed size is large, doesn't run into "Negative initial size" exception
 * @run testng/manual/othervm LargeCompressedEntrySizeTest
 */
public class LargeCompressedEntrySizeTest {

    private static final String LARGE_FILE_NAME = "LargeZipEntry.txt";
    private static final String ZIP_FILE_NAME = "8190753-test-compressed-size.zip";

    @BeforeMethod
    public void setUp() throws IOException {
        deleteFiles();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        deleteFiles();
    }

    /**
     * Delete the files created for use by the test
     *
     * @throws IOException if an error occurs deleting the files
     */
    private static void deleteFiles() throws IOException {
        Files.deleteIfExists(Path.of(ZIP_FILE_NAME));
    }


    /**
     * Using zip filesystem, creates a zip file and writes out a zip entry whose compressed size is
     * expected to be greater than 2gb.
     */
    @Test
    public void testLargeCompressedSizeWithZipFS() throws Exception {
        final Path zipFile = Path.of(ZIP_FILE_NAME);
        final long largeEntrySize = 6L * 1024L * 1024L * 1024L; // large value which exceeds Integer.MAX_VALUE
        try (FileSystem fs = FileSystems.newFileSystem(zipFile, Collections.singletonMap("create", "true"))) {
            try (OutputStream os = Files.newOutputStream(fs.getPath(LARGE_FILE_NAME))) {
                long remaining = largeEntrySize;
                // create a chunk of random bytes which we keep writing out
                final int chunkSize = 102400;
                final byte[] chunk = new byte[chunkSize];
                new Random().nextBytes(chunk);
                final long start = System.currentTimeMillis();
                for (long l = 0; l < largeEntrySize; l += chunkSize) {
                    final int numToWrite = (int) Math.min(remaining, chunkSize);
                    os.write(chunk, 0, numToWrite);
                    remaining -= numToWrite;
                }
                System.out.println("Took " + TimeUnit.SECONDS.toSeconds(System.currentTimeMillis() - start)
                        + " seconds to generate entry of size " + largeEntrySize);
            }
        }
    }

}
