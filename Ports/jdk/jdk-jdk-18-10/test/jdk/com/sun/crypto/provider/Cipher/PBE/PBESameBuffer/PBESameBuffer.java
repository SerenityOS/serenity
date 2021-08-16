/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8041787
 * @library .
 * @build PBEWrapper PBEWrapperCreator PBKDF2Wrapper AESPBEWrapper PBECipherWrapper
 * @summary Verify that same encrypt/decrypt buffer can be used for PBE ciphers
 * @author Alexander Fomin
 * @author rhalade
 * @run main PBESameBuffer
 */
import java.io.PrintStream;
import java.security.*;
import java.util.Random;
import javax.crypto.Cipher;

public class PBESameBuffer {

    private static final String[] pbeAlgorithms = {
        "pbeWithMD5ANDdes", "PBEWithMD5AndDES/CBC/PKCS5Padding",
        "pbeWithMD5ANDtripledes", "PBEWithMD5AndTRIPLEDES/CBC/PKCS5Padding",
        "PBEwithSHA1AndDESede", "PBEwithSHA1AndDESede/CBC/PKCS5Padding",
        "PBEwithSHA1AndRC2_40", "PBEwithSHA1AndRC2_40/CBC/PKCS5Padding",
        "PBEWithSHA1AndRC2_128", "PBEWithSHA1AndRC2_128/CBC/PKCS5Padding",
        "PBEWithSHA1AndRC4_40", "PBEWithSHA1AndRC4_40/ECB/NoPadding",
        "PBEWithSHA1AndRC4_128", "PBEWithSHA1AndRC4_128/ECB/NoPadding",
        "PBEWithHmacSHA1AndAES_128",
        "PBEWithHmacSHA224AndAES_128",
        "PBEWithHmacSHA256AndAES_128",
        "PBEWithHmacSHA384AndAES_128",
        "PBEWithHmacSHA512AndAES_128",
        "PBEWithHmacSHA1AndAES_256",
        "PBEWithHmacSHA224AndAES_256",
        "PBEWithHmacSHA256AndAES_256",
        "PBEWithHmacSHA384AndAES_256",
        "PBEWithHmacSHA512AndAES_256",
        "PBKDF2WithHmacSHA1",
        "PBKDF2WithHmacSHA224",
        "PBKDF2WithHmacSHA256",
        "PBKDF2WithHmacSHA384",
        "PBKDF2WithHmacSHA512"
    };

    private static final String PBEPASS = "Hush, it's supposed to be a secret!";

    private static final int INPUT_LENGTH = 800;
    private static final int[] OFFSETS = {0, 1, 2, 3};
    private static final int NUM_PAD_BYTES = 8;
    private static final int PBKDF2_ADD_PAD_BYTES = 8;

    private static int OUTPUT_OFFSET;

    public static void main(String[] args) {
        if (!(new PBESameBuffer().test(args, System.out))) {
            throw new RuntimeException("Some PBE algorithm tests failed");
        }
    }

    public boolean test(String[] args, PrintStream out) {
        boolean result = true;

        Provider p = Security.getProvider("SunJCE");

        for (int loop : OFFSETS) {
            OUTPUT_OFFSET = loop;

            // generate input data
            byte[] inputText = new byte[INPUT_LENGTH + NUM_PAD_BYTES
                    + OUTPUT_OFFSET * 2 + PBKDF2_ADD_PAD_BYTES];
            new Random().nextBytes(inputText);

            for (String algorithm : pbeAlgorithms) {
                out.println("=> Testing algorithm " + algorithm + " and offset "
                        + OUTPUT_OFFSET + ":");

                try {
                    // Initialize Cipher and key for this algorithm
                    PBEWrapper pbeCi = PBEWrapperCreator.createWrapper(p,
                            algorithm,
                            PBEPASS,
                            out);

                    // Encrypt
                    if ((pbeCi != null) && (!pbeCi.execute(Cipher.ENCRYPT_MODE,
                            inputText,
                            OUTPUT_OFFSET * 2,
                            INPUT_LENGTH))) {
                        result = false;
                    }

                    // PBKDF2 required 16 byte padding
                    int padLength = getPadLength(algorithm);

                    // Decrypt
                    // Note: inputText is implicitly padded by the above encrypt
                    // operation so decrypt operation can safely proceed
                    if ((pbeCi != null) && (!pbeCi.execute(Cipher.DECRYPT_MODE,
                            inputText,
                            OUTPUT_OFFSET,
                            INPUT_LENGTH + padLength))) {
                        result = false;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(out);
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Get the padding length for the given algorithm
     *
     * @param theAlgName algorithm name
     * @return padding length for the given algorithm
     */
    private int getPadLength(String theAlgName) {
        if (theAlgName.toUpperCase().contains("PBKDF2")) {
            return NUM_PAD_BYTES + PBKDF2_ADD_PAD_BYTES;
        }

        if (theAlgName.toUpperCase().contains("AES")) {
            return NUM_PAD_BYTES + PBKDF2_ADD_PAD_BYTES;
        }

        return NUM_PAD_BYTES;
    }
}
