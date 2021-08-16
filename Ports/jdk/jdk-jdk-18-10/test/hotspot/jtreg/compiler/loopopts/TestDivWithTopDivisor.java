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
 * @bug 8260284
 * @summary Fix "assert(_base == Int) failed: Not an Int" due to a top divisor not handled correctly in no_dependent_zero_check().
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler/loopopts/TestDivWithTopDivisor compiler.loopopts.TestDivWithTopDivisor
 */

package compiler.loopopts;

public class TestDivWithTopDivisor {
    static boolean bFld;

    static int test(int d, long e, long f) {
        float g = 1;
        int a, b = 4, c = 4;
        int iArr[] = new int[400];
        init(iArr, 8);
        if (bFld) {
        } else if (bFld) {
            if (bFld) {
                if (bFld) {
                    for (a = 7; a > 1; --a) {
                        if (bFld) {
                            try {
                                c = b / a;
                                b = 9 / a;
                            } catch (ArithmeticException k) {
                            }
                        }
                    }
                }
                g = 0;
            }
        } else {
            iArr[d] <<= b;
        }
        long l = f + c + checkSum(iArr);
        return (int) l;
    }

    public static void init(int[] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }

    public static long checkSum(int[] a) {
        long sum = 0;
        for (int j = 0; j < a.length; j++) {
            sum += (a[j] / (j + 1) + a[j] % (j + 1));
        }
        return sum;
    }

    public static void main(String[] s) {
        for (int i = 0; i < 10; i++) {
            test(3, 0, 0);
        }
    }
}

