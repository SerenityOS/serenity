/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright IBM Corp. 2013
 */

package com.sun.crypto.provider;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * This class represents the GCTR function defined in NIST 800-38D
 * under section 6.5.  With a given cipher object and initial counter
 * block, a counter mode operation is performed.  Blocksize is limited
 * to 16 bytes.
 *
 * If any invariant is broken, failures can occur because the
 * AESCrypt.encryptBlock method can be intrinsified on the HotSpot VM
 * (see JDK-8067648 for details).
 *
 * The counter mode operations can be intrinsified and parallelized
 * by using CounterMode.implCrypt() if HotSpot VM supports it on the
 * architecture.
 *
 * <p>This function is used in the implementation of GCM mode.
 *
 * @since 1.8
 */
final class GCTR extends CounterMode implements GCM {

    // Maximum buffer size rotating ByteBuffer->byte[] intrinsic copy
    private static final int MAX_LEN = 1024;
    private byte[] block;

    GCTR(SymmetricCipher cipher, byte[] initialCounterBlk) {
        super(cipher);
        if (initialCounterBlk.length != blockSize) {
            throw new RuntimeException("length of initial counter block (" +
                initialCounterBlk.length + ") not equal to blockSize (" +
                blockSize + ")");
        }

        iv = initialCounterBlk;
        reset();
    }

    @Override
    String getFeedback() {
        return "GCTR";
    }

    // return the number of blocks until the lower 32 bits roll over
    private long blocksUntilRollover() {
        ByteBuffer buf = ByteBuffer.wrap(counter, counter.length - 4, 4);
        buf.order(ByteOrder.BIG_ENDIAN);
        long ctr32 = 0xFFFFFFFFL & buf.getInt();
        long blocksLeft = (1L << 32) - ctr32;
        return blocksLeft;
    }

    private void checkBlock() {
        if (block == null) {
            block = new byte[blockSize];
        } else {
            Arrays.fill(block, (byte)0);
        }
    }

    /**
     * Using the given inLen, this operates only on blockSize data, leaving
     * the remainder in 'in'.
     * The return value will be (inLen - (inLen % blockSize))
     */
    public int update(byte[] in, int inOfs, int inLen, byte[] out, int outOfs) {
        if (inLen == 0) {
            return 0;
        }

        if (inLen - inOfs > in.length) {
            throw new RuntimeException("input length out of bound");
        }
        if (inLen < 0) {
            throw new RuntimeException("input length unsupported");
        }
        if (out.length - outOfs < (inLen - (inLen % blockSize))) {
            throw new RuntimeException("output buffer too small");
        }

        inLen -= inLen % blockSize;
        long blocksLeft = blocksUntilRollover();
        int numOfCompleteBlocks = inLen / blockSize;
        if (numOfCompleteBlocks >= blocksLeft) {
            // Counter Mode encryption cannot be used because counter will
            // roll over incorrectly. Use GCM-specific code instead.
            checkBlock();
            for (int i = 0; i < numOfCompleteBlocks; i++) {
                embeddedCipher.encryptBlock(counter, 0, block, 0);
                for (int n = 0; n < blockSize; n++) {
                    int index = (i * blockSize + n);
                    out[outOfs + index] =
                        (byte) ((in[inOfs + index] ^ block[n]));
                }
                GaloisCounterMode.increment32(counter);
            }
            return inLen;
        } else {
            return encrypt(in, inOfs, inLen, out, outOfs);
        }
    }

    /**
     * Operate on only blocksize data leaving the remainder in 'in' .
     */
    public int update(byte[] in, int inOfs, int inLen, ByteBuffer dst) {
        // If the bytebuffer is backed by arrays, use that instead of
        // allocating and copying for direct bytebuffers
        if (!dst.isDirect()) {
            int len = update(in, inOfs, inLen,  dst.array(),
                dst.arrayOffset() + dst.position());
            dst.position(dst.position() + len);
            return len;
        }

        // Direct ByteBuffer operation
        if (inLen - inOfs > in.length) {
            throw new RuntimeException("input length out of bound");
        }
        if (inLen < 0) {
            throw new RuntimeException("input length unsupported");
        }
        // See GaloisCounterMode. decryptFinal(bytebuffer, bytebuffer) for
        // details on the check for 'dst' having enough space for the result.

        long blocksLeft = blocksUntilRollover();
        int numOfCompleteBlocks = inLen / blockSize;
        if (numOfCompleteBlocks >= blocksLeft) {
            // Counter Mode encryption cannot be used because counter will
            // roll over incorrectly. Use GCM-specific code instead.
            checkBlock();
            for (int i = 0; i < numOfCompleteBlocks; i++) {
                embeddedCipher.encryptBlock(counter, 0, block, 0);
                for (int n = 0; n < blockSize; n++) {
                    int index = (i * blockSize + n);
                    dst.put((byte) ((in[inOfs + index] ^ block[n])));
                }
                GaloisCounterMode.increment32(counter);
            }
            return inLen;
        } else {
            int len = inLen - inLen % blockSize;
            int processed = len;
            byte[] out = new byte[Math.min(MAX_LEN, len)];
            int offset = inOfs;
            while (processed > MAX_LEN) {
                encrypt(in, offset, MAX_LEN, out, 0);
                dst.put(out, 0, MAX_LEN);
                processed -= MAX_LEN;
                offset += MAX_LEN;
            }
            encrypt(in, offset, processed, out, 0);
            // If dst is less than blocksize, insert only what it can.  Extra
            // bytes would cause buffers with enough size to fail with a
            // short buffer
            dst.put(out, 0, Math.min(dst.remaining(), processed));
            return len;
        }
    }

