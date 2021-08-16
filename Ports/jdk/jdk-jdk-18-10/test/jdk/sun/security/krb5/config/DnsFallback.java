/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6673164 6552334 8077102
 * @modules java.security.jgss/sun.security.krb5:+open
 * @run main/othervm DnsFallback
 * @summary fix dns_fallback parse error, and use dns by default
 */

import java.io.*;
import java.lang.reflect.Method;
import sun.security.krb5.Config;

public class DnsFallback {

    static Method useDNS_Realm;
    static Method useDNS_KDC;

    public static void main(String[] args) throws Exception {

        useDNS_Realm = Config.class.getDeclaredMethod("useDNS_Realm");
        useDNS_Realm.setAccessible(true);
        useDNS_KDC = Config.class.getDeclaredMethod("useDNS_KDC");
        useDNS_KDC.setAccessible(true);


        // for 6673164
        check("true", "true", true, true);
        check("false", "true", false, false);
        check("true", "false", true, true);
        check("false", "false", false, false);
        check("true", null, true, true);
        check("false", null, false, false);
        check(null, "true", true, true);
        check(null, "false", false, false);

        // for 6552334, no longer true
        //check(null, null, true, true);

        // 8077102
        check(null, null, false, true);
    }

    /**
     * Sets and checks.
     *
     * @param u dns_lookup_XXX value set, none if null
     * @param f dns_fallback value set, none if null
     * @param r expected useDNS_Realm
     * @param k expected useDNS_KDC
     */
    static void check(String u, String f, boolean r, boolean k)
            throws Exception {

        try (PrintStream ps =
                new PrintStream(new FileOutputStream("dnsfallback.conf"))) {
            ps.println("[libdefaults]\n");
            if (u != null) {
                ps.println("dns_lookup_realm=" + u);
                ps.println("dns_lookup_kdc=" + u);
            }
            if (f != null) {
                ps.println("dns_fallback=" + f);
            }
        }

        System.setProperty("java.security.krb5.conf", "dnsfallback.conf");
        Config.refresh();
        System.out.println("Testing " + u + ", " + f + ", " + r + ", " + k);

        if (!useDNS_Realm.invoke(Config.getInstance()).equals(r)) {
            throw new Exception("useDNS_Realm Fail");
        }

        if (!useDNS_KDC.invoke(Config.getInstance()).equals(k)) {
            throw new Exception("useDNS_KDC Fail");
        }
    }
}

