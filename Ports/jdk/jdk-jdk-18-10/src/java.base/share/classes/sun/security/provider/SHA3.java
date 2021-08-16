/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import jdk.internal.vm.annotation.IntrinsicCandidate;
import static sun.security.provider.ByteArrayAccess.*;
import java.nio.*;
import java.util.*;
import java.security.*;

/**
 * This class implements the Secure Hash Algorithm SHA-3 developed by
 * the National Institute of Standards and Technology along with the
 * National Security Agency as defined in FIPS PUB 202.
 *
 * <p>It implements java.security.MessageDigestSpi, and can be used
 * through Java Cryptography Architecture (JCA), as a pluggable
 * MessageDigest implementation.
 *
 * @since       9
 * @author      Valerie Peng
 */
abstract class SHA3 extends DigestBase {

    private static final int WIDTH = 200; // in bytes, e.g. 1600 bits
    private static final int DM = 5; // dimension of lanes

    private static final int NR = 24; // number of rounds

    // precomputed round constants needed by the step mapping Iota
    private static final long[] RC_CONSTANTS = {
        0x01L, 0x8082L, 0x800000000000808aL,
        0x8000000080008000L, 0x808bL, 0x80000001L,
        0x8000000080008081L, 0x8000000000008009L, 0x8aL,
        0x88L, 0x80008009L, 0x8000000aL,
        0x8000808bL, 0x800000000000008bL, 0x8000000000008089L,
        0x8000000000008003L, 0x8000000000008002L, 0x8000000000000080L,
        0x800aL, 0x800000008000000aL, 0x8000000080008081L,
        0x8000000000008080L, 0x80000001L, 0x8000000080008008L,
    };

    private final byte suffix;
    private byte[] state = new byte[WIDTH];
    private long[] lanes = new long[DM*DM];

    /**
     * Creates a new SHA-3 object.
     */
    SHA3(String name, int digestLength, byte suffix, int c) {
        super(name, digestLength, (WIDTH - c));
        this.suffix = suffix;
    }

    private void implCompressCheck(byte[] b, int ofs) {
        Objects.requireNonNull(b);
    }

    /**
     * Core compression function. Processes blockSize bytes at a time
     * and updates the state of this object.
     */
    void implCompress(byte[] b, int ofs) {
        implCompressCheck(b, ofs);
        implCompress0(b, ofs);
    }

    @IntrinsicCandidate
    private void implCompress0(byte[] b, int ofs) {
       for (int i = 0; i < buffer.length; i++) {
           state[i] ^= b[ofs++];
       }
       keccak();
    }

    /**
     * Return the digest. Subclasses do not need to reset() themselves,
     * DigestBase calls implReset() when necessary.
     */
    void implDigest(byte[] out, int ofs) {
        int numOfPadding =
            setPaddingBytes(suffix, buffer, (int)(bytesProcessed % buffer.length));
        if (numOfPadding < 1) {
            throw new ProviderException("Incorrect pad size: " + numOfPadding);
        }
        implCompress(buffer, 0);
        System.arraycopy(state, 0, out, ofs, engineGetDigestLength());
    }

    /**
     * Resets the internal state to start a new hash.
     */
    void implReset() {
        Arrays.fill(state, (byte)0);
        Arrays.fill(lanes, 0L);
    }

    /**
     * Utility function for padding the specified data based on the
     * pad10*1 algorithm (section 5.1) and the 2-bit suffix "01" required
     * for SHA-3 hash (section 6.1).
     */
    private static int setPaddingBytes(byte suffix, byte[] in, int len) {
        if (len != in.length) {
            // erase leftover values
            Arrays.fill(in, len, in.length, (byte)0);
            // directly store the padding bytes into the input
            // as the specified buffer is allocated w/ size = rateR
            in[len] |= suffix;
            in[in.length - 1] |= (byte) 0x80;
        }
        return (in.length - len);
    }

    /**
     * Utility function for transforming the specified byte array 's'
     * into array of lanes 'm' as defined in section 3.1.2.
     */
    private static void bytes2Lanes(byte[] s, long[] m) {
        int sOfs = 0;
        // Conversion traverses along x-axis before y-axis
        for (int y = 0; y < DM; y++, sOfs += 40) {
            b2lLittle(s, sOfs, m, DM*y, 40);
        }
    }

    /**
     * Utility function for transforming the specified array of
     * lanes 'm' into a byte array 's' as defined in section 3.1.3.
     */
    private static void lanes2Bytes(long[] m, byte[] s) {
        int sOfs = 0;
        // Conversion traverses along x-axis before y-axis
        for (int y = 0; y < DM; y++, sOfs += 40) {
            l2bLittle(m, DM*y, s, sOfs, 40);
        }
    }

