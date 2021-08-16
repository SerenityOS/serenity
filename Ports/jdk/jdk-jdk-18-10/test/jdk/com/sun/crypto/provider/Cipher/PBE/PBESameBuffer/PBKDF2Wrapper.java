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

import java.io.PrintStream;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Wrapper class to test a given SecretKeyFactory.PBKDF2 algorithm.
 *
 * @author Alexander Fomin
 */
public class PBKDF2Wrapper extends PBEWrapper {
    private static final String CIPHER_TANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int SALT_SIZE = 64;
    private static final int PKDF2_DEFAULT_KEY_LEN = 128;

    private static volatile byte[] iv;

    /**
     * PBKDF2Wrapper constructor. Instantiate Cipher using
     * "AES/CBC/PKCS5Padding" transformation. Generate a secret key using given
     * PKDF2 algorithms.
     *
     * @param p security Provider
     * @param algo PKDF2 algorithm
     * @param passwd password phrase
     * @param out print stream
     * @throws Exception all exceptions are thrown
     */
    public PBKDF2Wrapper(Provider p, String algo, String passwd,
            PrintStream out) throws Exception {
        super(algo,
                SecretKeyFactory.getInstance(algo, p).generateSecret(
                        new PBEKeySpec(passwd.toCharArray(),
                generateSalt(SALT_SIZE), ITERATION_COUNT, PKDF2_DEFAULT_KEY_LEN)),
                Cipher.getInstance(CIPHER_TANSFORMATION, p), out);
    }

    /**
     * Perform encryption/decryption operation (depending on the specified
     * edMode) on the same byte buffer. Compare result with the result at an
     * allocated buffer. If both results are equal - return true, otherwise
     * return false.
     *
     * @param edMode specified mode
     * @param inputText text to decrypt
     * @param offset offset in the text
     * @param len input length
     * @return ture - test passed; false - test failed
     */
    @Override
    public boolean execute(int edMode, byte[] inputText, int offset, int len) {
        int needBytesForResult = -1;
        String KEY_ALGORITHM = "AES";

        try {
            // init Cipher
            if (Cipher.ENCRYPT_MODE == edMode) {
                ci.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(),
                        KEY_ALGORITHM));
                iv = ci.getParameters().getParameterSpec(IvParameterSpec.class).
                        getIV();
            } else {
                ci.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(key.getEncoded(), KEY_ALGORITHM),
                        new IvParameterSpec(iv));
            }

            // First, generate the cipherText at an allocated buffer
            byte[] outputText = ci.doFinal(inputText, offset, len);

            // Second, generate cipherText again at the same buffer of plainText
            int myoff = offset / 2;
            int off = ci.update(inputText, offset, len, inputText, myoff);
            ci.doFinal(inputText, myoff + off);

            // Compare to see whether the two results are the same or not
            return equalsBlock(inputText, myoff, outputText, 0,
                    outputText.length);
        } catch (Exception ex) {
            out.println("Catch unexpected exception within " + algo
                    + " " + edMode + ": " + ex.getMessage()
                    + ". getOutputSize()" + "returned " + needBytesForResult);
            ex.printStackTrace(out);

            return false;
        }
    }
}
