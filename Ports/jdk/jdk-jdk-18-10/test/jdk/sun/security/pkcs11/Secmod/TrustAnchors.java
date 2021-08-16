/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6298106 6275523 6420252 8059627
 * @summary make sure we can access the NSS trust anchor module
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TrustAnchors
 * @run main/othervm -Djava.security.manager=allow TrustAnchors sm policy
 */

import java.io.File;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

public class TrustAnchors extends SecmodTest {

    public static void main(String[] args) throws Exception {
        if (initSecmod() == false) {
            return;
        }

        // our secmod.db file says nssckbi.*so*, so NSS does not find the
        // *DLL* on Windows nor the *DYLIB* on Mac OSX.
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("win") || osName.startsWith("mac")) {
            System.out.println("Test currently does not work on " + osName +
                ", skipping");
            return;
        }

        String configName = BASE + SEP + "nsstrust.cfg";
        Provider p = getSunPKCS11(configName);

        System.out.println(p);
        Security.addProvider(p);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
            System.setSecurityManager(new SecurityManager());
        }

        KeyStore ks = KeyStore.getInstance("PKCS11", p);
        ks.load(null, null);
        Collection<String> aliases = new TreeSet<>(Collections.list(ks.aliases()));
        System.out.println("entries: " + aliases.size());
        System.out.println(aliases);

        for (String alias : aliases) {
            if (ks.isCertificateEntry(alias) == false) {
                throw new Exception("not trusted: " + alias);
            }
            X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
            // verify self-signed certs
            if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
            System.out.print(".");
                cert.verify(cert.getPublicKey());
            } else {
                System.out.print("-");
            }
        }

        System.out.println();
        System.out.println("OK");
    }

}
