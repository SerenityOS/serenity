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
 * @summary converted from VM Testbase jit/t/t048.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t048.t048
 */

package jit.t.t048;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Register jams and spills

public class t048
{
    public static final GoldChecker goldChecker = new GoldChecker( "t048" );

    static int idiv(int i, int j)
    {
        int res = i / j;
        t048.goldChecker.println("i: " + i + ", j: " + j + ", res: " + res);
        return res;
    }

    static void intDivs()
    {
        int a, b, c, d, e, f, g, h, i, j, k, l, z;
        int t1, t2, t3, t4, t5, t6, t7;

        a=13; b=12; c=11; d=10; e=9; f=8; g=7; h=6; i=5; j=4; k=3; l=2;

        t1 = a/b;
        t2 = c/d;
        t3 = e/f;
        t4 = g/h;
        t5 = i/j;
        t6 = k/l;
        t048.goldChecker.println("t6 = " + t6);
        t048.goldChecker.println("t5 = " + t5);
        t5 /= t6;
        t048.goldChecker.println("t5 = " + t5);
        t048.goldChecker.println("t4 = " + t4);
        t4 /= t5;
        t3 /= t4;
        t2 /= t3;
        t1 /= t2;

        t048.goldChecker.println("t1 == " + t1);
        z = idiv
        (
            idiv(a,b),
            idiv
            (
                idiv(c,d),
                idiv
                (
                    idiv(e,f),
                    idiv
                    (
                        idiv(g,h),
                        idiv
                        (
                            idiv(i,j),
                            idiv(k, l)
                        )
                    )
                )
            )
        );
        z = (a/b) /
            ((c/d) /
            ((e/f) /
            ((g/h) /
            ((i/j) / (k/l)))));
        t048.goldChecker.println("Int: " + z);
    }

    static void longDivs()
    {
        long a, b, c, d, e, f, g, h, i, j, k, l, z;

        a=13; b=12; c=11; d=10; e=9; f=8; g=7; h=6; i=5; j=4; k=3; l=2;

        z = (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / (k/l)))));
        t048.goldChecker.println("Long: " + z);
    }

    static void floatDivs()
    {
        float a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        m=13; n=14; o=15; p=16; q=17; r=18; s=19; t=20;
        z = (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
        z /= (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Float: " + z);
    }

    static void doubleDivs()
    {
        double a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, z;

        a=1; b=2; c=3; d=4; e=5; f=6; g=7; h=8; i=9; j=10; k=11; l=12;
        m=13; n=14; o=15; p=16; q=17; r=18; s=19; t=20;
        z = (a/b) / ((c/d) / ((e/f) / ((g/h) / ((i/j) / ((k/l) / ((m/n) /
            ((o/p) / ((q/r) / (s/t)))))))));
        t048.goldChecker.println("Double: " + z);
    }

    public static void main(String argv[])
    {
        intDivs();
        longDivs();
        floatDivs();
        doubleDivs();
        t048.goldChecker.check();
    }
}
