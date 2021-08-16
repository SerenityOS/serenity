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
 * @summary converted from VM Testbase jit/t/t003.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t003.t003
 */

package jit.t.t003;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t003
{
    public static final GoldChecker goldChecker = new GoldChecker( "t003" );

    public static void main(String argv[])
    {
        int a[] = new int[2];
        int i = -1;

        a[0] = 39;
        a[1] = 40;
        t003.goldChecker.println("a[0] == " + a[0]);
        t003.goldChecker.println("a[1] == " + a[1]);

        try
        {
            a[2] = 41;
            t003.goldChecker.println("o-o-r high didn't throw exception");
        }
        catch(Throwable x)
        {
            t003.goldChecker.println("o-o-r high threw exception");
        }

        try
        {
            a[i] = 41;
            t003.goldChecker.println("o-o-r low didn't throw exception");
        }
        catch(Throwable x)
        {
            t003.goldChecker.println("o-o-r low threw exception");
        }
        t003.goldChecker.check();
    }
}
