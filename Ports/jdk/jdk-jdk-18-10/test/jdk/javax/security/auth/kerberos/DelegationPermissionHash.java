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

import javax.security.auth.kerberos.DelegationPermission;

/*
 * @test
 * @bug 8129575
 * @summary Checks if DelegationPermission.hashCode() works fine
 */
public class DelegationPermissionHash {

    static final String princ1 = "backup/bar.example.com@EXAMPLE.COM";
    static final String princ2 = "backup/foo.example.com@EXAMPLE.COM";
    static final String ONE_SPACE = " ";
    static final String TWO_SPACES = "  ";
    static final String QUOTE = "\"";

    public static void main(String[] args) {
        DelegationPermission one = new DelegationPermission(
                QUOTE + princ1 + QUOTE + ONE_SPACE + QUOTE + princ2 + QUOTE);
        DelegationPermission two = new DelegationPermission(
                QUOTE + princ1 + QUOTE + TWO_SPACES + QUOTE + princ2 + QUOTE);

        System.out.println("one.getName() = " + one.getName());
        System.out.println("two.getName() = " + two.getName());

        if (!one.implies(two) || !two.implies(one)) {
            throw new RuntimeException("Test failed: "
                    + "one and two don't imply each other");
        }

        if (!one.equals(two)) {
            throw new RuntimeException("Test failed: one is not equal to two");
        }

        System.out.println("one.hashCode() = " + one.hashCode());
        System.out.println("two.hashCode() = " + two.hashCode());
        if (one.hashCode() != two.hashCode()) {
            throw new RuntimeException("Test failed: hash codes are not equal");
        }

        System.out.println("Test passed");
    }
}
