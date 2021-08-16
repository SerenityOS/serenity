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
 * @modules jdk.crypto.cryptoki
 * @build jdk.test.lib.Convert
 * @run main/othervm TestChaChaPolyKAT
 * @summary ChaCha20-Poly1305 Cipher Implementation (KAT)
 */

import java.util.*;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.AEADBadTagException;
import java.nio.ByteBuffer;
import jdk.test.lib.Convert;

public class TestChaChaPolyKAT extends PKCS11Test {
    public static class TestData {
        public TestData(String name, String keyStr, String nonceStr, int ctr,
                int dir, String inputStr, String aadStr, String outStr) {
            testName = Objects.requireNonNull(name);
            HexFormat hex = HexFormat.of();
            key = hex.parseHex(Objects.requireNonNull(keyStr));
            nonce = hex.parseHex(Objects.requireNonNull(nonceStr));
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
            input = hex.parseHex(Objects.requireNonNull(inputStr));
            aad = (aadStr != null) ? hex.parseHex(aadStr) : null;
            expOutput = hex.parseHex(Objects.requireNonNull(outStr));
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

    private static final String ALGO = "ChaCha20-Poly1305";

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

    @Override
    public void main(Provider p) throws Exception {
        System.out.println("Testing " + p.getName());

        try {
            Cipher.getInstance(ALGO, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Skip; no support for " + ALGO);
            return;
        }

        int testsPassed = 0;
        int testNumber = 0;

        System.out.println("----- AEAD Tests -----");
        for (TestData test : aeadTestList) {
            System.out.println("*** Test " + ++testNumber + ": " +
                    test.testName);
            if (runAEADTest(p, test)) {
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

    private static boolean runAEADTest(Provider p, TestData testData)
            throws GeneralSecurityException {
        boolean result = false;

        Cipher mambo = Cipher.getInstance(ALGO, p);
        SecretKeySpec mamboKey = new SecretKeySpec(testData.key, "ChaCha20");
        IvParameterSpec mamboSpec = new IvParameterSpec(testData.nonce);

        mambo.init(testData.direction, mamboKey, mamboSpec);

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

        if (!Arrays.equals(out, 0, outOff, testData.expOutput, 0, outOff)) {
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

    public static void main(String[] args) throws Exception {
        main(new TestChaChaPolyKAT(), args);
    }
}
