/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8181978
 * @summary Test automatic keystore type detection for a large PKCS12 keystore
 */

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;

public class ProbeLargeKeystore {

    private static final String DIR = System.getProperty("test.src", ".");
    private static final String CERT = DIR + "/trusted.pem";
    private static final String ALIAS = "test-entry-";
    private static final int COUNT = 100;
    private static final String KEYSTORE = "test-keystore.p12";
    private static final char[] PASSWORD = "passphrase".toCharArray();

    public static final void main(String[] args) throws Exception {

        // Create a large PKCS12 keystore

        new File(KEYSTORE).delete();
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, null);
        Certificate cert = loadCertificate(CERT);

        for (int i = 0; i < COUNT; i++) {
            keystore.setCertificateEntry(ALIAS + i, cert);
        }

        try (FileOutputStream out = new FileOutputStream(KEYSTORE)) {
            keystore.store(out, PASSWORD);
        }

        // Test the automatic keystore type detection mechanism for PKCS12

        KeyStore largeKeystore =
           KeyStore.getInstance(new File(KEYSTORE), PASSWORD);

        if (largeKeystore.size() != COUNT) {
            throw new Exception("Error detecting a large PKCS12 keystore");
        }

        new File(KEYSTORE).delete();
        System.out.println("OK");
    }

    private static final Certificate loadCertificate(String certFile)
            throws Exception {
        try (FileInputStream certStream = new FileInputStream(certFile)) {
             CertificateFactory factory =
                 CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(certStream);
        }
    }
}
