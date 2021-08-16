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

/*
 * @test
 * @bug 8153029
 * @library /test/lib
 * @run main ChaCha20CipherUnitTest
 * @summary Unit test for com.sun.crypto.provider.ChaCha20Cipher.
 */

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jdk.test.lib.Utils;

public class ChaCha20CipherUnitTest {

    private static final byte[] NONCE
            = HexFormat.of().parseHex("012345670123456701234567");
    private static final SecretKeySpec KEY = new SecretKeySpec(
            HexFormat.of().parseHex("0123456701234567012345670123456701234567012345670123456701234567"),
            "ChaCha20");
    private static final ChaCha20ParameterSpec CHACHA20_PARAM_SPEC
            = new ChaCha20ParameterSpec(NONCE, 0);
    private static final IvParameterSpec IV_PARAM_SPEC
            = new IvParameterSpec(NONCE);

    public static void main(String[] args) throws Exception {
        testTransformations();
        testInit();
        testAEAD();
        testGetBlockSize();
    }

    private static void testTransformations() throws Exception {
        System.out.println("== transformations ==");

        checkTransformation("ChaCha20", true);
        checkTransformation("ChaCha20/None/NoPadding", true);
        checkTransformation("ChaCha20-Poly1305", true);
        checkTransformation("ChaCha20-Poly1305/None/NoPadding", true);

        checkTransformation("ChaCha20/ECB/NoPadding", false);
        checkTransformation("ChaCha20/None/PKCS5Padding", false);
        checkTransformation("ChaCha20-Poly1305/ECB/NoPadding", false);
        checkTransformation("ChaCha20-Poly1305/None/PKCS5Padding", false);
    }

    private static void checkTransformation(String transformation,
            boolean expected) throws Exception {
        try {
            Cipher.getInstance(transformation);
            if (!expected) {
                throw new RuntimeException(
                        "Unexpected transformation: " + transformation);
            } else {
                System.out.println("Expected transformation: " + transformation);
            }
        } catch (NoSuchAlgorithmException e) {
            if (!expected) {
                System.out.println("Unexpected transformation: " + transformation);
            } else {
                throw new RuntimeException("Unexpected fail: " + transformation, e);
            }
        }
    }

    private static void testInit() throws Exception {
        testInitOnCrypt(Cipher.ENCRYPT_MODE);
        testInitOnCrypt(Cipher.DECRYPT_MODE);
        testInitOnWrap(Cipher.WRAP_MODE);
        testInitOnWrap(Cipher.UNWRAP_MODE);
    }

    private static void testInitOnCrypt(int opMode) throws Exception {
        System.out.println("== init (" + getOpModeName(opMode) + ") ==");

        Cipher.getInstance("ChaCha20").init(opMode, KEY, CHACHA20_PARAM_SPEC);
        Cipher.getInstance("ChaCha20").init(opMode, KEY,
                CHACHA20_PARAM_SPEC, new SecureRandom());

        try {
            Cipher.getInstance("ChaCha20").init(opMode, KEY, IV_PARAM_SPEC);
            throw new RuntimeException("ChaCha20ParameterSpec is needed");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected " + e);
        }

        Cipher.getInstance("ChaCha20-Poly1305").init(opMode, KEY,
                IV_PARAM_SPEC);
        Cipher.getInstance("ChaCha20-Poly1305").init(opMode, KEY,
                IV_PARAM_SPEC, new SecureRandom());

        try {
            Cipher.getInstance("ChaCha20-Poly1305").init(opMode, KEY,
                    CHACHA20_PARAM_SPEC);
            throw new RuntimeException("IvParameterSpec is needed");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected " + e);
        }

        AlgorithmParameters algorithmParameters =
                AlgorithmParameters.getInstance("ChaCha20-Poly1305");
        algorithmParameters.init(
                new byte[] { 4, 12, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8 });
        try {
            Cipher.getInstance("ChaCha20").init(opMode, KEY,
                    algorithmParameters, new SecureRandom());
            throw new RuntimeException(
                    "ChaCha20 cipher doesn't accept AlgorithmParameters");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected " + e);
        }
        Cipher.getInstance("ChaCha20-Poly1305").init(opMode, KEY,
                algorithmParameters, new SecureRandom());
    }

