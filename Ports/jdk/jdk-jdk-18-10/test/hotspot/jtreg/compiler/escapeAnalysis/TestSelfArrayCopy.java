/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8229016 8231055
 * @summary Test correct elimination of array allocation with arraycopy to itself.
 * @library /test/lib
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.escapeAnalysis.TestSelfArrayCopy::test*
 *                   compiler.escapeAnalysis.TestSelfArrayCopy
 */

package compiler.escapeAnalysis;

import jdk.test.lib.Utils;

public class TestSelfArrayCopy {
    private static boolean b = false;
    private static final int rI1 = Utils.getRandomInstance().nextInt();
    private static final int rI2 = Utils.getRandomInstance().nextInt();

    private static int test1() {
        // Non-escaping allocation
        Integer[] array = {rI1, rI2};
        // Arraycopy with src == dst
        System.arraycopy(array, 0, array, 0, array.length - 1);
        if (b) {
            // Uncommon trap
            System.out.println(array[0]);
        }
        return array[0] + array[1];
    }

    private static int test2() {
        // Non-escaping allocation
        Integer[] array = {rI1, rI2};
        // Arraycopy with src == dst
        System.arraycopy(array, 0, array, 1, 1);
        if (b) {
            // Uncommon trap
            System.out.println(array[0]);
        }
        return array[0] + array[1];
    }

    public static void main(String[] args) {
        int expected1 = rI1 + rI2;
        int expected2 = rI1 + rI1;
        // Trigger compilation
        for (int i = 0; i < 20_000; ++i) {
            int result = test1();
            if (result != expected1) {
                throw new RuntimeException("Incorrect result: " + result + " != " + expected1);
            }
            result = test2();
            if (result != expected2) {
                throw new RuntimeException("Incorrect result: " + result + " != " + expected2);
            }
        }
        b = true;
        int result = test1();
        if (result != expected1) {
            throw new RuntimeException("Incorrect result: " + result + " != " + expected1);
        }
        result = test2();
        if (result != expected2) {
            throw new RuntimeException("Incorrect result: " + result + " != " + expected2);
        }
    }
}
