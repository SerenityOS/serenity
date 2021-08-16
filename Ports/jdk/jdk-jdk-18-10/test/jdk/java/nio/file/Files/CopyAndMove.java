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
 * @bug 4313887 6838333 6917021 7006126 6950237 8006645 8201407
 * @summary Unit test for java.nio.file.Files copy and move methods (use -Dseed=X to set PRNG seed)
 * @library .. /test/lib
 * @build jdk.test.lib.Platform jdk.test.lib.RandomFactory
 *        CopyAndMove PassThroughFileSystem
 * @run main/othervm CopyAndMove
 * @key randomness
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Platform;
import jdk.test.lib.RandomFactory;

public class CopyAndMove {
    static final Random rand = RandomFactory.getRandom();
    static boolean heads() { return rand.nextBoolean(); }
    private static boolean testPosixAttributes = false;

    public static void main(String[] args) throws Exception {
        Path dir1 = TestUtil.createTemporaryDirectory();
        try {

            // Same directory
            FileStore fileStore1 = getFileStore(dir1);
            printDirInfo("dir1", dir1, fileStore1);
            testPosixAttributes = fileStore1.supportsFileAttributeView("posix");
            testCopyFileToFile(dir1, dir1, TestUtil.supportsLinks(dir1));
            testMove(dir1, dir1, TestUtil.supportsLinks(dir1));

            // Different directories. Use test.dir if possible as it might be
            // a different volume/file system and so improve test coverage.
            String testDir = System.getProperty("test.dir", ".");
            Path dir2 = TestUtil.createTemporaryDirectory(testDir);
            try {
                boolean testSymbolicLinks =
                    TestUtil.supportsLinks(dir1) && TestUtil.supportsLinks(dir2);
                FileStore fileStore2 = getFileStore(dir2);
                printDirInfo("dir2", dir2, fileStore2);
                testPosixAttributes = fileStore1.supportsFileAttributeView("posix") &&
                                      fileStore2.supportsFileAttributeView("posix");
                testCopyFileToFile(dir1, dir2, testSymbolicLinks);
                testMove(dir1, dir2, testSymbolicLinks);
            } finally {
                TestUtil.removeAll(dir2);
            }

            // Target is location associated with custom provider
            Path dir3 = PassThroughFileSystem.create().getPath(dir1.toString());
            FileStore fileStore3 = getFileStore(dir3);
            printDirInfo("dir3", dir3, fileStore3);
            testPosixAttributes = fileStore1.supportsFileAttributeView("posix") &&
                                  fileStore3.supportsFileAttributeView("posix");
            testCopyFileToFile(dir1, dir3, false);
            testMove(dir1, dir3, false);

            // Test copy(InputStream,Path) and copy(Path,OutputStream)
            testCopyInputStreamToFile();
            testCopyFileToOuputStream();

        } finally {
            TestUtil.removeAll(dir1);
        }
    }

    static void printDirInfo(String name, Path dir, FileStore store)
        throws IOException {
        System.err.format("%s: %s (%s)%n", name, dir, store.type());
    }

    static void checkBasicAttributes(BasicFileAttributes attrs1,
                                     BasicFileAttributes attrs2)
    {
        // check file type
        assertTrue(attrs1.isRegularFile() == attrs2.isRegularFile());
        assertTrue(attrs1.isDirectory() == attrs2.isDirectory());
        assertTrue(attrs1.isSymbolicLink() == attrs2.isSymbolicLink());
        assertTrue(attrs1.isOther() == attrs2.isOther());

        // check last modified time if not a symbolic link
        if (!attrs1.isSymbolicLink()) {
            long time1 = attrs1.lastModifiedTime().to(TimeUnit.SECONDS);
            long time2 = attrs2.lastModifiedTime().to(TimeUnit.SECONDS);

            if (time1 != time2) {
                System.err.format("File time for %s is %s\n", attrs1.fileKey(), attrs1.lastModifiedTime());
                System.err.format("File time for %s is %s\n", attrs2.fileKey(), attrs2.lastModifiedTime());
                assertTrue(false);
            }
        }

        // check size
        if (attrs1.isRegularFile())
            assertTrue(attrs1.size() == attrs2.size());
    }

