/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048604
 * @library ../ /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main CipherNCFuncTest
 * @summary This test verifies the assertion "There should be no transformation
 *          on the plaintext/ciphertext in encryption/decryption mechanism" for
 *          feature "NullCipher".
 */

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NullCipher;
import javax.crypto.ShortBufferException;
import jdk.test.lib.RandomFactory;

public class CipherNCFuncTest {
    public static void main(String[] args) throws ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        byte[] plainText = new byte[801];
        // Initialization
        RandomFactory.getRandom().nextBytes(plainText);
        Cipher ci = new NullCipher();
        // Encryption
        byte[] cipherText = new byte[ci.getOutputSize(plainText.length)];
        int offset = ci.update(plainText, 0, plainText.length, cipherText, 0);
        ci.doFinal(cipherText, offset);
        // Decryption
        byte[] recoveredText = new byte[ci.getOutputSize(cipherText.length)];
        int len = ci.doFinal(cipherText, 0, cipherText.length, recoveredText);
        // Comparison
        if (len != plainText.length ||
                !TestUtilities.equalsBlock(plainText, cipherText, len) ||
                !TestUtilities.equalsBlock(plainText, recoveredText, len)) {
            throw new RuntimeException(
                "Test failed because plainText not equal to cipherText and revoveredText");
        }
    }
}
