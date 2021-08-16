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
 * @summary converted from VM Testbase jit/t/t065.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t065.t065
 */

package jit.t.t065;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Main() does getfields of k.b and putfields of l.b.  K.set() does putfields
// of k.b; l.show() does getfields of l.b.  The idea is, you jit only
// main.  If the jit and the VM agree about the container size of a static
// field of type byte, you get the right answers.  If not, the test fails.
class k
{
    byte b;
    int i = -129;
    void set()
    {
        b = (byte) i;
        ++i;
    }
}

class l
{
    byte b;
    int i = -129;
    void show()
    {
        t065.goldChecker.println("lo.b == " + b);
    }
}

public class t065
{
    public static final GoldChecker goldChecker = new GoldChecker( "t065" );

    public static void main(String argv[])
    {
        k ko = new k();
        l lo = new l();
        int i;
        for(i=0; i<258; i+=1)
        {
            ko.set();
            t065.goldChecker.println("ko.b == " + ko.b);
            lo.b = (byte) lo.i;
            ++lo.i;
            lo.show();
        }
        t065.goldChecker.check();
    }
}
