/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8001326 8194486
 * @summary the replaycache type none cannot stop an authenticator replay,
 * but it can stop a message replay when s.s.k.acceptor.subkey is true.
 * You should not really use none in production environment. This test merely
 * shows there can be other protections when replay cache is not working fine.
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts NoneReplayCacheTest
 */

import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;

public class NoneReplayCacheTest {

    public static void main(String[] args)
            throws Exception {

        new OneKDC(null);

        System.setProperty("sun.security.krb5.rcache", "none");
        System.setProperty("sun.security.krb5.acceptor.subkey", "true");

        Context c, s;
        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        s = Context.fromUserKtab(OneKDC.SERVER, OneKDC.KTAB, true);

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        byte[] first = c.take(new byte[0]);

        c.take(s.take(first));

        byte[] msg = c.wrap("hello".getBytes(), true);
        s.unwrap(msg, true);

        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        s.take(first);  // apreq replay not detectable
        try {
            s.unwrap(msg, true);    // msg replay detectable
            throw new Exception("This method should fail");
        } catch (GSSException gsse) {
            gsse.printStackTrace();
        }
    }
}
