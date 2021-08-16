/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333 6863864
 * @summary Unit test for java.nio.file.Files createSymbolicLink,
 *     readSymbolicLink, and createLink methods
 * @library ..
 * @build Links
 * @run main/othervm Links
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;

public class Links {

    static final boolean isWindows =
        System.getProperty("os.name").startsWith("Windows");

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion failed");
    }

    /**
     * Exercise createSymbolicLink and readLink methods
     */
    static void testSymLinks(Path dir) throws IOException {
        final Path link = dir.resolve("link");

        // Check if sym links are supported
        try {
            Files.createSymbolicLink(link, Paths.get("foo"));
            Files.delete(link);
        } catch (UnsupportedOperationException x) {
            // sym links not supported
            return;
        } catch (IOException x) {
            // probably insufficient privileges to create sym links (Windows)
            return;
        }

        // Test links to various targets
        String[] windowsTargets =
            { "foo", "C:\\foo", "\\foo", "\\\\server\\share\\foo" };
        String[] otherTargets = { "relative", "/absolute" };

        String[] targets = (isWindows) ? windowsTargets : otherTargets;
        for (String s: targets) {
            Path target = Paths.get(s);
            Files.createSymbolicLink(link, target);
            try {
                assertTrue(Files.readSymbolicLink(link).equals(target));
            } finally {
                Files.delete(link);
            }
        }

        // Test links to directory
        Path mydir = dir.resolve("mydir");
        Path myfile = mydir.resolve("myfile");
        try {
            Files.createDirectory(mydir);
            Files.createFile(myfile);

            // link -> "mydir"
            Files.createSymbolicLink(link, mydir.getFileName());
            assertTrue(Files.readSymbolicLink(link).equals(mydir.getFileName()));

            // Test access to directory via link
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(link)) {
                boolean found = false;
                for (Path entry: stream) {
                    if (entry.getFileName().equals(myfile.getFileName())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }

            // Test link2 -> link -> mydir
            final Path link2 = dir.resolve("link2");
            Path target2 = link.getFileName();
            Files.createSymbolicLink(link2, target2);
            try {
                assertTrue(Files.readSymbolicLink(link2).equals(target2));
                Files.newDirectoryStream(link2).close();
            } finally {
                Files.delete(link2);
            }

            // Remove mydir and re-create link2 before re-creating mydir
            // (This is a useful test on Windows to ensure that creating a
            // sym link to a directory sym link creates the right type of link).
            Files.delete(myfile);
            Files.delete(mydir);
            Files.createSymbolicLink(link2, target2);
            try {
                assertTrue(Files.readSymbolicLink(link2).equals(target2));
                Files.createDirectory(mydir);
                Files.newDirectoryStream(link2).close();
            } finally {
                Files.delete(link2);
            }

        } finally {
            Files.deleteIfExists(myfile);
            Files.deleteIfExists(mydir);
            Files.deleteIfExists(link);
        }
    }

    /**
     * Exercise createLink method
     */
    static void testHardLinks(Path dir) throws IOException {
        Path foo = dir.resolve("foo");
        Files.createFile(foo);
        try {
            Path bar = dir.resolve("bar");
            try {
                Files.createLink(bar, foo);
            } catch (UnsupportedOperationException x) {
                return;
            } catch (IOException x) {
                // probably insufficient privileges (Windows)
                return;
            }
            try {
                Object key1 = Files.readAttributes(foo, BasicFileAttributes.class).fileKey();
                Object key2 = Files.readAttributes(bar, BasicFileAttributes.class).fileKey();
                assertTrue((key1 == null) || (key1.equals(key2)));
            } finally {
                Files.delete(bar);
            }


        } finally {
            Files.delete(foo);
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            testSymLinks(dir);
            testHardLinks(dir);

            // repeat tests on Windows with long path
            if (isWindows) {
                Path dirWithLongPath = null;
                try {
                    dirWithLongPath = TestUtil.createDirectoryWithLongPath(dir);
                } catch (IOException x) {
                    System.out.println("Unable to create long path: " + x);
                }
                if (dirWithLongPath != null) {
                    System.out.println("");
                    System.out.println("** REPEAT TESTS WITH LONG PATH **");
                    testSymLinks(dirWithLongPath);
                    testHardLinks(dirWithLongPath);
                }
            }
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
