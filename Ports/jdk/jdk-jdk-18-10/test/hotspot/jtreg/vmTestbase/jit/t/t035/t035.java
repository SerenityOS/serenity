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
 * @summary converted from VM Testbase jit/t/t035.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t035.t035
 */

package jit.t.t035;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_idiv
// opc_imul
// opc_ishl
// opc_ishr
// opc_iushr
// opc_ladd
// opc_ldiv
// opc_lmul
// opc_lrem
// opc_lsub

public class t035
{
    public static final GoldChecker goldChecker = new GoldChecker( "t035" );

    public static void main(String argv[])
    {
        int i, j;
        long l, m;

        i = 3;
        j = 33;
        l = 402;
        m = 1;

        t035.goldChecker.println(i/j);
        t035.goldChecker.println(i*j);
        t035.goldChecker.println(i<<j);
        t035.goldChecker.println(i>>j);
        t035.goldChecker.println(i>>>j);
        t035.goldChecker.println(l+m);
        t035.goldChecker.println(l/m);
        t035.goldChecker.println(l*m);
        t035.goldChecker.println(l%m);
        t035.goldChecker.println(l-m);

        i = -i;
        l = -l;

        t035.goldChecker.println();
        t035.goldChecker.println(i/j);
        t035.goldChecker.println(i*j);
        t035.goldChecker.println(i<<j);
        t035.goldChecker.println(i>>j);
        t035.goldChecker.println(i>>>j);
        t035.goldChecker.println(l+m);
        t035.goldChecker.println(l/m);
        t035.goldChecker.println(l*m);
        t035.goldChecker.println(l%m);
        t035.goldChecker.println(l-m);

        t035.goldChecker.check();
    }
}
