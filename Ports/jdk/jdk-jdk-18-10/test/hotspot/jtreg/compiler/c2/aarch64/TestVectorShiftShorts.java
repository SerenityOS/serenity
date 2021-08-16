/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8255287
 * @summary aarch64: fix SVE patterns for vector shift count
 *
 * @requires os.arch == "aarch64" & vm.compiler2.enabled
 * @run main/othervm -XX:UseSVE=2 -Xbatch -XX:-TieredCompilation
 *      -XX:CompileCommand=compileonly,compiler.c2.aarch64.TestVectorShiftShorts::test_*
 *      compiler.c2.aarch64.TestVectorShiftShorts
 */

package compiler.c2.aarch64;

public class TestVectorShiftShorts {

    private static final int ARRLEN = 1000;
    private static final int ITERS  = 20000;

    public static void main(String args[]) {
        short[] a0 = new short[ARRLEN];
        short[] a1 = new short[ARRLEN];

        // Initialize
        test_init(a0, a1);

        // Warmup
        for (int i = 0; i < ITERS; i++) {
            test_lshift(a0, a1);
            test_urshift(a0, a1);
        }

        // Test and verify results
        test_init(a0, a1);
        test_lshift(a0, a1);
        verify_lshift(a0, a1);

        test_init(a0, a1);
        test_urshift(a0, a1);
        verify_urshift(a0, a1);

        // Finish
        System.out.println("Test passed");
    }

    static void test_init(short[] a0, short[] a1) {
        for (int i = 0; i < ARRLEN; i++) {
            a0[i] = (short)(i & 3);
            a1[i] = (short)i;
        }
    }

    static void test_lshift(short[] a0, short[] a1) {
        for (int i = 0; i < ARRLEN; i++) {
            a0[i] = (short)(a1[i] << 10);
        }
    }

    static void verify_lshift(short[] a0, short[] a1) {
        for (int i = 0; i < ARRLEN; i++) {
            if (a0[i] != (short)(a1[i] << 10)) {
                throw new RuntimeException("LShift test failed.");
            }
        }
    }

    static void test_urshift(short[] a0, short[] a1) {
        for (int i = 0; i < ARRLEN; i++) {
            a0[i] = (short)(a1[i] >>> 10);
        }
    }

    static void verify_urshift(short[] a0, short[] a1) {
        for (int i = 0; i < ARRLEN; i++) {
            if (a0[i] != (short)(a1[i] >>> 10)) {
                throw new RuntimeException("URshift test failed.");
            }
        }
    }

}
