/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4517355
 * @summary Verify that AES cipher.doFinal method does NOT need more
 *      than necessary bytes in decrypt mode
 * @author Valerie Peng
 * @key randomness
 */
import java.io.PrintStream;
import java.security.*;
import java.security.spec.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class Test4517355 {

    private static final String ALGO = "AES";
    private static final int KEYSIZE = 16; // in bytes

    private static byte[] plainText = new byte[125];

    public void execute(String mode, String padding) throws Exception {
        String transformation = ALGO + "/" + mode + "/" + padding;

        Cipher ci = Cipher.getInstance(transformation, "SunJCE");
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        SecretKey key = kg.generateKey();

        // TEST FIX 4517355
        ci.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = ci.doFinal(plainText);

        if (mode.equalsIgnoreCase("GCM")) {
            AlgorithmParameters params = ci.getParameters();
            ci.init(Cipher.DECRYPT_MODE, key, params);
        } else {
            byte[] iv = ci.getIV();
            AlgorithmParameterSpec aps = new IvParameterSpec(iv);
            ci.init(Cipher.DECRYPT_MODE, key, aps);
        }
        byte[] recoveredText = new byte[plainText.length];
        try {
            int len = ci.doFinal(cipherText, 0, cipherText.length,
                                 recoveredText);
        } catch (ShortBufferException ex) {
            throw new Exception("output buffer is the right size!");
        }

        // BONUS TESTS
        // 1. make sure the recoveredText is the same as the plainText
        if (!Arrays.equals(plainText, recoveredText)) {
            throw new Exception("encryption/decryption does not work!");
        }
        // 2. make sure encryption does happen
        if (Arrays.equals(plainText, cipherText)) {
            throw new Exception("encryption does not work!");
        }
        // 3. make sure padding is working
        if (padding.equalsIgnoreCase("PKCS5Padding")) {
            if ((cipherText.length/16)*16 != cipherText.length) {
                throw new Exception("padding does not work!");
            }
        }
        System.out.println(transformation + ": Passed");
    }

    public static void main (String[] args) throws Exception {
        Test4517355 test = new Test4517355();
        Random rdm = new Random();
        rdm.nextBytes(test.plainText);

        test.execute("CBC", "PKCS5Padding");
        test.execute("GCM", "NoPadding");
    }
}
