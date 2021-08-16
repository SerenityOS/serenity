/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * This class represents ciphers in cipher text stealing (CTS) mode.
 * <br>CTS provides a way to allow block ciphers to operate on partial
 * blocks without padding, and all bits of the message go through
 * the encryption algorithm, rather than simply being XOR'd.
 * <br>More details can be found in RFC 2040 section 8 "Description
 * of RC5-CTS".
 *
 * <p>This mode is implemented independently of a particular cipher.
 * Ciphers to which this mode should apply (e.g., DES) must be
 * <i>plugged-in</i> using the constructor.
 *
 * <p>NOTE#1: CTS requires the input data to be at least one block
 * long. Thus, callers of this class has to buffer the input data
 * to make sure the input data passed to encryptFinal()/decryptFinal()
 * is not shorter than a block.
 * <p>NOTE#2: This class does not deal with buffering or padding
 * just like all other cipher mode implementations.
 *
 * @author Valerie Peng
 */

final class CipherTextStealing extends CipherBlockChaining {

    CipherTextStealing(SymmetricCipher embeddedCipher) {
        super(embeddedCipher);
    }

    /**
     * Gets the name of this feedback mode.
     *
     * @return the string <code>CBC</code>
     */
    String getFeedback() {
        return "CTS";
    }

    /**
     * Performs the last encryption operation.
     *
     * <p>The input plain text <code>plain</code>, starting at
     * <code>plainOffset</code> and ending at
     * <code>(plainOffset + len - 1)</code>, is encrypted.
     * The result is stored in <code>cipher</code>, starting at
     * <code>cipherOffset</code>.
     *
     * <p>It is the application's responsibility to make sure that
     * <code>plainLen</code> is a multiple of the embedded cipher's block size,
     * as any excess bytes are ignored.
     *
     * @param plain the buffer with the input data to be encrypted
     * @param plainOffset the offset in <code>plain</code>
     * @param plainLen the length of the input data
     * @param cipher the buffer for the result
     * @param cipherOffset the offset in <code>cipher</code>
     * @return the number of bytes placed into <code>cipher</code>
     */
    int encryptFinal(byte[] plain, int plainOffset, int plainLen,
                     byte[] cipher, int cipherOffset)
        throws IllegalBlockSizeException {

        if (plainLen < blockSize) {
            throw new IllegalBlockSizeException("input is too short!");
        } else if (plainLen == blockSize) {
            encrypt(plain, plainOffset, plainLen, cipher, cipherOffset);
        } else {
            // number of bytes in the last block
            int nLeft = plainLen % blockSize;
            if (nLeft == 0) {
                encrypt(plain, plainOffset, plainLen, cipher, cipherOffset);
                // swap the last two blocks after encryption
                int lastBlkIndex = cipherOffset + plainLen - blockSize;
                int nextToLastBlkIndex = lastBlkIndex - blockSize;
                byte[] tmp = new byte[blockSize];
                System.arraycopy(cipher, lastBlkIndex, tmp, 0, blockSize);
                System.arraycopy(cipher, nextToLastBlkIndex,
                                 cipher, lastBlkIndex, blockSize);
                System.arraycopy(tmp, 0, cipher, nextToLastBlkIndex,
                                 blockSize);
            } else {
                int newPlainLen = plainLen - (blockSize + nLeft);
                if (newPlainLen > 0) {
                    encrypt(plain, plainOffset, newPlainLen, cipher,
                            cipherOffset);
                    plainOffset += newPlainLen;
                    cipherOffset += newPlainLen;
                }

                // Do final CTS step for last two blocks (the second of which
                // may or may not be incomplete).
                byte[] tmp = new byte[blockSize];
                // now encrypt the next-to-last block
                for (int i = 0; i < blockSize; i++) {
                    tmp[i] = (byte) (plain[plainOffset+i] ^ r[i]);
                }
                byte[] tmp2 = new byte[blockSize];
                embeddedCipher.encryptBlock(tmp, 0, tmp2, 0);
                System.arraycopy(tmp2, 0, cipher,
                                 cipherOffset+blockSize, nLeft);
                // encrypt the last block
                for (int i=0; i<nLeft; i++) {
                    tmp2[i] = (byte)
                        (plain[plainOffset+blockSize+i] ^ tmp2[i]);
                }
                embeddedCipher.encryptBlock(tmp2, 0, cipher, cipherOffset);
            }
        }
        return plainLen;
    }

