/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.*;
import java.util.stream.IntStream;
import static javax.crypto.Cipher.PRIVATE_KEY;
import static javax.crypto.Cipher.PUBLIC_KEY;

import jdk.test.lib.Asserts;
import jdk.test.lib.SigTestUtil;
import static jdk.test.lib.SigTestUtil.SignatureType;

/**
 * @test
 * @bug 8044199 8146293 8163498
 * @summary Ensure keys created from KeyFactory::getKeySpec and from constructors
 *          are equal.
 *          Create a signature for RSA and get its signed data. re-initiate
 *          the signature with the public key. The signature can be verified
 *          by acquired signed data.
 * @library /test/lib ../tools/keytool/fakegen
 * @build jdk.test.lib.SigTestUtil
 * @build java.base/sun.security.rsa.RSAKeyPairGenerator
 * @run main SignatureTest 512
 * @run main SignatureTest 768
 * @run main SignatureTest 1024
 * @run main SignatureTest 2048
 * @run main/timeout=240 SignatureTest 4096
 * @run main/timeout=240 SignatureTest 5120
 * @run main/timeout=480 SignatureTest 6144
 */
public class SignatureTest {
    /**
     * ALGORITHM name, fixed as RSA.
     */
    private static final String KEYALG = "RSA";

    /**
     * JDK default RSA Provider.
     */
    private static final String PROVIDER = "SunRsaSign";

    /**
     * How much times signature updated.
     */
    private static final int UPDATE_TIMES_FIFTY = 50;

    /**
     * How much times signature initial updated.
     */
    private static final int UPDATE_TIMES_HUNDRED = 100;

    public static void main(String[] args) throws Exception {
        int keySize = Integer.parseInt(args[0]);
        Iterable<String> md_alg_pkcs15 =
            SigTestUtil.getDigestAlgorithms(SignatureType.RSA, keySize);

        Iterable<String> md_alg_pss =
            SigTestUtil.getDigestAlgorithms(SignatureType.RSASSA_PSS, keySize);

        byte[] data = new byte[100];
        IntStream.range(0, data.length).forEach(j -> {
            data[j] = (byte) j;
        });

        // create a key pair
        KeyPair kpair = generateKeys(KEYALG, keySize);
        Key[] privs = manipulateKey(PRIVATE_KEY, kpair.getPrivate());
        Key[] pubs = manipulateKey(PUBLIC_KEY, kpair.getPublic());

        test(SignatureType.RSA, md_alg_pkcs15, privs, pubs, data);
        test(SignatureType.RSASSA_PSS, md_alg_pss, privs, pubs, data);
    }

    private static void test(SignatureType type, Iterable<String> digestAlgs,
            Key[] privs, Key[] pubs, byte[] data) throws RuntimeException {

        // For signature algorithm, create and verify a signature
        Arrays.stream(privs).forEach(priv
                -> Arrays.stream(pubs).forEach(pub
                -> digestAlgs.forEach(digestAlg -> {
            try {
                AlgorithmParameterSpec sigParams =
                    SigTestUtil.generateDefaultParameter(type, digestAlg);
                String sigAlg = SigTestUtil.generateSigAlg(type, digestAlg);
                checkSignature(data, (PublicKey) pub, (PrivateKey) priv,
                        sigAlg, sigParams);
            } catch (NoSuchAlgorithmException | InvalidKeyException |
                    SignatureException | NoSuchProviderException |
                    InvalidAlgorithmParameterException ex) {
                throw new RuntimeException(ex);
            }
        }
        )));
    }

