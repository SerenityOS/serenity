/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import static javax.crypto.Cipher.PRIVATE_KEY;
import static javax.crypto.Cipher.PUBLIC_KEY;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * @test @bug 8242335
 * @summary Test RSASSA-PSS serialized keys
 * @run main SerializedPSSKey
 */
public class SerializedPSSKey {

    private static final String ALGO = "RSASSA-PSS";
    private static final String OID = "1.2.840.113549.1.1.10";
    private static final String PROVIDER = "SunRsaSign";
    private static final int KEY_SIZE = 2048;
    private static final byte[] DATA = "Test".getBytes();
    /**
     * Digest algorithms to test w/ RSASSA-PSS signature algorithms
     */
    private static final String[] DIGEST_ALG = {
        "SHA-1", "SHA-224", "SHA-256", "SHA-384",
        "SHA-512", "SHA-512/224", "SHA-512/256"
    };

    public static void main(String[] args) throws Exception {

        for (String algo : new String[]{ALGO, OID}) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo, PROVIDER);

            // Algorithm-Independent Initialization
            kpg.initialize(KEY_SIZE);
            KeyPair kp = kpg.generateKeyPair();
            test(algo, kp, null);
            for (String digest : DIGEST_ALG) {
                PSSParameterSpec params = genPSSParameter(digest, KEY_SIZE);
                // RSAKeyGenParameterSpec#1 Initialization
                kpg.initialize(new RSAKeyGenParameterSpec(KEY_SIZE,
                        RSAKeyGenParameterSpec.F4, params));
                KeyPair kp2 = kpg.generateKeyPair();
                test(algo, kp2, params);
            }
            System.out.println("Successful with : " + algo);
        }
    }

    private static void test(String algo, KeyPair orig, PSSParameterSpec params)
            throws Exception {

        Key[] privs = manipulateKey(algo, PRIVATE_KEY, orig.getPrivate());
        Key[] pubs = manipulateKey(algo, PUBLIC_KEY, orig.getPublic());
        for (Key pri : privs) {
            for (Key pub : pubs) {
                testSerialize(orig, new KeyPair(
                        (PublicKey) pub, (PrivateKey) pri));
                if (params != null) {
                    testSignature(algo, (PublicKey) pub,
                            (PrivateKey) pri, params);
                }
            }
        }
    }

    private static void testSignature(String algo, PublicKey pub,
            PrivateKey priv, PSSParameterSpec params) throws Exception {

        Signature sig = Signature.getInstance(algo, PROVIDER);
        sig.setParameter(params);
        sig.initSign(priv);
        sig.update(DATA);
        byte[] signature = sig.sign();

        sig.initVerify(pub);
        sig.update(DATA);
        if (!sig.verify(signature)) {
            throw new RuntimeException("Signature verification failed");
        }
        // Re-verify the signature with another Signature instance
        Signature sig1 = Signature.getInstance(algo, PROVIDER);
        sig1.setParameter(params);
        sig1.initVerify(pub);
        sig1.update(DATA);
        if (!sig1.verify(signature)) {
            throw new RuntimeException("Signature verification failed");
        }
    }

    private static Key[] manipulateKey(String algo, int type, Key key)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchProviderException, InvalidKeyException {

        KeyFactory kf = KeyFactory.getInstance(algo, PROVIDER);
        switch (type) {
            case PUBLIC_KEY:
                return new Key[]{
                    kf.generatePublic(kf.getKeySpec(key, RSAPublicKeySpec.class)),
                    kf.generatePublic(new X509EncodedKeySpec(key.getEncoded())),
                    kf.generatePublic(new RSAPublicKeySpec(
                    ((RSAPublicKey) key).getModulus(),
                    ((RSAPublicKey) key).getPublicExponent(),
                    ((RSAPublicKey) key).getParams())),
                    kf.translateKey(key)
                };
            case PRIVATE_KEY:
                RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) key;
                return new Key[]{
                    kf.generatePrivate(
                    kf.getKeySpec(key, RSAPrivateCrtKeySpec.class)),
                    kf.generatePrivate(new PKCS8EncodedKeySpec(key.getEncoded())),
                    kf.generatePrivate(new RSAPrivateCrtKeySpec(
                    crtKey.getModulus(),
                    crtKey.getPublicExponent(),
                    crtKey.getPrivateExponent(),
                    crtKey.getPrimeP(),
                    crtKey.getPrimeQ(),
                    crtKey.getPrimeExponentP(),
                    crtKey.getPrimeExponentQ(),
                    crtKey.getCrtCoefficient(),
                    crtKey.getParams()
                    )),
                    kf.translateKey(key)
                };
        }
        throw new RuntimeException("We shouldn't reach here");
    }

    private static PSSParameterSpec genPSSParameter(String digest, int keySize)
            throws NoSuchAlgorithmException {

        int dgLen = MessageDigest.getInstance(digest).getDigestLength();
        // pick a salt length based on the key length and digestAlgo
        int saltLength = keySize / 8 - dgLen - 2;
        if (saltLength < 0) {
            System.out.printf("keysize: %s, digestLen: %s%n", keySize / 8, dgLen);
            return null;
        }
        return new PSSParameterSpec(digest, "MGF1", new MGF1ParameterSpec(digest),
                saltLength, PSSParameterSpec.TRAILER_FIELD_BC);
    }

    /**
     * Compare original KeyPair with transformed ones.
     */
    private static void testKeyEquals(KeyPair orig, PublicKey pubKey,
            PrivateKey priKey) {

        if (!orig.getPrivate().equals(priKey)
                && orig.getPrivate().hashCode() != priKey.hashCode()
                && !Arrays.equals(orig.getPrivate().getEncoded(),
                        priKey.getEncoded())) {
            throw new RuntimeException(
                    "PrivateKey is not equal with transformed one");
        }
        if (!orig.getPublic().equals(pubKey)
                && orig.getPublic().hashCode() != pubKey.hashCode()
                && !Arrays.equals(orig.getPublic().getEncoded(),
                        pubKey.getEncoded())) {
            throw new RuntimeException(
                    "PublicKey is not equal with transformed one");
        }
    }

    /**
     * Test serialization of KeyPair and Keys it holds.
     */
    private static void testSerialize(KeyPair orig, KeyPair transformed)
            throws Exception {

        testKeyEquals(orig, transformed.getPublic(), transformed.getPrivate());
        PrivateKey serializedPrivate = deserializedCopy(transformed.getPrivate(),
                PrivateKey.class);
        PublicKey serializedPublic = deserializedCopy(transformed.getPublic(),
                PublicKey.class);
        testKeyEquals(orig, serializedPublic, serializedPrivate);
        // Verify Serialized KeyPair instance.
        KeyPair copy = deserializedCopy(transformed, KeyPair.class);
        testKeyEquals(orig, copy.getPublic(), copy.getPrivate());
    }

    private static <T extends Object> T deserializedCopy(T orig, Class<T> type)
            throws IOException, ClassNotFoundException {
        return deserialize(serialize(orig), type);
    }

    /**
     * Deserialize the Key object.
     */
    private static <T extends Object> T deserialize(byte[] serialized,
            Class<T> type) throws IOException, ClassNotFoundException {

        T key = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            key = (T) ois.readObject();
        }
        return key;
    }

    /**
     * Serialize the given Key object.
     */
    private static <T extends Object> byte[] serialize(T key)
            throws IOException {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(key);
            return bos.toByteArray();
        }
    }

}
