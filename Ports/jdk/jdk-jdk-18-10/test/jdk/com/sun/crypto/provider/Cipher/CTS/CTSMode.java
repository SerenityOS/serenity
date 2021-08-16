/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5083253
 * @run main CTSMode
 * @summary Verify that CTR mode works as expected
 * @author Valerie Peng
 */

import java.util.*;
import java.security.AlgorithmParameters;
import javax.crypto.*;
import javax.crypto.spec.*;

public class CTSMode {

    private final static byte[] toByteArray(String s) {
        char[] c = s.toCharArray();
        byte[] t = new byte[c.length / 2];
        int n = 0;
        int d1 = -1;
        int d2 = -1;
        for (int i = 0; i < c.length; i++) {
            char ch = c[i];
            if (d1 == -1) {
                d1 = Character.digit(ch, 16);
            } else {
                d2 = Character.digit(ch, 16);
                if (d2 != -1) {
                    t[n++] = (byte)((d1 << 4) | d2);
                    d1 = -1;
                    d2 = -1;
                }
            }
        }
        if (d1 != -1) {
            throw new RuntimeException();
        }
        if (n == t.length) {
            return t;
        }
        byte[] b = new byte[n];
        System.arraycopy(t, 0, b, 0, n);
        return b;
    }

    private final static SecretKey KEY1 =
        new SecretKeySpec(toByteArray("636869636b656e207465726979616b69"),
                          "AES");

    private final static IvParameterSpec IV1 =
        new IvParameterSpec(new byte[16]);

    /*
     * Use test vectors, i.e. PLAIN1 and CIPHER1, from the appendix B
     * of RFC 3962 "Advanced Encryption Standard (AES) Encryption for
     * Kerberos 5".
     */
    private final static byte[][] PLAIN1 = {
        toByteArray("4920776f756c64206c696b652074686520"),
        toByteArray("4920776f756c64206c696b6520746865" +
                    "2047656e6572616c20476175277320"),
        toByteArray("4920776f756c64206c696b6520746865" +
                    "2047656e6572616c2047617527732043"),
        toByteArray("4920776f756c64206c696b6520746865" +
                    "2047656e6572616c2047617527732043" +
                    "6869636b656e2c20706c656173652c"),
        toByteArray("4920776f756c64206c696b6520746865" +
                    "2047656e6572616c2047617527732043" +
                    "6869636b656e2c20706c656173652c20"),
        toByteArray("4920776f756c64206c696b6520746865" +
                    "2047656e6572616c2047617527732043" +
                    "6869636b656e2c20706c656173652c20" +
                    "616e6420776f6e746f6e20736f75702e")
    };
    private final static byte[][] CIPHER1 = {
        toByteArray("c6353568f2bf8cb4d8a580362da7ff7f97"),
        toByteArray("fc00783e0efdb2c1d445d4c8eff7ed22" +
                    "97687268d6ecccc0c07b25e25ecfe5"),
        toByteArray("39312523a78662d5be7fcbcc98ebf5a8" +
                    "97687268d6ecccc0c07b25e25ecfe584"),
        toByteArray("97687268d6ecccc0c07b25e25ecfe584" +
                    "b3fffd940c16a18c1b5549d2f838029e" +
                    "39312523a78662d5be7fcbcc98ebf5"),
        toByteArray("97687268d6ecccc0c07b25e25ecfe584" +
                    "9dad8bbb96c4cdc03bc103e1a194bbd8" +
                    "39312523a78662d5be7fcbcc98ebf5a8"),
        toByteArray("97687268d6ecccc0c07b25e25ecfe584" +
                    "39312523a78662d5be7fcbcc98ebf5a8" +
                    "4807efe836ee89a526730dbc2f7bc840" +
                    "9dad8bbb96c4cdc03bc103e1a194bbd8"),
    };

    private final static byte[] IV2_SRC =
        toByteArray("11223344556677880011223344556677");

    private final static String[] ALGORITHMS2 = {
        "DES", "DESede", "Blowfish", "AES"
    };
    private final static int[] KEYSIZES2 = {
        8, 24, 16, 16
    };

