/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
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
 * @summary Test AES ciphers with 4 different modes with NoPadding. Check if
 *          data before encryption and after decryption is the same.
 * @key randomness
 */

public class CTR {

    private static final String ALGORITHM = "AES";

    private static final String PROVIDER = "SunJCE";

    private static final String[] MODES = {"CTR","CFB24","OFB32","GCM"};

    private static final String PADDING = "NoPadding";


    private static final int KEY_LENGTH = 128;

    public static void main(String argv[]) throws Exception {
        CTR test = new CTR();
        for (String mode : MODES) {
            test.runTest(ALGORITHM, mode, PADDING);
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
                Arrays.toString(plainText);
                if (!java.util.Arrays.equals(plainText, tmp)) {
                    System.out.println("Original: ");
                    dumpBytes(plainText);
                    System.out.println("Recovered: ");
                    dumpBytes(tmp);
                    throw new RuntimeException("Original text is not equal with recovered text, with mode:" + mo);
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | ShortBufferException | IllegalBlockSizeException
                | BadPaddingException e) {
            System.out.println("Test failed!");
            throw e;
        }
    }

    private void dumpBytes(byte[] bytes){
        for (byte b : bytes){
            System.out.print(Integer.toHexString(b));
        }
        System.out.println();
    }
}
