/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestIntegerComparison
 * @bug 8043284 8042786
 * @summary Tests optimizations of signed and unsigned integer comparison.
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.integerArithmetic.TestIntegerComparison::testSigned
 *      -XX:CompileCommand=compileonly,compiler.integerArithmetic.TestIntegerComparison::testUnsigned
 *      compiler.integerArithmetic.TestIntegerComparison
 */
package compiler.integerArithmetic;

public class TestIntegerComparison {
    /**
     * Tests optimization of signed integer comparison (see BoolNode::Ideal).
     * The body of the if statement is unreachable and should not be compiled.
     *
     * @param c Character (value in the integer range [0, 65535])
     */
    public static void testSigned(char c) {
        // The following addition may overflow. The result is in one
        // of the two ranges [IntMax] and [IntMin, IntMin + CharMax - 1].
        int result = c + Integer.MAX_VALUE;
        // CmpINode has to consider both result ranges instead of only
        // the general [IntMin, IntMax] range to be able to prove that
        // result is always unequal to CharMax.
        if (result == Character.MAX_VALUE) {
            // Unreachable
            throw new RuntimeException("Should not reach here!");
        }
    }

    /**
     * Tests optimization of unsigned integer comparison (see CmpUNode::Value).
     * The body of the if statement is unreachable and should not be compiled.
     *
     * @param c Character (value in the integer range [0, 65535])
     */
    public static void testUnsigned(char c) {
    /*
     * The following if statement consisting of two CmpIs is replaced
     * by a CmpU during optimization (see 'IfNode::fold_compares').
     *
     * The signed (lo < i) and (i < hi) are replaced by the unsigned
     * (i - (lo+1) < hi - (lo+1)). In this case the unsigned comparison
     * equals (result - 2) < 98 leading to the following CmpUNode:
     *
     * CmpU (AddI result, -2) 98
     *
     * With the value of result this is simplified to:
     *
     * CmpU (AddI c, -(CharMax - IntMin)) 98
     *
     * The subtraction may underflow. The result is in one of the two
     * ranges [IntMin], [IntMax - CharMax + 1]. Both ranges have to be
     * considered instead of only the general [IntMin, IntMax] to prove
     * that due to the overflow the signed comparison result < 98 is
     * always false.
     */
        int result = c - (Character.MAX_VALUE - Integer.MIN_VALUE) + 2;
        if (1 < result && result < 100) {
            // Unreachable
            throw new RuntimeException("Should not reach here!");
        }
    }

    /**
     * Tests optimizations of signed and unsigned integer comparison.
     */
    public static void main(String[] args) {
        // We use characters to get a limited integer range for free
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; ++i) {
            testSigned((char) i);
            testUnsigned((char) i);
        }
    }
}
