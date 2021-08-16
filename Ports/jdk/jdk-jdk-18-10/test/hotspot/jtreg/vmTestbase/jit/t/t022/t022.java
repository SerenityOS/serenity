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
 * @summary converted from VM Testbase jit/t/t022.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t022.t022
 */

package jit.t.t022;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// The double-precision version of t021.java.

public class t022
{
    public static final GoldChecker goldChecker = new GoldChecker( "t022" );

    public static void main(String argv[])
    {
        double a, b, c, d, e;

        a = 0.0;
        b = 1.0;
        c = 2.0;
        e = 41.0 - c;
        d = a + b * c / e;

        t022.goldChecker.println(a);
        t022.goldChecker.println(b);
        t022.goldChecker.println(c);
        t022.goldChecker.println(d);
        t022.goldChecker.check();
    }
}
