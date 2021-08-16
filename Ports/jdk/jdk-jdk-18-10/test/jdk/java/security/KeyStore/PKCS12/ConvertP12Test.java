/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;

/*
 * @test
 * @bug 8048619
 * @author  Bill Situ
 * @summary Test converting keystore from jceks to P12 and from P12 to other
 *  (jceks,jks). including following test cases:
 * Read jceks key store and convert to the p12 key store, then compare entries
 *  in the two key stores.
 * Read p12 key store and convert to the jceks key store, then compare entries
 *  in the two key stores.
 * Read p12 key store (contains only private key and a self-signed certificate)
 *  and convert to the jceks key store, then compare entries of two key stores.
 * Read p12 key store (contains 2 entries) and convert to the jceks key store,
 *  then compare entries in the two key stores.
 * Read p12 key store (entry password and key store password are different) and
 *  convert to the jceks key store, then compare entries in the two key stores.
 * Read p12 key store and convert to the jks key store, then compare entries
 *  in the two key stores.
 * Read p12 key store (contains only private key and a self-signed certificate)
 *  and convert to the jks key store, then compare entries in the two key stores.
 * Read p12 key store (contains 2 entries) and convert to the jks key store,
 *  then compare entries in the two key stores.
 * Read p12 key store (entry password and key store password are different) and
 * convert to the jks key store, then compare entries in the two key stores.
 */

public class ConvertP12Test {

    private static final String SUN_JSSE = "SunJSSE";
    private static final String SUN_JCE = "SunJCE";
    private static final String SUN = "SUN";
    private static final String PKCS12 = "pkcs12";
    private static final String JCE_KS = "JceKS";
    private static final String JKS = "JKS";

    public static void main(String args[]) throws Exception {

        ConvertP12Test jstest = new ConvertP12Test();

        jstest.driver("JceksToP12", "keystoreCA.jceks.data", JCE_KS, SUN_JCE,
                "storepass", "keypass", PKCS12, SUN_JSSE);

        jstest.driver("P12ToJceks_Chain", "ie_jceks_chain.pfx.data", PKCS12,
                SUN_JSSE, "pass", "pass", JCE_KS, SUN_JCE);

        jstest.driver("P12ToJceks_SelfSigned", "jdk_jceks_selfsigned.p12.data",
                PKCS12, SUN_JSSE, "pass", "pass", JCE_KS, SUN_JCE);

        jstest.driver("P12ToJceks_TwoEntry", "jdk_jceks_twoentry.p12.data",
                PKCS12, SUN_JSSE, "pass", "pass", JCE_KS, SUN_JCE);

        jstest.driver("P12ToJceks_TwoPass", "jdk_jceks_twopass.p12.data",
                PKCS12, SUN_JSSE, "storepass", "keypass", JCE_KS, SUN_JCE);

        jstest.driver("P12ToJks_Chain", "ie_jks_chain.pfx.data", PKCS12,
                SUN_JSSE, "pass", "pass", JKS, SUN);

        jstest.driver("P12ToJks_SelfSigned", "jdk_jks_selfsigned.p12.data",
                PKCS12, SUN_JSSE, "pass", "pass", JKS, SUN);

        jstest.driver("P12ToJks_TwoEntry", "jdk_jks_twoentry.p12.data", PKCS12,
                SUN_JSSE, "pass", "pass", JKS, SUN);

        jstest.driver("P12ToJks_TwoPass", "jdk_jks_twopass.p12.data", PKCS12,
                SUN_JSSE, "storepass", "keypass", JKS, SUN);

    }

