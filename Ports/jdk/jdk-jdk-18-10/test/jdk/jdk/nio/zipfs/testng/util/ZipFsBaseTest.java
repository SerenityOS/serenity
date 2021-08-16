/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package util;

import org.testng.annotations.DataProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.*;

public class ZipFsBaseTest {

    protected static final Path HERE = Path.of(".");
    // Enable for permissions output
    protected static final boolean DEBUG = false;
    private static final SecureRandom random = new SecureRandom();

    /**
     * DataProvider used to specify the Zip FS properties to use when creating
     * the Zip File along with the compression method used
     *
     * @return Zip FS properties and compression method used by the tests
     */
    @DataProvider(name = "zipfsMap")
    protected Object[][] zipfsMap() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED}
        };
    }

    /*
     * DataProvider used to verify that an entry can be copied or moved within
     * a Zip file system using a different compression from when the entry
     * was first created
     */
    @DataProvider(name = "copyMoveMap")
    protected Object[][] copyMoveMap() {
        return new Object[][]{
                {Map.of("create", "true"), ZipEntry.DEFLATED, ZipEntry.STORED},
                {Map.of("create", "true", "noCompression", "true"),
                        ZipEntry.STORED, ZipEntry.DEFLATED},
                {Map.of("create", "true", "noCompression", "false"),
                        ZipEntry.DEFLATED, ZipEntry.STORED}
        };
    }

    /**
     * DataProvider with the compression methods to be used for a given test run
     *
     * @return Compression methods to test with
     */
    @DataProvider(name = "compressionMethods")
    protected Object[][] compressionMethods() {
        return new Object[][]{
                {ZipEntry.DEFLATED},
                {ZipEntry.STORED}
        };
    }

    /**
     * Utility method to return a formatted String of the key:value entries for
     * a Map
     *
     * @param env Map to format
     * @return Formatted string of the Map entries
     */
    private static String formatMap(Map<String, ?> env) {
        return env.entrySet().stream()
                .map(e -> format("(%s:%s)", e.getKey(), e.getValue()))
                .collect(joining(", "));
    }

    /**
     * Generate a temporary file Path
     *
     * @param dir    Directory used to create the path
     * @param prefix The prefix string used to create the path
     * @param suffix The suffix string used to create the path
     * @return Path that was generated
     */
    protected static Path generatePath(Path dir, String prefix, String suffix) {
        long n = random.nextLong();
        String s = prefix + Long.toUnsignedString(n) + suffix;
        Path name = dir.getFileSystem().getPath(s);
        // the generated name should be a simple file name
        if (name.getParent() != null)
            throw new IllegalArgumentException("Invalid prefix or suffix");
        return dir.resolve(name);
    }

    /**
     * Verify that the given path is a Zip file containing exactly the
     * given entries.
     */
    protected static void verify(Path zipfile, Entry... entries) throws IOException {
        // check entries with Zip API
        try (ZipFile zf = new ZipFile(zipfile.toFile())) {
            // check entry count
            assertEquals(entries.length, zf.size());

            // Check compression method and content of each entry
            for (Entry e : entries) {
                ZipEntry ze = zf.getEntry(e.name);
                assertNotNull(ze);
                if (DEBUG) {
                    System.out.printf("Entry Name: %s, method: %s, Expected Method: %s%n",
                            e.name, ze.getMethod(), e.method);
                }
                assertEquals(e.method, ze.getMethod());
                try (InputStream in = zf.getInputStream(ze)) {
                    byte[] bytes = in.readAllBytes();
                    if (DEBUG) {
                        System.out.printf("bytes= %s, actual=%s%n",
                                new String(bytes), new String(e.bytes));
                    }

                    assertTrue(Arrays.equals(bytes, e.bytes));
                }
            }
        }

        // Check entries with FileSystem API
        try (FileSystem fs = FileSystems.newFileSystem(zipfile)) {
            // cCheck entry count
            Path top = fs.getPath("/");
            long count = Files.find(top, Integer.MAX_VALUE,
                    (path, attrs) -> attrs.isRegularFile()).count();
            assertEquals(entries.length, count);

            // Check content of each entry
            for (Entry e : entries) {
                Path file = fs.getPath(e.name);
                if (DEBUG) {
                    System.out.printf("Entry name = %s, bytes= %s, actual=%s%n", e.name,
                            new String(Files.readAllBytes(file)), new String(e.bytes));
                }
                assertEquals(Files.readAllBytes(file), e.bytes);
            }
        }
    }

    /**
     * Create a Zip File System using the specified properties and a Zip file
     * with the specified number of entries
     *
     * @param zipFile Path to the Zip File to create/update
     * @param env     Properties used for creating the Zip Filesystem
     * @param source  The path of the file to add to the Zip File
     * @throws IOException If an error occurs while creating/updating the Zip file
     */
    protected void zip(Path zipFile, Map<String, String> env, Path source) throws IOException {
        if (DEBUG) {
            System.out.printf("File:%s, adding:%s%n", zipFile.toAbsolutePath(), source);
        }
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env)) {
            Files.copy(source, zipfs.getPath(source.getFileName().toString()));
        }
    }

    /**
     * Create a Zip File System using the specified properties and a Zip file
     * with the specified number of entries
     *
     * @param zipFile Path to the Zip File to create
     * @param env     Properties used for creating the Zip Filesystem
     * @param entries The entries to add to the Zip File
     * @throws IOException If an error occurs while creating the Zip file
     */
    protected void zip(Path zipFile, Map<String, ?> env,
                    Entry... entries) throws IOException {
        if (DEBUG) {
            System.out.printf("Creating file: %s, env: %s%n", zipFile, formatMap(env));
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            for (Entry e : entries) {
                Path path = zipfs.getPath(e.name);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                Files.write(path, e.bytes);
            }
        }
    }

    /**
     * Recursively remove a Directory
     *
     * @param dir Directory to delete
     * @throws IOException If an error occurs
     */
    protected static void rmdir(Path dir) throws IOException {
        // Nothing to do if the the file does not exist
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }
    }

    /**
     * Represents an entry in a Zip file. An entry encapsulates a name, a
     * compression method, and its contents/data.
     */
    public static class Entry {
        public final String name;
        public final int method;
        public final byte[] bytes;

        public Entry(String name, int method, String contents) {
            this.name = name;
            this.method = method;
            this.bytes = contents.getBytes(StandardCharsets.UTF_8);
        }

        public static Entry of(String name, int method, String contents) {
            return new Entry(name, method, contents);
        }

        /**
         * Returns a new Entry with the same name and compression method as this
         * Entry but with the given content.
         */
        public Entry content(String contents) {
            return new Entry(name, method, contents);
        }
    }
}
