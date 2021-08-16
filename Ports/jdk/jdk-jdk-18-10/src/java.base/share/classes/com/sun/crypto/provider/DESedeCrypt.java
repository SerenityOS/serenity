/*
 * Copyright (c) 1997, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class implements the Triple DES algorithm (DES encryption, followed by
 * DES decryption, followed by DES encryption) on a byte array of size
 * <code>DES_BLOCK_SIZE</code>. Each DES operation has its own key.
 *
 * @author Gigi Ankeny
 * @author Jan Luehe
 *
 *
 * @see DESConstants
 * @see DESCipher
 */

final class DESedeCrypt extends DESCrypt implements DESConstants {

    /*
     * the expanded key used in encrypt/decrypt/encrypt phase
     */
    private byte[] key1 = null;
    private byte[] key2 = null;
    private byte[] key3 = null;
    private byte[] buf1, buf2;

    /*
     * constructor
     */
    DESedeCrypt() {
        buf1 = new byte[DES_BLOCK_SIZE];
        buf2 = new byte[DES_BLOCK_SIZE];
    }

    void init(boolean decrypting, String algorithm, byte[] keys)
            throws InvalidKeyException {
        if (!algorithm.equalsIgnoreCase("DESede")
                    && !algorithm.equalsIgnoreCase("TripleDES")) {
            throw new InvalidKeyException
                ("Wrong algorithm: DESede or TripleDES required");
        }
        if (keys.length != DES_BLOCK_SIZE * 3) {
            throw new InvalidKeyException("Wrong key size");
        }

        byte[] keybuf = new byte[DES_BLOCK_SIZE];

        // retrieve the first key
        key1 = new byte[128];
        System.arraycopy(keys, 0, keybuf, 0, DES_BLOCK_SIZE);
        expandKey(keybuf);
        System.arraycopy(expandedKey, 0, key1, 0, 128);

        // check if the third key is the same
        if (keyEquals(keybuf, 0, keys, DES_BLOCK_SIZE*2, DES_BLOCK_SIZE)) {
            key3 = key1;
        } else {
            key3 = new byte[128];
            System.arraycopy(keys, DES_BLOCK_SIZE*2, keybuf, 0,
                             DES_BLOCK_SIZE);
            expandKey(keybuf);
            System.arraycopy(expandedKey, 0, key3, 0, 128);
        }

        // retrieve the second key
        key2 = new byte[128];
        System.arraycopy(keys, DES_BLOCK_SIZE, keybuf, 0, DES_BLOCK_SIZE);
        expandKey(keybuf);
        System.arraycopy(expandedKey, 0, key2, 0, 128);

    }

    /**
     * Performs encryption operation.
     *
     * <p>The input plain text <code>plain</code>, starting at
     * <code>plainOffset</code> and ending at
     * <code>(plainOffset + blockSize - 1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * @param plain the buffer with the input data to be encrypted
     * @param plainOffset the offset in <code>plain</code>
     * @param cipher the buffer for the result
     * @param cipherOffset the offset in <code>cipher</code>
     */
    void encryptBlock(byte[] plain, int plainOffset,
                 byte[] cipher, int cipherOffset)
    {
        expandedKey = key1;
        decrypting = false;
        cipherBlock(plain, plainOffset, buf1, 0);

        expandedKey = key2;
        decrypting = true;
        cipherBlock(buf1, 0, buf2, 0);

        expandedKey = key3;
        decrypting = false;
        cipherBlock(buf2, 0, cipher, cipherOffset);
    }

    /**
     * Performs decryption operation.
     *
     * <p>The input cipher text <code>cipher</code>, starting at
     * <code>cipherOffset</code> and ending at
     * <code>(cipherOffset + blockSize - 1)</code>, is decrypted.
     * The result is stored in <code>plain</code>, starting at
     * <code>plainOffset</code>.
     *
     * @param cipher the buffer with the input data to be decrypted
     * @param cipherOffset the offset in <code>cipherOffset</code>
     * @param plain the buffer for the result
     * @param plainOffset the offset in <code>plain</code>
     */
    void decryptBlock(byte[] cipher, int cipherOffset,
                 byte[] plain, int plainOffset)
    {
        expandedKey = key3;
        decrypting = true;
        cipherBlock(cipher, cipherOffset, buf1, 0);

        expandedKey = key2;
        decrypting = false;
        cipherBlock(buf1, 0, buf2, 0);

        expandedKey = key1;
        decrypting = true;
        cipherBlock(buf2, 0, plain, plainOffset);
    }

    private boolean keyEquals(byte[] key1, int off1,
                              byte[] key2, int off2, int len) {

        for (int i=0; i<len; i++) {
            if (key1[i+off1] != key2[i+off2])
                return false;
        }
        return true;
    }
}
