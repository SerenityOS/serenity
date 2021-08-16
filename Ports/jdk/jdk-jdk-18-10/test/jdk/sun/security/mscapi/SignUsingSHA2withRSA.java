/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6753664 8180570
 * @summary Support SHA256 (and higher) in SunMSCAPI
 * @requires os.family == "windows"
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 */

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

public class SignUsingSHA2withRSA {

    private static final byte[] toBeSigned = new byte[] {
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10
    };

    private static List<byte[]> generatedSignatures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        if (ks.containsAlias("6753664")) {
            ks.deleteEntry("6753664");
        }

        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
        gen.generate(2048);

        ks.setKeyEntry("6753664", gen.getPrivateKey(), null,
                new Certificate[] {
                        gen.getSelfCertificate(new X500Name("cn=localhost,c=US"), 100)
                });

        try {
            run();
        } finally {
            ks.deleteEntry("6753664");
            ks.store(null, null);
        }
    }

    static void run() throws Exception {

        Provider[] providers = Security.getProviders("Signature.SHA256withRSA");
        if (providers == null) {
            System.out.println("No JCE providers support the " +
                "'Signature.SHA256withRSA' algorithm");
            System.out.println("Skipping this test...");
            return;

        } else {
            System.out.println("The following JCE providers support the " +
                "'Signature.SHA256withRSA' algorithm: ");
            for (Provider provider : providers) {
                System.out.println("    " + provider.getName());
            }
        }
        System.out.println("-------------------------------------------------");

        KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        ks.load(null, null);
        System.out.println("Loaded keystore: Windows-MY");

        Enumeration<String> e = ks.aliases();
        PrivateKey privateKey = null;
        PublicKey publicKey = null;

        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if (alias.equals("6753664")) {
                System.out.println("Loaded entry: " + alias);
                privateKey = (PrivateKey) ks.getKey(alias, null);
                publicKey = (PublicKey) ks.getCertificate(alias).getPublicKey();
            }
        }
        if (privateKey == null || publicKey == null) {
            throw new Exception("Cannot load the keys need to run this test");
        }
        System.out.println("-------------------------------------------------");

        generatedSignatures.add(signUsing("SHA256withRSA", privateKey));
        generatedSignatures.add(signUsing("SHA384withRSA", privateKey));
        generatedSignatures.add(signUsing("SHA512withRSA", privateKey));

        System.out.println("-------------------------------------------------");

        verifyUsing("SHA256withRSA", publicKey, generatedSignatures.get(0));
        verifyUsing("SHA384withRSA", publicKey, generatedSignatures.get(1));
        verifyUsing("SHA512withRSA", publicKey, generatedSignatures.get(2));

        System.out.println("-------------------------------------------------");
    }

    private static byte[] signUsing(String signAlgorithm,
        PrivateKey privateKey) throws Exception {

        // Must explicitly specify the SunMSCAPI JCE provider
        // (otherwise SunJCE is chosen because it appears earlier in the list)
        Signature sig1 = Signature.getInstance(signAlgorithm, "SunMSCAPI");
        if (sig1 == null) {
            throw new Exception("'" + signAlgorithm + "' is not supported");
        }
        System.out.println("Using " + signAlgorithm + " signer from the " +
            sig1.getProvider().getName() + " JCE provider");

        System.out.println("Using key: " + privateKey);
        sig1.initSign(privateKey);
        sig1.update(toBeSigned);
        byte [] sigBytes = null;

        try {
            sigBytes = sig1.sign();
            System.out.println("Generated RSA signature over a " +
                toBeSigned.length + "-byte data (signature length: " +
                sigBytes.length * 8 + " bits)");
            System.out.println(String.format("0x%0" +
                (sigBytes.length * 2) + "x",
                new java.math.BigInteger(1, sigBytes)));

        } catch (SignatureException se) {
                System.out.println("Error generating RSA signature: " + se);
        }

        return sigBytes;
    }

    private static void verifyUsing(String signAlgorithm, PublicKey publicKey,
        byte[] signature) throws Exception {

        // Must explicitly specify the SunMSCAPI JCE provider
        // (otherwise SunJCE is chosen because it appears earlier in the list)
        Signature sig1 = Signature.getInstance(signAlgorithm, "SunMSCAPI");
        if (sig1 == null) {
            throw new Exception("'" + signAlgorithm + "' is not supported");
        }
        System.out.println("Using " + signAlgorithm + " verifier from the "
            + sig1.getProvider().getName() + " JCE provider");

        System.out.println("Using key: " + publicKey);

        System.out.println("\nVerifying RSA Signature over a " +
            toBeSigned.length + "-byte data (signature length: " +
            signature.length * 8 + " bits)");
        System.out.println(String.format("0x%0" + (signature.length * 2) +
            "x", new java.math.BigInteger(1, signature)));

        sig1.initVerify(publicKey);
        sig1.update(toBeSigned);

        if (sig1.verify(signature)) {
            System.out.println("Verify PASSED\n");
        } else {
            throw new Exception("Verify FAILED");
        }
    }
}
