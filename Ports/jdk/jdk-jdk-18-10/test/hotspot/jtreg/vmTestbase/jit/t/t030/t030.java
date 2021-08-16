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
 * @summary converted from VM Testbase jit/t/t030.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t030.t030
 */

package jit.t.t030;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_dup2, opc_dup2_x1, opc_dup2_x2

public class t030
{
    public static final GoldChecker goldChecker = new GoldChecker( "t030" );

    double a[] = new double[1];
    double d, e;

    void doit()
    {
        e = d = a[0] = 39.0;
        t030.goldChecker.println();
        t030.goldChecker.println(a[0]);
        t030.goldChecker.println(d);
        t030.goldChecker.println(e);
    }

    public static void main(String argv[])
    {
        double a[] = new double[1];
        double d, e;
        t030 o = new t030();

        e = d = a[0] = 39.0;
        t030.goldChecker.println(a[0]);
        t030.goldChecker.println(d);
        t030.goldChecker.println(e);
        t030.goldChecker.check();
    }
}
