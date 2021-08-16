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
 * @bug 4313887 6838333 7029979
 * @summary Unit test for miscellenous java.nio.file.Path methods
 * @library ..
 */

import java.nio.file.*;
import static java.nio.file.LinkOption.*;
import java.io.*;

public class Misc {
    static final boolean isWindows =
        System.getProperty("os.name").startsWith("Windows");
    static boolean supportsLinks;

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            supportsLinks = TestUtil.supportsLinks(dir);

            // equals and hashCode methods
            testEqualsAndHashCode();

            // toFile method
            testToFile(dir);

            // toRealPath method
            testToRealPath(dir);


        } finally {
            TestUtil.removeAll(dir);
        }
    }

    /**
     * Exercise equals and hashCode methods
     */
    static void testEqualsAndHashCode() {
        Path thisFile = Paths.get("this");
        Path thatFile = Paths.get("that");

        assertTrue(thisFile.equals(thisFile));
        assertTrue(!thisFile.equals(thatFile));

        assertTrue(!thisFile.equals(null));
        assertTrue(!thisFile.equals(new Object()));

        Path likeThis = Paths.get("This");
        if (isWindows) {
            // case insensitive
            assertTrue(thisFile.equals(likeThis));
            assertTrue(thisFile.hashCode() == likeThis.hashCode());
        } else {
            // case senstive
            assertTrue(!thisFile.equals(likeThis));
        }
    }

    /**
     * Exercise toFile method
     */
    static void testToFile(Path dir) throws IOException {
        File d = dir.toFile();
        assertTrue(d.toString().equals(dir.toString()));
        assertTrue(d.toPath().equals(dir));
    }

    /**
     * Exercise toRealPath method
     */
    static void testToRealPath(Path dir) throws IOException {
        final Path file = Files.createFile(dir.resolve("foo"));
        final Path link = dir.resolve("link");

        /**
         * Test: totRealPath() will access same file as toRealPath(NOFOLLOW_LINKS)
         */
        assertTrue(Files.isSameFile(file.toRealPath(), file.toRealPath(NOFOLLOW_LINKS)));

        /**
         * Test: toRealPath should fail if file does not exist
         */
        Path doesNotExist = dir.resolve("DoesNotExist");
        try {
            doesNotExist.toRealPath();
            throw new RuntimeException("IOException expected");
        } catch (IOException expected) {
        }
        try {
            doesNotExist.toRealPath(NOFOLLOW_LINKS);
            throw new RuntimeException("IOException expected");
        } catch (IOException expected) {
        }

        /**
         * Test: toRealPath() should resolve links
         */
        if (supportsLinks) {
            Path resolvedFile = file;
            if (isWindows) {
                // Path::toRealPath does not work with environments using the
                // legacy subst mechanism. This is a workaround to keep the
                // test working if 'dir' points to a location on a subst drive.
                // See JDK-8213216.
                //
                Path tempLink = dir.resolve("tempLink");
                Files.createSymbolicLink(tempLink, dir.toAbsolutePath());
                Path resolvedDir = tempLink.toRealPath();
                Files.delete(tempLink);
                resolvedFile = resolvedDir.resolve(file.getFileName());
            }

            Files.createSymbolicLink(link, resolvedFile.toAbsolutePath());
            assertTrue(link.toRealPath().equals(resolvedFile.toRealPath()));
            Files.delete(link);
        }

        /**
         * Test: toRealPath(NOFOLLOW_LINKS) should not resolve links
         */
        if (supportsLinks) {
            Files.createSymbolicLink(link, file.toAbsolutePath());
            assertTrue(link.toRealPath(NOFOLLOW_LINKS).getFileName().equals(link.getFileName()));
            Files.delete(link);
        }

        /**
         * Test: toRealPath(NOFOLLOW_LINKS) with broken link
         */
        if (supportsLinks) {
            Path broken = Files.createSymbolicLink(link, doesNotExist);
            assertTrue(link.toRealPath(NOFOLLOW_LINKS).getFileName().equals(link.getFileName()));
            Files.delete(link);
        }

        /**
         * Test: toRealPath should eliminate "."
         */
        assertTrue(dir.resolve(".").toRealPath().equals(dir.toRealPath()));
        assertTrue(dir.resolve(".").toRealPath(NOFOLLOW_LINKS).equals(dir.toRealPath(NOFOLLOW_LINKS)));

        /**
         * Test: toRealPath should eliminate ".." when it doesn't follow a
         *       symbolic link
         */
        Path subdir = Files.createDirectory(dir.resolve("subdir"));
        assertTrue(subdir.resolve("..").toRealPath().equals(dir.toRealPath()));
        assertTrue(subdir.resolve("..").toRealPath(NOFOLLOW_LINKS).equals(dir.toRealPath(NOFOLLOW_LINKS)));
        Files.delete(subdir);

        // clean-up
        Files.delete(file);
    }

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion Failed");
    }
}
