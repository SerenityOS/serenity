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
 * @summary Unit test for ServicePermissionCollection subclass
 */

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.Enumeration;
import javax.security.auth.kerberos.ServicePermission;

public class ServicePermissionCollection {

    private static final String FOO = "host/foo.example.com@EXAMPLE.COM";
    private static final String BAR = "host/bar.example.com@EXAMPLE.COM";
    private static final String BAZ = "host/baz.example.com@EXAMPLE.COM";

    public static void main(String[] args) throws Exception {

        int testFail = 0;

        ServicePermission perm = new ServicePermission(FOO, "accept");
        PermissionCollection perms = perm.newPermissionCollection();

        // test 1
        System.out.println
            ("test 1: add throws IllegalArgExc for wrong permission type");
        try {
            perms.add(new SecurityPermission("createAccessControlContext"));
            System.err.println("Expected IllegalArgumentException");
            testFail++;
        } catch (IllegalArgumentException iae) {}

        // test 2
        System.out.println("test 2: implies returns false for wrong perm type");
        if (perms.implies(new SecurityPermission("getPolicy"))) {
            System.err.println("Expected false, returned true");
            testFail++;
        }

        // test 3
        System.out.println
            ("test 3: implies returns true for match on name and action");
        perms.add(new ServicePermission(FOO, "accept"));
        if (!perms.implies(new ServicePermission(FOO, "accept"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 4
        System.out.println
            ("test 4: implies returns false for match on name but not action");
        if (perms.implies(new ServicePermission(FOO, "initiate"))) {
            System.err.println("Expected false, returned true");
            testFail++;
        }

        // test 5
        System.out.println("test 5: implies returns true for match on " +
                           "name and subset of actions");
        perms.add(new ServicePermission(BAR, "accept, initiate"));
        if (!perms.implies(new ServicePermission(BAR, "accept"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 6
        System.out.println("test 6: implies returns false for aggregate " +
                           "match on name and action");
        perms.add(new ServicePermission(BAZ, "accept"));
        perms.add(new ServicePermission(BAZ, "initiate"));
        if (!perms.implies(new ServicePermission(BAZ, "initiate"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }
        if (!perms.implies(new ServicePermission(BAZ, "initiate, accept"))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 7
        System.out.println("test 7: implies returns true for wildcard " +
                           "match on name and action");
        perms.add(new ServicePermission("*", "initiate"));
        if (!perms.implies(new ServicePermission("Duke", "initiate"))) {
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
        // the 2 FOO permissions and the 2 BAZ permisssions
        // are combined into one
        if (numPerms != 4) {
            System.err.println("Expected 4, got " + numPerms);
            testFail++;
        }

        if (testFail > 0) {
            throw new Exception(testFail + " test(s) failed");
        }
    }
}
