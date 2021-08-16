/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.stream.IntStream;
import jdk.test.lib.SigTestUtil;
import static jdk.test.lib.SigTestUtil.SignatureType;
import static javax.crypto.Cipher.PRIVATE_KEY;
import static javax.crypto.Cipher.PUBLIC_KEY;

/**
 * @test
 * @bug 8146293 8238448
 * @summary Create a signature for RSASSA-PSS and get its signed data.
 *          re-initiate the signature with the public key. The signature
 *          can be verified by acquired signed data.
 * @library /test/lib
 * @build jdk.test.lib.SigTestUtil
 * @run main SignatureTestPSS 512
 * @run main SignatureTestPSS 768
 * @run main SignatureTestPSS 1024
 * @run main SignatureTestPSS 1025
 * @run main SignatureTestPSS 2048
 * @run main SignatureTestPSS 2049
 * @run main/timeout=240 SignatureTestPSS 4096
 * @run main/timeout=240 SignatureTestPSS 5120
 * @run main/timeout=480 SignatureTestPSS 6144
 */
public class SignatureTestPSS {
    /**
     * ALGORITHM name, fixed as RSASSA-PSS.
     */
    private static final String KEYALG = SignatureType.RSASSA_PSS.toString();

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
        int testSize = Integer.parseInt(args[0]);

        Iterable<String> md_alg_pss =
            SigTestUtil.getDigestAlgorithms(SignatureType.RSASSA_PSS, testSize);

        byte[] data = new byte[100];
        IntStream.range(0, data.length).forEach(j -> {
            data[j] = (byte) j;
        });

        // create a key pair
        KeyPair kpair = generateKeys(KEYALG, testSize);
        Key[] privs = manipulateKey(PRIVATE_KEY, kpair.getPrivate());
        Key[] pubs = manipulateKey(PUBLIC_KEY, kpair.getPublic());

        test(md_alg_pss, privs, pubs, data);
    }

    private static void test(Iterable<String> testAlgs, Key[] privs,
            Key[] pubs, byte[] data) throws RuntimeException {
        // For signature algorithm, create and verify a signature
        Arrays.stream(privs).forEach(priv
                -> Arrays.stream(pubs).forEach(pub
                -> testAlgs.forEach(testAlg -> {
            try {
                checkSignature(data, (PublicKey) pub, (PrivateKey) priv,
                        testAlg);
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
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchProviderException {
        KeyFactory kf = KeyFactory.getInstance(KEYALG, PROVIDER);
        switch (type) {
            case PUBLIC_KEY:
                try {
                    kf.getKeySpec(key, RSAPrivateKeySpec.class);
                    throw new RuntimeException("Expected InvalidKeySpecException"
                            + " not thrown");
                } catch (InvalidKeySpecException expected) {
                    System.out.println("Expected IKSE thrown for PublicKey");
                }
                return new Key[]{
                    kf.generatePublic(kf.getKeySpec(key, RSAPublicKeySpec.class)),
                    kf.generatePublic(new X509EncodedKeySpec(key.getEncoded())),
                    kf.generatePublic(new RSAPublicKeySpec(
                        ((RSAPublicKey)key).getModulus(),
                        ((RSAPublicKey)key).getPublicExponent(),
                        ((RSAPublicKey)key).getParams()))
                };
            case PRIVATE_KEY:
                try {
                    kf.getKeySpec(key, RSAPublicKeySpec.class);
                    throw new RuntimeException("Expected InvalidKeySpecException"
                            + " not thrown");
                } catch (InvalidKeySpecException expected) {
                    System.out.println("Expected IKSE thrown for PrivateKey");
                }
                return new Key[]{
                    kf.generatePrivate(kf.getKeySpec(key, RSAPrivateKeySpec.class)),
                    kf.generatePrivate(new PKCS8EncodedKeySpec(key.getEncoded())),
                    kf.generatePrivate(new RSAPrivateKeySpec(
                        ((RSAPrivateKey)key).getModulus(),
                        ((RSAPrivateKey)key).getPrivateExponent(),
                        ((RSAPrivateKey)key).getParams()))
                };
        }
        throw new RuntimeException("We shouldn't reach here");
    }

    private static void checkSignature(byte[] data, PublicKey pub,
            PrivateKey priv, String mdAlg) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        System.out.println("Testing against " + mdAlg);
        Signature sig = Signature.getInstance
            (SignatureType.RSASSA_PSS.toString(), PROVIDER);
        AlgorithmParameterSpec params =
            SigTestUtil.generateDefaultParameter(SignatureType.RSASSA_PSS, mdAlg);
        sig.setParameter(params);
        sig.initSign(priv);
        for (int i = 0; i < UPDATE_TIMES_HUNDRED; i++) {
            sig.update(data);
        }
        byte[] signedData = sig.sign();

        // Make sure signature verifies with original data
        // do we need to call sig.setParameter(params) again?
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES_HUNDRED; i++) {
            sig.update(data);
        }
        if (!sig.verify(signedData)) {
            throw new RuntimeException("Failed to verify signature");
        }

        // Make sure signature does NOT verify when the original data
        // has changed
        sig.initVerify(pub);
        for (int i = 0; i < UPDATE_TIMES_FIFTY; i++) {
            sig.update(data);
        }

        if (sig.verify(signedData)) {
            throw new RuntimeException("Failed to detect bad signature");
        }
    }
}