    private void driver(String testCase, String inKeyStore,
            String inKeyStoreType, String inKeyStoreTypePrv,
            String inStorePass, String inKeyPass, String outKeyStoreType,
            String outKeyStorePrv) throws Exception {

        String outStorePass = "pass";
        String outKeyPass = "pass";
        KeyStore inputKeyStore, outputKeyStore;

        out.println("Testing " + testCase);
        String keystorePath = System.getProperty("test.src", ".")
                + File.separator + "certs" + File.separator + "convertP12";
        out.println("Output KeyStore : " + inKeyStore + ".out");
        String outKeyStoreName = inKeyStore + ".out";
        try (FileOutputStream fout = new FileOutputStream(outKeyStoreName);) {
            inputKeyStore = KeyStore.getInstance(inKeyStoreType,
                    inKeyStoreTypePrv);

            // KeyStore have encoded by Base64.getMimeEncoder().encode(),need
            // decode first.
            byte[] input = Files.readAllBytes(Paths.get(keystorePath,
                    inKeyStore));
            ByteArrayInputStream arrayIn = new ByteArrayInputStream(Base64
                    .getMimeDecoder().decode(input));

            out.println("Input KeyStore : " + inKeyStore);

            inputKeyStore.load(arrayIn, inStorePass.toCharArray());

            outputKeyStore = KeyStore.getInstance(outKeyStoreType,
                    outKeyStorePrv);
            outputKeyStore.load(null, null);

            run(inputKeyStore, outputKeyStore, inKeyPass, outKeyPass);

            outputKeyStore.store(fout, outStorePass.toCharArray());

            // for P12ToJks_TwoEntry test case will test includes each other,
            // others just test compareKeystore
            if (testCase.contains("TwoEntry")) {

                compareKeyStore(inputKeyStore, outputKeyStore, inKeyPass,
                        outKeyPass, 2);
                compareKeyStore(outputKeyStore, inputKeyStore, outKeyPass,
                        inKeyPass, 2);
            } else {
                compareKeyStore(inputKeyStore, outputKeyStore, inKeyPass,
                        outKeyPass, 1);
            }
            out.println("Test " + testCase + " STATUS: Pass!!");
        } catch (Exception ex) {
            out.println("Test " + testCase + " STATUS: failed with exception: "
                    + ex.getMessage());
            throw ex;
        }
    }

    private void run(KeyStore inputKeyStore, KeyStore outputKeyStore,
            String inKeyPass, String outKeyPass) throws Exception {
        Enumeration<String> e = inputKeyStore.aliases();
        String alias;
        while (e.hasMoreElements()) {
            alias = e.nextElement();
            Certificate[] certs = inputKeyStore.getCertificateChain(alias);

            boolean isCertEntry = inputKeyStore.isCertificateEntry(alias);
            // Test KeyStore only contain key pair entries.
            if (isCertEntry == true) {
                throw new RuntimeException(
                        "inputKeystore should not be certEntry because test"
                                + " keystore only contain key pair entries"
                                + " for alias:" + alias);
            }

            boolean isKeyEntry = inputKeyStore.isKeyEntry(alias);
            Key key = null;
            if (isKeyEntry) {
                key = inputKeyStore.getKey(alias, inKeyPass.toCharArray());
            } else {
                throw new RuntimeException("Entry type unknown for alias:"
                        + alias);
            }
            outputKeyStore.setKeyEntry(alias, key, outKeyPass.toCharArray(),
                    certs);
        }
    }

    private void compareKeyStore(KeyStore a, KeyStore b, String inKeyPass,
            String outKeyPass, int keyStoreSize) throws Exception {
        if (a.size() != keyStoreSize || b.size() != keyStoreSize) {
            throw new RuntimeException("size not match or size not equal to "
                    + keyStoreSize);
        }

        Enumeration<String> eA = a.aliases();
        while (eA.hasMoreElements()) {
            String aliasA = eA.nextElement();

            if (!b.containsAlias(aliasA)) {
                throw new RuntimeException("alias not match for alias:"
                        + aliasA);
            }

            compareKeyEntry(a, b, inKeyPass, outKeyPass, aliasA);
        }
    }

    private void compareKeyEntry(KeyStore a, KeyStore b, String aPass,
            String bPass, String alias) throws KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {
        Certificate[] certsA = a.getCertificateChain(alias);
        Certificate[] certsB = b.getCertificateChain(alias);

        if (!Arrays.equals(certsA, certsB)) {
            throw new RuntimeException("Certs don't match for alias:" + alias);
        }

        Key keyA = a.getKey(alias, aPass.toCharArray());
        Key keyB = b.getKey(alias, bPass.toCharArray());

        if (!keyA.equals(keyB)) {
            throw new RuntimeException(
                    "Key don't match for alias:" + alias);
        }
    }
}
