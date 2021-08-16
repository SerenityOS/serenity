/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8229701
 * @summary C2 OSR compilation fails with "shouldn't process one node several times" in final graph reshaping
 *
 * @run main/othervm ConvI2LWideningAssertTooStrong
 *
 */

public class ConvI2LWideningAssertTooStrong {

    public static final int N = 400;

    public static long instanceCount=708L;
    public static volatile int iFld1=30517;
    public static int iArrFld[]=new int[N];

    public static void vMeth(short s) {
        int i9=29117, i11=-6;

        for (i9 = 11; i9 < 377; i9++) {
            switch ((i9 % 8) + 22) {
            case 24:
                instanceCount = i9;
                instanceCount += instanceCount;
                break;
            case 25:
                try {
                    i11 = (20705 % i11);
                    iArrFld[i9 - 1] = (55094 / iFld1);
                } catch (ArithmeticException a_e) {}
                break;
            default:
            }
        }
    }

    public static void main(String[] strArr) {
        ConvI2LWideningAssertTooStrong _instance = new ConvI2LWideningAssertTooStrong();
        for (int i = 0; i < 10 * 202 * 8; i++ ) {
            _instance.vMeth((short)20806);
        }
    }
}
