/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2015 Red Hat, Inc.
 */

package com.sun.crypto.provider;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.ProviderException;

import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * This class represents the GHASH function defined in NIST 800-38D
 * under section 6.4. It needs to be constructed w/ a hash subkey, i.e.
 * block H. Given input of 128-bit blocks, it will process and output
 * a 128-bit block.
 *
 * <p>This function is used in the implementation of GCM mode.
 *
 * @since 1.8
 */

final class GHASH implements Cloneable, GCM {

    private static final int AES_BLOCK_SIZE = 16;

    // Handle for converting byte[] <-> long
    private static final VarHandle asLongView =
        MethodHandles.byteArrayViewVarHandle(long[].class,
            ByteOrder.BIG_ENDIAN);

    // Maximum buffer size rotating ByteBuffer->byte[] intrinsic copy
    private static final int MAX_LEN = 1024;

    // Multiplies state[0], state[1] by subkeyH[0], subkeyH[1].
    private static void blockMult(long[] st, long[] subH) {
        long Z0 = 0;
        long Z1 = 0;
        long V0 = subH[0];
        long V1 = subH[1];
        long X;

        // Separate loops for processing state[0] and state[1].
        X = st[0];
        for (int i = 0; i < 64; i++) {
            // Zi+1 = Zi if bit i of x is 0
            long mask = X >> 63;
            Z0 ^= V0 & mask;
            Z1 ^= V1 & mask;

            // Save mask for conditional reduction below.
            mask = (V1 << 63) >> 63;

            // V = rightshift(V)
            long carry = V0 & 1;
            V0 = V0 >>> 1;
            V1 = (V1 >>> 1) | (carry << 63);

            // Conditional reduction modulo P128.
            V0 ^= 0xe100000000000000L & mask;
            X <<= 1;
        }

        X = st[1];
        for (int i = 64; i < 127; i++) {
            // Zi+1 = Zi if bit i of x is 0
            long mask = X >> 63;
            Z0 ^= V0 & mask;
            Z1 ^= V1 & mask;

            // Save mask for conditional reduction below.
            mask = (V1 << 63) >> 63;

            // V = rightshift(V)
            long carry = V0 & 1;
            V0 = V0 >>> 1;
            V1 = (V1 >>> 1) | (carry << 63);

            // Conditional reduction.
            V0 ^= 0xe100000000000000L & mask;
            X <<= 1;
        }

        // calculate Z128
        long mask = X >> 63;
        Z0 ^= V0 & mask;
        Z1 ^= V1 & mask;

        // Save result.
        st[0] = Z0;
        st[1] = Z1;

    }

    /* subkeyHtbl and state are stored in long[] for GHASH intrinsic use */

    // hashtable subkeyHtbl holds 2*9 powers of subkeyH computed using
    // carry-less multiplication
    private long[] subkeyHtbl;

    // buffer for storing hash
    private final long[] state;

    /**
     * Initializes the cipher in the specified mode with the given key
     * and iv.
     *
     * @param subkeyH the hash subkey
     *
     * @exception ProviderException if the given key is inappropriate for
     * initializing this digest
     */
    GHASH(byte[] subkeyH) throws ProviderException {
        if ((subkeyH == null) || subkeyH.length != AES_BLOCK_SIZE) {
            throw new ProviderException("Internal error");
        }
        state = new long[2];
        subkeyHtbl = new long[2*9];
        subkeyHtbl[0] = (long)asLongView.get(subkeyH, 0);
        subkeyHtbl[1] = (long)asLongView.get(subkeyH, 8);
    }

    // Cloning constructor
    private GHASH(GHASH g) {
        state = g.state.clone();
        subkeyHtbl = g.subkeyHtbl.clone();
    }

    @Override
    public GHASH clone() {
        return new GHASH(this);
    }

    private static void processBlock(byte[] data, int ofs, long[] st,
        long[] subH) {
        st[0] ^= (long)asLongView.get(data, ofs);
        st[1] ^= (long)asLongView.get(data, ofs + 8);
        blockMult(st, subH);
    }

    int update(byte[] in) {
        return update(in, 0, in.length);
    }

    int update(byte[] in, int inOfs, int inLen) {
        if (inLen == 0) {
            return 0;
        }
        int len = inLen - (inLen % AES_BLOCK_SIZE);
        ghashRangeCheck(in, inOfs, len, state, subkeyHtbl);
        processBlocks(in, inOfs, len / AES_BLOCK_SIZE, state, subkeyHtbl);
        return len;
    }

