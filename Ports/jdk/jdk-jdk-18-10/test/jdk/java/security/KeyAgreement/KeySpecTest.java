/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8184359
 * @summary Standard tests on KeySpec, KeyFactory, KeyPairs and Keys.
 *  Arguments order <KeyExchangeAlgorithm> <Provider> <KeyGenAlgorithm> <Curve*>
 * @run main KeySpecTest DiffieHellman SunJCE DiffieHellman
 * @run main KeySpecTest ECDH SunEC EC
 * @run main KeySpecTest XDH SunEC XDH X25519
 * @run main KeySpecTest XDH SunEC XDH X448
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;

public class KeySpecTest {

    public static void main(String[] args) throws Exception {

        String kaAlgo = args[0];
        String provider = args[1];
        String kpgAlgo = args[2];
        KeyPair kp = genKeyPair(provider, kpgAlgo,
                (args.length > 3) ? args[3] : kpgAlgo);
        testKeySpecs(provider, kaAlgo, kpgAlgo, kp);
        testEncodedKeySpecs(provider, kaAlgo, kpgAlgo, kp);
    }

    /**
     * Generate keyPair based on KeyPairGenerator algorithm.
     */
    private static KeyPair genKeyPair(String provider, String kpgAlgo,
            String kpgInit) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(kpgAlgo, provider);
        switch (kpgInit) {
            case "DiffieHellman":
                kpg.initialize(512);
                break;
            case "EC":
                kpg.initialize(256);
                break;
            case "X25519":
                kpg.initialize(255);
                break;
            case "X448":
                kpg.initialize(448);
                break;
            default:
                throw new RuntimeException("Invalid Algo name " + kpgInit);
        }
        return kpg.generateKeyPair();
    }

    /**
     * Standard Test with Keys and the one generated through Spec.
     */
    private static void testKeySpecs(String provider, String kaAlgo,
            String kpgAlgo, KeyPair kp) throws Exception {

        KeyFactory kf = KeyFactory.getInstance(kpgAlgo, provider);
        // For each public KeySpec supported by KeyPairGenerator
        for (Class pubSpecType
                : getCompatibleKeySpecs(kpgAlgo, KeyType.PUBLIC)) {
            //For each private KeySpec supported by KeyPairGenerator
            for (Class priSpecType
                    : getCompatibleKeySpecs(kpgAlgo, KeyType.PRIVATE)) {
                // Transform original PublicKey through KeySpec
                KeySpec pubSpec = kf.getKeySpec(kp.getPublic(), pubSpecType);
                PublicKey pubKey = kf.generatePublic(pubSpec);
                // Transform original PrivateKey through KeySpec
                KeySpec priSpec = kf.getKeySpec(kp.getPrivate(), priSpecType);
                PrivateKey priKey = kf.generatePrivate(priSpec);

                // Test the keys are equal after transformation through KeySpec.
                testKeyEquals(kp, pubKey, priKey);
                // Test the keys are serializable.
                testSerialize(kp);
                // Test Parameter name.
                if (!kaAlgo.equals("DiffieHellman")) {
                    testParamName(priSpec, pubSpec);
                }
                // Compare KeyAgreement secret generated from original keys
                // and by the keys after transformed through KeySpec.
                if (!Arrays.equals(getKeyAgreementSecret(provider, kaAlgo,
                        kp.getPublic(), kp.getPrivate()),
                        getKeyAgreementSecret(provider, kaAlgo,
                                pubKey, priKey))) {
                    throw new RuntimeException("KeyAgreement secret mismatch.");
                }
            }
        }
    }

    /**
     * Standard Test with Keys and the one generated from encoded bytes.
     */
    private static void testEncodedKeySpecs(String provider, String kaAlgo,
            String kpgAlgo, KeyPair kp) throws Exception {

        KeyFactory kf = KeyFactory.getInstance(kpgAlgo, provider);
        PKCS8EncodedKeySpec priSpec
                = new PKCS8EncodedKeySpec(kp.getPrivate().getEncoded());
        PrivateKey priKey = kf.generatePrivate(priSpec);

        X509EncodedKeySpec pubSpec
                = new X509EncodedKeySpec(kp.getPublic().getEncoded());
        PublicKey pubKey = kf.generatePublic(pubSpec);

        // Test the keys are equal after transformation through KeySpec.
        testKeyEquals(kp, pubKey, priKey);
        // Test the keys are serializable.
        testSerialize(kp);
        // Test Parameter name.
        if (!kaAlgo.equals("DiffieHellman")) {
            testParamName(priSpec, pubSpec);
        }
        // Compare KeyAgreement secret generated from original keys
        // and by the keys after transformed through KeySpec.
        if (!Arrays.equals(getKeyAgreementSecret(provider, kaAlgo,
                kp.getPublic(), kp.getPrivate()),
                getKeyAgreementSecret(provider, kaAlgo, pubKey, priKey))) {
            throw new RuntimeException("KeyAgreement secret mismatch.");
        }
    }

    private enum KeyType {
        PUBLIC, PRIVATE;
    }

    /**
     * Provides Compatible KeySpec Type list for KeyPairGenerator algorithm.
     */
    private static List<Class> getCompatibleKeySpecs(String kpgAlgo,
            KeyType type) {

        List<Class> specs = new ArrayList<>();
        switch (kpgAlgo) {
            case "DiffieHellman":
                if (type == KeyType.PUBLIC) {
                    return Arrays.asList(X509EncodedKeySpec.class,
                            DHPublicKeySpec.class);
                } else {
                    return Arrays.asList(PKCS8EncodedKeySpec.class,
                            DHPrivateKeySpec.class);
                }
            case "EC":
                if (type == KeyType.PUBLIC) {
                    return Arrays.asList(X509EncodedKeySpec.class,
                            ECPublicKeySpec.class);
                } else {
                    return Arrays.asList(PKCS8EncodedKeySpec.class,
                            ECPrivateKeySpec.class);
                }
            case "XDH":
                if (type == KeyType.PUBLIC) {
                    return Arrays.asList(X509EncodedKeySpec.class,
                            XECPublicKeySpec.class);
                } else {
                    return Arrays.asList(PKCS8EncodedKeySpec.class,
                            XECPrivateKeySpec.class);
                }
        }
        return specs;
    }

    /**
     * Generate KeyAgreement Secret.
     */
    private static byte[] getKeyAgreementSecret(String provider, String kaAlgo,
            PublicKey pubKey, PrivateKey priKey) throws Exception {

        KeyAgreement ka = KeyAgreement.getInstance(kaAlgo, provider);
        ka.init(priKey);
        ka.doPhase(pubKey, true);
        return ka.generateSecret();
    }

    /**
     * Compare original KeyPair with transformed ones.
     */
    private static void testKeyEquals(KeyPair kp, PublicKey pubKey,
            PrivateKey priKey) {

        if (!kp.getPrivate().equals(priKey)
                && kp.getPrivate().hashCode() != priKey.hashCode()) {
            throw new RuntimeException("PrivateKey is not equal with PrivateKey"
                    + " generated through KeySpec");
        }
        if (!kp.getPublic().equals(pubKey)
                && kp.getPublic().hashCode() != pubKey.hashCode()) {
            throw new RuntimeException("PublicKey is not equal with PublicKey"
                    + " generated through KeySpec");
        }
    }

    /**
     * Compare the parameter names of Public/Private KeySpec from a pair.
     */
    private static void testParamName(KeySpec priSpec, KeySpec pubSpec) {

        if (priSpec instanceof XECPrivateKeySpec
                && pubSpec instanceof XECPublicKeySpec) {
            if (((NamedParameterSpec) ((XECPrivateKeySpec) priSpec)
                    .getParams()).getName()
                    != ((NamedParameterSpec) ((XECPublicKeySpec) pubSpec)
                            .getParams()).getName()) {
                throw new RuntimeException("Curve name mismatch found");
            }
        }
    }

    /**
     * Test serialization of KeyPair and Keys it holds.
     */
    private static void testSerialize(KeyPair keyPair) throws Exception {

        // Verify Serialized PrivateKey instance.
        if (!keyPair.getPrivate().equals(
                deserializedCopy(keyPair.getPrivate(), PrivateKey.class))) {
            throw new RuntimeException("PrivateKey is not equal with PrivateKey"
                    + " generated through Serialization");
        }
        // Verify Serialized PublicKey instance.
        if (!keyPair.getPublic().equals(
                deserializedCopy(keyPair.getPublic(), PublicKey.class))) {
            throw new RuntimeException("PublicKey is not equal with PublicKey"
                    + " generated through Serialization");
        }
        // Verify Serialized KeyPair instance.
        KeyPair copy = deserializedCopy(keyPair, KeyPair.class);
        if (!keyPair.getPrivate().equals(copy.getPrivate())) {
            throw new RuntimeException("PrivateKey is not equal with PrivateKey"
                    + " generated through Serialized KeyPair");
        }
        if (!keyPair.getPublic().equals(copy.getPublic())) {
            throw new RuntimeException("PublicKey is not equal with PublicKey"
                    + " generated through Serialized KeyPair");
        }
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
