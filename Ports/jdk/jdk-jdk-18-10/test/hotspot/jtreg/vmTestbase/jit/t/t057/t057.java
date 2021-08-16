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
 * @summary converted from VM Testbase jit/t/t057.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t057.t057
 */

package jit.t.t057;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Just like t056 except here the exception is not explicitly thrown.

public class t057
{
    public static final GoldChecker goldChecker = new GoldChecker( "t057" );

    static int i;
    static int j;

    // Routine nest in which exception is thrown and caught in
    // the fourth routine.

    static void r14(){r24();}
    static void r24(){r34();}
    static void r34(){r44();}
    static void r44()
    {
        try
        {
            i/=j;
        }
        catch(Throwable t)
        {
            t057.goldChecker.println("caught in r44");
        }
    }

    // Thrown in the fourth; caught in the third.

    static void r13(){r23();}
    static void r23(){r33();}
    static void r33()
    {
        try
        {
            r43();
        }
        catch(Throwable t)
        {
            t057.goldChecker.println("caught in r33");
        }
    }
    static void r43() throws Throwable
    {
        i/=j;
    }

    // Thrown in fourth; caught in second.

    static void r12(){r22();}
    static void r22()
    {
        try
        {
            r32();
        }
        catch(Throwable t)
        {
            t057.goldChecker.println("caught in r22");
        }
    }
    static void r32() throws Throwable {r42();}
    static void r42() throws Throwable
    {
        i/=j;
    }

    // Thrown in fourth; caught in first.

    static void r11()
    {
        try
        {
            r21();
        }
        catch(Throwable t)
        {
            t057.goldChecker.println("caught in r11");
        }
    }
    static void r21() throws Throwable {r31();}
    static void r31() throws Throwable {r41();}
    static void r41() throws Throwable
    {
        i/=j;
    }

    public static void main(String argv[])
    {
        t057.goldChecker.print("Calling r14: "); r14();
        t057.goldChecker.print("Calling r13: "); r13();
        t057.goldChecker.print("Calling r12: "); r12();
        t057.goldChecker.print("Calling r11: "); r11();
                                           t057.goldChecker.check();
    }
}
