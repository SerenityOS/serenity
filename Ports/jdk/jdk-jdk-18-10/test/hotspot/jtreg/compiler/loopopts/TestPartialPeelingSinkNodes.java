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
 *
 */

/*
 * @test
 * @bug 8256934
 * @summary Sinking of nodes in partial peeling creates too many clones resulting in a live node limit exceeded assertion failure.
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,compiler.loopopts.TestPartialPeelingSinkNodes::test
 *                   compiler.loopopts.TestPartialPeelingSinkNodes
 */

package compiler.loopopts;

public class TestPartialPeelingSinkNodes {
    static int i5 = 168, iFld = 2, x, y;
    static boolean b = false, b2 = false;

    public static void main(String[] strArr) {
        test();
    }

    // The algorithm in partial peeling creates ~90000 nodes for this method which triggers the assertion failure.
    public static void test() {
        for (int i = 0; i < 2480; i++) {
            int i2 = -37052, i3 = 39651, i4 = -37052;
            int i5 = 168, i6 = -133, i7 = 1, i8 = -10;
            double d = -20.82293;

            float fArr[] = new float[400];
            for (int j = 0; j < 400; j++) {
                fArr[j] = (j % 2 == 0) ? 0.300F + j : 0.300F - j;
            }

            while (--i5 > 0) {
                i6 = 1;
                do {
                    i4 += (((i6 * i2) + i3) - i3);
                    i2 += i4;
                } while (++i6 < 9);
                i3 -= i4;
                for (i7 = 1; i7 < 18; i7++) {
                    i4 = i5;
                    d -= i4;
                    i2 -= i8;
                    i2 = i8;
                }
            }
        }
    }
}