    // Will process as many blocks it can and will leave the remaining.
    int update(ByteBuffer ct, int inLen) {
        inLen -= (inLen % AES_BLOCK_SIZE);
        if (inLen == 0) {
            return 0;
        }

        // If ct is a direct bytebuffer, send it directly to the intrinsic
        if (ct.isDirect()) {
            int processed = inLen;
            processBlocksDirect(ct, inLen);
            return processed;
        } else if (!ct.isReadOnly()) {
            // If a non-read only heap bytebuffer, use the array update method
            int processed = update(ct.array(),
                ct.arrayOffset() + ct.position(),
                inLen);
            ct.position(ct.position() + processed);
            return processed;
        }

        // Read only heap bytebuffers have to be copied and operated on
        int to_process = inLen;
        byte[] in = new byte[Math.min(MAX_LEN, inLen)];
        while (to_process > MAX_LEN ) {
            ct.get(in, 0, MAX_LEN);
            update(in, 0 , MAX_LEN);
            to_process -= MAX_LEN;
        }
        ct.get(in, 0, to_process);
        update(in, 0, to_process);
        return inLen;
    }

    int doFinal(ByteBuffer src, int inLen) {
        int processed = 0;

        if (inLen >= AES_BLOCK_SIZE) {
            processed = update(src, inLen);
        }

        if (inLen == processed) {
            return processed;
        }
        byte[] block = new byte[AES_BLOCK_SIZE];
        src.get(block, 0, inLen - processed);
        update(block, 0, AES_BLOCK_SIZE);
        return inLen;
    }

    int doFinal(byte[] in, int inOfs, int inLen) {
        int remainder = inLen % AES_BLOCK_SIZE;
        inOfs += update(in, inOfs, inLen - remainder);
        if (remainder > 0) {
            byte[] block = new byte[AES_BLOCK_SIZE];
            System.arraycopy(in, inOfs, block, 0,
                remainder);
            update(block, 0, AES_BLOCK_SIZE);
        }
        return inLen;
    }

    private static void ghashRangeCheck(byte[] in, int inOfs, int inLen,
        long[] st, long[] subH) {
        if (inLen < 0) {
            throw new RuntimeException("invalid input length: " + inLen);
        }
        if (inOfs < 0) {
            throw new RuntimeException("invalid offset: " + inOfs);
        }
        if (inLen > in.length - inOfs) {
            throw new RuntimeException("input length out of bound: " +
                                       inLen + " > " + (in.length - inOfs));
        }
        if (inLen % AES_BLOCK_SIZE != 0) {
            throw new RuntimeException("input length/block size mismatch: " +
                                       inLen);
        }

        // These two checks are for C2 checking
        if (st.length != 2) {
            throw new RuntimeException("internal state has invalid length: " +
                                       st.length);
        }
        if (subH.length != 18) {
            throw new RuntimeException("internal subkeyHtbl has invalid length: " +
                                       subH.length);
        }
    }
    /*
     * This is an intrinsified method.  The method's argument list must match
     * the hotspot signature.  This method and methods called by it, cannot
     * throw exceptions or allocate arrays as it will breaking intrinsics
     */
    @IntrinsicCandidate
    private static void processBlocks(byte[] data, int inOfs, int blocks,
        long[] st, long[] subH) {
        int offset = inOfs;
        while (blocks > 0) {
            processBlock(data, offset, st, subH);
            blocks--;
            offset += AES_BLOCK_SIZE;
        }
    }

    // ProcessBlock for Direct ByteBuffers
    private void processBlocksDirect(ByteBuffer ct, int inLen) {
        byte[] data = new byte[Math.min(MAX_LEN, inLen)];
        while (inLen > MAX_LEN) {
            ct.get(data, 0, MAX_LEN);
            processBlocks(data, 0, MAX_LEN / AES_BLOCK_SIZE, state,
                subkeyHtbl);
            inLen -= MAX_LEN;
        }
        if (inLen >= AES_BLOCK_SIZE) {
            int len = inLen - (inLen % AES_BLOCK_SIZE);
            ct.get(data, 0, len);
            processBlocks(data, 0, len / AES_BLOCK_SIZE, state,
                subkeyHtbl);
        }
    }

    byte[] digest() {
        byte[] result = new byte[AES_BLOCK_SIZE];
        asLongView.set(result, 0, state[0]);
        asLongView.set(result, 8, state[1]);
        // Reset state
        state[0] = 0;
        state[1] = 0;
        return result;
    }


    /**
     * None of the out or dst values are necessary, they are to satisfy the
     * GCM interface requirement
     */
    @Override
    public int update(byte[] in, int inOfs, int inLen, byte[] out, int outOfs) {
        return update(in, inOfs, inLen);
    }

    @Override
    public int update(byte[] in, int inOfs, int inLen, ByteBuffer dst) {
        return update(in, inOfs, inLen);
    }

    @Override
    public int update(ByteBuffer src, ByteBuffer dst) {
        return update(src, src.remaining());
    }

    @Override
    public int doFinal(byte[] in, int inOfs, int inLen, byte[] out,
        int outOfs) {
        return doFinal(in, inOfs, inLen);
    }

    @Override
    public int doFinal(ByteBuffer src, ByteBuffer dst) {
        return doFinal(src, src.remaining());
    }
}
