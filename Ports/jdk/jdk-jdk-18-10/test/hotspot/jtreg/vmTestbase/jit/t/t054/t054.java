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
 * @summary converted from VM Testbase jit/t/t054.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t054.t054
 */

package jit.t.t054;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;


// A more comprehensive test for the tomcatv bug, that being failure
// correctly to reverse the sense of a jump.

public class t054
{
    public static final GoldChecker goldChecker = new GoldChecker( "t054" );

    public static void main(String argv[])
    {
        int i;
        int n;

        t054.goldChecker.println("vbl :: vbl");
        n = 1;
        for(i=0; i<n; i+=1)
            t054.goldChecker.println("1");
        for(i=1; i<=n; i+=1)
            t054.goldChecker.println("2");
        for(i=1; i==n; i+=1)
            t054.goldChecker.println("3");
        for(i=0; i!=n; i+=1)
            t054.goldChecker.println("4");
        for(i=2; i>n; i-=1)
            t054.goldChecker.println("5");
        for(i=1; i>=n; i-=1)
            t054.goldChecker.println("6");

        t054.goldChecker.println();
        t054.goldChecker.println("vbl :: reg");
        n = 2;
        for(i=0; i<n-1; i+=1)
            t054.goldChecker.println("1");
        for(i=1; i<=n-1; i+=1)
            t054.goldChecker.println("2");
        for(i=1; i==n-1; i+=1)
            t054.goldChecker.println("3");
        for(i=0; i!=n-1; i+=1)
            t054.goldChecker.println("4");
        for(i=2; i>n-1; i-=1)
            t054.goldChecker.println("5");
        for(i=1; i>=n-1; i-=1)
            t054.goldChecker.println("6");
        t054.goldChecker.check();
    }
}
