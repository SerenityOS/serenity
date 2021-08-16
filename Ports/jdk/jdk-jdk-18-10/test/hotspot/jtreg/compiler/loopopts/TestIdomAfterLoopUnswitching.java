/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8268261
 * @summary Test idom data after unswitching loop following by full unroll.
 * @run main/othervm -XX:CompileCommand=compileonly,TestIdomAfterLoopUnswitching::*
 *                   -Xcomp -XX:-TieredCompilation TestIdomAfterLoopUnswitching
 */

public class TestIdomAfterLoopUnswitching {

    public static void main(String[] k) {
        test1();
        test2();
    }

    public static void test1() {
        float h = 0;
        for (int j = 0; j < 3; ++j) {
            float k = 9;
            float[] fla = new float[2];
            for (int n = 0; n < 5; ++n) {
                if (j >= 1) {
                    if (n <= 1) {
                        h += k;
                    }
                }
            }
            for (int l12 = 0; l12 < 9; ++l12) {
                for (int o = 0; o < 1; ++o) {
                    fla[0] += 1.0f;
                }
            }
        }
    }

    public static void test2() {
        float[] fla = new float[1000];
        for (int i = 0; i < 1000; i++) {
            for (float fl2 : fla) {
                fla[100] = 1.0f;
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 14; j++) {
                fla[2] = fla[j];
            }
        }
    }
}
