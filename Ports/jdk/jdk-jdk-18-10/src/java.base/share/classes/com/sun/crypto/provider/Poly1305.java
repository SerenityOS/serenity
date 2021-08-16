/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Objects;

import sun.security.util.math.*;
import sun.security.util.math.intpoly.*;

/**
 * This class represents the Poly1305 function defined in RFC 7539.
 *
 * This function is used in the implementation of ChaCha20/Poly1305
 * AEAD mode.
 */
final class Poly1305 {

    private static final int KEY_LENGTH = 32;
    private static final int RS_LENGTH = KEY_LENGTH / 2;
    private static final int BLOCK_LENGTH = 16;
    private static final int TAG_LENGTH = 16;

    private static final IntegerFieldModuloP ipl1305 =
            new IntegerPolynomial1305();

    private byte[] keyBytes;
    private final byte[] block = new byte[BLOCK_LENGTH];
    private int blockOffset;

    private IntegerModuloP r;
    private IntegerModuloP s;
    private MutableIntegerModuloP a;
    private final MutableIntegerModuloP n = ipl1305.get1().mutable();

    Poly1305() { }

    /**
     * Initialize the Poly1305 object
     *
     * @param newKey the {@code Key} which will be used for the authentication.
     * @param params this parameter is unused.
     *
     * @throws InvalidKeyException if {@code newKey} is {@code null} or is
     *      not 32 bytes in length.
     */
    void engineInit(Key newKey, AlgorithmParameterSpec params)
            throws InvalidKeyException {
        Objects.requireNonNull(newKey, "Null key provided during init");
        keyBytes = newKey.getEncoded();
        if (keyBytes == null) {
            throw new InvalidKeyException("Key does not support encoding");
        } else if (keyBytes.length != KEY_LENGTH) {
            throw new InvalidKeyException("Incorrect length for key: " +
                    keyBytes.length);
        }

        engineReset();
        setRSVals();
    }

    /**
     * Returns the length of the MAC (authentication tag).
     *
     * @return the length of the auth tag, which is always 16 bytes.
     */
    int engineGetMacLength() {
        return TAG_LENGTH;
    }

    /**
     * Reset the Poly1305 object, discarding any current operation but
     *      maintaining the same key.
     */
    void engineReset() {
        // Clear the block and reset the offset
        Arrays.fill(block, (byte)0);
        blockOffset = 0;
        // Discard any previous accumulator and start at zero
        a = ipl1305.get0().mutable();
    }

    /**
     * Update the MAC with bytes from a {@code ByteBuffer}
     *
     * @param buf the {@code ByteBuffer} containing the data to be consumed.
     *      Upon return the buffer's position will be equal to its limit.
     */
    void engineUpdate(ByteBuffer buf) {
        int remaining = buf.remaining();
        while (remaining > 0) {
            int bytesToWrite = Integer.min(remaining,
                    BLOCK_LENGTH - blockOffset);

            if (bytesToWrite >= BLOCK_LENGTH) {
                // If bytes to write == BLOCK_LENGTH, then we have no
                // left-over data from previous updates and we can create
                // the IntegerModuloP directly from the input buffer.
                processBlock(buf, bytesToWrite);
            } else {
                // We have some left-over data from previous updates, so
                // copy that into the holding block until we get a full block.
                buf.get(block, blockOffset, bytesToWrite);
                blockOffset += bytesToWrite;

                if (blockOffset >= BLOCK_LENGTH) {
                    processBlock(block, 0, BLOCK_LENGTH);
                    blockOffset = 0;
                }
            }

            remaining -= bytesToWrite;
        }
    }

    /**
     * Update the MAC with bytes from an array.
     *
     * @param input the input bytes.
     * @param offset the starting index from which to update the MAC.
     * @param len the number of bytes to process.
     */
    void engineUpdate(byte[] input, int offset, int len) {
        Objects.checkFromIndexSize(offset, len, input.length);
        if (blockOffset > 0) {
            // We have some left-over data from previous updates
            int blockSpaceLeft = BLOCK_LENGTH - blockOffset;
            if (len < blockSpaceLeft) {
                System.arraycopy(input, offset, block, blockOffset, len);
                blockOffset += len;
                return; // block wasn't filled
            } else {
                System.arraycopy(input, offset, block, blockOffset,
                        blockSpaceLeft);
                offset += blockSpaceLeft;
                len -= blockSpaceLeft;
                processBlock(block, 0, BLOCK_LENGTH);
                blockOffset = 0;
            }
        }
        while (len >= BLOCK_LENGTH) {
            processBlock(input, offset, BLOCK_LENGTH);
            offset += BLOCK_LENGTH;
            len -= BLOCK_LENGTH;
        }
        if (len > 0) { // and len < BLOCK_LENGTH
            System.arraycopy(input, offset, block, 0, len);
            blockOffset = len;
        }
    }

    /**
     * Update the MAC with a single byte of input
     *
     * @param input the byte to update the MAC with.
     */
    void engineUpdate(byte input) {
        assert (blockOffset < BLOCK_LENGTH);
        // we can't hold fully filled unprocessed block
        block[blockOffset++] = input;

        if (blockOffset == BLOCK_LENGTH) {
            processBlock(block, 0, BLOCK_LENGTH);
            blockOffset = 0;
        }
    }


    /**
     * Finish the authentication operation and reset the MAC for a new
     * authentication operation.
     *
     * @return the authentication tag as a byte array.
     */
    byte[] engineDoFinal() {
        byte[] tag = new byte[BLOCK_LENGTH];

        // Finish up: process any remaining data < BLOCK_SIZE, then
        // create the tag from the resulting little-endian integer.
        if (blockOffset > 0) {
            processBlock(block, 0, blockOffset);
            blockOffset = 0;
        }

        // Add in the s-half of the key to the accumulator
        a.addModPowerTwo(s, tag);

        // Reset for the next auth
        engineReset();
        return tag;
    }

    /**
     * Process a single block of data.  This should only be called
     * when the block array is complete.  That may not necessarily
     * be a full 16 bytes if the last block has less than 16 bytes.
     */
    private void processBlock(ByteBuffer buf, int len) {
        n.setValue(buf, len, (byte)0x01);
        a.setSum(n);                    // a += (n | 0x01)
        a.setProduct(r);                // a = (a * r) % p
    }

    private void processBlock(byte[] block, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, block.length);
        n.setValue(block, offset, length, (byte)0x01);
        a.setSum(n);                    // a += (n | 0x01)
        a.setProduct(r);                // a = (a * r) % p
    }

    /**
     * Partition the authentication key into the R and S components, clamp
     * the R value, and instantiate IntegerModuloP objects to R and S's
     * numeric values.
     */
    private void setRSVals() {
        // Clamp the bytes in the "r" half of the key.
        keyBytes[3] &= 15;
        keyBytes[7] &= 15;
        keyBytes[11] &= 15;
        keyBytes[15] &= 15;
        keyBytes[4] &= 252;
        keyBytes[8] &= 252;
        keyBytes[12] &= 252;

        // Create IntegerModuloP elements from the r and s values
        r = ipl1305.getElement(keyBytes, 0, RS_LENGTH, (byte)0);
        s = ipl1305.getElement(keyBytes, RS_LENGTH, RS_LENGTH, (byte)0);
    }
}
