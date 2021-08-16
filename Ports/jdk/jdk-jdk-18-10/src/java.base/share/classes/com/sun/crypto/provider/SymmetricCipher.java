/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This abstract class represents the core of all block ciphers. It allows to
 * initialize the cipher and encrypt/decrypt single blocks. Larger quantities
 * are handled by modes, which are subclasses of FeedbackCipher.
 *
 * @author Gigi Ankeny
 * @author Jan Luehe
 *
 *
 * @see AESCrypt
 * @see DESCrypt
 * @see DESedeCrypt
 * @see BlowfishCrypt
 * @see FeedbackCipher
 */
abstract class SymmetricCipher {

    SymmetricCipher() {
        // empty
    }

    /**
     * Retrieves this cipher's block size.
     *
     * @return the block size of this cipher
     */
    abstract int getBlockSize();

    /**
     * Initializes the cipher in the specified mode with the given key.
     *
     * @param decrypting flag indicating encryption or decryption
     * @param algorithm the algorithm name
     * @param key the key
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     */
    abstract void init(boolean decrypting, String algorithm, byte[] key)
        throws InvalidKeyException;

    /**
     * Encrypt one cipher block.
     *
     * <p>The input <code>plain</code>, starting at <code>plainOffset</code>
     * and ending at <code>(plainOffset+blockSize-1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * @param plain the input buffer with the data to be encrypted
     * @param plainOffset the offset in <code>plain</code>
     * @param cipher the buffer for the encryption result
     * @param cipherOffset the offset in <code>cipher</code>
     */
    abstract void encryptBlock(byte[] plain, int plainOffset,
                          byte[] cipher, int cipherOffset);

    /**
     * Decrypt one cipher block.
     *
     * <p>The input <code>cipher</code>, starting at <code>cipherOffset</code>
     * and ending at <code>(cipherOffset+blockSize-1)</code>, is decrypted.
     * The result is stored in <code>plain</code>, starting at
     * <code>plainOffset</code>.
     *
     * @param cipher the input buffer with the data to be decrypted
     * @param cipherOffset the offset in <code>cipher</code>
     * @param plain the buffer for the decryption result
     * @param plainOffset the offset in <code>plain</code>
     */
    abstract void decryptBlock(byte[] cipher, int cipherOffset,
                          byte[] plain, int plainOffset);
}
