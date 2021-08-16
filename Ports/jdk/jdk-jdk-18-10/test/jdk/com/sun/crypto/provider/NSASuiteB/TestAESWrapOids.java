/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static javax.crypto.Cipher.getMaxAllowedKeyLength;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/*
 * @test
 * @bug 8075286 8248268
 * @summary Test the AES-Key-Wrap and AES-Key-Wrap-Pad algorithm OIDs in JDK.
 *          OID and Algorithm transformation string should match.
 *          Both could be able to be used to generate the algorithm instance.
 * @run main TestAESWrapOids
 */
public class TestAESWrapOids {

    private static final String PROVIDER_NAME = "SunJCE";

    private static final List<DataTuple> DATA = Arrays.asList(
            new DataTuple("2.16.840.1.101.3.4.1.5", "AESWrap_128"),
            new DataTuple("2.16.840.1.101.3.4.1.25", "AESWrap_192"),
            new DataTuple("2.16.840.1.101.3.4.1.45", "AESWrap_256"),
            new DataTuple("2.16.840.1.101.3.4.1.5", "AES_128/KW/NoPadding"),
            new DataTuple("2.16.840.1.101.3.4.1.25", "AES_192/KW/NoPadding"),
            new DataTuple("2.16.840.1.101.3.4.1.45", "AES_256/KW/NoPadding"),
            new DataTuple("2.16.840.1.101.3.4.1.8", "AES_128/KWP/NoPadding"),
            new DataTuple("2.16.840.1.101.3.4.1.28", "AES_192/KWP/NoPadding"),
            new DataTuple("2.16.840.1.101.3.4.1.48", "AES_256/KWP/NoPadding"));

    public static void main(String[] args) throws Exception {
        for (DataTuple dataTuple : DATA) {
            int maxAllowedKeyLength = getMaxAllowedKeyLength(
                    dataTuple.algorithm);
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
                            "Catch expected InvalidKeyException "
                                    + "due to the key length %d is greater "
                                    + "than max supported key length %d%n",
                            dataTuple.keyLength, maxAllowedKeyLength);
                }
            }
        }
    }

    private static void runTest(DataTuple dataTuple, boolean supportedKeyLength)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException {
        Cipher algorithmCipher = Cipher.getInstance(
                dataTuple.algorithm, PROVIDER_NAME);
        Cipher oidCipher = Cipher.getInstance(dataTuple.oid, PROVIDER_NAME);

        if (algorithmCipher == null) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance failed.%n",
                    dataTuple.algorithm));
        }

        if (oidCipher == null) {
            throw new RuntimeException(
                    String.format("Test failed: OID %s getInstance failed.%n",
                            dataTuple.oid));
        }

        if (!algorithmCipher.getAlgorithm().equals(
                dataTuple.algorithm)) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance "
                            + "doesn't generate expected algorithm.%n",
                    dataTuple.oid));
        }

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(dataTuple.keyLength);
        SecretKey key = kg.generateKey();

        // Wrap the key
        algorithmCipher.init(Cipher.WRAP_MODE, key);
        if (!supportedKeyLength) {
            throw new RuntimeException(String.format(
                    "The key length %d is not supported, so the initialization"
                            + " of algorithmCipher should fail.%n",
                    dataTuple.keyLength));
        }

        // Unwrap the key
        oidCipher.init(Cipher.UNWRAP_MODE, key);
        if (!supportedKeyLength) {
            throw new RuntimeException(String.format(
                    "The key length %d is not supported, so the initialization"
                            + " of oidCipher should fail.%n",
                    dataTuple.keyLength));
        }

        byte[] keyWrapper = algorithmCipher.wrap(key);
        Key unwrappedKey = oidCipher.unwrap(keyWrapper, "AES",
                Cipher.SECRET_KEY);

        // Comparison
        if (!Arrays.equals(key.getEncoded(), unwrappedKey.getEncoded())) {
            throw new RuntimeException("Key comparison failed");
        }
    }

    private static class DataTuple {

        private final String oid;
        private final String algorithm;
        private final int keyLength;

        private DataTuple(String oid, String algorithm) {
            this.oid = oid;
            this.algorithm = algorithm;
            this.keyLength = switch (oid) {
                case "2.16.840.1.101.3.4.1.5", "2.16.840.1.101.3.4.1.8"->128;
                case "2.16.840.1.101.3.4.1.25", "2.16.840.1.101.3.4.1.28"->192;
                case "2.16.840.1.101.3.4.1.45", "2.16.840.1.101.3.4.1.48"->256;
                default->throw new RuntimeException("Unrecognized oid: " + oid);
            };
        }
    }
}
