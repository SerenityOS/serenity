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

import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 8229888
 * @summary Updating an existing zip file does not preserve original permissions
 * @library /test/lib
 * @modules jdk.zipfs
 * @run testng/othervm ZipFSPermissionsTest
 * @run testng/othervm/java.security.policy=ZipFSPermissionsTest.policy ZipFSPermissionsTest
 */
public class ZipFSPermissionsTest {

    // Files used for testing
    private static final Path zipFile = Path.of("zipPermsTest.zip");
    private static final Path entry0 = Path.of("Entry-0.txt");
    // Path of 2nd file to add to the Zip file
    private static final Path entry1 = Path.of("Entry-1.txt");

    // Enable for permissions output
    private static final boolean DEBUG = false;

    /**
     * Create the files used by the test
     */
    @BeforeSuite
    public void setUp() throws Exception {
        boolean supportsPosix = FileSystems.getDefault()
                .supportedFileAttributeViews().contains("posix");

        // Check to see if File System supports POSIX permissions
        if (supportsPosix) {
            System.out.println("File Store Supports Posix");
        } else {
            // As there is no POSIX permission support, skip running the test
            throw new SkipException("Cannot set permissions on this File Store");
        }
        Files.writeString(entry0, "Tennis Pro");
        Files.writeString(entry1, "Tennis is a lifetime sport!");
    }

    /**
     * Re-create the initial Zip file prior to each run.
     */
    @BeforeMethod
    public void before() throws Exception {
        Files.deleteIfExists(zipFile);
        zip(zipFile, Map.of("create", "true"), entry0);
    }

    /**
     * Remove Zip file used by test after each run.
     */
    @AfterMethod
    public void tearDown() throws Exception {
        Files.deleteIfExists(zipFile);
    }

    /**
     * Remove files used by test as part of final test run clean-up
     */
    @AfterSuite
    public void suiteCleanUp() throws Exception {
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(entry0);
        Files.deleteIfExists(entry1);
    }

    /**
     * Validate that the Zip file permissions are as expected after updating the
     * Zip file
     * @param newPerms The permissions to set on the Zip File before updating the
     *                 file
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "posixPermissions")
    public void testZipPerms(Set<PosixFilePermission> newPerms) throws Exception {
        if (DEBUG) {
            System.out.printf("Test Run with perms= %s%n", newPerms);
        }

        PosixFileAttributes attrs = getPosixAttributes(zipFile);

        // Permissions used to verify the results of updating the Zip file
        if (newPerms == null) {
            // Use current Zip File permissions;
            newPerms = attrs.permissions();
        }
        displayPermissions("Original permissions", zipFile);

        // Now set the new permissions
        Files.setPosixFilePermissions(zipFile, newPerms);
        displayPermissions("Revised permissions", zipFile);

        // Update the Zip file
        zip(zipFile, Map.of(), entry1);

        // Validate that the permissions are as expected after updating the
        // Zip file
        PosixFileAttributes afterAttrs = getPosixAttributes(zipFile);
        displayPermissions("Permissions after updating the Zip File", zipFile);
        assertEquals(afterAttrs.permissions(), newPerms,
                "Permissions were not updated as expected!");
    }

    /**
     * Display the permissions for the specified Zip file when {@code DEBUG}
     * is set to {@code true}
     *
     * @param msg     String to include in the message
     * @param zipFile Path to the Zip File
     * @throws IOException If an error occurs obtaining the permissions
     */
    public void displayPermissions(String msg, Path zipFile) throws IOException {
        if (DEBUG) {
            PosixFileAttributeView view = Files.getFileAttributeView(zipFile,
                    PosixFileAttributeView.class);
            if (view == null) {
                System.out.println("Could not obtain a PosixFileAttributeView!");
                return;
            }
            PosixFileAttributes attrs = view.readAttributes();
            System.out.printf("%s: %s, Owner: %s, Group:%s, permissions: %s%n", msg,
                    zipFile.getFileName(), attrs.owner().getName(),
                    attrs.group().getName(), PosixFilePermissions.toString(attrs.permissions()));
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
    public void zip(Path zipFile, Map<String, String> env, Path source) throws IOException {
        if (DEBUG) {
            System.out.printf("File:%s, adding:%s%n", zipFile.toAbsolutePath(), source);
        }
        try (FileSystem zipfs =
                     FileSystems.newFileSystem(zipFile, env)) {
            Files.copy(source, zipfs.getPath(source.getFileName().toString()));
        }
    }

    /**
     * Returns a file's POSIX file attributes.
     *
     * @param path The path to the Zip file
     * @return The POSIX file attributes for the specified file or
     * null if the  POSIX attribute view is not available
     * @throws IOException If an error occurs obtaining the POSIX attributes for
     *                     the specified file
     */
    public PosixFileAttributes getPosixAttributes(Path path) throws IOException {
        PosixFileAttributes attrs = null;
            PosixFileAttributeView view =
                    Files.getFileAttributeView(path, PosixFileAttributeView.class);
            // Return if the attribute view is not supported
            if (view == null) {
                return null;
            }
            attrs = view.readAttributes();
        return attrs;
    }

    /*
     * DataProvider used to verify the permissions on a Zip file
     * are as expected after updating the Zip file
     */
    @DataProvider(name = "posixPermissions")
    private Object[][] posixPermissions() {
        return new Object[][]{
                {null},
                {Set.of(OWNER_READ, OWNER_WRITE, OTHERS_READ)},
                {Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)},
                {Set.of(OWNER_READ, OWNER_WRITE, OTHERS_READ, OTHERS_WRITE)},
                {Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_READ,
                        OTHERS_WRITE, OTHERS_EXECUTE)},
                {Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                        GROUP_READ, GROUP_WRITE,GROUP_EXECUTE, OTHERS_READ,
                        OTHERS_WRITE, OTHERS_EXECUTE)},
                {Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, GROUP_WRITE,
                        OTHERS_READ, OTHERS_WRITE)},
        };
    }
}
