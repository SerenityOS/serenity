/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6893158 6907425 7197159 8194486
 * @summary AP_REQ check should use key version number
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts MoreKvno
 */

import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.Krb5;

public class MoreKvno {

    static PrincipalName p;
    public static void main(String[] args)
            throws Exception {

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();

        // Rewrite keytab, 3 set of keys with different kvno
        KeyTab ktab = KeyTab.create(OneKDC.KTAB);
        p = new PrincipalName(
            OneKDC.SERVER+"@"+OneKDC.REALM, PrincipalName.KRB_NT_SRV_HST);
        ktab.addEntry(p, "pass1".toCharArray(), 1, true);
        ktab.addEntry(p, "pass3".toCharArray(), 3, true);
        ktab.addEntry(p, "pass2".toCharArray(), 2, true);
        ktab.save();

        char[] pass = "pass2".toCharArray();
        kdc.addPrincipal(OneKDC.SERVER, pass);
        go(OneKDC.SERVER, "com.sun.security.jgss.krb5.accept", pass);

        pass = "pass3".toCharArray();
        kdc.addPrincipal(OneKDC.SERVER, pass);
        // "server" initiate also, check pass2 is used at authentication
        go(OneKDC.SERVER, "server", pass);

        try {
            pass = "pass4".toCharArray();
            kdc.addPrincipal(OneKDC.SERVER, pass);
            go(OneKDC.SERVER, "com.sun.security.jgss.krb5.accept", pass);
            throw new Exception("This test should fail");
        } catch (GSSException gsse) {
            // Since 7197159, different kvno is accepted, this return code
            // will never be thrown out again.
            //KrbException ke = (KrbException)gsse.getCause();
            //if (ke.returnCode() != Krb5.KRB_AP_ERR_BADKEYVER) {
            //    throw new Exception("Not expected failure code: " +
            //            ke.returnCode());
            //}
        }
    }

    static void go(String server, String entry, char[] pass) throws Exception {
        Context c, s;

        // Part 1: Test keytab
        c = Context.fromUserPass("dummy", "bogus".toCharArray(), false);
        s = Context.fromJAAS(entry);

        c.startAsClient(server, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        s.dispose();
        c.dispose();

        // Part 2: Test username/password pair
        c = Context.fromUserPass("dummy", "bogus".toCharArray(), false);
        s = Context.fromUserPass(p.getNameString(), pass, true);

        c.startAsClient(server, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        s.dispose();
        c.dispose();
    }
}
