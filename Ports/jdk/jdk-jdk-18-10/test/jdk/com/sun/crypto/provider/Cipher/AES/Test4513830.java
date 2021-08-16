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
 * @bug 4513830
 * @summary Verify the output size returned by AES cipher.getOutputSize
 *      method in DECRYPT mode does not add extra bytes for padding
 * @author Valerie Peng
 * @key randomness
 */
import java.io.PrintStream;
import java.security.*;
import java.security.spec.*;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class Test4513830 {

    private static final String ALGO = "AES";
    private static final String MODE = "ECB";
    private static final String PADDING = "PKCS5Padding";
    private static final int KEYSIZE = 16; // in bytes
    private static final int TEXTLENGTHS[] = {
        16, 17, 18, 19, 20, 21, 22, 23 };

    public boolean execute() throws Exception {
        Random rdm = new Random();
        byte[] plainText=new byte[125];
        rdm.nextBytes(plainText);

        Cipher ci = Cipher.getInstance(ALGO+"/"+MODE+"/"+PADDING, "SunJCE");

        // TEST FIX 4513830
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        SecretKey key = kg.generateKey();

        ci.init(Cipher.DECRYPT_MODE, key);
        int recoveredTextLength = ci.getOutputSize(16);

        if (recoveredTextLength != 16) {
            throw new Exception("output size should not increase when decrypting!");
        }

        // BONUS TESTS
        // 1. call getOutputSize with various lengths and see if
        // the returned size is correct.
        for (int i=0; i<TEXTLENGTHS.length; i++) {
            ci.init(Cipher.ENCRYPT_MODE, key);
            int cipherTextLength = ci.getOutputSize(TEXTLENGTHS[i]);
            if (cipherTextLength != 32) {
                throw new Exception("output size " + cipherTextLength
                                    + " for input size " + TEXTLENGTHS[i]
                                    + " when encrypting is wrong!");
            }
        }

        // passed all tests...hooray!
        return true;
    }

    public static void main (String[] args) throws Exception {
        Test4513830 test = new Test4513830();
        String testName = test.getClass().getName() + "[" + ALGO +
            "/" + MODE + "/" + PADDING + "]";
        if (test.execute()) {
            System.out.println(testName + ": Passed!");
        }
    }
}
