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

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.spi.ToolProvider;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8229887
 * @summary Validate ZIP FileSystem can replace existing STORED and DEFLATED entries
 * @modules jdk.zipfs
 * @run testng UpdateEntryTest
 */
@Test
public class UpdateEntryTest {

    private static final Path HERE = Path.of(".");

    // Use the ToolProvider interface for accessing the jar tool
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found")
            );

    /**
     * Represents an entry in a ZIP file. An entry encapsulates a name, a
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

        /**
         * Writes this entry to the given ZIP output stream.
         */
        ZipEntry put(ZipOutputStream zos) throws IOException {
            ZipEntry e = new ZipEntry(name);
            e.setMethod(method);
            e.setTime(System.currentTimeMillis());
            if (method == ZipEntry.STORED) {
                var crc = new CRC32();
                crc.update(bytes);
                e.setCrc(crc.getValue());
                e.setSize(bytes.length);
            }
            zos.putNextEntry(e);
            zos.write(bytes);
            return e;
        }
    }

    /**
     * Validate that you can replace an existing entry in a JAR file that
     * was added with the STORED(no-compression) option
     */
    public void testReplaceStoredEntry() throws IOException {
        String jarFileName = "updateStoredEntry.jar";
        String storedFileName = "storedFile.txt";
        String replacedValue = "bar";
        Path zipFile = Path.of(jarFileName);

        // Create JAR file with a STORED(non-compressed) entry
        Files.writeString(Path.of(storedFileName), "foobar");
        JAR_TOOL.run(System.out, System.err,
                "cM0vf", jarFileName, storedFileName);

        // Replace the STORED entry using the default(DEFLATED) compression
        // method.
        try (FileSystem fs = FileSystems.newFileSystem(zipFile)) {
            Files.writeString(fs.getPath(storedFileName), replacedValue);
        }
        Entry e1 = Entry.of(storedFileName, ZipEntry.DEFLATED, replacedValue);
        verify(zipFile, e1);
    }

    /**
     * Test updating an entry that is STORED (not compressed)
     */
    public void test1() throws IOException {
        Entry e1 = Entry.of("foo", ZipEntry.STORED, "hello");
        Entry e2 = Entry.of("bar", ZipEntry.STORED, "world");
        test(e1, e2);
    }

    /**
     * Test updating an entry that is DEFLATED (compressed)
     */
    public void test2() throws IOException {
        Entry e1 = Entry.of("foo", ZipEntry.DEFLATED, "hello");
        Entry e2 = Entry.of("bar", ZipEntry.STORED, "world");
        test(e1, e2);
    }

    private void test(Entry e1, Entry e2) throws IOException {
        Path zipfile = Files.createTempFile(HERE, "test", "zip");

        // create zip file
        try (OutputStream out = Files.newOutputStream(zipfile);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            e1.put(zos);
            e2.put(zos);
        }

        verify(zipfile, e1, e2);

        String newContents = "hi";

        // Set the required compression method
        Map<String, Boolean> map = Map.of("noCompression",
                e1.method != ZipEntry.DEFLATED);

        // replace contents of e1
        try (FileSystem fs = FileSystems.newFileSystem(zipfile, map)) {
            Path foo = fs.getPath(e1.name);
            Files.writeString(foo, newContents);
        }

        verify(zipfile, e1.content(newContents), e2);
    }


    /**
     * Verify that the given path is a zip files containing exactly the
     * given entries.
     */
    private static void verify(Path zipfile, Entry... entries) throws IOException {
        // check entries with zip API
        try (ZipFile zf = new ZipFile(zipfile.toFile())) {
            // check entry count
            assertTrue(zf.size() == entries.length);

            // check compression method and content of each entry
            for (Entry e : entries) {
                ZipEntry ze = zf.getEntry(e.name);
                assertTrue(ze != null);
                assertTrue(ze.getMethod() == e.method);
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
            assertTrue(count == entries.length);

            // check content of each entry
            for (Entry e : entries) {
                Path file = fs.getPath(e.name);
                byte[] bytes = Files.readAllBytes(file);
                assertTrue(Arrays.equals(bytes, e.bytes));
            }
        }
    }
}
