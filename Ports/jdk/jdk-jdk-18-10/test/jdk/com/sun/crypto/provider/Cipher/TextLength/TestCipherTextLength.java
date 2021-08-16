/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/*
 * @test
 * @bug 8048601
 * @summary Performs multiple-part encryption/decryption depending on the
 *  specified encryption mode and check if the results obtained by
 *  different ways are the same.
 */
public class TestCipherTextLength {

    /* Algorithms tested by DESCipherWrapper */
    private static final String[] DES_ALGORITHMS = {"DES", "DESede",
        "Blowfish"};
    private static final String[] DES_MODES = {"ECB", "CBC", "PCBC"};
    private static final String[] DES_PADDING = {"PKCS5Padding"};

    /* Algorithms tested by PBECipherWrapper */
    private static final String[] PBE_ALGORITHMS = {"PBEWithMD5AndDES",
        "PBEWithMD5AndDES/CBC/PKCS5Padding", "PBEWithMD5ANDTripleDES",
        "PBEWithMD5AndTripleDES/CBC/PKCS5Padding", "PBEwithSHA1AndDESede",
        "PBEwithSHA1AndDESede/CBC/PKCS5Padding", "PBEwithSHA1AndRC2_40",
        "PBEwithSHA1Andrc2_40/CBC/PKCS5Padding", "PBEWithSHA1AndRC2_128",
        "PBEWithSHA1andRC2_128/CBC/PKCS5Padding", "PBEWithSHA1AndRC4_40",
        "PBEWithsha1AndRC4_40/ECB/NoPadding", "PBEWithSHA1AndRC4_128",
        "PBEWithSHA1AndRC4_128/ECB/NoPadding", "PBEWithHmacSHA1AndAES_128",
        "PBEWithHmacSHA224AndAES_128", "PBEWithHmacSHA256AndAES_128",
        "PBEWithHmacSHA384AndAES_128", "PBEWithHmacSHA512AndAES_128",
        "PBEWithHmacSHA1AndAES_256", "PBEWithHmacSHA224AndAES_256",
        "PBEWithHmacSHA256AndAES_256", "PBEWithHmacSHA384AndAES_256",
        "PBEWithHmacSHA512AndAES_256", "PBKDF2WithHmacSHA1",
        "PBKDF2WithHmacSHA224", "PBKDF2WithHmacSHA256",
        "PBKDF2WithHmacSHA384", "PBKDF2WithHmacSHA512"};
    private static final String PBE_PASSWORD = "Hush, it's a secret!!";

    // Algorithm tested by PBKDF2Wrappter
    private static final String PBKDF2 = "PBKDF2";

    // Algorithm tested by AESPBEWrapper
    private static final String AES = "AES";

    public static void main(String[] args) throws Exception {
        byte[] plainText = new byte[64];
        for (int i = 0; i < 64; i++) {
            plainText[i] = (byte) (i & 0xff);
        }

        new TestCipherTextLength().runAll(plainText);
    }

    public void runAll(byte[] plainText) throws Exception {

        // Testing DES/Blowfish Cipher
        for (String algorithm : DES_ALGORITHMS) {
            for (String desMode : DES_MODES) {
                for (String padding : DES_PADDING) {
                    out.println("=>Testing: " + algorithm + "/" + desMode
                            + "/" + padding);
                    DESCipherWrapper desCi = new DESCipherWrapper(algorithm,
                            desMode, padding);
                    desCi.execute(Cipher.ENCRYPT_MODE, plainText);
                    desCi.execute(Cipher.DECRYPT_MODE, desCi.getResult());
                    if (!Arrays.equals(plainText, desCi.getResult())) {
                        throw new RuntimeException(
                                "Plain and recovered texts are not same for:"
                                + algorithm + "/" + desMode + "/"
                                + padding);
                    }
                }
            }
        }

        // Testing PBE Cipher
        for (String algorithm : PBE_ALGORITHMS) {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength(algorithm);
            boolean isUnlimited = maxKeyLen == Integer.MAX_VALUE;
            if (!isUnlimited
                    && (algorithm.contains("TripleDES") || algorithm
                    .contains("AES_256"))) {
                out.println("Test " + algorithm + " will be ignored");
                continue;
            }

            out.println("=>Testing: " + algorithm);
            PBECipherWrapper pbeCi = createWrapper(algorithm, PBE_PASSWORD);
            pbeCi.execute(Cipher.ENCRYPT_MODE, plainText);
            pbeCi.execute(Cipher.DECRYPT_MODE, pbeCi.getResult());
            if (!Arrays.equals(plainText, pbeCi.getResult())) {
                throw new RuntimeException(
                        "Plain and recovered texts are not same for:"
                        + algorithm);
            }
        }
    }

    private PBECipherWrapper createWrapper(String algo, String passwd)
            throws InvalidKeySpecException, NoSuchAlgorithmException,
            NoSuchPaddingException {
        if (algo.contains(PBKDF2)) {
            return new PBECipherWrapper.PBKDF2(algo, passwd);
        } else if (algo.contains(AES)) {
            return new PBECipherWrapper.AES(algo, passwd);
        } else {
            return new PBECipherWrapper.Legacy(algo, passwd);
        }
    }
}
