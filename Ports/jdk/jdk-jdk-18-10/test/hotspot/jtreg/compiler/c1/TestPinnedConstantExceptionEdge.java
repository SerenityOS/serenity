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
 * @bug 8255058
 * @summary Add check in LinearScan::resolve_exception_edge for pinned constant that is not virtual which cannot be used to find an interval which
            resulted in an assertion error.
 * @run main/othervm -Xcomp -XX:TieredStopAtLevel=1 -XX:CompileCommand=dontinline,compiler.c1.TestPinnedConstantExceptionEdge::dontInline
 *                   -XX:CompileCommand=compileonly,compiler.c1.TestPinnedConstantExceptionEdge::* compiler.c1.TestPinnedConstantExceptionEdge
 */
package compiler.c1;

public class TestPinnedConstantExceptionEdge {

    public static long iFld = 0;
    public static boolean b1;
    public static boolean b2;

    public static void test() {
        int x = 5;
        int y = 11;
        for (int i = 1; i < 8; i++) {
            for (int j = 1; j < 2; ++j) {
                if (b1) {
                    try {
                        y = (x / x);
                        y = (500 / i);
                        y = (-214 / i);
                    } catch (ArithmeticException a_e) {}
                    // Recursion too deep in UseCountComputer::uses_do and therefore constant 1 is pinned.
                    iFld += (b1 ? 1 : 0) + (b2 ? 1 : 0) + 5 + 7 + 6 + 5 + y
                            + dontInline(7) + dontInline(5) + 8 + 8 + 9
                            + dontInline(3) + dontInline(3) + dontInline(4)
                            + dontInline(dontInline(5)) + dontInline(2);
                    return;
                }
            }
        }
    }

    // Not inlined
    public static int dontInline(int a) {
        return 0;
    }

    public static void main(String[] strArr) {
        test();
    }
}