    private static String toString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(Character.forDigit(k >> 4, 16));
            sb.append(Character.forDigit(k & 0xf, 16));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        test1();
        test2();
        test3();
   }

    /**
     * Test with the test vectors and see if results match.
     */
    private static void test1() throws Exception {
        for (int i = 0; i < PLAIN1.length; i++) {
            String algo = KEY1.getAlgorithm();
            int MAX_KEYSIZE = Cipher.getMaxAllowedKeyLength(algo);
            if (KEY1.getEncoded().length > MAX_KEYSIZE) {
                // skip tests using keys whose length exceeds
                // what's configured in jce jurisdiction policy files.
                continue;
            }
            System.out.println("Running test1_" + i +  " (" + algo + ")");
            Cipher cipher = Cipher.getInstance(algo+ "/CTS/NoPadding",
                                               "SunJCE");
            byte[] plainText = PLAIN1[i];
            byte[] cipherText = CIPHER1[i];
            cipher.init(Cipher.ENCRYPT_MODE, KEY1, IV1);
            byte[] enc = cipher.doFinal(plainText);
            if (Arrays.equals(cipherText, enc) == false) {
                System.out.println("plain:  " + toString(plainText));
                System.out.println("cipher: " + toString(cipherText));
                System.out.println("actual: " + toString(enc));
                throw new RuntimeException("Encryption failure for test " + i);
            }
            cipher.init(Cipher.DECRYPT_MODE, KEY1, IV1);
            byte[] dec = cipher.doFinal(cipherText);
            if (Arrays.equals(plainText, dec) == false) {
                System.out.println("cipher: " + toString(cipherText));
                System.out.println("plain:  " + toString(plainText));
                System.out.println("actual: " + toString(enc));
                throw new RuntimeException("Decryption failure for test " + i);
            }
        }
    }

    /**
     * Test with a combination of update/doFinal calls and make
     * sure that same data is recovered after decryption.
     */
    private static void test2() throws Exception {
        for (int i = 0; i < ALGORITHMS2.length; i++) {
            String algo = ALGORITHMS2[i];
            System.out.println("Running test2_" + i +  " (" + algo + ")");
            int keySize = KEYSIZES2[i];
            int MAX_KEYSIZE = Cipher.getMaxAllowedKeyLength(algo);
            if (keySize > MAX_KEYSIZE) {
                // skip tests using keys whose length exceeds
                // what's configured in jce jurisdiction policy files.
                continue;
            }
            Cipher cipher =
                Cipher.getInstance(algo+"/CTS/NoPadding", "SunJCE");
            int blockSize = cipher.getBlockSize();
            SecretKeySpec key = new SecretKeySpec(new byte[keySize], algo);
            // Make sure encryption works for inputs with valid length
            byte[] plainText = PLAIN1[3];
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherText = new byte[plainText.length];
            int firstPartLen = blockSize + 1;
            int processed1 = cipher.update(plainText, 0, firstPartLen,
                                           cipherText, 0);
            int processed2 = cipher.doFinal(plainText, firstPartLen,
                                            plainText.length-firstPartLen,
                                            cipherText, processed1);
            AlgorithmParameters params = cipher.getParameters();
            if ((processed1 + processed2) != plainText.length) {
                System.out.println("processed1 = " + processed1);
                System.out.println("processed2 = " + processed2);
                System.out.println("total length = " + plainText.length);
                throw new RuntimeException("Encryption failure for test " + i);
            }
            // Make sure IllegalBlockSizeException is thrown for inputs
            // with less-than-a-block length
            try {
                cipher.doFinal(new byte[blockSize-1]);
                throw new RuntimeException("Expected IBSE is not thrown");
            } catch (IllegalBlockSizeException ibse) {
            }
            // Make sure data is encrypted as in CBC mode for inputs
            // which is exactly one block long
            IvParameterSpec iv2 = new IvParameterSpec(IV2_SRC, 0, blockSize);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv2);
            Cipher cipher2 =
                Cipher.getInstance(algo+"/CBC/NoPadding", "SunJCE");
            cipher2.init(Cipher.ENCRYPT_MODE, key, iv2);

            byte[] eout = cipher.doFinal(IV2_SRC, 0, blockSize);
            byte[] eout2 = cipher2.doFinal(IV2_SRC, 0, blockSize);
            if (!Arrays.equals(eout, eout2)) {
                throw new RuntimeException("Different encryption output " +
                                           "for CBC and CTS");
            }
            // Make sure decryption works for inputs with valid length
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            byte[] recoveredText =
                new byte[cipher.getOutputSize(cipherText.length)];
            processed1 = cipher.update(cipherText, 0, firstPartLen,
                                       recoveredText, 0);
            processed2 = cipher.update(cipherText, firstPartLen,
                                       cipherText.length-firstPartLen,
                                       recoveredText, processed1);
            int processed3 =
                cipher.doFinal(recoveredText, processed1+processed2);
            if ((processed1 + processed2 + processed3) != plainText.length) {
                System.out.println("processed1 = " + processed1);
                System.out.println("processed2 = " + processed2);
                System.out.println("processed3 = " + processed3);
                System.out.println("total length = " + plainText.length);
                throw new RuntimeException("Decryption failure for test " + i);
            }
            if (Arrays.equals(plainText, recoveredText) == false) {
                System.out.println("plain:  " + toString(plainText));
                System.out.println("recovered: " + toString(recoveredText));
                throw new RuntimeException("Decryption failure for test " + i);
            }
            // Make sure IllegalBlockSizeException is thrown for inputs
            // with less-than-a-block length
            try {
                cipher.doFinal(new byte[blockSize-1]);
                throw new RuntimeException("Expected IBSE is not thrown");
            } catch (IllegalBlockSizeException ibse) {
            }
            // Make sure data is decrypted as in CBC mode for inputs
            // which is exactly one block long
            cipher.init(Cipher.DECRYPT_MODE, key, iv2);
            cipher2.init(Cipher.DECRYPT_MODE, key, iv2);
            byte[] dout = cipher.doFinal(eout);
            byte[] dout2 = cipher2.doFinal(eout2);
            if (!Arrays.equals(dout, dout2)) {
                throw new RuntimeException("Different decryption output " +
                                           "for CBC and CTS");
            }
        }
    }

    /**
     * Test with a shortBuffer and see if encryption/decryption
     * still works correctly afterwards.
     */
    private static void test3() throws Exception {
        // Skip PLAIN1[0, 1, 2] due to their lengths
        for (int i = 3; i < PLAIN1.length; i++) {
            String algo = KEY1.getAlgorithm();
            System.out.println("Running test3_" + i +  " (" + algo + ")");
            int MAX_KEYSIZE = Cipher.getMaxAllowedKeyLength(algo);
            if (KEY1.getEncoded().length > MAX_KEYSIZE) {
                // skip tests using keys whose length exceeds
                // what's configured in jce jurisdiction policy files.
                continue;
            }
            Cipher cipher =
                Cipher.getInstance(algo+ "/CTS/NoPadding", "SunJCE");
            byte[] plainText = PLAIN1[i];
            byte[] cipherText = CIPHER1[i];
            byte[] enc = new byte[plainText.length];
            cipher.init(Cipher.ENCRYPT_MODE, KEY1, IV1);
            int halfInput = plainText.length/2;
            int processed1 = cipher.update(plainText, 0, halfInput,
                                           enc, 0);
            try {
                cipher.doFinal(plainText, halfInput,
                               plainText.length-halfInput,
                               new byte[1], 0);
                throw new RuntimeException("Expected Exception is not thrown");
            } catch (ShortBufferException sbe) {
                // expected exception thrown; retry
                int processed2 =
                    cipher.doFinal(plainText, halfInput,
                                   plainText.length-halfInput,
                                   enc, processed1);
                if ((processed1 + processed2) != enc.length) {
                    System.out.println("processed1 = " + processed1);
                    System.out.println("processed2 = " + processed2);
                    System.out.println("total length = " + enc.length);
                    throw new RuntimeException("Encryption length check " +
                                               "failed");
                }
            }
            if (Arrays.equals(cipherText, enc) == false) {
                System.out.println("plain:  " + toString(plainText));
                System.out.println("cipher: " + toString(cipherText));
                System.out.println("actual: " + toString(enc));
                throw new RuntimeException("Encryption failure for test " + i);
            }
            cipher.init(Cipher.DECRYPT_MODE, KEY1, IV1);
            byte[] dec =
                new byte[cipher.getOutputSize(cipherText.length)];
            processed1 = cipher.update(cipherText, 0, halfInput,
                                       dec, 0);
            try {
                cipher.update(cipherText, halfInput,
                              cipherText.length-halfInput,
                              new byte[1], 0);
                throw new RuntimeException("Expected Exception is not thrown");
            } catch (ShortBufferException sbe) {
                // expected exception thrown; retry
                int processed2 = cipher.update(cipherText, halfInput,
                                               cipherText.length-halfInput,
                                               dec, processed1);
                int processed3 =
                    cipher.doFinal(dec, processed1+processed2);
                if ((processed1 + processed2 + processed3) != dec.length) {
                    System.out.println("processed1 = " + processed1);
                    System.out.println("processed2 = " + processed2);
                    System.out.println("processed3 = " + processed3);
                    System.out.println("total length = " + dec.length);
                    throw new RuntimeException("Decryption length check " +
                                               "failed");
                }
            }
            if (Arrays.equals(plainText, dec) == false) {
                System.out.println("cipher: " + toString(cipherText));
                System.out.println("plain:  " + toString(plainText));
                System.out.println("actualD: " + toString(dec));
                throw new RuntimeException("Decryption failure for test " + i);
            }
        }
    }
}
