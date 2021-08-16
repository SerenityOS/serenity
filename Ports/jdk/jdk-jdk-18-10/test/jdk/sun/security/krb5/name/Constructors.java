/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6966259
 * @summary Make PrincipalName and Realm immutable
 * @modules java.security.jgss/sun.security.krb5
 * @run main/othervm Constructors
 */

import java.util.Arrays;
import sun.security.krb5.*;

public class Constructors {
    public static void main(String[] args) throws Exception {

        int type;
        boolean testNoDefaultDomain;

        // Part 1: on format

        // Good ones
        type = PrincipalName.KRB_NT_UNKNOWN;
        checkName("a", type, "R", "R", false, "a");
        checkName("a@R2", type, "R", "R", false, "a");
        checkName("a/b", type, "R", "R", false, "a", "b");
        checkName("a/b@R2", type, "R", "R", false, "a", "b");
        checkName("a/b/c", type, "R", "R", false, "a", "b", "c");
        checkName("a/b/c@R2", type, "R", "R", false, "a", "b", "c");
        // Weird ones
        checkName("a\\/b", type, "R", "R", false, "a/b");
        checkName("a\\/b\\/c", type, "R", "R", false, "a/b/c");
        checkName("a\\/b\\@R2", type, "R", "R", false, "a/b@R2");
        // Bad ones
        checkName("a", type, "", null, false);
        checkName("a/", type, "R", null, false);
        checkName("/a", type, "R", null, false);
        checkName("a//b", type, "R", null, false);
        checkName("a@", type, null, null, false);
        type = PrincipalName.KRB_NT_SRV_HST;

        // Part 2: on realm choices

        // When there is no default realm
        System.setProperty("java.security.krb5.conf",
                System.getProperty("test.src", ".") + "/empty.conf");
        Config.refresh();

        // A Windows client login to AD always has a default realm
        try {
            Realm r = Realm.getDefault();
            System.out.println("testNoDefaultDomain = false. Realm is " + r);
            testNoDefaultDomain = false;
        } catch (RealmException re) {
            // Great. This is what we expected
            testNoDefaultDomain = true;
        }

        if (testNoDefaultDomain) {
            type = PrincipalName.KRB_NT_UNKNOWN;
            checkName("a", type, "R1", "R1", false, "a");      // arg
            checkName("a@R1", type, null, "R1", false, "a");   // or r in name
            checkName("a@R2", type, "R1", "R1", false, "a");   // arg over r
            checkName("a", type, null, null, false);      // fail if none
            checkName("a/b@R1", type, null, "R1", false, "a", "b");
            type = PrincipalName.KRB_NT_SRV_HST;
            // Let's pray "b.h" won't be canonicalized
            checkName("a/b.h", type, "R1", "R1", false, "a", "b.h");    // arg
            checkName("a/b.h@R1", type, null, "R1", false, "a", "b.h"); // or r in name
            checkName("a/b.h@R1", type, "R2", "R2", false, "a", "b.h"); // arg over r
            checkName("a/b.h", type, null, null, false);    // fail if none
        }

        // When there is default realm
        System.setProperty("java.security.krb5.conf",
                System.getProperty("test.src", ".") + "/krb5.conf");
        Config.refresh();

        type = PrincipalName.KRB_NT_UNKNOWN;
        checkName("a", type, "R1", "R1", false, "a");      // arg
        checkName("a@R1", type, null, "R1", false, "a");   // or r in name
        checkName("a@R2", type, "R1", "R1", false, "a");   // arg over r
        checkName("a", type, null, "R", true, "a");       // default
        checkName("a/b", type, null, "R", true, "a", "b");
        type = PrincipalName.KRB_NT_SRV_HST;
        checkName("a/b.h3", type, "R1", "R1", false, "a", "b.h3");     // arg
        checkName("a/b.h@R1", type, null, "R1", false, "a", "b.h");    // or r in name
        checkName("a/b.h3@R2", type, "R1", "R1", false, "a", "b.h3");  // arg over r
        checkName("a/b.h2", type, "R1", "R1", false, "a", "b.h2");     // arg over map
        checkName("a/b.h2@R1", type, null, "R1", false, "a", "b.h2");  // r over map
        checkName("a/b.h2", type, null, "R2", true, "a", "b.h2");     // map
        checkName("a/b.h", type, null, "R", true, "a", "b.h");        // default
    }

    // Check if the creation matches the expected output.
    // Note: realm == null means creation failure
    static void checkName(String n, int t, String s,
            String realm, boolean deduced, String... parts)
            throws Exception {
        PrincipalName pn = null;
        try {
            pn = new PrincipalName(n, t, s);
        } catch (Exception e) {
            if (realm == null) {
                return; // This is expected
            } else {
                throw e;
            }
        }
        if (!pn.getRealmAsString().equals(realm)
                || !Arrays.equals(pn.getNameStrings(), parts)) {
            throw new Exception(pn.toString() + " vs "
                    + Arrays.toString(parts) + "@" + realm);
        }
        if (deduced != pn.isRealmDeduced()) {
            throw new Exception("pn.realmDeduced is " + pn.isRealmDeduced());
        }
    }
}
