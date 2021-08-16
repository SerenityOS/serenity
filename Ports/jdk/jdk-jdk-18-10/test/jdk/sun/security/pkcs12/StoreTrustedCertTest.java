/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005408
 * @summary KeyStore API enhancements
 */

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.security.cert.Certificate;
import javax.crypto.*;
import javax.crypto.spec.*;

// Store a trusted certificate in a keystore and retrieve it again.

public class StoreTrustedCertTest {
    private final static String DIR = System.getProperty("test.src", ".");
    private static final char[] PASSWORD = "passphrase".toCharArray();
    private static final String KEYSTORE = "truststore.p12";
    private static final String CERT = DIR + "/trusted.pem";
    private static final String ALIAS = "my trustedcert";
    private static final String ALIAS2 = "my trustedcert with attributes";

    public static void main(String[] args) throws Exception {

        new File(KEYSTORE).delete();

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, null);

        Certificate cert = loadCertificate(CERT);
        Set<KeyStore.Entry.Attribute> attributes = new HashSet<>();
        attributes.add(new PKCS12Attribute("1.3.5.7.9", "that's odd"));
        attributes.add(new PKCS12Attribute("2.4.6.8.10", "that's even"));

        // Set trusted certificate entry
        keystore.setEntry(ALIAS,
            new KeyStore.TrustedCertificateEntry(cert), null);

        // Set trusted certificate entry with attributes
        keystore.setEntry(ALIAS2,
            new KeyStore.TrustedCertificateEntry(cert, attributes), null);

        try (FileOutputStream outStream = new FileOutputStream(KEYSTORE)) {
            System.out.println("Storing keystore to: " + KEYSTORE);
            keystore.store(outStream, PASSWORD);
        }

        try (FileInputStream inStream = new FileInputStream(KEYSTORE)) {
            System.out.println("Loading keystore from: " + KEYSTORE);
            keystore.load(inStream, PASSWORD);
            System.out.println("Loaded keystore with " + keystore.size() +
                " entries");
        }

        KeyStore.Entry entry = keystore.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            System.out.println("Retrieved trusted certificate entry: " + entry);
        } else {
            throw new Exception("Not a trusted certificate entry");
        }
        System.out.println();

        entry = keystore.getEntry(ALIAS2, null);
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            KeyStore.TrustedCertificateEntry trustedEntry =
                (KeyStore.TrustedCertificateEntry) entry;
            Set<KeyStore.Entry.Attribute> entryAttributes =
                trustedEntry.getAttributes();

            if (entryAttributes.containsAll(attributes)) {
                System.out.println("Retrieved trusted certificate entry " +
                    "with attributes: " + entry);
            } else {
                throw new Exception("Failed to retrieve entry attributes");
            }
        } else {
            throw new Exception("Not a trusted certificate entry");
        }
    }

    private static Certificate loadCertificate(String certFile)
        throws Exception {
        X509Certificate cert = null;
        try (FileInputStream certStream = new FileInputStream(certFile)) {
            CertificateFactory factory =
                CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(certStream);
        }
    }
}
