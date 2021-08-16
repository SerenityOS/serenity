/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196199
 * @summary java/text/Bidi/Bug6665028.java failed: Bidi run count incorrect
 *
 * @run main/othervm/timeout=400 -Xmx128m -Xbatch -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation
 *      -XX:+SafepointALot -XX:GuaranteedSafepointInterval=100
 *      -XX:CompileCommand=exclude,compiler.runtime.Test7196199::test
 *      compiler.runtime.Test7196199
 */

package compiler.runtime;

public class Test7196199 {
    private static final int ARRLEN = 97;
    private static final int ITERS = 5000;
    private static final int INI_ITERS = 1000;
    private static final int SFP_ITERS = 10000;
    private static final float SFP_ITERS_F = 10000.f;
    private static final float VALUE = 15.f;

    public static void main(String args[]) {
        int errn = test();
        if (errn > 0) {
            System.err.println("FAILED: " + errn + " errors");
            System.exit(97);
        }
        System.out.println("PASSED");
    }

    static int test() {
        float[] a0 = new float[ARRLEN];
        float[] a1 = new float[ARRLEN];
        // Initialize
        for (int i = 0; i < ARRLEN; i++) {
            a0[i] = 0.f;
            a1[i] = (float) i;
        }
        System.out.println("Warmup");
        for (int i = 0; i < INI_ITERS; i++) {
            test_incrc(a0);
            test_incrv(a0, VALUE);
            test_addc(a0, a1);
            test_addv(a0, a1, VALUE);
        }
        // Test and verify results
        System.out.println("Verification");
        int errn = 0;
        for (int i = 0; i < ARRLEN; i++)
            a0[i] = 0.f;

        System.out.println("  test_incrc");
        for (int j = 0; j < ITERS; j++) {
            test_incrc(a0);
            for (int i = 0; i < ARRLEN; i++) {
                errn += verify("test_incrc: ", i, a0[i], VALUE * SFP_ITERS_F);
                a0[i] = 0.f; // Reset
            }
        }

        System.out.println("  test_incrv");
        for (int j = 0; j < ITERS; j++) {
            test_incrv(a0, VALUE);
            for (int i = 0; i < ARRLEN; i++) {
                errn += verify("test_incrv: ", i, a0[i], VALUE * SFP_ITERS_F);
                a0[i] = 0.f; // Reset
            }
        }

        System.out.println("  test_addc");
        for (int j = 0; j < ITERS; j++) {
            test_addc(a0, a1);
            for (int i = 0; i < ARRLEN; i++) {
                errn += verify("test_addc: ", i, a0[i], ((float) i + VALUE) * SFP_ITERS_F);
                a0[i] = 0.f; // Reset
            }
        }

        System.out.println("  test_addv");
        for (int j = 0; j < ITERS; j++) {
            test_addv(a0, a1, VALUE);
            for (int i = 0; i < ARRLEN; i++) {
                errn += verify("test_addv: ", i, a0[i], ((float) i + VALUE) * SFP_ITERS_F);
                a0[i] = 0.f; // Reset
            }
        }

        if (errn > 0)
            return errn;

        System.out.println("Time");
        long start, end;

        start = System.currentTimeMillis();
        for (int i = 0; i < INI_ITERS; i++) {
            test_incrc(a0);
        }
        end = System.currentTimeMillis();
        System.out.println("test_incrc: " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < INI_ITERS; i++) {
            test_incrv(a0, VALUE);
        }
        end = System.currentTimeMillis();
        System.out.println("test_incrv: " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < INI_ITERS; i++) {
            test_addc(a0, a1);
        }
        end = System.currentTimeMillis();
        System.out.println("test_addc: " + (end - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < INI_ITERS; i++) {
            test_addv(a0, a1, VALUE);
        }
        end = System.currentTimeMillis();
        System.out.println("test_addv: " + (end - start));

        return errn;
    }

    static void test_incrc(float[] a0) {
        // Non-counted loop with safepoint.
        for (long l = 0; l < SFP_ITERS; l++) {
            // Counted and vectorized loop.
            for (int i = 0; i < a0.length; i += 1) {
                a0[i] += VALUE;
            }
        }
    }

    static void test_incrv(float[] a0, float b) {
        // Non-counted loop with safepoint.
        for (long l = 0; l < SFP_ITERS; l++) {
            // Counted and vectorized loop.
            for (int i = 0; i < a0.length; i += 1) {
                a0[i] += b;
            }
        }
    }

    static void test_addc(float[] a0, float[] a1) {
        // Non-counted loop with safepoint.
        for (long l = 0; l < SFP_ITERS; l++) {
            // Counted and vectorized loop.
            for (int i = 0; i < a0.length; i += 1) {
                a0[i] += a1[i] + VALUE;
            }
        }
    }

    static void test_addv(float[] a0, float[] a1, float b) {
        // Non-counted loop with safepoint.
        for (long l = 0; l < SFP_ITERS; l++) {
            // Counted and vectorized loop.
            for (int i = 0; i < a0.length; i += 1) {
                a0[i] += a1[i] + b;
            }
        }
    }

    static int verify(String text, int i, float elem, float val) {
        if (elem != val) {
            System.err.println(text + "[" + i + "] = " + elem + " != " + val);
            return 1;
        }
        return 0;
    }
}
