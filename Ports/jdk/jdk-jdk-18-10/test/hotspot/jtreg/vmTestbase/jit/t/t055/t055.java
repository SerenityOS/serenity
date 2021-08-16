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
 * @summary converted from VM Testbase jit/t/t055.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t055.t055
 */

package jit.t.t055;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t055
{
    public static final GoldChecker goldChecker = new GoldChecker( "t055" );

    static void Palmer(int catcher) throws Throwable
    {
        try
        {
            Throwable slider = new Throwable();
            throw slider;
        }
        catch(Throwable splitter)
        {
            if(catcher == 1)
                t055.goldChecker.println("Caught by Palmer");
            else
                throw splitter;
        }
    }

    static void Tinker(int catcher) throws Throwable
    {
        try
        {
            Palmer(catcher);
        }
        catch(Throwable curve)
        {
            if(catcher == 2)
                t055.goldChecker.println("Caught by Tinker");
            else
                throw curve;
        }
    }

    static void Evers(int catcher) throws Throwable
    {
        try
        {
            Tinker(catcher);
        }
        catch(Throwable straightChange)
        {
            if(catcher == 3)
                t055.goldChecker.println("Caught by Evers");
            else
                throw straightChange;
        }
    }

    static void Chance(int catcher) throws Throwable
    {
        try
        {
            Evers(catcher);
        }
        catch(Throwable knuckler)
        {
            if(catcher == 4)
                t055.goldChecker.println("Caught by Chance");
            else
                throw knuckler;
        }
    }

    public static void main(String argv[])
    {
        int i;

        for(i=1; i<=5; i+=1)
        {
            try
            {
                t055.goldChecker.print(i + ": ");
                Chance(i);
            }
            catch(Throwable screwball)
            {
                t055.goldChecker.println("Caught in main");
            }
        }

        t055.goldChecker.println("So long for now from Candlestick Park.");
        t055.goldChecker.check();
    }
}
