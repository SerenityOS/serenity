/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import static java.lang.System.err;
import static java.lang.System.out;

/**
 * @test
 * @bug 8048830
 * @summary Test imports certificate from file to PKCS12 keystore store it as
 * trusted certificate Check import errors (must be not errors) & check keystore
 * content after import
 * @library ../
 * @library /test/lib
 * @run main StoreTrustedCertAPITest
 */
public class StoreTrustedCertAPITest {
    private static final char[] PASSWORD = "passwd".toCharArray();
    private static final String ALIAS = "testkey_stcapi";
    private static final String WORKING_DIRECTORY = System.getProperty(
            "test.classes", "." + File.separator);
    private static final String CERT_PATH = WORKING_DIRECTORY + File.separator
            + "cert.data";
    private static final String KEYSTORE_PATH = WORKING_DIRECTORY
            + File.separator + "ks.pkcs12";

    /**
     * Test logic (environment has set up)
     */
    private void runTest() throws FileNotFoundException, CertificateException,
            KeyStoreException, IOException, NoSuchAlgorithmException {
        Certificate cert;
        CertificateFactory cf;
        try (FileInputStream fi = new FileInputStream(CERT_PATH)) {
            cf = CertificateFactory.getInstance("X.509");
            cert = cf.generateCertificate(fi);
            KeyStore ks = KeyStore.getInstance(
                    Utils.KeyStoreType.pkcs12.name());
            ks.load(null, null);
            ks.setCertificateEntry(ALIAS, cert);
            Utils.saveKeyStore(ks, KEYSTORE_PATH, PASSWORD);
            ks = Utils.loadKeyStore(KEYSTORE_PATH, Utils.KeyStoreType.pkcs12,
                    PASSWORD);
            final Certificate ksCert = ks.getCertificate(ALIAS);
            if (!ksCert.equals(cert)) {
                err.println("Orig cert: " + cert.toString());
                err.println("Cert from keystore: " + ksCert.toString());
                throw new RuntimeException("Certificates don't match");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        StoreTrustedCertAPITest test = new StoreTrustedCertAPITest();
        test.setUp();
        test.runTest();
        out.println("Test Passed");
    }

    private void setUp() {
        Utils.createKeyStore(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS);
        Utils.exportCert(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH,
                ALIAS, CERT_PATH);
        new File(KEYSTORE_PATH).delete();
    }
}
