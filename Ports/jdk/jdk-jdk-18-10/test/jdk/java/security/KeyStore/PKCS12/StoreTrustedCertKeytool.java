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
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import jdk.test.lib.process.OutputAnalyzer;
import static java.lang.System.out;

/**
 * @test
 * @bug 8048830
 * @summary Tests keytool command imports certificate , list keystore, print
 * certificate and import password help.
 * @library ../
 * @library /test/lib
 * @run main StoreTrustedCertKeytool
 */
public class StoreTrustedCertKeytool {
    private static final String PASSWORD = "passwd";
    private static final String ALIAS = "testkey_stckey";
    private static final String FILE_SEPARATOR = File.separator;
    private static final String WORKING_DIRECTORY = System.getProperty(
            "test.classes", "." + FILE_SEPARATOR);
    private static final String CERT_PATH = WORKING_DIRECTORY
            + FILE_SEPARATOR
            + "cert.data";
    private static final String KEYSTORE_PATH = WORKING_DIRECTORY
            + FILE_SEPARATOR + "ks.pkcs12";

    protected void run() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        setUp();
        importCert();
        out.println("Import Cert test passed");
        listCerts();
        out.println("listCerts test passed");
        printCert();
        out.println("print cert test passed");
        helpImportPassword();
        out.println("help import test passed");
    }

    private void importCert() {
        final String[] command = new String[]{"-debug", "-importcert",
            "-alias", ALIAS, "-file", CERT_PATH, "-noprompt", "-keystore",
            KEYSTORE_PATH, "-storetype", "pkcs12", "-storepass", PASSWORD};
        // If the keystore exists delete it.
        File keystoreFile = new File(KEYSTORE_PATH);
        if (keystoreFile.exists()) {
            keystoreFile.delete();
        }
        Utils.executeKeytoolCommand(command);
    }

    private void listCerts() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        final String[] command = new String[]{"-debug", "-list", "-v",
            "-alias", ALIAS, "-keystore", KEYSTORE_PATH, "-storetype", "pkcs12",
            "-storepass", PASSWORD};
        OutputAnalyzer output = Utils.executeKeytoolCommand(command);
        if (output == null) {
            throw new RuntimeException("Keystore print fails");
        }
        X509Certificate ksCert = null;
        final KeyStore ks = Utils.loadKeyStore(KEYSTORE_PATH,
                Utils.KeyStoreType.pkcs12, PASSWORD.toCharArray());
        ksCert = (X509Certificate) ks.getCertificate(ALIAS);

        if (ksCert == null) {
            throw new RuntimeException("Certificate " + ALIAS
                    + " not found in Keystore " + KEYSTORE_PATH);
        }
        String serialNumber = ksCert.getSerialNumber().toString(16);
        output.shouldContain(serialNumber);
    }

    private void printCert() {
        final String[] command = new String[]{"-debug", "-printcert",
            "-file", CERT_PATH};
        Utils.executeKeytoolCommand(command);

    }

    private void helpImportPassword() {
        final String[] command = new String[]{"-debug", "-help",
            "-importpassword"};
        Utils.executeKeytoolCommand(command);
    }

    public static void main(String[] args) throws Exception {
        final StoreTrustedCertKeytool test = new StoreTrustedCertKeytool();
        test.run();
        out.println("Test Passed");
    }

    private void setUp() {
        Utils.createKeyStore(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS);
        Utils.exportCert(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS,
                CERT_PATH);
        new File(KEYSTORE_PATH).delete();
    }
}
