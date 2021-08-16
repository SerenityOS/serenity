/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6578658
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.tools.keytool
 * @requires os.family == "windows"
 * @summary Sign using the NONEwithRSA signature algorithm from SunMSCAPI
 */

import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.*;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class SignUsingNONEwithRSA {

    private static final List<byte[]> precomputedHashes = Arrays.asList(
        // A MD5 hash
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16
        },
        // A SHA-1 hash
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20
        },
        // A concatenation of SHA-1 and MD5 hashes (used during SSL handshake)
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36
        },
        // A SHA-256 hash
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32
        },
        // A SHA-384 hash
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48
        },
        // A SHA-512 hash
        new byte[] {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20,
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x50,
            0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x60,
            0x61, 0x62, 0x63, 0x64
        });

    private static List<byte[]> generatedSignatures = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        Provider[] providers = Security.getProviders("Signature.NONEwithRSA");
        if (providers == null) {
            System.out.println("No JCE providers support the " +
                "'Signature.NONEwithRSA' algorithm");
            System.out.println("Skipping this test...");
            return;

        } else {
            System.out.println("The following JCE providers support the " +
                "'Signature.NONEwithRSA' algorithm: ");
            for (Provider provider : providers) {
                System.out.println("    " + provider.getName());
            }
        }
        System.out.println(
                "Creating a temporary RSA keypair in the Windows-My store");
        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1withRSA");
        ckg.generate(1024);
        RSAPrivateCrtKey k = (RSAPrivateCrtKey) ckg.getPrivateKey();
        ks.setKeyEntry("6578658", k, null, new X509Certificate[]{
                    ckg.getSelfCertificate(new X500Name("cn=6578658,c=US"), 1000)
                });
        ks.store(null, null);

        System.out.println("---------------------------------------------");

        try {
            KeyPair keys = getKeysFromKeyStore();
            signAllUsing("SunMSCAPI", keys.getPrivate());
            System.out.println("---------------------------------------------");

            verifyAllUsing("SunMSCAPI", keys.getPublic());
            System.out.println("---------------------------------------------");

            verifyAllUsing("SunJCE", keys.getPublic());
            System.out.println("---------------------------------------------");

            keys = generateKeys();
            signAllUsing("SunJCE", keys.getPrivate());
            System.out.println("---------------------------------------------");

            verifyAllUsing("SunMSCAPI", keys.getPublic());
            System.out.println("---------------------------------------------");
        } finally {
            System.out.println(
                    "Deleting temporary RSA keypair from Windows-My store");
            ks.deleteEntry("6578658");
        }

    }

    private static KeyPair getKeysFromKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        ks.load(null, null);
        System.out.println("Loaded keystore: Windows-MY");

        Enumeration<String> e = ks.aliases();
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if (alias.equals("6578658")) {
                System.out.println("Loaded entry: " + alias);
                privateKey = (PrivateKey) ks.getKey(alias, null);
                publicKey = (PublicKey) ks.getCertificate(alias).getPublicKey();
            }
        }
        if (privateKey == null || publicKey == null) {
            throw new Exception("Cannot load the keys need to run this test");
        }

        return new KeyPair(publicKey, privateKey);
    }


    private static KeyPair generateKeys() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024, null);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        if (privateKey == null || publicKey == null) {
            throw new Exception("Cannot load the keys need to run this test");
        }

        return new KeyPair(publicKey, privateKey);
    }

    private static void signAllUsing(String providerName, PrivateKey privateKey)
            throws Exception {
        Signature sig1 = Signature.getInstance("NONEwithRSA", providerName);
        if (sig1 == null) {
            throw new Exception("'NONEwithRSA' is not supported");
        }
        if (sig1.getProvider() != null) {
            System.out.println("Using NONEwithRSA signer from the " +
                sig1.getProvider().getName() + " JCE provider");
        } else {
            System.out.println(
                "Using NONEwithRSA signer from the internal JCE provider");
        }

        System.out.println("Using key: " + privateKey);
        generatedSignatures.clear();
        for (byte[] hash : precomputedHashes) {
            sig1.initSign(privateKey);
            sig1.update(hash);

            try {

                byte [] sigBytes = sig1.sign();
                System.out.println("\nGenerated RSA signature over a " +
                    hash.length + "-byte hash (signature length: " +
                    sigBytes.length * 8 + " bits)");
                System.out.println(String.format("0x%0" +
                    (sigBytes.length * 2) + "x",
                    new java.math.BigInteger(1, sigBytes)));
                generatedSignatures.add(sigBytes);

            } catch (SignatureException se) {
                System.out.println("Error generating RSA signature: " + se);
            }
        }
    }

    private static void verifyAllUsing(String providerName, PublicKey publicKey)
            throws Exception {
        Signature sig1 = Signature.getInstance("NONEwithRSA", providerName);
        if (sig1.getProvider() != null) {
            System.out.println("\nUsing NONEwithRSA verifier from the " +
                sig1.getProvider().getName() + " JCE provider");
        } else {
            System.out.println(
                "\nUsing NONEwithRSA verifier from the internal JCE provider");
        }

        System.out.println("Using key: " + publicKey);

        int i = 0;
        for (byte[] hash : precomputedHashes) {

            byte[] sigBytes = generatedSignatures.get(i++);
            System.out.println("\nVerifying RSA Signature over a " +
                hash.length + "-byte hash (signature length: " +
                sigBytes.length * 8 + " bits)");
            System.out.println(String.format("0x%0" +
                (sigBytes.length * 2) + "x",
                new java.math.BigInteger(1, sigBytes)));

            sig1.initVerify(publicKey);
            sig1.update(hash);
            if (sig1.verify(sigBytes)) {
                System.out.println("Verify PASSED");
            } else {
                throw new Exception("Verify FAILED");
            }
        }
    }
}
