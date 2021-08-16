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
 * @bug 4313887 6838333 7006126 7023034
 * @summary Unit test for Files.createTempXXX
 * @library ..
 */

import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.Set;

public class TemporaryFiles {

    static void checkInDirectory(Path file, Path dir) {
        if (dir == null)
            dir = Paths.get(System.getProperty("java.io.tmpdir"));
        if (!file.getParent().equals(dir))
            throw new RuntimeException("Not in expected directory");
    }

    static void testTempFile(String prefix, String suffix, Path dir)
        throws IOException
    {
        Path file = (dir == null) ?
            Files.createTempFile(prefix, suffix) :
            Files.createTempFile(dir, prefix, suffix);
        try {
            // check file name
            String name = file.getFileName().toString();
            if (prefix != null && !name.startsWith(prefix))
                throw new RuntimeException("Should start with " + prefix);
            if (suffix == null && !name.endsWith(".tmp"))
                throw new RuntimeException("Should end with .tmp");
            if (suffix != null && !name.endsWith(suffix))
                throw new RuntimeException("Should end with " + suffix);

            // check file is in expected directory
            checkInDirectory(file, dir);

            // check that file can be opened for reading and writing
            Files.newByteChannel(file, READ).close();
            Files.newByteChannel(file, WRITE).close();
            Files.newByteChannel(file, READ,WRITE).close();

            // check file permissions are 0600 or more secure
            if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
                perms.remove(PosixFilePermission.OWNER_READ);
                perms.remove(PosixFilePermission.OWNER_WRITE);
                if (!perms.isEmpty())
                    throw new RuntimeException("Temporary file is not secure");
            }
        } finally {
            Files.delete(file);
        }
    }

    static void testTempFile(String prefix, String suffix)
        throws IOException
    {
        testTempFile(prefix, suffix, null);
    }

    static void testTempDirectory(String prefix, Path dir) throws IOException {
        Path subdir = (dir == null) ?
            Files.createTempDirectory(prefix) :
            Files.createTempDirectory(dir, prefix);
        try {
            // check file name
            String name = subdir.getFileName().toString();
            if (prefix != null && !name.startsWith(prefix))
                throw new RuntimeException("Should start with " + prefix);

            // check directory is in expected directory
            checkInDirectory(subdir, dir);

            // check directory is empty
            DirectoryStream<Path> stream = Files.newDirectoryStream(subdir);
            try {
                if (stream.iterator().hasNext())
                    throw new RuntimeException("Tempory directory not empty");
            } finally {
                stream.close();
            }

            // check that we can create file in directory
            Path file = Files.createFile(subdir.resolve("foo"));
            try {
                Files.newByteChannel(file, READ,WRITE).close();
            } finally {
                Files.delete(file);
            }

            // check file permissions are 0700 or more secure
            if (Files.getFileStore(subdir).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(subdir);
                perms.remove(PosixFilePermission.OWNER_READ);
                perms.remove(PosixFilePermission.OWNER_WRITE);
                perms.remove(PosixFilePermission.OWNER_EXECUTE);
                if (!perms.isEmpty())
                    throw new RuntimeException("Temporary directory is not secure");
            }
        } finally {
            Files.delete(subdir);
        }
    }

    static void testTempDirectory(String prefix) throws IOException {
        testTempDirectory(prefix, null);
    }

    static void testInvalidFileTemp(String prefix, String suffix) throws IOException {
        try {
            Path file = Files.createTempFile(prefix, suffix);
            Files.delete(file);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) { }
    }

    public static void main(String[] args) throws IOException {
        // temporary-file directory
        testTempFile("blah", ".dat");
        testTempFile("blah", null);
        testTempFile(null, ".dat");
        testTempFile(null, null);
        testTempDirectory("blah");
        testTempDirectory(null);

        // a given directory
        Path dir = Files.createTempDirectory("tmpdir");
        try {
            testTempFile("blah", ".dat", dir);
            testTempFile("blah", null, dir);
            testTempFile(null, ".dat", dir);
            testTempFile(null, null, dir);
            testTempDirectory("blah", dir);
            testTempDirectory(null, dir);
        } finally {
            Files.delete(dir);
        }

        // invalid prefix and suffix
        testInvalidFileTemp("../blah", null);
        testInvalidFileTemp("dir/blah", null);
        testInvalidFileTemp("blah", ".dat/foo");

        // nulls
        try {
            Files.createTempFile("blah", ".tmp", (FileAttribute<?>[])null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Files.createTempFile("blah", ".tmp", new FileAttribute<?>[] { null });
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Files.createTempDirectory("blah", (FileAttribute<?>[])null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Files.createTempDirectory("blah", new FileAttribute<?>[] { null });
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Files.createTempFile((Path)null, "blah", ".tmp");
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Files.createTempDirectory((Path)null, "blah");
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
    }
}
