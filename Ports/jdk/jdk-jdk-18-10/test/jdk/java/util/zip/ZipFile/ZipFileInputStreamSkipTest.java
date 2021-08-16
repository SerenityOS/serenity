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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8231451
 * @summary Basic tests for ZipFileInputStream::skip
 * @modules jdk.zipfs
 * @run testng/othervm ZipFileInputStreamSkipTest
 */
public class ZipFileInputStreamSkipTest {

    // Stored and Deflated Zip File paths used by the tests
    private final Path STORED_ZIPFILE = Path.of("skipStoredEntries.zip");
    private final Path DEFLATED_ZIPFILE = Path.of("skipDeflatedEntries.zip");

    // Saved Entries added to the relevant Zip file
    private final HashMap<String, Entry> STORED_ZIP_ENTRIES = new HashMap<>();
    private final HashMap<String, Entry> DEFLATED_ZIP_ENTRIES = new HashMap<>();

    /**
     * Create the Zip Files used by the tests
     *
     * @throws IOException If an error occurs creating the Zip Files
     */
    @BeforeClass
    private void createZip() throws IOException {
        Entry e0 = Entry.of("Entry-0", ZipEntry.STORED, "Tennis Pro");
        Entry e1 = Entry.of("Entry-1", ZipEntry.STORED,
                "United States Tennis Association");
        Entry e2 = Entry.of("Entry-2", ZipEntry.DEFLATED, "Cardio Tennis");
        Entry e3 = Entry.of("Entry-3", ZipEntry.DEFLATED, "USTA League Championships");

        // Add entries
        STORED_ZIP_ENTRIES.put(e0.name, e0);
        STORED_ZIP_ENTRIES.put(e1.name, e1);
        DEFLATED_ZIP_ENTRIES.put(e2.name, e2);
        DEFLATED_ZIP_ENTRIES.put(e3.name, e3);

        Files.deleteIfExists(STORED_ZIPFILE);
        Files.deleteIfExists(DEFLATED_ZIPFILE);

        createZipFile(STORED_ZIPFILE,
                Map.of("create", "true", "noCompression", "true"),
                e0, e1);

        createZipFile(DEFLATED_ZIPFILE, Map.of("create", "true"), e2, e3);
    }

    /**
     * Delete Zip Files created for the test
     *
     * @throws IOException If an error occurs during cleanup
     */
    @AfterClass
    private void cleanUp() throws IOException {
        Files.deleteIfExists(STORED_ZIPFILE);
        Files.deleteIfExists(DEFLATED_ZIPFILE);
    }

    /**
     * Validate that you can skip forward within a STORED entry
     * and then read the expected data for the entry
     *
     * @throws Exception If an error occurs during the test
     */
    @Test
    public void testStoredSkip() throws Exception {

        try (ZipFile zf = new ZipFile(STORED_ZIPFILE.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entrySize = entry.getSize();
                long midpoint = entrySize / 2;
                Entry expected = STORED_ZIP_ENTRIES.get(entry.getName());
                assertNotNull(expected);
                try (InputStream in = zf.getInputStream(entry)) {

                    // Check that if we specify 0, that we return the correct
                    // skip value value
                    assertEquals(in.skip(0), 0);

                    // Try to skip past EOF and should return remaining bytes
                    assertEquals(in.skip(entrySize + 100), entrySize);

                    // Return to BOF and then specify a value which would
                    // overflow the projected skip value and return the
                    // number of bytes moved to reach EOF
                    assertEquals(in.skip(-entrySize), -entrySize);
                    assertEquals(in.skip(Long.MAX_VALUE), entrySize);

                    // From midpoint, try to skip past EOF and then skip back
                    // to BOF
                    assertEquals(in.skip(-entrySize), -entrySize);
                    assertEquals(in.skip(midpoint), midpoint);
                    assertEquals(in.skip(1000), entrySize - midpoint);
                    assertEquals(in.skip(-entrySize), -entrySize);

                    // Read remaining bytes and validate against expected bytes
                    byte[] bytes = in.readAllBytes();
                    assertEquals(bytes, expected.bytes);
                    assertEquals(bytes.length, expected.bytes.length);
                }
            }
        }
    }

    /**
     * Validate that you can skip backwards within a STORED entry
     * and then read the expected data for the entry
     *
     * @throws Exception If an error occurs during the test
     */
    @Test
    public void testStoredNegativeSkip() throws Exception {

        try (ZipFile zf = new ZipFile(STORED_ZIPFILE.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entrySize = entry.getSize();
                var midpoint = entrySize / 2;
                Entry expected = STORED_ZIP_ENTRIES.get(entry.getName());
                assertNotNull(expected);
                try (InputStream in = zf.getInputStream(entry)) {

                    // Check that if you try to move past BOF
                    // that we return the correct value
                    assertEquals(in.skip(-1), 0);
                    assertEquals(in.skip(-100), 0);
                    assertEquals(in.skip(Long.MIN_VALUE), 0);

                    // Go to midpoint in file; then specify a value before
                    // BOF which should result in the number of
                    // bytes to BOF returned
                    assertEquals(in.skip(midpoint), midpoint);
                    assertEquals(in.skip(-(midpoint + 10)), -midpoint);

                    // From midpoint, move back a couple of bytes
                    assertEquals(in.skip(midpoint), midpoint);
                    assertEquals(in.skip(-2), -2);

                    // Read the remaining bytes and compare to the expected bytes
                    byte[] bytes = in.readAllBytes();
                    assertEquals(bytes, Arrays.copyOfRange(expected.bytes,
                            (int)midpoint - 2, (int) entrySize));
                    assertEquals(bytes.length, entrySize - midpoint + 2);
                }
            }
        }
    }

    /**
     * Validate that you can skip forward within a DEFLATED entry
     * and then read the expected data for the entry
     *
     * @throws Exception If an error occurs during the test
     */
    @Test
    public void testDeflatedSkip() throws Exception {
        try (ZipFile zf = new ZipFile(DEFLATED_ZIPFILE.toFile())) {
            var toSkip = 5; // Bytes to Skip
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Entry expected = DEFLATED_ZIP_ENTRIES.get(entry.getName());
                assertNotNull(expected);
                try (InputStream in = zf.getInputStream(entry)) {
                    assertEquals(in.skip(toSkip), toSkip);
                    byte[] bytes = in.readAllBytes();
                    var ebytes = Arrays.copyOfRange(expected.bytes,
                            toSkip, expected.bytes.length);
                    assertEquals(bytes, ebytes);
                    assertEquals(bytes.length, expected.bytes.length - toSkip);
                }
            }
        }
    }

    /**
     * Validate that an IllegalArgumentException is thrown if you specify
     * a negative skip value for a DEFLATED entry.
     *
     * @throws Exception If an unexpected error occurs during the test
     */
    @Test
    public void testDeflatedIOException() throws Exception {
        try (ZipFile zf = new ZipFile(DEFLATED_ZIPFILE.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                assertNotNull(entry);
                try (InputStream in = zf.getInputStream(entry)) {
                    // Cannot specify a negative value
                    assertThrows(IllegalArgumentException.class, () -> in.skip((-1)));
                }
            }
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
    private void createZipFile(Path zipFile, Map<String, String> env,
                               Entry... entries) throws IOException {
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

}
