/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6866804 7006126 8028270 8065109
 * @summary Unit test for java.nio.file.Files
 * @library ..
 * @build CheckPermissions
 * @run main/othervm -Djava.security.manager=allow CheckPermissions
 */

import java.nio.ByteBuffer;
import java.nio.file.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import java.nio.file.attribute.*;
import java.nio.channels.SeekableByteChannel;
import java.security.Permission;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Checks each method that accesses the file system does the right permission
 * check when there is a security manager set.
 */

public class CheckPermissions {

    static class Checks {
        private List<Permission> permissionsChecked = new ArrayList<>();
        private Set<String>  propertiesChecked = new HashSet<>();
        private List<String> readsChecked   = new ArrayList<>();
        private List<String> writesChecked  = new ArrayList<>();
        private List<String> deletesChecked = new ArrayList<>();
        private List<String> execsChecked   = new ArrayList<>();

        List<Permission> permissionsChecked()  { return permissionsChecked; }
        Set<String> propertiesChecked()        { return propertiesChecked; }
        List<String> readsChecked()            { return readsChecked; }
        List<String> writesChecked()           { return writesChecked; }
        List<String> deletesChecked()          { return deletesChecked; }
        List<String> execsChecked()            { return execsChecked; }
    }

    static ThreadLocal<Checks> myChecks =
        new ThreadLocal<Checks>() {
            @Override protected Checks initialValue() {
                return null;
            }
        };

    static void prepare() {
        myChecks.set(new Checks());
    }

    static void assertCheckPermission(Permission expected) {
        if (!myChecks.get().permissionsChecked().contains(expected))
          throw new RuntimeException(expected + " not checked");
    }

    static void assertCheckPropertyAccess(String key) {
        if (!myChecks.get().propertiesChecked().contains(key))
            throw new RuntimeException("Property " + key + " not checked");
    }

    static void assertChecked(Path file, List<String> list) {
        String s = file.toString();
        for (String f: list) {
            if (f.endsWith(s))
                return;
        }
        throw new RuntimeException("Access not checked");
    }

    static void assertCheckRead(Path file) {
        assertChecked(file, myChecks.get().readsChecked());
    }

    static void assertCheckWrite(Path file) {
        assertChecked(file, myChecks.get().writesChecked());
    }

    static void assertCheckWriteToDirectory(Path dir) {
        String s = dir.toString();
        List<String> list = myChecks.get().writesChecked();
        for (String f: list) {
            if (f.startsWith(s)) {
                return;
            }
        }
        throw new RuntimeException("Access not checked");
    }

    static void assertCheckDelete(Path file) {
        assertChecked(file, myChecks.get().deletesChecked());
    }

    static void assertCheckExec(Path file) {
        assertChecked(file, myChecks.get().execsChecked());
    }

    static class LoggingSecurityManager extends SecurityManager {
        static void install() {
            System.setSecurityManager(new LoggingSecurityManager());
        }

        @Override
        public void checkPermission(Permission perm) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.permissionsChecked().add(perm);
        }

