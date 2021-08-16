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
 * @summary converted from VM Testbase jit/t/t025.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t025.t025
 */

package jit.t.t025;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_daload, opc_dastore, opc_faload, opc_fastore

public class t025
{
    public static final GoldChecker goldChecker = new GoldChecker( "t025" );

    static void show(double d[], float f[])
    {
        for(int i=0; i<10; i+=1)
            t025.goldChecker.println(i + ": " + f[i] + ", " + d[i]);
    }

    public static void main(String argv[])
    {
        double d[] = new double[10];
        float f[] = new float[10];
        d[0] = f[0] = 0.0f;
        d[1] = f[1] = 1.0f;
        for(int i=2; i<10; i+=1)
            d[i] = f[i] = f[i-1] + f[i-2];
        show(d, f);
        t025.goldChecker.check();
    }
}
