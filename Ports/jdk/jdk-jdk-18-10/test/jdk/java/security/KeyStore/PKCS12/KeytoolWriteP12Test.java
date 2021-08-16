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
import jdk.test.lib.process.OutputAnalyzer;
import static java.lang.System.out;

/**
 * @test
 * @bug 8048830
 * @summary Tests for creating pkcs12 keystore with various algorithms
 * @library ../
 * @library /test/lib
 * @run main KeytoolWriteP12Test
 */
public class KeytoolWriteP12Test {
    private static final String ALIAS = "pkcs12testCA";
    private static final Utils.KeyStoreType PKCS12 = Utils.KeyStoreType.pkcs12;
    private static final int FAILED_EXIT_CODE = 1;
    private static final String CERT_FILE_NAME = "cert.data";
    private static final String DNAME = "CN=PKCS12 Test CA, OU=Security SQE, "
            + "O=JavaSoft, C=US";
    private static final String WORKING_DIRECTORY = System.
            getProperty("test.classes", "." + File.separator);
    private enum Algorithm {
        DSA, RSA, ECC
    };
    private void run() {
        out.println("Running DSA Test");
        keytoolListTest("kt_DSA.p12", Algorithm.DSA);
        out.println("DSA Test passed");

        out.println("Running RSA Test");
        final String rsaKeyStoreName = "kt_RSA_MD5.p12";
        keytoolListTest(rsaKeyStoreName, Algorithm.RSA);
        out.println("RSA Test passed");

        out.println("Running RSA and Signing Algorithm SHA1withRSA Test");
        keytoolListTest("kt_RSA_SHA1.p12", Algorithm.RSA,
                "-sigalg", "SHA1withRSA");
        out.println("RSA and Signing Algorithm SHA1withRSA Test Passed");

        out.println("Running Keysize 256 Test");
        keytoolListNegativeTest("kt_DSA_256.p12", Algorithm.DSA, "-keysize",
                "256");
        out.println("Keysize 256 Test Passed");

        out.println("Running Keysize 1023 Test");
        keytoolListTest("kt_RSA_MD5_1023.p12", Algorithm.RSA, "-keysize",
                "1023");
        out.println("Keysize 1023 Test Passed");
        out.println("Running Export certificate Test");
        exportTest(rsaKeyStoreName);
        out.println("Export certificate Test Passed");
    }

    private void exportTest(String keyStore) {
        final String keyStoreName = WORKING_DIRECTORY + File.separator
                + keyStore;
        deleteKeyStoreFile(keyStoreName);
        Utils.createKeyStore(DNAME, PKCS12, keyStore, ALIAS,
                Algorithm.RSA.name());
        final String certFilePath = WORKING_DIRECTORY + File.separator
                + CERT_FILE_NAME;
        Utils.exportCert(PKCS12, keyStore,
                ALIAS, certFilePath);
        final String[] command = new String[]{"-debug", "-printcert", "-v",
            "-file", certFilePath};
        Utils.executeKeytoolCommand(command);
    }

    private void keytoolListTest(String keyStore, Algorithm algorithm,
            String ...optionalArgs) {
        final String keyStoreName = WORKING_DIRECTORY + File.separator
                + keyStore;
        final String[] command = new String[]{"-debug", "-list", "-v", "-alias",
            ALIAS, "-keystore", keyStoreName, "-storetype", "pkcs12",
            "-storepass", Utils.DEFAULT_PASSWD};
        deleteKeyStoreFile(keyStoreName);
        Utils.createKeyStore(DNAME, PKCS12, keyStoreName, ALIAS,
                algorithm.name(), optionalArgs);
        OutputAnalyzer output = Utils.executeKeytoolCommand(command);
        output.shouldContain(DNAME);
    }

    private void keytoolListNegativeTest(String keyStore, Algorithm algorithm,
            String... optionalArgs) {
        final String keyStoreName = WORKING_DIRECTORY  + File.separator
                + keyStore;
        deleteKeyStoreFile(keyStoreName);
        Utils.createKeyStore(DNAME, PKCS12, keyStoreName, ALIAS,
                algorithm.name(), optionalArgs, FAILED_EXIT_CODE);
    }

    public static void main(String[] args) {
        KeytoolWriteP12Test test = new KeytoolWriteP12Test();
        test.run();
        out.println("Test Passed");
    }

    private void deleteKeyStoreFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
