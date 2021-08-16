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
 * @summary converted from VM Testbase jit/t/t023.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t023.t023
 */

package jit.t.t023;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_d2f, opc_d2i, opc_d2l

public class t023
{
    public static final GoldChecker goldChecker = new GoldChecker( "t023" );

    static void do1(double d)
    {
        int i;
        long l;
        float f;
        i = (int) d;
        l = (long) d;
        f = (float) d;
        t023.goldChecker.println();
        t023.goldChecker.println(d);
        t023.goldChecker.println(i);
        t023.goldChecker.println(l);
        t023.goldChecker.println(f);
    }

    public static void main(String argv[])
    {
        do1(39.0);
        do1(1e10);
        do1(1e300);
        do1(1.23456789876);
        t023.goldChecker.check();
    }
}
