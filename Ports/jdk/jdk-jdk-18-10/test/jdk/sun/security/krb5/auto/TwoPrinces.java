/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6894072 8194486
 * @summary always refresh keytab
 * @library /test/lib
 * @compile -XDignore.symbol.file TwoPrinces.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts TwoPrinces
 */

import java.io.File;
import java.io.FileOutputStream;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;

public class TwoPrinces {

    public static void main(String[] args)
            throws Exception {

        KDC k1 = KDC.create("R1");
        k1.addPrincipal("u1", "hello".toCharArray());
        k1.addPrincipalRandKey("krbtgt/R1");
        k1.addPrincipalRandKey("host/same.host");

        KDC k2 = KDC.create("R2");
        k2.addPrincipal("u2", "hello".toCharArray());
        k2.addPrincipalRandKey("krbtgt/R2");
        k2.addPrincipalRandKey("host/same.host");

        System.setProperty("java.security.krb5.conf", "krb5.conf");

        // R1 is the default realm now
        KDC.saveConfig("krb5.conf", k1, k2);
        Config.refresh();

        k1.writeKtab("ktab1");
        k2.writeKtab("ktab2");

        // A JAAS config file with 2 Krb5LoginModules, after commit, the
        // subject with have principals and keytabs from both sides
        System.setProperty("java.security.auth.login.config", "jaas.conf");
        File f = new File("jaas.conf");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write((
                "me {\n"
                + "  com.sun.security.auth.module.Krb5LoginModule required"
                + "    isInitiator=true principal=\"host/same.host@R1\""
                + "    useKeyTab=true keyTab=ktab1 storeKey=true;\n"
                + "  com.sun.security.auth.module.Krb5LoginModule required"
                + "    isInitiator=true principal=\"host/same.host@R2\""
                + "    useKeyTab=true keyTab=ktab2 storeKey=true;\n"
                + "};\n"
                ).getBytes());
        fos.close();

        /*
         * This server side context will be able to act as services in both
         * realms. Please note that we still don't support a single instance
         * of server to accept connections from two realms at the same time.
         * Therefore, we must call startAsServer in a given realm to start
         * working there. The same Subject never changes anyway.
         */
        Context s = Context.fromJAAS("me");

        // Default realm still R1
        s.startAsServer("host@same.host", GSSUtil.GSS_KRB5_MECH_OID);
        Context c1 = Context.fromUserPass("u1", "hello".toCharArray(), false);
        c1.startAsClient("host@same.host", GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c1, s);

        KDC.saveConfig("krb5.conf", k2, k1);
        Config.refresh();

        // Default realm now R2
        s.startAsServer("host@same.host", GSSUtil.GSS_KRB5_MECH_OID);
        Context c2 = Context.fromUserPass("u2", "hello".toCharArray(), false);
        c2.startAsClient("host@same.host", GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c2, s);
    }
}