    private static void testInitOnWrap(int opMode) throws Exception {
        String opModeName = getOpModeName(opMode);
        System.out.println("== init (" + opModeName + ") ==");

        Cipher chacha20Cipher = Cipher.getInstance("ChaCha20");
        try {
            chacha20Cipher.init(opMode, KEY, new SecureRandom());
            throw new RuntimeException(
                    "Unexpected opration mode: " + opModeName);
        } catch (Exception e) {
            if (e instanceof UnsupportedOperationException) {
                System.out.println("Expected " + e);
            } else {
                throw new RuntimeException("Unexpected exception: " + e);
            }
        }
    }

    private static void testAEAD() throws Exception {
        byte[] expectedPlainttext = HexFormat.of().parseHex("01234567");
        byte[] ciphertext = testUpdateAAD(Cipher.ENCRYPT_MODE, expectedPlainttext);
        byte[] plaintext = testUpdateAAD(Cipher.DECRYPT_MODE, ciphertext);
        if (!Arrays.equals(plaintext, expectedPlainttext)) {
            System.out.println("ciphertext: " + Arrays.toString(ciphertext));
            System.out.println("plaintext: " + Arrays.toString(plaintext));
            throw new RuntimeException("AEAD failed");
        }
    }

    private static byte[] testUpdateAAD(int opMode, byte[] input)
            throws Exception {
        String opModeName = getOpModeName(opMode);
        System.out.println("== updateAAD (" + opModeName + ") ==");

        byte[] aad = HexFormat.of().parseHex("0000");
        ByteBuffer aadBuf = ByteBuffer.wrap(aad);

        Cipher cipher = Cipher.getInstance("ChaCha20");
        cipher.init(opMode, KEY, CHACHA20_PARAM_SPEC);
        try {
            cipher.updateAAD(aadBuf);
            throw new RuntimeException("ChaCha20 cipher cannot apply AAD");
        } catch (IllegalStateException e) {
            System.out.println("Expected " + e);
        }

        Cipher aeadCipher = Cipher.getInstance("ChaCha20-Poly1305");
        try {
            aeadCipher.updateAAD(aadBuf);
            throw new RuntimeException(
                    "Cannot update AAD on uninitialized Cipher");
        } catch (IllegalStateException e) {
            System.out.println("Expected " + e);
        }
        aeadCipher.init(opMode, KEY, IV_PARAM_SPEC);
        aeadCipher.update(input);
        try {
            aeadCipher.updateAAD(aad);
            throw new RuntimeException(
                    "Cannot update AAD after plaintext/cipertext update");
        } catch (IllegalStateException e) {
            System.out.println("Expected " + e);
        }

        aeadCipher = Cipher.getInstance("ChaCha20-Poly1305");
        aeadCipher.init(opMode, KEY, IV_PARAM_SPEC);
        aeadCipher.updateAAD(aadBuf);
        return aeadCipher.doFinal(input);
    }

    private static void testGetBlockSize() throws Exception {
        testGetBlockSize(Cipher.ENCRYPT_MODE);
        testGetBlockSize(Cipher.DECRYPT_MODE);
    }

    private static void testGetBlockSize(int opMode) throws Exception {
        System.out.println("== getBlockSize (" + getOpModeName(opMode) + ") ==");

        Cipher cipher = Cipher.getInstance("ChaCha20");
        cipher.init(opMode, KEY, CHACHA20_PARAM_SPEC);
        if (cipher.getBlockSize() != 0) {
            throw new RuntimeException("Block size must be 0");
        }
    }

    private static String getOpModeName(int opMode) {
        switch (opMode) {
        case Cipher.ENCRYPT_MODE:
            return "ENCRYPT";

        case Cipher.DECRYPT_MODE:
            return "DECRYPT";

        case Cipher.WRAP_MODE:
            return "WRAP";

        case Cipher.UNWRAP_MODE:
            return "UNWRAP";

        default:
            return "";
        }
    }
}
