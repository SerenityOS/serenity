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
 * @bug 8134031
 * @summary Bad rewiring of memory edges when we split unique types during EA
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      -XX:CompileCommand=dontinline,compiler.escapeAnalysis.TestEABadMergeMem::m_notinlined
 *      compiler.escapeAnalysis.TestEABadMergeMem
 */

package compiler.escapeAnalysis;

public class TestEABadMergeMem {

    static class Box {
        int i;
    }

    static void m_notinlined() {
    }

    static float dummy1;
    static float dummy2;

    static int test(Box a, Box c, int i, int j, int k, boolean flag1, boolean flag2) {
        Box b = new Box(); // non escaping
        a.i = i;
        b.i = j;
        c.i = k;

        m_notinlined();

        boolean flag3 = false;
        if (flag1) {
            for (int ii = 0; ii < 100; ii++) {
                if (flag2) {
                    dummy1 = (float)ii;
                } else {
                    dummy2 = (float)ii;
                }
            }
            flag3 = true;
        }
        // Memory Phi here with projection of not inlined call as one edge, MergeMem as other

        if (flag3) { // will split through Phi during loopopts
            int res = c.i + b.i;
            m_notinlined(); // prevents split through phi during igvn
            return res;
        } else {
            return 44 + 43;
        }
    }

    static public void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            // m(2);
            Box a = new Box();
            Box c = new Box();
            int res = test(a, c, 42, 43, 44, (i%2) == 0, (i%3) == 0);
            if (res != 44 + 43) {
                throw new RuntimeException("Bad result " + res);
            }
        }
    }

}
