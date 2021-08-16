/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 * @requires vm.compiler2.enabled
 * @bug 8268019
 * @summary Splitting an If through a dying loop header region that is not a LoopNode, yet, results in a dead data loop.
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,compiler.c2.TestDeadLoopSplitIfLoop::test -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+StressIGVN -XX:StressSeed=2382674767 -XX:CompileCommand=dontinline,compiler.c2.TestDeadLoopSplitIfLoop::test
 *                   compiler.c2.TestDeadLoopSplitIfLoop
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,compiler.c2.TestDeadLoopSplitIfLoop::test -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+StressIGVN -XX:CompileCommand=dontinline,compiler.c2.TestDeadLoopSplitIfLoop::test
 *                   compiler.c2.TestDeadLoopSplitIfLoop
 */
package compiler.c2;

public class TestDeadLoopSplitIfLoop {
    int a;
    int b;
    boolean c;

    public static void main(String[] g) {
        TestDeadLoopSplitIfLoop h = new TestDeadLoopSplitIfLoop();
        h.test();
    }

    void test() {
        int e = 4;
        long f[] = new long[a];
        if (c) {
        } else if (c) {
            // Dead path is removed after parsing which results in a dead data loop for certain node orderings in IGVN.
            switch (126) {
                case 126:
                    do {
                        f[e] = b;
                        switch (6) {
                            case 7:
                                f = f;
                        }
                    } while (e++ < 93);
            }
        }
    }
}
