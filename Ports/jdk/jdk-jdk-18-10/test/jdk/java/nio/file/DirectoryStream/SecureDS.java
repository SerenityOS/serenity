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
 * @bug 4313887 6838333
 * @summary Unit test for java.nio.file.SecureDirectoryStream
 * @library ..
 */

import java.nio.file.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.nio.channels.*;
import java.io.IOException;
import java.util.*;

public class SecureDS {
    static boolean supportsLinks;

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            DirectoryStream<Path> stream = newDirectoryStream(dir);
            stream.close();
            if (!(stream instanceof SecureDirectoryStream)) {
                if (System.getProperty("os.name").equals("Linux"))
                    throw new AssertionError(
                            "SecureDirectoryStream not supported.");
                System.out.println("SecureDirectoryStream not supported.");
                return;
            }

            supportsLinks = TestUtil.supportsLinks(dir);

            // run tests
            doBasicTests(dir);
            doMoveTests(dir);
            miscTests(dir);

        } finally {
            TestUtil.removeAll(dir);
        }
    }

    // Exercise each of SecureDirectoryStream's method (except move)
    static void doBasicTests(Path dir) throws IOException {
        Path dir1 = createDirectory(dir.resolve("dir1"));
        Path dir2 = dir.resolve("dir2");

        // create a file, directory, and two sym links in the directory
        Path fileEntry = Paths.get("myfile");
        createFile(dir1.resolve(fileEntry));
        Path dirEntry = Paths.get("mydir");
        createDirectory(dir1.resolve(dirEntry));
        // myfilelink -> myfile
        Path link1Entry = Paths.get("myfilelink");
        if (supportsLinks)
            createSymbolicLink(dir1.resolve(link1Entry), fileEntry);
        // mydirlink -> mydir
        Path link2Entry = Paths.get("mydirlink");
        if (supportsLinks)
            createSymbolicLink(dir1.resolve(link2Entry), dirEntry);

        // open directory and then move it so that it is no longer accessible
        // via its original path.
        SecureDirectoryStream<Path> stream =
            (SecureDirectoryStream<Path>)newDirectoryStream(dir1);
        move(dir1, dir2);

        // Test: iterate over all entries
        int count = 0;
        for (Path entry: stream) { count++; }
        assertTrue(count == (supportsLinks ? 4 : 2));

        // Test: getFileAttributeView to access directory's attributes
        assertTrue(stream
            .getFileAttributeView(BasicFileAttributeView.class)
                .readAttributes()
                    .isDirectory());

        // Test: getFileAttributeView to access attributes of entries
        assertTrue(stream
            .getFileAttributeView(fileEntry, BasicFileAttributeView.class)
                .readAttributes()
                    .isRegularFile());
        assertTrue(stream
            .getFileAttributeView(fileEntry, BasicFileAttributeView.class, NOFOLLOW_LINKS)
                .readAttributes()
                    .isRegularFile());
        assertTrue(stream
            .getFileAttributeView(dirEntry, BasicFileAttributeView.class)
                .readAttributes()
                    .isDirectory());
        assertTrue(stream
            .getFileAttributeView(dirEntry, BasicFileAttributeView.class, NOFOLLOW_LINKS)
                .readAttributes()
                    .isDirectory());
        if (supportsLinks) {
            assertTrue(stream
                .getFileAttributeView(link1Entry, BasicFileAttributeView.class)
                    .readAttributes()
                        .isRegularFile());
            assertTrue(stream
                .getFileAttributeView(link1Entry, BasicFileAttributeView.class, NOFOLLOW_LINKS)
                    .readAttributes()
                        .isSymbolicLink());
            assertTrue(stream
                .getFileAttributeView(link2Entry, BasicFileAttributeView.class)
                    .readAttributes()
                        .isDirectory());
            assertTrue(stream
                .getFileAttributeView(link2Entry, BasicFileAttributeView.class, NOFOLLOW_LINKS)
                    .readAttributes()
                        .isSymbolicLink());
        }

        // Test: newByteChannel
        Set<StandardOpenOption> opts = Collections.emptySet();
        stream.newByteChannel(fileEntry, opts).close();
        if (supportsLinks) {
            stream.newByteChannel(link1Entry, opts).close();
            try {
                Set<OpenOption> mixed = new HashSet<>();
                mixed.add(READ);
                mixed.add(NOFOLLOW_LINKS);
                stream.newByteChannel(link1Entry, mixed).close();
                shouldNotGetHere();
            } catch (IOException x) { }
        }

        // Test: newDirectoryStream
        stream.newDirectoryStream(dirEntry).close();
        stream.newDirectoryStream(dirEntry, LinkOption.NOFOLLOW_LINKS).close();
        if (supportsLinks) {
            stream.newDirectoryStream(link2Entry).close();
            try {
                stream.newDirectoryStream(link2Entry, LinkOption.NOFOLLOW_LINKS)
                    .close();
                shouldNotGetHere();
            } catch (IOException x) { }
        }

        // Test: delete
        if (supportsLinks) {
            stream.deleteFile(link1Entry);
            stream.deleteFile(link2Entry);
        }
        stream.deleteDirectory(dirEntry);
        stream.deleteFile(fileEntry);

        // clean-up
        stream.close();
        delete(dir2);
    }

    // Exercise SecureDirectoryStream's move method
    static void doMoveTests(Path dir) throws IOException {
        Path dir1 = createDirectory(dir.resolve("dir1"));
        Path dir2 = createDirectory(dir.resolve("dir2"));

        // create dir1/myfile, dir1/mydir, dir1/mylink
        Path fileEntry = Paths.get("myfile");
        createFile(dir1.resolve(fileEntry));
        Path dirEntry = Paths.get("mydir");
        createDirectory(dir1.resolve(dirEntry));
        Path linkEntry = Paths.get("mylink");
        if (supportsLinks)
            createSymbolicLink(dir1.resolve(linkEntry), Paths.get("missing"));

        // target name
        Path target = Paths.get("newfile");

        // open stream to both directories
        SecureDirectoryStream<Path> stream1 =
            (SecureDirectoryStream<Path>)newDirectoryStream(dir1);
        SecureDirectoryStream<Path> stream2 =
            (SecureDirectoryStream<Path>)newDirectoryStream(dir2);

        // Test: move dir1/myfile -> dir2/newfile
        stream1.move(fileEntry, stream2, target);
        assertTrue(notExists(dir1.resolve(fileEntry)));
        assertTrue(exists(dir2.resolve(target)));
        stream2.deleteFile(target);

        // Test: move dir1/mydir -> dir2/newfile
        stream1.move(dirEntry, stream2, target);
        assertTrue(notExists(dir1.resolve(dirEntry)));
        assertTrue(exists(dir2.resolve(target)));
        stream2.deleteDirectory(target);

        // Test: move dir1/mylink -> dir2/newfile
        if (supportsLinks) {
            stream1.move(linkEntry, stream2, target);
            assertTrue(isSymbolicLink(dir2.resolve(target)));
            stream2.deleteFile(target);
        }

        // Test: move between devices
        String testDirAsString = System.getProperty("test.dir");
        if (testDirAsString != null) {
            Path testDir = Paths.get(testDirAsString);
            if (!getFileStore(dir1).equals(getFileStore(testDir))) {
                SecureDirectoryStream<Path> ts =
                    (SecureDirectoryStream<Path>)newDirectoryStream(testDir);
                createFile(dir1.resolve(fileEntry));
                try {
                    stream1.move(fileEntry, ts, target);
                    shouldNotGetHere();
                } catch (AtomicMoveNotSupportedException x) { }
                ts.close();
                stream1.deleteFile(fileEntry);
            }
        }

        // clean-up
        delete(dir1);
        delete(dir2);
    }

    // null and ClosedDirectoryStreamException
    static void miscTests(Path dir) throws IOException {
        Path file = Paths.get("file");
        createFile(dir.resolve(file));

        SecureDirectoryStream<Path> stream =
            (SecureDirectoryStream<Path>)newDirectoryStream(dir);

        // NullPointerException
        try {
            stream.getFileAttributeView(null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.getFileAttributeView(null, BasicFileAttributeView.class);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.getFileAttributeView(file, null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.newByteChannel(null, EnumSet.of(CREATE,WRITE));
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.newByteChannel(null, EnumSet.of(CREATE,WRITE,null));
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.newByteChannel(file, null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.move(null, stream, file);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.move(file, null, file);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.move(file, stream, null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.newDirectoryStream(null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.deleteFile(null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }
        try {
            stream.deleteDirectory(null);
            shouldNotGetHere();
        } catch (NullPointerException x) { }

        // close stream
        stream.close();
        stream.close();     // should be no-op

        // ClosedDirectoryStreamException
        try {
            stream.newDirectoryStream(file);
            shouldNotGetHere();
        } catch (ClosedDirectoryStreamException x) { }
        try {
            stream.newByteChannel(file, EnumSet.of(READ));
            shouldNotGetHere();
        } catch (ClosedDirectoryStreamException x) { }
        try {
            stream.move(file, stream, file);
            shouldNotGetHere();
        } catch (ClosedDirectoryStreamException x) { }
        try {
            stream.deleteFile(file);
            shouldNotGetHere();
        } catch (ClosedDirectoryStreamException x) { }

        // clean-up
        delete(dir.resolve(file));
    }

    static void assertTrue(boolean b) {
        if (!b) throw new RuntimeException("Assertion failed");
    }

    static void shouldNotGetHere() {
        assertTrue(false);
    }
}
