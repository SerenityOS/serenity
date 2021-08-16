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
 * @summary converted from VM Testbase jit/t/t007.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t007.t007
 */

package jit.t.t007;

import nsk.share.TestFailure;

public class t007
{
    public static void main(String argv[])
    {
        int i;
        i = 39;
        int result;
        switch(i)
        {
        case 30: result = 30; break;
        case 31: result = 31; break;
        case 32: result = 32; break;
        case 33: result = 33; break;
        case 34: result = 34; break;
        case 35: result = 35; break;
        case 36: result = 36; break;
        case 37: result = 37; break;
        case 38: result = 38; break;
        case 39: result = 39; break;
        case 40: result = 40; break;
        case 41: result = 41; break;
        case 42: result = 42; break;
        case 43: result = 43; break;
        case 44: result = 44; break;
        case 45: result = 45; break;
        case 46: result = 46; break;
        default: result = -1; break;
        }

        if(result != 39)
                throw new TestFailure("Test failed: result != 39 (" + result + ")");
    }
}
