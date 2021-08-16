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
 * @summary converted from VM Testbase jit/t/t075.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t075.t075
 */

package jit.t.t075;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t075{
    public static final GoldChecker goldChecker = new GoldChecker( "t075" );

    static int i0 = 0;
    static int i1 = 1;
    static int i2 = 2;
    static int i3 = 3;
    static int i4 = 4;
    static int i5 = 5;
    static int i6 = 6;
    static int i7 = 7;
    static int i8 = 8;
    static int i9 = 9;
    static int i10 = 10;
    static int i11 = 11;
    static int i12 = 12;
    static int i13 = 13;
    static int i14 = 14;
    static int i15 = 15;
    static int i16 = 16;
    static int i17 = 17;
    static int i18 = 18;
    static int i19 = 19;
    static int i20 = 20;
    static int i21 = 21;
    static int i22 = 22;
    static int i23 = 23;
    static int i24 = 24;
    static int i25 = 25;
    static int i26 = 26;
    static int i27 = 27;
    static int i28 = 28;
    static int i29 = 29;
    static int i30 = 30;
    static int i31 = 31;

    public static void main(String[] argv){
        int a;
        a =
        (((((i0 + i1) + (i2 + i3)) + ((i4 + i5) + (i6 + i7))) +
        (((i8 + i9) + (i10 + i11)) + ((i12 + i13) + (i14 + i15)))) +
        ((((i16 + i17) + (i18 + i19)) + ((i20 + i21) + (i22 + i23))) +
        (((i24 + i25) + (i26 + i27)) + ((i28 + i29) + (i30 + i31)))));
        t075.goldChecker.println(a);
        t075.goldChecker.check();
    }
}
