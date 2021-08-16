/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8011402 8211969 8237995
 * @summary Move blacklisting certificate logic from hard code to data
 * @modules java.base/sun.security.util
 */

import sun.security.util.UntrustedCertificates;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;

public class CheckBlockedCerts {
    public static void main(String[] args) throws Exception {

        String home = System.getProperty("java.home");
        boolean failed = false;

        // Root CAs should always be trusted
        File file = new File(home, "lib/security/cacerts");
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(file)) {
            ks.load(fis, null);
        }
        System.out.println("Check for cacerts: " + ks.size());
        for (String alias: Collections.list(ks.aliases())) {
            X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
            if (UntrustedCertificates.isUntrusted(cert)) {
                System.out.print(alias + " is untrusted");
                failed = true;
            }
        }

        // All certs in the pem files
        Set<Certificate> blocked = new HashSet<>();

        // Assumes the full src is available
        File blockedCertsFile = new File(System.getProperty("test.src"),
                "../../../../../make/data/blockedcertsconverter/blocked.certs.pem");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(blockedCertsFile)) {
            Collection<? extends Certificate> certs
                    = cf.generateCertificates(fis);
            System.out.println(certs.size());
            for (Certificate c: certs) {
                blocked.add(c);
                X509Certificate cert = ((X509Certificate)c);
                if (!UntrustedCertificates.isUntrusted(cert)) {
                    System.out.println(cert.getSubjectX500Principal() +
                            " is trusted");
                    failed = true;
                }
            }
        }

        // Check the blocked.certs file itself
        file = new File(home, "lib/security/blocked.certs");
        System.out.print("Check for " + file + ": ");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)))) {
            int acount = 0;
            int ccount = 0;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.startsWith("Algorithm")) {
                    acount++;
                } else if (!line.isEmpty() && !line.startsWith("#")) {
                    ccount++;
                }
            }
            System.out.println(acount + " algs, " + ccount + " certs" );
            if (acount != 1) {
                System.out.println("There are " + acount + " algorithms");
                failed = true;
            }
            // There are two unique fingerprints for each RSA certificate
            if (ccount != blocked.size() * 2
                    && !blocked.isEmpty()) {
                System.out.println("Wrong blocked.certs size: "
                        + ccount + " fingerprints, "
                        + blocked.size() + " certs");
                failed = true;
            }
        }

        if (failed) {
            throw new Exception("Failed");
        }
    }
}
