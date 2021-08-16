/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Wrapper class to test a given AES-based PBE algorithm.
 *
 * @author Alexander Fomin
 */
public class AESPBEWrapper extends PBEWrapper {

    private AlgorithmParameters pbeParams;

    /**
     * Constructor. Instantiate Cipher using the given AES-based PBE algorithms.
     *
     * @param p security Provider
     * @param algo PKDF2 algorithm
     * @param passwd password phrase
     * @param out print stream
     * @throws Exception all exceptions are thrown
     */
    public AESPBEWrapper(Provider p, String algo, String passwd,
            PrintStream out) throws Exception {
        super(algo,
                SecretKeyFactory.getInstance(algo, p).generateSecret(
                        new PBEKeySpec(passwd.toCharArray())),
                Cipher.getInstance(algo, p), out);
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
        boolean isUnlimited;
        try {
            isUnlimited =
                (Cipher.getMaxAllowedKeyLength(this.algo) == Integer.MAX_VALUE);
        } catch (NoSuchAlgorithmException nsae) {
            out.println("Got unexpected exception for " + this.algo);
            nsae.printStackTrace(out);
            return false;
        }
        try {
            // init Cipher
            if (Cipher.ENCRYPT_MODE == edMode) {
                ci.init(Cipher.ENCRYPT_MODE, this.key);
                pbeParams = ci.getParameters();
            } else {
                ci.init(Cipher.DECRYPT_MODE, this.key, pbeParams);
            }

            if (this.algo.endsWith("AES_256") && !isUnlimited) {
                out.print("Expected exception not thrown for " + this.algo);
                return false;
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
            if ((ex instanceof InvalidKeyException)
                    && this.algo.endsWith("AES_256") && !isUnlimited) {
                out.println("Expected InvalidKeyException thrown");
                return true;
            } else {
                out.println("Got unexpected exception for " + algo);
                ex.printStackTrace(out);
                return false;
            }
        }
    }
}
