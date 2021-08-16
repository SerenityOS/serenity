/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6958026 8242151
 * @summary Problem with PKCS12 keystore
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file PKCS12SameKeyId.java
 * @run main PKCS12SameKeyId
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

public class PKCS12SameKeyId {

    private static final String JKSFILE = "PKCS12SameKeyId.jks";
    private static final String P12FILE = "PKCS12SameKeyId.p12";
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static final int SIZE = 10;

    public static void main(String[] args) throws Exception {

        // Prepare a JKS keystore with many entries
        new File(JKSFILE).delete();
        for (int i=0; i<SIZE; i++) {
            System.err.print(".");
            String cmd = "-keystore " + JKSFILE
                    + " -storepass changeit -keypass changeit -keyalg rsa "
                    + "-genkeypair -alias p" + i + " -dname CN=" + i;
            sun.security.tools.keytool.Main.main(cmd.split(" "));
        }

        // Prepare EncryptedPrivateKeyInfo parameters, copied from various
        // places in PKCS12KeyStore.java
        AlgorithmParameters algParams =
                AlgorithmParameters.getInstance("PBEWithSHA1AndDESede");
        algParams.init(new PBEParameterSpec("12345678".getBytes(), 1024));
        AlgorithmId algid = new AlgorithmId(
                ObjectIdentifier.of("1.2.840.113549.1.12.1.3"), algParams);

        PBEKeySpec keySpec = new PBEKeySpec(PASSWORD);
        SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
        SecretKey skey = skFac.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("PBEWithSHA1AndDESede");
        cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);

        // Pre-calculated keys and certs and aliases
        byte[][] keys = new byte[SIZE][];
        Certificate[][] certChains = new Certificate[SIZE][];
        String[] aliases = new String[SIZE];

        // Reads from JKS keystore and pre-calculate
        KeyStore ks = KeyStore.getInstance("jks");
        try (FileInputStream fis = new FileInputStream(JKSFILE)) {
            ks.load(fis, PASSWORD);
        }
        for (int i=0; i<SIZE; i++) {
            aliases[i] = "p" + i;
            byte[] enckey = cipher.doFinal(
                    ks.getKey(aliases[i], PASSWORD).getEncoded());
            keys[i] = new EncryptedPrivateKeyInfo(algid, enckey).getEncoded();
            certChains[i] = ks.getCertificateChain(aliases[i]);
        }

        // Write into PKCS12 keystore. Use this overloaded version of
        // setKeyEntry() to be as fast as possible, so that they would
        // have same localKeyId.
        KeyStore p12 = KeyStore.getInstance("pkcs12");
        p12.load(null, PASSWORD);
        for (int i=0; i<SIZE; i++) {
            p12.setKeyEntry(aliases[i], keys[i], certChains[i]);
        }
        try (FileOutputStream fos = new FileOutputStream(P12FILE)) {
            p12.store(fos, PASSWORD);
        }

        // Check private keys still match certs
        p12 = KeyStore.getInstance("pkcs12");
        try (FileInputStream fis = new FileInputStream(P12FILE)) {
            p12.load(fis, PASSWORD);
        }
        for (int i=0; i<SIZE; i++) {
            String a = "p" + i;
            X509Certificate x = (X509Certificate)p12.getCertificate(a);
            X500Name name = (X500Name)x.getSubjectDN();
            if (!name.getCommonName().equals(""+i)) {
                throw new Exception(a + "'s cert is " + name);
            }
        }
    }
}
