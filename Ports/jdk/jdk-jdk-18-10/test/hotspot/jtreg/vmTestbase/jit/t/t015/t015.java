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
 * @summary converted from VM Testbase jit/t/t015.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t015.t015
 */

package jit.t.t015;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t015
{
    public static final GoldChecker goldChecker = new GoldChecker( "t015" );

    int x;

    t015(int i)
    {
        x = i;
    }


    public static void main(String argv[])
    {
        t015 a[] = new t015[2];
        int i;

        t015.goldChecker.println("Cookin' ...");
        for(i=0; i<2; i+=1)
            a[i] = null;

        t015.goldChecker.println("Pass 1 checking ...");
        for(i=0; i<2; i+=1)
            if(a[i] != null)
                t015.goldChecker.println("Fubar at " + i + " on pass 1");

        t015.goldChecker.println("Initializing elements ...");
        for(i=0; i<2; i+=1)
            a[i] = new t015(i);

        t015.goldChecker.println("Pass 2 checking ...");
        for(i=0; i<2; i+=1)
        {
            if(a[i] == null)
                t015.goldChecker.println("Null at " + i + " on pass 2");
            else if(a[i].x != i)
                t015.goldChecker.println("a[i].x != i at " + i + " on pass 2");
        }

        t015.goldChecker.println("Except as noted above, I'm a happy camper.");
        t015.goldChecker.check();
    }
}
