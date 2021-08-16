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
 * @summary Unit test for DelegationPermissionCollection subclass
 */

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.Enumeration;
import javax.security.auth.kerberos.DelegationPermission;

public class DelegationPermissionCollection {

    private static final String FOO = "\"host/foo.example.com@EXAMPLE.COM\"";
    private static final String BAR = "\"host/bar.example.com@EXAMPLE.COM\"";
    private static final String TGT = "\"krbtgt/EXAMPLE.COM@EXAMPLE.COM\"";

    public static void main(String[] args) throws Exception {

        int testFail = 0;

        DelegationPermission perm = new DelegationPermission(FOO + " " + TGT);
        PermissionCollection perms = perm.newPermissionCollection();

        // test 1
        System.out.println
            ("test 1: add throws IllegalArgException for wrong perm type");
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
        System.out.println("test 3: implies returns true for match on name");
        perms.add(new DelegationPermission(FOO + " " + TGT));
        if (!perms.implies(new DelegationPermission(FOO + " " + TGT))) {
            System.err.println("Expected true, returned false");
            testFail++;
        }

        // test 4
        System.out.println
            ("test 4: implies returns false for non-match on name");
        if (perms.implies(new DelegationPermission(BAR + " " + TGT))) {
            System.err.println("Expected false, returned true");
            testFail++;
        }

        // test 5
        System.out.println("test 5: elements returns correct number of perms");
        int numPerms = 0;
        Enumeration<Permission> e = perms.elements();
        while (e.hasMoreElements()) {
            numPerms++;
            System.out.println(e.nextElement());
        }
        if (numPerms != 1) {
            System.err.println("Expected 1, got " + numPerms);
            testFail++;
        }

        if (testFail > 0) {
            throw new Exception(testFail + " test(s) failed");
        }
    }
}
