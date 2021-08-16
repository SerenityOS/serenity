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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.stream.IntStream;
import static javax.crypto.Cipher.PRIVATE_KEY;
import static javax.crypto.Cipher.PUBLIC_KEY;

/**
 * @test
 * @bug 8146293 8238448 8172366
 * @summary Create a signature for RSASSA-PSS and get its signed data.
 *          re-initiate the signature with the public key. The signature
 *          can be verified by acquired signed data.
 * @run main SignatureTest2 768
 * @run main SignatureTest2 1024
 * @run main SignatureTest2 1025
 * @run main SignatureTest2 2048
 * @run main SignatureTest2 2049
 * @run main/timeout=240 SignatureTest2 4096
 */
public class SignatureTest2 {
    /**
     * ALGORITHM name, fixed as RSA.
     */
    private static final String KEYALG = "RSASSA-PSS";

    /**
     * JDK default RSA Provider.
     */
    private static final String PROVIDER = "SunRsaSign";

    /**
     * How much times signature updated.
     */
    private static final int UPDATE_TIMES_TWO = 2;

    /**
     * How much times signature initial updated.
     */
    private static final int UPDATE_TIMES_TEN = 10;

    /**
     * Digest algorithms to test w/ RSASSA-PSS signature algorithms
     */
    private static final String[] DIGEST_ALG = {
        "SHA-1", "SHA-224", "SHA-256", "SHA-384",
        "SHA-512", "SHA-512/224", "SHA-512/256",
        "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"
    };

    private static final String SIG_ALG = "RSASSA-PSS";

    private static PSSParameterSpec genPSSParameter(String digestAlgo,
        int digestLen, int keySize) {
        // pick a salt length based on the key length and digestAlgo
        int saltLength = keySize/8 - digestLen - 2;
        if (saltLength < 0) {
            System.out.println("keysize: " + keySize/8 + ", digestLen: " + digestLen);
            return null;
        }
        return new PSSParameterSpec(digestAlgo, "MGF1",
            new MGF1ParameterSpec(digestAlgo), saltLength, 1);
    }

    public static void main(String[] args) throws Exception {
        final int testSize = Integer.parseInt(args[0]);

        byte[] data = new byte[100];
        IntStream.range(0, data.length).forEach(j -> {
            data[j] = (byte) j;
        });

        // create a key pair
        KeyPair kpair = generateKeys(KEYALG, testSize);
        Key[] privs = manipulateKey(PRIVATE_KEY, kpair.getPrivate());
        Key[] pubs = manipulateKey(PUBLIC_KEY, kpair.getPublic());

        // For messsage digest algorithm, create and verify a RSASSA-PSS signature
        Arrays.stream(privs).forEach(priv
                -> Arrays.stream(pubs).forEach(pub
                -> Arrays.stream(DIGEST_ALG).forEach(testAlg -> {
            checkSignature(data, (PublicKey) pub, (PrivateKey) priv,
                    testAlg, testSize);
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
                return new Key[]{
                    kf.generatePublic(kf.getKeySpec(key, RSAPublicKeySpec.class)),
                    kf.generatePublic(new X509EncodedKeySpec(key.getEncoded())),
                    kf.generatePublic(new RSAPublicKeySpec(
                    ((RSAPublicKey) key).getModulus(),
                    ((RSAPublicKey) key).getPublicExponent()))
                };
            case PRIVATE_KEY:
                return new Key[]{
                    kf.generatePrivate(kf.getKeySpec(key,
                    RSAPrivateKeySpec.class)),
                    kf.generatePrivate(new PKCS8EncodedKeySpec(
                    key.getEncoded())),
                    kf.generatePrivate(new RSAPrivateKeySpec(((RSAPrivateKey) key).getModulus(),
                    ((RSAPrivateKey) key).getPrivateExponent()))
                };
        }
        throw new RuntimeException("We shouldn't reach here");
    }

    private static void checkSignature(byte[] data, PublicKey pub,
            PrivateKey priv, String digestAlg, int keySize) throws RuntimeException {
        try {
            Signature sig = Signature.getInstance(SIG_ALG, PROVIDER);
            int digestLen = MessageDigest.getInstance(digestAlg).getDigestLength();
            PSSParameterSpec params = genPSSParameter(digestAlg, digestLen, keySize);
            if (params == null) {
                System.out.println("Skip test due to short key size");
                return;
            }
            sig.setParameter(params);
            sig.initSign(priv);
            for (int i = 0; i < UPDATE_TIMES_TEN; i++) {
                sig.update(data);
            }
            byte[] signedDataTen = sig.sign();

            // Make sure signature can be generated without re-init
            sig.update(data);
            byte[] signedDataOne = sig.sign();

            // Make sure signature verifies with original data
            System.out.println("Verify using params " + sig.getParameters());
            sig.initVerify(pub);
            sig.setParameter(params);
            for (int i = 0; i < UPDATE_TIMES_TEN; i++) {
                sig.update(data);
            }
            if (!sig.verify(signedDataTen)) {
                throw new RuntimeException("Signature verification test#1 failed w/ "
                    + digestAlg);
            }

            // Make sure signature can verify without re-init
            sig.update(data);
            if (!sig.verify(signedDataOne)) {
                throw new RuntimeException("Signature verification test#2 failed w/ "
                    + digestAlg);
            }

            // Make sure signature does NOT verify when the original data
            // has changed
            for (int i = 0; i < UPDATE_TIMES_TWO; i++) {
                sig.update(data);
            }

            if (sig.verify(signedDataOne)) {
                throw new RuntimeException("Bad signature accepted w/ "
                    + digestAlg);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException |
                 SignatureException | NoSuchProviderException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