    static void checkPosixAttributes(PosixFileAttributes attrs1,
                                     PosixFileAttributes attrs2)
    {
        assertTrue(attrs1.permissions().equals(attrs2.permissions()),
            "permissions%n1 (%d): %s%n2 (%d): %s%n%n",
             attrs1.permissions().size(), attrs1.permissions(),
             attrs2.permissions().size(), attrs2.permissions());
        assertTrue(attrs1.owner().equals(attrs2.owner()),
             "owner%n1: %s%n2: %s%n%n", attrs1.owner(), attrs2.owner());
        assertTrue(attrs1.group().equals(attrs2.group()),
             "group%n1: %s%n2: %s%n%n", attrs1.group(), attrs2.group());
    }

    static void checkDosAttributes(DosFileAttributes attrs1,
                                   DosFileAttributes attrs2)
    {
        assertTrue(attrs1.isReadOnly() == attrs2.isReadOnly(),
            "isReadOnly%n1: %s%n2: %s%n%n", attrs1.isReadOnly(), attrs2.isReadOnly());
        assertTrue(attrs1.isHidden() == attrs2.isHidden(),
            "isHidden%n1: %s%n2: %s%n%n", attrs1.isHidden(), attrs2.isHidden());
        assertTrue(attrs1.isSystem() == attrs2.isSystem(),
            "isSystem%n1: %s%n2: %s%n%n", attrs1.isSystem(), attrs2.isSystem());
    }

    static void checkUserDefinedFileAttributes(Map<String,ByteBuffer> attrs1,
                                     Map<String,ByteBuffer> attrs2)
    {
        assert attrs1.size() == attrs2.size();
        for (String name: attrs1.keySet()) {
            ByteBuffer bb1 = attrs1.get(name);
            ByteBuffer bb2 = attrs2.get(name);
            assertTrue(bb2 != null);
            assertTrue(bb1.equals(bb2));
        }
    }

    static Map<String,ByteBuffer> readUserDefinedFileAttributes(Path file)
        throws IOException
    {
        UserDefinedFileAttributeView view =
            getFileAttributeView(file, UserDefinedFileAttributeView.class);
        Map<String,ByteBuffer> result = new HashMap<>();
        for (String name: view.list()) {
            int size = view.size(name);
            ByteBuffer bb = ByteBuffer.allocate(size);
            int n = view.read(name, bb);
            assertTrue(n == size);
            bb.flip();
            result.put(name, bb);
        }
        return result;
    }

    // move source to target with verification
    static void moveAndVerify(Path source, Path target, CopyOption... options)
        throws IOException
    {
        // read attributes before file is moved
        BasicFileAttributes basicAttributes = null;
        PosixFileAttributes posixAttributes = null;
        DosFileAttributes dosAttributes = null;
        Map<String,ByteBuffer> namedAttributes = null;

        // get file attributes of source file
        if (Platform.isWindows()) {
            dosAttributes = readAttributes(source, DosFileAttributes.class, NOFOLLOW_LINKS);
            basicAttributes = dosAttributes;
        } else {
            posixAttributes = readAttributes(source, PosixFileAttributes.class, NOFOLLOW_LINKS);
            basicAttributes = posixAttributes;
        }
        if (basicAttributes == null)
            basicAttributes = readAttributes(source, BasicFileAttributes.class, NOFOLLOW_LINKS);

        // hash file contents if regular file
        int hash = (basicAttributes.isRegularFile()) ? computeHash(source) : 0;

        // record link target if symbolic link
        Path linkTarget = null;
        if (basicAttributes.isSymbolicLink())
            linkTarget = readSymbolicLink(source);

        // read named attributes if available (and file is not a sym link)
        if (!basicAttributes.isSymbolicLink() &&
            getFileStore(source).supportsFileAttributeView("xattr"))
        {
            namedAttributes = readUserDefinedFileAttributes(source);
        }

        // move file
        Path result = move(source, target, options);
        assertTrue(result == target);

        // verify source does not exist
        assertTrue(notExists(source));

        // verify file contents
        if (basicAttributes.isRegularFile()) {
            if (computeHash(target) != hash)
                throw new RuntimeException("Failed to verify move of regular file");
        }

        // verify link target
        if (basicAttributes.isSymbolicLink()) {
            if (!readSymbolicLink(target).equals(linkTarget))
                throw new RuntimeException("Failed to verify move of symbolic link");
        }

        // verify basic attributes
        checkBasicAttributes(basicAttributes,
            readAttributes(target, BasicFileAttributes.class, NOFOLLOW_LINKS));

        // verify other attributes when same provider
        if (source.getFileSystem().provider() == target.getFileSystem().provider()) {

            // verify POSIX attributes
            if (posixAttributes != null &&
                !basicAttributes.isSymbolicLink() &&
                testPosixAttributes)
            {
                checkPosixAttributes(posixAttributes,
                    readAttributes(target, PosixFileAttributes.class, NOFOLLOW_LINKS));
            }

            // verify DOS attributes
            if (dosAttributes != null && !basicAttributes.isSymbolicLink()) {
                DosFileAttributes attrs =
                    readAttributes(target, DosFileAttributes.class, NOFOLLOW_LINKS);
                checkDosAttributes(dosAttributes, attrs);
            }

            // verify named attributes
            if (namedAttributes != null &&
                getFileStore(target).supportsFileAttributeView("xattr"))
            {
                checkUserDefinedFileAttributes(namedAttributes,
                                               readUserDefinedFileAttributes(target));
            }
        }
    }

