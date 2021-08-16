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
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

import jdk.test.lib.Convert;

/*
 * @test
 * @bug 8209632
 * @summary Verify KeyLength for EDDSA, ED25519, ED448.
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main EdDSAKeySize
 */
public class EdDSAKeySize {

    private static final String EDDSA = "EDDSA";
    private static final String ED25519 = "ED25519";
    private static final String ED448 = "ED448";
    private static final String OIDN25519 = "1.3.101.112";
    private static final String OID25519 = "OID.1.3.101.112";
    private static final String OIDN448 = "1.3.101.113";
    private static final String OID448 = "OID.1.3.101.113";
    private static final String PROVIDER = "SunEC";
    private static final SecureRandom RND = new SecureRandom(new byte[]{0x1});

    public static void main(String[] args) throws Exception {

        for (boolean initWithRandom : new boolean[]{true, false}) {

            // As per rfc8032 the generated keysize for ED25519 is
            // 32 octets(256 bits) and ED448 is 57 octets(456 bits).
            // Case with default parameter
            testKeyAttributes(PROVIDER, EDDSA, initWithRandom, null, 256);
            testKeyAttributes(PROVIDER, ED25519, initWithRandom, null, 256);
            testKeyAttributes(PROVIDER, ED448, initWithRandom, null, 456);

            // With named parameter
            testKeyAttributes(PROVIDER, EDDSA, initWithRandom, ED25519, 256);
            testKeyAttributes(PROVIDER, ED25519, initWithRandom, ED25519, 256);
            testKeyAttributes(PROVIDER, OIDN25519, initWithRandom, ED25519, 256);
            testKeyAttributes(PROVIDER, OID25519, initWithRandom, ED25519, 256);
            testKeyAttributes(PROVIDER, ED448, initWithRandom, ED448, 456);
            testKeyAttributes(PROVIDER, OIDN448, initWithRandom, ED448, 456);
            testKeyAttributes(PROVIDER, OID448, initWithRandom, ED448, 456);

            // With size parameter
            testKeyAttributes(PROVIDER, EDDSA, initWithRandom, 255, 256);
            testKeyAttributes(PROVIDER, ED25519, initWithRandom, 255, 256);
            testKeyAttributes(PROVIDER, OIDN25519, initWithRandom, 255, 256);
            testKeyAttributes(PROVIDER, OID25519, initWithRandom, 255, 256);
            testKeyAttributes(PROVIDER, ED448, initWithRandom, 448, 456);
            testKeyAttributes(PROVIDER, OIDN448, initWithRandom, 448, 456);
            testKeyAttributes(PROVIDER, OID448, initWithRandom, 448, 456);
        }
    }

    /**
     * Test standard Key attributes.
     */
    private static void testKeyAttributes(String provider, String name,
            boolean initWithRandom, Object param, int keySize) throws Exception {

        System.out.printf("Case name: %s, param: %s, Expected keysize: %s, "
                + "Initiate with random: %s%n", name, param, keySize,
                initWithRandom);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, provider);
        if (initWithRandom) {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param, RND);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param), RND);
            }
        } else {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param));
            }
        }
        KeyPair kp = kpg.generateKeyPair();
        NamedParameterSpec namedSpec = getNamedParamSpec(name);

        // Verify original PrivateKey with it's different representation
        Key[] privs = manipulateKey(provider, name, PRIVATE_KEY,
                kp.getPrivate(), namedSpec);
        Arrays.stream(privs).forEach(
                priv -> testPrivateKey((EdECPrivateKey) kp.getPrivate(),
                        (EdECPrivateKey) priv, keySize));

        // Verify original PublicKey with it's different representation
        Key[] pubs = manipulateKey(provider, name, PUBLIC_KEY,
                kp.getPublic(), namedSpec);
        Arrays.stream(pubs).forEach(
                pub -> testPublicKey((EdECPublicKey) kp.getPublic(),
                        (EdECPublicKey) pub));
        System.out.println("Passed.");
    }

    private static NamedParameterSpec getNamedParamSpec(String algo) {
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

    private static Key[] manipulateKey(String provider, String algo, int type,
            Key key, NamedParameterSpec namedSpec)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchProviderException, InvalidKeyException {

        KeyFactory kf = KeyFactory.getInstance(algo, provider);
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
     * Basic PrivateKey Test cases
     */
    private static void testPrivateKey(EdECPrivateKey orig,
            EdECPrivateKey generated, int size) {

        equals(orig.getBytes().get().length * 8, size);
        equals(generated.getBytes().get().length * 8, size);
        equals(orig.getBytes().get(), generated.getBytes().get());
        equals(orig.getFormat(), generated.getFormat());
        equals(orig.getEncoded(), generated.getEncoded());
        equals(((NamedParameterSpec) orig.getParams()).getName(),
                ((NamedParameterSpec) generated.getParams()).getName());
    }

    /**
     * Basic PublicKey Test cases
     */
    private static void testPublicKey(EdECPublicKey orig,
            EdECPublicKey generated) {

        equals(orig.getPoint().getY(), generated.getPoint().getY());
        equals(orig.getPoint().isXOdd(), generated.getPoint().isXOdd());
        equals(orig.getFormat(), generated.getFormat());
        equals(orig.getEncoded(), generated.getEncoded());
        equals(((NamedParameterSpec) orig.getParams()).getName(),
                ((NamedParameterSpec) generated.getParams()).getName());
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
