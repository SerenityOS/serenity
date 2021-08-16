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
 * @summary converted from VM Testbase jit/t/t039.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t039.t039
 */

package jit.t.t039;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_lreturn, opc_monitorenter, opc_monitorexit

public class t039
{
    public static final GoldChecker goldChecker = new GoldChecker( "t039" );

    private static long f0 = 0, f1 = 1;
    private static Object x = new Object();

    private static long nextFib()
    {
        long res;

        synchronized(x)
        {
            res = f0 + f1;
            f0 = f1;
            f1 = res;
        }
        return res;
    }

    public static void main(String argv[])
    {
        for(int i=2; i<10; ++i)
            t039.goldChecker.println(i + ": " + nextFib());
        t039.goldChecker.check();
    }
}
