/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8153029 8257769
 * @library /test/lib
 * @run main ChaCha20Poly1305ParamTest
 * @summary ChaCha20 Cipher Implementation (parameters)
 */

import java.util.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.AEADBadTagException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;
import jdk.test.lib.Convert;

public class ChaCha20Poly1305ParamTest {
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
            if ((direction != Cipher.ENCRYPT_MODE) &&
                    (direction != Cipher.DECRYPT_MODE)) {
                throw new IllegalArgumentException(
                        "Direction must be ENCRYPT_MODE or DECRYPT_MODE");
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
        add(new TestData("RFC 7539 A.5 Sample Decryption",
            "1c9240a5eb55d38af333888604f6b5f0473917c1402b80099dca5cbc207075c0",
            "000000000102030405060708",
            1, Cipher.DECRYPT_MODE,
            "64a0861575861af460f062c79be643bd5e805cfd345cf389f108670ac76c8cb2" +
            "4c6cfc18755d43eea09ee94e382d26b0bdb7b73c321b0100d4f03b7f355894cf" +
            "332f830e710b97ce98c8a84abd0b948114ad176e008d33bd60f982b1ff37c855" +
            "9797a06ef4f0ef61c186324e2b3506383606907b6a7c02b0f9f6157b53c867e4" +
            "b9166c767b804d46a59b5216cde7a4e99040c5a40433225ee282a1b0a06c523e" +
            "af4534d7f83fa1155b0047718cbc546a0d072b04b3564eea1b422273f548271a" +
            "0bb2316053fa76991955ebd63159434ecebb4e466dae5a1073a6727627097a10" +
            "49e617d91d361094fa68f0ff77987130305beaba2eda04df997b714d6c6f2c29" +
            "a6ad5cb4022b02709beead9d67890cbb22392336fea1851f38",
            "f33388860000000000004e91",
            "496e7465726e65742d4472616674732061726520647261667420646f63756d65" +
            "6e74732076616c696420666f722061206d6178696d756d206f6620736978206d" +
            "6f6e74687320616e64206d617920626520757064617465642c207265706c6163" +
            "65642c206f72206f62736f6c65746564206279206f7468657220646f63756d65" +
            "6e747320617420616e792074696d652e20497420697320696e617070726f7072" +
            "6961746520746f2075736520496e7465726e65742d4472616674732061732072" +
            "65666572656e6365206d6174657269616c206f7220746f206369746520746865" +
            "6d206f74686572207468616e206173202fe2809c776f726b20696e2070726f67" +
            "726573732e2fe2809d"));
    }};

    // 12-byte nonce DER-encoded as an OCTET_STRING
    public static final byte[] NONCE_OCTET_STR_12 = {
        4, 12, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8
    };

    // Invalid 16-byte nonce DER-encoded as an OCTET_STRING
    public static final byte[] NONCE_OCTET_STR_16 = {
        4, 16, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    // Throwaway key for default init tests
    public static final SecretKey DEF_KEY = new SecretKeySpec(new byte[]
        {
             0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
        }, "ChaCha20");

    public static void main(String args[]) throws Exception {
        int testsPassed = 0;
        int testNumber = 0;

        // Try some default initializations
        testDefaultAlgParams("ChaCha20", Cipher.ENCRYPT_MODE, true);
        testDefaultAlgParams("ChaCha20-Poly1305", Cipher.ENCRYPT_MODE, true);
        testDefaultAlgParamSpec("ChaCha20", Cipher.ENCRYPT_MODE, true);
        testDefaultAlgParamSpec("ChaCha20-Poly1305", Cipher.ENCRYPT_MODE, true);
        testDefaultAlgParams("ChaCha20", Cipher.DECRYPT_MODE, false);
        testDefaultAlgParams("ChaCha20-Poly1305", Cipher.DECRYPT_MODE, false);
        testDefaultAlgParamSpec("ChaCha20", Cipher.DECRYPT_MODE, false);
        testDefaultAlgParamSpec("ChaCha20-Poly1305", Cipher.DECRYPT_MODE,
                false);

        // Try (and hopefully fail) to create a ChaCha20 AlgorithmParameterSpec
        System.out.println(
                "*** Test: Try to make ChaCha20 AlgorithmParameterSpec");
        try {
            ChaCha20ParameterSpec badChaCha20Spec =
                    new ChaCha20ParameterSpec(NONCE_OCTET_STR_16, 1);
            throw new RuntimeException("ChaCha20 AlgorithmParameterSpec " +
                    "with 16 byte nonce should fail");
        } catch (IllegalArgumentException iae) {
            System.out.println("Caught expected exception: " + iae);
        }

        // Try (and hopefully fail) to create a ChaCha20 AlgorithmParameters
        System.out.println(
                "*** Test: Try to make ChaCha20 AlgorithmParameters");
        try {
            AlgorithmParameters apsNoChaCha20 =
                    AlgorithmParameters.getInstance("ChaCha20");
            throw new RuntimeException(
                    "ChaCha20 AlgorithmParameters should fail");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Caught expected exception: " + nsae);
        }

        // Create the AlgorithmParameters object from a valid encoding
        System.out.println("*** Test: Create and init ChaCha20-Poly1305 APS");
        AlgorithmParameters apsGood =
                AlgorithmParameters.getInstance("ChaCha20-Poly1305");
        apsGood.init(NONCE_OCTET_STR_12);
        System.out.println("Test Passed");

        // Pull an AlgorithmParameters object out of the initialized cipher
        // and compare its value against the original.
        System.out.println("*** Test: Init ChaCha20-Poly1305 Cipher using " +
                "AP, retrieve AP and compare");
        Cipher cc20p1305 = Cipher.getInstance("ChaCha20-Poly1305");
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY, apsGood);
        AlgorithmParameters pulledParams = cc20p1305.getParameters();
        byte[] apsGoodData = apsGood.getEncoded();
        byte[] pulledParamsData = pulledParams.getEncoded();
        if (!Arrays.equals(apsGoodData, pulledParamsData)) {
            throw new RuntimeException(
                "Retrieved parameters do not match those used to init cipher");
        }
        System.out.println("Test Passed");

        // Try the same test with ChaCha20.  It should always be null.
        System.out.println("*** Test: Init ChaCha20 Cipher using " +
                "AP, retrieve AP and compare");
        Cipher cc20 = Cipher.getInstance("ChaCha20");
        cc20.init(Cipher.ENCRYPT_MODE, DEF_KEY);
        pulledParams = cc20.getParameters();
        if (pulledParams != null) {
            throw new RuntimeException("Unexpected non-null " +
                    "AlgorithmParameters from ChaCha20 cipiher");
        }
        System.out.println("Test Passed");

        // Create and try to init using invalid encoding
        AlgorithmParameters apsBad =
                AlgorithmParameters.getInstance("ChaCha20-Poly1305");
        System.out.println("*** Test: Use invalid encoding scheme");
        try {
            apsBad.init(NONCE_OCTET_STR_12, "OraclePrivate");
            throw new RuntimeException("Allowed unsupported encoding scheme: " +
                    apsBad.getAlgorithm());
        } catch (IOException iae) {
            System.out.println("Caught expected exception: " + iae);
        }

        // Try to init using supported scheme but invalid length
        System.out.println("*** Test: Use supported scheme, nonce too large");
        try {
            apsBad.init(NONCE_OCTET_STR_16, "ASN.1");
            throw new RuntimeException("Allowed invalid encoded length");
        } catch (IOException ioe) {
            System.out.println("Caught expected exception: " + ioe);
        }

        // The next set of tests cover cases where ChaCha20-Poly1305 cipher
        // objects have the getParameters() call executed after instantiation
        // but before initialization.
        System.out.println("*** Test: getParameters before init");
        cc20p1305 = Cipher.getInstance("ChaCha20-Poly1305");
        AlgorithmParameters algParams = cc20p1305.getParameters();
        byte[] preInitNonce = getNonceFromParams(algParams);
        // A second pre-init getParameters() call should return a new set of
        // random parameters.
        AlgorithmParameters algParamsTwo = cc20p1305.getParameters();
        byte[] secondNonce = getNonceFromParams(algParamsTwo);
        if (MessageDigest.isEqual(preInitNonce, secondNonce)) {
            throw new RuntimeException("Unexpected nonce match between " +
                    "two pre-init getParameters() calls");
        }

        // Next we will initialize the Cipher object using a form of init
        // that doesn't take AlgorithmParameters or AlgorithmParameterSpec.
        // The nonce created using the pre-init getParameters() call should
        // be overwritten by a freshly generated set of random parameters.
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY);
        AlgorithmParameters postInitAps = cc20p1305.getParameters();
        byte[] postInitNonce = getNonceFromParams(postInitAps);
        if (MessageDigest.isEqual(preInitNonce, postInitNonce)) {
            throw new RuntimeException("Unexpected nonce match between " +
                    "pre and post-init getParameters() calls");
        }
        System.out.println("Test Passed");

        // After an initialization, subsequent calls to getParameters() should
        // return the same parameter value until the next initialization takes
        // place.
        System.out.println("*** Test: getParameters after init");
        AlgorithmParameters postInitApsTwo = cc20p1305.getParameters();
        byte[] postInitNonceTwo = getNonceFromParams(postInitApsTwo);
        if (!MessageDigest.isEqual(postInitNonce, postInitNonceTwo)) {
            throw new RuntimeException("Unexpected nonce mismatch between " +
                    "two post-init getParameters() calls");
        }
        System.out.println("Test Passed");

        // Test reinitialization use cases.
        // First test: instantiate, init(no param), encrypt.  Get params
        // and attempt to reinit with same parameters.  Should fail.
        System.out.println("*** Test: Init w/ random nonce, init 2nd time");
        cc20p1305 = Cipher.getInstance("ChaCha20-Poly1305");
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY);
        algParams = cc20p1305.getParameters();
        preInitNonce = getNonceFromParams(algParams);
        // Perform a simple encryption operation
        cc20p1305.doFinal(aeadTestList.get(0).input);
        try {
            // Now try to reinitialize using the same parameters
            cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY, algParams);
            throw new RuntimeException("Illegal key/nonce reuse");
        } catch (InvalidKeyException ike) {
            System.out.println("Caught expected exception: " + ike);
        }

        // Test the reinit guard using an AlgorithmParameterSpec with the
        // Same nonce value.  This should also be a failure.
        try {
            cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY,
                    new IvParameterSpec(preInitNonce));
            throw new RuntimeException("Illegal key/nonce reuse");
        } catch (InvalidKeyException ike) {
            System.out.println("Caught expected exception: " + ike);
        }

        // Try one more time, this time providing a new 12-byte nonce, which
        // should be allowed even if the key is the same.
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY,
                new IvParameterSpec(NONCE_OCTET_STR_12, 2, 12));
        System.out.println("Test Passed");

        // Reinit test: instantiate, init(no param), getParam, encrypt,
        // then init(no param).  Should work and the parameters should be
        // different after each init.
        cc20p1305 = Cipher.getInstance("ChaCha20-Poly1305");
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY);
        byte[] paramInitOne = getNonceFromParams(cc20p1305.getParameters());
        // Perform a simple encryption operation
        cc20p1305.doFinal(aeadTestList.get(0).input);
        // reinit (no params)
        cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY);
        byte[] paramInitTwo = getNonceFromParams(cc20p1305.getParameters());
        if (MessageDigest.isEqual(paramInitOne, paramInitTwo)) {
            throw new RuntimeException("Unexpected nonce match between " +
                    "pre and post-init getParameters() calls");
        }
        System.out.println("Test Passed");

        // Reinit test: instantiate, init(no param), doFinal, then doFinal
        // again without intervening init.  Should fail due to no-reuse
        // protections.
        try {
            cc20p1305 = Cipher.getInstance("ChaCha20-Poly1305");
            cc20p1305.init(Cipher.ENCRYPT_MODE, DEF_KEY);
            cc20p1305.doFinal(aeadTestList.get(0).input);
            cc20p1305.doFinal(aeadTestList.get(0).input);
            throw new RuntimeException("Illegal key/nonce reuse");
        } catch (IllegalStateException ise) {
            System.out.println("Caught expected exception: " + ise);
        }

        System.out.println("----- AEAD Tests -----");
        for (TestData test : aeadTestList) {
            System.out.println("*** Test " + ++testNumber + ": " +
                    test.testName);
            if (runAEADTest(test)) {
                testsPassed++;
            }
        }
        System.out.println();

        System.out.println("Total tests: " + testNumber +
                ", Passed: " + testsPassed + ", Failed: " +
                (testNumber - testsPassed));
        if (testsPassed != testNumber) {
            throw new RuntimeException("One or more tests failed.  " +
                    "Check output for details");
        }
    }

    /**
     * Attempt default inits with null AlgorithmParameters
     *
     * @param alg the algorithm (ChaCha20, ChaCha20-Poly1305)
     * @param mode the Cipher mode (ENCRYPT_MODE, etc.)
     */
    private static void testDefaultAlgParams(String alg, int mode,
            boolean shouldPass) {
        byte[] ivOne = null, ivTwo = null;
        System.out.println("Test default AlgorithmParameters: Cipher = " +
                alg + ", mode = " + mode);
        try {
            AlgorithmParameters params = null;
            Cipher cipher = Cipher.getInstance(alg);
            cipher.init(mode, DEF_KEY, params, null);
            ivOne = cipher.getIV();
            cipher.init(mode, DEF_KEY, params, null);
            ivTwo = cipher.getIV();
            if (!shouldPass) {
                throw new RuntimeException(
                        "Did not receive expected exception");
            }
        } catch (GeneralSecurityException gse) {
            if (shouldPass) {
                throw new RuntimeException(gse);
            }
            System.out.println("Caught expected exception: " + gse);
            return;
        }
        if (Arrays.equals(ivOne, ivTwo)) {
            throw new RuntimeException(
                    "FAIL! Two inits generated same nonces");
        } else {
            System.out.println("IV 1:\n" + dumpHexBytes(ivOne, 16, "\n", " "));
            System.out.println("IV 1:\n" + dumpHexBytes(ivTwo, 16, "\n", " "));
        }
    }

    /**
     * Attempt default inits with null AlgorithmParameters
     *
     * @param alg the algorithm (ChaCha20, ChaCha20-Poly1305)
     * @param mode the Cipher mode (ENCRYPT_MODE, etc.)
     */
    private static void testDefaultAlgParamSpec(String alg, int mode,
            boolean shouldPass) {
        byte[] ivOne = null, ivTwo = null;
        System.out.println("Test default AlgorithmParameterSpec: Cipher = " +
                alg + ", mode = " + mode);
        try {
            AlgorithmParameterSpec params = null;
            Cipher cipher = Cipher.getInstance(alg);
            cipher.init(mode, DEF_KEY, params, null);
            ivOne = cipher.getIV();
            cipher.init(mode, DEF_KEY, params, null);
            ivTwo = cipher.getIV();
            if (!shouldPass) {
                throw new RuntimeException(
                        "Did not receive expected exception");
            }
        } catch (GeneralSecurityException gse) {
            if (shouldPass) {
                throw new RuntimeException(gse);
            }
            System.out.println("Caught expected exception: " + gse);
            return;
        }
        if (Arrays.equals(ivOne, ivTwo)) {
            throw new RuntimeException(
                    "FAIL! Two inits generated same nonces");
        } else {
            System.out.println("IV 1:\n" + dumpHexBytes(ivOne, 16, "\n", " "));
            System.out.println("IV 2:\n" + dumpHexBytes(ivTwo, 16, "\n", " "));
        }
    }

    private static boolean runAEADTest(TestData testData)
            throws GeneralSecurityException, IOException {
        boolean result = false;

        Cipher mambo = Cipher.getInstance("ChaCha20-Poly1305");
        SecretKeySpec mamboKey = new SecretKeySpec(testData.key, "ChaCha20");
        AlgorithmParameters mamboParams =
                AlgorithmParameters.getInstance("ChaCha20-Poly1305");

        // Put the nonce into ASN.1 ChaCha20-Poly1305 parameter format
        byte[] derNonce = new byte[testData.nonce.length + 2];
        derNonce[0] = 0x04;
        derNonce[1] = (byte)testData.nonce.length;
        System.arraycopy(testData.nonce, 0, derNonce, 2,
                testData.nonce.length);
        mamboParams.init(derNonce);

        mambo.init(testData.direction, mamboKey, mamboParams);

        byte[] out = new byte[mambo.getOutputSize(testData.input.length)];
        int outOff = 0;
        try {
            mambo.updateAAD(testData.aad);
            outOff += mambo.update(testData.input, 0, testData.input.length,
                    out, outOff);
            outOff += mambo.doFinal(out, outOff);
        } catch (AEADBadTagException abte) {
            // If we get a bad tag or derive a tag mismatch, log it
            // and register it as a failure
            System.out.println("FAIL: " + abte);
            return false;
        }

        if (!Arrays.equals(out, testData.expOutput)) {
            System.out.println("ERROR - Output Mismatch!");
            System.out.println("Expected:\n" +
                    dumpHexBytes(testData.expOutput, 16, "\n", " "));
            System.out.println("Actual:\n" +
                    dumpHexBytes(out, 16, "\n", " "));
            System.out.println();
        } else {
            result = true;
        }

        return result;
    }

    private static byte[] getNonceFromParams(AlgorithmParameters params)
            throws InvalidParameterSpecException {
        return params.getParameterSpec(IvParameterSpec.class).getIV();
    }

    /**
     * Dump the hex bytes of a buffer into string form.
     *
     * @param data The array of bytes to dump to stdout.
     * @param itemsPerLine The number of bytes to display per line
     *      if the {@code lineDelim} character is blank then all bytes
     *      will be printed on a single line.
     * @param lineDelim The delimiter between lines
     * @param itemDelim The delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(byte[] data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        return dumpHexBytes(ByteBuffer.wrap(data), itemsPerLine, lineDelim,
                itemDelim);
    }

    private static String dumpHexBytes(ByteBuffer data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            data.mark();
            int i = 0;
            while (data.remaining() > 0) {
                if (i % itemsPerLine == 0 && i != 0) {
                    sb.append(lineDelim);
                }
                sb.append(String.format("%02X", data.get())).append(itemDelim);
                i++;
            }
            data.reset();
        }

        return sb.toString();
    }
}

