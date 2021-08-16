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
 * @bug 8248552
 * @summary A Division/modulo node whose zero check was removed is split through an induction variable phi and executed before
 *          the loop limit check resulting in a SIGFPE because the divisor is zero.
 *
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.c2.loopopts.TestSplitThruPhiDivMod::test* compiler.c2.loopopts.TestSplitThruPhiDivMod
 */
package compiler.c2.loopopts;

public class TestSplitThruPhiDivMod {

    int x;

    public int testMod() {
        int i1 = 2;
        for (int i = 5; i < 25; i++) {
            for (int j = 50; j > 1; j -= 2) {
                /*
                 * Zero check is removed based on the type of the induction variable phi (variable j) since its always between 1 and 50.
                 * However, when splitting the modulo node through the phi, it can be executed right after the subtraction j-2 which can be
                 * 0 before evaluation the loop limit condition in the last iteration when j is 2: j-2 = 2-2 = 0. This results in a SIGFPE.
                 * The fix is to not split a division or modulo node 'n' through the induction variable phi if the zero check was removed
                 * earlier and the new inputs of the clones of 'n' after the split could be zero (i.e. the type of the clones of 'n' include 0).
                 */
                x = (20 % j); // Problematic division as part of modulo. Results in a SIGFPE, even though j is always non-zero.
                i1 = (i1 / i);
                for (int k = 3; k > 1; k--) {
                    switch ((i % 4) + 22) {
                    case 22:
                        switch (j % 10) {
                        case 83:
                            x += 5;
                            break;
                        }
                    }
                }
            }
        }
        return i1;
    }

    public int testDiv() {
        int i1 = 2;
        for (int i = 5; i < 25; i++) {
            for (int j = 50; j > 1; j -= 2) {
                // Same issue as above but with a division node. See explanation above.
                x = (20 / j); // Problematic division. Results in a SIGFPE, even though j is always non-zero.
                i1 = (i1 / i);
                for (int k = 3; k > 1; k--) {
                    switch ((i % 4) + 22) {
                    case 22:
                        switch (j % 10) {
                        case 83:
                            x += 5;
                            break;
                        }
                    }
                }
            }
        }
        return i1;
    }

    public static void main(String[] strArr) {
        TestSplitThruPhiDivMod t = new TestSplitThruPhiDivMod();
        for (int i = 0; i < 10000; i++) {
            t.testDiv();
            t.testMod();
        }
    }
}
