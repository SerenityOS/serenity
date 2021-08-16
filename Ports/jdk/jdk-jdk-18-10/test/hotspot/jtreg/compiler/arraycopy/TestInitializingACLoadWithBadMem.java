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
 * @bug 8182036
 * @summary Load from initializing arraycopy uses wrong memory state
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM -XX:+StressLCM TestInitializingACLoadWithBadMem
 *
 */

import java.util.Arrays;

public class TestInitializingACLoadWithBadMem {
    static Object test_dummy;
    static int test1() {
        int[] src = new int[10];
        test_dummy = src;
        int[] dst = new int[10];
        src[1] = 0x42;
        // arraycopy generates a load from src/store to dst which must
        // be after the store to src above.
        System.arraycopy(src, 1, dst, 1, 9);
        return dst[1];
    }

    static public void main(String[] args) {
        int[] src = new int[10];
        for (int i = 0; i < 20000; i++) {
            int res = test1();
            if (res != 0x42) {
                throw new RuntimeException("bad result: " + res + " != " + 0x42);
            }
        }
    }
}
