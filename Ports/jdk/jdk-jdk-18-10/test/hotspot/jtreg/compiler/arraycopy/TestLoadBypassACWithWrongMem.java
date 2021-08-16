/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @key stress randomness
 * @bug 8181742
 * @summary Loads that bypass arraycopy ends up with wrong memory state
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM -XX:+StressLCM TestLoadBypassACWithWrongMem
 *
 */

import java.util.Arrays;

public class TestLoadBypassACWithWrongMem {

    static int test1(int[] src) {
        int[] dst = new int[10];
        System.arraycopy(src, 0, dst, 0, 10);
        src[1] = 0x42;
        // dst[1] is transformed to src[1], src[1] must use the
        // correct memory state (not the store above).
        return dst[1];
    }

    static int test2(int[] src) {
        int[] dst = (int[])src.clone();
        src[1] = 0x42;
        // Same as above for clone
        return dst[1];
    }

    static Object test5_src = null;
    static int test3() {
        int[] dst = new int[10];
        System.arraycopy(test5_src, 0, dst, 0, 10);
        ((int[])test5_src)[1] = 0x42;
        System.arraycopy(test5_src, 0, dst, 0, 10);
        // dst[1] is transformed to test5_src[1]. test5_src is Object
        // but test5_src[1] must be on the slice for int[] not
        // Object+some offset.
        return dst[1];
    }

    static public void main(String[] args) {
        int[] src = new int[10];
        for (int i = 0; i < 20000; i++) {
            Arrays.fill(src, 0);
            int res = test1(src);
            if (res != 0) {
                throw new RuntimeException("bad result: " + res + " != " + 0);
            }
            Arrays.fill(src, 0);
            res = test2(src);
            if (res != 0) {
                throw new RuntimeException("bad result: " + res + " != " + 0);
            }
            Arrays.fill(src, 0);
            test5_src = src;
            res = test3();
            if (res != 0x42) {
                throw new RuntimeException("bad result: " + res + " != " + 0x42);
            }
         }
    }
}
