/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

/**
 * Implementation for the MD2 algorithm, see RFC1319. It is very slow and
 * not particular secure. It is only supported to be able to verify
 * RSA/Verisign root certificates signed using MD2withRSA. It should not
 * be used for anything else.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class MD2 extends DigestBase {

    // state, 48 ints
    private int[] X;

    // checksum, 16 ints. they are really bytes, but byte arithmetic in
    // the JVM is much slower that int arithmetic.
    private int[] C;

    // temporary store for checksum C during final digest
    private byte[] cBytes;

    /**
     * Create a new MD2 digest. Called by the JCA framework
     */
    public MD2() {
        super("MD2", 16, 16);
        X = new int[48];
        C = new int[16];
        cBytes = new byte[16];
    }

    public Object clone() throws CloneNotSupportedException {
        MD2 copy = (MD2) super.clone();
        copy.X = copy.X.clone();
        copy.C = copy.C.clone();
        copy.cBytes = new byte[16];
        return copy;
    }

    // reset state and checksum
    void implReset() {
        Arrays.fill(X, 0);
        Arrays.fill(C, 0);
    }

    // finish the digest
    void implDigest(byte[] out, int ofs) {
        int padValue = 16 - ((int)bytesProcessed & 15);
        engineUpdate(PADDING[padValue], 0, padValue);
        for (int i = 0; i < 16; i++) {
            cBytes[i] = (byte)C[i];
        }
        implCompress(cBytes, 0);
        for (int i = 0; i < 16; i++) {
            out[ofs + i] = (byte)X[i];
        }
    }

    // one iteration of the compression function
    void implCompress(byte[] b, int ofs) {
        for (int i = 0; i < 16; i++) {
            int k = b[ofs + i] & 0xff;
            X[16 + i] = k;
            X[32 + i] = k ^ X[i];
        }

        // update the checksum
        int t = C[15];
        for (int i = 0; i < 16; i++) {
            t = (C[i] ^= S[X[16 + i] ^ t]);
        }

        t = 0;
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 48; j++) {
                t = (X[j] ^= S[t]);
            }
            t = (t + i) & 0xff;
        }
    }

    // substitution table derived from Pi. Copied from the RFC.
    private static final int[] S = new int[] {
        41, 46, 67, 201, 162, 216, 124, 1, 61, 54, 84, 161, 236, 240, 6,
        19, 98, 167, 5, 243, 192, 199, 115, 140, 152, 147, 43, 217, 188,
        76, 130, 202, 30, 155, 87, 60, 253, 212, 224, 22, 103, 66, 111, 24,
        138, 23, 229, 18, 190, 78, 196, 214, 218, 158, 222, 73, 160, 251,
        245, 142, 187, 47, 238, 122, 169, 104, 121, 145, 21, 178, 7, 63,
        148, 194, 16, 137, 11, 34, 95, 33, 128, 127, 93, 154, 90, 144, 50,
        39, 53, 62, 204, 231, 191, 247, 151, 3, 255, 25, 48, 179, 72, 165,
        181, 209, 215, 94, 146, 42, 172, 86, 170, 198, 79, 184, 56, 210,
        150, 164, 125, 182, 118, 252, 107, 226, 156, 116, 4, 241, 69, 157,
        112, 89, 100, 113, 135, 32, 134, 91, 207, 101, 230, 45, 168, 2, 27,
        96, 37, 173, 174, 176, 185, 246, 28, 70, 97, 105, 52, 64, 126, 15,
        85, 71, 163, 35, 221, 81, 175, 58, 195, 92, 249, 206, 186, 197,
        234, 38, 44, 83, 13, 110, 133, 40, 132, 9, 211, 223, 205, 244, 65,
        129, 77, 82, 106, 220, 55, 200, 108, 193, 171, 250, 36, 225, 123,
        8, 12, 189, 177, 74, 120, 136, 149, 139, 227, 99, 232, 109, 233,
        203, 213, 254, 59, 0, 29, 57, 242, 239, 183, 14, 102, 88, 208, 228,
        166, 119, 114, 248, 235, 117, 75, 10, 49, 68, 80, 180, 143, 237,
        31, 26, 219, 153, 141, 51, 159, 17, 131, 20,
    };

    // digest padding. 17 element array.
    // padding[0] is null
    // padding[i] is an array of i time the byte value i (i = 1..16)
    private static final byte[][] PADDING;

    static {
        PADDING = new byte[17][];
        for (int i = 1; i < 17; i++) {
            byte[] b = new byte[i];
            Arrays.fill(b, (byte)i);
            PADDING[i] = b;
        }
    }

}
