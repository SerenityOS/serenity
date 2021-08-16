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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * @test
 * @bug 8043836
 * @summary Test AES ciphers with different modes and padding schemes (ECB mode
 *          doesn't use IV). The test tries 3 different read methods of
 *          CipherInputStream.
 * @key randomness
 */
public class CICO {
    private static final String ALGORITHM = "aEs";
    private static final String[] MODES = { "PCBC", "ECb", "cbC", "cFB",
        "cFB24", "cFB32", "Cfb40", "CFB72", "OfB", "OfB20", "OfB48",
        "OfB56", "OFB64", "OFB112", "CFB112", "pCbC" };
    private static final String[] PADDING = { "noPadding", "pkcs5padding" };
    private static final String PROVIDER = "SunJCE";
    private static final int NREADS = 3;
    private static final int KEY_LENGTH = 128;

    private final byte[] plainText = new byte[1600000];


    public static void main(String argv[]) throws Exception {
        CICO test = new CICO();
        for (String mode : MODES) {
            for (String pad : PADDING) {
                for (int m = 0; m < NREADS; m++) {
                    test.runTest(ALGORITHM, mode, pad, m);
                }
            }
        }
    }

    public void runTest(String algo, String mo, String pad, int whichRead) throws Exception {
        Cipher ci1 = null;
        Cipher ci2 = null;
        byte[] iv = null;
        AlgorithmParameterSpec aps = null;
        SecretKey key = null;

        try {
            // Do initialization
            Random rdm = new Random();
            rdm.nextBytes(plainText);
            KeyGenerator kg = KeyGenerator.getInstance(algo, PROVIDER);
            if (!kg.getAlgorithm().equals(algo)) {
                throw new RuntimeException("Unexpected algorithm <"
                        + kg.getAlgorithm() + ">, expected value is <" + algo
                        + ">");
            }

            kg.init(KEY_LENGTH);
            key = kg.generateKey();

            ci1 = Cipher.getInstance(algo + "/" + mo + "/" + pad, PROVIDER);

            if (mo.equalsIgnoreCase("ECB")) {
                ci1.init(Cipher.ENCRYPT_MODE, key);
            } else {
                ci1.init(Cipher.ENCRYPT_MODE, key, aps);
            }

            if (!mo.equalsIgnoreCase("ECB")) {
                iv = ci1.getIV();
                aps = new IvParameterSpec(iv);
            } else {
                aps = null;
            }

            ci2 = Cipher.getInstance(algo + "/" + mo + "/" + pad, PROVIDER);
            if (mo.equalsIgnoreCase("ECB")) {
                ci2.init(Cipher.DECRYPT_MODE, key);
            } else {
                ci2.init(Cipher.DECRYPT_MODE, key, aps);
            }

            ByteArrayInputStream baInput = new ByteArrayInputStream(plainText);
            ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
            try (CipherInputStream ciInput = new CipherInputStream(baInput, ci1);
                    CipherOutputStream ciOutput = new CipherOutputStream(
                            baOutput, ci2)) {
                // According to specification, CipherInputStream does not support the
                // mark and reset methods
                if (ciInput.markSupported()) {
                    throw new RuntimeException(
                            "CipherInputStream unexpectedly supports the mark and reset methods");
                }

                // Read from the input and write to the output using 2 types
                // of buffering : byte[] and int
                switch (whichRead) {
                case 0:
                    int buffer0 = ciInput.read();
                    while (buffer0 != -1) {
                        ciOutput.write(buffer0);
                        buffer0 = ciInput.read();
                    }
                    break;
                case 1:
                    byte[] buffer1 = new byte[20];
                    int len1 = ciInput.read(buffer1);
                    while (len1 != -1) {
                        ciOutput.write(buffer1, 0, len1);
                        len1 = ciInput.read(buffer1);
                    }
                    break;
                case NREADS - 1:
                    byte[] buffer2 = new byte[ci1
                                              .getOutputSize(plainText.length)];
                    int offset2 = 0;
                    int len2 = 0;
                    while (len2 != -1) {
                        len2 = ciInput.read(buffer2, offset2, buffer2.length
                                - offset2);
                        offset2 += len2;
                    }
                    ciOutput.write(buffer2, 0, buffer2.length);
                    break;
                }
            }

            // Get the output
            byte[] recoveredText = new byte[baOutput.size()];
            recoveredText = baOutput.toByteArray();
            if (!java.util.Arrays.equals(plainText, recoveredText)) {
                throw new RuntimeException(
                        "Original text is not equal with recovered text, with "
                                + algo + "/" + mo + "/" + pad + "/" + whichRead);
            }

            // Compare input and output

        } catch (NoSuchAlgorithmException e) {
            //OFB20 is for negative testing
            if (!mo.equalsIgnoreCase("OFB20")) {
                System.out.println("Unexpected NoSuchAlgorithmException with "
                        + algo + "/" + mo + "/" + pad + "/" + whichRead);
                throw new RuntimeException("Test failed!");
            }
        } catch (IOException | NoSuchProviderException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.out.println("Unexpected Exception with "
                    + algo + "/" + mo + "/" + pad + "/" + whichRead);
            System.out.println("Test failed!");
            throw e;
        }
    }
}
