/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.EdDSAParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/*
 * @test
 * @bug 8209632
 * @summary Test Signature with variation of serialized EDDSA Keys.
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main EdDSATest
 */
public class EdDSATest {

    private static final String EDDSA = "EdDSA";
    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String OIDN25519 = "1.3.101.112";
    private static final String OID25519 = "OID.1.3.101.112";
    private static final String OIDN448 = "1.3.101.113";
    private static final String OID448 = "OID.1.3.101.113";
    private static final String PROVIDER = "SunEC";
    private static final byte[] MSG = "TEST".getBytes();
    private static final SecureRandom S_RND = new SecureRandom(new byte[]{0x1});

    public static void main(String[] args) throws Exception {

        for (boolean random : new boolean[]{true, false}) {

            // Default Parameter
            test(PROVIDER, EDDSA, null, random);
            test(PROVIDER, ED25519, null, random);
            test(PROVIDER, ED448, null, random);

            // With named parameter
            test(PROVIDER, EDDSA, ED25519, random);
            test(PROVIDER, ED25519, ED25519, random);
            test(PROVIDER, OIDN25519, ED25519, random);
            test(PROVIDER, OID25519, ED25519, random);
            test(PROVIDER, ED448, ED448, random);
            test(PROVIDER, OIDN448, ED448, random);
            test(PROVIDER, OID448, ED448, random);

            // With size parameter
            test(PROVIDER, EDDSA, 255, random);
            test(PROVIDER, ED25519, 255, random);
            test(PROVIDER, OIDN25519, 255, random);
            test(PROVIDER, OID25519, 255, random);
            test(PROVIDER, ED448, 448, random);
            test(PROVIDER, OIDN448, 448, random);
            test(PROVIDER, OID448, 448, random);
        }
    }

    // Test signature using a KeyPair and the corresponding transformed one.
    private static void test(String provider, String name, Object param,
            boolean random) throws Exception {

        System.out.printf("Case Algo:%s, Param:%s, Intitiate with random:%s%n",
                name, param, random);
        KeyPair origkp = genKeyPair(provider, name, param, random);
        testSignature(provider, name, origkp, origkp);
        NamedParameterSpec namedSpec = namedParamSpec(name);
        // Test all possible transformed private/public keys
        for (Key priKey : manipulateKey(provider, name, PRIVATE_KEY,
                origkp.getPrivate(), namedSpec)) {
            for (Key pubKey : manipulateKey(provider, name, PUBLIC_KEY,
                    origkp.getPublic(), namedSpec)) {
                EdECPrivateKey pri = (EdECPrivateKey) priKey;
                EdECPublicKey pub = (EdECPublicKey) pubKey;
                // Test the keys are serializable.
                testSerialize(origkp, new KeyPair(pub, pri));
                // Test signature
                testSignature(provider, name, origkp, new KeyPair(pub, pri));
            }
        }
        System.out.println("Passed.");
    }

