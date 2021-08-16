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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

/**
 * @test
 * @bug 8210469
 * @summary Verify ZIP FileSystem works with a Security Manager
 * @modules jdk.zipfs
 * @compile PropertyPermissionTests.java
 * @run testng/othervm/java.security.policy=PropertyPermissions.policy  PropertyPermissionTests
 */
public class PropertyPermissionTests {

    // Map to used for creating a ZIP archive
    private static final Map<String, String> ZIPFS_OPTIONS = Map.of("create", "true");

    // The ZIP file system provider
    private static final FileSystemProvider ZIPFS_PROVIDER = getZipFSProvider();

    // Primary jar file used for testing
    private static Path jarFile;

    /**
     * Create the JAR files used by the tests
     */
    @BeforeClass
    public void setUp()  throws Exception {
        jarFile = Utils.createJarFile("basic.jar",
                "META-INF/services/java.nio.file.spi.FileSystemProvider");
    }

    /**
     * Remove JAR files used by test as part of clean-up
     */
    @AfterClass
    public void tearDown() throws Exception {
        Files.deleteIfExists(jarFile);
    }

    /**
     * Validate that the ZIP File System can be successfully closed when a Security Manager
     * has been enabled.
     */
    @Test
    public void test0000() throws IOException {
        FileSystem zipfs = ZIPFS_PROVIDER.newFileSystem(
                Paths.get("basic.jar"), ZIPFS_OPTIONS);
        zipfs.close();
    }

    /**
     * Returns the Zip FileSystem Provider
     */
    private static FileSystemProvider getZipFSProvider() {
        for (FileSystemProvider fsProvider : FileSystemProvider.installedProviders()) {
            if ("jar".equals(fsProvider.getScheme())) {
                return fsProvider;
            }
        }
        return null;
    }

}
