/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4291610
 * @summary BasicPermission.newPermissionCollection collection does not
 *      enforce homogeneity
 */

import java.security.BasicPermission;

public class Homogeneity {

    public static void main(String[] args) {

        java.lang.RuntimePermission rp = new java.lang.RuntimePermission
                                        ("*");
        java.lang.RuntimePermission rp2 = new java.lang.RuntimePermission
                                        ("exitVM");
        java.net.NetPermission np = new java.net.NetPermission
                                        ("setDefaultAuthenticator");

        // should be able to add identical BasicPermission subclasses to the
        // same collection
        java.security.PermissionCollection perms = rp.newPermissionCollection();
        try {
            perms.add(rp);
            perms.add(rp2);
        } catch (IllegalArgumentException iae) {
            throw new SecurityException("GOOD ADD TEST FAILED");
        }

        // make sure you can't add different BasicPermission subclasses
        // to the same collection
        try {
            // this should fail
            perms.add(np);
            throw new SecurityException("BAD ADD TEST FAILED");
        } catch (IllegalArgumentException iae) {
            // good
        }

        // make sure a BasicPermissionCollection doesn't imply
        // random BasicPermission subclasses
        if (perms.implies(np)) {
            throw new SecurityException("IMPLIES TEST FAILED");
        }
    }
}
