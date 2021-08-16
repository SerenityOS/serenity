/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8263971
 * @summary C2 crashes with SIGFPE with -XX:+StressGCM and -XX:+StressIGVN
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestLostDependencyOnZeroTripGuard -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM -XX:StressSeed=886771365 TestLostDependencyOnZeroTripGuard
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestLostDependencyOnZeroTripGuard -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM TestLostDependencyOnZeroTripGuard
 *
 */

public class TestLostDependencyOnZeroTripGuard {

    public static final int N = 400;

    public int iArrFld[]=new int[N];

    public void mainTest(String[] strArr1) {

        int i=57657, i1=577, i2=6, i3=157, i4=12, i23=61271;
        boolean bArr[]=new boolean[N];

        for (i = 9; 379 > i; i++) {
            i2 = 1;
            do {
                i1 <<= i3;
            } while (++i2 < 68);
            for (i23 = 68; i23 > 3; i23--) {
                bArr[i23 + 1] = true;
                try {
                    i1 = (-42360 / i23);
                    iArrFld[i + 1] = (i4 % 15384);
                } catch (ArithmeticException a_e) {}
            }
        }
    }

    public static void main(String[] strArr) {
        TestLostDependencyOnZeroTripGuard _instance = new TestLostDependencyOnZeroTripGuard();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest(strArr);
        }
    }
}
