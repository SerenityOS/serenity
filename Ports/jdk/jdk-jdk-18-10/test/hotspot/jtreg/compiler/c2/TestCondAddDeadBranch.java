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
 * @bug 8268883
 * @summary C2: assert(false) failed: unscheduable graph
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestCondAddDeadBranch TestCondAddDeadBranch
 *
 */

public class TestCondAddDeadBranch {

    public static final int N = 400;

    public static long instanceCount=47540L;
    public int iFld=-41658;
    public static float fFld=95.926F;
    public static double dFld=0.15667;
    public static int iFld1=4;
    public static short sFld=5587;
    public static short sArrFld[]=new short[N];

    static {
        init(TestCondAddDeadBranch.sArrFld, (short)-26197);
    }

    public static long vSmallMeth_check_sum = 0;

    public static void vSmallMeth(long l, long l1, int i) {
        vSmallMeth_check_sum += l + l1 + i;
    }

    public static void vMeth(int i1, int i2) {
        int i3=-27774, i4=9, i5=2, i6=12, i7=0, i8=-4, i29=53186;
        long lArr[][]=new long[N][N];
    }

    public void mainTest(String[] strArr1) {

        short s=838;
        int i31=238, i32=-19630, i33=-1, i34=181, i35=155, i36=-8401, i37=-50, i38=-153, iArr[][]=new int[N][N];
        float f1=46.763F, fArr[]=new float[N];
        byte byArr[]=new byte[N];
        boolean bArr[]=new boolean[N];

        init(fArr, -59.7F);
        init(byArr, (byte)63);
        init(iArr, 39165);

        for (float f : fArr) {
            for (int smallinvoc=0; smallinvoc<62; smallinvoc++) vSmallMeth(TestCondAddDeadBranch.instanceCount = ((TestCondAddDeadBranch.instanceCount++)
                * 65430), ((iFld + iFld) * (iFld++)) + (iFld + (iFld + iFld)), -s);
        }
        TestCondAddDeadBranch.instanceCount |= -2906416119L;
        for (byte by : byArr) {
            vMeth(iFld, -8);
            iFld |= (int)TestCondAddDeadBranch.instanceCount;
            i31 = 1;
            do {
                TestCondAddDeadBranch.sFld -= (short)TestCondAddDeadBranch.iFld1;
            } while (++i31 < 63);
        }
        for (i32 = 7; i32 < 160; i32++) {
            for (i34 = 1; i34 < 164; i34 += 3) {
                try {
                    iFld = (105 / i33);
                    TestCondAddDeadBranch.iFld1 = (iArr[i34 + 1][i34 + 1] % iArr[i32 + 1][i34 - 1]);
                    TestCondAddDeadBranch.iFld1 = (254 / i32);
                } catch (ArithmeticException a_e) {}
            }
            i33 <<= i31;
        }
        switch ((((TestCondAddDeadBranch.iFld1 >>> 1) % 5) * 5) + 60) {
        case 84:
        case 82:
            TestCondAddDeadBranch.fFld += TestCondAddDeadBranch.instanceCount;
            i36 = 1;
            break;
        case 83:
            TestCondAddDeadBranch.dFld += i32;
            break;
        }

        System.out.println("s i31 i32 = " + s + "," + i31 + "," + i32);
        System.out.println("i33 i34 i35 = " + i33 + "," + i34 + "," + i35);
    }

    public static void main(String[] strArr) {
        TestCondAddDeadBranch _instance = new TestCondAddDeadBranch();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest(strArr);
        }
    }

    public static void init(short[] a, short seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (short) ((j % 2 == 0) ? seed + j : seed - j);
        }
    }

    public static void init(float[] a, float seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }

    public static void init(byte[] a, byte seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (byte) ((j % 2 == 0) ? seed + j : seed - j);
        }
    }

    public static void init(int[][] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            init(a[j], seed);
        }
    }

    public static void init(int[] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }
}
