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
 * @summary converted from VM Testbase jit/t/t101.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t101.t101
 */

package jit.t.t101;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// int2char when the int resides in esi.  The temptation is to emit, say,
//
//     movzwl %esi, %eax
//
// , which ain't anything Ma Intel understands.

public class t101
{
    public static final GoldChecker goldChecker = new GoldChecker( "t101" );

    public static void main(String[] argv)
    {
        char a[] = new char[8];
        char x,x0,x1,x2,x3,x4,x5,x6,x7;
        int i;
        for(i=0; i<8; i+=1)
            a[i] = (char) i;
        x = (char) (
            (x0=(char)(int)a[0]) +
            ((x1=(char)(int)a[1]) +
            ((x2=(char)(int)a[2]) +
            ((x3=(char)(int)a[3]) +
            ((x4=(char)(int)a[4]) +
            ((x5=(char)(int)a[5]) +
            ((x6=(char)(int)a[6]) +
            (x7=(char)(int)a[7])))))))
            );

        t101.goldChecker.println("... and the answer is " + (int) x);
        t101.goldChecker.check();
    }
}
