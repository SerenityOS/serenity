/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.crypto.provider;

import java.security.InvalidKeyException;
import java.security.ProviderException;
import sun.security.util.ArrayUtil;


/**
 * This class represents ciphers in Plaintext Cipher Block Chaining (PCBC)
 * mode.
 *
 * <p>This mode is implemented independently of a particular cipher.
 * Ciphers to which this mode should apply (e.g., DES) must be
 * <i>plugged-in</i> using the constructor.
 *
 * <p>NOTE: This class does not deal with buffering or padding.
 *
 * @author Gigi Ankeny
 */

final class PCBC extends FeedbackCipher {

    /*
     * output buffer
     */
    private final byte[] k;

    // variables for save/restore calls
    private byte[] kSave = null;

    PCBC(SymmetricCipher embeddedCipher) {
        super(embeddedCipher);
        k = new byte[blockSize];
    }

    /**
     * Gets the name of this feedback mode.
     *
     * @return the string <code>PCBC</code>
     */
    String getFeedback() {
        return "PCBC";
    }

    /**
     * Initializes the cipher in the specified mode with the given key
     * and iv.
     *
     * @param decrypting flag indicating encryption or decryption
     * @param algorithm the algorithm name
     * @param key the key
     * @param iv the iv
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     */
    void init(boolean decrypting, String algorithm, byte[] key, byte[] iv)
            throws InvalidKeyException {
        if ((key == null) || (iv == null) || (iv.length != blockSize)) {
            throw new InvalidKeyException("Internal error");
        }
        this.iv = iv;
        reset();
        embeddedCipher.init(decrypting, algorithm, key);
    }

    /**
     * Resets the iv to its original value.
     * This is used when doFinal is called in the Cipher class, so that the
     * cipher can be reused (with its original iv).
     */
    void reset() {
        System.arraycopy(iv, 0, k, 0, blockSize);
    }

    /**
     * Save the current content of this cipher.
     */
    void save() {
        if (kSave == null) {
            kSave = new byte[blockSize];
        }
        System.arraycopy(k, 0, kSave, 0, blockSize);

    }

    /**
     * Restores the content of this cipher to the previous saved one.
     */
    void restore() {
        System.arraycopy(kSave, 0, k, 0, blockSize);
    }

    /**
     * Performs encryption operation.
     *
     * <p>The input plain text <code>plain</code>, starting at
     * <code>plainOffset</code> and ending at
     * <code>(plainOffset + plainLen - 1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * @param plain the buffer with the input data to be encrypted
     * @param plainOffset the offset in <code>plain</code>
     * @param plainLen the length of the input data
     * @param cipher the buffer for the result
     * @param cipherOffset the offset in <code>cipher</code>
     * @exception ProviderException if <code>plainLen</code> is not
     * a multiple of the block size
     * @return the length of the encrypted data
     */
    int encrypt(byte[] plain, int plainOffset, int plainLen,
                byte[] cipher, int cipherOffset)
    {
        ArrayUtil.blockSizeCheck(plainLen, blockSize);
        ArrayUtil.nullAndBoundsCheck(plain, plainOffset, plainLen);
        ArrayUtil.nullAndBoundsCheck(cipher, cipherOffset, plainLen);

        int i;
        int endIndex = plainOffset + plainLen;

        for (; plainOffset < endIndex;
             plainOffset += blockSize, cipherOffset += blockSize) {
            for (i = 0; i < blockSize; i++) {
                k[i] ^= plain[i + plainOffset];
            }
            embeddedCipher.encryptBlock(k, 0, cipher, cipherOffset);
            for (i = 0; i < blockSize; i++) {
                k[i] = (byte)(plain[i + plainOffset] ^ cipher[i + cipherOffset]);
            }
        }
        return plainLen;
    }

    /**
     * Performs decryption operation.
     *
     * <p>The input cipher text <code>cipher</code>, starting at
     * <code>cipherOffset</code> and ending at
     * <code>(cipherOffset + cipherLen - 1)</code>, is decrypted.
     * The result is stored in <code>plain</code>, starting at
     * <code>plainOffset</code>.
     *
     * @param cipher the buffer with the input data to be decrypted
     * @param cipherOffset the offset in <code>cipherOffset</code>
     * @param cipherLen the length of the input data
     * @param plain the buffer for the result
     * @param plainOffset the offset in <code>plain</code>
     * @exception ProviderException if <code>cipherLen</code> is not
     * a multiple of the block size
     * @return the length of the decrypted data
     */
    int decrypt(byte[] cipher, int cipherOffset, int cipherLen,
                byte[] plain, int plainOffset)
    {
        ArrayUtil.blockSizeCheck(cipherLen, blockSize);
        ArrayUtil.nullAndBoundsCheck(cipher, cipherOffset, cipherLen);
        ArrayUtil.nullAndBoundsCheck(plain, plainOffset, cipherLen);

        int i;
        int endIndex = cipherOffset + cipherLen;

        for (; cipherOffset < endIndex;
             plainOffset += blockSize, cipherOffset += blockSize) {
            embeddedCipher.decryptBlock(cipher, cipherOffset,
                                   plain, plainOffset);
            for (i = 0; i < blockSize; i++) {
                plain[i + plainOffset] ^= k[i];
            }
            for (i = 0; i < blockSize; i++) {
                k[i] = (byte)(plain[i + plainOffset] ^ cipher[i + cipherOffset]);
            }
        }
        return cipherLen;
    }
}
