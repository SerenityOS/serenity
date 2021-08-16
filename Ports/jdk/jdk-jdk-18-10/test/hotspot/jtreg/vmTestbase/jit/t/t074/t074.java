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
 * @summary converted from VM Testbase jit/t/t074.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t074.t074
 */

package jit.t.t074;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t074{
    public static final GoldChecker goldChecker = new GoldChecker( "t074" );

    static int i0 = 0;
    static int i1 = 1;
    static int i2 = 2;
    static int i3 = 3;
    static int i4 = 4;
    static int i5 = 5;
    static int i6 = 6;
    static int i7 = 7;
    static int i8 = 8;
    static int i9 = 9;
    static int i10 = 10;
    static int i11 = 11;
    static int i12 = 12;
    static int i13 = 13;
    static int i14 = 14;
    static int i15 = 15;

    public static void main(String[] argv){
        int a;
        a =
        ((((i0 + i1) + (i2 + i3)) + ((i4 + i5) + (i6 + i7))) +
        (((i8 + i9) + (i10 + i11)) + ((i12 + i13) + (i14 + i15))));
        t074.goldChecker.println(a);
        t074.goldChecker.check();
    }
}
