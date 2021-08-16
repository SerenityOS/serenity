/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8254887
 * @summary C2: assert(cl->trip_count() > 0) failed: peeling a fully unrolled loop
 *
 * @run main/othervm -Xbatch TestPeelingNeverEnteredLoop
 *
 */

public class TestPeelingNeverEnteredLoop {

    public static final int N = 400;

    public static byte byFld=83;

    public static void lMeth() {

        int iArr1[][]=new int[N][N];
        byte byArr[][]=new byte[N][N];

        int i10 = 1;
        do {
            int i11 = 1;
            do {
                iArr1[i10 - 1][i11] = TestPeelingNeverEnteredLoop.byFld;
                byArr[i10][i11] -= (byte)-20046;
                for (int i12 = 1; 1 > i12; ++i12) {
                }
            } while (++i11 < 8);
        } while (++i10 < 212);
    }

    public static void main(String[] strArr) {
        TestPeelingNeverEnteredLoop _instance = new TestPeelingNeverEnteredLoop();
        for (int i = 0; i < 1500; i++ ) {
            _instance.lMeth();
        }
    }
}
