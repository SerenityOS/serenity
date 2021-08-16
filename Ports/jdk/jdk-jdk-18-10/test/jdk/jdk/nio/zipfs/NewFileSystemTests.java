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
 */

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8218875
 * @summary ZIP File System tests that leverage Files.newFileSystem
 * @modules jdk.zipfs
 * @compile NewFileSystemTests.java
 * @run testng NewFileSystemTests
 * @run testng/othervm/java.security.policy=test.policy  NewFileSystemTests
 */
public class NewFileSystemTests {

    // The Zip file system scheme
    private static final String ZIPFS_SCHEME = "jar";
    // Map to used for creating a ZIP archive
    private static final Map<String, String> ZIPFS_OPTIONS = Map.of("create", "true");
    // Primary jar file used for testing
    private static Path jarFile;
    // URI for jar file used for testing
    private static URI jarURI;

    /**
     * Create the JAR file used by the tests
     */
    @BeforeClass
    public void setUp() throws Exception {
        jarFile = Utils.createJarFile("basic.jar",
                "README");
        jarURI = new URI(ZIPFS_SCHEME, jarFile.toUri().toString(), null);

    }

    /**
     * Remove JAR file used by test as part of clean-up
     */
    @AfterClass
    public void tearDown() throws Exception {
        Files.deleteIfExists(jarFile);
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(Path, Map<String, ?>)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test
    public void testNewFileSystemPathMap() throws IOException {
        try (FileSystem zipfs = FileSystems.newFileSystem(Path.of("basic.jar"),
                ZIPFS_OPTIONS)) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(Path)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test
    public void testNewFileSystemPath() throws IOException {
        try (FileSystem zipfs = FileSystems.newFileSystem(Path.of("basic.jar"))) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(Path, ClassLoader)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test(dataProvider = "classLoaders")
    public void testNewFileSystemPathClassLoader(ClassLoader cl) throws Exception {
        try (FileSystem zipfs = FileSystems.newFileSystem(Path.of("basic.jar"),
                cl)) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(Path, Map<String, ?>, ClassLoader)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test(dataProvider = "classLoaders")
    public void testNewFileSystemPathMapClassLoader(ClassLoader cl) throws Exception {
        try (FileSystem zipfs = FileSystems.newFileSystem(Path.of("basic.jar"),
                ZIPFS_OPTIONS, cl)) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(URI, Map<String, ?>)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test
    public void testNewFileSystemUriMap() throws Exception {
        try (FileSystem zipfs = FileSystems.newFileSystem(jarURI, ZIPFS_OPTIONS)) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(URI, Map<String, ?>, ClassLoader)}
     * will return a Zip file system
     *
     * @throws IOException
     */
    @Test(dataProvider = "classLoaders")
    public void testNewFileSystemURIMapClassLoader(ClassLoader cl) throws Exception {
        try (FileSystem zipfs = FileSystems.newFileSystem(jarURI, ZIPFS_OPTIONS,
                cl)) {
            checkFileSystem(zipfs);
        }
    }

    /**
     * Validate that {@code FileSystems.newFileSystem(Path, Map<String, ?>)}
     * will throw a {@code NullPointerException} when the specified {@code Map}
     * is null
     *
     */
    @Test
    public void testNewFileSystemPathNullMap() {
        Map<String, ?> nullMap = null;
        assertThrows(NullPointerException.class, () ->
                FileSystems.newFileSystem(Path.of("basic.jar"), nullMap));
    }

    /*
     * DataProvider used to verify that a Zip file system may be returned
     * when specifying a class loader
     */
    @DataProvider(name = "classLoaders")
    private Object[][] classLoaders() {
        return new Object[][]{
                {null},
                {ClassLoader.getSystemClassLoader()}
        };
    }

    /**
     * Validate that the given FileSystem is a Zip file system.
     *
     * @param fs File System to validate
     */
    private void checkFileSystem(FileSystem fs) {

        assertNotNull(fs, "Error: FileSystem was not returned");
        assertTrue(fs.provider().getScheme().equalsIgnoreCase(ZIPFS_SCHEME));
        assertTrue(fs.isOpen());
        assertEquals(fs.getSeparator(), "/");

        // one root
        Iterator<Path> roots = fs.getRootDirectories().iterator();
        assertTrue(roots.next().toString().equals("/"));
        assertFalse(roots.hasNext());
    }
}
