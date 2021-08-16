/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015396
 * @summary double a%b returns NaN for some (a,b) (|a| < inf, |b|>0) (on Core i7 980X)
 *
 * @run main compiler.floatingpoint.ModNaN
 */

package compiler.floatingpoint;

public class ModNaN {
    /* This bug was seen in the field for a particular version of the VM,
     * but never reproduced internally, and the reason was never known,
     * nor were the exact circumstances of the failure.
     */
    /*
     * Failed on Windows 7/Core i7 980X/1.6.0_38 (64-bit):
     *
     * NaNs (i.e., when it fails, this is what we expect to see):
     *
     * 8.98846567431158E307 % 1.295163E-318 = NaN
     * (0x7FE0000000000000L % 0x0000000000040000L)
     *
     * 1.7976931348623157E308 % 2.59032E-318 = NaN
     * (0x7FEFFFFFFFFFFFFFL % 0x000000000007FFFFL)
     *
     * 1.7976931348623157E308 % 1.060997895E-314 = NaN
     * (0x7FEFFFFFFFFFFFFFL % 0x000000007FFFFFFFL)
     *
     * 1.7976931348623157E308 % 6.767486E-317 = NaN
     * (0x7FEFFFFFFFFFFFFFL % 0x0000000000d10208L)
     *
     * 1.7976931348623157E308 % 7.528725E-318 = NaN
     * (0x7FEFFFFFFFFFFFFFL % 0x0000000000174077L)
     *
     * These cases did not fail, even when the previous five did:
     * 8.98846567431158E307 % 1.29516E-318 = 2.53E-321
     * (0x7fe0000000000000L % 0x000000000003ffffL)
     *
     * 1.7976931348623157E308 % 2.590327E-318 = 0.0
     * (0x7fefffffffffffffL % 0x0000000000080000L)
     *
     * 1.7976931348623157E308 % 1.060965516E-314 = 9.35818525E-315
     * (0x7fefffffffffffffL % 0x000000007ffeffffL)
     *
     */

    static double[][] bad = new double[][] {
            /*
             * These hex numbers correspond to the base-10 doubles in the
             * comment above; this can be checked by observing the output
             * of testWithPrint.
             */
            new double[] { Double.longBitsToDouble(0x7FE0000000000000L),
                    Double.longBitsToDouble(0x0000000000040000L) },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    Double.longBitsToDouble(0x000000000007FFFFL) },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    Double.longBitsToDouble(0x000000007FFFFFFFL) },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    6.767486E-317 },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    7.528725E-318 }, };

    static double[][] good = new double[][] {
            new double[] { Double.longBitsToDouble(0x7FE0000000000000L),
                    Double.longBitsToDouble(0x000000000003FFFFL) },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    Double.longBitsToDouble(0x0000000000080000L) },
            new double[] { Double.longBitsToDouble(0x7FEFFFFFFFFFFFFFL),
                    Double.longBitsToDouble(0x000000007FFEFFFFL) }, };

    public static void main(String[] args) throws InterruptedException {
        int N = 10000;
        testWithPrint();
        for (int i = 0; i < N; i++)
            testStrict();
        for (int i = 0; i < N; i++)
            test();
        Thread.sleep(1000); // pause to let the compiler work
        for (int i = 0; i < 10; i++)
            testStrict();
        for (int i = 0; i < 10; i++)
            test();
    }

    public strictfp static void testWithPrint() {
        for (double[] ab : bad) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            System.out.println("" + a + "("+toHexRep(a)+") mod " +
                                    b + "("+toHexRep(b)+") yields " +
                                    mod + "("+toHexRep(mod)+")");
        }

        for (double[] ab : good) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            System.out.println("" + a + "("+toHexRep(a)+") mod " +
                                    b + "("+toHexRep(b)+") yields " +
                                    mod + "("+toHexRep(mod)+")");
        }
    }

    public strictfp static void testStrict() {
        for (double[] ab : bad) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            check(mod);
        }

        for (double[] ab : good) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            check(mod);
        }
    }

    public static void test() {
        for (double[] ab : bad) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            check(mod);
        }

        for (double[] ab : good) {
            double a = ab[0];
            double b = ab[1];
            double mod = a % b;
            check(mod);
        }
    }

    static String toHexRep(double d) {
        return "0x" + Long.toHexString(Double.doubleToRawLongBits(d)) + "L";
    }

    static void check(double mod) {
        if (Double.isNaN(mod)) {
            throw new Error("Saw a NaN, fail");
        }
    }
}