    /**
     * Step mapping Theta as defined in section 3.2.1 .
     */
    private static long[] smTheta(long[] a) {
        long c0 = a[0]^a[5]^a[10]^a[15]^a[20];
        long c1 = a[1]^a[6]^a[11]^a[16]^a[21];
        long c2 = a[2]^a[7]^a[12]^a[17]^a[22];
        long c3 = a[3]^a[8]^a[13]^a[18]^a[23];
        long c4 = a[4]^a[9]^a[14]^a[19]^a[24];
        long d0 = c4 ^ Long.rotateLeft(c1, 1);
        long d1 = c0 ^ Long.rotateLeft(c2, 1);
        long d2 = c1 ^ Long.rotateLeft(c3, 1);
        long d3 = c2 ^ Long.rotateLeft(c4, 1);
        long d4 = c3 ^ Long.rotateLeft(c0, 1);
        for (int y = 0; y < a.length; y += DM) {
            a[y] ^= d0;
            a[y+1] ^= d1;
            a[y+2] ^= d2;
            a[y+3] ^= d3;
            a[y+4] ^= d4;
        }
        return a;
    }

    /**
     * Merged Step mapping Rho (section 3.2.2) and Pi (section 3.2.3).
     * for performance. Optimization is achieved by precalculating
     * shift constants for the following loop
     *   int xNext, yNext;
     *   for (int t = 0, x = 1, y = 0; t <= 23; t++, x = xNext, y = yNext) {
     *        int numberOfShift = ((t + 1)*(t + 2)/2) % 64;
     *        a[y][x] = Long.rotateLeft(a[y][x], numberOfShift);
     *        xNext = y;
     *        yNext = (2 * x + 3 * y) % DM;
     *   }
     * and with inplace permutation.
     */
    private static long[] smPiRho(long[] a) {
        long tmp = Long.rotateLeft(a[10], 3);
        a[10] = Long.rotateLeft(a[1], 1);
        a[1] = Long.rotateLeft(a[6], 44);
        a[6] = Long.rotateLeft(a[9], 20);
        a[9] = Long.rotateLeft(a[22], 61);
        a[22] = Long.rotateLeft(a[14], 39);
        a[14] = Long.rotateLeft(a[20], 18);
        a[20] = Long.rotateLeft(a[2], 62);
        a[2] = Long.rotateLeft(a[12], 43);
        a[12] = Long.rotateLeft(a[13], 25);
        a[13] = Long.rotateLeft(a[19], 8);
        a[19] = Long.rotateLeft(a[23], 56);
        a[23] = Long.rotateLeft(a[15], 41);
        a[15] = Long.rotateLeft(a[4], 27);
        a[4] = Long.rotateLeft(a[24], 14);
        a[24] = Long.rotateLeft(a[21], 2);
        a[21] = Long.rotateLeft(a[8], 55);
        a[8] = Long.rotateLeft(a[16], 45);
        a[16] = Long.rotateLeft(a[5], 36);
        a[5] = Long.rotateLeft(a[3], 28);
        a[3] = Long.rotateLeft(a[18], 21);
        a[18] = Long.rotateLeft(a[17], 15);
        a[17] = Long.rotateLeft(a[11], 10);
        a[11] = Long.rotateLeft(a[7], 6);
        a[7] = tmp;
        return a;
    }

    /**
     * Step mapping Chi as defined in section 3.2.4.
     */
    private static long[] smChi(long[] a) {
        for (int y = 0; y < a.length; y+=DM) {
            long ay0 = a[y];
            long ay1 = a[y+1];
            long ay2 = a[y+2];
            long ay3 = a[y+3];
            long ay4 = a[y+4];
            a[y] = ay0 ^ ((~ay1) & ay2);
            a[y+1] = ay1 ^ ((~ay2) & ay3);
            a[y+2] = ay2 ^ ((~ay3) & ay4);
            a[y+3] = ay3 ^ ((~ay4) & ay0);
            a[y+4] = ay4 ^ ((~ay0) & ay1);
        }
        return a;
    }

    /**
     * Step mapping Iota as defined in section 3.2.5.
     */
    private static long[] smIota(long[] a, int rndIndex) {
        a[0] ^= RC_CONSTANTS[rndIndex];
        return a;
    }

    /**
     * The function Keccak as defined in section 5.2 with
     * rate r = 1600 and capacity c = (digest length x 2).
     */
    private void keccak() {
        // convert the 200-byte state into 25 lanes
        bytes2Lanes(state, lanes);
        // process the lanes through step mappings
        for (int ir = 0; ir < NR; ir++) {
            smIota(smChi(smPiRho(smTheta(lanes))), ir);
        }
        // convert the resulting 25 lanes back into 200-byte state
        lanes2Bytes(lanes, state);
    }

    public Object clone() throws CloneNotSupportedException {
        SHA3 copy = (SHA3) super.clone();
        copy.state = copy.state.clone();
        copy.lanes = new long[DM*DM];
        return copy;
    }

    /**
     * SHA3-224 implementation class.
     */
    public static final class SHA224 extends SHA3 {
        public SHA224() {
            super("SHA3-224", 28, (byte)0x06, 56);
        }
    }

    /**
     * SHA3-256 implementation class.
     */
    public static final class SHA256 extends SHA3 {
        public SHA256() {
            super("SHA3-256", 32, (byte)0x06, 64);
        }
    }

    /**
     * SHAs-384 implementation class.
     */
    public static final class SHA384 extends SHA3 {
        public SHA384() {
            super("SHA3-384", 48, (byte)0x06, 96);
        }
    }

    /**
     * SHA3-512 implementation class.
     */
    public static final class SHA512 extends SHA3 {
        public SHA512() {
            super("SHA3-512", 64, (byte)0x06, 128);
        }
    }
}
