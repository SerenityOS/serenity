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
 * @bug 7067974 8194486
 * @summary multiple ETYPE-INFO-ENTRY with same etype and different salt
 * @library /test/lib
 * @compile -XDignore.symbol.file DupEtypes.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DupEtypes 1
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DupEtypes 2
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts DupEtypes 3
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DupEtypes 4
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DupEtypes 5
 */

import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;

public class DupEtypes {

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();

        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "default_keytab_name = " + OneKDC.KTAB,
                "allow_weak_crypto = true");
        Config.refresh();

        // Rewrite to include DES keys
        kdc.writeKtab(OneKDC.KTAB);

        // Different test cases, read KDC.processAsReq for details
        kdc.setOption(KDC.Option.DUP_ETYPE, Integer.parseInt(args[0]));

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        s.dispose();
        c.dispose();
    }
}
