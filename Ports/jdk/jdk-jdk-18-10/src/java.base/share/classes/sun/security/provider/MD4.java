/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.util.Arrays;

import static sun.security.provider.ByteArrayAccess.*;
import static sun.security.util.SecurityConstants.PROVIDER_VER;

/**
 * The MD4 class is used to compute an MD4 message digest over a given
 * buffer of bytes. It is an implementation of the RSA Data Security Inc
 * MD4 algorithim as described in internet RFC 1320.
 *
 * <p>The MD4 algorithm is very weak and should not be used unless it is
 * unavoidable. Therefore, it is not registered in our standard providers. To
 * obtain an implementation, call the static getInstance() method in this
 * class.
 *
 * @author      Andreas Sterbenz
 */
public final class MD4 extends DigestBase {

    // state of this object
    private int[] state;

    // rotation constants
    private static final int S11 = 3;
    private static final int S12 = 7;
    private static final int S13 = 11;
    private static final int S14 = 19;
    private static final int S21 = 3;
    private static final int S22 = 5;
    private static final int S23 = 9;
    private static final int S24 = 13;
    private static final int S31 = 3;
    private static final int S32 = 9;
    private static final int S33 = 11;
    private static final int S34 = 15;

    private static final Provider md4Provider;

    static {
        md4Provider = new Provider("MD4Provider", PROVIDER_VER,
            "MD4 MessageDigest") {
            @java.io.Serial
            private static final long serialVersionUID = -8850464997518327965L;
        };
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                md4Provider.put("MessageDigest.MD4", "sun.security.provider.MD4");
                return null;
            }
        });
    }

    public static MessageDigest getInstance() {
        try {
            return MessageDigest.getInstance("MD4", md4Provider);
        } catch (NoSuchAlgorithmException e) {
            // should never occur
            throw new ProviderException(e);
        }
    }

    // Standard constructor, creates a new MD4 instance.
    public MD4() {
        super("MD4", 16, 64);
        state = new int[4];
        resetHashes();
    }

    // clone this object
    public Object clone() throws CloneNotSupportedException {
        MD4 copy = (MD4) super.clone();
        copy.state = copy.state.clone();
        return copy;
    }

    /**
     * Reset the state of this object.
     */
    void implReset() {
        // Load magic initialization constants.
        resetHashes();
    }

    private void resetHashes() {
        state[0] = 0x67452301;
        state[1] = 0xefcdab89;
        state[2] = 0x98badcfe;
        state[3] = 0x10325476;
    }

    /**
     * Perform the final computations, any buffered bytes are added
     * to the digest, the count is added to the digest, and the resulting
     * digest is stored.
     */
    void implDigest(byte[] out, int ofs) {
        long bitsProcessed = bytesProcessed << 3;

        int index = (int)bytesProcessed & 0x3f;
        int padLen = (index < 56) ? (56 - index) : (120 - index);
        engineUpdate(padding, 0, padLen);

        i2bLittle4((int)bitsProcessed, buffer, 56);
        i2bLittle4((int)(bitsProcessed >>> 32), buffer, 60);
        implCompress(buffer, 0);

        i2bLittle(state, 0, out, ofs, 16);
    }

    private static int FF(int a, int b, int c, int d, int x, int s) {
        a += ((b & c) | ((~b) & d)) + x;
        return ((a << s) | (a >>> (32 - s)));
    }

    private static int GG(int a, int b, int c, int d, int x, int s) {
        a += ((b & c) | (b & d) | (c & d)) + x + 0x5a827999;
        return ((a << s) | (a >>> (32 - s)));
    }

    private static int HH(int a, int b, int c, int d, int x, int s) {
        a += ((b ^ c) ^ d) + x + 0x6ed9eba1;
        return ((a << s) | (a >>> (32 - s)));
    }

    /**
     * This is where the functions come together as the generic MD4
     * transformation operation. It consumes sixteen
     * bytes from the buffer, beginning at the specified offset.
     */
    void implCompress(byte[] buf, int ofs) {
        int x0 = (int) LE.INT_ARRAY.get(buf, ofs);
        int x1 = (int) LE.INT_ARRAY.get(buf, ofs + 4);
        int x2 = (int) LE.INT_ARRAY.get(buf, ofs + 8);
        int x3 = (int) LE.INT_ARRAY.get(buf, ofs + 12);
        int x4 = (int) LE.INT_ARRAY.get(buf, ofs + 16);
        int x5 = (int) LE.INT_ARRAY.get(buf, ofs + 20);
        int x6 = (int) LE.INT_ARRAY.get(buf, ofs + 24);
        int x7 = (int) LE.INT_ARRAY.get(buf, ofs + 28);
        int x8 = (int) LE.INT_ARRAY.get(buf, ofs + 32);
        int x9 = (int) LE.INT_ARRAY.get(buf, ofs + 36);
        int x10 = (int) LE.INT_ARRAY.get(buf, ofs + 40);
        int x11 = (int) LE.INT_ARRAY.get(buf, ofs + 44);
        int x12 = (int) LE.INT_ARRAY.get(buf, ofs + 48);
        int x13 = (int) LE.INT_ARRAY.get(buf, ofs + 52);
        int x14 = (int) LE.INT_ARRAY.get(buf, ofs + 56);
        int x15 = (int) LE.INT_ARRAY.get(buf, ofs + 60);

        int a = state[0];
        int b = state[1];
        int c = state[2];
        int d = state[3];

        /* Round 1 */
        a = FF (a, b, c, d, x0,  S11); /* 1 */
        d = FF (d, a, b, c, x1,  S12); /* 2 */
        c = FF (c, d, a, b, x2,  S13); /* 3 */
        b = FF (b, c, d, a, x3,  S14); /* 4 */
        a = FF (a, b, c, d, x4,  S11); /* 5 */
        d = FF (d, a, b, c, x5,  S12); /* 6 */
        c = FF (c, d, a, b, x6,  S13); /* 7 */
        b = FF (b, c, d, a, x7,  S14); /* 8 */
        a = FF (a, b, c, d, x8,  S11); /* 9 */
        d = FF (d, a, b, c, x9,  S12); /* 10 */
        c = FF (c, d, a, b, x10, S13); /* 11 */
        b = FF (b, c, d, a, x11, S14); /* 12 */
        a = FF (a, b, c, d, x12, S11); /* 13 */
        d = FF (d, a, b, c, x13, S12); /* 14 */
        c = FF (c, d, a, b, x14, S13); /* 15 */
        b = FF (b, c, d, a, x15, S14); /* 16 */

        /* Round 2 */
        a = GG (a, b, c, d, x0,  S21); /* 17 */
        d = GG (d, a, b, c, x4,  S22); /* 18 */
        c = GG (c, d, a, b, x8,  S23); /* 19 */
        b = GG (b, c, d, a, x12, S24); /* 20 */
        a = GG (a, b, c, d, x1,  S21); /* 21 */
        d = GG (d, a, b, c, x5,  S22); /* 22 */
        c = GG (c, d, a, b, x9,  S23); /* 23 */
        b = GG (b, c, d, a, x13, S24); /* 24 */
        a = GG (a, b, c, d, x2,  S21); /* 25 */
        d = GG (d, a, b, c, x6,  S22); /* 26 */
        c = GG (c, d, a, b, x10, S23); /* 27 */
        b = GG (b, c, d, a, x14, S24); /* 28 */
        a = GG (a, b, c, d, x3,  S21); /* 29 */
        d = GG (d, a, b, c, x7,  S22); /* 30 */
        c = GG (c, d, a, b, x11, S23); /* 31 */
        b = GG (b, c, d, a, x15, S24); /* 32 */

        /* Round 3 */
        a = HH (a, b, c, d, x0,  S31); /* 33 */
        d = HH (d, a, b, c, x8,  S32); /* 34 */
        c = HH (c, d, a, b, x4,  S33); /* 35 */
        b = HH (b, c, d, a, x12, S34); /* 36 */
        a = HH (a, b, c, d, x2,  S31); /* 37 */
        d = HH (d, a, b, c, x10, S32); /* 38 */
        c = HH (c, d, a, b, x6,  S33); /* 39 */
        b = HH (b, c, d, a, x14, S34); /* 40 */
        a = HH (a, b, c, d, x1,  S31); /* 41 */
        d = HH (d, a, b, c, x9,  S32); /* 42 */
        c = HH (c, d, a, b, x5,  S33); /* 43 */
        b = HH (b, c, d, a, x13, S34); /* 44 */
        a = HH (a, b, c, d, x3,  S31); /* 45 */
        d = HH (d, a, b, c, x11, S32); /* 46 */
        c = HH (c, d, a, b, x7,  S33); /* 47 */
        b = HH (b, c, d, a, x15, S34); /* 48 */

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
    }

}
