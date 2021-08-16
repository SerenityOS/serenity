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
 * @bug 8249605
 * @summary A dead memory loop is detected when replacing an actually dead memory phi node in PhiNode::Ideal()
 *          by a dead MergeMemNode which builds a cycle with one of its slices.
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.c2.TestDeadPhiMergeMemLoop::main
 *                   -XX:CompileCommand=dontinline,compiler.c2.TestDeadPhiMergeMemLoop::dontInline
 *                   compiler.c2.TestDeadPhiMergeMemLoop
 */

package compiler.c2;

public class TestDeadPhiMergeMemLoop {

    public static boolean bFld = false;
    public static double dArrFld[] = new double[400];

    public static void main(String[] strArr) {
        int x = 1;
        int i = 0;
        int iArr[] = new int[400];
        dontInline();

        if (bFld) {
            x += x;
        } else if (bFld) {
            float f = 1;
            while (++f < 132) {
                if (bFld) {
                    dArrFld[5] = 3;
                    for (i = (int)(f); i < 12; i++) {
                    }
                }
            }
        }
    }

    // Not inlined
    public static void dontInline() {
    }
}
