/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8210387
 * @summary Test removal of unreachable regions during CCP.
 * @library /test/lib
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *                   -XX:CompileOnly=compiler.c2.TestUnreachableRegionDuringCCP::test
 *                   compiler.c2.TestUnreachableRegionDuringCCP
 */

package compiler.c2;

import jdk.test.lib.Asserts;

public class TestUnreachableRegionDuringCCP {
    static int iFld1 = -1;
    static int iFld2 = -1;
    static int iArrFld[] = new int[100];

    public static void test() {
        int i = 1;
        do {
            iArrFld[i] = iFld1;
            iFld1 = 42;
            for (int j = 1; j < 5; j++) {
                if (i != 0) {
                    return; // Always returns
                }
                iFld2 += j;
            }
        } while (++i < 10);
    }

    public static void main(String[] args) {
        test();
        Asserts.assertEQ(iFld1, 42);
        Asserts.assertEQ(iFld2, -1);
        Asserts.assertEQ(iArrFld[1], -1);
    }
}
