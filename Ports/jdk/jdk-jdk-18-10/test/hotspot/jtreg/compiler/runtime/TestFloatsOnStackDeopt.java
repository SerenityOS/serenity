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

package compiler.runtime;

/*
 * @test
 * @summary testing deoptimization on safepoint with floating point values on stack
 * @bug 8202710
 * @run main/othervm -XX:+DeoptimizeALot
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                   compiler.runtime.TestFloatsOnStackDeopt
 */

public class TestFloatsOnStackDeopt {

    private static final int ARRLEN = 97;
    private static final int ITERS1 = 100;
    private static final int ITERS2 = 40000;
    private static final float VALUE = 15.f;
    public static String dummyString = "long long string";
    static volatile boolean pleaseStop = false;

    static void run_loop_with_safepoint(float[] a0, float b) {
        // Non-counted loop with safepoint.
        for (long l = 0; l < ITERS2; l++) {
            // Counted and vectorized loop.
            for (int i = 0; i < a0.length; i += 1) {
                a0[i] += b;
            }
        }
    }

    static int test() {
        // thread provokes frequent GC - together with +DeoptimizeALot and safepoint it forces executed function deoptimization
        Thread th = new Thread() {
            public void run() {
                while (!pleaseStop) {
                    synchronized(this) { try { wait(1); } catch (Exception ex) {} }
                    dummyString = new StringBuilder(dummyString).append(dummyString).toString();
                    if (dummyString.length() > 1024*1024) { dummyString = "long long string"; }
                }
            }
        };
        th.start();

        int errn = 0;
        for (int j = 0; j < ITERS1; j++) {
            float[] x0 = new float[ARRLEN];
            run_loop_with_safepoint(x0, VALUE);
            for (int i = 0; i < ARRLEN; i++) {
                if (x0[i] != VALUE * ITERS2) {
                    System.err.println("(" + j + "): " + "x0[" + i + "] = " + x0[i] + " != " + VALUE * ITERS2);
                    errn++;
                }
                x0[i] = 0.f; // Reset
            }
            if (errn > 0) break;
        }

        pleaseStop = true;
        try {
            th.join();
        } catch (InterruptedException e) {
            throw new Error("InterruptedException in main thread ", e);
        }
        return errn;
    }

    public static void main(String args[]) {
        int errn = test();
        System.err.println((errn > 0) ? "FAILED" : "PASSED");
    }
}

