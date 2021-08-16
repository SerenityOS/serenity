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
 * @summary converted from VM Testbase jit/t/t058.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t058.t058
 */

package jit.t.t058;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

interface l
{
    void voodoo();
}

class k implements l
{
    double d;
    long l;
    int i;
    float f;
    short s;
    char c;
    byte b;

    static double sd;
    static long sl;
    static int si;
    static float sf;
    static short ss;
    static char sc;
    static byte sb;

    int value;

    private int voodooCount;

    k()
    {
        value = 0;
    }

    k(int n)
    {
        value = n;
    }

    public void voodoo()
    {
        voodooCount += 1;
        t058.goldChecker.println("Voodoo " + voodooCount);
    }
}

public class t058
{
    public static final GoldChecker goldChecker = new GoldChecker( "t058" );

    public static void main(String argv[])
    {
        k ko;
        l lo;
        k ka[];
        k kaaa[][][];
        Object o;
        boolean b;
        int i,j,z;

        ko = new k();
        ko.voodoo();
        lo = ko;
        lo.voodoo();
        ka = new k[2];
        t058.goldChecker.println(ka.getClass().getName());
        kaaa = new k[2][2][2];
        t058.goldChecker.println(kaaa.getClass().getName());

        o = ka;
        t058.goldChecker.println(o.getClass().getName());
        o = kaaa;
        t058.goldChecker.println(o.getClass().getName());
        o = ko;
        t058.goldChecker.println(o.getClass().getName());
        ko = (k) o;
        t058.goldChecker.println(ko.getClass().getName());
        b = o instanceof k;
        t058.goldChecker.println("o instanceof k: " + b);
        lo.voodoo();

        o = new Object();
        t058.goldChecker.println(o.getClass().getName());

        t058.goldChecker.println();
        t058.goldChecker.println("Here come the instance variables of ko:");

        ko.d = 39.0;
        ko.l = 40;
        ko.i = 41;
        ko.f = 42.0f;
        ko.s = (short) 43;
        ko.c = (char) 44;
        ko.b = (byte) 45;

        t058.goldChecker.println(ko.d);
        t058.goldChecker.println(ko.l);
        t058.goldChecker.println(ko.i);
        t058.goldChecker.println(ko.f);
        t058.goldChecker.println((int) ko.s);
        t058.goldChecker.println((int) ko.c);
        t058.goldChecker.println((int) ko.b);


        t058.goldChecker.println();
        t058.goldChecker.println("Here come the static variables of k:");

        k.sd = 46.0;
        k.sl = 47;
        k.si = 48;
        k.sf = 49.0f;
        k.ss = (short) 50;
        k.sc = (char) 51;
        k.sb = (byte) 52;

        t058.goldChecker.println(k.sd);
        t058.goldChecker.println(k.sl);
        t058.goldChecker.println(k.si);
        t058.goldChecker.println(k.sf);
        t058.goldChecker.println((int) k.ss);
        t058.goldChecker.println((int) k.sc);
        t058.goldChecker.println((int) k.sb);

        /* Initialize the arrays. */
        for(i=0; i<2; i+=1)
        {
            ka[i] = new k(i);
            for(j=0; j<2; j+=1)
            {
                for(z=0; z<2; z+=1)
                {
                    kaaa[i][j][z] = new k(100*i + 10*j + z);
                }
            }
        }

        /* Display the arrays. */
        t058.goldChecker.println();
        t058.goldChecker.println("Here come the array values");
        for(i=0; i<2; i+=1)
        {
            t058.goldChecker.println(ka[i].value);
            for(j=0; j<2; j+=1)
            {
                for(z=0; z<2; z+=1)
                {
                    t058.goldChecker.println(kaaa[i][j][z].value);
                }
            }
            if (i < 1) {
                t058.goldChecker.println();
            }
        }
        t058.goldChecker.check();
    }
}
