/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4337761
 * @summary add principal "alias" grant syntax to policy file
 *
 * Note: the keystore password is "Alias.password".
 * Note: this test also covers 4501853 as well.
 *
 * @run main/othervm/policy=Alias.policy Alias
 */

import java.security.*;
import java.util.*;

public class Alias {

    public static void main(String[] args) {

        Principal[] principals = new Principal[3];
        principals[0] = new com.sun.security.auth.UnixPrincipal("unix");
        principals[1] = new javax.security.auth.x500.X500Principal("cn=x509");
        principals[2] = new javax.security.auth.x500.X500Principal
                                        ("emailaddress=duke@sun");

        java.net.URL url = null;
        try {
            url = new java.net.URL("http://alias");
        } catch (java.net.MalformedURLException mue) {
            System.out.println("test 1 failed");
            throw new SecurityException(mue.getMessage());
        }
        CodeSource cs =
            new CodeSource(url, (java.security.cert.Certificate[]) null);

        ProtectionDomain pd = new ProtectionDomain
                (cs,
                null,
                null,
                principals);

        PermissionCollection perms = Policy.getPolicy().getPermissions(pd);

        if (perms.implies(new SecurityPermission("ALIAS"))) {
            System.out.println("test succeeded");
        } else {
            System.out.println("test 2 failed");
            throw new SecurityException("test failed");
        }
    }
}
