/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8187822
 * @summary C2 conditonal move optimization might create broken graph
 * @requires vm.flavor == "server"
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,TestCMovSplitThruPhi::not_inlined -XX:CompileOnly=TestCMovSplitThruPhi::test -XX:-LoopUnswitching TestCMovSplitThruPhi
 *
 */

public class TestCMovSplitThruPhi {
    static int f;

    static int test(boolean flag1, boolean flag2, boolean flag3, boolean flag4) {
        int v3 = 0;
        if (flag4) {
            for (int i = 0; i < 10; i++) {
                int v1 = 0;
                if (flag1) {
                    v1 = not_inlined();
                }
                // AddI below will be candidate for split through Phi
                int v2 = v1;
                if (flag2) {
                    v2 = f + v1;
                }
                // test above will be converted to CMovI
                if (flag3) {
                    v3 = v2 * 2;
                    break;
                }
            }
        }
        return v3;
    }

    private static int not_inlined() {
        return 0;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            test((i % 2) == 0, (i % 2) == 0, (i % 100) == 1, (i % 1000) == 1);
        }
    }
}
