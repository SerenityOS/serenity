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
 * @bug 6894072 8004488 8194486
 * @summary always refresh keytab
 * @library /test/lib
 * @compile -XDignore.symbol.file KeyTabCompat.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts KeyTabCompat
 */

import javax.security.auth.kerberos.KerberosKey;
import sun.security.jgss.GSSUtil;

/*
 * There are 2 compat issues to check:
 *
 * 1. If there is only KerberosKeys in private credential set and no
 *    KerberosPrincipal. JAAS login should go on.
 * 2. If KeyTab is used, user won't get KerberosKeys from
 *    private credentials set.
 */
public class KeyTabCompat {

    public static void main(String[] args)
            throws Exception {
        OneKDC kdc = new OneKDC("aes128-cts");
        kdc.writeJAASConf();
        kdc.addPrincipal(OneKDC.SERVER, "pass1".toCharArray());
        kdc.writeKtab(OneKDC.KTAB);

        Context c, s;

        // Part 1
        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        s = Context.fromUserPass(OneKDC.USER2, OneKDC.PASS2, true);

        s.s().getPrincipals().clear();

        c.startAsClient(OneKDC.USER2, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        // Part 2
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        s.status();

        if (s.s().getPrivateCredentials(KerberosKey.class).size() != 0) {
            throw new Exception("There should be no KerberosKey");
        }
    }
}
