/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

/**
 * @test
 * @bug 8043836
 * @summary Test AES ciphers with different modes and padding schemes (ECB mode
 *          doesn't use IV). The test tries 3 different read methods of
 *          CipherInputStream.
 * @key randomness
 */
public class Padding {

    private static final String ALGORITHM = "AES";
    private static final String PROVIDER = "SunJCE";
    private static final String[] MODES_PKCS5PAD = {
        "ECb", "CbC", "PCBC", "OFB",
        "OFB150", "cFB", "CFB7", "cFB8", "cFB16", "cFB24", "cFB32",
        "Cfb40", "cfB48", "cfB56", "cfB64", "cfB72", "cfB80", "cfB88",
        "cfB96", "cfb104", "cfB112", "cfB120", "OFB8", "OFB16", "OFB24",
        "OFB32", "OFB40", "OFB48", "OFB56", "OFB64", "OFB72", "OFB80",
        "OFB88", "OFB96", "OFB104", "OFB112", "OFB120" };
    private static final String[] MODES_NOPAD = { "CTR", "CTS", "GCM" };

    private static final int KEY_LENGTH = 128;

    public static void main(String argv[]) throws Exception {
        Padding test = new Padding();
        for (String mode : MODES_PKCS5PAD) {
            test.runTest(ALGORITHM, mode, "PKCS5Padding");
        }
        for (String mode : MODES_NOPAD) {
            test.runTest(ALGORITHM, mode, "NoPadding");
        }
    }

    public void runTest(String algo, String mo, String pad) throws Exception {
        Cipher ci = null;
        byte[] iv = null;
        AlgorithmParameterSpec aps = null;
        SecretKey key = null;
        try {
            Random rdm = new Random();
            byte[] plainText;

            ci = Cipher.getInstance(algo + "/" + mo + "/" + pad, PROVIDER);
            KeyGenerator kg = KeyGenerator.getInstance(algo, PROVIDER);
            kg.init(KEY_LENGTH);
            key = kg.generateKey();

            for (int i = 0; i < 15; i++) {
                plainText = new byte[1600 + i + 1];
                rdm.nextBytes(plainText);

                if (!mo.equalsIgnoreCase("GCM")) {
                    ci.init(Cipher.ENCRYPT_MODE, key, aps);
                } else {
                    ci.init(Cipher.ENCRYPT_MODE, key);
                }

                byte[] cipherText = new byte[ci.getOutputSize(plainText.length)];
                int offset = ci.update(plainText, 0, plainText.length,
                        cipherText, 0);
                ci.doFinal(cipherText, offset);
                if (!mo.equalsIgnoreCase("ECB")) {
                    iv = ci.getIV();
                    aps = new IvParameterSpec(iv);
                } else {
                    aps = null;
                }

                if (!mo.equalsIgnoreCase("GCM")) {
                    ci.init(Cipher.DECRYPT_MODE, key, aps);
                } else {
                    ci.init(Cipher.DECRYPT_MODE, key, ci.getParameters());
                }

                byte[] recoveredText = new byte[ci.getOutputSize(cipherText.length)];
                int len = ci.doFinal(cipherText, 0, cipherText.length,
                        recoveredText);
                byte[] tmp = new byte[len];

                for (int j = 0; j < len; j++) {
                    tmp[j] = recoveredText[j];
                }

                if (!java.util.Arrays.equals(plainText, tmp)) {
                    System.out.println("Original: ");
                    dumpBytes(plainText);
                    System.out.println("Recovered: ");
                    dumpBytes(tmp);
                    throw new RuntimeException(
                            "Original text is not equal with recovered text, with mode:"
                                    + mo);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            //CFB7 and OFB150 are for negative testing
            if (!mo.equalsIgnoreCase("CFB7") && !mo.equalsIgnoreCase("OFB150")) {
                System.out
                .println("Unexpected NoSuchAlgorithmException with mode: "
                        + mo);
                throw new RuntimeException("Test failed!");
            }
        } catch ( NoSuchProviderException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | ShortBufferException | IllegalBlockSizeException
                | BadPaddingException e) {
            System.out.println("Test failed!");
            throw e;
        }
    }

    private void dumpBytes(byte[] bytes) {
        for (byte b : bytes) {
            System.out.print(Integer.toHexString(b));
        }

        System.out.println();
    }
}