    private static KeyPair genKeyPair(String provider, String name,
            Object param, boolean random) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, provider);
        if (random) {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param, S_RND);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param), S_RND);
            }
        } else {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param));
            }
        }
        equals(kpg.getProvider().getName(), provider);
        equals(kpg.getAlgorithm(), name);
        return kpg.generateKeyPair();
    }

    private static NamedParameterSpec namedParamSpec(String algo) {
        NamedParameterSpec namedSpec = switch (algo) {
            case EDDSA
                , OIDN25519, OID25519 -> new NamedParameterSpec(ED25519);
            case OIDN448
                , OID448 -> new NamedParameterSpec(ED448);
            default->
                new NamedParameterSpec(algo);
        };
            return namedSpec;
    }

    private static Key[] manipulateKey(String provider, String name, int type,
            Key key, NamedParameterSpec namedSpec)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchProviderException, InvalidKeyException {

        KeyFactory kf = KeyFactory.getInstance(name, provider);
        switch (type) {
            case PUBLIC_KEY:
                return new Key[]{
                    kf.generatePublic(new X509EncodedKeySpec(key.getEncoded())),
                    kf.generatePublic(kf.getKeySpec(
                    key, EdECPublicKeySpec.class)),
                    kf.generatePublic(new EdECPublicKeySpec(namedSpec,
                    ((EdECPublicKey) key).getPoint())),
                    kf.translateKey(key)
                };
            case PRIVATE_KEY:
                return new Key[]{
                    kf.generatePrivate(new PKCS8EncodedKeySpec(key.getEncoded())),
                    kf.generatePrivate(
                    kf.getKeySpec(key, EdECPrivateKeySpec.class)),
                    kf.generatePrivate(new EdECPrivateKeySpec(namedSpec,
                    ((EdECPrivateKey) key).getBytes().get())),
                    kf.translateKey(key)
                };
        }
        throw new RuntimeException("We shouldn't reach here");
    }

    /**
     * Test Signature with a set of parameter combination.
     */
    private static void testSignature(String provider, String name,
            KeyPair origkp, KeyPair kp) throws Exception {

        signAndVerify(provider, name, origkp, kp, null);
        // Test Case with Pre-Hash enabled and disabled.
        for (boolean preHash : new boolean[]{true, false}) {
            signAndVerify(provider, name, origkp, kp,
                    new EdDSAParameterSpec(preHash));
            // Test Case with Context combined.
            for (byte[] context : new byte[][]{
                {}, "a".getBytes(), new byte[255]}) {
                signAndVerify(provider, name, origkp, kp,
                        new EdDSAParameterSpec(preHash, context));
            }
        }
    }

    private static void signAndVerify(String provider, String name,
            KeyPair origkp, KeyPair kp, EdDSAParameterSpec params)
            throws Exception {

        Signature sig = Signature.getInstance(name, provider);
        if (params != null) {
            sig.setParameter(params);
        }
        sig.initSign(origkp.getPrivate());
        sig.update(MSG);
        byte[] origSign = sig.sign();

        sig.update(MSG);
        byte[] computedSig = sig.sign();
        equals(origSign, computedSig);
        // EdDSA signatures size (64 and 114 bytes) for Ed25519 and Ed448.
        int expectedSigSize = edSignatureSize(name);
        equals(origSign.length, expectedSigSize);
        sig.initSign(kp.getPrivate());
        sig.update(MSG);
        equals(computedSig, sig.sign());
        // Use same signature instance to verify with transformed PublicKey.
        sig.initVerify(kp.getPublic());
        sig.update(MSG);
        if (!sig.verify(origSign)) {
            throw new RuntimeException(String.format("Signature did not verify"
                    + " for name:%s, prehash:%s, context:%s", name,
                    (params == null) ? null : params.isPrehash(),
                    (params == null) ? null : params.getContext().get()));
        }

        // Create a new signature to re-verify.
        sig = Signature.getInstance(name, provider);
        if (params != null) {
            sig.setParameter(params);
        }
        // Verify the signature with transformed PublicKey.
        sig.initVerify(kp.getPublic());
        sig.update(MSG);
        if (!sig.verify(origSign)) {
            throw new RuntimeException(String.format("Signature did not verify"
                    + " for name:%s, prehash:%s, context:%s",
                    name, (params == null) ? null : params.isPrehash(),
                    (params == null) ? null : params.getContext().get()));
        }
        equals(sig.getAlgorithm(), name);
        equals(sig.getProvider().getName(), provider);
    }

    private static int edSignatureSize(String algo) {
        int size = switch (algo) {
            case EDDSA
                , ED25519, OIDN25519, OID25519 -> 64;
            case ED448
                , OIDN448, OID448 -> 114;
            default->
                -1;
        };
            return size;
    }

    /**
     * Compare original KeyPair with transformed ones.
     */
    private static void testKeyEquals(KeyPair origkp, PublicKey pubKey,
            PrivateKey priKey) {

        if (!origkp.getPrivate().equals(priKey)
                && !Arrays.equals(origkp.getPrivate().getEncoded(),
                        priKey.getEncoded())
                && origkp.getPrivate().hashCode() != priKey.hashCode()) {
            throw new RuntimeException(
                    "PrivateKey is not equal with transformed one");
        }
        if (!origkp.getPublic().equals(pubKey)
                && !Arrays.equals(origkp.getPublic().getEncoded(),
                        pubKey.getEncoded())
                && origkp.getPublic().hashCode() != pubKey.hashCode()) {
            throw new RuntimeException(
                    "PublicKey is not equal with transformed one");
        }
    }

    /**
     * Test serialization of KeyPair and Keys.
     */
    private static void testSerialize(KeyPair origkp, KeyPair kp)
            throws Exception {

        testKeyEquals(origkp, kp.getPublic(), kp.getPrivate());
        PrivateKey priv = deserializedCopy(kp.getPrivate(), PrivateKey.class);
        PublicKey pub = deserializedCopy(kp.getPublic(), PublicKey.class);
        testKeyEquals(origkp, pub, priv);
        // Verify Serialized KeyPair instance.
        KeyPair copy = deserializedCopy(kp, KeyPair.class);
        testKeyEquals(origkp, copy.getPublic(), copy.getPrivate());
    }

    private static <T extends Object> T deserializedCopy(T origkp, Class<T> type)
            throws IOException, ClassNotFoundException {
        return deserialize(serialize(origkp), type);
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

    private static void equals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new RuntimeException(String.format("Actual: %s, Expected: %s",
                    actual, expected));
        }
    }

    private static void equals(byte[] actual, byte[] expected) {
        if (!Arrays.equals(actual, expected)) {
            throw new RuntimeException(String.format("Actual array: %s, "
                    + "Expected array:%s", HexFormat.of().withUpperCase().formatHex(actual),
                    HexFormat.of().withUpperCase().formatHex(expected)));
        }
    }
}
