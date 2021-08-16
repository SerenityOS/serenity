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
 * @summary converted from VM Testbase jit/t/t062.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t062.t062
 */

package jit.t.t062;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Like t058.java except in this case the class with all the fields and
// stuff is loaded before the client class, so no patching is required.

interface l
{
    void voodoo();
}

public class t062 implements l
{
    public static final GoldChecker goldChecker = new GoldChecker( "t062" );

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

    t062()
    {
        value = 0;
    }

    t062(int n)
    {
        value = n;
    }

    public void voodoo()
    {
        voodooCount += 1;
        t062.goldChecker.println("Voodoo " + voodooCount);
    }

    public static void main(String argv[])
    {
        k.main();
        t062.goldChecker.check();
    }
}

class k
{
    public static void main()
    {
        t062 ko;
        l lo;
        t062 ka[];
        t062 kaaa[][][];
        Object o;
        boolean b;
        int i,ii,z;

        ko = new t062();
        ko.voodoo();
        lo = ko;
        lo.voodoo();
        ka = new t062[2];
        t062.goldChecker.println(ka.getClass().getName());
        kaaa = new t062[2][2][2];
        t062.goldChecker.println(kaaa.getClass().getName());

        o = ka;
        t062.goldChecker.println(o.getClass().getName());
        o = kaaa;
        t062.goldChecker.println(o.getClass().getName());
        o = ko;
        t062.goldChecker.println(o.getClass().getName());
        ko = (t062) o;
        t062.goldChecker.println(ko.getClass().getName());
        b = o instanceof t062;
        t062.goldChecker.println("o instanceof t062: " + b);
        lo.voodoo();

        o = new Object();
        t062.goldChecker.println(o.getClass().getName());

        t062.goldChecker.println();
        t062.goldChecker.println("Here come the instance variables of ko:");

        ko.d = 39.0;
        ko.l = 40;
        ko.i = 41;
        ko.f = 42.0f;
        ko.s = (short) 43;
        ko.c = (char) 44;
        ko.b = (byte) 45;

        t062.goldChecker.println(ko.d);
        t062.goldChecker.println(ko.l);
        t062.goldChecker.println(ko.i);
        t062.goldChecker.println(ko.f);
        t062.goldChecker.println((int) ko.s);
        t062.goldChecker.println((int) ko.c);
        t062.goldChecker.println((int) ko.b);


        t062.goldChecker.println();
        t062.goldChecker.println("Here come the static variables of t062:");

        t062.sd = 46.0;
        t062.sl = 47;
        t062.si = 48;
        t062.sf = 49.0f;
        t062.ss = (short) 50;
        t062.sc = (char) 51;
        t062.sb = (byte) 52;

        t062.goldChecker.println(t062.sd);
        t062.goldChecker.println(t062.sl);
        t062.goldChecker.println(t062.si);
        t062.goldChecker.println(t062.sf);
        t062.goldChecker.println((int) t062.ss);
        t062.goldChecker.println((int) t062.sc);
        t062.goldChecker.println((int) t062.sb);

        /* Initialize the arrays. */
        for(i=0; i<2; i+=1)
        {
            ka[i] = new t062(i);
            for(ii=0; ii<2; ii+=1)
            {
                for(z=0; z<2; z+=1)
                {
                    kaaa[i][ii][z] = new t062(100*i + 10*ii + z);
                }
            }
        }

        /* Display the arrays. */
        t062.goldChecker.println();
        t062.goldChecker.println("Here come the array values");
        for(i=0; i<2; i+=1)
        {
            t062.goldChecker.println(ka[i].value);
            for(ii=0; ii<2; ii+=1)
            {
                for(z=0; z<2; z+=1)
                {
                    t062.goldChecker.println(kaaa[i][ii][z].value);
                }
            }
            if (i < 1) {
                t062.goldChecker.println();
            }
        }
        t062.goldChecker.check();
    }
}
