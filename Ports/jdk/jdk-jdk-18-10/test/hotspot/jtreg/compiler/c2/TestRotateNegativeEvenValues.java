/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8257182
 * @requires vm.compiler2.enabled
 * @summary Test RotateLeftNode and RotateRightNode with negative even numbers which produces a wrong result due to
 *          applying an arithemtic instead of a logical shift.
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.c2.TestRotateNegativeEvenValues::run
 *                   -XX:CompileCommand=inline,compiler.c2.TestRotateNegativeEvenValues::test
 *                   -Xcomp -XX:-TieredCompilation compiler.c2.TestRotateNegativeEvenValues
 */
package compiler.c2;

public class TestRotateNegativeEvenValues {

    public static void run() {
        test(1 << 31); // INT_MIN
        test(1L << 63); // LONG_MIN
        test(-1 << 10); // -1024
        test(-1L << 10); // -1024
        test(-1 << 20); // -1048576
        test(-1L << 20); // -1048576
        test(-2); // 111...10
        test(-2L); // 111...10
        test(-3546); // Random minus even number
        test(-3546L); // Random minus even number
    }

    // Inlined such that negativeEvenNumber is a constant
    public static void test(int negativeEvenNumber) {
        for (int i = 1; i <= 1; i++) {
            int leftShift = negativeEvenNumber << -i;
            int rightShift = negativeEvenNumber >>> i;
            if ((leftShift | rightShift) != (rightShift | leftShift)) {
                int or1 = leftShift | rightShift;
                int or2 = rightShift | leftShift;
                throw new RuntimeException("Or operations are not equal:" + " " + or1 + " vs. "+ or2
                                           + " - leftShift: " + leftShift + ", rightShift: " + rightShift);
            }
        }
    }

    // Inlined such that negativeEvenNumber is a constant
    public static void test(long negativeEvenNumber) {
        for (int i = 1; i <= 1; i++) {
            long leftShift = negativeEvenNumber << -i;
            long rightShift = negativeEvenNumber >>> i;
            if ((leftShift | rightShift) != (rightShift | leftShift)) {
                long or1 = leftShift | rightShift;
                long or2 = rightShift | leftShift;
                throw new RuntimeException("Or operations are not equal:" + " " + or1 + " vs. "+ or2
                                           + " - leftShift: " + leftShift + ", rightShift: " + rightShift);
            }
        }
    }

    public static void main(String argv[]) {
        run();
    }
}
