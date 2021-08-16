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
 * @summary Unit test for java.nio.file.attribute.PosixFileAttributeView
 * @library ../..
 */

import java.nio.file.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

/**
 * Unit test for PosixFileAttributeView, passing silently if this attribute
 * view is not available.
 */

public class Basic {

    /**
     * Use view to update permission to the given mode and check that the
     * permissions have been updated.
     */
    static void testPermissions(Path file, String mode) throws IOException {
        System.out.format("change mode: %s\n", mode);
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(mode);

        // change permissions and re-read them.
        Files.setPosixFilePermissions(file, perms);
        Set<PosixFilePermission> current = Files.getPosixFilePermissions(file);
        if (!current.equals(perms)) {
            throw new RuntimeException("Actual permissions: " +
                PosixFilePermissions.toString(current) + ", expected: " +
                PosixFilePermissions.toString(perms));
        }

        // repeat test using setAttribute/getAttribute
        Files.setAttribute(file, "posix:permissions", perms);
        current = (Set<PosixFilePermission>)Files.getAttribute(file, "posix:permissions");
        if (!current.equals(perms)) {
            throw new RuntimeException("Actual permissions: " +
                PosixFilePermissions.toString(current) + ", expected: " +
                PosixFilePermissions.toString(perms));
        }
    }

    /**
     * Check that the actual permissions of a file match or make it more
     * secure than requested
     */
    static void checkSecure(Set<PosixFilePermission> requested,
                            Set<PosixFilePermission> actual)
    {
        for (PosixFilePermission perm: actual) {
            if (!requested.contains(perm)) {
                throw new RuntimeException("Actual permissions: " +
                    PosixFilePermissions.toString(actual) + ", requested: " +
                    PosixFilePermissions.toString(requested) +
                    " - file is less secure than requested");
            }
        }
    }

    /**
     * Create file with given mode and check that the file is created with a
     * mode that is not less secure
     */
    static void createWithPermissions(Path file,
                                      String mode)
        throws IOException
    {
        Set<PosixFilePermission> requested = PosixFilePermissions.fromString(mode);
        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(requested);
        System.out.format("create file with mode: %s\n", mode);
        Files.createFile(file, attr);
        try {
            checkSecure(requested,
                Files.getFileAttributeView(file, PosixFileAttributeView.class)
                     .readAttributes()
                     .permissions());
        } finally {
            Files.delete(file);
        }

        System.out.format("create directory with mode: %s\n", mode);
        Files.createDirectory(file, attr);
        try {
            checkSecure(requested,
                Files.getFileAttributeView(file, PosixFileAttributeView.class)
                     .readAttributes()
                     .permissions());
        } finally {
            Files.delete(file);
        }
    }

    /**
     * Test the setPermissions/permissions methods.
     */
    static void permissionTests(Path dir)
        throws IOException
    {
        System.out.println("-- Permission Tests  --");

        // create file and test updating and reading its permissions
        Path file = dir.resolve("foo");
        System.out.format("create %s\n", file);
        Files.createFile(file);
        try {
            // get initial permissions so that we can restore them later
            PosixFileAttributeView view =
                Files.getFileAttributeView(file, PosixFileAttributeView.class);
            Set<PosixFilePermission> save = view.readAttributes()
                .permissions();

            // test various modes
            try {
                testPermissions(file, "---------");
                testPermissions(file, "r--------");
                testPermissions(file, "-w-------");
                testPermissions(file, "--x------");
                testPermissions(file, "rwx------");
                testPermissions(file, "---r-----");
                testPermissions(file, "----w----");
                testPermissions(file, "-----x---");
                testPermissions(file, "---rwx---");
                testPermissions(file, "------r--");
                testPermissions(file, "-------w-");
                testPermissions(file, "--------x");
                testPermissions(file, "------rwx");
                testPermissions(file, "r--r-----");
                testPermissions(file, "r--r--r--");
                testPermissions(file, "rw-rw----");
                testPermissions(file, "rwxrwx---");
                testPermissions(file, "rw-rw-r--");
                testPermissions(file, "r-xr-x---");
                testPermissions(file, "r-xr-xr-x");
                testPermissions(file, "rwxrwxrwx");
            } finally {
                view.setPermissions(save);
            }
        } finally {
            Files.delete(file);
        }

        // create link (to file that doesn't exist) and test reading of
        // permissions
        if (TestUtil.supportsLinks(dir)) {
            Path link = dir.resolve("link");
            System.out.format("create link %s\n", link);
            Files.createSymbolicLink(link, file);
            try {
                PosixFileAttributes attrs =
                    Files.getFileAttributeView(link,
                                               PosixFileAttributeView.class,
                                               NOFOLLOW_LINKS)
                         .readAttributes();
                if (!attrs.isSymbolicLink()) {
                    throw new RuntimeException("not a link");
                }
            } finally {
                Files.delete(link);
            }
        }

        System.out.println("OKAY");
    }

