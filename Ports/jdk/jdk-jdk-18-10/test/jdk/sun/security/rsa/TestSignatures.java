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
 * @bug 4853305 4963723 8146293
 * @summary Test signing/verifying using all the signature algorithms
 * @library /test/lib
 * @build jdk.test.lib.SigTestUtil
 * @run main TestSignatures
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.interfaces.*;

import jdk.test.lib.SigTestUtil;
import static jdk.test.lib.SigTestUtil.SignatureType;

public class TestSignatures {

    private final static String BASE = System.getProperty("test.src", ".");

    private static final char[] password = "test12".toCharArray();

    private static Provider provider;

    private static byte[] data;

    static KeyStore getKeyStore() throws Exception {
        InputStream in = new FileInputStream(new File(BASE, "rsakeys.ks"));
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(in, password);
        in.close();
        return ks;
    }

    private static void testSignature(String mdAlg, PrivateKey privateKey,
            PublicKey publicKey) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        System.out.println("Testing against " + mdAlg + "...");
        String sigAlg = SigTestUtil.generateSigAlg(SignatureType.RSA, mdAlg);
        Signature s = Signature.getInstance(sigAlg, provider);
        s.initSign(privateKey);
        s.update(data);
        byte[] sig = s.sign();
        s.initVerify(publicKey);
        s.update(data);
        boolean result;
        result = s.verify(sig);
        if (result == false) {
            throw new RuntimeException("Verification 1 failed");
        }
        s.update(data);
        result = s.verify(sig);
        if (result == false) {
            throw new RuntimeException("Verification 2 failed");
        }
        result = s.verify(sig);
        if (result == true) {
            throw new RuntimeException("Verification 3 succeeded");
        }
    }

    private static void test(PrivateKey privateKey, PublicKey publicKey)
            throws Exception {

        int testSize = ((RSAPublicKey)publicKey).getModulus().bitLength();
        System.out.println("modulus size = " + testSize);
        // work around a corner case where the key size is one bit short
        if ((testSize & 0x07) != 0) {
            testSize += (8 - (testSize & 0x07));
            System.out.println("adjusted modulus size = " + testSize);
        }
        Iterable<String> sign_alg_pkcs15 =
            SigTestUtil.getDigestAlgorithms(SignatureType.RSA, testSize);
        sign_alg_pkcs15.forEach(testAlg -> {
            try {
                testSignature(testAlg, privateKey, publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException |
                     SignatureException ex) {
                throw new RuntimeException(ex);
            }
        }
        );
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        provider = Security.getProvider("SunRsaSign");
        data = new byte[2048];
        new Random().nextBytes(data);
        KeyStore ks = getKeyStore();
        KeyFactory kf = KeyFactory.getInstance("RSA", provider);
        for (Enumeration e = ks.aliases(); e.hasMoreElements(); ) {
            String alias = (String)e.nextElement();
            if (ks.isKeyEntry(alias)) {
                System.out.println("* Key " + alias + "...");
                PrivateKey privateKey = (PrivateKey)ks.getKey(alias, password);
                PublicKey publicKey = ks.getCertificate(alias).getPublicKey();
                privateKey = (PrivateKey)kf.translateKey(privateKey);
                publicKey = (PublicKey)kf.translateKey(publicKey);
                test(privateKey, publicKey);
            }
        }
        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }
}