    /**
     * Operate on only blocksize data leaving the remainder in the src buffer.
     */
    public int update(ByteBuffer src, ByteBuffer dst) {
        int len;

        // If the bytebuffer is backed by arrays, use that instead of
        // allocating and copying for direct bytebuffers
        if (src.hasArray() && dst.hasArray()) {
            len = update(src.array(), src.arrayOffset() + src.position(),
                src.remaining() - (src.remaining() % blockSize),
                dst.array(), dst.arrayOffset() + dst.position());
            src.position(src.position() + len);
            dst.position(dst.position() + len);
            return len;
        }

        // Direct bytebuffer operation
        long blocksLeft = blocksUntilRollover();
        int numOfCompleteBlocks = src.remaining() / blockSize;
        if (numOfCompleteBlocks >= blocksLeft) {
            // Counter Mode encryption cannot be used because counter will
            // roll over incorrectly. Use GCM-specific code instead.
            checkBlock();
            for (int i = 0; i < numOfCompleteBlocks; i++) {
                embeddedCipher.encryptBlock(counter, 0, block, 0);
                for (int n = 0; n < blockSize; n++) {
                    dst.put((byte) (src.get() ^ block[n]));
                }
                GaloisCounterMode.increment32(counter);
            }
            return numOfCompleteBlocks * blockSize;
        }

        len = src.remaining() - (src.remaining() % blockSize);
        int processed = len;
        byte[] in = new byte[Math.min(MAX_LEN, len)];
        while (processed > MAX_LEN) {
            src.get(in, 0, MAX_LEN);
            encrypt(in, 0, MAX_LEN, in, 0);
            dst.put(in, 0, MAX_LEN);
            processed -= MAX_LEN;
        }
        src.get(in, 0, processed);
        encrypt(in, 0, processed, in, 0);
        dst.put(in, 0, processed);
        return len;
    }

    /**
     * doFinal operation by using update() for any full block operations needed,
     * then operating on the final bytes in the input buffer.
     *
     * This method will not write any block padding to the output buffer
     */
    public int doFinal(byte[] in, int inOfs, int inLen, byte[] out,
        int outOfs) {
        if (inLen == 0) {
            return 0;
        }
        int lastBlockSize = inLen % blockSize;
        int completeBlkLen = inLen - lastBlockSize;
        // process the complete blocks first
        update(in, inOfs, completeBlkLen, out, outOfs);
        if (lastBlockSize != 0) {
            // do the last partial block
            checkBlock();
            embeddedCipher.encryptBlock(counter, 0, block, 0);
            for (int n = 0; n < lastBlockSize; n++) {
                out[outOfs + completeBlkLen + n] =
                    (byte) ((in[inOfs + completeBlkLen + n] ^ block[n]));
            }
        }
        return inLen;
    }

    /**
     * doFinal operation by using update() for any full block operations needed,
     * then operating on the final bytes in the input buffer.
     *
     * If src and dst are array-backed bytebuffers, call doFinal(byte[]...) for
     * less memory usage.
     */
    public int doFinal(ByteBuffer src, ByteBuffer dst) {
        // If the bytebuffer is backed by arrays, use that instead of
        // allocating and copying for direct bytebuffers
        if (src.hasArray() && dst.hasArray()) {
            int len = doFinal(src.array(), src.arrayOffset() + src.position(),
                src.remaining(), dst.array(),
                dst.arrayOffset() + dst.position());
            src.position(src.position() + len);
            dst.position(dst.position() + len);
            return len;
        }

        int len = src.remaining();
        int lastBlockSize = len % blockSize;
        update(src, dst);
        if (lastBlockSize != 0) {
            checkBlock();
            // do the last partial block
            embeddedCipher.encryptBlock(counter, 0, block, 0);
            for (int n = 0; n < lastBlockSize; n++) {
                dst.put((byte) (src.get() ^ block[n]));
            }
        }
        return len;
    }
}
