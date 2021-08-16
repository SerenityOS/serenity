/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8161571 8178370
 * @summary Reject signatures presented for verification that contain extra
 *          bytes.
 * @modules jdk.crypto.ec
 * @run main SignatureLength
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

public class SignatureLength {

    public static void main(String[] args) throws Exception {
        for (Provider p0 : Security.getProviders()) {
            for (Provider p1 : Security.getProviders()) {
                for (Provider p2 : Security.getProviders()) {
                    // SunMSCAPI signer can only be initialized with
                    // a key generated with SunMSCAPI
                    if (!p0.getName().equals("SunMSCAPI")
                            && p1.getName().equals("SunMSCAPI")) continue;

                    // SunMSCAPI generated key can only be signed
                    // with SunMSCAPI signer
                    if (p0.getName().equals("SunMSCAPI")
                            && !p1.getName().equals("SunMSCAPI")) continue;

                    // SunMSCAPI and SunPKCS11 verifiers may return false
                    // instead of throwing SignatureException
                    boolean mayNotThrow = p2.getName().equals("SunMSCAPI")
                            || p2.getName().startsWith("SunPKCS11");

                    main0("EC", 256, "SHA256withECDSA", p0, p1, p2, mayNotThrow);
                    main0("RSA", 2048, "SHA256withRSA", p0, p1, p2, mayNotThrow);
                    main0("DSA", 2048, "SHA256withDSA", p0, p1, p2, mayNotThrow);
                }
            }
        }
    }

    private static void main0(String keyAlgorithm, int keysize,
            String signatureAlgorithm, Provider generatorProvider,
            Provider signerProvider, Provider verifierProvider,
            boolean mayNotThrow) throws Exception {

        KeyPairGenerator generator;
        Signature signer;
        Signature verifier;

        try {
            generator = KeyPairGenerator.getInstance(keyAlgorithm,
                    generatorProvider);
            signer = Signature.getInstance(signatureAlgorithm,
                    signerProvider);
            verifier = Signature.getInstance(signatureAlgorithm,
                    verifierProvider);
        } catch (NoSuchAlgorithmException nsae) {
            // ignore this set of providers
            return;
        }

        byte[] plaintext = "aaa".getBytes("UTF-8");

        // Generate
        generator.initialize(keysize);
        System.out.println("Generating " + keyAlgorithm + " keypair using " +
            generator.getProvider().getName() + " JCE provider");
        KeyPair keypair = generator.generateKeyPair();

        // Sign
        signer.initSign(keypair.getPrivate());
        signer.update(plaintext);
        System.out.println("Signing using " + signer.getProvider().getName() +
            " JCE provider");
        byte[] signature = signer.sign();

        // Invalidate
        System.out.println("Invalidating signature ...");
        byte[] badSignature = new byte[signature.length + 5];
        System.arraycopy(signature, 0, badSignature, 0, signature.length);
        badSignature[signature.length] = 0x01;
        badSignature[signature.length + 1] = 0x01;
        badSignature[signature.length + 2] = 0x01;
        badSignature[signature.length + 3] = 0x01;
        badSignature[signature.length + 4] = 0x01;

        // Verify
        verifier.initVerify(keypair.getPublic());
        verifier.update(plaintext);
        System.out.println("Verifying using " +
            verifier.getProvider().getName() + " JCE provider");

        try {
            boolean valid = verifier.verify(badSignature);
            System.out.println("Valid? " + valid);
            if (mayNotThrow) {
                if (valid) {
                    throw new Exception(
                        "ERROR: expected a SignatureException but none was thrown"
                        + " and invalid signature was verified");
                } else {
                    System.out.println("OK: verification failed as expected");
                }
            } else {
                throw new Exception(
                    "ERROR: expected a SignatureException but none was thrown");
            }
        } catch (SignatureException e) {
            System.out.println("OK: caught expected exception: " + e);
        }
        System.out.println();
    }
}
