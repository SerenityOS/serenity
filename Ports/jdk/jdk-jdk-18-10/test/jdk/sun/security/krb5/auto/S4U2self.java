/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6355584 8194486
 * @summary Introduce constrained Kerberos delegation
 * @library /test/lib
 * @compile -XDignore.symbol.file S4U2self.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 0
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 1
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 2
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 3
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 4
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self krb5 5
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dsun.security.krb5.debug=false S4U2self spnego
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSUtil;

public class S4U2self {

    public static void main(String[] args) throws Exception {
        // Test case, different policy settings in KDC:
        //                   |     ALLOW_S4U2SELF on
        //                   |   USER    USER2    none
        // ALLOW_S4U2PORXY   |-------------------------
        // USER to BACKEND   |   0       1        2
        // USER2 to BACKEND  |   3
        // USER to SERVER    |   4
        //      none         |   5
        //
        // 0 should succeed, all other fail
        int test = 0;
        Oid mech;
        if (args[0].equals("spnego")) {
            mech = GSSUtil.GSS_SPNEGO_MECH_OID;
        } else if (args[0].contains("krb5")) {
            mech = GSSUtil.GSS_KRB5_MECH_OID;
            test = Integer.parseInt(args[1]);
        } else {
            throw new Exception("Unknown mech");
        }

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();

        switch (test) {
            case 1:
                kdc.setOption(KDC.Option.ALLOW_S4U2SELF, Arrays.asList(
                        new String[]{OneKDC.USER2 + "@" + OneKDC.REALM}));
                break;
            case 2:
                // No S4U2self
                break;
            default:
                kdc.setOption(KDC.Option.ALLOW_S4U2SELF, Arrays.asList(
                        new String[]{OneKDC.USER + "@" + OneKDC.REALM}));
                break;
        }

        Map<String,List<String>> map = new HashMap<>();
        switch (test) {
            case 3:
                map.put(OneKDC.USER2 + "@" + OneKDC.REALM, Arrays.asList(
                        new String[]{OneKDC.BACKEND + "@" + OneKDC.REALM}));
                kdc.setOption(KDC.Option.ALLOW_S4U2PROXY, map);
                break;
            case 4:
                map.put(OneKDC.USER + "@" + OneKDC.REALM, Arrays.asList(
                        new String[]{OneKDC.SERVER + "@" + OneKDC.REALM}));
                kdc.setOption(KDC.Option.ALLOW_S4U2PROXY, map);
                break;
            case 5:
                // No S4U2proxy set
                break;
            default:
                map.put(OneKDC.USER + "@" + OneKDC.REALM, Arrays.asList(
                        new String[]{OneKDC.BACKEND + "@" + OneKDC.REALM}));
                kdc.setOption(KDC.Option.ALLOW_S4U2PROXY, map);
                break;
        }

        Context c, s;
        c = Context.fromJAAS("client");

        c = c.impersonate(OneKDC.USER2);
        c.status();

        c.startAsClient(OneKDC.BACKEND, mech);

        s = Context.fromJAAS("backend");
        s.startAsServer(mech);

        Context.handshake(c, s);

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        c.status();
        s.status();

        String n1 = c.x().getSrcName().toString().split("@")[0];
        String n2 = s.x().getSrcName().toString().split("@")[0];
        if (!n1.equals(OneKDC.USER2) || !n2.equals(OneKDC.USER2)) {
            throw new Exception("Impersonate failed");
        }

        s.dispose();
        c.dispose();
    }
}
