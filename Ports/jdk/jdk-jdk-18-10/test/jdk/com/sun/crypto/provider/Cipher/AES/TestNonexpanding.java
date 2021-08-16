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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * @test
 * @bug 8043836
 * @summary Test AES encryption with no padding. Expect the original data length
 *          is the same as the encrypted data.
 * @key randomness
 */
public class TestNonexpanding {

    private static final String ALGORITHM = "AES";
    private static final String PROVIDER = "SunJCE";
    private static final String[] MODES = { "ECb", "CbC", "OFB", "OFB150",
        "cFB", "CFB7", "cFB8", "cFB16", "cFB24", "cFB32", "Cfb40", "cfB48",
        "cfB56", "cfB64", "cfB72", "cfB80", "cfB88", "cfB96", "cfb104",
        "cfB112", "cfB120", "GCM" };
    private static final String PADDING = "NoPadding";
    private static final int KEY_LENGTH = 128;

    public static void main(String argv[]) throws Exception {
        TestNonexpanding test = new TestNonexpanding();
        for (String mode : MODES) {
            test.runTest(ALGORITHM, mode, PADDING);
        }
    }

    public void runTest(String algo, String mo, String pad) throws Exception {
        Cipher ci = null;
        SecretKey key = null;
        try {
            // Initialization
            Random rdm = new Random();
            byte[] plainText = new byte[128];
            rdm.nextBytes(plainText);

            ci = Cipher.getInstance(algo + "/" + mo + "/" + pad, PROVIDER);

            KeyGenerator kg = KeyGenerator.getInstance(algo, PROVIDER);
            kg.init(KEY_LENGTH);
            key = kg.generateKey();

            // encrypt
            ci.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherText = new byte[ci.getOutputSize(plainText.length)];
            int offset = ci.update(plainText, 0, plainText.length, cipherText,
                    0);
            ci.doFinal(cipherText, offset);

            // Comparison
            if (!(plainText.length == cipherText.length)) {
                // The result of encryption in GCM is a combination of an
                // authentication tag and cipher text.
                if (mo.equalsIgnoreCase("GCM")) {
                    GCMParameterSpec spec = ci.getParameters().getParameterSpec(GCMParameterSpec.class);
                    int cipherTextLength = cipherText.length - spec.getTLen()
                            / 8;
                    if (plainText.length == cipherTextLength) {
                        return;
                    }
                }
                System.out.println("Original length: " + plainText.length);
                System.out.println("Cipher text length: " + cipherText.length);
                throw new RuntimeException("Test failed!");
            }
        } catch (NoSuchAlgorithmException e) {
            //CFB7 and OFB150 are for negative testing
            if (!mo.equalsIgnoreCase("CFB7") && !mo.equalsIgnoreCase("OFB150")) {
                System.out.println("Unexpected NoSuchAlgorithmException with mode: "
                        + mo);
                throw new RuntimeException("Test failed!");
            }
        } catch ( NoSuchProviderException | NoSuchPaddingException
                | InvalidKeyException | InvalidParameterSpecException
                | ShortBufferException | IllegalBlockSizeException
                | BadPaddingException e) {
            System.out.println("Test failed!");
            throw e;
        }
    }
}
