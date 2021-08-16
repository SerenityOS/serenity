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
 * @bug 6853328 7172701 8194486
 * @summary Support OK-AS-DELEGATE flag
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts OkAsDelegateXRealm false
 *      KDC no OK-AS-DELEGATE, fail
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dtest.kdc.policy.ok-as-delegate OkAsDelegateXRealm true
 *      KDC set OK-AS-DELEGATE for all, succeed
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dtest.kdc.policy.ok-as-delegate=host/host.r3.local
 *      OkAsDelegateXRealm false
 *      KDC set OK-AS-DELEGATE for host/host.r3.local only, fail
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dtest.kdc.policy.ok-as-delegate=host/host.r3.local,krbtgt/R2,krbtgt/R3
 *      OkAsDelegateXRealm true
 *      KDC set OK-AS-DELEGATE for all three, succeed
 */
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.security.jgss.ExtendedGSSContext;
import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;

public class OkAsDelegateXRealm implements CallbackHandler {

    /**
     * @param args boolean if the program should succeed
     */
    public static void main(String[] args)
            throws Exception {

        // Create and start the KDCs. Here we have 3 realms: R1, R2 and R3.
        // R1 is trusted by R2, and R2 trusted by R3.
        KDC kdc1 = KDC.create("R1");
        kdc1.setOption(KDC.Option.OK_AS_DELEGATE,
                System.getProperty("test.kdc.policy.ok-as-delegate"));
        kdc1.addPrincipal("dummy", "bogus".toCharArray());
        kdc1.addPrincipalRandKey("krbtgt/R1");
        kdc1.addPrincipal("krbtgt/R2@R1", "r1->r2".toCharArray());

        KDC kdc2 = KDC.create("R2");
        kdc2.setOption(KDC.Option.OK_AS_DELEGATE,
                System.getProperty("test.kdc.policy.ok-as-delegate"));
        kdc2.addPrincipalRandKey("krbtgt/R2");
        kdc2.addPrincipal("krbtgt/R2@R1", "r1->r2".toCharArray());
        kdc2.addPrincipal("krbtgt/R3@R2", "r2->r3".toCharArray());

        KDC kdc3 = KDC.create("R3");
        kdc3.setOption(KDC.Option.OK_AS_DELEGATE,
                System.getProperty("test.kdc.policy.ok-as-delegate"));
        kdc3.addPrincipalRandKey("krbtgt/R3");
        kdc3.addPrincipal("krbtgt/R3@R2", "r2->r3".toCharArray());
        kdc3.addPrincipalRandKey("host/host.r3.local");

        KDC.saveConfig("krb5-localkdc.conf", kdc1, kdc2, kdc3,
                "forwardable=true",
                "[capaths]",
                "R1 = {",
                "    R2 = .",
                "    R3 = R2",
                "}",
                "[domain_realm]",
                ".r3.local=R3"
                );

        System.setProperty("java.security.krb5.conf", "krb5-localkdc.conf");
        kdc3.writeKtab("localkdc.ktab");

        FileOutputStream fos = new FileOutputStream("jaas-localkdc.conf");

        // Defines the client and server on R1 and R3 respectively.
        fos.write(("com.sun.security.jgss.krb5.initiate {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule\n" +
                "    required\n" +
                "    principal=dummy\n" +
                "    doNotPrompt=false\n" +
                "    useTicketCache=false\n" +
                "    ;\n};\n" +
                "com.sun.security.jgss.krb5.accept {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"host/host.r3.local@R3\"\n" +
                "    useKeyTab=true\n" +
                "    keyTab=localkdc.ktab\n" +
                "    isInitiator=false\n" +
                "    storeKey=true;\n};\n" +
                "\n").getBytes());
        fos.close();

        Security.setProperty("auth.login.defaultCallbackHandler",
                "OkAsDelegateXRealm");

        System.setProperty("java.security.auth.login.config", "jaas-localkdc.conf");

        Config.refresh();

        Context c = Context.fromJAAS("com.sun.security.jgss.krb5.initiate");
        Context s = Context.fromJAAS("com.sun.security.jgss.krb5.accept");

        // Test twice. The frist time the whole cross realm process is tried,
        // the second time the cached service ticket is used. This is to make sure
        // the behaviors are the same, especailly for the case when one of the
        // cross-realm TGTs does not have OK-AS-DELEGATE on.

        for (int i=0; i<2; i++) {
            c.startAsClient("host@host.r3.local", GSSUtil.GSS_KRB5_MECH_OID);
            s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
            ((ExtendedGSSContext)c.x()).requestDelegPolicy(true);

            Context.handshake(c, s);
            boolean succeed = true;
            try {
                s.x().getDelegCred();
            } catch (GSSException gsse) {
                succeed = false;
            }
            if (succeed != Boolean.parseBoolean(args[0])) {
                throw new Exception("Test fail at round #" + i);
            }
        }
    }

    @Override
    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName("dummy");
            }
            if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword("bogus".toCharArray());
            }
        }
    }
}

