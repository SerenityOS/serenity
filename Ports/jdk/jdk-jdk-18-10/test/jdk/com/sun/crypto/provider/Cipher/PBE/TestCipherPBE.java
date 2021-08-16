/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/*
 * @test
 * @bug 8048601
 * @summary Tests for PBE ciphers
 */
public class TestCipherPBE {

    private static final String[] ALGORITHMS = {"PBEWithMD5AndDES",
        "PBEWithMD5AndDES/CBC/PKCS5Padding", "PBEWithMD5AndTripleDES",
        "PBEWithMD5AndTripleDES/CBC/PKCS5Padding"};

    private static final String KEY_ALGO = "pbeWithMD5ANDdes";
    private final byte[] SALT;
    private final byte[] PLAIN_TEXT;

    public TestCipherPBE() {
        SALT = generateBytes(8);
        PLAIN_TEXT = generateBytes(200);
    }

    public static void main(String[] args) throws Exception {

        new TestCipherPBE().runAll();
    }

    private void runAll() throws Exception {
        for (String algorithm : ALGORITHMS) {
            runTest(algorithm);
        }
    }

    private void runTest(String algorithm)
            throws InvalidKeySpecException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, ShortBufferException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {

        out.println("=> Testing: " + algorithm);

        boolean isUnlimited =
            (Cipher.getMaxAllowedKeyLength(algorithm) == Integer.MAX_VALUE);

        try {
            // Initialization
            AlgorithmParameterSpec algoParamSpec
                    = new PBEParameterSpec(SALT, 6);

            SecretKey secretKey
                    = SecretKeyFactory.getInstance(KEY_ALGO).generateSecret(
                    new PBEKeySpec(("Secret Key Value").toCharArray()));

            Cipher ci = Cipher.getInstance(algorithm);
            ci.init(Cipher.ENCRYPT_MODE, secretKey, algoParamSpec);

            // Encryption
            byte[] cipherText = ci.doFinal(PLAIN_TEXT);

            // Decryption
            ci.init(Cipher.DECRYPT_MODE, secretKey, algoParamSpec);
            byte[] recoveredText = ci.doFinal(cipherText);

            if (algorithm.contains("TripleDES") && !isUnlimited) {
                throw new RuntimeException(
                        "Expected InvalidKeyException not thrown");
            }

            // Comparison
            if (!Arrays.equals(PLAIN_TEXT, recoveredText)) {
                throw new RuntimeException(
                        "Test failed: plainText is not equal to recoveredText");
            }
            out.println("Test Passed.");
        } catch (InvalidKeyException ex) {
            if (algorithm.contains("TripleDES") && !isUnlimited) {
                out.println("Expected InvalidKeyException thrown");
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    public static byte[] generateBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i & 0xff);
        }
        return bytes;
    }
}
