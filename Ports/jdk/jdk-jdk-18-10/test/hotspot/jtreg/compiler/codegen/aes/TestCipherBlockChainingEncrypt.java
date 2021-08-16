/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8209951
 * @summary SIGBUS in com.sun.crypto.provider.CipherBlockChaining
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbatch
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestCipherBlockChainingEncrypt
 */

package compiler.codegen.aes;

import java.io.PrintStream;
import java.security.*;
import java.util.Random;
import java.lang.reflect.Method;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import compiler.whitebox.CompilerWhiteBoxTest;
import sun.hotspot.code.Compiler;
import jdk.test.lib.Utils;
import jtreg.SkippedException;

public class TestCipherBlockChainingEncrypt {
    private static String algorithm = "PBEWithHmacSHA1AndAES_256";
    private static final String PBEPASS = "Hush, it's supposed to be a secret!";

    private static final int INPUT_LENGTH = 800;
    private static final int[] OFFSETS = {0};
    private static final int NUM_PAD_BYTES = 8;
    private static final int PBKDF2_ADD_PAD_BYTES = 8;

    private static SecretKey key;
    private static Cipher ci;

    public static void main(String[] args) throws Exception {
        if (!Compiler.isIntrinsicAvailable(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "com.sun.crypto.provider.CipherBlockChaining", "implEncrypt", byte[].class, int.class, int.class, byte[].class, int.class)) {
            throw new SkippedException("Base64 intrinsic is not available");
        }
        for(int i=0; i<2_000; i++) {
          if (!(new TestCipherBlockChainingEncrypt().test(args))) {
            throw new RuntimeException("TestCipherBlockChainingEncrypt test failed");
       }
     }
   }

    public boolean test(String[] args) throws Exception {
        boolean result = true;

        Provider p = Security.getProvider("SunJCE");
        ci = Cipher.getInstance(algorithm, p);
        key = SecretKeyFactory.getInstance(algorithm, p).generateSecret(
                        new PBEKeySpec(PBEPASS.toCharArray()));

        // generate input data
        byte[] inputText = new byte[INPUT_LENGTH + NUM_PAD_BYTES
                + PBKDF2_ADD_PAD_BYTES];
        Utils.getRandomInstance().nextBytes(inputText);

        try {
            // Encrypt
            execute(Cipher.ENCRYPT_MODE,
                    inputText,
                    0,
                    INPUT_LENGTH);

            // PBKDF2 required 16 byte padding
            int padLength = NUM_PAD_BYTES + PBKDF2_ADD_PAD_BYTES;

            // Decrypt
            // Note: inputText is implicitly padded by the above encrypt
            // operation so decrypt operation can safely proceed
            execute(Cipher.DECRYPT_MODE,
                    inputText,
                    0,
                    INPUT_LENGTH + padLength);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            result = false;
        }
        return result;
    }

    private void execute(int edMode, byte[] inputText, int offset, int len) {
        try {
            // init Cipher
            if (Cipher.ENCRYPT_MODE == edMode) {
                ci.init(Cipher.ENCRYPT_MODE, this.key);
            } else {
                ci.init(Cipher.DECRYPT_MODE, this.key, ci.getParameters());
            }

            // First, generate the cipherText at an allocated buffer
            byte[] outputText = ci.doFinal(inputText, offset, len);

            // Second, generate cipherText again at the same buffer of plainText
            int myoff = offset / 2;
            int off = ci.update(inputText, offset, len, inputText, myoff);
            ci.doFinal(inputText, myoff + off);

            // Compare to see whether the two results are the same or not
            boolean e = equalsBlock(inputText, myoff, outputText, 0,
                    outputText.length);
        } catch (Exception ex) {
                System.out.println("Got unexpected exception for " + algorithm);
                ex.printStackTrace(System.out);
        }
    }

    private boolean equalsBlock(byte[] b1, int off1,
            byte[] b2, int off2, int len) {
        for (int i = off1, j = off2, k = 0; k < len; i++, j++, k++) {
            if (b1[i] != b2[j]) {
                return false;
            }
        }
        return true;
    }
}
