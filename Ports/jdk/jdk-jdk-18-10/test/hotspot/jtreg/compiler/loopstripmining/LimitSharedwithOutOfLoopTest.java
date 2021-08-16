/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8193597
 * @summary limit test is shared with out of loop if
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-BackgroundCompilation -XX:LoopUnrollLimit=0 -XX:+UseCountedLoopSafepoints -XX:LoopStripMiningIter=1000 LimitSharedwithOutOfLoopTest
 *
 */

public class LimitSharedwithOutOfLoopTest {
    public static void main(String[] args) {
        boolean[] array1 = new boolean[2001];
        boolean[] array2 = new boolean[2001];
        boolean[] array3 = new boolean[2001];
        array2[1000] = true;
        array3[2000] = true;
        for (int i = 0; i < 20_000; i++) {
            if (test(2000, array1)) {
                throw new RuntimeException("bad return");
            }
            if (!test(2000, array2)) {
                throw new RuntimeException("bad return");
            }
            if (test(2000, array3)) {
                throw new RuntimeException("bad return");
            }
        }
    }

    static volatile boolean barrier;

    private static boolean test(int limit, boolean[] array) {
        for (int i = 0; i < limit;) {
            i++;
            if (array[i]) {
                // Same test as end of loop test. When loop is strip
                // mined, this must not become the end of inner loop
                // test.
                if (i < limit) {
                    return true;
                }
                return false;
            }
            barrier = true;
        }
        return false;
    }
}
