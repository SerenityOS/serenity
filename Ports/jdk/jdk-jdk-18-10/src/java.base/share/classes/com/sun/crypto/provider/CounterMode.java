/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.IntrinsicCandidate;
import sun.security.util.ArrayUtil;

/**
 * This class represents ciphers in counter (CTR) mode.
 *
 * <p>This mode is implemented independently of a particular cipher.
 * Ciphers to which this mode should apply (e.g., DES) must be
 * <i>plugged-in</i> using the constructor.
 *
 * <p>NOTE: This class does not deal with buffering or padding.
 *
 * @author Andreas Sterbenz
 * @since 1.4.2
 */
class CounterMode extends FeedbackCipher {

    // current counter value
    final byte[] counter;

    // encrypted bytes of the previous counter value
    private final byte[] encryptedCounter;

    // number of bytes in encryptedCounter already used up
    private int used;

    // variables for save/restore calls
    private byte[] counterSave = null;
    private byte[] encryptedCounterSave = null;
    private int usedSave = 0;

    CounterMode(SymmetricCipher embeddedCipher) {
        super(embeddedCipher);
        counter = new byte[blockSize];
        encryptedCounter = new byte[blockSize];
    }

    /**
     * Gets the name of the feedback mechanism
     *
     * @return the name of the feedback mechanism
     */
    String getFeedback() {
        return "CTR";
    }

    /**
     * Resets the iv to its original value.
     * This is used when doFinal is called in the Cipher class, so that the
     * cipher can be reused (with its original iv).
     */
    void reset() {
        System.arraycopy(iv, 0, counter, 0, blockSize);
        used = blockSize;
    }

    /**
     * Save the current content of this cipher.
     */
    void save() {
        if (counterSave == null) {
            counterSave = new byte[blockSize];
            encryptedCounterSave = new byte[blockSize];
        }
        System.arraycopy(counter, 0, counterSave, 0, blockSize);
        System.arraycopy(encryptedCounter, 0, encryptedCounterSave, 0,
            blockSize);
        usedSave = used;
    }

    /**
     * Restores the content of this cipher to the previous saved one.
     */
    void restore() {
        System.arraycopy(counterSave, 0, counter, 0, blockSize);
        System.arraycopy(encryptedCounterSave, 0, encryptedCounter, 0,
            blockSize);
        used = usedSave;
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
        // always encrypt mode for embedded cipher
        embeddedCipher.init(false, algorithm, key);
    }

    /**
     * Performs encryption operation.
     *
     * <p>The input plain text <code>plain</code>, starting at
     * <code>plainOffset</code> and ending at
     * <code>(plainOffset + len - 1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * @param in the buffer with the input data to be encrypted
     * @param inOff the offset in <code>plain</code>
     * @param len the length of the input data
     * @param out the buffer for the result
     * @param outOff the offset in <code>cipher</code>
     * @return the length of the encrypted data
     */
    int encrypt(byte[] in, int inOff, int len, byte[] out, int outOff) {
        return crypt(in, inOff, len, out, outOff);
    }

    // CTR encrypt and decrypt are identical
    int decrypt(byte[] in, int inOff, int len, byte[] out, int outOff) {
        return crypt(in, inOff, len, out, outOff);
    }

    /**
     * Increment the counter value.
     */
    private static void increment(byte[] b) {
        int n = b.length - 1;
        while ((n >= 0) && (++b[n] == 0)) {
            n--;
        }
    }

    /**
     * Do the actual encryption/decryption operation.
     * Essentially we XOR the input plaintext/ciphertext stream with a
     * keystream generated by encrypting the counter values. Counter values
     * are encrypted on demand.
     */
    private int crypt(byte[] in, int inOff, int len, byte[] out, int outOff) {
        if (len == 0) {
            return 0;
        }

        ArrayUtil.nullAndBoundsCheck(in, inOff, len);
        ArrayUtil.nullAndBoundsCheck(out, outOff, len);
        return implCrypt(in, inOff, len, out, outOff);
    }

    // Implementation of crpyt() method. Possibly replaced with a compiler intrinsic.
    @IntrinsicCandidate
    private int implCrypt(byte[] in, int inOff, int len, byte[] out, int outOff) {
        int result = len;
        while (len-- > 0) {
            if (used >= blockSize) {
                embeddedCipher.encryptBlock(counter, 0, encryptedCounter, 0);
                increment(counter);
                used = 0;
            }
            out[outOff++] = (byte)(in[inOff++] ^ encryptedCounter[used++]);
        }
        return result;
    }

}
