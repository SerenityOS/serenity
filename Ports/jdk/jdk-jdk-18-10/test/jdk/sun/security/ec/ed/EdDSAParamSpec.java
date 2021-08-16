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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.EdDSAParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/*
 * @test
 * @bug 8209632
 * @summary Test EdDSAParameterSpec.
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main EdDSAParamSpec
 */
public class EdDSAParamSpec {

    private static final String EDDSA = "EdDSA";
    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String PROVIDER = "SunEC";
    private static final byte[] MSG = "TEST".getBytes();
    private static final SecureRandom RND = new SecureRandom(new byte[]{0x1});

    public static void main(String[] args) throws Exception {

        testParam(PROVIDER, EDDSA);
        testParam(PROVIDER, ED25519);
        testParam(PROVIDER, ED448);
    }

    /**
     * Test Signature.
     */
    private static void testParam(String provider, String name)
            throws Exception {

        KeyPair kp = genKeyPair(provider, name);
        Signature sig = Signature.getInstance(name, provider);
        EdDSAParameterSpec initParam
                = new EdDSAParameterSpec(true, "testContext".getBytes());
        sig.setParameter(initParam);
        byte[] origSign = sign(sig, kp.getPrivate(), MSG);
        for (boolean preHash : new boolean[]{true, false}) {
            System.out.printf("Testing signature for name: %s,"
                    + " algorithm spec: (prehash:%s)%n", name, preHash);
            verifyPublic(sig, kp.getPublic(), MSG,
                    new EdDSAParameterSpec(preHash), initParam, origSign);
            // Test Case with Context size combined.
            // As per rfc8032, value of context is maximum of 255 octet
            byte[] maxCtx = new byte[255];
            RND.nextBytes(maxCtx);
            for (byte[] context : new byte[][]{"others".getBytes(), maxCtx}) {
                System.out.printf("Testing signature for name: %s,"
                        + " algorithm spec: (prehash:%s, context:%s)%n",
                        name, preHash, HexFormat.of().withUpperCase().formatHex(context));
                EdDSAParameterSpec params
                        = new EdDSAParameterSpec(preHash, context);
                verifyPublic(sig, kp.getPublic(), MSG, params, initParam,
                        origSign);
            }
        }
        System.out.println("Passed.");
    }

    private static KeyPair genKeyPair(String provider, String name)
            throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, provider);
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

    private static void verifyPublic(Signature sig, PublicKey pubKey,
            byte[] msg, EdDSAParameterSpec params, EdDSAParameterSpec initParam,
            byte[] origSign) throws Exception {

        sig.setParameter(params);
        if (verify(sig, pubKey, msg, origSign)) {
            byte[] context = params.getContext().isPresent()
                    ? params.getContext().get() : null;
            byte[] initContext = initParam.getContext().isPresent()
                    ? initParam.getContext().get() : null;
            boolean preHash = params.isPrehash();
            boolean initPreHash = initParam.isPrehash();
            // The signature should not get verified other than same parameter
            // which is set through the signature instance.
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

}
