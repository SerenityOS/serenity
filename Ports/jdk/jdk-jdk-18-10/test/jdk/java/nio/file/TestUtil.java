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

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.io.IOException;

public class TestUtil {
    private TestUtil() {
    }

    static Path createTemporaryDirectory(String where) throws IOException {
        Path dir = FileSystems.getDefault().getPath(where);
        return Files.createTempDirectory(dir, "name");
    }

    static Path createTemporaryDirectory() throws IOException {
        return Files.createTempDirectory("name");
    }

    static void removeAll(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.delete(file);
                } catch (IOException x) {
                    System.err.format("Unable to delete %s: %s\n", file, x);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                try {
                    Files.delete(dir);
                } catch (IOException x) {
                    System.err.format("Unable to delete %s: %s\n", dir, x);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.format("Unable to visit %s: %s\n", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static void deleteUnchecked(Path file) {
        try {
            Files.delete(file);
        } catch (IOException exc) {
            System.err.format("Unable to delete %s: %s\n", file, exc);
        }
    }

    /**
     * Creates a directory tree in the given directory so that the total
     * size of the path is more than 2k in size. This is used for long
     * path tests on Windows.
     */
    static Path createDirectoryWithLongPath(Path dir)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<240; i++) {
            sb.append('A');
        }
        String name = sb.toString();
        do {
            dir = dir.resolve(name).resolve(".");
            Files.createDirectory(dir);
        } while (dir.toString().length() < 2048);
        return dir;
    }

    /**
     * Returns true if symbolic links are supported
     */
    static boolean supportsLinks(Path dir) {
        Path link = dir.resolve("testlink");
        Path target = dir.resolve("testtarget");
        try {
            Files.createSymbolicLink(link, target);
            Files.delete(link);
            return true;
        } catch (UnsupportedOperationException x) {
            return false;
        } catch (IOException x) {
            return false;
        }
    }
}
