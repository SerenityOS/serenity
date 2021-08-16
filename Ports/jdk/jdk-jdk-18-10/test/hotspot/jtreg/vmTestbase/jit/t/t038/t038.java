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
 * @summary converted from VM Testbase jit/t/t038.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t038.t038
 */

package jit.t.t038;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_laload, opc_lastore, opc_lconst_0

public class t038
{
    public static final GoldChecker goldChecker = new GoldChecker( "t038" );

    public static void main(String argv[])
    {
        long a[] = new long[10];
        int i;
        a[0] = 0l;
        a[1] = 1l;
        for(i = 2; i < 10; i++)
            a[i] = a[i-1] + a[i-2];
        i = 0;
        do
            t038.goldChecker.println(a[i++]);
        while(i < 10);
        t038.goldChecker.check();
    }
}
