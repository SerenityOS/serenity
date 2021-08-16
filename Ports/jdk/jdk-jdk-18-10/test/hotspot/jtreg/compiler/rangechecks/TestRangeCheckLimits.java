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
 * @bug 8262017
 * @summary Dominator failure because ConvL2I node becomes TOP due to missing overflow/underflow handling in range check elimination
 *          in PhaseIdealLoop::add_constraint().
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,compiler.rangechecks.TestRangeCheckLimits::*
 *                   compiler.rangechecks.TestRangeCheckLimits
 */

 package compiler.rangechecks;

 public class TestRangeCheckLimits {
    static int a = 400;
    static volatile int b;
    static long lFld;
    static int iFld;

    public static void main(String[] k) {
        // Test all cases in PhaseIdealLoop::add_constraint().
        testPositiveCaseMainLoop();
        testNegativeCaseMainLoop();
        testPositiveCasePreLoop();
        testNegativeCasePreLoop();
    }

    public static void testPositiveCaseMainLoop() {
        int e, f, g = 0, h[] = new int[a];
        double i[] = new double[a];
        long j = 9;
        Helper.init(h, 3);
        for (e = 5; e < 154; e++) {
            for (f = 1; f < 169; f += 2) {
                b = e;
            }
            i[1] = b;
            for (g = 8; g < 168; g += 2) {
                j = g - 5;
                if (j > Integer.MAX_VALUE - 1) {
                    switch (3) {
                        case 3:
                    }
                }
            }
        }
        if (g != 168) {
            throw new RuntimeException("fail");
        }
        lFld = j;
    }


    public static void testPositiveCasePreLoop() {
        int e, f, g = 0, h[] = new int[a];
        double i[] = new double[a];
        long j = 9;
        Helper.init(h, 3);
        for (e = 5; e < 154; e++) {
            for (f = 1; f < 169; f += 2) {
                b = e;
            }
            i[1] = b;
            for (g = 8; g < 168; g += 2) {
                j = g + 5;
                if (j > 180) {
                    switch (3) {
                        case 3:
                    }
                }
            }
        }
        if (g != 168) {
            throw new RuntimeException("fail");
        }
        lFld = j;
    }

    public static void testNegativeCaseMainLoop() {
        int e, f, g = 0, h[] = new int[a];
        double i[] = new double[a];
        long j = 9;
        Helper.init(h, 3);
        for (e = 5; e < 154; e++) {
            for (f = 1; f < 169; f += 2) {
                b = e;
            }
            i[1] = b;
            for (g = 8; g < 168; g += 2) {
                j = g;
                if (j < 5) {
                    switch (3) {
                        case 3:
                    }
                }
            }
        }
        if (g != 168) {
            throw new RuntimeException("fail");
        }
        lFld = j;
    }


    public static void testNegativeCasePreLoop() {
        int e, f, g = 0, h[] = new int[a];
        double i[] = new double[a];
        long j = 9;
        Helper.init(h, 3);
        for (e = 5; e < 154; e++) {
            for (f = 1; f < 169; f += 2) {
                b = e;
            }
            i[1] = b;
            for (g = 168; g > 8; g -= 2) {
                j = g - 5;
                if (j > Integer.MAX_VALUE - 1) {
                    switch (3) {
                        case 3:
                    }
                }
            }
        }
        if (g != 8) {
            throw new RuntimeException("fail");
        }
        lFld = j;
    }
}

class Helper {
    public static void init(int[] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }
}
