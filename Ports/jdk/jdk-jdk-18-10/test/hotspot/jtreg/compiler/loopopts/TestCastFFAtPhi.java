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
 * @bug 8268017
 * @summary C2: assert(phi_type->isa_int() || phi_type->isa_ptr() || phi_type->isa_long()) failed: bad phi type
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestCastFFAtPhi TestCastFFAtPhi
 *
 */

public class TestCastFFAtPhi {
    static int N = 400;
    static double dArrFld[] = new double[N];
    static long iMeth_check_sum = 0;

    static {
        init(dArrFld, 90.71133);
    }

    float fArrFld[] = new float[N];

    public static void main(String[] strArr) {
        TestCastFFAtPhi _instance = new TestCastFFAtPhi();
        for (int i = 0; i < 10; i++) {
            _instance.mainTest();
        }
    }

    void mainTest() {
        int i24 = 121110, i28, i30;
        float f2 = 2.486F;

        for (i28 = 322; i28 > 6; i28--) {
            i30 = 1;
            do {
                i24 = (int) f2;
                fArrFld[1] += i30;
                switch (((i28 % 4) * 5) + 32) {
                    case 36:
                        f2 *= f2;
                }
            } while (++i30 < 80);
        }
        System.out.println(i24 + ",");
    }

    public static void init(double[] a, double seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }
}
