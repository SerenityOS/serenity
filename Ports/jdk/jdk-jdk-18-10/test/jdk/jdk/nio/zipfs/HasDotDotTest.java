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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
/**
 * @test
 * @bug 8251329
 * @summary Excercise Zip FS with "." or ".." in a Zip Entry name
 * @modules jdk.zipfs
 * @run testng/othervm HasDotDotTest
 */
public class HasDotDotTest {
    // Zip file to be created
    private static final Path ZIPFILE = Path.of("zipfsDotDotTest.zip");
    // Data for Zip entries
    private static final byte[] ENTRY_DATA =
            "Tennis Anyone".getBytes(StandardCharsets.UTF_8);
    // Display output
    private static final boolean DEBUG = false;

    /**
     * DataProvider containing Zip entry names which should result in an IOException
     * @return Array of Zip entry names
     */
    @DataProvider
    private Object[][] checkForDotOrDotDotPaths() {
        return new Object[][]{
                {"/./foo"},
                {"/../foo"},
                {"/../foo/.."},
                {"/foo/.."},
                {"/foo/."},
                {"/.."},
                {"/."},
                {"/.foo/./"},
                {"/.././"},
        };
    }

    // Zip entry names to create a Zip file with for validating they are not
    // interpreted as a "." or ".." entry
    private final String[] VALID_PATHS =
            {"/foo.txt", "/..foo.txt", "/.foo.txt", "/.foo/bar.txt", "/foo/bar.txt"};
    // Paths to be returned from Files::walk via Zip FS
    private final String[] EXPECTED_PATHS =
            {"/", "/..foo.txt", "/foo.txt", "/.foo.txt", "/.foo",
                    "/.foo/bar.txt", "/foo/bar.txt", "/foo"};

    /**
     * Creates a Zip file
     * @param zip path for Zip to be created
     * @param entries  the entries to add to the Zip file
     * @throws IOException  if an error occurs
     */
    private static void createZip(Path zip, String... entries) throws IOException {
        try (var os = Files.newOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            for (var e : entries) {
                var ze = new ZipEntry(e);
                var crc = new CRC32();
                ze.setMethod(ZipEntry.STORED);
                crc.update(ENTRY_DATA);
                ze.setCrc(crc.getValue());
                ze.setSize(ENTRY_DATA.length);
                zos.putNextEntry(ze);
                zos.write(ENTRY_DATA);
            }
        }
    }

    /**
     * Test to validate an IOException is thrown when opening a Zip file using
     * Zip FS and the path contains a "." or ".."
     * @param path
     * @throws IOException
     */
    @Test(dataProvider = "checkForDotOrDotDotPaths")
    public void hasDotOrDotDotTest(String path) throws IOException {
        if (DEBUG) {
            System.out.printf("Validating entry: %s%n", path);
        }
        Files.deleteIfExists(ZIPFILE);
        createZip(ZIPFILE, path);
        assertThrows(IOException.class, () ->
                FileSystems.newFileSystem(ZIPFILE, Map.of()));
        Files.deleteIfExists(ZIPFILE);
    }

    /**
     * Validate that an entry with a name containing a "." or ".." can be
     * accessed via Files::walk
     * @throws IOException if an error occurs
     */
    @Test
    public void validPaths() throws IOException {
        Files.deleteIfExists(ZIPFILE);
        createZip(ZIPFILE, VALID_PATHS);
        /*
          Walk through the Zip file and collect the Zip FS entries
         */
        try (FileSystem zipfs = FileSystems.newFileSystem(ZIPFILE)) {
            Path zipRoot = zipfs.getPath("/");
            try (Stream<Path> files = Files.walk(zipRoot, Integer.MAX_VALUE)) {
                var entries = files.map(Path::toString)
                        .sorted()
                        .toArray(String[]::new);
                if (DEBUG) {
                    for (String zipEntry : entries) {
                        System.out.println(zipEntry);
                    }
                }
                Arrays.sort(EXPECTED_PATHS);
                assertTrue(Arrays.equals(entries, EXPECTED_PATHS));
            }
        }
        Files.deleteIfExists(ZIPFILE);
    }
}
