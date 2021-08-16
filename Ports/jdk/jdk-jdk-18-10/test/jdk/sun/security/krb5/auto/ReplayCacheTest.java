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
 * @bug 7118809 8001326 8194486
 * @summary rcache deadlock
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts ReplayCacheTest jvm
 * @run main/othervm -Djdk.net.hosts.file=TestHosts ReplayCacheTest dfl
 */

import java.io.File;
import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.KrbException;
import sun.security.krb5.internal.Krb5;

public class ReplayCacheTest {

    public static void main(String[] args)
            throws Exception {

        new OneKDC(null);

        if (args[0].equals("dfl")) {
            // Store file in scratch directory
            args[0] = "dfl:" + System.getProperty("user.dir") + File.separator;
            System.setProperty("sun.security.krb5.rcache", args[0]);
        }

        Context c, s;
        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        s = Context.fromUserKtab(OneKDC.SERVER, OneKDC.KTAB, true);

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        byte[] first = c.take(new byte[0]);
        c.take(s.take(first));

        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        try {
            s.take(first);  // Replay the last apreq sent
            throw new Exception("This method should fail");
        } catch (GSSException gsse) {
            gsse.printStackTrace();
            KrbException ke = (KrbException)gsse.getCause();
            if (ke.returnCode() != Krb5.KRB_AP_ERR_REPEAT) {
                throw gsse;
            }
        }
    }
}
