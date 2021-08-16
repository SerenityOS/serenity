/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4313887 6838333 8005566 8032220 8215467 8255576
 * @summary Unit test for miscellenous methods in java.nio.file.Files
 * @library ..
 */

import java.nio.file.*;
import static java.nio.file.Files.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

public class Misc {

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            testCreateDirectories(dir);
            testIsHidden(dir);
            testIsSameFile(dir);
            testFileTypeMethods(dir);
            testAccessMethods(dir);
        } finally {
             TestUtil.removeAll(dir);
        }
    }

    /**
     * Tests createDirectories
     */
    static void testCreateDirectories(Path tmpdir) throws IOException {
        // a no-op
        createDirectories(tmpdir);

        // create one directory
        Path subdir = tmpdir.resolve("a");
        createDirectories(subdir);
        assertTrue(exists(subdir));

        // create parents
        subdir = subdir.resolve("b/c/d");
        createDirectories(subdir);
        assertTrue(exists(subdir));

        // existing file is not a directory
        Path file = createFile(tmpdir.resolve("x"));
        try {
            createDirectories(file);
            throw new RuntimeException("failure expected");
        } catch (FileAlreadyExistsException x) { }
        try {
            createDirectories(file.resolve("y"));
            throw new RuntimeException("failure expected");
        } catch (IOException x) { }

        // the root directory always exists
        Path root = Paths.get("/");
        Files.createDirectories(root);
        Files.createDirectories(root.toAbsolutePath());
    }

    /**
     * Tests isHidden
     */
    static void testIsHidden(Path tmpdir) throws IOException {
        // passing an empty path must not throw any runtime exception
        assertTrue(!isHidden(Path.of("")));

        assertTrue(!isHidden(tmpdir));

        Path file = tmpdir.resolve(".foo");
        if (System.getProperty("os.name").startsWith("Windows")) {
            createFile(file);
            try {
                setAttribute(file, "dos:hidden", true);
                try {
                    assertTrue(isHidden(file));
                } finally {
                    setAttribute(file, "dos:hidden", false);
                }
            } finally {
                delete(file);
            }
            Path dir = tmpdir.resolve("hidden");
            createDirectory(dir);
            try {
                setAttribute(dir, "dos:hidden", true);
                try {
                    assertTrue(isHidden(dir));
                } finally {
                    setAttribute(dir, "dos:hidden", false);
                }
            } finally {
                delete(dir);
            }
        } else {
            assertTrue(isHidden(file));
        }
    }

    /**
     * Tests isSameFile
     */
    static void testIsSameFile(Path tmpdir) throws IOException {
        Path thisFile = tmpdir.resolve("thisFile");
        Path thatFile = tmpdir.resolve("thatFile");

        /**
         * Test: isSameFile for self
         */
        assertTrue(isSameFile(thisFile, thisFile));

        /**
         * Test: Neither files exist
         */
        try {
            isSameFile(thisFile, thatFile);
            throw new RuntimeException("IOException not thrown");
        } catch (IOException x) {
        }
        try {
            isSameFile(thatFile, thisFile);
            throw new RuntimeException("IOException not thrown");
        } catch (IOException x) {
        }

        createFile(thisFile);
        try {
            /**
             * Test: One file exists
             */
            try {
                isSameFile(thisFile, thatFile);
                throw new RuntimeException("IOException not thrown");
            } catch (IOException x) {
            }
            try {
                isSameFile(thatFile, thisFile);
                throw new RuntimeException("IOException not thrown");
            } catch (IOException x) {
            }

            /**
             * Test: Both file exists
             */
            createFile(thatFile);
            try {
                assertTrue(!isSameFile(thisFile, thatFile));
                assertTrue(!isSameFile(thatFile, thisFile));
            } finally {
                delete(thatFile);
            }

            /**
             * Test: Symbolic links
             */
            if (TestUtil.supportsLinks(tmpdir)) {
                createSymbolicLink(thatFile, thisFile);
                try {
                    assertTrue(isSameFile(thisFile, thatFile));
                    assertTrue(isSameFile(thatFile, thisFile));
                } finally {
                    TestUtil.deleteUnchecked(thatFile);
                }
            }
        } finally {
            delete(thisFile);
        }

        // nulls
        try {
            isSameFile(thisFile, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            isSameFile(null, thatFile);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
    }

    /**
     * Exercise isRegularFile, isDirectory, isSymbolicLink
     */
    static void testFileTypeMethods(Path tmpdir) throws IOException {
        assertTrue(!isRegularFile(tmpdir));
        assertTrue(!isRegularFile(tmpdir, NOFOLLOW_LINKS));
        assertTrue(isDirectory(tmpdir));
        assertTrue(isDirectory(tmpdir, NOFOLLOW_LINKS));
        assertTrue(!isSymbolicLink(tmpdir));

        Path file = createFile(tmpdir.resolve("foo"));
        try {
            assertTrue(isRegularFile(file));
            assertTrue(isRegularFile(file, NOFOLLOW_LINKS));
            assertTrue(!isDirectory(file));
            assertTrue(!isDirectory(file, NOFOLLOW_LINKS));
            assertTrue(!isSymbolicLink(file));

            if (TestUtil.supportsLinks(tmpdir)) {
                Path link = tmpdir.resolve("link");

                createSymbolicLink(link, tmpdir);
                try {
                    assertTrue(!isRegularFile(link));
                    assertTrue(!isRegularFile(link, NOFOLLOW_LINKS));
                    assertTrue(isDirectory(link));
                    assertTrue(!isDirectory(link, NOFOLLOW_LINKS));
                    assertTrue(isSymbolicLink(link));
                } finally {
                    delete(link);
                }

                createSymbolicLink(link, file);
                try {
                    assertTrue(isRegularFile(link));
                    assertTrue(!isRegularFile(link, NOFOLLOW_LINKS));
                    assertTrue(!isDirectory(link));
                    assertTrue(!isDirectory(link, NOFOLLOW_LINKS));
                    assertTrue(isSymbolicLink(link));
                } finally {
                    delete(link);
                }

                createLink(link, file);
                try {
                    assertTrue(isRegularFile(link));
                    assertTrue(isRegularFile(link, NOFOLLOW_LINKS));
                    assertTrue(!isDirectory(link));
                    assertTrue(!isDirectory(link, NOFOLLOW_LINKS));
                    assertTrue(!isSymbolicLink(link));
                } finally {
                    delete(link);
                }
            }

        } finally {
            delete(file);
        }
    }

    /**
     * Exercise isReadbale, isWritable, isExecutable, exists, notExists
     */
    static void testAccessMethods(Path tmpdir) throws IOException {
        // should return false when file does not exist
        Path doesNotExist = tmpdir.resolve("doesNotExist");
        assertTrue(!isReadable(doesNotExist));
        assertTrue(!isWritable(doesNotExist));
        assertTrue(!isExecutable(doesNotExist));
        assertTrue(!exists(doesNotExist));
        assertTrue(notExists(doesNotExist));

        Path file = createFile(tmpdir.resolve("foo"));
        try {
            // files exist
            assertTrue(isReadable(file));
            assertTrue(isWritable(file));
            assertTrue(exists(file));
            assertTrue(!notExists(file));
            assertTrue(isReadable(tmpdir));
            assertTrue(isWritable(tmpdir));
            assertTrue(exists(tmpdir));
            assertTrue(!notExists(tmpdir));


            // sym link exists
            if (TestUtil.supportsLinks(tmpdir)) {
                Path link = tmpdir.resolve("link");

                createSymbolicLink(link, file);
                try {
                    assertTrue(isReadable(link));
                    assertTrue(isWritable(link));
                    assertTrue(exists(link));
                    assertTrue(!notExists(link));
                } finally {
                    delete(link);
                }

                createSymbolicLink(link, doesNotExist);
                try {
                    assertTrue(!isReadable(link));
                    assertTrue(!isWritable(link));
                    assertTrue(!exists(link));
                    assertTrue(exists(link, NOFOLLOW_LINKS));
                    assertTrue(notExists(link));
                    assertTrue(!notExists(link, NOFOLLOW_LINKS));
                } finally {
                    delete(link);
                }
            }

            /**
             * Test: Edit ACL to deny WRITE and EXECUTE
             */
            if (getFileStore(file).supportsFileAttributeView("acl")) {
                AclFileAttributeView view =
                    getFileAttributeView(file, AclFileAttributeView.class);
                UserPrincipal owner = view.getOwner();
                List<AclEntry> acl = view.getAcl();

                // Insert entry to deny WRITE and EXECUTE
                AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.DENY)
                    .setPrincipal(owner)
                    .setPermissions(AclEntryPermission.WRITE_DATA,
                                    AclEntryPermission.EXECUTE)
                    .build();
                acl.add(0, entry);
                view.setAcl(acl);
                try {
                    if (isRoot()) {
                        // root has all permissions
                        assertTrue(isWritable(file));
                        assertTrue(isExecutable(file));
                    } else {
                        assertTrue(!isWritable(file));
                        assertTrue(!isExecutable(file));
                    }
                } finally {
                    // Restore ACL
                    acl.remove(0);
                    view.setAcl(acl);
                }
            }

            /**
             * Test: Windows DOS read-only attribute
             */
            if (System.getProperty("os.name").startsWith("Windows")) {
                setAttribute(file, "dos:readonly", true);
                try {
                    assertTrue(!isWritable(file));
                } finally {
                    setAttribute(file, "dos:readonly", false);
                }

                // Read-only attribute does not make direcory read-only
                DosFileAttributeView view =
                    getFileAttributeView(tmpdir, DosFileAttributeView.class);
                boolean save = view.readAttributes().isReadOnly();
                view.setReadOnly(true);
                try {
                    assertTrue(isWritable(file));
                } finally {
                    view.setReadOnly(save);
                }
            }
        } finally {
            delete(file);
        }
    }

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion Failed");
    }

    private static boolean isRoot() {
        if (System.getProperty("os.name").startsWith("Windows"))
            return false;

        Path passwd = Paths.get("/etc/passwd");
        return Files.isWritable(passwd);
    }
}
