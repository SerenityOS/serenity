/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/t/t078.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t078.t078
 */

package jit.t.t078;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t078{
    public static final GoldChecker goldChecker = new GoldChecker( "t078" );

    static double i0 = 0.0;
    static double i1 = 1.0;
    static double i2 = 2.0;
    static double i3 = 3.0;
    static double i4 = 4.0;
    static double i5 = 5.0;
    static double i6 = 6.0;
    static double i7 = 7.0;
    static double i8 = 8.0;
    static double i9 = 9.0;
    static double i10 = 10.0;
    static double i11 = 11.0;
    static double i12 = 12.0;
    static double i13 = 13.0;
    static double i14 = 14.0;
    static double i15 = 15.0;
    static double i16 = 16.0;
    static double i17 = 17.0;
    static double i18 = 18.0;
    static double i19 = 19.0;
    static double i20 = 20.0;
    static double i21 = 21.0;
    static double i22 = 22.0;
    static double i23 = 23.0;
    static double i24 = 24.0;
    static double i25 = 25.0;
    static double i26 = 26.0;
    static double i27 = 27.0;
    static double i28 = 28.0;
    static double i29 = 29.0;
    static double i30 = 30.0;
    static double i31 = 31.0;

    public static void main(String[] argv){
        double a;
        a =
        (((((i0 + i1) + (i2 + i3)) + ((i4 + i5) + (i6 + i7))) +
        (((i8 + i9) + (i10 + i11)) + ((i12 + i13) + (i14 + i15)))) +
        ((((i16 + i17) + (i18 + i19)) + ((i20 + i21) + (i22 + i23))) +
        (((i24 + i25) + (i26 + i27)) + ((i28 + i29) + (i30 + i31)))));
        t078.goldChecker.println(a);
        t078.goldChecker.check();
    }
}
