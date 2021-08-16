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
 * @bug 8269575
 * @summary C2: assert(false) failed: graph should be schedulable after JDK-8252372
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestSunkNodeDueToBrokenAntiDependency TestSunkNodeDueToBrokenAntiDependency
 *
 */

public class TestSunkNodeDueToBrokenAntiDependency {

    public static final int N = 400;

    public static volatile long instanceCount=-154L;
    public volatile int iArrFld[]=new int[N];

    public void mainTest() {

        int i8=8, i9=-3, i10=-199, i11=13, i12=8, i13=2;
        long lArr1[]=new long[N];

        for (int i7 : iArrFld) {
            for (i8 = 1; i8 < 63; ++i8) {
                i10 = 1;
                while (++i10 < 2) {
                    i7 += (int)instanceCount;
                    lArr1[i10 + 1] -= 3;
                }
                i11 = 2;
                do {
                    byte by2=-104;
                    by2 = (byte)instanceCount;
                } while (--i11 > 0);
                i9 <<= 6;
                for (i12 = i8; 2 > i12; i12++) {
                    switch (((i11 >>> 1) % 1) + 66) {
                    case 66:
                        instanceCount -= i13;
                        break;
                    }

                }
            }
        }
    }
    public static void main(String[] strArr) {
        TestSunkNodeDueToBrokenAntiDependency _instance = new TestSunkNodeDueToBrokenAntiDependency();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest();
        }
    }
}
