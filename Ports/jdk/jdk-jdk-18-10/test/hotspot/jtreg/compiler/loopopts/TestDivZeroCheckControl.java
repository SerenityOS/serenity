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
 *
 */

/*
 * @test
 * @bug 8229496
 * @summary Verify that zero check is executed before division/modulo operation.
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xbatch -XX:LoopUnrollLimit=0
 *                   -XX:CompileCommand=dontinline,compiler.loopopts.TestDivZeroCheckControl::test*
 *                   compiler.loopopts.TestDivZeroCheckControl
 */

/*
 * @test
 * @summary Verify that zero check is executed before division/modulo operation.
 * @requires vm.graal.enabled
 * @run main/othervm -Xbatch
 *                   -XX:CompileCommand=dontinline,compiler.loopopts.TestDivZeroCheckControl::test*
 *                   compiler.loopopts.TestDivZeroCheckControl
 */

package compiler.loopopts;

public class TestDivZeroCheckControl {

    public static int test1(int div, int array[]) {
        int res = 0;
        for (int i = 0; i < 256; i++) {
            int j = 0;
            do {
                array[i] = i;
                try {
                    res = 1 % div;
                } catch (ArithmeticException ex) { }
            } while (++j < 9);
        }
        return res;
    }

    // Same as test1 but with division instead of modulo
    public static int test2(int div, int array[]) {
        int res = 0;
        for (int i = 0; i < 256; i++) {
            int j = 0;
            do {
                array[i] = i;
                try {
                    res = 1 / div;
                } catch (ArithmeticException ex) { }
            } while (++j < 9);
        }
        return res;
    }

    // Same as test1 but with long
    public static long test3(long div, int array[]) {
        long res = 0;
        for (int i = 0; i < 256; i++) {
            int j = 0;
            do {
                array[i] = i;
                try {
                    res = 1L % div;
                } catch (ArithmeticException ex) { }
            } while (++j < 9);
        }
        return res;
    }

    // Same as test2 but with long
    public static long test4(long div, int array[]) {
        long res = 0;
        for (int i = 0; i < 256; i++) {
            int j = 0;
            do {
                array[i] = i;
                try {
                    res = 1L / div;
                } catch (ArithmeticException ex) { }
            } while (++j < 9);
        }
        return res;
    }

    public static void main(String[] args) {
        int array[] = new int[256];
        for (int i = 0; i < 50_000; ++i) {
            test1(0, array);
            test2(0, array);
            test3(0, array);
            test4(0, array);
        }
    }
}
