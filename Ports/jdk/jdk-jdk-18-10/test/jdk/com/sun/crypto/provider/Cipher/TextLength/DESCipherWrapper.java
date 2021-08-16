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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/**
 * Wrapper class to test a given DES algorithm.
 */
public class DESCipherWrapper {

    private final Cipher ci;
    private final byte[] iv;
    private final SecretKey key;
    private final String algo;
    private final String mode;
    private final String pad;
    private final int keyStrength;
    private byte[] resultText = null;

    public DESCipherWrapper(String algo, String mode, String pad)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        ci = Cipher.getInstance(algo + "/" + mode + "/" + pad);

        iv = new byte[8];
        for (int i = 0; i < 8; i++) {
            iv[i] = (byte) (i & 0xff);
        }

        KeyGenerator kg = KeyGenerator.getInstance(algo);
        key = kg.generateKey();
        keyStrength = algo.equalsIgnoreCase("DESede") ? 112
                : key.getEncoded().length * 8;

        this.algo = algo;
        this.mode = mode;
        this.pad = pad;
    }

    public byte[] getResult() {
        return resultText.clone();
    }

    public void execute(int edMode, byte[] inputText)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException,
            ShortBufferException, NoSuchAlgorithmException {
        AlgorithmParameterSpec aps = null;

        try {
            if (!mode.equalsIgnoreCase("ECB")) {
                aps = new IvParameterSpec(iv);
            }
            ci.init(edMode, key, aps);

            // Generate a resultText using a single-part enc/dec
            resultText = ci.doFinal(inputText);

            // Generate outputText for each multi-part en/de-cryption
            /* Combination #1:
            update(byte[], int, int)
            doFinal(byte[], int, int)
             */
            byte[] part11 = ci.update(inputText, 0, inputText.length);
            byte[] part12 = ci.doFinal();
            byte[] outputText1 = new byte[part11.length + part12.length];
            System.arraycopy(part11, 0, outputText1, 0, part11.length);
            System.arraycopy(part12, 0, outputText1, part11.length,
                    part12.length);

            List<byte[]> outputTexts = new ArrayList<>(4);
            outputTexts.add(outputText1);

            /* Combination #2:
            update(byte[], int, int)
            doFinal(byte[], int, int, byte[], int)
             */
            byte[] part21 = ci.update(inputText, 0, inputText.length - 5);
            byte[] part22 = new byte[ci.getOutputSize(inputText.length)];
            int len2 = ci
                    .doFinal(inputText, inputText.length - 5, 5, part22, 0);
            byte[] outputText2 = new byte[part21.length + len2];
            System.arraycopy(part21, 0, outputText2, 0, part21.length);
            System.arraycopy(part22, 0, outputText2, part21.length, len2);

            outputTexts.add(outputText2);

            /* Combination #3:
            update(byte[], int, int, byte[], int)
            doFinal(byte[], int, int)
             */
            byte[] part31 = new byte[ci.getOutputSize(inputText.length)];
            int len3 = ci.update(inputText, 0, inputText.length - 8, part31, 0);
            byte[] part32 = ci.doFinal(inputText, inputText.length - 8, 8);
            byte[] outputText3 = new byte[len3 + part32.length];
            System.arraycopy(part31, 0, outputText3, 0, len3);
            System.arraycopy(part32, 0, outputText3, len3, part32.length);

            outputTexts.add(outputText3);

            /* Combination #4:
            update(byte[], int, int, byte[], int)
            doFinal(byte[], int, int, byte[], int)
             */
            byte[] part41 = new byte[ci.getOutputSize(inputText.length)];
            int len4 = ci.update(inputText, 0, inputText.length - 8, part41, 0);
            int rest4 = ci.doFinal(inputText, inputText.length - 8, 8, part41,
                    len4);
            byte[] outputText4 = new byte[len4 + rest4];
            System.arraycopy(part41, 0, outputText4, 0, outputText4.length);

            outputTexts.add(outputText4);

            // Compare results
            for (int k = 0; k < outputTexts.size(); k++) {
                if (!Arrays.equals(resultText, outputTexts.get(k))) {
                    out.println(" Testing: " + algo + "/" + mode + "/" + pad);
                    throw new RuntimeException(
                            "Compare value of resultText and combination " + k
                            + " are not same. Test failed.");
                }
            }
            if (keyStrength > Cipher.getMaxAllowedKeyLength(algo)) {
                throw new RuntimeException(
                        "Expected exception uncaught, keyStrength "
                        + keyStrength);
            }
        } catch (InvalidKeyException ex) {
            if (keyStrength <= Cipher.getMaxAllowedKeyLength(algo)) {
                out.println("Unexpected exception in " + algo + "/" + mode
                        + "/" + pad + " ,  KeySize " + keyStrength);
                throw ex;
            }
            out.println("Caught InvalidKeyException as expected");
        }
    }
}
