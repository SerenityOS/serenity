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
 * @summary converted from VM Testbase jit/t/t042.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t042.t042
 */

package jit.t.t042;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// The special little twiddle that occurs when you invoke a method
// of an array.

public class t042
{
    public static final GoldChecker goldChecker = new GoldChecker( "t042" );

    static void sameAs(Object x, Object y, String call)
    {
        t042.goldChecker.print(call + ": ");
        if(x == null)
            t042.goldChecker.println("left object is null");
        if(x != null)
            t042.goldChecker.println(x.equals(y));
    }

    public static void main(String argv[])
    {
        int a[] = new int[3];
        int b[] = null;
        int c[] = a;
        Object x = a;
        Object y = new Object();
        Object z = null;

        sameAs(a, a, "sameAs(a, a)");
        sameAs(a, b, "sameAs(a, b)");
        sameAs(a, c, "sameAs(a, c)");
        sameAs(a, x, "sameAs(a, x)");
        sameAs(a, y, "sameAs(a, y)");
        sameAs(a, z, "sameAs(a, z)");

        t042.goldChecker.println();
        sameAs(b, a, "sameAs(b, a)");
        sameAs(b, b, "sameAs(b, b)");
        sameAs(b, c, "sameAs(b, c)");
        sameAs(b, x, "sameAs(b, x)");
        sameAs(b, y, "sameAs(b, y)");
        sameAs(b, z, "sameAs(b, z)");

        t042.goldChecker.println();
        sameAs(c, a, "sameAs(c, a)");
        sameAs(c, b, "sameAs(c, b)");
        sameAs(c, c, "sameAs(c, c)");
        sameAs(c, x, "sameAs(c, x)");
        sameAs(c, y, "sameAs(c, y)");
        sameAs(c, z, "sameAs(c, z)");

        t042.goldChecker.println();
        sameAs(x, a, "sameAs(x, a)");
        sameAs(x, b, "sameAs(x, b)");
        sameAs(x, c, "sameAs(x, c)");
        sameAs(x, x, "sameAs(x, x)");
        sameAs(x, y, "sameAs(x, y)");
        sameAs(x, z, "sameAs(x, z)");

        t042.goldChecker.println();
        sameAs(y, a, "sameAs(y, a)");
        sameAs(y, b, "sameAs(y, b)");
        sameAs(y, c, "sameAs(y, c)");
        sameAs(y, x, "sameAs(y, x)");
        sameAs(y, y, "sameAs(y, y)");
        sameAs(y, z, "sameAs(y, z)");

        t042.goldChecker.println();
        sameAs(z, a, "sameAs(z, a)");
        sameAs(z, b, "sameAs(z, b)");
        sameAs(z, c, "sameAs(z, c)");
        sameAs(z, x, "sameAs(z, x)");
        sameAs(z, y, "sameAs(z, y)");
        sameAs(z, z, "sameAs(z, z)");
        t042.goldChecker.check();
    }
}
