/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8210392
 * @summary C2 Assert failure: Live node limit exceeded
 *
 * @run main/othervm compiler.loopopts.Test8210392
 */

package compiler.loopopts;

public class Test8210392 {
    public static int ival = 17;

    public static int intFn() {
        int v = 0, k = 0;
        for (int i = 17; i < 311; i += 3) {
            v = Test8210392.ival;
            int j = 1;
            do {
                v *= i;
                v += j * v;
                while (++k < 1)
                    ;
            } while (++j < 13);
        }
        return v;
    }

    public void mainTest() {
        for (int i = 0; i < 30000; i++) {
            Test8210392.ival = intFn();
        }
    }

    public static void main(String[] _args) {
        Test8210392 tc = new Test8210392();
        for (int i = 0; i < 10; i++) {
            tc.mainTest();
        }
    }
}

