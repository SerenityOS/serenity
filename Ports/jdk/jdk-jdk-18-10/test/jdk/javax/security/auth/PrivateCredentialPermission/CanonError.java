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
 * @bug 4364833
 * @summary PrivateCredentialPermission incorrectly canonicalizes
 *              spaces in names
 */

import javax.security.auth.*;

public class CanonError {

    public static void main(String[] args) {

        // test regular equals and implies
        PrivateCredentialPermission pcp1 = new PrivateCredentialPermission
                ("a b \"pcp1\"", "read");
        PrivateCredentialPermission pcp2 = new PrivateCredentialPermission
                ("a b \"pcp1\"", "read");
        if (!pcp1.equals(pcp2) || !pcp2.equals(pcp1))
            throw new SecurityException("CanonError test failed: #1");
        if (!pcp1.implies(pcp2) || !pcp2.implies(pcp1))
            throw new SecurityException("CanonError test failed: #2");

        // test equals/implies failure
        PrivateCredentialPermission pcp3 = new PrivateCredentialPermission
                ("a b \"pcp3\"", "read");
        if (pcp1.equals(pcp3) || pcp3.equals(pcp1))
            throw new SecurityException("CanonError test failed: #3");
        if (pcp1.implies(pcp3) || pcp3.implies(pcp1))
            throw new SecurityException("CanonError test failed: #4");

        // test spaces in name
        PrivateCredentialPermission pcp_4 = new PrivateCredentialPermission
                ("a b \"pcp 4\"", "read");
        PrivateCredentialPermission pcp__4 = new PrivateCredentialPermission
                ("a b \"pcp  4\"", "read");
        if (pcp_4.equals(pcp__4) || pcp__4.equals(pcp_4))
            throw new SecurityException("CanonError test failed: #5");
        if (pcp_4.implies(pcp__4) || pcp__4.implies(pcp_4))
            throw new SecurityException("CanonError test failed: #6");

        String credClass = pcp__4.getCredentialClass();
        System.out.println("credentialClass = " + credClass);
        String[][] principals = pcp__4.getPrincipals();
        if (!principals[0][1].equals("pcp  4"))
            throw new SecurityException("CanonError test failed: #7");
        for (int i = 0; i < principals.length; i++) {
            for (int j = 0; j < 2; j++) {
                System.out.println("principals[" + i + "][" + j + "] = " +
                                principals[i][j]);
            }
        }

        credClass = pcp_4.getCredentialClass();
        System.out.println("credentialClass = " + credClass);
        principals = pcp_4.getPrincipals();
        if (!principals[0][1].equals("pcp 4"))
            throw new SecurityException("CanonError test failed: #8");
        for (int i = 0; i < principals.length; i++) {
            for (int j = 0; j < 2; j++) {
                System.out.println("principals[" + i + "][" + j + "] = " +
                                principals[i][j]);
            }
        }

        System.out.println("CanonError test passed");
    }
}
