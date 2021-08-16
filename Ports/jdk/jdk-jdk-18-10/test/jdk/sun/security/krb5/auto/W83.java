/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6932525 6951366 6959292 8194486
 * @summary kerberos login failure on win2008 with AD set to win2000 compat mode
 * and cannot login if session key and preauth does not use the same etype
 * @library /test/lib
 * @compile -XDignore.symbol.file W83.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -D6932525 -Djdk.net.hosts.file=TestHosts W83
 * @run main/othervm -D6959292 -Djdk.net.hosts.file=TestHosts W83
 */
import com.sun.security.auth.module.Krb5LoginModule;
import sun.security.krb5.Config;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.crypto.EType;
import sun.security.krb5.internal.ktab.KeyTab;

public class W83 {
    public static void main(String[] args) throws Exception {

        W83 x = new W83();

        // Cannot use OneKDC. kinit command cannot resolve
        // hostname kdc.rabbit.hole
        KDC kdc = new KDC(OneKDC.REALM, "127.0.0.1", 0, true);
        kdc.addPrincipal(OneKDC.USER, OneKDC.PASS);
        kdc.addPrincipalRandKey("krbtgt/" + OneKDC.REALM);
        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "allow_weak_crypto = true");
        System.setProperty("java.security.krb5.conf", OneKDC.KRB5_CONF);
        Config.refresh();

        kdc.writeKtab(OneKDC.KTAB);

        KeyTab ktab = KeyTab.getInstance(OneKDC.KTAB);
        for (int etype: EType.getBuiltInDefaults()) {
            if (etype != EncryptedData.ETYPE_ARCFOUR_HMAC) {
                ktab.deleteEntries(new PrincipalName(OneKDC.USER), etype, -1);
            }
        }
        ktab.save();

        if (System.getProperty("6932525") != null) {
            // For 6932525 and 6951366, make sure the etypes sent in 2nd AS-REQ
            // is not restricted to that of preauth
            kdc.setOption(KDC.Option.ONLY_RC4_TGT, true);
        }
        if (System.getProperty("6959292") != null) {
            // For 6959292, make sure that when etype for enc-part in 2nd AS-REQ
            // is different from that of preauth, client can still decrypt it
            kdc.setOption(KDC.Option.RC4_FIRST_PREAUTH, true);
        }
        x.go();
    }

    void go() throws Exception {
        Krb5LoginModule krb5 = new Krb5LoginModule();
        StringBuffer error = new StringBuffer();
        try {
            Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        } catch (Exception e) {
            e.printStackTrace();
            error.append("Krb5LoginModule password login error\n");
        }
        try {
            Context.fromUserKtab(OneKDC.USER, OneKDC.KTAB, false);
        } catch (Exception e) {
            e.printStackTrace();
            error.append("Krb5LoginModule keytab login error\n");
        }
        try {
            Class.forName("sun.security.krb5.internal.tools.Kinit");
            String cmd = System.getProperty("java.home") +
                    System.getProperty("file.separator") +
                    "bin" +
                    System.getProperty("file.separator") +
                    "kinit";

            int p = execute(
                cmd,
                "-J-Djava.security.krb5.conf=" + OneKDC.KRB5_CONF,
                "-c", "cache1",
                OneKDC.USER,
                new String(OneKDC.PASS));
            if (p != 0) {
                error.append("kinit password login error\n");
            }
            p = execute(
                cmd,
                "-J-Djava.security.krb5.conf=" + OneKDC.KRB5_CONF,
                "-c", "cache2",
                "-k", "-t", OneKDC.KTAB,
                OneKDC.USER);
            if (p != 0) {
                error.append("kinit keytab login error\n");
            }
        } catch (ClassNotFoundException cnfe) {
            System.out.println("No kinit, test ignored.");
            // Ignore, not on windows
        }
        if (error.length() != 0) {
            throw new Exception(error.toString());
        }
    }

    private static int execute(String... args) throws Exception {
        for (String arg: args) {
            System.out.printf("%s ", arg);
        }
        System.out.println();
        Process p = Runtime.getRuntime().exec(args);
        return p.waitFor();
    }
}
