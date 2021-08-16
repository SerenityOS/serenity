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
 * @summary converted from VM Testbase jit/t/t046.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t046.t046
 */

package jit.t.t046;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Register jams and spills

public class t046
{
    public static final GoldChecker goldChecker = new GoldChecker( "t046" );

    static void intSubs()
    {
        int a, b, c, d, e, f, g, h, i, j, k, l, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        z = (a-b) - ((c-d) - ((e-f) - ((g-h) - ((i-j) - (k-l)))));
        t046.goldChecker.println("Int: " + z);
    }

    static void longSubs()
    {
        long a, b, c, d, e, f, g, h, i, j, k, l, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        z = (a-b) - ((c-d) - ((e-f) - ((g-h) - ((i-j) - (k-l)))));
        t046.goldChecker.println("Long: " + z);
    }

    static void floatSubs()
    {
        float a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        m=13; n=14; o=15; p=16; q=17; r=18; s=19; t=20;
        z = (a-b) - ((c-d) - ((e-f) - ((g-h) - ((i-j) - ((k-l) - ((m-n) -
            ((o-p) - ((q-r) - (s-t)))))))));
        t046.goldChecker.println("Float: " + z);
    }

    static void doubleSubs()
    {
        double a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        m=13; n=14; o=15; p=16; q=17; r=18; s=19; t=20;
        z = (a-b) - ((c-d) - ((e-f) - ((g-h) - ((i-j) - ((k-l) - ((m-n) -
            ((o-p) - ((q-r) - (s-t)))))))));
        t046.goldChecker.println("Double: " + z);
    }

    public static void main(String argv[])
    {
        intSubs();
        longSubs();
        floatSubs();
        doubleSubs();
        t046.goldChecker.check();
    }
}
