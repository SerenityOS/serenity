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
 * @summary converted from VM Testbase jit/t/t032.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t032.t032
 */

package jit.t.t032;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_i2d, opc_i2f, opc_i2l
// opc_l2d, opc_l2f, opc_l2i

public class t032
{
    public static final GoldChecker goldChecker = new GoldChecker( "t032" );

    static int i;
    static long l;
    static double d;
    static float f;

    static void showem()
    {
        t032.goldChecker.println();
        t032.goldChecker.println(d);
        t032.goldChecker.println(f);
        t032.goldChecker.println(i);
        t032.goldChecker.println(l);
    }

    public static void main(String argv[])
    {
        i = 39;
        d = i;
        f = (float) i;
        l = i;
        showem();

        l = 39;
        d = (double) l;
        f = (float) l;
        i = (int) l;
        showem();

        l = 1000000000;;
        d = (double) l;
        f = (float) l;
        i = (int) l;
        showem();

        t032.goldChecker.check();
    }
}
