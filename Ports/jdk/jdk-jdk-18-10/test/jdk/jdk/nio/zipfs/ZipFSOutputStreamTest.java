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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;


/**
 * @test
 * @summary Verify that the outputstream created for zip file entries, through the ZipFileSystem
 * works fine for varying sizes of the zip file entries
 * @bug 8190753 8011146
 * @run testng/timeout=300 ZipFSOutputStreamTest
 */
public class ZipFSOutputStreamTest {
    // List of files to be added to the ZIP file along with their sizes in bytes
    private static final Map<String, Long> ZIP_ENTRIES = Map.of(
            "f1", Integer.MAX_VALUE + 1L, // a value which when cast to an integer, becomes a negative value
            "f2", 25L * 1024L * 1024L, // 25 MB
            "d1/f3", 1234L,
            "d1/d2/f4", 0L);

    private static final Path ZIP_FILE = Path.of("zipfs-outputstream-test.zip");

    @BeforeMethod
    public void setUp() throws IOException {
        deleteFiles();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        deleteFiles();
    }

    private static void deleteFiles() throws IOException {
        Files.deleteIfExists(ZIP_FILE);
    }

    @DataProvider(name = "zipFSCreationEnv")
    private Object[][] zipFSCreationEnv() {
        return new Object[][]{
                {Map.of("create", "true", "noCompression", "true")}, // STORED
                {Map.of("create", "true", "noCompression", "false")} // DEFLATED

        };
    }

    /**
     * Create a zip filesystem and write out entries of varying sizes using the outputstream returned
     * by the ZipFileSystem. Then verify that the generated zip file entries are as expected,
     * both in size and content
     */
    @Test(dataProvider = "zipFSCreationEnv")
    public void testOutputStream(final Map<String, ?> env) throws Exception {
        final byte[] chunk = new byte[1024];
        new Random().nextBytes(chunk);
        try (final FileSystem zipfs = FileSystems.newFileSystem(ZIP_FILE, env)) {
            // create the zip with varying sized entries
            for (final Map.Entry<String, Long> entry : ZIP_ENTRIES.entrySet()) {
                final Path entryPath = zipfs.getPath(entry.getKey());
                if (entryPath.getParent() != null) {
                    Files.createDirectories(entryPath.getParent());
                }
                try (final OutputStream os = Files.newOutputStream(entryPath)) {
                    writeAsChunks(os, chunk, entry.getValue());
                }
            }
        }
        // now verify the written content
        try (final FileSystem zipfs = FileSystems.newFileSystem(ZIP_FILE)) {
            for (final Map.Entry<String, Long> entry : ZIP_ENTRIES.entrySet()) {
                final Path entryPath = zipfs.getPath(entry.getKey());
                try (final InputStream is = Files.newInputStream(entryPath)) {
                    final byte[] buf = new byte[chunk.length];
                    int numRead;
                    long totalRead = 0;
                    while ((numRead = is.read(buf)) != -1) {
                        totalRead += numRead;
                        // verify the content
                        for (int i = 0, chunkoffset = (int) ((totalRead - numRead) % chunk.length);
                             i < numRead; i++, chunkoffset++) {
                            Assert.assertEquals(buf[i], chunk[chunkoffset % chunk.length],
                                    "Unexpected content in " + entryPath);
                        }
                    }
                    Assert.assertEquals(totalRead, (long) entry.getValue(),
                            "Unexpected number of bytes read from zip entry " + entryPath);
                }
            }
        }
    }

    /**
     * Repeatedly writes out to the outputstream, the chunk of data, till the number of bytes
     * written to the stream equals the totalSize
     */
    private static void writeAsChunks(final OutputStream os, final byte[] chunk,
                                      final long totalSize) throws IOException {
        long remaining = totalSize;
        for (long l = 0; l < totalSize; l += chunk.length) {
            final int numToWrite = (int) Math.min(remaining, chunk.length);
            os.write(chunk, 0, numToWrite);
            remaining -= numToWrite;
        }
    }
}
