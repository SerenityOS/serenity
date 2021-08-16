/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4503866
 * @summary generalized permission name expansion in policy files
 *
 * Note: the keystore used is Alias.keystore - password is "Alias.password".
 *
 * @run main/othervm/policy=AliasExpansion.policy AliasExpansion
 */

import javax.security.auth.*;

public class AliasExpansion {

    public static void main(String[] args) throws Exception {

        PrivateCredentialPermission pcp = new PrivateCredentialPermission
                ("java.lang.String javax.security.auth.x500.X500Principal " +
                "\"CN=x509\"", "read");
        SecurityManager sm = System.getSecurityManager();
        sm.checkPermission(pcp);

        java.security.SecurityPermission sp =
                new java.security.SecurityPermission
                        ("abcde javax.security.auth.x500.X500Principal " +
                        "\"CN=x509\" fghij " +
                        "javax.security.auth.x500.X500Principal " +
                        "\"CN=x509\"");
        sm.checkPermission(sp);

        sp = new java.security.SecurityPermission
                ("javax.security.auth.x500.X500Principal \"CN=x509\"");
        sm.checkPermission(sp);

        sp = new java.security.SecurityPermission
                ("javax.security.auth.x500.X500Principal \"CN=x509\" abc");
        sm.checkPermission(sp);

        System.out.println("test passed");
    }
}
