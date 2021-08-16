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
 * @summary converted from VM Testbase jit/t/t093.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t093.t093
 */

package jit.t.t093;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t093
{
    public static final GoldChecker goldChecker = new GoldChecker( "t093" );

    static long i, j;

    static void foo()
    {
        i /= j;
    }

    public static void main(String argv[])
    {
        try
        {
            foo();
        }
        catch(Throwable x)
        {
            t093.goldChecker.println("Hiya.");
        }
        try
        {
            foo();
        }
        catch(Throwable x)
        {
            t093.goldChecker.println("Bye.");
        }
        t093.goldChecker.check();
    }
}
