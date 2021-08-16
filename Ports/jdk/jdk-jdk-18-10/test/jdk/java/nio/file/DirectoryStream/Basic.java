/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333
 * @summary Unit test for java.nio.file.DirectoryStream
 * @library ..
 */

import java.nio.file.*;
import static java.nio.file.Files.*;
import java.util.*;
import java.io.IOException;

public class Basic {
    static boolean found;

    static void doTest(final Path dir) throws IOException {
        DirectoryStream<Path> stream;

        // test that directory is empty
        try (DirectoryStream<Path> ds = newDirectoryStream(dir)) {
            if (ds.iterator().hasNext())
                throw new RuntimeException("directory not empty");
        }

        // create file in directory
        final Path foo = Paths.get("foo");
        createFile(dir.resolve(foo));

        // iterate over directory and check there is one entry
        stream = newDirectoryStream(dir);
        found = false;
        try {
            for (Path entry: stream) {
                if (entry.getFileName().equals(foo)) {
                    if (found)
                        throw new RuntimeException("entry already found");
                    found = true;
                } else {
                    throw new RuntimeException("entry " + entry.getFileName() +
                        " not expected");
                }
            }
        } finally {
            stream.close();
        }
        if (!found)
            throw new RuntimeException("entry not found");

        // check filtering: f* should match foo
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            private PathMatcher matcher =
                dir.getFileSystem().getPathMatcher("glob:f*");
            public boolean accept(Path file) {
                return matcher.matches(file.getFileName());
            }
        };

        found = false;
        try (DirectoryStream<Path> ds = newDirectoryStream(dir, filter)) {
            for (Path entry: ds) {
                if (entry.getFileName().equals(foo))
                   found = true;
            }
            if (!found)
                throw new RuntimeException(String.format("Error: entry: %s was not found", foo));
        }

        // check filtering: z* should not match any files
        filter = new DirectoryStream.Filter<Path>() {
            private PathMatcher matcher =
                dir.getFileSystem().getPathMatcher("glob:z*");
            public boolean accept(Path file) {
                return matcher.matches(file.getFileName());
            }
        };
        try (DirectoryStream<Path> ds = newDirectoryStream(dir, filter)) {
            if (ds.iterator().hasNext())
                throw new RuntimeException("no matching entries expected");
        }

        // check that an IOException thrown by a filter is propagated
        filter = new DirectoryStream.Filter<Path>() {
            public boolean accept(Path file) throws IOException {
                throw new java.util.zip.ZipException();
            }
        };
        stream = newDirectoryStream(dir, filter);
        try {
            stream.iterator().hasNext();
            throw new RuntimeException("DirectoryIteratorException expected");
        } catch (DirectoryIteratorException x) {
            IOException cause = x.getCause();
            if (!(cause instanceof java.util.zip.ZipException))
                throw new RuntimeException("Expected IOException not propagated");
        } finally {
            stream.close();
        }

        // check that exception or error thrown by filter is not thrown
        // by newDirectoryStream or iterator method.
        stream = newDirectoryStream(dir, new DirectoryStream.Filter<Path>() {
            public boolean accept(Path file) {
                throw new RuntimeException("Should not be visible");
            }
        });
        try {
            stream.iterator();
        } finally {
            stream.close();
        }

        // test NotDirectoryException
        try {
            newDirectoryStream(dir.resolve(foo));
            throw new RuntimeException("NotDirectoryException not thrown");
        } catch (NotDirectoryException x) {
        }

        // test UnsupportedOperationException
        stream = newDirectoryStream(dir);
        Iterator<Path> i = stream.iterator();
        i.next();
        try {
            i.remove();
            throw new RuntimeException("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException uoe) {
        }

        // test IllegalStateException
        stream = newDirectoryStream(dir);
        stream.iterator();
        try {
            // attempt to obtain second iterator
            stream.iterator();
            throw new RuntimeException("IllegalStateException not thrown as expected");
        } catch (IllegalStateException x) {
        }
        stream.close();

        stream = newDirectoryStream(dir);
        stream.close();
        try {
            // attempt to obtain iterator after stream is closed
            stream.iterator();
            throw new RuntimeException("IllegalStateException not thrown as expected");
        } catch (IllegalStateException x) {
        }

        // test that iterator reads to end of stream when closed
        stream = newDirectoryStream(dir);
        i = stream.iterator();
        stream.close();
        while (i.hasNext())
            i.next();

        stream = newDirectoryStream(dir);
        i = stream.iterator();
        stream.close();
        try {
            for (;;) i.next();
        } catch (NoSuchElementException expected) { }
    }

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            doTest(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
