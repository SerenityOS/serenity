/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026844
 * @summary Test decrementExact
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main compiler.intrinsics.mathexact.DecExactLTest
 */

package compiler.intrinsics.mathexact;

public class DecExactLTest {
    public static long[] values = {1, 1, 1, 1};
    public static long[] minvalues = {Long.MIN_VALUE, Long.MIN_VALUE};

    public static void main(String[] args) {
        runTest(new Verify.DecExactL());
    }

    public static void runTest(Verify.UnaryLongMethod method) {
        for (int i = 0; i < 20000; ++i) {
            Verify.verifyUnary(Long.MIN_VALUE, method);
            Verify.verifyUnary(minvalues[0], method);
            Verify.verifyUnary(Long.MIN_VALUE - values[2], method);
            Verify.verifyUnary(0, method);
            Verify.verifyUnary(values[2], method);
            Verify.verifyUnary(Long.MAX_VALUE, method);
            Verify.verifyUnary(Long.MIN_VALUE - values[0] + values[3], method);
            Verify.verifyUnary(Long.MIN_VALUE + 1 - values[0], method);
        }
    }
}
