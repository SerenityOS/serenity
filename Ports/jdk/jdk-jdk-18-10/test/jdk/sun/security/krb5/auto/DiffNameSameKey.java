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
 * @bug 8005447 8194486
 * @summary default principal can act as anyone
 * @library /test/lib
 * @compile -XDignore.symbol.file DiffNameSameKey.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts DiffNameSameKey a
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DiffNameSameKey b
 */

import sun.security.jgss.GSSUtil;
import sun.security.krb5.PrincipalName;

/**
 * This test confirms the compatibility codes described in
 * ServiceCreds.getEKeys(). If the acceptor starts as x.us.oracle.com
 * but client requests for x.us, as long as the KDC supports both names
 * and the keys are the same, the auth should succeed.
 */
public class DiffNameSameKey {

    static final String SERVER2 = "x" + OneKDC.SERVER;

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new KDC2();
        kdc.addPrincipal(SERVER2, "samepass".toCharArray());
        kdc.addPrincipal(OneKDC.SERVER, "samepass".toCharArray());
        kdc.writeJAASConf();
        kdc.writeKtab(OneKDC.KTAB);

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        switch (args[0]) {
            case "a":   // If server starts as another service, should fail
                c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_SPNEGO_MECH_OID);
                s.startAsServer(SERVER2.replace('/', '@'),
                        GSSUtil.GSS_SPNEGO_MECH_OID);
                break;
            case "b":   // If client requests another server with the same keys,
                        // succeed to be compatible
                c.startAsClient(SERVER2, GSSUtil.GSS_SPNEGO_MECH_OID);
                s.startAsServer(OneKDC.SERVER.replace('/', '@'),
                        GSSUtil.GSS_SPNEGO_MECH_OID);
                break;
        }

        Context.handshake(c, s);

        s.dispose();
        c.dispose();
    }

    /**
     * This KDC returns the same salt for all principals. This means same
     * passwords generate same keys.
     */
    static class KDC2 extends OneKDC {
        KDC2() throws Exception {
            super(null);
        }
        @Override
        public String getSalt(PrincipalName pn) {
            return "SAME";
        }
    }
}
