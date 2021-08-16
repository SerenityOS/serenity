/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6268673
 * @summary Test new RuntimePermission.exitVM wildcard syntax
 * @author Sean Mullan
 */

import java.security.PermissionCollection;

public class ExitVM {

    public static void main(String[]args) throws Exception {

        RuntimePermission newWildcard = new RuntimePermission("exitVM.*");
        RuntimePermission oldWildcard = new RuntimePermission("exitVM");
        RuntimePermission other = new RuntimePermission("exitVM.23");
        System.out.println("Testing RuntimePermission(\"exitVM.*\")");
        System.out.println("    testing getName()");
        if (!newWildcard.getName().equals("exitVM.*")) {
            throw new Exception
                ("expected: exitVM.* received:" + newWildcard.getName());
        }
        System.out.println
            ("    testing equals(new RuntimePermission(\"exitVM.*\"))");
        if (!newWildcard.equals(new RuntimePermission("exitVM.*"))) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing equals(new RuntimePermission(\"exitVM.23\"))");
        if (newWildcard.equals(other)) {
            throw new Exception("expected false, received true");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.23\"))");
        if (!newWildcard.implies(other)) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.*\"))");
        if (!newWildcard.implies(new RuntimePermission("exitVM.*"))) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM\"))");
        if (!newWildcard.implies(oldWildcard)) {
            throw new Exception("expected true, received false");
        }
        System.out.println("Testing RuntimePermission(\"exitVM\")");
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.*\"))");
        if (!oldWildcard.implies(newWildcard)) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM\"))");
        if (!oldWildcard.implies(new RuntimePermission("exitVM"))) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.23\"))");
        if (!oldWildcard.implies(other)) {
            throw new Exception("expected true, received false");
        }

        // now test permission collections
        System.out.println("Testing PermissionCollection containing " +
                           "RuntimePermission(\"exitVM.*\")");
        PermissionCollection newPC = newWildcard.newPermissionCollection();
        newPC.add(newWildcard);
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.23\"))");
        if (!newPC.implies(other)) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.*\"))");
        if (!newPC.implies(new RuntimePermission("exitVM.*"))) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM\"))");
        if (!newPC.implies(oldWildcard)) {
            throw new Exception("expected true, received false");
        }
        System.out.println("Testing PermissionCollection containing " +
                           "RuntimePermission(\"exitVM\")");
        PermissionCollection oldPC = oldWildcard.newPermissionCollection();
        oldPC.add(oldWildcard);
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.23\"))");
        if (!oldPC.implies(other)) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM.*\"))");
        if (!oldPC.implies(new RuntimePermission("exitVM.*"))) {
            throw new Exception("expected true, received false");
        }
        System.out.println
            ("    testing implies(new RuntimePermission(\"exitVM\"))");
        if (!oldPC.implies(oldWildcard)) {
            throw new Exception("expected true, received false");
        }
    }
}