    /**
     * Performs decryption operation.
     *
     * <p>The input cipher text <code>cipher</code>, starting at
     * <code>cipherOffset</code> and ending at
     * <code>(cipherOffset + len - 1)</code>, is decrypted.
     * The result is stored in <code>plain</code>, starting at
     * <code>plainOffset</code>.
     *
     * <p>It is the application's responsibility to make sure that
     * <code>cipherLen</code> is a multiple of the embedded cipher's block
     * size, as any excess bytes are ignored.
     *
     * <p>It is also the application's responsibility to make sure that
     * <code>init</code> has been called before this method is called.
     * (This check is omitted here, to avoid double checking.)
     *
     * @param cipher the buffer with the input data to be decrypted
     * @param cipherOffset the offset in <code>cipherOffset</code>
     * @param cipherLen the length of the input data
     * @param plain the buffer for the result
     * @param plainOffset the offset in <code>plain</code>
     * @return the number of bytes placed into <code>plain</code>
     */
    int decryptFinal(byte[] cipher, int cipherOffset, int cipherLen,
                     byte[] plain, int plainOffset)
        throws IllegalBlockSizeException {
        if (cipherLen < blockSize) {
            throw new IllegalBlockSizeException("input is too short!");
        } else if (cipherLen == blockSize) {
            decrypt(cipher, cipherOffset, cipherLen, plain, plainOffset);
        } else {
            // number of bytes in the last block
            int nLeft = cipherLen % blockSize;
            if (nLeft == 0) {
                // swap the last two blocks before decryption
                int lastBlkIndex = cipherOffset + cipherLen - blockSize;
                int nextToLastBlkIndex =
                    cipherOffset + cipherLen - 2*blockSize;
                byte[] tmp = new byte[2*blockSize];
                System.arraycopy(cipher, lastBlkIndex, tmp, 0, blockSize);
                System.arraycopy(cipher, nextToLastBlkIndex,
                                 tmp, blockSize, blockSize);
                int cipherLen2 = cipherLen-2*blockSize;
                decrypt(cipher, cipherOffset, cipherLen2, plain, plainOffset);
                decrypt(tmp, 0, 2*blockSize, plain, plainOffset+cipherLen2);
            } else {
                int newCipherLen = cipherLen-(blockSize+nLeft);
                if (newCipherLen > 0) {
                    decrypt(cipher, cipherOffset, newCipherLen, plain,
                            plainOffset);
                    cipherOffset += newCipherLen;
                    plainOffset += newCipherLen;
                }
                // Do final CTS step for last two blocks (the second of which
                // may or may not be incomplete).

                // now decrypt the next-to-last block
                byte[] tmp = new byte[blockSize];
                embeddedCipher.decryptBlock(cipher, cipherOffset, tmp, 0);
                for (int i = 0; i < nLeft; i++) {
                    plain[plainOffset+blockSize+i] =
                        (byte) (cipher[cipherOffset+blockSize+i] ^ tmp[i]);
                }

                // decrypt the last block
                System.arraycopy(cipher, cipherOffset+blockSize, tmp, 0,
                                 nLeft);
                embeddedCipher.decryptBlock(tmp, 0, plain, plainOffset);
                //System.arraycopy(r, 0, tmp, 0, r.length);
                for (int i=0; i<blockSize; i++) {
                    plain[plainOffset+i] = (byte)
                        (plain[plainOffset+i]^r[i]);
                }
            }
        }
        return cipherLen;
    }
}
