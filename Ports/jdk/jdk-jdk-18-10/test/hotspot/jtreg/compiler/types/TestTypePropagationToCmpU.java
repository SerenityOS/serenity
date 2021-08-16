/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080156 8060036
 * @summary Test correctness of type propagation to CmpUNodes.
 *
 * @run main compiler.types.TestTypePropagationToCmpU
 */

package compiler.types;

public class TestTypePropagationToCmpU {
    public static void main(String[] args) {
        try {
            // Trigger compilation
            for (int i = 0; i < 100_000; ++i) {
                test();
            }
        } catch (NullPointerException e) {
            // Test should never throw a NullPointerException
            throw new RuntimeException("Test failed");
        }
    }

    static int global = 42;

    public static void test() {
        int a = Integer.MIN_VALUE;
        int b = global;
        char[] buf = { 0 };
        for (int i = 0; i <= b; ++i) {
            a = i - b;
        }
        // C2 adds a range check and an uncommon trap here to ensure that the array index
        // is in bounds. If type information is not propagated correctly to the corresponding
        // CmpUNode, this trap may be always taken. Because C2 also removes the unnecessary
        // allocation of 'buf', a NullPointerException is thrown in this case.
        char c = buf[(a * 11) / 2 - a]; // a is 0 here if global >= 0
        buf[0] = 0;
    }
}
