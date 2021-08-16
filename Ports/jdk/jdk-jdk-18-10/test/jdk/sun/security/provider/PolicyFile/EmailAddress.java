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
 * @bug 4702543
 * @summary X500Principal encodes EmailAddress incorrectly -
 *
 *      fix has compatibility ramifications for policy.
 *
 *      this test is related to the Alias.java test in the same directory.
 *      the email address encoding in EmailAddress.policy is the one
 *      taken from the persistent certificate stored in Alias.keystore,
 *      and which has the incorrect encoding.  the alias is 'duke',
 *      and the DN is:  "emailaddress=duke@sun".  the cert was generated
 *      by a 1.4 JDK, so it has the wrong encoding for "duke@sun"
 *      (UTF-8 string instead of IA5String, i believe).
 *
 *      administrators would have placed an incorrectly encoded DN entry
 *      like this in their policies.  the fix for the above bug
 *      would have broken their policy because the incorrect
 *      encoding would be compared to a properly encoded DN from
 *      the current call thread.  if you run this test without
 *      a fix for the compatibility issue, the debug output will
 *      show the differences in the encodings.
 *
 *      so in addition to fixing the encoding,
 *      the policy implementation was updated to read the
 *      incorrectly encoded DN strings, generate new X500Principals,
 *      and dump out new DN strings that had the correct encoding.
 *      thus access control checks would no longer fail.
 *
 * @run main/othervm/policy=EmailAddress.policy -Djava.security.debug=policy EmailAddress
 */

import java.security.*;
import java.util.*;

public class EmailAddress {

    public static void main(String[] args) {

        Principal[] principals = new Principal[1];
        principals[0] = new javax.security.auth.x500.X500Principal
                                        ("emailaddress=duke@sun");

        java.net.URL url = null;
        try {
            url = new java.net.URL("http://emailaddress");
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

        if (perms.implies(new SecurityPermission("EMAILADDRESS"))) {
            System.out.println("test succeeded");
        } else {
            System.out.println("test 2 failed");
            throw new SecurityException("test failed");
        }
    }
}
