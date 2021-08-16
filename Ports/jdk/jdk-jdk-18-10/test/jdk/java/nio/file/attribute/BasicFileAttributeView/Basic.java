/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for java.nio.file.attribute.BasicFileAttributeView
 * @library ../..
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class Basic {

    static void check(boolean okay, String msg) {
        if (!okay)
            throw new RuntimeException(msg);
    }

    static void checkAttributesOfDirectory(Path dir)
        throws IOException
    {
        BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);
        check(attrs.isDirectory(), "is a directory");
        check(!attrs.isRegularFile(), "is not a regular file");
        check(!attrs.isSymbolicLink(), "is not a link");
        check(!attrs.isOther(), "is not other");

        // last-modified-time should match java.io.File in seconds
        File f = new File(dir.toString());
        check(f.lastModified()/1000 == attrs.lastModifiedTime().to(TimeUnit.SECONDS),
              "last-modified time should be the same");
    }

    static void checkAttributesOfFile(Path dir, Path file)
        throws IOException
    {
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        check(attrs.isRegularFile(), "is a regular file");
        check(!attrs.isDirectory(), "is not a directory");
        check(!attrs.isSymbolicLink(), "is not a link");
        check(!attrs.isOther(), "is not other");

        // size and last-modified-time should match java.io.File in seconds
        File f = new File(file.toString());
        check(f.length() == attrs.size(), "size should be the same");
        check(f.lastModified()/1000 == attrs.lastModifiedTime().to(TimeUnit.SECONDS),
              "last-modified time should be the same");

        // copy last-modified time from directory to file,
        // re-read attribtues, and check they match
        BasicFileAttributeView view =
            Files.getFileAttributeView(file, BasicFileAttributeView.class);
        BasicFileAttributes dirAttrs = Files.readAttributes(dir, BasicFileAttributes.class);
        view.setTimes(dirAttrs.lastModifiedTime(), null, null);

        attrs = view.readAttributes();
        check(attrs.lastModifiedTime().equals(dirAttrs.lastModifiedTime()),
            "last-modified time should be equal");

        // security tests
        check (!(attrs instanceof PosixFileAttributes),
            "should not be able to cast to PosixFileAttributes");
    }

    static void checkAttributesOfLink(Path link)
        throws IOException
    {
        BasicFileAttributes attrs =
            Files.readAttributes(link, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        check(attrs.isSymbolicLink(), "is a link");
        check(!attrs.isDirectory(), "is a directory");
        check(!attrs.isRegularFile(), "is not a regular file");
        check(!attrs.isOther(), "is not other");
    }

    static void attributeReadWriteTests(Path dir)
        throws IOException
    {
        // create file
        Path file = dir.resolve("foo");
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write("this is not an empty file".getBytes("UTF-8"));
        }

        // check attributes of directory and file
        checkAttributesOfDirectory(dir);
        checkAttributesOfFile(dir, file);

        // symbolic links may be supported
        Path link = dir.resolve("link");
        try {
            Files.createSymbolicLink(link, file);
        } catch (UnsupportedOperationException x) {
            return;
        } catch (IOException x) {
            return;
        }
        checkAttributesOfLink(link);
    }

    public static void main(String[] args) throws IOException {
        // create temporary directory to run tests
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            attributeReadWriteTests(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
