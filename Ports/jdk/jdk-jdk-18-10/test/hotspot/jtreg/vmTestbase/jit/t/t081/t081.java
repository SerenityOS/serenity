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
 * @summary converted from VM Testbase jit/t/t081.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t081.t081
 */

package jit.t.t081;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t081
{
    public static final GoldChecker goldChecker = new GoldChecker( "t081" );

    public static void main(String argv[])
    {
        int i = 0x87654321;
        int i1, i31, i32, i63;
        i1 = 1;
        i31 = 31;
        i32 = 32;
        i63 = 63;

        t081.goldChecker.println(i >> 1);
        t081.goldChecker.println(i >> 31);
        t081.goldChecker.println(i >> 32);
        t081.goldChecker.println(i >> 63);

        t081.goldChecker.println("");
        t081.goldChecker.println(i << 1);
        t081.goldChecker.println(i << 31);
        t081.goldChecker.println(i << 32);
        t081.goldChecker.println(i << 63);

        t081.goldChecker.println("");
        t081.goldChecker.println(i >>> 1);
        t081.goldChecker.println(i >>> 31);
        t081.goldChecker.println(i >>> 32);
        t081.goldChecker.println(i >>> 63);

        t081.goldChecker.println("");
        t081.goldChecker.println(i >> i1);
        t081.goldChecker.println(i >> i31);
        t081.goldChecker.println(i >> i32);
        t081.goldChecker.println(i >> i63);

        t081.goldChecker.println("");
        t081.goldChecker.println(i << i1);
        t081.goldChecker.println(i << i31);
        t081.goldChecker.println(i << i32);
        t081.goldChecker.println(i << i63);

        t081.goldChecker.println("");
        t081.goldChecker.println(i >>> i1);
        t081.goldChecker.println(i >>> i31);
        t081.goldChecker.println(i >>> i32);
        t081.goldChecker.println(i >>> i63);

        t081.goldChecker.check();
    }
}
