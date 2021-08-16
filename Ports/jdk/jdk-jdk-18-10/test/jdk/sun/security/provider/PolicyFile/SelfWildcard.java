/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4510424
 * @summary ${{self}} expansion fails for grants with wildcard principal names
 */

import java.util.*;
import java.security.*;
import javax.security.auth.Subject.*;
import javax.security.auth.x500.*;

public class SelfWildcard {

    private static final String SELF_ONE =
        "javax.security.auth.x500.X500Principal \"CN=foo\"";
    private static final String SELF_TWOTHREE =
        "javax.security.auth.x500.X500Principal \"CN=foo\", " +
        "javax.security.auth.x500.X500Principal \"CN=bar\"";
    private static final String SELF_FOURFIVE =
        "javax.security.auth.x500.X500Principal \"CN=foo\", " +
        "javax.security.auth.x500.X500Principal \"CN=bar\", " +
        "com.sun.security.auth.UnixPrincipal \"foobar\"";

    public static void main(String[] args) throws Exception {
        if (System.getProperty("test.src") == null) {
            System.setProperty("test.src", ".");
        }
        System.setProperty("java.security.policy",
                "file:${test.src}/SelfWildcard.policy");

        Principal[] ps = {
                new X500Principal("CN=foo"),
                new X500Principal("CN=bar"),
                new com.sun.security.auth.UnixPrincipal("foobar") };
        ProtectionDomain pd = new ProtectionDomain
                (new CodeSource(null, (java.security.cert.Certificate[]) null),
                    null, null, ps);
        PermissionCollection perms = Policy.getPolicy().getPermissions(pd);
        System.out.println("perms = " + perms);
        System.out.println();

        Enumeration e = perms.elements();
        while (e.hasMoreElements()) {
            Permission p = (Permission)e.nextElement();
            if (p instanceof UnresolvedPermission &&
                p.toString().indexOf(SELF_ONE) < 0 &&
                p.toString().indexOf(SELF_TWOTHREE) < 0 &&
                p.toString().indexOf(SELF_FOURFIVE) < 0) {
                throw new SecurityException("Test Failed");
            }
        }

        System.out.println("Test Succeeded");
    }
}