    /**
     * Test creating a file and directory with initial permissios
     */
    static void createTests(Path dir)
        throws IOException
    {
        System.out.println("-- Create Tests  --");

        Path file = dir.resolve("foo");

        createWithPermissions(file, "---------");
        createWithPermissions(file, "r--------");
        createWithPermissions(file, "-w-------");
        createWithPermissions(file, "--x------");
        createWithPermissions(file, "rwx------");
        createWithPermissions(file, "---r-----");
        createWithPermissions(file, "----w----");
        createWithPermissions(file, "-----x---");
        createWithPermissions(file, "---rwx---");
        createWithPermissions(file, "------r--");
        createWithPermissions(file, "-------w-");
        createWithPermissions(file, "--------x");
        createWithPermissions(file, "------rwx");
        createWithPermissions(file, "r--r-----");
        createWithPermissions(file, "r--r--r--");
        createWithPermissions(file, "rw-rw----");
        createWithPermissions(file, "rwxrwx---");
        createWithPermissions(file, "rw-rw-r--");
        createWithPermissions(file, "r-xr-x---");
        createWithPermissions(file, "r-xr-xr-x");
        createWithPermissions(file, "rwxrwxrwx");

        System.out.println("OKAY");
    }

    /**
     * Test setOwner/setGroup methods - this test simply exercises the
     * methods to avoid configuration.
     */
    static void ownerTests(Path dir)
        throws IOException
    {
        System.out.println("-- Owner Tests  --");

        Path file = dir.resolve("gus");
        System.out.format("create %s\n", file);

        Files.createFile(file);
        try {

            // read attributes of directory to get owner/group
            PosixFileAttributeView view =
                Files.getFileAttributeView(file, PosixFileAttributeView.class);
            PosixFileAttributes attrs = view.readAttributes();

            // set to existing owner/group
            view.setOwner(attrs.owner());
            view.setGroup(attrs.group());

            // repeat test using set/getAttribute
            UserPrincipal owner = (UserPrincipal)Files.getAttribute(file, "posix:owner");
            Files.setAttribute(file, "posix:owner", owner);
            UserPrincipal group = (UserPrincipal)Files.getAttribute(file, "posix:group");
            Files.setAttribute(file, "posix:group", group);

        } finally {
            Files.delete(file);
        }

        System.out.println("OKAY");
    }

    /**
     * Test the lookupPrincipalByName/lookupPrincipalByGroupName methods
     */
    static void lookupPrincipalTests(Path dir)
        throws IOException
    {
        System.out.println("-- Lookup UserPrincipal Tests --");

        UserPrincipalLookupService lookupService = dir.getFileSystem()
            .getUserPrincipalLookupService();

        // read attributes of directory to get owner/group
        PosixFileAttributes attrs = Files.readAttributes(dir, PosixFileAttributes.class);

        // lookup owner and check it matches file's owner
        System.out.format("lookup: %s\n", attrs.owner().getName());
        try {
            UserPrincipal owner = lookupService.lookupPrincipalByName(attrs.owner().getName());
            if (owner instanceof GroupPrincipal)
                throw new RuntimeException("owner is a group?");
            if (!owner.equals(attrs.owner()))
                throw new RuntimeException("owner different from file owner");
        } catch (UserPrincipalNotFoundException x) {
            System.out.println("user not found - test skipped");
        }

        // lookup group and check it matches file's group-owner
        System.out.format("lookup group: %s\n", attrs.group().getName());
        try {
            GroupPrincipal group = lookupService.lookupPrincipalByGroupName(attrs.group().getName());
            if (!group.equals(attrs.group()))
                throw new RuntimeException("group different from file group-owner");
        } catch (UserPrincipalNotFoundException x) {
            System.out.println("group not found - test skipped");
        }

        // test that UserPrincipalNotFoundException is thrown
        String invalidPrincipal = "scumbag99";
        try {
            System.out.format("lookup: %s\n", invalidPrincipal);
            lookupService.lookupPrincipalByName(invalidPrincipal);
            throw new RuntimeException("'" + invalidPrincipal + "' is a valid user?");
        } catch (UserPrincipalNotFoundException x) {
        }
        try {
            System.out.format("lookup group: %s\n", invalidPrincipal);
            lookupService.lookupPrincipalByGroupName("idonotexist");
            throw new RuntimeException("'" + invalidPrincipal + "' is a valid group?");
        } catch (UserPrincipalNotFoundException x) {
        }
        System.out.println("OKAY");
    }

    /**
     * Test various exceptions are thrown as expected
     */
    @SuppressWarnings("unchecked")
    static void exceptionsTests(Path dir)
        throws IOException
    {
        System.out.println("-- Exceptions --");

        PosixFileAttributeView view =
            Files.getFileAttributeView(dir,PosixFileAttributeView.class);

        // NullPointerException
        try {
            view.setOwner(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            view.setGroup(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }

        UserPrincipalLookupService lookupService = dir.getFileSystem()
            .getUserPrincipalLookupService();
        try {
            lookupService.lookupPrincipalByName(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            lookupService.lookupPrincipalByGroupName(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            view.setPermissions(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
        }
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(null);
            view.setPermissions(perms);
            throw new RuntimeException("NullPointerException not thrown");
        }  catch (NullPointerException x) {
        }

        // ClassCastException
        try {
            Set perms = new HashSet();  // raw type
            perms.add(new Object());
            view.setPermissions(perms);
            throw new RuntimeException("ClassCastException not thrown");
        }  catch (ClassCastException x) {
        }

        System.out.println("OKAY");
    }

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            if (!Files.getFileStore(dir).supportsFileAttributeView("posix")) {
                System.out.println("PosixFileAttributeView not supported");
                return;
            }

            permissionTests(dir);
            createTests(dir);
            ownerTests(dir);
            lookupPrincipalTests(dir);
            exceptionsTests(dir);

        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
