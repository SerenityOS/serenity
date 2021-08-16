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
 * @summary converted from VM Testbase jit/t/t064.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t064.t064
 */

package jit.t.t064;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Main() does getstatics of k.b and putstatics of l.b.  K.set() does putstatics
// of k.b; l.show() does getstatics of l.b.  The idea is, you jit only
// main.  If the jit and the VM agree about the container size of a static
// field of type byte, you get the right answers.  If not, the test fails.
class k
{
    static byte b;
    static int i = -129;
    static void set()
    {
        b = (byte) i;
        ++i;
    }
}

class l
{
    static byte b;
    static int i = -129;
    static void show()
    {
        t064.goldChecker.println("l.b == " + b);
    }
}

public class t064
{
    public static final GoldChecker goldChecker = new GoldChecker( "t064" );

    public static void main(String argv[])
    {
        int i;
        for(i=0; i<258; i+=1)
        {
            k.set();
            t064.goldChecker.println("k.b == " + k.b);
            l.b = (byte) l.i;
            ++l.i;
            l.show();
        }
        t064.goldChecker.check();
    }
}
