/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131051 8194486 8187218
 * @summary KDC might issue a renewable ticket even if not requested
 * @library /test/lib
 * @compile -XDignore.symbol.file LongLife.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts LongLife
 */

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import sun.security.krb5.Config;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import java.security.PrivilegedExceptionAction;

public class LongLife {

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new OneKDC(null).writeJAASConf();

        test(kdc, "10h", false, 36000, false);
        test(kdc, "2d", false, KDC.DEFAULT_LIFETIME, true);
        test(kdc, "2d", true, 2 * 24 * 3600, false);

        // 8187218: getRemainingLifetime() is negative if lifetime
        // is longer than 30 days.
        test(kdc, "30d", true, 30 * 24 * 3600, false);
    }

    static void test(
            KDC kdc,
            String ticketLifetime,
            boolean forceTill, // if true, KDC will not try RENEWABLE
            int expectedLifeTime,
            boolean expectedRenewable) throws Exception {

        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "ticket_lifetime = " + ticketLifetime);
        Config.refresh();

        if (forceTill) {
            System.setProperty("test.kdc.force.till", "");
        } else {
            System.clearProperty("test.kdc.force.till");
        }

        Context c = Context.fromJAAS("client");

        GSSCredential cred = Subject.doAs(c.s(),
                (PrivilegedExceptionAction<GSSCredential>)
                ()-> {
                    GSSManager m = GSSManager.getInstance();
                    return m.createCredential(GSSCredential.INITIATE_ONLY);
                });

        KerberosTicket tgt = c.s().getPrivateCredentials(KerberosTicket.class)
                .iterator().next();
        System.out.println(tgt);

        int actualLifeTime = cred.getRemainingLifetime();
        if (actualLifeTime < expectedLifeTime - 60
                || actualLifeTime > expectedLifeTime + 60) {
            throw new Exception("actualLifeTime is " + actualLifeTime);
        }

        if (tgt.isRenewable() != expectedRenewable) {
            throw new Exception("TGT's RENEWABLE flag is " + tgt.isRenewable());
        }
    }
}
