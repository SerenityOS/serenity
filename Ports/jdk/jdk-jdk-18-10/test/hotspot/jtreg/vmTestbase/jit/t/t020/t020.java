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
 * @summary converted from VM Testbase jit/t/t020.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t020.t020
 */

package jit.t.t020;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// opc_checkcast, opc_instanceof, opc_ifnull, opc_ifnonnull

public class t020
{
    public static final GoldChecker goldChecker = new GoldChecker( "t020" );

    private static void show(t020 x)
    {
        try
        {
            t020.goldChecker.print(" is a t020");
            if(x instanceof k)
                t020.goldChecker.print(" and also a k");
            if(x == null)
                t020.goldChecker.print(" and also null");
            if(x != null)
                t020.goldChecker.print(" and also not null");
            t020.goldChecker.println(".");
        }
        catch(Throwable e)
        {
            t020.goldChecker.println(" " + e.getMessage());
        }
    }

    public static void main(String argv[])
    {
        t020 a[] = new t020[4];
        t020 t = new k();
        k u = null;
        a[0] = new t020();
        a[1] = t;
        a[3] = u;

        for(int i=0; i<4; i+=1)
        {
            t020.goldChecker.print("a[" + i + "]");
            show(a[i]);
        }

        t020.goldChecker.println();
        for(int i=0; i<4; i+=1)
        {
            try
            {
                t020.goldChecker.print("Assigning a[" + i + "] to u");
                u = (k) a[i];
                t020.goldChecker.print(" worked ok");
            }
            catch(Throwable x)
            {
                t020.goldChecker.print(" failed");
            }
            finally
            {
                t020.goldChecker.println(".");
            }
        }
        t020.goldChecker.check();
    }

}

class k extends t020
{
}
