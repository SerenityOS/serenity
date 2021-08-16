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
 * @bug 8173181
 * @summary KeyStore regression due to default keystore being changed to PKCS12
 */

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Test that a PKCS12 keystore entry can be created with an empty alias name.
 */
public class EmptyAlias {

    private static final String DIR = System.getProperty("test.src", ".");
    private static final String CERT = DIR + "/trusted.pem";
    private static final String EMPTY_ALIAS = "";

    public static void main(String[] args) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, null);

        keystore.setCertificateEntry(EMPTY_ALIAS, loadCertificate(CERT));
        KeyStore.Entry entry = keystore.getEntry(EMPTY_ALIAS, null);

        if (entry == null) {
            throw new Exception(
                "Error retrieving keystore entry using its (empty) alias");
        }

        System.out.println("OK");
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
