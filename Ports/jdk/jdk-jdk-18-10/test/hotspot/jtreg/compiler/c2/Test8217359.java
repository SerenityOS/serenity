/*
 * Copyright (c) 2019, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8217359
 * @summary C2 compiler triggers SIGSEGV after transformation in ConvI2LNode::Ideal
 *
 * @run main/othervm -XX:-TieredCompilation
 *      -XX:CompileCommand=compileonly,compiler.c2.Test8217359::test
 *      compiler.c2.Test8217359
 */

package compiler.c2;

public class Test8217359 {

    public static int ival = 0;
    public static long lsum = 0;
    public static long lval = 0;

    public static void test() {
        short s = -25632;
        float f = 0.512F, f1 = 2.556F;
        int i6 = 32547, i7 = 9, i8 = -9, i9 = 36, i10 = -223;

        for (i6 = 4; i6 < 182; i6++) {
            i8 = 1;
            while (++i8 < 17) {
                f1 = 1;
                do {
                    i7 += (182 + (f1 * f1));
                } while (++f1 < 1);

                Test8217359.ival += (i8 | Test8217359.ival);
            }
        }

        for (i9 = 9; i9 < 100; ++i9) {
            i10 -= i6;
            i10 >>= s;
            i7 += (((i9 * i10) + i6) - Test8217359.lval);
        }

        lsum += i6 + i7 + i8;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 16000; i++) {
            test();
        }
    }

}
