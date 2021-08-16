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
 * @bug 4313887 6838333 6891404
 * @summary Unit test for java.nio.file.attribute.AclFileAttribueView
 * @library ../..
 * @key randomness
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

import static java.nio.file.attribute.AclEntryType.*;
import static java.nio.file.attribute.AclEntryPermission.*;
import static java.nio.file.attribute.AclEntryFlag.*;

public class Basic {

    static void printAcl(List<AclEntry> acl) {
        for (AclEntry entry: acl) {
            System.out.format("  %s%n", entry);
        }
    }

    // sanity check read and writing ACL
    static void testReadWrite(Path dir) throws IOException {
        Path file = dir.resolve("foo");
        if (Files.notExists(file))
            Files.createFile(file);

        AclFileAttributeView view =
            Files.getFileAttributeView(file, AclFileAttributeView.class);

        // print existing ACL
        List<AclEntry> acl = view.getAcl();
        System.out.println(" -- current ACL --");
        printAcl(acl);

        // insert entry to grant owner read access
        UserPrincipal owner = view.getOwner();
        AclEntry entry = AclEntry.newBuilder()
            .setType(ALLOW)
            .setPrincipal(owner)
            .setPermissions(READ_DATA, READ_ATTRIBUTES)
            .build();
        System.out.println(" -- insert (entry 0) --");
        System.out.format("  %s%n", entry);
        acl.add(0, entry);
        view.setAcl(acl);

        // re-ACL and check entry
        List<AclEntry> newacl = view.getAcl();
        System.out.println(" -- current ACL --");
        printAcl(acl);
        if (!newacl.get(0).equals(entry)) {
            throw new RuntimeException("Entry 0 is not expected");
        }

        // if PosixFileAttributeView then repeat test with OWNER@
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            owner = file.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName("OWNER@");
            entry = AclEntry.newBuilder(entry).setPrincipal(owner).build();

            System.out.println(" -- replace (entry 0) --");
            System.out.format("  %s%n", entry);

            acl.set(0, entry);
            view.setAcl(acl);
            newacl = view.getAcl();
            System.out.println(" -- current ACL --");
            printAcl(acl);
            if (!newacl.get(0).equals(entry)) {
                throw new RuntimeException("Entry 0 is not expected");
            }
        }
    }

    static FileAttribute<List<AclEntry>> asAclAttribute(final List<AclEntry> acl) {
        return new FileAttribute<List<AclEntry>>() {
            public String name() { return "acl:acl"; }
            public List<AclEntry> value() { return acl; }
        };
    }

    static void assertEquals(List<AclEntry> actual, List<AclEntry> expected) {
        if (!actual.equals(expected)) {
            System.err.format("Actual: %s\n", actual);
            System.err.format("Expected: %s\n", expected);
            throw new RuntimeException("ACL not expected");
        }
    }

    // sanity check create a file or directory with initial ACL
    static void testCreateFile(Path dir) throws IOException {
        UserPrincipal user = Files.getOwner(dir);
        AclFileAttributeView view;

        // create file with initial ACL
        System.out.println("-- create file with initial ACL --");
        Path file = dir.resolve("gus");
        List<AclEntry> fileAcl = Arrays.asList(
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(user)
                .setPermissions(SYNCHRONIZE, READ_DATA, WRITE_DATA,
                    READ_ATTRIBUTES, READ_ACL, WRITE_ATTRIBUTES, DELETE)
                .build());
        Files.createFile(file, asAclAttribute(fileAcl));
        view = Files.getFileAttributeView(file, AclFileAttributeView.class);
        assertEquals(view.getAcl(), fileAcl);

        // create directory with initial ACL
        System.out.println("-- create directory with initial ACL --");
        Path subdir = dir.resolve("stuff");
        List<AclEntry> dirAcl = Arrays.asList(
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(user)
                .setPermissions(SYNCHRONIZE, ADD_FILE, DELETE)
                .build(),
            AclEntry.newBuilder(fileAcl.get(0))
                .setFlags(FILE_INHERIT)
                .build());
        Files.createDirectory(subdir, asAclAttribute(dirAcl));
        view = Files.getFileAttributeView(subdir, AclFileAttributeView.class);
        assertEquals(view.getAcl(), dirAcl);
    }

    public static void main(String[] args) throws IOException {
        // use work directory rather than system temporary directory to
        // improve chances that ACLs are supported
        Path dir = Paths.get("./work" + new Random().nextInt());
        Files.createDirectory(dir);
        try {
            if (!Files.getFileStore(dir).supportsFileAttributeView("acl")) {
                System.out.println("ACLs not supported - test skipped!");
                return;
            }
            testReadWrite(dir);

            // only currently feasible on Windows
            if (System.getProperty("os.name").startsWith("Windows"))
                testCreateFile(dir);

        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
