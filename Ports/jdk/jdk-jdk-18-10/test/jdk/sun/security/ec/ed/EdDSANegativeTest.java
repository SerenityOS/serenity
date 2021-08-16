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

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdDSAParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/*
 * @test
 * @bug 8209632
 * @summary Negative cases for EDDSA.
 * @library /test/lib
 * @run main EdDSANegativeTest
 */
public class EdDSANegativeTest {

    private static final String EDDSA = "EdDSA";
    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String PROVIDER = "SunEC";
    private static final String OTHER = "other";
    private static final byte[] MSG = "TEST".getBytes();

    public static void main(String[] args) throws Exception {
        byName();
        byParam();
        byInvalidKey();
        byInvalidKeyType();
    }

    private static void byName() throws Exception {

        for (String name : new String[]{null, "", "EDDSA", "eddsa", "EDdsa",
            EDDSA, ED25519, "ed25519", "ED25519", ED448, "eD448", "ED448",
            "ed448", OTHER}) {
            try {
                KeyPair kp = genKeyPair(name);
                KeyFactory kf = KeyFactory.getInstance(name, PROVIDER);
                EdECPrivateKey edPri
                        = (EdECPrivateKey) kf.translateKey(kp.getPrivate());
                EdECPublicKey edPub
                        = (EdECPublicKey) kf.translateKey(kp.getPublic());
                Signature sig = Signature.getInstance(name, PROVIDER);
                byte[] computedSig = sign(sig, edPri, MSG);
                if (!verify(sig, edPub, MSG, computedSig)) {
                    throw new RuntimeException("Signature verification failed");
                }
                if (name == null || "".equals(name)) {
                    throw new RuntimeException(
                            "Should not reach here for algo: " + name);
                }
                System.out.println("Passed: byName: " + name);
            } catch (NullPointerException e) {
                if (name != null) {
                    throw new RuntimeException(
                            "Unknown issue with algo name: " + name, e);
                }
            } catch (NoSuchAlgorithmException e) {
                if (!("".equals(name) || OTHER.equals(name))) {
                    throw new RuntimeException(
                            "Unknown issue with algo name: " + name, e);
                }
            }
        }
    }

    private static void byParam() throws Exception {
        testParam(EDDSA);
        testParam(ED25519);
        testParam(ED448);
    }

    private static void byInvalidKey() throws Exception {
        testInvalidKey(EDDSA);
        testInvalidKey(ED25519);
        testInvalidKey(ED448);
    }

    private static void byInvalidKeyType() throws Exception {
        testInvalidKeyType(EDDSA);
        testInvalidKeyType(ED25519);
        testInvalidKeyType(ED448);
    }

    /**
     * Test Signature.
     */
    private static void testParam(String name) throws Exception {

        KeyPair kp = genKeyPair(name);
        Signature sig = Signature.getInstance(name, PROVIDER);
        // Set initial paramter to generate a signature
        EdDSAParameterSpec initParam
                = new EdDSAParameterSpec(true, "testContext".getBytes());
        sig.setParameter(initParam);
        byte[] computedSig = sign(sig, kp.getPrivate(), MSG);
        // Signature should not get verified other than same parameter
        // which is set through the signature instance.
        for (boolean preHash : new boolean[]{true, false}) {
            // Test case with prehash as parameter without context set.
            verify(sig, kp.getPublic(), MSG, new EdDSAParameterSpec(preHash),
                    initParam, computedSig);
            // Test Case with Context combined of different sizes.
            // As per rfc8032, value of context is maximum of 255 octet
            for (byte[] context : new byte[][]{{}, "other".getBytes(),
            new byte[255], new byte[500]}) {
                System.out.printf("Testing signature for name: %s, algorithm "
                        + "spec: (prehash:%s, context:%s)%n", name, preHash,
                        HexFormat.of().withUpperCase().formatHex(context));
                try {
                    verify(sig, kp.getPublic(), MSG,
                            new EdDSAParameterSpec(preHash, context),
                            initParam, computedSig);
                } catch (InvalidParameterException e) {
                    if (context.length <= 255) {
                        throw new RuntimeException("Should not throw exception "
                                + "when context size <= 255 octet: "
                                + context.length);
                    }
                }
            }
        }
    }

