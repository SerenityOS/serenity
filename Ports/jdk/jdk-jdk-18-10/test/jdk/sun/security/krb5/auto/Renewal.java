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
 * @bug 8044500 8194486
 * @summary Add kinit options and krb5.conf flags that allow users to
 *          obtain renewable tickets and specify ticket lifetimes
 * @library /test/lib
 * @compile -XDignore.symbol.file Renewal.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts Renewal
 */

import jdk.test.lib.process.Proc;
import sun.security.krb5.Config;
import sun.security.krb5.internal.ccache.Credentials;
import sun.security.krb5.internal.ccache.FileCredentialsCache;

import javax.security.auth.kerberos.KerberosTicket;
import java.util.Date;
import java.util.Set;

// The basic krb5 test skeleton you can copy from
public class Renewal {

    static OneKDC kdc;
    static String clazz = "sun.security.krb5.internal.tools.Kinit";
    static String hostsFileName = null;

    public static void main(String[] args) throws Exception {
        hostsFileName = System.getProperty("test.src", ".") + "/TestHosts";
        System.setProperty("jdk.net.hosts.file", hostsFileName);

        kdc = new OneKDC(null);
        kdc.writeJAASConf();
        kdc.setOption(KDC.Option.PREAUTH_REQUIRED, false);

        checkLogin(null, null, KDC.DEFAULT_LIFETIME, -1);
        checkLogin("1h", null, 3600, -1);
        checkLogin(null, "2d", KDC.DEFAULT_LIFETIME, 86400*2);
        checkLogin("1h", "10h", 3600, 36000);
        // When rtime is before till, use till as rtime
        checkLogin("10h", "1h", 36000, 36000);

        try {
            Class.forName(clazz);
        } catch (ClassNotFoundException cnfe) {
            return;
        }

        checkKinit(null, null, null, null, KDC.DEFAULT_LIFETIME, -1);
        checkKinit("1h", "10h", null, null, 3600, 36000);
        checkKinit(null, null, "30m", "5h", 1800, 18000);
        checkKinit("1h", "10h", "30m", "5h", 1800, 18000);

        checkKinitRenew();
    }

    static int count = 0;

    static void checkKinit(
            String s1,      // ticket_lifetime in krb5.conf, null if none
            String s2,      // renew_lifetime in krb5.conf, null if none
            String c1,      // -l on kinit, null if none
            String c2,      // -r on kinit, null if none
            int t1, int t2  // expected lifetimes, -1 of unexpected
                ) throws Exception {
        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                s1 != null ? ("ticket_lifetime = " + s1) : "",
                s2 != null ? ("renew_lifetime = " + s2) : "");
        Proc p = Proc.create(clazz);
        if (c1 != null) {
            p.args("-l", c1);
        }
        if (c2 != null) {
            p.args("-r", c2);
        }
        count++;
        p.args(OneKDC.USER, new String(OneKDC.PASS))
                .inheritIO()
                .prop("jdk.net.hosts.file", hostsFileName)
                .prop("java.security.krb5.conf", OneKDC.KRB5_CONF)
                .env("KRB5CCNAME", "ccache" + count)
                .start();
        if (p.waitFor() != 0) {
            throw new Exception();
        }
        FileCredentialsCache fcc =
                FileCredentialsCache.acquireInstance(null, "ccache" + count);
        Credentials cred = fcc.getDefaultCreds();
        checkRough(cred.getEndTime().toDate(), t1);
        if (cred.getRenewTill() == null) {
            checkRough(null, t2);
        } else {
            checkRough(cred.getRenewTill().toDate(), t2);
        }
    }

    static void checkKinitRenew() throws Exception {

        Proc p = Proc.create(clazz)
                .args("-R")
                .inheritIO()
                .prop("jdk.net.hosts.file", hostsFileName)
                .prop("java.security.krb5.conf", OneKDC.KRB5_CONF)
                .env("KRB5CCNAME", "ccache" + count)
                .start();
        if (p.waitFor() != 0) {
            throw new Exception();
        }
    }

    static void checkLogin(
            String s1,      // ticket_lifetime in krb5.conf, null if none
            String s2,      // renew_lifetime in krb5.conf, null if none
            int t1, int t2  // expected lifetimes, -1 of unexpected
                ) throws Exception {
        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                s1 != null ? ("ticket_lifetime = " + s1) : "",
                s2 != null ? ("renew_lifetime = " + s2) : "");
        Config.refresh();

        Context c;
        c = Context.fromJAAS("client");

        Set<KerberosTicket> tickets =
                c.s().getPrivateCredentials(KerberosTicket.class);
        if (tickets.size() != 1) {
            throw new Exception();
        }
        KerberosTicket ticket = tickets.iterator().next();

        checkRough(ticket.getEndTime(), t1);
        checkRough(ticket.getRenewTill(), t2);
    }

    static void checkRough(Date t, int duration) throws Exception {
        Date now = new Date();
        if (t == null && duration == -1) {
            return;
        }
        long change = (t.getTime() - System.currentTimeMillis()) / 1000;
        //accounting the delay factor in processing the instructions on host mc
        if (change > duration + 40 || change < duration - 40) {
            throw new Exception("Timestamp is " + t + ", actual difference "
                    + change + " is not " + duration);
        }
    }
}
