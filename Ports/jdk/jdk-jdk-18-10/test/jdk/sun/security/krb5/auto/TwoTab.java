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
 * @bug 7152176 8194486
 * @summary More krb5 tests
 * @library /test/lib
 * @compile -XDignore.symbol.file TwoTab.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts TwoTab
 */

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.Security;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ktab.KeyTab;

// Two services using their own keytab.
public class TwoTab {

    public static void main(String[] args) throws Exception {

        KDC k = new OneKDC(null);

        // Write JAAS conf, two service using different keytabs
        System.setProperty("java.security.auth.login.config", OneKDC.JAAS_CONF);
        File f = new File(OneKDC.JAAS_CONF);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write((
                "server {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + OneKDC.SERVER + "\"\n" +
                "    useKeyTab=true\n" +
                "    keyTab=server.keytab\n" +
                "    storeKey=true;\n};\n" +
                "server2 {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + OneKDC.BACKEND + "\"\n" +
                "    useKeyTab=true\n" +
                "    keyTab=backend.keytab\n" +
                "    storeKey=true;\n};\n"
                ).getBytes());
        }
        f.deleteOnExit();

        k.writeKtab("server.keytab", false, "server/host.rabbit.hole@RABBIT.HOLE");
        k.writeKtab("backend.keytab", false, "backend/host.rabbit.hole@RABBIT.HOLE");

        Context c, s, s2;
        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        s = Context.fromJAAS("server");
        s2 = Context.fromJAAS("server2");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        s.dispose();
        c.dispose();

        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        c.startAsClient(OneKDC.BACKEND, GSSUtil.GSS_KRB5_MECH_OID);
        s2.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s2);

        Context.transmit("i say high --", c, s2);
        Context.transmit("   you say low", s2, c);

        s2.dispose();
        c.dispose();
    }
}
