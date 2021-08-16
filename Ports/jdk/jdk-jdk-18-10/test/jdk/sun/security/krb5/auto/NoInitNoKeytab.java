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
 * @bug 7089889 8194486
 * @summary Krb5LoginModule.login() throws an exception if used without a keytab
 * @library /test/lib
 * @compile -XDignore.symbol.file NoInitNoKeytab.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts NoInitNoKeytab
 */

import java.io.FileOutputStream;
import sun.security.jgss.GSSUtil;

// The basic krb5 test skeleton you can copy from
public class NoInitNoKeytab {

    public static void main(String[] args) throws Exception {

        new OneKDC(null).writeJAASConf();
        try (FileOutputStream fos =
                new FileOutputStream(OneKDC.JAAS_CONF, true)) {
            fos.write((
                "noinit {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + OneKDC.USER + "\"\n" +
                "    useKeyTab=false\n" +
                "    isInitiator=false\n" +
                "    storeKey=true;\n};\n").getBytes());
        }
        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("noinit");

        c.startAsClient(OneKDC.USER, GSSUtil.GSS_SPNEGO_MECH_OID);
        s.startAsServer(GSSUtil.GSS_SPNEGO_MECH_OID);

        Context.handshake(c, s);

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        s.dispose();
        c.dispose();
    }
}
