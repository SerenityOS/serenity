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
 * @summary converted from VM Testbase jit/t/t077.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t077.t077
 */

package jit.t.t077;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t077{
    public static final GoldChecker goldChecker = new GoldChecker( "t077" );

    static float i0 = 0.0f;
    static float i1 = 1.0f;
    static float i2 = 2.0f;
    static float i3 = 3.0f;
    static float i4 = 4.0f;
    static float i5 = 5.0f;
    static float i6 = 6.0f;
    static float i7 = 7.0f;
    static float i8 = 8.0f;
    static float i9 = 9.0f;
    static float i10 = 10.0f;
    static float i11 = 11.0f;
    static float i12 = 12.0f;
    static float i13 = 13.0f;
    static float i14 = 14.0f;
    static float i15 = 15.0f;
    static float i16 = 16.0f;
    static float i17 = 17.0f;
    static float i18 = 18.0f;
    static float i19 = 19.0f;
    static float i20 = 20.0f;
    static float i21 = 21.0f;
    static float i22 = 22.0f;
    static float i23 = 23.0f;
    static float i24 = 24.0f;
    static float i25 = 25.0f;
    static float i26 = 26.0f;
    static float i27 = 27.0f;
    static float i28 = 28.0f;
    static float i29 = 29.0f;
    static float i30 = 30.0f;
    static float i31 = 31.0f;

    public static void main(String[] argv){
        float a;
        a =
        (((((i0 + i1) + (i2 + i3)) + ((i4 + i5) + (i6 + i7))) +
        (((i8 + i9) + (i10 + i11)) + ((i12 + i13) + (i14 + i15)))) +
        ((((i16 + i17) + (i18 + i19)) + ((i20 + i21) + (i22 + i23))) +
        (((i24 + i25) + (i26 + i27)) + ((i28 + i29) + (i30 + i31)))));
        t077.goldChecker.println(a);
        t077.goldChecker.check();
    }
}
