/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @key randomness
 * @bug 6661247
 * @summary Internal bug in 32-bit HotSpot optimizer while bit manipulations
 *
 * @library /test/lib
 * @run main compiler.c2.Test6661247
 */

package compiler.c2;

import java.nio.LongBuffer;
import java.util.Random;
import jdk.test.lib.Utils;

// This isn't a completely reliable test for 6661247 since the results
// depend on what the local schedule looks like but it does reproduce
// the issue in current builds.

public class Test6661247 {

    public static void test(boolean[] src, int srcPos, LongBuffer dest, long destPos, int count) {
        int countStart = (destPos & 63) == 0 ? 0 : 64 - (int)(destPos & 63);
        if (countStart > count)
            countStart = count;
        for (int srcPosMax = srcPos + countStart; srcPos < srcPosMax; srcPos++, destPos++) {
            if (src[srcPos])
                dest.put((int)(destPos >>> 6), dest.get((int)(destPos >>> 6)) | 1L << (destPos & 63));
            else
                dest.put((int)(destPos >>> 6), dest.get((int)(destPos >>> 6)) & ~(1L << (destPos & 63)));
        }
        count -= countStart;
        int cnt = count >>> 6;
        for (int k = (int)(destPos >>> 6), kMax = k + cnt; k < kMax; k++) {
            int low = (src[srcPos] ? 1 : 0)
                | (src[srcPos + 1] ? 1 << 1 : 0)
                | (src[srcPos + 2] ? 1 << 2 : 0)
                | (src[srcPos + 3] ? 1 << 3 : 0)
                | (src[srcPos + 4] ? 1 << 4 : 0)
                | (src[srcPos + 5] ? 1 << 5 : 0)
                | (src[srcPos + 6] ? 1 << 6 : 0)
                | (src[srcPos + 7] ? 1 << 7 : 0)
                | (src[srcPos + 8] ? 1 << 8 : 0)
                | (src[srcPos + 9] ? 1 << 9 : 0)
                | (src[srcPos + 10] ? 1 << 10 : 0)
                | (src[srcPos + 11] ? 1 << 11 : 0)
                | (src[srcPos + 12] ? 1 << 12 : 0)
                | (src[srcPos + 13] ? 1 << 13 : 0)
                | (src[srcPos + 14] ? 1 << 14 : 0)
                | (src[srcPos + 15] ? 1 << 15 : 0)
                | (src[srcPos + 16] ? 1 << 16 : 0)
                | (src[srcPos + 17] ? 1 << 17 : 0)
                | (src[srcPos + 18] ? 1 << 18 : 0)
                | (src[srcPos + 19] ? 1 << 19 : 0)
                | (src[srcPos + 20] ? 1 << 20 : 0)
                | (src[srcPos + 21] ? 1 << 21 : 0)
                | (src[srcPos + 22] ? 1 << 22 : 0)
                | (src[srcPos + 23] ? 1 << 23 : 0)
                | (src[srcPos + 24] ? 1 << 24 : 0)
                | (src[srcPos + 25] ? 1 << 25 : 0)
                | (src[srcPos + 26] ? 1 << 26 : 0)
                | (src[srcPos + 27] ? 1 << 27 : 0)
                | (src[srcPos + 28] ? 1 << 28 : 0)
                | (src[srcPos + 29] ? 1 << 29 : 0)
                | (src[srcPos + 30] ? 1 << 30 : 0)
                | (src[srcPos + 31] ? 1 << 31 : 0)
                ;
            srcPos += 32;
            int high = (src[srcPos] ? 1 : 0)        // PROBLEM!
                | (src[srcPos + 1] ? 1 << 1 : 0)
                | (src[srcPos + 2] ? 1 << 2 : 0)
                | (src[srcPos + 3] ? 1 << 3 : 0)
                | (src[srcPos + 4] ? 1 << 4 : 0)
                | (src[srcPos + 5] ? 1 << 5 : 0)
                | (src[srcPos + 6] ? 1 << 6 : 0)
                | (src[srcPos + 7] ? 1 << 7 : 0)
                | (src[srcPos + 8] ? 1 << 8 : 0)
                | (src[srcPos + 9] ? 1 << 9 : 0)
                | (src[srcPos + 10] ? 1 << 10 : 0)
                | (src[srcPos + 11] ? 1 << 11 : 0)
                | (src[srcPos + 12] ? 1 << 12 : 0)
                | (src[srcPos + 13] ? 1 << 13 : 0)
                | (src[srcPos + 14] ? 1 << 14 : 0)
                | (src[srcPos + 15] ? 1 << 15 : 0)
                | (src[srcPos + 16] ? 1 << 16 : 0)
                | (src[srcPos + 17] ? 1 << 17 : 0)
                | (src[srcPos + 18] ? 1 << 18 : 0)
                | (src[srcPos + 19] ? 1 << 19 : 0)
                | (src[srcPos + 20] ? 1 << 20 : 0)
                | (src[srcPos + 21] ? 1 << 21 : 0)
                | (src[srcPos + 22] ? 1 << 22 : 0)
                | (src[srcPos + 23] ? 1 << 23 : 0)
                | (src[srcPos + 24] ? 1 << 24 : 0)
                | (src[srcPos + 25] ? 1 << 25 : 0)
                | (src[srcPos + 26] ? 1 << 26 : 0)
                | (src[srcPos + 27] ? 1 << 27 : 0)
                | (src[srcPos + 28] ? 1 << 28 : 0)
                | (src[srcPos + 29] ? 1 << 29 : 0)
                | (src[srcPos + 30] ? 1 << 30 : 0)
                | (src[srcPos + 31] ? 1 << 31 : 0)
                ;
            srcPos += 32;
            dest.put(k, ((long)low & 0xFFFFFFFFL) | (((long)high) << 32));
            destPos += 64;
        }
        int countFinish = count & 63;
        for (int srcPosMax = srcPos + countFinish; srcPos < srcPosMax; srcPos++, destPos++) {
            if (src[srcPos])
                dest.put((int)(destPos >>> 6), dest.get((int)(destPos >>> 6)) | 1L << (destPos & 63));
            else
                dest.put((int)(destPos >>> 6), dest.get((int)(destPos >>> 6)) & ~(1L << (destPos & 63)));
        }
    }
    public static void main(String[] args) {
        Random r = Utils.getRandomInstance();
        int entries = 1000;
        boolean[] src = new boolean[entries * 64];
        long[] dest = new long[entries];
        long[] result = new long[entries];

        for (int c = 0; c < 2000; c++) {
            for (int i = 0; i < entries; i++) {
                long l = r.nextLong();
                for (int bit = 0; bit < 64; bit++) {
                    src[i * 64 + bit] = (l & (1L << bit)) != 0;
                }
                dest[i] = 0;
                result[i] = l;
            }
            test(src, 0, LongBuffer.wrap(dest, 0, dest.length), 0, src.length);
            for (int i = 0; i < entries; i++) {
                if (dest[i] != result[i]) {
                    throw new InternalError(i + ": " + Long.toHexString(dest[i]) + " != " + Long.toHexString(result[i]));
                }
            }
        }
    }
}
