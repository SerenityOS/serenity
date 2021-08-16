/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8255410
 * @library /test/lib ..
 * @run main/othervm TestChaChaPolyNoReuse
 * @summary Test PKCS#11 ChaCha20-Poly1305 Cipher Implementation
 * (key/nonce reuse check)
 */

import java.util.*;
import javax.crypto.Cipher;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;

public class TestChaChaPolyNoReuse extends PKCS11Test {

    private static final String KEY_ALGO = "ChaCha20";
    private static final String CIPHER_ALGO = "ChaCha20-Poly1305";

    /**
     * Basic TestMethod interface definition
     */
    public interface TestMethod {
        /**
         * Runs the actual test case
         *
         * @param provider the provider to provide the requested Cipher obj.
         *
         * @return true if the test passes, false otherwise.
         */
        boolean run(Provider p);
    }

    public static class TestData {
        public TestData(String name, String keyStr, String nonceStr, int ctr,
                int dir, String inputStr, String aadStr, String outStr) {
            testName = Objects.requireNonNull(name);
            HexFormat hex = HexFormat.of();
            key = hex.parseHex(keyStr);
            nonce = hex.parseHex(nonceStr);
            if ((counter = ctr) < 0) {
                throw new IllegalArgumentException(
                        "counter must be 0 or greater");
            }
            direction = dir;
            if (direction != Cipher.ENCRYPT_MODE) {
                throw new IllegalArgumentException(
                        "Direction must be ENCRYPT_MODE");
            }
            input = hex.parseHex(inputStr);
            aad = (aadStr != null) ? hex.parseHex(aadStr) : null;
            expOutput = hex.parseHex(outStr);
        }

        public final String testName;
        public final byte[] key;
        public final byte[] nonce;
        public final int counter;
        public final int direction;
        public final byte[] input;
        public final byte[] aad;
        public final byte[] expOutput;
    }

    public static final List<TestData> aeadTestList =
            new LinkedList<TestData>() {{
        add(new TestData("RFC 7539 Sample AEAD Test Vector",
            "808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f",
            "070000004041424344454647",
            1, Cipher.ENCRYPT_MODE,
            "4c616469657320616e642047656e746c656d656e206f662074686520636c6173" +
            "73206f66202739393a204966204920636f756c64206f6666657220796f75206f" +
            "6e6c79206f6e652074697020666f7220746865206675747572652c2073756e73" +
            "637265656e20776f756c642062652069742e",
            "50515253c0c1c2c3c4c5c6c7",
            "d31a8d34648e60db7b86afbc53ef7ec2a4aded51296e08fea9e2b5a736ee62d6" +
            "3dbea45e8ca9671282fafb69da92728b1a71de0a9e060b2905d6a5b67ecd3b36" +
            "92ddbd7f2d778b8c9803aee328091b58fab324e4fad675945585808b4831d7bc" +
            "3ff4def08e4b7a9de576d26586cec64b61161ae10b594f09e26a7e902ecbd060" +
            "0691"));
    }};

    /**
     * Make sure we do not use this Cipher object without initializing it
     * at all
     */
    public static final TestMethod noInitTest = new TestMethod() {
        @Override
        public boolean run(Provider p) {
            System.out.println("----- No Init Test -----");
            try {
                Cipher cipher = Cipher.getInstance(CIPHER_ALGO, p);
                TestData testData = aeadTestList.get(0);

                // Attempting to use the cipher without initializing it
                // should throw an IllegalStateException
                try {
                    cipher.updateAAD(testData.aad);
                    throw new RuntimeException(
                            "Expected IllegalStateException not thrown");
                } catch (IllegalStateException ise) {
                    // Do nothing, this is what we expected to happen
                }
            } catch (Exception exc) {
                System.out.println("Unexpected exception: " + exc);
                exc.printStackTrace();
                return false;
            }

            return true;
        }
    };

    /**
     * Attempt to run two full encryption operations without an init in
     * between.
     */
    public static final TestMethod encTwiceNoInit = new TestMethod() {
        @Override
        public boolean run(Provider p) {
            System.out.println("----- Encrypt 2nd time without init -----");
            try {
                AlgorithmParameterSpec spec;
                Cipher cipher = Cipher.getInstance(CIPHER_ALGO, p);
                TestData testData = aeadTestList.get(0);
                spec = new IvParameterSpec(testData.nonce);
                SecretKey key = new SecretKeySpec(testData.key, KEY_ALGO);

                // Initialize and encrypt
                cipher.init(testData.direction, key, spec);
                cipher.updateAAD(testData.aad);
                cipher.doFinal(testData.input);
                System.out.println("First encryption complete");

                // Now attempt to encrypt again without changing the key/IV
                // This should fail.
                try {
                    cipher.updateAAD(testData.aad);
                } catch (IllegalStateException ise) {
                    // Do nothing, this is what we expected to happen
                }
                try {
                    cipher.doFinal(testData.input);
                    throw new RuntimeException(
                            "Expected IllegalStateException not thrown");
                } catch (IllegalStateException ise) {
                    // Do nothing, this is what we expected to happen
                }
            } catch (Exception exc) {
                System.out.println("Unexpected exception: " + exc);
                exc.printStackTrace();
                return false;
            }

            return true;
        }
    };

    /**
     * Encrypt once successfully, then attempt to init with the same
     * key and nonce.
     */
    public static final TestMethod encTwiceInitSameParams = new TestMethod() {
        @Override
        public boolean run(Provider p) {
            System.out.println("----- Encrypt, then init with same params " +
                     "-----");
            try {
                AlgorithmParameterSpec spec;
                Cipher cipher = Cipher.getInstance(CIPHER_ALGO, p);
                TestData testData = aeadTestList.get(0);
                spec = new IvParameterSpec(testData.nonce);
                SecretKey key = new SecretKeySpec(testData.key, KEY_ALGO);

                // Initialize then encrypt
                cipher.init(testData.direction, key, spec);
                cipher.updateAAD(testData.aad);
                cipher.doFinal(testData.input);
                System.out.println("First encryption complete");

                // Initializing after the completed encryption with
                // the same key and nonce should fail.
                try {
                    cipher.init(testData.direction, key, spec);
                    throw new RuntimeException(
                            "Expected IKE or IAPE not thrown");
                } catch (InvalidKeyException |
                        InvalidAlgorithmParameterException e) {
                    // Do nothing, this is what we expected to happen
                }
            } catch (Exception exc) {
                System.out.println("Unexpected exception: " + exc);
                exc.printStackTrace();
                return false;
            }

            return true;
        }
    };

    public static final List<TestMethod> testMethodList =
            Arrays.asList(noInitTest, encTwiceNoInit, encTwiceInitSameParams);

    @Override
    public void main(Provider p) throws Exception {
        System.out.println("Testing " + p.getName());
        try {
            Cipher.getInstance(CIPHER_ALGO, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Skip; no support for " + CIPHER_ALGO);
            return;
        }

        int testsPassed = 0;
        int testNumber = 0;

        for (TestMethod tm : testMethodList) {
            testNumber++;
            boolean result = tm.run(p);
            System.out.println("Result: " + (result ? "PASS" : "FAIL"));
            if (result) {
                testsPassed++;
            }
        }

        System.out.println("Total Tests: " + testNumber +
                ", Tests passed: " + testsPassed);
        if (testsPassed < testNumber) {
            throw new RuntimeException(
                    "Not all tests passed.  See output for failure info");
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestChaChaPolyNoReuse(), args);
    }
}
