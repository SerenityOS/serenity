/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8056179
 * @summary Unit test for FilePermissionCollection subclass
 */

import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.Enumeration;

public class FilePermissionCollection {

    public static void main(String[] args) throws Exception {

        int testFail = 0;

        FilePermission perm = new FilePermission("/tmp/foo", "read");
        PermissionCollection perms = perm.newPermissionCollection();

        // test 1
        System.out.println
            ("test 1: add throws IllegalArgExc for wrong perm type");
        try {
            perms.add(new SecurityPermission("createAccessControlContext"));
            System.out.println("Expected IllegalArgumentException");
            testFail++;
        } catch (IllegalArgumentException iae) {}

        // test 2
        System.out.println("test 2: implies returns false for wrong perm type");
        if (perms.implies(new SecurityPermission("getPolicy"))) {
            System.out.println("Expected false, returned true");
            testFail++;
        }

        // test 3
        System.out.println("test 3: implies returns true for match on " +
                           "name and action");
        perms.add(new FilePermission("/tmp/foo", "read"));
        if (!perms.implies(new FilePermission("/tmp/foo", "read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 4
        System.out.println("test 4: implies returns false for match on " +
                           "name but not action");
        if (perms.implies(new FilePermission("/tmp/foo", "write"))) {
            System.out.println("Expected false, returned true");
            testFail++;
        }

        // test 5
        System.out.println("test 5: implies returns true for match on " +
                           "name and subset of actions");
        perms.add(new FilePermission("/tmp/bar", "read, write"));
        if (!perms.implies(new FilePermission("/tmp/bar", "write"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 6
        System.out.println("test 6: implies returns true for aggregate " +
                           "match on name and action");
        perms.add(new FilePermission("/tmp/baz", "read"));
        perms.add(new FilePermission("/tmp/baz", "write"));
        if (!perms.implies(new FilePermission("/tmp/baz", "read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }
        if (!perms.implies(new FilePermission("/tmp/baz", "write,read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 7
        System.out.println("test 7: implies returns true for wildcard " +
                           "and match on action");
        perms.add(new FilePermission("/usr/tmp/*", "read"));
        if (!perms.implies(new FilePermission("/usr/tmp/foo", "read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 8
        System.out.println
            ("test 8: implies returns false for non-match on wildcard");
        if (perms.implies(new FilePermission("/usr/tmp/bar/foo", "read"))) {
            System.out.println("Expected false, returned true");
            testFail++;
        }

        // test 9
        System.out.println
            ("test 9: implies returns true for deep wildcard match");
        perms.add(new FilePermission("/usr/tmp/-", "read"));
        if (!perms.implies(new FilePermission("/usr/tmp/bar/foo", "read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 10
        //System.out.println("test 10: implies returns true for relative match");
        perms.add(new FilePermission(".", "read"));
        //if (!perms.implies(new FilePermission(System.getProperty("user.dir"),
        //                                      "read"))) {
        //    System.out.println("Expected true, returned false");
        //    testFail++;
        //}

        // test 11
        System.out.println("test 11: implies returns true for all " +
                           "wildcard and match on action");
        perms.add(new FilePermission("<<ALL FILES>>", "read"));
        if (!perms.implies(new FilePermission("/tmp/foobar", "read"))) {
            System.out.println("Expected true, returned false");
            testFail++;
        }

        // test 12
        System.out.println("test 12: implies returns false for wildcard " +
                           "and non-match on action");
        if (perms.implies(new FilePermission("/tmp/foobar", "write"))) {
            System.out.println("Expected false, returned true");
            testFail++;
        }

        // test 13
        System.out.println("test 13: elements returns correct number of perms");
        int numPerms = 0;
        Enumeration<Permission> e = perms.elements();
        while (e.hasMoreElements()) {
            numPerms++;
            System.out.println(e.nextElement());
        }
        // the two "/tmp/baz" entries were combined into one
        if (numPerms != 7) {
            System.out.println("Expected 7, got " + numPerms);
            testFail++;
        }

        if (testFail > 0) {
            throw new Exception(testFail + " test(s) failed");
        }
    }
}
