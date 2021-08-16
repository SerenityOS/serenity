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
 * @summary converted from VM Testbase jit/t/t080.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t080.t080
 */

package jit.t.t080;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Like t079.java except this one has lots of local variables.

public class t080
{
    public static final GoldChecker goldChecker = new GoldChecker( "t080" );

    public static void main(String[] argv)
    {
        double d[] = new double[2];
        double e[] = null;
        double x = 39.0, y = 42.0;
        double z;
        int i = 0, j = 1;
        int r;

        // The following is just bulk.  It's designed to get the offset
        // of the (used) locals outside the reach of a signed byte.
        int i00; i00 = 0;
        int i01; i01 = 1;
        int i02; i02 = 2;
        int i03; i03 = 3;
        int i04; i04 = 4;
        int i05; i05 = 5;
        int i06; i06 = 6;
        int i07; i07 = 7;
        int i08; i08 = 8;
        int i09; i09 = 9;

        int i10; i10 = 0;
        int i11; i11 = 1;
        int i12; i12 = 2;
        int i13; i13 = 3;
        int i14; i14 = 4;
        int i15; i15 = 5;
        int i16; i16 = 6;
        int i17; i17 = 7;
        int i18; i18 = 8;
        int i19; i19 = 9;

        int i20; i20 = 0;
        int i21; i21 = 1;
        int i22; i22 = 2;
        int i23; i23 = 3;
        int i24; i24 = 4;
        int i25; i25 = 5;
        int i26; i26 = 6;
        int i27; i27 = 7;
        int i28; i28 = 8;
        int i29; i29 = 9;

        int i30; i30 = 0;
        int i31; i31 = 1;
        int i32; i32 = 2;
        int i33; i33 = 3;
        int i34; i34 = 4;
        int i35; i35 = 5;
        int i36; i36 = 6;
        int i37; i37 = 7;
        int i38; i38 = 8;
        int i39; i39 = 9;

        (e = d)[r = i + j] = (z = x + y);
        t080.goldChecker.println(e[1]);
        t080.goldChecker.check();
    }
}
