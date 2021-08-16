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
 * @bug 4313887 6838333 7017446
 * @summary Unit test for java.nio.file.Files
 * @library ..
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Exercises getAttribute/setAttribute/readAttributes methods.
 */

public class FileAttributes {

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion Failed");
    }

    static void checkEqual(Object o1, Object o2) {
        if (o1 == null) {
            assertTrue(o2 == null);
        } else {
            assertTrue (o1.equals(o2));
        }
    }

    // Exercise getAttribute/setAttribute/readAttributes on basic attributes
    static void checkBasicAttributes(Path file, BasicFileAttributes attrs)
        throws IOException
    {
        // getAttribute
        checkEqual(attrs.size(), Files.getAttribute(file, "size"));
        checkEqual(attrs.lastModifiedTime(), Files.getAttribute(file, "basic:lastModifiedTime"));
        checkEqual(attrs.lastAccessTime(), Files.getAttribute(file, "lastAccessTime"));
        checkEqual(attrs.creationTime(), Files.getAttribute(file, "basic:creationTime"));
        assertTrue((Boolean)Files.getAttribute(file, "isRegularFile"));
        assertTrue(!(Boolean)Files.getAttribute(file, "basic:isDirectory"));
        assertTrue(!(Boolean)Files.getAttribute(file, "isSymbolicLink"));
        assertTrue(!(Boolean)Files.getAttribute(file, "basic:isOther"));
        checkEqual(attrs.fileKey(), Files.getAttribute(file, "basic:fileKey"));

        // setAttribute
        FileTime modTime = attrs.lastModifiedTime();
        Files.setAttribute(file, "basic:lastModifiedTime", FileTime.fromMillis(0L));
        checkEqual(Files.getLastModifiedTime(file),
                   FileTime.fromMillis(0L));
        Files.setAttribute(file, "lastModifiedTime", modTime);
        checkEqual(Files.getLastModifiedTime(file), modTime);

        Map<String,Object> map;
        map = Files.readAttributes(file, "*");
        assertTrue(map.size() >= 9);
        checkEqual(attrs.isRegularFile(), map.get("isRegularFile")); // check one

        map = Files.readAttributes(file, "basic:*");
        assertTrue(map.size() >= 9);
        checkEqual(attrs.lastAccessTime(), map.get("lastAccessTime")); // check one

        map = Files.readAttributes(file, "size,lastModifiedTime");
        assertTrue(map.size() == 2);
        checkEqual(attrs.size(), map.get("size"));
        checkEqual(attrs.lastModifiedTime(), map.get("lastModifiedTime"));
    }

    // Exercise getAttribute/setAttribute/readAttributes on posix attributes
    static void checkPosixAttributes(Path file, PosixFileAttributes attrs)
        throws IOException
    {
        checkBasicAttributes(file, attrs);

        // getAttribute
        checkEqual(attrs.permissions(), Files.getAttribute(file, "posix:permissions"));
        checkEqual(attrs.owner(), Files.getAttribute(file, "posix:owner"));
        checkEqual(attrs.group(), Files.getAttribute(file, "posix:group"));

        // setAttribute
        Set<PosixFilePermission> orig = attrs.permissions();
        Set<PosixFilePermission> newPerms = new HashSet<>(orig);
        newPerms.remove(PosixFilePermission.OTHERS_READ);
        newPerms.remove(PosixFilePermission.OTHERS_WRITE);
        newPerms.remove(PosixFilePermission.OTHERS_EXECUTE);
        Files.setAttribute(file, "posix:permissions", newPerms);
        checkEqual(Files.getPosixFilePermissions(file), newPerms);
        Files.setAttribute(file, "posix:permissions", orig);
        checkEqual(Files.getPosixFilePermissions(file), orig);
        Files.setAttribute(file, "posix:owner", attrs.owner());
        Files.setAttribute(file, "posix:group", attrs.group());

        // readAttributes
        Map<String,Object> map;
        map = Files.readAttributes(file, "posix:*");
        assertTrue(map.size() >= 12);
        checkEqual(attrs.permissions(), map.get("permissions")); // check one

        map = Files.readAttributes(file, "posix:size,owner");
        assertTrue(map.size() == 2);
        checkEqual(attrs.size(), map.get("size"));
        checkEqual(attrs.owner(), map.get("owner"));
    }

    // Exercise getAttribute/readAttributes on unix attributes
    static void checkUnixAttributes(Path file) throws IOException {
        // getAttribute
        int mode = (Integer)Files.getAttribute(file, "unix:mode");
        long ino = (Long)Files.getAttribute(file, "unix:ino");
        long dev = (Long)Files.getAttribute(file, "unix:dev");
        long rdev = (Long)Files.getAttribute(file, "unix:rdev");
        int nlink = (Integer)Files.getAttribute(file, "unix:nlink");
        int uid = (Integer)Files.getAttribute(file, "unix:uid");
        int gid = (Integer)Files.getAttribute(file, "unix:gid");
        FileTime ctime = (FileTime)Files.getAttribute(file, "unix:ctime");

        // readAttributes
        Map<String,Object> map;
        map = Files.readAttributes(file, "unix:*");
        assertTrue(map.size() >= 20);

        map = Files.readAttributes(file, "unix:size,uid,gid");
        assertTrue(map.size() == 3);
        checkEqual(map.get("size"),
                   Files.readAttributes(file, BasicFileAttributes.class).size());
    }

    // Exercise getAttribute/setAttribute on dos attributes
    static void checkDosAttributes(Path file, DosFileAttributes attrs)
        throws IOException
    {
        checkBasicAttributes(file, attrs);

        // getAttribute
        checkEqual(attrs.isReadOnly(), Files.getAttribute(file, "dos:readonly"));
        checkEqual(attrs.isHidden(), Files.getAttribute(file, "dos:hidden"));
        checkEqual(attrs.isSystem(), Files.getAttribute(file, "dos:system"));
        checkEqual(attrs.isArchive(), Files.getAttribute(file, "dos:archive"));

        // setAttribute
        boolean value;

        value = attrs.isReadOnly();
        Files.setAttribute(file, "dos:readonly", !value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isReadOnly(), !value);
        Files.setAttribute(file, "dos:readonly", value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isReadOnly(), value);

        value = attrs.isHidden();
        Files.setAttribute(file, "dos:hidden", !value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isHidden(), !value);
        Files.setAttribute(file, "dos:hidden", value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isHidden(), value);

        value = attrs.isSystem();
        Files.setAttribute(file, "dos:system", !value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isSystem(), !value);
        Files.setAttribute(file, "dos:system", value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isSystem(), value);

        value = attrs.isArchive();
        Files.setAttribute(file, "dos:archive", !value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isArchive(), !value);
        Files.setAttribute(file, "dos:archive", value);
        checkEqual(Files.readAttributes(file, DosFileAttributes.class).isArchive(), value);

        // readAttributes
        Map<String,Object> map;
        map = Files.readAttributes(file, "dos:*");
        assertTrue(map.size() >= 13);
        checkEqual(attrs.isReadOnly(), map.get("readonly")); // check one

        map = Files.readAttributes(file, "dos:size,hidden");
        assertTrue(map.size() == 2);
        checkEqual(attrs.size(), map.get("size"));
        checkEqual(attrs.isHidden(), map.get("hidden"));
    }

    static void checkBadSet(Path file, String attribute, Object value)
        throws IOException
    {
        try {
            Files.setAttribute(file, attribute, 0);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignore) { }
    }

    static void checkBadGet(Path file, String attribute) throws IOException {
        try {
            Files.getAttribute(file, attribute);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignore) { }
    }

    static void checkBadRead(Path file, String attribute) throws IOException {
        try {
            Files.readAttributes(file, attribute);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignore) { }
    }

    static void miscTests(Path file) throws IOException {
        // unsupported views
        try {
            Files.setAttribute(file, "foo:bar", 0);
            throw new RuntimeException("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignore) { }
        try {
            Files.getAttribute(file, "foo:bar");
            throw new RuntimeException("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignore) { }
        try {
            Files.readAttributes(file, "foo:*");
            throw new RuntimeException("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignore) { }

        // bad args
        checkBadSet(file, "", 0);
        checkBadSet(file, "basic:", 0);
        checkBadSet(file, "basic:foobar", 0);
        checkBadGet(file, "");
        checkBadGet(file, "basic:");
        checkBadGet(file, "basic:foobar");
        checkBadGet(file, "basic:size,lastModifiedTime");
        checkBadGet(file, "basic:*");
        checkBadRead(file, "");
        checkBadRead(file, "basic:");
        checkBadRead(file, "basic:foobar");
        checkBadRead(file, "basic:size,foobar");

        // nulls
        try {
            Files.getAttribute(file, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) { }
        try {
            Files.getAttribute(file, "isRegularFile", (LinkOption[])null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) { }
        try {
            Files.setAttribute(file, null, 0L);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) { }
    }

    static void doTests(Path dir) throws IOException {
        Path file = dir.resolve("foo");
        Files.createFile(file);
        FileStore store = Files.getFileStore(file);
        try {
            checkBasicAttributes(file,
                Files.readAttributes(file, BasicFileAttributes.class));

            if (store.supportsFileAttributeView("posix"))
                checkPosixAttributes(file,
                    Files.readAttributes(file, PosixFileAttributes.class));

            if (store.supportsFileAttributeView("unix"))
                checkUnixAttributes(file);

            if (store.supportsFileAttributeView("dos"))
                checkDosAttributes(file,
                    Files.readAttributes(file, DosFileAttributes.class));

            miscTests(file);
        } finally {
            Files.delete(file);
        }
    }


    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            doTests(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
