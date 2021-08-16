/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026722
 * @summary Verify that the compare after addExact is a signed compare
 *
 * @run main compiler.intrinsics.mathexact.CompareTest
 */


package compiler.intrinsics.mathexact;

public class CompareTest {
    public static long store = 0;
    public static long addValue = 1231;

    public static void main(String[] args) {
        for (int i = 0; i < 20000; ++i) {
            runTest(i, i);
            runTest(i-1, i);
        }
    }

    public static long create(long value, int v) {
        if ((value | v) == 0) {
            return 0;
        }

        // C2 turned this test into unsigned test when a control edge was set on the Cmp
        if (value < -31557014167219200L || value > 31556889864403199L) {
            throw new RuntimeException("error");
        }

        return value;
    }

    public static void runTest(long value, int value2) {
        long res = Math.addExact(value, addValue);
        store = create(res, Math.floorMod(value2, 100000));
    }
}
