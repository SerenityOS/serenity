/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8231093
 * @summary Test Zip FS compressionMethod property
 * @modules jdk.zipfs
 * @run testng CompressionModeTest
 */
public class CompressionModeTest {

    private static final Path HERE = Path.of(".");

    /**
     * Number of ZIP entries to create
     */
    private static final int ENTRIES = 5;

    /**
     * Value used for creating the required entries in a ZIP or JAR file
     */
    private static final String ZIP_FILE_VALUE = "US Open 2019";
    private static final byte[] ZIP_FILE_ENTRY =
            ZIP_FILE_VALUE.getBytes(StandardCharsets.UTF_8);

    private static final SecureRandom random = new SecureRandom();

    /**
     * Validate that you can create a ZIP file with and without compression
     * and that entries are created with the specified compression method.
     *
     * @param env         Properties used for creating the ZIP Filesystem
     * @param compression Indicates whether the files are DEFLATED(default)
     *                    or STORED
     * @throws Exception If an error occurs during the creation, verification or
     *                   deletion of the ZIP file
     */
    @Test(dataProvider = "validCompressionMethods", enabled = true)
    public void testValidCompressionMehods(Map<String, String> env,
                                           int compression) throws Exception {

        System.out.printf("ZIP FS Map = %s, Compression mode= %s%n ",
                formatMap(env), compression);

        Path zipfile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipfile);
        createZipFile(zipfile, env, ENTRIES);
        verify(zipfile, compression, ENTRIES, 0);
        Files.deleteIfExists(zipfile);
    }

    /**
     * Validate that an IllegalArgumentException is thrown when an invalid
     * value is specified for the compressionMethod property.
     *
     * @param env Properties used for creating the ZIP Filesystem
     * @throws Exception if an error occurs other than the expected
     * IllegalArgumentException
     */
    @Test(dataProvider = "invalidCompressionMethod")
    public void testInvalidCompressionMethod(Map<String, String> env) throws Exception {
        System.out.printf("ZIP FS Map = %s%n ", formatMap(env));
        Path zipfile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipfile);
        assertThrows(IllegalArgumentException.class, () ->
                createZipFile(zipfile, env, ENTRIES));
        Files.deleteIfExists(zipfile);
    }

    /**
     * Create a ZIP File System using the specified properties and a ZIP file
     * with the specified number of entries
     *
     * @param zipFile Path to the ZIP File to create
     * @param env     Properties used for creating the ZIP Filesystem
     * @param entries Number of entries to add to the ZIP File
     * @throws IOException If an error occurs while creating the ZIP file
     */
    private void createZipFile(Path zipFile, Map<String, String> env,
                               int entries) throws IOException {
        System.out.printf("Creating file = %s%n", zipFile);
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env)) {

            for (int i = 0; i < entries; i++) {
                Files.writeString(zipfs.getPath("Entry-" + i), ZIP_FILE_VALUE);
            }
        }
    }

    /**
     * DataProvider used to validate that you can create a ZIP file with and
     * without compression.
     */
    @DataProvider(name = "validCompressionMethods")
    private Object[][] validCompressionMethods() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED},
                {Map.of("create", "true", "compressionMethod", "STORED"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "compressionMethod", "DEFLATED"),
                        ZipEntry.DEFLATED},
                {Map.of("create", "true", "compressionMethod", "stored"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "compressionMethod", "deflated"),
                        ZipEntry.DEFLATED}
        };
    }

    /**
     * DataProvider used to validate that an IllegalArgumentException is thrown
     * for an invalid value for the compressionMethod property.
     */
    @DataProvider(name = "invalidCompressionMethod")
    private Object[][] invalidCompressionMethod() {
        HashMap<String, String> map = new HashMap<>();
        map.put("create", "true");
        map.put("compressionMethod", null);
        return new Object[][]{
                {map},
                {Map.of("create", "true", "compressionMethod", "")},
                {Map.of("create", "true", "compressionMethod",
                        Integer.parseInt("5"))},
                {Map.of("create", "true", "compressionMethod", "invalid")}
        };
    }

    /**
     * Verify that the given path is a ZIP file containing the
     * expected entries.
     *
     * @param zipfile ZIP file to be validated
     * @param method  Expected Compression method: STORED or DEFLATED
     * @param entries Number of expected entries
     * @param start   Starting number for verifying entries
     * @throws Exception If an error occurs while examining the ZIP file
     */
    private static void verify(Path zipfile, int method, int entries,
                               int start) throws Exception {
        // check entries with ZIP API
        try (ZipFile zf = new ZipFile(zipfile.toFile())) {
            // check entry count
            assertEquals(entries, zf.size());

            // check compression method and content of each entry
            for (int i = start; i < entries; i++) {
                ZipEntry ze = zf.getEntry("Entry-" + i);
                assertNotNull(ze);
                assertEquals(method, ze.getMethod());
                try (InputStream is = zf.getInputStream(ze)) {
                    byte[] bytes = is.readAllBytes();
                    assertTrue(Arrays.equals(bytes, ZIP_FILE_ENTRY));
                }
            }
        }
        // check entries with FileSystem API
        try (FileSystem fs = FileSystems.newFileSystem(zipfile)) {

            // check entry count
            Path top = fs.getPath("/");
            long count = Files.find(top, Integer.MAX_VALUE, (path, attrs) ->
                    attrs.isRegularFile() || (attrs.isDirectory() &&
                            path.getFileName() != null &&
                            path.getFileName().toString().equals("META-INF")))
                    .count();
            assertEquals(entries, count);

            // check content of each entry
            for (int i = start; i < entries; i++) {
                Path file = fs.getPath("Entry-" + i);
                byte[] bytes = Files.readAllBytes(file);
                assertTrue(Arrays.equals(bytes, ZIP_FILE_ENTRY));
            }
        }
    }

    /**
     * Generate a temporary file Path
     *
     * @param dir    Directory used to create the path
     * @param prefix The prefix string used to create the path
     * @param suffix The suffix string used to create the path
     * @return Path that was generated
     */
    private static Path generatePath(Path dir, String prefix, String suffix) {
        long n = random.nextLong();
        String s = prefix + Long.toUnsignedString(n) + suffix;
        Path name = dir.getFileSystem().getPath(s);
        // the generated name should be a simple file name
        if (name.getParent() != null)
            throw new IllegalArgumentException("Invalid prefix or suffix");
        return dir.resolve(name);
    }

    /**
     * Utility method to return a formatted String of the key:value entries for
     * a Map
     *
     * @param env Map to format
     * @return Formatted string of the Map entries
     */
    private static String formatMap(Map<String, String> env) {
        return env.entrySet().stream()
                .map(e -> format("(%s:%s)", e.getKey(), e.getValue()))
                .collect(joining(", "));
    }
}