    private static void testInvalidKey(String name) throws Exception {
        KeyPair kp = genKeyPair(name);
        KeyPair kp1 = genKeyPair(name);
        Signature sig = Signature.getInstance(name, PROVIDER);
        byte[] computedSig = sign(sig, kp.getPrivate(), MSG);
        if (verify(sig, kp1.getPublic(), MSG, computedSig)) {
            throw new RuntimeException("Signature verification failed "
                    + "for unpaired key.");
        }
        System.out.println("Passed: testInvalidKey: " + name);
    }

    private static void testInvalidKeyType(String name) throws Exception {

        KeyFactory kf = KeyFactory.getInstance(name, PROVIDER);
        try {
            kf.translateKey(new InvalidPrivateKey());
        } catch (InvalidKeyException e) {
            // Expected exception and not to be handled
        }
        try {
            kf.translateKey(new InvalidPublicKey());
        } catch (InvalidKeyException e) {
            // Expected exception and not to be handled
        }
        System.out.println("Passed: testInvalidKeyType: " + name);
    }

    private static KeyPair genKeyPair(String name) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, PROVIDER);
        return kpg.generateKeyPair();
    }

    private static byte[] sign(Signature sig, PrivateKey priKey, byte[] msg)
            throws Exception {
        sig.initSign(priKey);
        sig.update(msg);
        return sig.sign();
    }

    private static boolean verify(Signature sig, PublicKey pubKey, byte[] msg,
            byte[] sign) throws Exception {
        sig.initVerify(pubKey);
        sig.update(msg);
        return sig.verify(sign);
    }

    private static void verify(Signature sig, PublicKey pubKey, byte[] msg,
            EdDSAParameterSpec params, EdDSAParameterSpec initParam,
            byte[] computedSig) throws Exception {

        sig.setParameter(params);
        if (verify(sig, pubKey, msg, computedSig)) {
            byte[] context = params.getContext().isPresent()
                    ? params.getContext().get() : null;
            byte[] initContext = initParam.getContext().isPresent()
                    ? initParam.getContext().get() : null;
            boolean preHash = params.isPrehash();
            boolean initPreHash = initParam.isPrehash();
            // The signature should not get verified with other parameters
            // set through signature instance.
            if (!(equals(context, initContext) && equals(preHash, initPreHash))) {
                throw new RuntimeException(String.format("Signature verification"
                        + " success with different param context(actual:%s, "
                        + "expected:%s), Prehash(actual:%s, expected:%s)",
                        HexFormat.of().withUpperCase().formatHex(context),
                        HexFormat.of().withUpperCase().formatHex(initContext),
                        preHash, initPreHash));
            } else {
                System.out.println("Atleast a case matched");
            }
        }
    }

    private static boolean equals(Object actual, Object expected) {
        if (actual == expected) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        boolean equals = actual.equals(expected);
        if (!equals) {
            throw new RuntimeException(String.format("Actual: %s, Expected: %s",
                    actual, expected));
        }
        return equals;
    }

    private static boolean equals(byte[] actual, byte[] expected) {
        if (actual == expected) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        boolean equals = Arrays.equals(actual, expected);
        if (!equals) {
            throw new RuntimeException(String.format("Actual array: %s, "
                    + "Expected array:%s", HexFormat.of().withUpperCase().formatHex(actual),
                    HexFormat.of().withUpperCase().formatHex(expected)));
        }
        return equals;
    }

    private static class InvalidPrivateKey implements PrivateKey {

        @Override
        public String getAlgorithm() {
            return "test";
        }

        @Override
        public String getFormat() {
            return "test";
        }

        @Override
        public byte[] getEncoded() {
            return "test".getBytes();
        }

    }

    private static class InvalidPublicKey implements PublicKey {

        @Override
        public String getAlgorithm() {
            return "test";
        }

        @Override
        public String getFormat() {
            return "test";
        }

        @Override
        public byte[] getEncoded() {
            return "test".getBytes();
        }

    }
}
