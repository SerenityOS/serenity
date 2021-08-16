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
 * @summary converted from VM Testbase jit/t/t024.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t024.t024
 */

package jit.t.t024;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_f2d, opc_f2i, opc_f2l

public class t024
{
    public static final GoldChecker goldChecker = new GoldChecker( "t024" );

    static void do1(float f)
    {
        int i;
        long l;
        double d;
        i = (int) f;
        l = (long) f;
        d = f;
        t024.goldChecker.println();
        t024.goldChecker.println(f);
        t024.goldChecker.println(i);
        t024.goldChecker.println(l);
        t024.goldChecker.println(d);
    }

    public static void main(String argv[])
    {
        t024.goldChecker.println("t024");
        do1(39.0f);
        do1(1e10f);
        do1((float) 1e300);
        do1(1.23456789876f);
        t024.goldChecker.check();
    }
}
