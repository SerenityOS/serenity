/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6298106
 * @summary make sure we can add a trusted cert to the NSS KeyStore module
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm AddTrustedCert
 * @run main/othervm -Djava.security.manager=allow AddTrustedCert sm policy
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

public class AddTrustedCert extends SecmodTest {

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
        }

        if (initSecmod() == false) {
            return;
        }

        X509Certificate cert;
        try (InputStream in = new FileInputStream(BASE + SEP + "anchor.cer")) {
            CertificateFactory factory =
                    CertificateFactory.getInstance("X.509");
            cert = (X509Certificate)factory.generateCertificate(in);
        }

        String configName = BASE + SEP + "nss.cfg";
        Provider p = getSunPKCS11(configName);

        if (improperNSSVersion(p)) {
            System.out.println(
                    "Skip test due to improper NSS version in [3.28, 3.35). "
                    + "See JDK-8180837 for more detatils.");
            return;
        }

        System.out.println(p);
        Security.addProvider(p);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
            System.setSecurityManager(new SecurityManager());
        }

        KeyStore ks = KeyStore.getInstance(PKCS11, p);
        ks.load(null, password);
        Collection<String> aliases = new TreeSet<>(Collections.list(
                ks.aliases()));
        System.out.println("entries: " + aliases.size());
        System.out.println(aliases);
        int size1 = aliases.size();

        String alias = "anchor";
        if (ks.containsAlias(alias)) {
            throw new Exception("Alias exists: " + alias);
        }

        ks.setCertificateEntry(alias, cert);
        KeyStore.Entry first = ks.getEntry(alias, null);
        System.out.println("first entry = " + first);
        if (!ks.entryInstanceOf(alias, TrustedCertificateEntry.class)) {
            throw new Exception("Unexpected first entry type: " + first);
        }

        ks.setCertificateEntry(alias, cert);
        KeyStore.Entry second = ks.getEntry(alias, null);
        System.out.println("second entry = " + second);
        if (!ks.entryInstanceOf(alias, TrustedCertificateEntry.class)) {
            throw new Exception("Unexpected second entry type: "
                    + second);
        }

        aliases = new TreeSet<>(Collections.list(ks.aliases()));
        System.out.println("entries: " + aliases.size());
        System.out.println(aliases);
        int size2 = aliases.size();

        if ((size2 != size1 + 1) || (aliases.contains(alias) == false)) {
            throw new Exception("Trusted cert not added");
        }
        X509Certificate cert2 = (X509Certificate)ks.getCertificate(alias);
        if (cert.equals(cert2) == false) {
            throw new Exception("KeyStore returned incorrect certificate");
        }

        ks.deleteEntry(alias);
        if (ks.containsAlias(alias)) {
            throw new Exception("Alias still exists: " + alias);
        }

        System.out.println("OK");
    }

    private static boolean improperNSSVersion(Provider p) {
        double nssVersion = getNSSVersion();
        if (p.getName().equalsIgnoreCase("SunPKCS11-NSSKeyStore")
                && nssVersion >= 3.28 && nssVersion < 3.35) {
            return true;
        }

        return false;
    }
}
