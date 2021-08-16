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
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import static org.testng.Assert.assertThrows;

/**
 * @test
 * @bug 8223771
 * @summary Validate the correct Exception is thrown if the Zip/JAR is not found
 *
 * @modules jdk.zipfs
 * @run testng/othervm NonExistentPathTests
 */
public class NonExistentPathTests {
    private static final String ZIPFS_SCHEME = "jar";
    private static final ClassLoader CLASS_LOADER = null;
    // Non-exist JAR file to test against
    private static final Path INVALID_JAR_FILE = Path.of("jarDoesNotExist.jar");
    // Standard Exception expected from FileSystems.newFileSystem
    private static Class<? extends Exception> testException = IOException.class;

    /**
     * Validate that the correct Exception is thrown  when specifying a Path
     * to a JAR that does not exist and is not being created.
     */
    @Test
    public void testNewFileSystemWithPath() {
        assertThrows(testException, () ->
                FileSystems.newFileSystem(INVALID_JAR_FILE));
        assertThrows(testException, () ->
                FileSystems.newFileSystem(INVALID_JAR_FILE, Map.of()));
        assertThrows(testException, () ->
                FileSystems.newFileSystem(INVALID_JAR_FILE, CLASS_LOADER));
        assertThrows(testException, () ->
                FileSystems.newFileSystem(INVALID_JAR_FILE, Map.of(), CLASS_LOADER));
    }

    /**
     * Validate that the correct Exception is thrown  when specifying a URI
     * to a JAR that does not exist and is not being created.
     */
    @Test
    public void testNewFileSystemWithUri() throws Exception {
        var jarURI = new URI(ZIPFS_SCHEME,
                INVALID_JAR_FILE.toUri().toString(), null);

        assertThrows(testException, () ->
                FileSystems.newFileSystem(jarURI, Map.of()));

        assertThrows(testException, () ->
                FileSystems.newFileSystem(jarURI, Map.of(), CLASS_LOADER));
    }
}
