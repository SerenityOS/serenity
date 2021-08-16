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
package test;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import util.ZipFsBaseTest;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8211917
 * @summary Validate that Zip FS will always add META-INF/MANIFEST.MF to the
 * beginning of a Zip file allowing the Manifest be found and processed
 * by java.util.jar.JarInputStream.

 */
public class ManifestOrderTest extends ZipFsBaseTest {

    // Manifest PATH within a JAR
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    // Manifests used by the tests
    private static String MANIFEST_ENTRY;
    private static String MANIFEST_ENTRY2;

    // Manifest Attributes used by the tests
    private static Map<Name, String> MANIFEST_ATTRS;
    private static Map<Name, String> MANIFEST_ATTRS2;

    //  Used when the test does not expect to find a Manifest
    private static final Map<Name, String> NO_ATTRIBUTES = Map.of();

    // JAR Tool via ToolProvider API
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found")
            );

    /**
     * Create the Manifests and Map of attributes included in the Manifests
     */
    @BeforeSuite
    public void setup() {
        String jdkVendor = System.getProperty("java.vendor");
        String jdkVersion = System.getProperty("java.version");
        String attributeKey = "Player";
        String attributeValue = "Rafael Nadal";
        String attributeKey2 = "Country";
        String attributeValue2 = "Spain";
        String jdkVendorVersion = jdkVersion + " (" + jdkVendor + ")";
        MANIFEST_ENTRY = "Manifest-Version: 1.0"
                + System.lineSeparator()
                + "Created-By: " + jdkVendorVersion
                + System.lineSeparator()
                + attributeKey + ": " + attributeValue
                + System.lineSeparator();
        MANIFEST_ENTRY2 = MANIFEST_ENTRY
                + attributeKey2 + ": " + attributeValue2
                + System.lineSeparator();

        MANIFEST_ATTRS =
                Map.of(Name.MANIFEST_VERSION, "1.0",
                        new Name("Created-By"), jdkVendorVersion,
                        new Name(attributeKey), attributeValue);
        MANIFEST_ATTRS2 = new HashMap<>();
        MANIFEST_ATTRS.forEach(MANIFEST_ATTRS2::put);
        MANIFEST_ATTRS2.put(new Name(attributeKey2), attributeValue2);
    }

    /**
     * Validate that JarInputStream can find META-INF/MANIFEST.MF when its written
     * as the first entry within a JAR using Zip FS
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void testJarWithManifestAddedFirst(final Map<String, String> env,
                                              final int compression)
            throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        // Create the initial JAR writing out the Manifest first
        final Entry[] entries = newEntries(compression);
        Entry manifest = Entry.of(MANIFEST_NAME, compression, MANIFEST_ENTRY);
        zip(jarPath, env, manifest, entries[0], entries[1], entries[2]);
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Add an additional entry and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                e00);
        verify(jarPath, MANIFEST_ATTRS, compression, entries[0], entries[1],
                entries[2], e00);
    }

    /**
     * Validate that JarInputStream can find META-INF/MANIFEST.MF when its written
     * as the last entry within a JAR using Zip FS
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void testJarWithManifestAddedLast(final Map<String, String> env,
                                             final int compression) throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        // Create the initial JAR writing out the Manifest last
        final Entry[] entries = newEntries(compression);
        Entry manifest = Entry.of(MANIFEST_NAME, compression, MANIFEST_ENTRY);
        zip(jarPath, env, entries[0], entries[1], entries[2], manifest);
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Add an additional entry and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                e00);
        verify(jarPath, MANIFEST_ATTRS, compression, entries[0], entries[1],
                entries[2], e00);
    }

    /**
     * Validate that JarInputStream can find META-INF/MANIFEST.MF when its written
     * between other entries within a JAR using Zip FS
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void testJarWithManifestAddedInBetween(final Map<String, String> env,
                                                  final int compression)
            throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        // Create the initial JAR writing out the Manifest in between other entries
        final Entry[] entries = newEntries(compression);
        Entry manifest = Entry.of(MANIFEST_NAME, compression, MANIFEST_ENTRY);
        zip(jarPath, env, entries[0], entries[1], manifest, entries[2]);
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Add an additional entry and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                e00);
        verify(jarPath, MANIFEST_ATTRS, compression, entries[0], entries[1],
                entries[2], e00);
    }

    /**
     * Validate that JarInputStream can read all entries from a JAR created
     * using Zip FS without adding a Manifest
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void testJarWithNoManifest(final Map<String, String> env,
                                      final int compression) throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        // Create the initial JAR writing without a Manifest
        final Entry[] entries = newEntries(compression);
        zip(jarPath, env, entries[0], entries[1], entries[2]);
        verify(jarPath, NO_ATTRIBUTES, compression, entries);

        // Add an additional entry and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                e00);
        verify(jarPath, NO_ATTRIBUTES, compression, entries[0], entries[1],
                entries[2], e00);
    }

    /**
     * Validate that JarInputStream can read META-INF/MANIFEST.MF when the
     * the Manfiest is copied to the JAR using Files::copy
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void testManifestCopiedFromOSFile(final Map<String, String> env,
                                             final int compression) throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        final Path manifest = Paths.get(".", "test-MANIFEST.MF");
        Files.deleteIfExists(manifest);

        // Create initial JAR without a Manifest
        Files.writeString(manifest, MANIFEST_ENTRY);
        final Entry[] entries = newEntries(compression);
        zip(jarPath, env, entries[0], entries[1], entries[2]);
        verify(jarPath, NO_ATTRIBUTES, compression, entries);

        // Add the Manifest via Files::copy and verify
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(jarPath, env)) {
            Files.copy(manifest, zipfs.getPath("META-INF", "MANIFEST.MF"));
        }
        verify(jarPath, MANIFEST_ATTRS, compression, entries);
    }

    /**
     * Validate that JarInputStream can find META-INF/MANIFEST.MF when the
     * entries are copied from one JAR to another JAR using Zip FS and Files::copy
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void copyJarToJarTest(final Map<String, String> env, final int compression)
            throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        final Path jarPath2 = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        Files.deleteIfExists(jarPath2);

        // Create initial JAR with a Manifest
        final Entry[] entries = newEntries(compression);
        Entry manifest = Entry.of(MANIFEST_NAME, compression, MANIFEST_ENTRY);
        zip(jarPath, env, manifest, entries[0], entries[1], entries[2]);
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Create the another JAR via Files::copy and verify
        try (FileSystem zipfs = FileSystems.newFileSystem(jarPath, env);
             FileSystem zipfsTarget = FileSystems.newFileSystem(jarPath2,
                     Map.of("create", "true", "noCompression",
                             compression == ZipEntry.STORED))) {
            Path mPath = zipfsTarget.getPath(manifest.name);
            if (mPath.getParent() != null) {
                Files.createDirectories(mPath.getParent());
            }
            Files.copy(zipfs.getPath(manifest.name), mPath);
            for (Entry e : entries) {
                Path target = zipfsTarget.getPath(e.name);
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.copy(zipfs.getPath(e.name), target);
            }
        }
        verify(jarPath2, MANIFEST_ATTRS, compression, entries);
    }

    /**
     * Validate a JAR created using the jar tool and is updated using Zip FS
     * contains the expected entries and Manifest
     *
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "compressionMethods")
    public void testJarToolGeneratedJarWithManifest(final int compression)
            throws Exception {

        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        final Entry[] entries = newEntries(compression);
        final Path tmpdir = Paths.get("tmp");
        rmdir(tmpdir);
        Files.createDirectory(tmpdir);
        // Create a directory to hold the files to bad added to the JAR
        for (final Entry entry : entries) {
            Path p = Path.of("tmp", entry.name);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.write(Path.of("tmp", entry.name), entry.bytes);
        }
        // Create a file containing the Manifest
        final Path manifestFile = Paths.get(".", "test-jar-MANIFEST.MF");
        Files.deleteIfExists(manifestFile);
        Files.writeString(manifestFile, MANIFEST_ENTRY);

        // Create a JAR via the jar tool and verify
        final int exitCode = JAR_TOOL.run(System.out, System.err, "cvfm"
                        + (compression == ZipEntry.STORED ? "0" : ""),
                jarPath.getFileName().toString(),
                manifestFile.toAbsolutePath().toString(),
                "-C", tmpdir.toAbsolutePath().toString(), ".");
        assertEquals(exitCode, 0, "jar tool exited with failure");
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Add an additional entry and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                entries[0], entries[1], e00, entries[2]);
        verify(jarPath, MANIFEST_ATTRS, compression, entries[0], entries[1],
                e00, entries[2]);
    }

    /**
     * Validate that JarInputStream can read all entries from a JAR created
     * using Zip FS with a Manifest created by java.util.jar.Manifest
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void createWithManifestTest(final Map<String, String> env,
                                       final int compression) throws Exception {
        Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);

        // Create the JAR and verify
        Entry e00 = Entry.of("Entry-00", compression, "Indian Wells");
        try (FileSystem zipfs = FileSystems.newFileSystem(jarPath, env)) {
            // Add our Manifest using Manifest::write
            Path manifestPath = zipfs.getPath("META-INF", "MANIFEST.MF");
            if (manifestPath.getParent() != null) {
                Files.createDirectories(manifestPath.getParent());
            }
            try (final OutputStream os = Files.newOutputStream(manifestPath)) {
                final Manifest manifest = new Manifest();
                final Attributes attributes = manifest.getMainAttributes();
                // Populate the Manifest Attributes
                MANIFEST_ATTRS.forEach(attributes::put);
                manifest.write(os);
            }
            Files.write(zipfs.getPath(e00.name), e00.bytes);

        }
        verify(jarPath, MANIFEST_ATTRS, compression, e00);
    }

    /**
     * Validate that JarInputStream can find META-INF/MANIFEST.MF when it has
     * been updated by Zip FS
     *
     * @param env         Zip FS properties to use when creating the Zip File
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void updateManifestTest(final Map<String, String> env,
                                   final int compression) throws Exception {
        final Path jarPath = generatePath(HERE, "test", ".jar");
        Files.deleteIfExists(jarPath);
        // Create the initial JAR with a Manifest
        final Entry[] entries = newEntries(compression);
        Entry manifest = Entry.of(MANIFEST_NAME, compression, MANIFEST_ENTRY);
        zip(jarPath, env, manifest, entries[0], entries[1], entries[2]);
        verify(jarPath, MANIFEST_ATTRS, compression, entries);

        // Add an additional entry, update the Manifest and re-verify
        Entry e00 = Entry.of("Entry-00", compression, "Roger Federer");
        Entry revisedManifest = manifest.content(MANIFEST_ENTRY2);
        zip(jarPath, Map.of("noCompression", compression == ZipEntry.STORED),
                revisedManifest, e00);
        verify(jarPath, MANIFEST_ATTRS2, compression, entries[0], entries[1],
                entries[2], e00);
    }

    /**
     * Create an Entry array used when creating a JAR using Zip FS
     *
     * @param compressionMethod The compression method used by the test
     * @return the Entry array
     */
    private static Entry[] newEntries(final int compressionMethod) {
        return new Entry[]{new Entry("hello.txt", compressionMethod, "hello"),
                new Entry("META-INF/bar/world.txt", compressionMethod, "world"),
                new Entry("META-INF/greeting.txt", compressionMethod, "greeting")};
    }

    /**
     * Verify the entries including the Manifest in a JAR
     *
     * @param jar         Path to the JAR
     * @param attributes  A Map containing the attributes expected in the Manifest;
     *                    otherwise empty
     * @param entries     Entries to validate in the JAR
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    private static void verify(final Path jar, final Map<?, ?> attributes,
                               final int compression, Entry... entries)
            throws Exception {
        // If the compression method is not STORED, then use JarInputStream
        // to validate the entries in the JAR.  The current JAR/ZipInputStream
        // implementation supports PKZIP version 2.04g which only supports
        // bit 8 for DEFLATED entries(JDK-8143613).  Zip FS will set this bit
        // for STORED entries as is now allowed by the PKZIP spec, resulting
        // in the error "only DEFLATED entries can have EXT descriptor"
        if (ZipEntry.STORED != compression) {
            try (final JarInputStream jis =
                         new JarInputStream(Files.newInputStream(jar))) {
                // Verify the Manifest
                validateManifest(attributes, jis.getManifest());
                // Verify the rest of the expected entries are present
                final Map<String, Entry> expected = Arrays.stream(entries)
                        .collect(Collectors.toMap(entry -> entry.name, entry -> entry));
                JarEntry je = jis.getNextJarEntry();
                assertNotNull(je, "Jar is empty");
                while (je != null) {
                    if (je.isDirectory()) {
                        // skip directories
                        je = jis.getNextJarEntry();
                        continue;
                    }
                    final Entry e = expected.remove(je.getName());
                    assertNotNull(e, "Unexpected entry in jar ");
                    assertEquals(je.getMethod(), e.method, "Compression method mismatch");
                    assertEquals(jis.readAllBytes(), e.bytes);

                    je = jis.getNextJarEntry();
                }
                assertEquals(expected.size(), 0, "Missing entries in jar!");
            }
        }

        // Verify using JarFile
        try (final JarFile jf = new JarFile(jar.toFile())) {
            // Validate Manifest
            validateManifest(attributes, jf.getManifest());
            for (Entry e : entries) {
                JarEntry je = jf.getJarEntry(e.name);
                assertNotNull(je, "Entry does not exist");
                if (DEBUG) {
                    System.out.printf("Entry Name: %s, method: %s, Expected Method: %s%n",
                            e.name, je.getMethod(), e.method);
                }
                assertEquals(e.method, je.getMethod(), "Compression methods mismatch");
                try (InputStream in = jf.getInputStream(je)) {
                    byte[] bytes = in.readAllBytes();
                    if (DEBUG) {
                        System.out.printf("bytes= %s, actual=%s%n",
                                new String(bytes), new String(e.bytes));
                    }
                    assertTrue(Arrays.equals(bytes, e.bytes), "Entries do not match");
                }
            }
        }

        // Check entries with FileSystem API
        try (FileSystem fs = FileSystems.newFileSystem(jar)) {
            // Check entry count
            Path top = fs.getPath("/");
            long count = Files.find(top, Integer.MAX_VALUE,
                    (path, attrs) -> attrs.isRegularFile()).count();
            assertEquals(entries.length + (!attributes.isEmpty() ? 1 : 0), count);
            Path mf = fs.getPath("META-INF", "MANIFEST.MF");
            Manifest m = null;

            if (!attributes.isEmpty()) {
                assertTrue(Files.exists(mf));
                m = new Manifest(Files.newInputStream(mf));
            }
            validateManifest(attributes, m);

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
     * Validate whether the Manifest contains the expected attributes
     *
     * @param attributes A Map containing the attributes expected in the Manifest;
     *                   otherwise empty
     * @param m          The Manifest to validate
     */
    private static void validateManifest(Map<?, ?> attributes, Manifest m) {
        if (!attributes.isEmpty()) {
            assertNotNull(m, "Manifest is missing!");
            Attributes attrs = m.getMainAttributes();
            attributes.forEach((k, v) ->
            {
                if (DEBUG) {
                    System.out.printf("Key: %s, Value: %s%n", k, v);
                }
                assertTrue(attrs.containsKey(k));
                assertEquals(v, attrs.get(k));
            });
        } else {
            assertNull(m, "Manifest was found!");
        }
    }
}
