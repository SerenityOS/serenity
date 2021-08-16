/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

/**
 * @test
 * @bug 8080462 8226651 8242332
 * @summary Ensure that PSS key and params check are implemented properly
 *         regardless of call sequence
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main KeyAndParamCheckForPSS
 */
public class KeyAndParamCheckForPSS extends PKCS11Test {

    /**
     * ALGORITHM name, fixed as RSA for PKCS11
     */
    private static final String KEYALG = "RSA";
    private static final String SIGALG = "RSASSA-PSS";

    public static void main(String[] args) throws Exception {
        main(new KeyAndParamCheckForPSS(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        Signature sig;
        try {
            sig = Signature.getInstance(SIGALG, p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Skip testing RSASSA-PSS" +
                " due to no support");
            return;
        }

        // NOTE: key length >= (digest length + 2) in bytes
        // otherwise, even salt length = 0 would not work
        runTest(p, 1024, "SHA-256", "SHA-256");
        runTest(p, 1024, "SHA-256", "SHA-384");
        runTest(p, 1024, "SHA-256", "SHA-512");
        runTest(p, 1024, "SHA-384", "SHA-256");
        runTest(p, 1024, "SHA-384", "SHA-384");
        runTest(p, 1024, "SHA-384", "SHA-512");
        runTest(p, 1040, "SHA-512", "SHA-256");
        runTest(p, 1040, "SHA-512", "SHA-384");
        runTest(p, 1040, "SHA-512", "SHA-512");
        runTest(p, 1024, "SHA3-256", "SHA3-256");
        runTest(p, 1024, "SHA3-256", "SHA3-384");
        runTest(p, 1024, "SHA3-256", "SHA3-512");
        runTest(p, 1024, "SHA3-384", "SHA3-256");
        runTest(p, 1024, "SHA3-384", "SHA3-384");
        runTest(p, 1024, "SHA3-384", "SHA3-512");
        runTest(p, 1040, "SHA3-512", "SHA3-256");
        runTest(p, 1040, "SHA3-512", "SHA3-384");
        runTest(p, 1040, "SHA3-512", "SHA3-512");
    }

    private void runTest(Provider p, int keySize, String hashAlg,
            String mgfHashAlg) throws Exception {

        // skip further test if this provider does not support hashAlg or
        // mgfHashAlg
        try {
            MessageDigest.getInstance(hashAlg, p);
            MessageDigest.getInstance(mgfHashAlg, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("No support for " + hashAlg + ", skip");
            return;
        }

        System.out.println("Testing [" + keySize + " " + hashAlg + "]");

        // create a key pair with the supplied size
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEYALG, p);
        kpg.initialize(keySize);
        KeyPair kp = kpg.generateKeyPair();

        int bigSaltLen = keySize/8 - 14;
        AlgorithmParameterSpec paramsBad = new PSSParameterSpec(hashAlg,
            "MGF1", new MGF1ParameterSpec(mgfHashAlg), bigSaltLen, 1);
        AlgorithmParameterSpec paramsGood = new PSSParameterSpec(hashAlg,
            "MGF1", new MGF1ParameterSpec(mgfHashAlg), 0, 1);

        PrivateKey priv = kp.getPrivate();
        PublicKey pub = kp.getPublic();

        // test#1 - setParameter then initSign
        Signature sig = Signature.getInstance("RSASSA-PSS", p);
        sig.setParameter(paramsBad);
        try {
            sig.initSign(priv);
            throw new RuntimeException("Expected IKE not thrown");
        } catch (InvalidKeyException ike) {
            System.out.println("test#1: got expected IKE");
        }

        sig.setParameter(paramsGood);
        sig.initSign(priv);
        System.out.println("test#1: pass");

        // test#2 - setParameter then initVerify
        sig = Signature.getInstance("RSASSA-PSS", p);
        sig.setParameter(paramsBad);
        try {
            sig.initVerify(pub);
            throw new RuntimeException("Expected IKE not thrown");
        } catch (InvalidKeyException ike) {
            System.out.println("test#2: got expected IKE");
        }

        sig.setParameter(paramsGood);
        sig.initVerify(pub);

        System.out.println("test#2: pass");

        // test#3 - initSign, then setParameter
        sig = Signature.getInstance("RSASSA-PSS", p);
        sig.initSign(priv);
        try {
            sig.setParameter(paramsBad);
            throw new RuntimeException("Expected IAPE not thrown");
        } catch (InvalidAlgorithmParameterException iape) {
            System.out.println("test#3: got expected IAPE");
        }

        sig.setParameter(paramsGood);
        System.out.println("test#3: pass");

        // test#4 - initVerify, then setParameter
        sig = Signature.getInstance("RSASSA-PSS", p);
        sig.initVerify(pub);
        try {
            sig.setParameter(paramsBad);
            throw new RuntimeException("Expected IAPE not thrown");
        } catch (InvalidAlgorithmParameterException iape) {
            System.out.println("test#4: got expected IAPE");
        }

        sig.setParameter(paramsGood);
        System.out.println("test#4: pass");
    }
}
