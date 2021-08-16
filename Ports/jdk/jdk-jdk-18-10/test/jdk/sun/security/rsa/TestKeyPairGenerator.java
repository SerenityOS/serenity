/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4853305 4865198 4888410 4963723 8146293
 * @summary Verify that the RSA KeyPairGenerator works
 * @library /test/lib
 * @build jdk.test.lib.SigTestUtil
 * @run main TestKeyPairGenerator
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.math.BigInteger;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import jdk.test.lib.SigTestUtil;
import static jdk.test.lib.SigTestUtil.SignatureType;

public class TestKeyPairGenerator {

    private static Provider provider;

    private static byte[] data;

    private static void testSignature(SignatureType type, String mdAlg,
            PrivateKey privateKey, PublicKey publicKey) throws
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        System.out.println("Testing against " + mdAlg + "...");
        String sigAlg = SigTestUtil.generateSigAlg(type, mdAlg);
        Signature s = Signature.getInstance(sigAlg, provider);
        s.initSign(privateKey);
        s.update(data);
        byte[] sig = s.sign();
        s.initVerify(publicKey);
        s.update(data);
        boolean result = s.verify(sig);
        if (result == false) {
            throw new RuntimeException("Verification failed");
        }
    }

    private static void test(PrivateKey privateKey, PublicKey publicKey) throws Exception {

        int testSize = ((RSAPublicKey)publicKey).getModulus().bitLength();
        System.out.println("modulus size = " + testSize);

        Iterable<String> md_alg_pkcs15 =
            SigTestUtil.getDigestAlgorithms(SignatureType.RSA, testSize);
        md_alg_pkcs15.forEach(mdAlg -> {
            try {
                testSignature(SignatureType.RSA, mdAlg, privateKey, publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException |
                     SignatureException ex) {
                throw new RuntimeException(ex);
            }
        }
        );
    }

    // regression test for 4865198
    private static void testInvalidSignature(KeyPair kp1, KeyPair kp2) throws Exception {
        System.out.println("Testing signature with incorrect key...");
        Signature sig = Signature.getInstance("MD5withRSA", provider);
        sig.initSign(kp1.getPrivate());
        byte[] data = new byte[100];
        sig.update(data);
        byte[] signature = sig.sign();
        sig.initVerify(kp1.getPublic());
        sig.update(data);
        if (sig.verify(signature) == false) {
            throw new Exception("verification failed");
        }
        sig.initVerify(kp2.getPublic());
        sig.update(data);
        // verify needs to return false and not throw an Exception
        try {
            if (sig.verify(signature)) {
                throw new Exception("verification unexpectedly succeeded");
            }
        } catch (SignatureException se) {
            // Yet another kind of failure, OK.
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        provider = Security.getProvider("SunRsaSign");
        data = new byte[2048];
        // keypair generation is very slow, test only a few short keys
        int[] keyLengths = {512, 512, 1024};
        BigInteger[] pubExps = {null, BigInteger.valueOf(3), null};
        KeyPair[] keyPairs = new KeyPair[3];
        new Random().nextBytes(data);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", provider);
        for (int i = 0; i < keyLengths.length; i++) {
            int len = keyLengths[i];
            BigInteger exp = pubExps[i];
            System.out.println("Generating " + len + " bit keypair...");
            if (exp == null) {
                kpg.initialize(len);
            } else {
                kpg.initialize(new RSAKeyGenParameterSpec(len, exp));
            }
            KeyPair kp = kpg.generateKeyPair();
            keyPairs[i] = kp;
            RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
            System.out.println(publicKey);
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey)kp.getPrivate();
            if (publicKey.getModulus().equals(privateKey.getModulus()) == false) {
                throw new Exception("Moduli do not match");
            }
            if (publicKey.getPublicExponent().equals(privateKey.getPublicExponent()) == false) {
                throw new Exception("Exponents do not match");
            }
            int keyLen = publicKey.getModulus().bitLength();
            if ((keyLen > len) || (keyLen < len - 1)) {
                throw new Exception("Incorrect key length: " + keyLen);
            }
            if (exp != null) {
                if (exp.equals(publicKey.getPublicExponent()) == false) {
                    throw new Exception("Incorrect exponent");
                }
            }
            test(privateKey, publicKey);
        }
        testInvalidSignature(keyPairs[0], keyPairs[1]);
        testInvalidSignature(keyPairs[0], keyPairs[2]);
        testInvalidSignature(keyPairs[2], keyPairs[0]);
        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }
}
