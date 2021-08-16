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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8231766
 * @summary Test Files::copy and Files::move with Zip FS
 * @modules jdk.zipfs
 * @run testng/othervm CopyMoveTests
 */
public class CopyMoveTests {
    // Enable debugging output
    private static final boolean DEBUG = false;
    // Path to current directory
    private static final Path HERE = Path.of(".");
    // Value to use when creating Zip Entries
    private static final String ZIP_FILE_VALUE = "US Open 2019";
    // Value used to create the OS file to be copied into/from a Zip File
    private static final String OS_FILE_VALUE = "Hello World!";
    private static final SecureRandom random = new SecureRandom();

    /*
     * DataProvider used to verify that a FileAlreadyExistsException is
     * thrown with copying a file without the REPLACE_EXISTING option
     */
    @DataProvider(name = "zipfsMap")
    private Object[][] zipfsMap() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED}
        };
    }

    /*
     * DataProvider used to verify that an entry may be copied or moved within
     * a Zip file system with the correct compression method
     */
    @DataProvider(name = "copyMoveMap")
    private Object[][] copyMoveMap() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED, ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED, ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED, ZipEntry.STORED}
        };
    }

    /**
     * Validate that an entry that is copied within a Zip file is copied with
     * the correct compression
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when copying the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void copyTest(Map<String, String> createMap, int compression,
                         int expectedCompression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);
        Entry e00 = Entry.of("Entry-00", expectedCompression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);

        // Create the Zip File with the initial entries
        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile,
                Map.of("noCompression", expectedCompression == ZipEntry.STORED))) {
            Files.copy(zipfs.getPath(e0.name), zipfs.getPath(e00.name));
        }
        // Verify entries e0, e1 and e00 exist
        verify(zipFile, e0, e1, e00);
        Files.deleteIfExists(zipFile);

    }

    /**
     * Validate that an entry that is copied from one Zip file to another,
     * is copied with the correct compression
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when copying the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void copyZipToZipTest(Map<String, String> createMap, int compression,
                            int expectedCompression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);
        Entry e00 = Entry.of("Entry-00", expectedCompression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, createMap);
             FileSystem zipfsTarget = FileSystems.newFileSystem(zipFile2,
                     Map.of("create", "true", "noCompression",
                             expectedCompression == ZipEntry.STORED))) {
            Files.copy(zipfs.getPath(e0.name), zipfsTarget.getPath(e00.name));
        }
        // Only 1 entry copied to the secondary Zip file
        verify(zipFile2, e00);
        // Verify  entries e0 and e1 remain in the original Zip file
        verify(zipFile, e0, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate that an external file copied to a Zip file is copied with
     * the correct compression
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when copying the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void copyFromOsTest(Map<String, String> createMap, int compression,
                           int expectedCompression) throws Exception {

        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(osFile);
        Files.writeString(osFile, OS_FILE_VALUE);
        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);
        Entry e00 = Entry.of("Entry-00", expectedCompression, OS_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile,
                Map.of("noCompression", expectedCompression == ZipEntry.STORED))) {
            Files.copy(osFile, zipfs.getPath(e00.name));
        }
        verify(zipFile, e0, e1, e00);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate that an entry that is copied from a Zip file to an OS file contains
     * the correct bytes and the file remains in the Zip file
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when moving the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void CopyFromZipTest(Map<String, String> createMap, int compression,
                                int expectedCompression) throws Exception {

        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, Map.of())) {
            Files.copy(zipfs.getPath(e0.name), osFile);
        }

        // Entries e0 and e1 should exist
        verify(zipFile, e0, e1);
        // Check to see if the file exists and the bytes match
        assertTrue(Files.isRegularFile(osFile));
        assertEquals(Files.readAllBytes(osFile), e0.bytes);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate that an entry that is moved within a Zip file is moved with
     * the correct compression
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when moving the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void moveTest(Map<String, String> createMap, int compression,
                         int expectedCompression) throws Exception {

        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);
        Entry e00 = Entry.of("Entry-00", expectedCompression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile,
                Map.of("noCompression", expectedCompression == ZipEntry.STORED))) {
            Files.move(zipfs.getPath(e0.name), zipfs.getPath(e00.name));
        }
        // Entry e0 should not exist but Entry e00 should
        verify(zipFile, e1, e00);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate that an entry that is moved one Zip file to another is moved with
     * the correct compression
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when moving the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void moveZipToZipTest(Map<String, String> createMap, int compression,
                            int expectedCompression) throws Exception {

        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);
        Entry e00 = Entry.of("Entry-00", expectedCompression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile,
                Map.of("noCompression", expectedCompression == ZipEntry.STORED));
             FileSystem zipfsTarget = FileSystems.newFileSystem(zipFile2,
                     Map.of("create", "true", "noCompression",
                             expectedCompression == ZipEntry.STORED))) {
            Files.move(zipfs.getPath(e0.name), zipfsTarget.getPath(e00.name));
        }
        // Only Entry e00 should exist
        verify(zipFile2, e00);
        // Only Entry e1 should exist
        verify(zipFile, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate that an entry that is moved from a Zip file to an OS file contains
     * the correct bytes and is removed from the Zip file
     *
     * @param createMap           Zip FS properties to use when creating the Zip File
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when moving the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap", enabled = true)
    public void moveFromZipTest(Map<String, String> createMap, int compression,
                            int expectedCompression) throws Exception {

        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, Map.of())) {
            Files.move(zipfs.getPath(e0.name), osFile);
        }

        // Only Entry e1 should exist
        verify(zipFile, e1);
        // Check to see if the file exists and the bytes match
        assertTrue(Files.isRegularFile(osFile));
        assertEquals(Files.readAllBytes(osFile), e0.bytes);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate that a FileAlreadyExistsException is thrown when copying a
     * file and not specifying the REPLACE_EXISTING option.
     *
     * @param createMap Properties used for creating the ZIP Filesystem
     * @throws Exception if an error occurs
     */
    @Test(dataProvider = "zipfsMap", enabled = true)
    public void testFAEWithCopy(Map<String, String> createMap,
                                int compression) throws Exception {
        if (DEBUG) {
            System.out.printf("ZIP FS Map = %s%n ", formatMap(createMap));
        }
        Entry e0 = Entry.of("Entry-0", compression, ZIP_FILE_VALUE);
        Entry e1 = Entry.of("Entry-1", compression, ZIP_FILE_VALUE);

        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);

        createZipFile(zipFile, createMap, e0, e1);
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, createMap)) {
            assertThrows(FileAlreadyExistsException.class, () ->
                    Files.copy(zipfs.getPath(e0.name),
                            zipfs.getPath(e1.name)));
        }
        Files.deleteIfExists(zipFile);
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

    /**
     * Create a Zip file using the Zip File System with the specified
     * Zip File System properties
     *
     * @param zipFile Path to the Zip File to create
     * @param env     Properties used for creating the Zip Filesystem
     * @param entries The entries to add to the Zip File
     * @throws IOException If an error occurs while creating the Zip file
     */
    private void createZipFile(Path zipFile, Map<String, String> env,
                               Entry... entries) throws IOException {
        if (DEBUG) {
            System.out.printf("Creating Zip file: %s with the Properties: %s%n",
                    zipFile, formatMap(env));
        }
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env)) {
            for (Entry e : entries) {
                Files.writeString(zipfs.getPath(e.name), new String(e.bytes));
            }
        }
    }

    /**
     * Represents an entry in a Zip file. An entry encapsulates a name, a
     * compression method, and its contents/data.
     */
    static class Entry {
        private final String name;
        private final int method;
        private final byte[] bytes;

        Entry(String name, int method, String contents) {
            this.name = name;
            this.method = method;
            this.bytes = contents.getBytes(StandardCharsets.UTF_8);
        }

        static Entry of(String name, int method, String contents) {
            return new Entry(name, method, contents);
        }

        /**
         * Returns a new Entry with the same name and compression method as this
         * Entry but with the given content.
         */
        Entry content(String contents) {
            return new Entry(name, method, contents);
        }
    }

    /**
     * Verify that the given path is a Zip file containing exactly the
     * given entries.
     */
    private static void verify(Path zipfile, Entry... entries) throws IOException {
        // check entries with zip API
        try (ZipFile zf = new ZipFile(zipfile.toFile())) {
            // check entry count
            assertEquals(entries.length, zf.size());

            // check compression method and content of each entry
            for (Entry e : entries) {
                ZipEntry ze = zf.getEntry(e.name);
                //System.out.printf("Name: %s, method: %s, Expected Method: %s%n", e.name, ze.getMethod(), e.method);
                assertNotNull(ze);
                assertEquals(e.method, ze.getMethod());
                try (InputStream in = zf.getInputStream(ze)) {
                    byte[] bytes = in.readAllBytes();
                    assertTrue(Arrays.equals(bytes, e.bytes));
                }
            }
        }

        // check entries with FileSystem API
        try (FileSystem fs = FileSystems.newFileSystem(zipfile)) {
            // check entry count
            Path top = fs.getPath("/");
            long count = Files.find(top, Integer.MAX_VALUE,
                    (path, attrs) -> attrs.isRegularFile()).count();
            assertEquals(entries.length, count);

            // check content of each entry
            for (Entry e : entries) {
                Path file = fs.getPath(e.name);
                byte[] bytes = Files.readAllBytes(file);
                assertTrue(Arrays.equals(bytes, e.bytes));
            }
        }
    }
}