        @Override
        public void checkPropertyAccess(String key) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.propertiesChecked().add(key);
        }

        @Override
        public void checkRead(String file) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.readsChecked().add(file);
        }

        @Override
        public void checkWrite(String file) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.writesChecked().add(file);
        }

        @Override
        public void checkDelete(String file) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.deletesChecked().add(file);
        }

        @Override
        public void checkExec(String file) {
            Checks checks = myChecks.get();
            if (checks != null)
                checks.execsChecked().add(file);
        }
    }

    static void testBasicFileAttributeView(BasicFileAttributeView view, Path file)
        throws IOException
    {
        prepare();
        view.readAttributes();
        assertCheckRead(file);

        prepare();
        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        view.setTimes(null, now, now);
        assertCheckWrite(file);
    }

    static void testPosixFileAttributeView(PosixFileAttributeView view, Path file)
        throws IOException
    {
        prepare();
        PosixFileAttributes attrs = view.readAttributes();
        assertCheckRead(file);
        assertCheckPermission(new RuntimePermission("accessUserInformation"));

        prepare();
        view.setPermissions(attrs.permissions());
        assertCheckWrite(file);
        assertCheckPermission(new RuntimePermission("accessUserInformation"));

        prepare();
        view.setOwner(attrs.owner());
        assertCheckWrite(file);
        assertCheckPermission(new RuntimePermission("accessUserInformation"));

        prepare();
        view.setOwner(attrs.owner());
        assertCheckWrite(file);
        assertCheckPermission(new RuntimePermission("accessUserInformation"));
    }

    public static void main(String[] args) throws IOException {
        final Path testdir = Paths.get(System.getProperty("test.dir", ".")).toAbsolutePath();
        final Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));

        Path file = createFile(testdir.resolve("file1234"));
        try {
            LoggingSecurityManager.install();

            // -- check access --

            prepare();
            exists(file);
            assertCheckRead(file);

            prepare();
            isReadable(file);
            assertCheckRead(file);

            prepare();
            isWritable(file);
            assertCheckWrite(file);

            prepare();
            isExecutable(file);
            assertCheckExec(file);

            // -- copy --

            Path target = testdir.resolve("target1234");
            prepare();
            copy(file, target);
            try {
                assertCheckRead(file);
                assertCheckWrite(target);
            } finally {
                delete(target);
            }

            if (TestUtil.supportsLinks(testdir)) {
                Path link = testdir.resolve("link1234");
                createSymbolicLink(link, file);
                try {
                    prepare();
                    copy(link, target, LinkOption.NOFOLLOW_LINKS);
                    try {
                        assertCheckRead(link);
                        assertCheckWrite(target);
                        assertCheckPermission(new LinkPermission("symbolic"));
                    } finally {
                        delete(target);
                    }

                    prepare();
                    readSymbolicLink(link);
                    assertCheckPermission(new FilePermission(link.toString(), "readlink"));
                } finally {
                    delete(link);
                }
            }

            // -- createDirectory --

            Path subdir = testdir.resolve("subdir1234");
            prepare();
            createDirectory(subdir);
            try {
                assertCheckWrite(subdir);
            } finally {
                delete(subdir);
            }

            // -- createFile --

            Path fileToCreate = testdir.resolve("file7890");
            prepare();
            createFile(fileToCreate);
            try {
                assertCheckWrite(fileToCreate);
            } finally {
                delete(fileToCreate);
            }

            // -- createSymbolicLink --

            if (TestUtil.supportsLinks(testdir)) {
                prepare();
                Path link = testdir.resolve("link1234");
                createSymbolicLink(link, file);
                try {
                    assertCheckWrite(link);
                    assertCheckPermission(new LinkPermission("symbolic"));
                } finally {
                    delete(link);
                }
            }

            // -- createLink --

            if (TestUtil.supportsLinks(testdir)) {
                prepare();
                Path link = testdir.resolve("entry234");
                createLink(link, file);
                try {
                    assertCheckWrite(link);
                    assertCheckPermission(new LinkPermission("hard"));
                } finally {
                    delete(link);
                }
            }

            // -- createTempFile --

            prepare();
            Path tmpfile1 = createTempFile("foo", null);
            try {
                assertCheckWriteToDirectory(tmpdir);
            } finally {
                delete(tmpfile1);
            }
            prepare();
            Path tmpfile2 = createTempFile(testdir, "foo", ".tmp");
            try {
                assertCheckWriteToDirectory(testdir);
            } finally {
                delete(tmpfile2);
            }

            // -- createTempDirectory --

            prepare();
            Path tmpdir1 = createTempDirectory("foo");
            try {
                assertCheckWriteToDirectory(tmpdir);
            } finally {
                delete(tmpdir1);
            }
            prepare();
            Path tmpdir2 = createTempDirectory(testdir, "foo");
            try {
                assertCheckWriteToDirectory(testdir);
            } finally {
                delete(tmpdir2);
            }

            // -- delete/deleteIfExists --

            Path fileToDelete = testdir.resolve("file7890");

            createFile(fileToDelete);
            prepare();
            delete(fileToDelete);
            assertCheckDelete(fileToDelete);

            createFile(fileToDelete);
            prepare();
            deleteIfExists(fileToDelete);   // file exists
            assertCheckDelete(fileToDelete);

            prepare();
            deleteIfExists(fileToDelete);   // file does not exist
            assertCheckDelete(fileToDelete);

            // -- exists/notExists --

            prepare();
            exists(file);
            assertCheckRead(file);

            prepare();
            notExists(file);
            assertCheckRead(file);

            // -- getFileStore --

            prepare();
            getFileStore(file);
            assertCheckRead(file);
            assertCheckPermission(new RuntimePermission("getFileStoreAttributes"));

            // -- isSameFile --

            prepare();
            isSameFile(file, testdir);
            assertCheckRead(file);
            assertCheckRead(testdir);

            // -- move --

            Path target2 = testdir.resolve("target1234");
            prepare();
            move(file, target2);
            try {
                assertCheckWrite(file);
                assertCheckWrite(target2);
            } finally {
                // restore file
                move(target2, file);
            }

            // -- newByteChannel --

            prepare();
            try (SeekableByteChannel sbc = newByteChannel(file)) {
                assertCheckRead(file);
            }
            prepare();
            try (SeekableByteChannel sbc = newByteChannel(file, WRITE)) {
                assertCheckWrite(file);
            }
            prepare();
            try (SeekableByteChannel sbc = newByteChannel(file, READ, WRITE)) {
                assertCheckRead(file);
                assertCheckWrite(file);
            }

            prepare();
            try (SeekableByteChannel sbc = newByteChannel(file, DELETE_ON_CLOSE)) {
                assertCheckRead(file);
                assertCheckDelete(file);
            }
            createFile(file); // restore file

            // -- newBufferedReader/newBufferedWriter --

            prepare();
            try (BufferedReader br = newBufferedReader(file)) {
                assertCheckRead(file);
            }

            prepare();
            try (BufferedWriter bw = newBufferedWriter(file, WRITE)) {
                assertCheckWrite(file);
            }

            prepare();
            try (BufferedWriter bw = newBufferedWriter(file, DELETE_ON_CLOSE)) {
                assertCheckWrite(file);
                assertCheckDelete(file);
            }
            createFile(file); // restore file

            prepare();
            try (BufferedWriter bw = newBufferedWriter(file,
                StandardCharsets.UTF_16, WRITE)) {
                assertCheckWrite(file);
            }

            prepare();
            try (BufferedWriter bw = newBufferedWriter(file,
                StandardCharsets.UTF_16, DELETE_ON_CLOSE)) {
                assertCheckWrite(file);
                assertCheckDelete(file);
            }
            createFile(file); // restore file

            // -- newInputStream/newOutputStream --

            prepare();
            try (InputStream in = newInputStream(file)) {
                assertCheckRead(file);
            }
            prepare();
            try (OutputStream out = newOutputStream(file)) {
                assertCheckWrite(file);
            }

            // -- write --

            prepare();
            Files.write(file, new byte[]{(byte) 42, (byte) 666}, WRITE);
            assertCheckWrite(file);

            prepare();
            Files.write(file, new byte[]{(byte) 42, (byte) 666}, WRITE,
                DELETE_ON_CLOSE);
            assertCheckWrite(file);
            assertCheckDelete(file);
            createFile(file); // restore file

            List<String> lines = Arrays.asList("42", "666");

            prepare();
            Files.write(file, lines, StandardCharsets.UTF_16, WRITE);
            assertCheckWrite(file);

            prepare();
            Files.write(file, lines, StandardCharsets.UTF_16, WRITE,
                DELETE_ON_CLOSE);
            assertCheckWrite(file);
            assertCheckDelete(file);
            createFile(file); // restore file

            prepare();
            Files.write(file, lines, WRITE);
            assertCheckWrite(file);

            prepare();
            Files.write(file, lines, WRITE, DELETE_ON_CLOSE);
            assertCheckWrite(file);
            assertCheckDelete(file);
            createFile(file); // restore file

            // -- newDirectoryStream --

            prepare();
            try (DirectoryStream<Path> stream = newDirectoryStream(testdir)) {
                assertCheckRead(testdir);

                if (stream instanceof SecureDirectoryStream<?>) {
                    Path entry;
                    SecureDirectoryStream<Path> sds =
                        (SecureDirectoryStream<Path>)stream;

                    // newByteChannel
                    entry = file.getFileName();
                    prepare();
                    try (SeekableByteChannel sbc = sds.newByteChannel(entry, EnumSet.of(READ))) {
                        assertCheckRead(file);
                    }
                    prepare();
                    try (SeekableByteChannel sbc = sds.newByteChannel(entry, EnumSet.of(WRITE))) {
                        assertCheckWrite(file);
                    }

                    // deleteFile
                    entry = file.getFileName();
                    prepare();
                    sds.deleteFile(entry);
                    assertCheckDelete(file);
                    createFile(testdir.resolve(entry));  // restore file

                    // deleteDirectory
                    entry = Paths.get("subdir1234");
                    createDirectory(testdir.resolve(entry));
                    prepare();
                    sds.deleteDirectory(entry);
                    assertCheckDelete(testdir.resolve(entry));

                    // move
                    entry = Paths.get("tempname1234");
                    prepare();
                    sds.move(file.getFileName(), sds, entry);
                    assertCheckWrite(file);
                    assertCheckWrite(testdir.resolve(entry));
                    sds.move(entry, sds, file.getFileName());  // restore file

                    // newDirectoryStream
                    entry = Paths.get("subdir1234");
                    createDirectory(testdir.resolve(entry));
                    try {
                        prepare();
                        sds.newDirectoryStream(entry).close();
                        assertCheckRead(testdir.resolve(entry));
                    } finally {
                        delete(testdir.resolve(entry));
                    }

                    // getFileAttributeView to access attributes of directory
                    testBasicFileAttributeView(sds
                        .getFileAttributeView(BasicFileAttributeView.class), testdir);
                    testPosixFileAttributeView(sds
                        .getFileAttributeView(PosixFileAttributeView.class), testdir);

                    // getFileAttributeView to access attributes of entry
                    entry = file.getFileName();
                    testBasicFileAttributeView(sds
                        .getFileAttributeView(entry, BasicFileAttributeView.class), file);
                    testPosixFileAttributeView(sds
                        .getFileAttributeView(entry, PosixFileAttributeView.class), file);

                } else {
                    System.out.println("SecureDirectoryStream not tested");
                }
            }

            // -- toAbsolutePath --

            prepare();
            file.getFileName().toAbsolutePath();
            assertCheckPropertyAccess("user.dir");

            // -- toRealPath --

            prepare();
            file.toRealPath();
            assertCheckRead(file);

            prepare();
            file.toRealPath(LinkOption.NOFOLLOW_LINKS);
            assertCheckRead(file);

            prepare();
            Paths.get(".").toRealPath();
            assertCheckPropertyAccess("user.dir");

            prepare();
            Paths.get(".").toRealPath(LinkOption.NOFOLLOW_LINKS);
            assertCheckPropertyAccess("user.dir");

            // -- register --

            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                prepare();
                testdir.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
                assertCheckRead(testdir);
            }

            // -- getAttribute/setAttribute/readAttributes --

            prepare();
            getAttribute(file, "size");
            assertCheckRead(file);

            prepare();
            setAttribute(file, "lastModifiedTime",
                FileTime.fromMillis(System.currentTimeMillis()));
            assertCheckWrite(file);

            prepare();
            readAttributes(file, "*");
            assertCheckRead(file);

            // -- BasicFileAttributeView --
            testBasicFileAttributeView(
                getFileAttributeView(file, BasicFileAttributeView.class), file);

            // -- PosixFileAttributeView --

            {
                PosixFileAttributeView view =
                    getFileAttributeView(file, PosixFileAttributeView.class);
                if (view != null &&
                    getFileStore(file).supportsFileAttributeView(PosixFileAttributeView.class))
                {
                    testPosixFileAttributeView(view, file);
                } else {
                    System.out.println("PosixFileAttributeView not tested");
                }
            }

            // -- DosFileAttributeView --

            {
                DosFileAttributeView view =
                    getFileAttributeView(file, DosFileAttributeView.class);
                if (view != null &&
                    getFileStore(file).supportsFileAttributeView(DosFileAttributeView.class))
                {
                    prepare();
                    view.readAttributes();
                    assertCheckRead(file);

                    prepare();
                    view.setArchive(false);
                    assertCheckWrite(file);

                    prepare();
                    view.setHidden(false);
                    assertCheckWrite(file);

                    prepare();
                    view.setReadOnly(false);
                    assertCheckWrite(file);

                    prepare();
                    view.setSystem(false);
                    assertCheckWrite(file);
                } else {
                    System.out.println("DosFileAttributeView not tested");
                }
            }

            // -- FileOwnerAttributeView --

            {
                FileOwnerAttributeView view =
                    getFileAttributeView(file, FileOwnerAttributeView.class);
                if (view != null &&
                    getFileStore(file).supportsFileAttributeView(FileOwnerAttributeView.class))
                {
                    prepare();
                    UserPrincipal owner = view.getOwner();
                    assertCheckRead(file);
                    assertCheckPermission(new RuntimePermission("accessUserInformation"));

                    prepare();
                    view.setOwner(owner);
                    assertCheckWrite(file);
                    assertCheckPermission(new RuntimePermission("accessUserInformation"));

                } else {
                    System.out.println("FileOwnerAttributeView not tested");
                }
            }

            // -- UserDefinedFileAttributeView --

            {
                UserDefinedFileAttributeView view =
                    getFileAttributeView(file, UserDefinedFileAttributeView.class);
                if (view != null &&
                    getFileStore(file).supportsFileAttributeView(UserDefinedFileAttributeView.class))
                {
                    prepare();
                    view.write("test", ByteBuffer.wrap(new byte[100]));
                    assertCheckWrite(file);
                    assertCheckPermission(new RuntimePermission("accessUserDefinedAttributes"));

                    prepare();
                    view.read("test", ByteBuffer.allocate(100));
                    assertCheckRead(file);
                    assertCheckPermission(new RuntimePermission("accessUserDefinedAttributes"));

                    prepare();
                    view.size("test");
                    assertCheckRead(file);
                    assertCheckPermission(new RuntimePermission("accessUserDefinedAttributes"));

                    prepare();
                    view.list();
                    assertCheckRead(file);
                    assertCheckPermission(new RuntimePermission("accessUserDefinedAttributes"));

                    prepare();
                    view.delete("test");
                    assertCheckWrite(file);
                    assertCheckPermission(new RuntimePermission("accessUserDefinedAttributes"));
                } else {
                    System.out.println("UserDefinedFileAttributeView not tested");
                }
            }

            // -- AclFileAttributeView --
            {
                AclFileAttributeView view =
                    getFileAttributeView(file, AclFileAttributeView.class);
                if (view != null &&
                    getFileStore(file).supportsFileAttributeView(AclFileAttributeView.class))
                {
                    prepare();
                    List<AclEntry> acl = view.getAcl();
                    assertCheckRead(file);
                    assertCheckPermission(new RuntimePermission("accessUserInformation"));
                    prepare();
                    view.setAcl(acl);
                    assertCheckWrite(file);
                    assertCheckPermission(new RuntimePermission("accessUserInformation"));
                } else {
                    System.out.println("AclFileAttributeView not tested");
                }
            }

            // -- UserPrincipalLookupService

            UserPrincipalLookupService lookupService =
                FileSystems.getDefault().getUserPrincipalLookupService();
            UserPrincipal owner = getOwner(file);

            prepare();
            lookupService.lookupPrincipalByName(owner.getName());
            assertCheckPermission(new RuntimePermission("lookupUserInformation"));

            try {
                UserPrincipal group = readAttributes(file, PosixFileAttributes.class).group();
                prepare();
                lookupService.lookupPrincipalByGroupName(group.getName());
                assertCheckPermission(new RuntimePermission("lookupUserInformation"));
            } catch (UnsupportedOperationException ignore) {
                System.out.println("lookupPrincipalByGroupName not tested");
            }


        } finally {
            deleteIfExists(file);
        }
    }
}
