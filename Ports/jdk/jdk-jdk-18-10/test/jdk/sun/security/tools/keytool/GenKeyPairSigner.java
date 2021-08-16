/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260693
 * @summary Test for keytool -genkeypair with -signer and -signerkeypass options
 * @library /test/lib
 * @modules java.base/sun.security.util
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Arrays;
import sun.security.util.DerValue;
import sun.security.util.KeyUtil;
import sun.security.util.KnownOIDs;
import static sun.security.util.KnownOIDs.*;

public class GenKeyPairSigner {

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks);
    }

    static OutputAnalyzer ktjks(String cmd, String ks, String kpass) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks + " -storetype jks" + " -keypass " +
                kpass);
    }

    public static void main(String[] args) throws Exception {
        testSignerPKCS12();
        testSignerJKS();
        testSignerOpt();
    }

    static void testSignerPKCS12() throws Exception {
        KeyStore kstore;
        X509Certificate cert;
        String sigName, pKeyAlg;
        PublicKey pKey;
        int keyLen;

        /*
         * The signer alias is stored in the PKCS12 keystore
         */
        System.out.println("Testing the signer alias that is stored in the PKCS12 keystore");
        System.out.println("Generating a root cert with SubjectKeyIdentifier extension");
        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg EdDSA -alias ca -dname CN=CA -ext bc:c " +
                "-ext 2.5.29.14=04:14:00:01:02:03:04:05:06:07:08:09:10:11:12:13:14:15:16:17:18:19")
                .shouldContain("Generating 255 bit Ed25519 key pair and self-signed certificate (Ed25519) with a validity of 90 days")
                .shouldContain("for: CN=CA")
                .shouldHaveExitValue(0);

        System.out.println("Generating an XDH cert with -signer option");
        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg XDH -alias e1 -dname CN=E1 -signer ca")
                .shouldContain("Generating 255 bit XDH key pair and a certificate (Ed25519) issued by <ca> with a validity of 90 days")
                .shouldContain("for: CN=E1")
                .shouldHaveExitValue(0);

        // examine the resulting cert
        kstore = KeyStore.getInstance(new File("ks"), "changeit".toCharArray());
        cert = (X509Certificate)kstore.getCertificate("e1");

        Certificate[] certChain = kstore.getCertificateChain("e1");
        if (certChain.length != 2) {
            throw new Exception("Generated cert chain is in error");
        }

        sigName = cert.getSigAlgName();
        if (sigName != "Ed25519") {
            throw new Exception("Signature algorithm name is in error");
        }

        pKey = cert.getPublicKey();
        keyLen = KeyUtil.getKeySize(pKey);
        if (keyLen != 255) {
            throw new Exception("Key size is in error");
        }

        pKeyAlg = pKey.getAlgorithm();
        if (pKeyAlg != "XDH") {
            throw new Exception("Subject Public Key Algorithm is in error");
        }

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-list -v")
                .shouldContain("Alias name: e1")
                .shouldContain("Certificate chain length: 2")
                .shouldContain("Signature algorithm name: Ed25519")
                .shouldContain("Subject Public Key Algorithm: 255-bit XDH key")
                .shouldHaveExitValue(0);

        // check to make sure that cert's AKID is created from the SKID of the signing cert
        byte[] expectedId = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19};

        byte[] authorityKeyIdExt = cert.getExtensionValue(
                KnownOIDs.AuthorityKeyID.value());

        byte[] authorityKeyId = null;
        if (authorityKeyIdExt == null) {
            throw new Exception("Failed to get AKID extension from the cert");
        } else {
            try {
                authorityKeyId = new DerValue(authorityKeyIdExt).getOctetString();
            } catch (IOException e) {
                throw new Exception("Failed to get AKID encoded OctetString in the cert");
            }
        }

        authorityKeyId = Arrays.copyOfRange(authorityKeyId, 4, authorityKeyId.length);
        if (!Arrays.equals(authorityKeyId, expectedId)) {
            throw new Exception("Failed due to AKID mismatch");
        }

        kt("-genkeypair -keyalg RSA -alias ca2 -dname CN=CA2 -ext bc:c ",
                "ks");

        System.out.println("Generating an X448 cert with -signer option");
        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg X448 -alias e2 -dname CN=E2 -sigalg SHA384withRSA -signer ca2")
                .shouldContain("Generating 448 bit XDH key pair and a certificate (SHA384withRSA) issued by <ca2> with a validity of 90 days")
                .shouldContain("for: CN=E2")
                .shouldHaveExitValue(0);

        // examine the resulting cert
        kstore = KeyStore.getInstance(new File("ks"), "changeit".toCharArray());
        cert = (X509Certificate)kstore.getCertificate("e2");
        sigName = cert.getSigAlgName();
        if (sigName != "SHA384withRSA") {
            throw new Exception("Signature algorithm name is in error");
        }

        pKey = cert.getPublicKey();
        keyLen = KeyUtil.getKeySize(pKey);
        if (keyLen != 448) {
            throw new Exception("Key size is in error");
        }

        pKeyAlg = pKey.getAlgorithm();
        if (pKeyAlg != "XDH") {
            throw new Exception("Subject Public Key Algorithm is in error");
        }

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-list -v")
                .shouldContain("Alias name: e2")
                .shouldContain("Signature algorithm name: SHA384withRSA")
                .shouldContain("Subject Public Key Algorithm: 448-bit XDH key")
                .shouldHaveExitValue(0);

        kt("-genkeypair -keyalg DSA -alias ca3 -dname CN=CA3 -ext bc:c ",
                "ks");

        System.out.println("Generating a DH cert with -signer option");
        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg DH -alias e3 -dname CN=E3 -signer ca3")
                .shouldContain("Generating 2,048 bit DH key pair and a certificate (SHA256withDSA) issued by <ca3> with a validity of 90 days")
                .shouldContain("for: CN=E3")
                .shouldHaveExitValue(0);

        // examine the resulting cert
        kstore = KeyStore.getInstance(new File("ks"), "changeit".toCharArray());
        cert = (X509Certificate)kstore.getCertificate("e3");
        sigName = cert.getSigAlgName();
        if (sigName != "SHA256withDSA") {
            throw new Exception("Signature algorithm name is in error");
        }

        pKey = cert.getPublicKey();
        keyLen = KeyUtil.getKeySize(pKey);
        if (keyLen != 2048) {
            throw new Exception("Key size is in error");
        }

        pKeyAlg = pKey.getAlgorithm();
        if (pKeyAlg != "DH") {
            throw new Exception("Subject Public Key Algorithm is in error");
        }

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-list -v")
                .shouldContain("Alias name: e3")
                .shouldContain("Signature algorithm name: SHA256withRSA")
                .shouldContain("Subject Public Key Algorithm: 2048-bit DH key")
                .shouldHaveExitValue(0);
    }

    static void testSignerJKS() throws Exception {
        KeyStore kstore;
        X509Certificate cert;
        String sigName, pKeyAlg;
        PublicKey pKey;
        int keyLen;

        /*
         * The signer alias is stored in the JKS keystore
         * Using JKS keystore here is to test the scenario when the private key
         * of the signer entry is protected by a password different from the
         * store password, and -signerkeypass option needs to be specified
         * along with -signer option.
         */
        System.out.println("Testing the signer alias that is stored in the JKS keystore");
        ktjks("-genkeypair -keyalg RSA -keysize 1024 -alias ca -dname CN=CA -ext bc:c",
                "ksjks", "cakeypass");

        System.out.println("Generating an DSA cert with -signer and -signerkeypass options");
        SecurityTools.keytool("-keystore ksjks -storepass changeit -storetype jks " +
                "-genkeypair -keyalg DSA -keysize 1024 -alias ca1 -dname CN=CA1 " +
                "-keypass ca1keypass -signer ca -signerkeypass cakeypass")
                .shouldContain("Generating 1,024 bit DSA key pair and a certificate (SHA256withRSA) issued by <ca> with a validity of 90 days")
                .shouldContain("for: CN=CA1")
                .shouldContain("The generated certificate #1 of 2 uses a 1024-bit DSA key which is considered a security risk")
                .shouldContain("The generated certificate #2 of 2 uses a 1024-bit RSA key which is considered a security risk")
                .shouldHaveExitValue(0);

        System.out.println("Generating an XDH cert with -signer and -signerkeypass options");
        SecurityTools.keytool("-keystore ksjks -storepass changeit -storetype jks " +
                "-genkeypair -keyalg XDH -alias e1 -dname CN=E1 " +
                "-keypass e1keypass -signer ca1 -signerkeypass ca1keypass")
                .shouldContain("Generating 255 bit XDH key pair and a certificate (SHA256withDSA) issued by <ca1> with a validity of 90 days")
                .shouldContain("for: CN=E1")
                .shouldContain("The generated certificate #2 of 3 uses a 1024-bit DSA key which is considered a security risk")
                .shouldContain("The generated certificate #3 of 3 uses a 1024-bit RSA key which is considered a security risk")
                .shouldHaveExitValue(0);

        // examine the resulting cert
        kstore = KeyStore.getInstance(new File("ksjks"), "changeit".toCharArray());
        cert = (X509Certificate)kstore.getCertificate("e1");

        Certificate[] certChain = kstore.getCertificateChain("e1");
        if (certChain.length != 3) {
            throw new Exception("Generated cert chain is in error");
        }

        sigName = cert.getSigAlgName();
        if (sigName != "SHA256withDSA") {
            throw new Exception("Signature algorithm name is in error");
        }

        pKey = cert.getPublicKey();
        keyLen = KeyUtil.getKeySize(pKey);
        if (keyLen != 255) {
            throw new Exception("Key size is in error");
        }

        pKeyAlg = pKey.getAlgorithm();
        if (pKeyAlg != "XDH") {
            throw new Exception("Subject Public Key Algorithm is in error");
        }

        SecurityTools.keytool("-keystore ksjks -storepass changeit " +
                "-list -v")
                .shouldContain("Alias name: e1")
                .shouldContain("Certificate chain length: 3")
                .shouldContain("Signature algorithm name: SHA256withDSA")
                .shouldContain("Subject Public Key Algorithm: 255-bit XDH key")
                .shouldHaveExitValue(0);
    }

    static void testSignerOpt() throws Exception {

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg X25519 -alias e4 -dname CN=E4")
                .shouldContain("Cannot derive signature algorithm from XDH")
                .shouldHaveExitValue(1);

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-genkeypair -keyalg X448 -alias e4 -dname CN=E4 -signer noca")
                .shouldContain("Alias <noca> does not exist")
                .shouldHaveExitValue(1);

        SecurityTools.keytool("-genkeypair --help")
                .shouldContain("-signer <alias>         signer alias")
                .shouldContain("-signerkeypass <arg>    signer key password")
                .shouldHaveExitValue(0);
    }
}
