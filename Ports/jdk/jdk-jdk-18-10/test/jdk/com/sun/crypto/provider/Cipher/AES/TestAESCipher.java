/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HexFormat;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @test
 * @bug 8043836
 * @summary Test AES ciphers with different modes and padding schemes (ECB mode
 *          doesn't use IV).
 * @author Liwen Wang
 * @author Parag Salvi
 */
public class TestAESCipher {

    private static final String ALGORITHM = "AES";
    private static final String PROVIDER = "SunJCE";
    private static final String[] MODES = { "ECb", "CbC", "CTR", "PCBC", "OFB",
        "OFB150", "cFB", "CFB7", "cFB8", "cFB16", "cFB24", "cFB32",
        "Cfb40", "cfB48", "cfB56", "cfB64", "cfB72", "cfB80", "cfB88",
        "cfB96", "cfb104", "cfB112", "cfB120", "OFB8", "OFB16", "OFB24",
        "OFB32", "OFB40", "OFB48", "OFB56", "OFB64", "OFB72", "OFB80",
        "OFB88", "OFB96", "OFB104", "OFB112", "OFB120", "GCM" };
    private static final String[] PADDING = { "NoPadding", "PKCS5Padding" };
    private static final int KEY_LENGTH = 16;
    static byte[] plainText = new byte[128];
    static byte[] key = new byte[KEY_LENGTH];

    public static void main(String argv[]) throws Exception {
        TestAESCipher test = new TestAESCipher();
        for (String mode : MODES) {
            int padKinds = 1;
            if (mode.equalsIgnoreCase("ECB") || mode.equalsIgnoreCase("PCBC")
                    || mode.equalsIgnoreCase("CBC")) {
                padKinds = PADDING.length;
            }

            for (int k = 0; k < padKinds; k++) {
                test.runTest(ALGORITHM, mode, PADDING[k]);
            }
        }
    }


    public void runTest(String algo, String mo, String pad) throws Exception {
        Cipher ci;
        System.out.println("Testing " + algo + "/" + mo + "/" + pad);

        byte[] iv = new byte[16];
        AlgorithmParameterSpec aps = new GCMParameterSpec(128, iv);
        SecretKey key = new SecretKeySpec(this.key, 0, KEY_LENGTH,"AES");

        try {
            // Initialization
            ci = Cipher.getInstance(algo + "/" + mo + "/" + pad, PROVIDER);

            // encrypt
            if (mo.equalsIgnoreCase("GCM")) {
                ci.init(Cipher.ENCRYPT_MODE, key, aps);
            } else if (mo.equalsIgnoreCase("ECB")) {
                ci.init(Cipher.ENCRYPT_MODE, key, (AlgorithmParameterSpec)null);
            } else {
                ci.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            }

            byte[] cipherText = new byte[ci.getOutputSize(plainText.length)];
            int offset = ci.update(plainText, 0, plainText.length, cipherText,
                0);
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

            // Comparison
            if (!java.util.Arrays.equals(plainText, 0 , plainText.length,
                recoveredText, 0, len)) {
                System.out.println("Original: ");
                System.out.println(HexFormat.of().formatHex(plainText));
                System.out.println("Recovered: ");
                System.out.println(HexFormat.of().
                    formatHex(recoveredText, 0, len));
                throw new RuntimeException("Original text is not equal with " +
                    "recovered text, with mode:" + mo);
            }

        } catch (NoSuchAlgorithmException e) {
            //CFB7 and OFB150 are for negative testing
            if (!mo.equalsIgnoreCase("CFB7") && !mo.equalsIgnoreCase("OFB150")) {
                System.out.println("Unexpected NoSuchAlgorithmException with" +
                    " mode: " + mo);
                throw new RuntimeException("Test failed!");
            }
        }  catch ( NoSuchProviderException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | ShortBufferException | IllegalBlockSizeException
                | BadPaddingException e) {
            System.out.println("Test failed!");
            throw e;
        }
    }
}
