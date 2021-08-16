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

import static javax.crypto.Cipher.ENCRYPT_MODE;
import static javax.crypto.Cipher.getMaxAllowedKeyLength;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

/*
 * @test
 * @bug 8075286
 * @summary Test the AES algorithm OIDs in JDK.
 *          OID and Algorithm transformation string should match.
 *          Both could be able to be used to generate the algorithm instance.
 * @run main TestAESOids
 */
public class TestAESOids {

    private static final String PROVIDER_NAME = "SunJCE";
    private static final byte[] INPUT = "1234567890123456".getBytes();

    private static final List<DataTuple> DATA = Arrays.asList(
            new DataTuple("2.16.840.1.101.3.4.1.1", "AES_128/ECB/NoPadding",
                    128, "ECB"),
            new DataTuple("2.16.840.1.101.3.4.1.2", "AES_128/CBC/NoPadding",
                    128, "CBC"),
            new DataTuple("2.16.840.1.101.3.4.1.3", "AES_128/OFB/NoPadding",
                    128, "OFB"),
            new DataTuple("2.16.840.1.101.3.4.1.4", "AES_128/CFB/NoPadding",
                    128, "CFB"),
            new DataTuple("2.16.840.1.101.3.4.1.21", "AES_192/ECB/NoPadding",
                    192, "ECB"),
            new DataTuple("2.16.840.1.101.3.4.1.22", "AES_192/CBC/NoPadding",
                    192, "CBC"),
            new DataTuple("2.16.840.1.101.3.4.1.23", "AES_192/OFB/NoPadding",
                    192, "OFB"),
            new DataTuple("2.16.840.1.101.3.4.1.24", "AES_192/CFB/NoPadding",
                    192, "CFB"),
            new DataTuple("2.16.840.1.101.3.4.1.41", "AES_256/ECB/NoPadding",
                    256, "ECB"),
            new DataTuple("2.16.840.1.101.3.4.1.42", "AES_256/CBC/NoPadding",
                    256, "CBC"),
            new DataTuple("2.16.840.1.101.3.4.1.43", "AES_256/OFB/NoPadding",
                    256, "OFB"),
            new DataTuple("2.16.840.1.101.3.4.1.44", "AES_256/CFB/NoPadding",
                    256, "CFB"));

    public static void main(String[] args) throws Exception {
        for (DataTuple dataTuple : DATA) {
            int maxAllowedKeyLength =
                    getMaxAllowedKeyLength(dataTuple.algorithm);
            boolean supportedKeyLength =
                    maxAllowedKeyLength >= dataTuple.keyLength;

            try {
                runTest(dataTuple, supportedKeyLength);
                System.out.println("passed");
            } catch (InvalidKeyException ike) {
                if (supportedKeyLength) {
                    throw new RuntimeException(String.format(
                            "The key length %d is supported, but test failed.",
                            dataTuple.keyLength), ike);
                } else {
                    System.out.printf(
                            "Catch expected InvalidKeyException due "
                                    + "to the key length %d is greater than "
                                    + "max supported key length %d%n",
                            dataTuple.keyLength, maxAllowedKeyLength);
                }
            }
        }
    }

    private static void runTest(DataTuple dataTuple,
            boolean supportedKeyLength) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        Cipher algorithmCipher = Cipher.getInstance(dataTuple.algorithm,
                PROVIDER_NAME);
        Cipher oidCipher = Cipher.getInstance(dataTuple.oid, PROVIDER_NAME);

        if (algorithmCipher == null) {
            throw new RuntimeException(
                    String.format("Test failed: algorithm string %s getInstance"
                            + " failed.%n", dataTuple.algorithm));
        }

        if (oidCipher == null) {
            throw new RuntimeException(
                    String.format("Test failed: OID %s getInstance failed.%n",
                            dataTuple.oid));
        }

        if (!algorithmCipher.getAlgorithm().equals(dataTuple.algorithm)) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance "
                            + "doesn't generate expected algorithm.%n",
                    dataTuple.algorithm));
        }

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(dataTuple.keyLength);
        SecretKey key = kg.generateKey();

        // encrypt
        algorithmCipher.init(ENCRYPT_MODE, key);
        if (!supportedKeyLength) {
            throw new RuntimeException(String.format(
                    "The key length %d is not supported, so the initialization "
                            + "of algorithmCipher should fail.%n",
                    dataTuple.keyLength));
        }

        byte[] cipherText = new byte[algorithmCipher.getOutputSize(INPUT.length)];
        int offset = algorithmCipher.update(INPUT, 0, INPUT.length,
                cipherText, 0);
        algorithmCipher.doFinal(cipherText, offset);

        AlgorithmParameterSpec aps = null;
        if (!dataTuple.mode.equalsIgnoreCase("ECB")) {
            aps = new IvParameterSpec(algorithmCipher.getIV());
        }

        oidCipher.init(Cipher.DECRYPT_MODE, key, aps);
        if (!supportedKeyLength) {
            throw new RuntimeException(String.format(
                    "The key length %d is not supported, so the "
                            + "initialization of oidCipher should fail.%n",
                    dataTuple.keyLength));
        }

        byte[] recoveredText = new byte[oidCipher.getOutputSize(cipherText.length)];
        oidCipher.doFinal(cipherText, 0, cipherText.length, recoveredText);

        // Comparison
        if (!Arrays.equals(INPUT, recoveredText)) {
            throw new RuntimeException(
                    "Decrypted data is not the same as the original text");
        }
    }

    private static class DataTuple {

        private final String oid;
        private final String algorithm;
        private final int keyLength;
        private final String mode;

        private DataTuple(String oid, String algorithm, int keyLength,
                String mode) {
            this.oid = oid;
            this.algorithm = algorithm;
            this.keyLength = keyLength;
            this.mode = mode;
        }
    }
}
