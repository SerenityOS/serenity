/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4364848
 * @bug 4364851
 * @summary     PrivateCredentialPermission doesn't ignore principal order
 *              PrivateCredentialPermission incorrectly ignores case
 */

import javax.security.auth.*;

public class Equals {

    public static void main(String[] args) {

        // test regular equals and implies
        PrivateCredentialPermission pcp1 = new PrivateCredentialPermission
                ("a b \"pcp1\" c \"pcp2\"", "read");
        PrivateCredentialPermission pcp2 = new PrivateCredentialPermission
                ("a b \"pcp1\" c \"pcp2\"", "read");
        if (!pcp1.equals(pcp2) || !pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #1");
        if (!pcp1.implies(pcp2) || !pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #2");

        // reverse principals
        pcp1 = new PrivateCredentialPermission
                ("a c \"pcp2\" b \"pcp1\"", "read");
        pcp2 = new PrivateCredentialPermission
                ("a b \"pcp1\" c \"pcp2\"", "read");
        if (!pcp1.equals(pcp2) || !pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #3");
        if (!pcp1.implies(pcp2) || !pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #4");

        // test equals/implies failure
        pcp1 = new PrivateCredentialPermission
                ("a b \"pcp1\"", "read");
        if (pcp1.equals(pcp2) || pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #5");
        if (!pcp1.implies(pcp2) || pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #6");

        // test wildcards
        pcp1 = new PrivateCredentialPermission
                ("* b \"pcp1\" c \"pcp2\"", "read");
        if (pcp1.equals(pcp2) || pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #7");
        if (!pcp1.implies(pcp2) || pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #8");

        pcp1 = new PrivateCredentialPermission
                ("a c \"pcp2\" b \"*\"", "read");
        if (pcp1.equals(pcp2) || pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #9");
        if (!pcp1.implies(pcp2) || pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #10");

        pcp2 = new PrivateCredentialPermission
                ("a b \"*\" c \"pcp2\"", "read");
        if (!pcp1.equals(pcp2) || !pcp2.equals(pcp1))
            throw new SecurityException("Equals test failed: #11");
        if (!pcp1.implies(pcp2) || !pcp2.implies(pcp1))
            throw new SecurityException("Equals test failed: #12");

        System.out.println("Equals test passed");
    }
}
