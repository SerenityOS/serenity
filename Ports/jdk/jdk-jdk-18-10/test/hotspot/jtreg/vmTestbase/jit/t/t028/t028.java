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
 * @summary converted from VM Testbase jit/t/t028.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t028.t028
 */

package jit.t.t028;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_dneg, opc_fneg

public class t028
{
    public static final GoldChecker goldChecker = new GoldChecker( "t028" );

    public static void main(String argv[])
    {
        double d = 1e300;
        double x, y;
        float e = 1e-34f;
        float f, g;
        t028.goldChecker.println(d); t028.goldChecker.println(e);
        d = -(x = -(y = -d)); e = -(f = -(g = -e));
        t028.goldChecker.println(d); t028.goldChecker.println(e);
        d = -d; e = -e;
        d /= e;
        t028.goldChecker.println(d); t028.goldChecker.println(e);
        d = -d; e = -e;
        t028.goldChecker.println(d); t028.goldChecker.println(e);
                               t028.goldChecker.check();
    }
}
