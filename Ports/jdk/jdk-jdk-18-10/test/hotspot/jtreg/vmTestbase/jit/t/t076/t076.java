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
 * @summary converted from VM Testbase jit/t/t076.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t076.t076
 */

package jit.t.t076;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t076{
    public static final GoldChecker goldChecker = new GoldChecker( "t076" );

    static long i0 = 0;
    static long i1 = 1;
    static long i2 = 2;
    static long i3 = 3;
    static long i4 = 4;
    static long i5 = 5;
    static long i6 = 6;
    static long i7 = 7;
    static long i8 = 8;
    static long i9 = 9;
    static long i10 = 10;
    static long i11 = 11;
    static long i12 = 12;
    static long i13 = 13;
    static long i14 = 14;
    static long i15 = 15;
    static long i16 = 16;
    static long i17 = 17;
    static long i18 = 18;
    static long i19 = 19;
    static long i20 = 20;
    static long i21 = 21;
    static long i22 = 22;
    static long i23 = 23;
    static long i24 = 24;
    static long i25 = 25;
    static long i26 = 26;
    static long i27 = 27;
    static long i28 = 28;
    static long i29 = 29;
    static long i30 = 30;
    static long i31 = 31;

    public static void main(String[] argv){
        long a;
        a =
        (((((i0 + i1) + (i2 + i3)) + ((i4 + i5) + (i6 + i7))) +
        (((i8 + i9) + (i10 + i11)) + ((i12 + i13) + (i14 + i15)))) +
        ((((i16 + i17) + (i18 + i19)) + ((i20 + i21) + (i22 + i23))) +
        (((i24 + i25) + (i26 + i27)) + ((i28 + i29) + (i30 + i31)))));
        t076.goldChecker.println(a);
        t076.goldChecker.check();
    }
}
