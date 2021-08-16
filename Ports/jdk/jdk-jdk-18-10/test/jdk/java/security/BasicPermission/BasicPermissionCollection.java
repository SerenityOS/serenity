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
 * @summary Unit test for BasicPermissionCollection subclass
 */

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.Enumeration;

public class BasicPermissionCollection {

    public static void main(String[] args) throws Exception {

        int testFail = 0;

        TestPermission perm = new TestPermission("foo");
        PermissionCollection perms = perm.newPermissionCollection();

        // test 1
        System.out.println("test 1: add throws IllegalArgumentExc");
        try {
            perms.add(new SecurityPermission("createAccessControlContext"));
            System.err.println("Expected IllegalArgumentException");
            testFail++;
        } catch (IllegalArgumentException iae) {}

        // test 2
        System.out.println("test 2: implies returns false for wrong class");
        if (perms.implies(new SecurityPermission("getPolicy"))) {
            System.err.println("Expected false, returned true");
            testFail++;
        }

        // test 3
        System.out.println("test 3: implies returns true for match on name");
        perms.add(new TestPermission("foo"));
        if (!perms.implies(new TestPermission("foo"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 4
        System.out.println("test 4: implies returns true for wildcard match");
        perms.add(new TestPermission("bar.*"));
        if (!perms.implies(new TestPermission("bar.foo"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 5
        System.out.println
            ("test 5: implies returns false for invalid wildcard");
        perms.add(new TestPermission("baz*"));
        if (perms.implies(new TestPermission("baz.foo"))) {
            System.err.println("Expected false, returned true");
            testFail++;
        }

        // test 6
        System.out.println
            ("test 6: implies returns true for deep wildcard match");
        if (!perms.implies(new TestPermission("bar.foo.baz"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 7
        System.out.println
            ("test 7: implies returns true for all wildcard match");
        perms.add(new TestPermission("*"));
        if (!perms.implies(new TestPermission("yes"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 8
        System.out.println("test 8: elements returns correct number of perms");
        int numPerms = 0;
        Enumeration<Permission> e = perms.elements();
        while (e.hasMoreElements()) {
            numPerms++;
            System.out.println(e.nextElement());
        }
        if (numPerms != 4) {
            System.err.println("Expected 4, got " + numPerms);
            testFail++;
        }

        if (testFail > 0) {
            throw new Exception(testFail + " test(s) failed");
        }
    }

    private static class TestPermission extends BasicPermission {
        TestPermission(String name) {
            super(name);
        }
    }
}
