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
 * @summary converted from VM Testbase jit/t/t008.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t008.t008
 */

package jit.t.t008;

import nsk.share.TestFailure;

public class t008
{
    public static void main(String argv[])
    {
        int i;
        i = 39;
        int result;
        switch(i)
        {
        case 30: result = 30; break;
        case 39: result = 31; break;
        case 402: result = 32; break;
        case 1033: result = 33; break;
        case 2034: result = 34; break;
        case 2135: result = 35; break;
        case 35036: result = 36; break;
        case 35037: result = 37; break;
        case 100038: result = 38; break;
        case 200039: result = 39; break;
        case 300040: result = 40; break;
        case 400041: result = 41; break;
        case 500042: result = 42; break;
        case 600043: result = 43; break;
        case 700044: result = 44; break;
        case 800045: result = 45; break;
        case 900046: result = 46; break;
        default: result = -1; break;
        }

        if(result != 31)
                throw new TestFailure("Test failed: result != 31 (" + result + ")");
    }
}
