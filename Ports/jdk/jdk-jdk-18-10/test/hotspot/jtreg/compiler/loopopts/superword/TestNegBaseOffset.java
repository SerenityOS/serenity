/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202948
 * @summary Test skipping vector packs with negative base offset.
 * @comment Test fails only with -Xcomp when profiling data is not present.
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockExperimentalVMOptions
 *                   -Xcomp -XX:-TieredCompilation -XX:CICompilerCount=1
 *                   -XX:CompileOnly=compiler/loopopts/superword/TestNegBaseOffset
 *                   compiler.loopopts.superword.TestNegBaseOffset
 */

package compiler.loopopts.superword;

public class TestNegBaseOffset {
    public static final int N = 400;
    public static int iFld=10;
    public static int iArr[]=new int[N];

    public static void mainTest() {
        int i0=1, i2;
        while (++i0 < 339) {
            if ((i0 % 2) == 0) {
                for (i2 = 2; i2 > i0; i2 -= 3) {
                    iArr[i2 - 1] &= iFld;
                }
            }
        }
    }

    public static void main(String[] strArr) {
        for (int i = 0; i < 10; i++) {
            mainTest();
        }
    }
}