    /**
     * Tests all possible ways to invoke move
     */
    static void testMove(Path dir1, Path dir2, boolean supportsLinks)
        throws IOException
    {
        Path source, target, entry;

        boolean sameDevice = getFileStore(dir1).equals(getFileStore(dir2));

        // -- regular file --

        /**
         * Test: move regular file, target does not exist
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        moveAndVerify(source, target);
        delete(target);

        /**
         * Test: move regular file, target exists
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        try {
            moveAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(target);
        createDirectory(target);
        try {
            moveAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(source);
        delete(target);

        /**
         * Test: move regular file, target does not exist
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move regular file, target exists
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move regular file, target exists and is empty directory
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move regular file, target exists and is non-empty directory
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        entry = target.resolve("foo");
        createFile(entry);
        try {
            moveAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(entry);
        delete(source);
        delete(target);

        /**
         * Test atomic move of regular file (same file store)
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir1);
        moveAndVerify(source, target, ATOMIC_MOVE);
        delete(target);

        /**
         * Test atomic move of regular file (different file store)
         */
        if (!sameDevice) {
            source = createSourceFile(dir1);
            target = getTargetFile(dir2);
            try {
                moveAndVerify(source, target, ATOMIC_MOVE);
                throw new RuntimeException("AtomicMoveNotSupportedException expected");
            } catch (AtomicMoveNotSupportedException x) {
            }
            delete(source);
        }

        // -- directories --

        /*
         * Test: move empty directory, target does not exist
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        moveAndVerify(source, target);
        delete(target);

        /**
         * Test: move empty directory, target exists
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        try {
            moveAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(target);
        createDirectory(target);
        try {
            moveAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(source);
        delete(target);

        /**
         * Test: move empty directory, target does not exist
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move empty directory, target exists
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move empty, target exists and is empty directory
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        moveAndVerify(source, target, REPLACE_EXISTING);
        delete(target);

        /**
         * Test: move empty directory, target exists and is non-empty directory
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        entry = target.resolve("foo");
        createFile(entry);
        try {
            moveAndVerify(source, target, REPLACE_EXISTING);
            throw new RuntimeException("DirectoryNotEmptyException expected");
        } catch (DirectoryNotEmptyException x) {
        }
        delete(entry);
        delete(source);
        delete(target);

        /**
         * Test: move non-empty directory (same file system)
         */
        source = createSourceDirectory(dir1);
        createFile(source.resolve("foo"));
        target = getTargetFile(dir1);
        moveAndVerify(source, target);
        delete(target.resolve("foo"));
        delete(target);

        /**
         * Test: move non-empty directory (different file store)
         */
        if (!sameDevice) {
            source = createSourceDirectory(dir1);
            createFile(source.resolve("foo"));
            target = getTargetFile(dir2);
            try {
                moveAndVerify(source, target);
                throw new RuntimeException("IOException expected");
            } catch (IOException x) {
                if (!(x instanceof DirectoryNotEmptyException)) {
                    throw new RuntimeException
                        ("DirectoryNotEmptyException expected", x);
                }
            }
            delete(source.resolve("foo"));
            delete(source);
        }

        /**
         * Test atomic move of directory (same file store)
         */
        source = createSourceDirectory(dir1);
        createFile(source.resolve("foo"));
        target = getTargetFile(dir1);
        moveAndVerify(source, target, ATOMIC_MOVE);
        delete(target.resolve("foo"));
        delete(target);

        // -- symbolic links --

        /**
         * Test: Move symbolic link to file, target does not exist
         */
        if (supportsLinks) {
            Path tmp = createSourceFile(dir1);
            source = dir1.resolve("link");
            createSymbolicLink(source, tmp);
            target = getTargetFile(dir2);
            moveAndVerify(source, target);
            delete(target);
            delete(tmp);
        }

        /**
         * Test: Move symbolic link to directory, target does not exist
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir2);
            target = getTargetFile(dir2);
            moveAndVerify(source, target);
            delete(target);
        }

        /**
         * Test: Move broken symbolic link, target does not exists
         */
        if (supportsLinks) {
            Path tmp = Paths.get("doesnotexist");
            source = dir1.resolve("link");
            createSymbolicLink(source, tmp);
            target = getTargetFile(dir2);
            moveAndVerify(source, target);
            delete(target);
        }

        /**
         * Test: Move symbolic link, target exists
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir2);
            target = getTargetFile(dir2);
            createFile(target);
            try {
                moveAndVerify(source, target);
                throw new RuntimeException("FileAlreadyExistsException expected");
            } catch (FileAlreadyExistsException x) {
            }
            delete(source);
            delete(target);
        }

        /**
         * Test: Move regular file, target exists
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir2);
            target = getTargetFile(dir2);
            createFile(target);
            moveAndVerify(source, target, REPLACE_EXISTING);
            delete(target);
        }

        /**
         * Test: move symbolic link, target exists and is empty directory
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir2);
            target = getTargetFile(dir2);
            createDirectory(target);
            moveAndVerify(source, target, REPLACE_EXISTING);
            delete(target);
        }

        /**
         * Test: symbolic link, target exists and is non-empty directory
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir2);
            target = getTargetFile(dir2);
            createDirectory(target);
            entry = target.resolve("foo");
            createFile(entry);
            try {
                moveAndVerify(source, target);
                throw new RuntimeException("FileAlreadyExistsException expected");
            } catch (FileAlreadyExistsException x) {
            }
            delete(entry);
            delete(source);
            delete(target);
        }

        /**
         * Test atomic move of symbolic link (same file store)
         */
        if (supportsLinks) {
            source = dir1.resolve("link");
            createSymbolicLink(source, dir1);
            target = getTargetFile(dir2);
            createFile(target);
            moveAndVerify(source, target, REPLACE_EXISTING);
            delete(target);
        }

        // -- misc. tests --

        /**
         * Test nulls
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        try {
            move(null, target);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        try {
            move(source, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        try {
            move(source, target, (CopyOption[])null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        try {
            CopyOption[] opts = { REPLACE_EXISTING, null };
            move(source, target, opts);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        delete(source);

        /**
         * Test UOE
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        try {
            move(source, target, new CopyOption() { });
        } catch (UnsupportedOperationException x) { }
        try {
            move(source, target, REPLACE_EXISTING,  new CopyOption() { });
        } catch (UnsupportedOperationException x) { }
        delete(source);
    }

    // copy source to target with verification
    static void copyAndVerify(Path source, Path target, CopyOption... options)
        throws IOException
    {
        Path result = copy(source, target, options);
        assertTrue(result == target);

        // get attributes of source and target file to verify copy
        boolean followLinks = true;
        LinkOption[] linkOptions = new LinkOption[0];
        boolean copyAttributes = false;
        for (CopyOption opt : options) {
            if (opt == NOFOLLOW_LINKS) {
                followLinks = false;
                linkOptions = new LinkOption[] { NOFOLLOW_LINKS };
            }
            if (opt == COPY_ATTRIBUTES)
                copyAttributes = true;
        }
        BasicFileAttributes basicAttributes =
            readAttributes(source, BasicFileAttributes.class, linkOptions);

        // check hash if regular file
        if (basicAttributes.isRegularFile())
            assertTrue(computeHash(source) == computeHash(target));

        // check link target if symbolic link
        if (basicAttributes.isSymbolicLink())
            assert(readSymbolicLink(source).equals(readSymbolicLink(target)));

        // check that attributes are copied
        if (copyAttributes && followLinks) {
            checkBasicAttributes(basicAttributes,
                readAttributes(source, BasicFileAttributes.class, linkOptions));

            // verify other attributes when same provider
            if (source.getFileSystem().provider() == target.getFileSystem().provider()) {

                // check POSIX attributes are copied
                if (!Platform.isWindows() && testPosixAttributes) {
                    checkPosixAttributes(
                        readAttributes(source, PosixFileAttributes.class, linkOptions),
                        readAttributes(target, PosixFileAttributes.class, linkOptions));
                }

                // check DOS attributes are copied
                if (Platform.isWindows()) {
                    checkDosAttributes(
                        readAttributes(source, DosFileAttributes.class, linkOptions),
                        readAttributes(target, DosFileAttributes.class, linkOptions));
                }

                // check named attributes are copied
                if (followLinks &&
                    getFileStore(source).supportsFileAttributeView("xattr") &&
                    getFileStore(target).supportsFileAttributeView("xattr"))
                {
                    checkUserDefinedFileAttributes(readUserDefinedFileAttributes(source),
                                                   readUserDefinedFileAttributes(target));
                }
            }
        }
    }

    /**
     * Tests all possible ways to invoke copy to copy a file to a file
     */
    static void testCopyFileToFile(Path dir1, Path dir2, boolean supportsLinks)
        throws IOException
    {
        Path source, target, link, entry;

        // -- regular file --

        /**
         * Test: move regular file, target does not exist
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target);
        delete(source);
        delete(target);

        /**
         * Test: copy regular file, target exists
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        try {
            copyAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(target);
        createDirectory(target);
        try {
            copyAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(source);
        delete(target);

        /**
         * Test: copy regular file, target does not exist
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy regular file, target exists
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy regular file, target exists and is empty directory
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy regular file, target exists and is non-empty directory
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        entry = target.resolve("foo");
        createFile(entry);
        try {
            copyAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(entry);
        delete(source);
        delete(target);

        /**
         * Test: copy regular file + attributes
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target, COPY_ATTRIBUTES);
        delete(source);
        delete(target);


        // -- directory --

        /*
         * Test: copy directory, target does not exist
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target);
        delete(source);
        delete(target);

        /**
         * Test: copy directory, target exists
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        try {
            copyAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(target);
        createDirectory(target);
        try {
            copyAndVerify(source, target);
            throw new RuntimeException("FileAlreadyExistsException expected");
        } catch (FileAlreadyExistsException x) {
        }
        delete(source);
        delete(target);

        /**
         * Test: copy directory, target does not exist
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy directory, target exists
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createFile(target);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy directory, target exists and is empty directory
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        copyAndVerify(source, target, REPLACE_EXISTING);
        delete(source);
        delete(target);

        /**
         * Test: copy directory, target exists and is non-empty directory
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        createDirectory(target);
        entry = target.resolve("foo");
        createFile(entry);
        try {
            copyAndVerify(source, target, REPLACE_EXISTING);
            throw new RuntimeException("DirectoryNotEmptyException expected");
        } catch (DirectoryNotEmptyException x) {
        }
        delete(entry);
        delete(source);
        delete(target);

        /*
         * Test: copy directory + attributes
         */
        source = createSourceDirectory(dir1);
        target = getTargetFile(dir2);
        copyAndVerify(source, target, COPY_ATTRIBUTES);
        delete(source);
        delete(target);

        // -- symbolic links --

        /**
         * Test: Follow link
         */
        if (supportsLinks) {
            source = createSourceFile(dir1);
            link = dir1.resolve("link");
            createSymbolicLink(link, source);
            target = getTargetFile(dir2);
            copyAndVerify(link, target);
            delete(link);
            delete(source);
        }

        /**
         * Test: Copy link (to file)
         */
        if (supportsLinks) {
            source = createSourceFile(dir1);
            link = dir1.resolve("link");
            createSymbolicLink(link, source);
            target = getTargetFile(dir2);
            copyAndVerify(link, target, NOFOLLOW_LINKS);
            delete(link);
            delete(source);
        }

        /**
         * Test: Copy link (to directory)
         */
        if (supportsLinks) {
            source = dir1.resolve("mydir");
            createDirectory(source);
            link = dir1.resolve("link");
            createSymbolicLink(link, source);
            target = getTargetFile(dir2);
            copyAndVerify(link, target, NOFOLLOW_LINKS);
            delete(link);
            delete(source);
        }

        /**
         * Test: Copy broken link
         */
        if (supportsLinks) {
            assertTrue(notExists(source));
            link = dir1.resolve("link");
            createSymbolicLink(link, source);
            target = getTargetFile(dir2);
            copyAndVerify(link, target, NOFOLLOW_LINKS);
            delete(link);
        }

        /**
         * Test: Copy link to UNC (Windows only)
         */
        if (supportsLinks && Platform.isWindows()) {
            Path unc = Paths.get("\\\\rialto\\share\\file");
            link = dir1.resolve("link");
            createSymbolicLink(link, unc);
            target = getTargetFile(dir2);
            copyAndVerify(link, target, NOFOLLOW_LINKS);
            delete(link);
        }

        // -- misc. tests --

        /**
         * Test nulls
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        try {
            copy(source, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        try {
            copy(source, target, (CopyOption[])null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        try {
            CopyOption[] opts = { REPLACE_EXISTING, null };
            copy(source, target, opts);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException x) { }
        delete(source);

        /**
         * Test UOE
         */
        source = createSourceFile(dir1);
        target = getTargetFile(dir2);
        try {
            copy(source, target, new CopyOption() { });
        } catch (UnsupportedOperationException x) { }
        try {
            copy(source, target, REPLACE_EXISTING,  new CopyOption() { });
        } catch (UnsupportedOperationException x) { }
        delete(source);
    }

    /**
     * Test copy from an input stream to a file
     */
    static void testCopyInputStreamToFile() throws IOException {
        testCopyInputStreamToFile(0);
        for (int i=0; i<100; i++) {
            testCopyInputStreamToFile(rand.nextInt(32000));
        }

        // FileAlreadyExistsException
        Path target = createTempFile("blah", null);
        try {
            InputStream in = new ByteArrayInputStream(new byte[0]);
            try {
                copy(in, target);
                throw new RuntimeException("FileAlreadyExistsException expected");
            } catch (FileAlreadyExistsException ignore) { }
        } finally {
            delete(target);
        }
        Path tmpdir = createTempDirectory("blah");
        try {
            if (TestUtil.supportsLinks(tmpdir)) {
                Path link = createSymbolicLink(tmpdir.resolve("link"),
                                                  tmpdir.resolve("target"));
                try {
                    InputStream in = new ByteArrayInputStream(new byte[0]);
                    try {
                        copy(in, link);
                        throw new RuntimeException("FileAlreadyExistsException expected");
                    } catch (FileAlreadyExistsException ignore) { }
                } finally {
                    delete(link);
                }
            }
        } finally {
            delete(tmpdir);
        }


        // nulls
        try {
            copy((InputStream)null, target);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            copy(new ByteArrayInputStream(new byte[0]), (Path)null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
    }

    static void testCopyInputStreamToFile(int size) throws IOException {
        Path tmpdir = createTempDirectory("blah");
        Path source = tmpdir.resolve("source");
        Path target = tmpdir.resolve("target");
        try {
            boolean testReplaceExisting = rand.nextBoolean();

            // create source file
            byte[] b = new byte[size];
            rand.nextBytes(b);
            write(source, b);

            // target file might already exist
            if (testReplaceExisting && rand.nextBoolean()) {
                write(target, new byte[rand.nextInt(512)]);
            }

            // copy from stream to file
            InputStream in = new FileInputStream(source.toFile());
            try {
                long n;
                if (testReplaceExisting) {
                    n = copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    n = copy(in, target);
                }
                assertTrue(in.read() == -1);   // EOF
                assertTrue(n == size);
                assertTrue(size(target) == size);
            } finally {
                in.close();
            }

            // check file
            byte[] read = readAllBytes(target);
            assertTrue(Arrays.equals(read, b));

        } finally {
            deleteIfExists(source);
            deleteIfExists(target);
            delete(tmpdir);
        }
    }

    /**
     * Test copy from file to output stream
     */
    static void testCopyFileToOuputStream() throws IOException {
        testCopyFileToOuputStream(0);
        for (int i=0; i<100; i++) {
            testCopyFileToOuputStream(rand.nextInt(32000));
        }

        // nulls
        try {
            copy((Path)null, new ByteArrayOutputStream());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
        try {
            Path source = createTempFile("blah", null);
            delete(source);
            copy(source, (OutputStream)null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException ignore) { }
    }

    static void testCopyFileToOuputStream(int size) throws IOException {
        Path source = createTempFile("blah", null);
        try {
            byte[] b = new byte[size];
            rand.nextBytes(b);
            write(source, b);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            long n = copy(source, out);
            assertTrue(n == size);
            assertTrue(out.size() == size);

            byte[] read = out.toByteArray();
            assertTrue(Arrays.equals(read, b));

            // check output stream is open
            out.write(0);
            assertTrue(out.size() == size+1);
        } finally {
            delete(source);
        }
    }

    static void assertTrue(boolean value) {
        if (!value)
            throw new RuntimeException("Assertion failed");
    }

    static void assertTrue(boolean value, String format, Object... args) {
        if (!value) {
            System.err.format(format, args);
            throw new RuntimeException("Assertion failed");
        }
    }

    // computes simple hash of the given file
    static int computeHash(Path file) throws IOException {
        int h = 0;

        try (InputStream in = newInputStream(file)) {
            byte[] buf = new byte[1024];
            int n;
            do {
                n = in.read(buf);
                for (int i=0; i<n; i++) {
                    h = 31*h + (buf[i] & 0xff);
                }
            } while (n > 0);
        }
        return h;
    }

    // create file of random size in given directory
    static Path createSourceFile(Path dir) throws IOException {
        String name = "source" + Integer.toString(rand.nextInt());
        Path file = dir.resolve(name);
        createFile(file);
        byte[] bytes = new byte[rand.nextInt(128*1024)];
        rand.nextBytes(bytes);
        try (OutputStream out = newOutputStream(file)) {
            out.write(bytes);
        }
        randomizeAttributes(file);
        return file;
    }

    // create directory in the given directory
    static Path createSourceDirectory(Path dir) throws IOException {
        String name = "sourcedir" + Integer.toString(rand.nextInt());
        Path subdir = dir.resolve(name);
        createDirectory(subdir);
        randomizeAttributes(subdir);
        return subdir;
    }

    // "randomize" the file attributes of the given file.
    static void randomizeAttributes(Path file) throws IOException {
        boolean isDirectory = isDirectory(file, NOFOLLOW_LINKS);

        if (Platform.isWindows()) {
            DosFileAttributeView view =
                getFileAttributeView(file, DosFileAttributeView.class, NOFOLLOW_LINKS);
            // only set or unset the hidden attribute
            view.setHidden(heads());
        } else {
            Set<PosixFilePermission> perms =
                getPosixFilePermissions(file, NOFOLLOW_LINKS);
            PosixFilePermission[] toChange = {
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE
            };
            for (PosixFilePermission perm: toChange) {
                if (heads()) {
                    perms.add(perm);
                } else {
                    perms.remove(perm);
                }
            }
            setPosixFilePermissions(file, perms);
        }

        boolean addUserDefinedFileAttributes = heads() &&
            getFileStore(file).supportsFileAttributeView("xattr");

        // remove this when copying a direcory copies its named streams
        if (Platform.isWindows() && isDirectory)
            addUserDefinedFileAttributes = false;

        if (addUserDefinedFileAttributes) {
            UserDefinedFileAttributeView view =
                getFileAttributeView(file, UserDefinedFileAttributeView.class);
            int n = rand.nextInt(16);
            while (n > 0) {
                byte[] value = new byte[1 + rand.nextInt(100)];
                view.write("user." + Integer.toString(n), ByteBuffer.wrap(value));
                n--;
            }
        }
    }

    // create name for file in given directory
    static Path getTargetFile(Path dir) throws IOException {
        String name = "target" + Integer.toString(rand.nextInt());
        return dir.resolve(name);
    }
 }