    private static KeyPair generateKeys(String keyalg, int size)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyalg);
        kpg.initialize(size);
        return kpg.generateKeyPair();
    }

    private static Key[] manipulateKey(int type, Key key)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeyFactory kf = KeyFactory.getInstance(KEYALG, PROVIDER);

        switch (type) {
            case PUBLIC_KEY:
                try {
                    kf.getKeySpec(key, RSAPrivateKeySpec.class);
                    throw new RuntimeException("Expected InvalidKeySpecException "
                            + "not thrown");
                } catch (InvalidKeySpecException expected) {
                }

                RSAPublicKeySpec pubKeySpec1 = kf.getKeySpec(key, RSAPublicKeySpec.class);
                RSAPublicKeySpec pubKeySpec2 = new RSAPublicKeySpec(
                        ((RSAPublicKey) key).getModulus(),
                        ((RSAPublicKey) key).getPublicExponent());

                Asserts.assertTrue(keySpecEquals(pubKeySpec1, pubKeySpec2),
                        "Both RSAPublicKeySpec should be equal");

                X509EncodedKeySpec x509KeySpec1 = kf.getKeySpec(key, X509EncodedKeySpec.class);
                X509EncodedKeySpec x509KeySpec2 = new X509EncodedKeySpec(key.getEncoded());

                Asserts.assertTrue(encodedKeySpecEquals(x509KeySpec1, x509KeySpec2),
                        "Both X509EncodedKeySpec should be equal");

                return new Key[]{
                        key,
                        kf.generatePublic(pubKeySpec1),
                        kf.generatePublic(x509KeySpec1)
                };
            case PRIVATE_KEY:
                try {
                    kf.getKeySpec(key, RSAPublicKeySpec.class);
                    throw new RuntimeException("Expected InvalidKeySpecException"
                            + " not thrown");
                } catch (InvalidKeySpecException expected) {
                }
                RSAPrivateKeySpec privKeySpec1 = kf.getKeySpec(key, RSAPrivateKeySpec.class);
                RSAPrivateKeySpec privKeySpec2 = new RSAPrivateKeySpec(
                        ((RSAPrivateKey) key).getModulus(),
                        ((RSAPrivateKey) key).getPrivateExponent());

                Asserts.assertTrue(keySpecEquals(privKeySpec1, privKeySpec2),
                        "Both RSAPrivateKeySpec should be equal");

                PKCS8EncodedKeySpec pkcsKeySpec1 = kf.getKeySpec(key, PKCS8EncodedKeySpec.class);
                PKCS8EncodedKeySpec pkcsKeySpec2 = new PKCS8EncodedKeySpec(key.getEncoded());

                Asserts.assertTrue(encodedKeySpecEquals(pkcsKeySpec1, pkcsKeySpec2),
                        "Both PKCS8EncodedKeySpec should be equal");

                return new Key[]{
                        key,
                        kf.generatePrivate(privKeySpec1),
                        kf.generatePrivate(pkcsKeySpec1)
                };
        }
        throw new RuntimeException("We shouldn't reach here");
    }

    private static void checkSignature(byte[] data, PublicKey pub,
            PrivateKey priv, String sigAlg, AlgorithmParameterSpec sigParams)
            throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        System.out.println("Testing " + sigAlg);
        Signature sig = Signature.getInstance(sigAlg, PROVIDER);
        sig.setParameter(sigParams);

        sig.initSign(priv);
        for (int i = 0; i < UPDATE_TIMES_HUNDRED; i++) {
            sig.update(data);
        }
        byte[] signedData = sig.sign();

        // Make sure signature verifies with original data
        sig.setParameter(sigParams);
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES_HUNDRED; i++) {
            sig.update(data);
        }
        if (!sig.verify(signedData)) {
            throw new RuntimeException("Failed to verify " + sigAlg
                    + " signature");
        }

        // Make sure signature does NOT verify when the original data
        // has changed
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES_FIFTY; i++) {
            sig.update(data);
        }

        if (sig.verify(signedData)) {
            throw new RuntimeException("Failed to detect bad " + sigAlg
                    + " signature");
        }
    }

    private static boolean keySpecEquals(RSAPublicKeySpec spec1, RSAPublicKeySpec spec2) {
        return spec1.getModulus().equals(spec2.getModulus())
                && spec1.getPublicExponent().equals(spec2.getPublicExponent())
                && Objects.equals(spec1.getParams(), spec2.getParams());
    }

    private static boolean keySpecEquals(RSAPrivateKeySpec spec1, RSAPrivateKeySpec spec2) {
        return spec1.getModulus().equals(spec2.getModulus())
                && spec1.getPrivateExponent().equals(spec2.getPrivateExponent())
                && Objects.equals(spec1.getParams(), spec2.getParams());
    }

    private static boolean encodedKeySpecEquals(EncodedKeySpec spec1, EncodedKeySpec spec2) {
        return Objects.equals(spec1.getAlgorithm(), spec2.getAlgorithm())
                && spec1.getFormat().equals(spec2.getFormat())
                && Arrays.equals(spec1.getEncoded(), spec2.getEncoded());
    }
}
