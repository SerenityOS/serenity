/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8249607 8260420
 * @library /test/lib
 * @summary A LoadNode is pinned in split_if_with_blocks_post() on a loop exit node x that is part of a strip mined loop. It has a late control y outside
            the outer strip mined loop. After pre-main-post, the dominator chain of y does not include x anymore resulting in an assertion failure.
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.TestSplitIfPinnedLoadInStripMinedLoop::*
 *                   compiler.loopopts.TestSplitIfPinnedLoadInStripMinedLoop
 */
package compiler.loopopts;

import jdk.test.lib.Asserts;

public class TestSplitIfPinnedLoadInStripMinedLoop {

    public boolean bFld = false;
    public short sFld = 4;
    public static int iFld = 5;
    public static float fFld= 6.0f;
    public static int iArrFld[] = new int[400];

    public void test1() {
        int x = 7;
        int y = 8;
        int a = 9;
        float f = 10.0f;
        double d = 11.0f;
        double dArr[] = new double[400];

        for (int i = 16; i < 350; i++) {
            for (int j = 1; j < 75; j++) {
                for (int k = 1; k < 3; k++) {
                }
                f = j * 6;
                y = j;
                try {
                    x = (y / 148);
                } catch (ArithmeticException a_e) {}
                if (bFld) {
                    break;
                }
                dArr[1] = 4;
            }
            for (int k = 75; k > i; k--) {
                iArrFld[k] = 5;
            }
            for (int k = 4; k < 75; k++) {
                f -= fFld;
                // The LoadSNode for sFld is cloned in split_if_with_blocks_post() for each use such that they can float out of the loop. All control
                // inputs of the clone are set to the latest control of the original LoadSNode which in this case is the StoreSNode for iFld that is
                // aninput to a MergeMemNode which is an input to the SafePointNode in the outer strip mined loop. Both these nodes are not part
                // of the loop body and thus the StoreNode is also not part of the loop anymore. This means that all the new LoadNode clones get
                // the loop exit l inside the outer strip mined loop as control input. Some of these clones (**) have a late control outside of
                // this outer strip mined loop. The dominator chain from the controls nodes of (**) contain l. However, after pre-main-post, we
                // insert additional Region nodes but do not account for these control inputs of the LoadSNodes. They remain unchanged and still
                // have l as control input. As a consequence, we do not find l on the dominator chains from the control nodes of (**) anymore
                // resulting in a dominator assertion failure.
                iFld = sFld;
            }
            switch ((i % 8) + 27) {
            case 27:
                if (bFld) {
                    for (a = 1; a < 75; a++) {
                        iFld += 6; // (**)
                    }
                } else {
                    d -= x;
                }
                break;
            case 28:
                iFld = y;
                // Fall through
            case 33:
            case 34:
                iFld -= (int)d; // (**)
                break;
            }
        }
    }

    static class MyClass {
        int x = 42;
    }

    int res = 0;

    // The obj1.x load has two uses: The 'res' store and the return. After cloning, both loads end up in the
    // OuterStripMinedLoop which triggers an assert in LoopNode::verify_strip_mined:
    // assert(found_sfpt) failed: no node in loop that's not input to safepoint
    int test2(MyClass obj1, MyClass obj2) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                obj2.x = 42; // Prevents obj1.x load from floating up because obj2 could alias obj1
                res = obj1.x;
            }
            for (int j = 0; j < 10_000; ++j) {
            }
        }
        return res;
    }

    // Same as test2 but with reference to outer loop induction variable 'i' and different order of instructions.
    // Triggers an assert in PhaseIdealLoop::build_loop_late_post_work if loop strip mining verification is disabled:
    // assert(false) failed: Bad graph detected in build_loop_late
    int test3(MyClass obj1, MyClass obj2) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                res = obj1.x + i;
                obj2.x = 42;
            }
            for (int j = 0; j < 10_000; ++j) {
            }
        }
        return res;
    }

    // Same as test2 but with reference to inner loop induction variable 'j' and different order of instructions.
    // Triggers an assert in PhaseCFG::insert_anti_dependences if loop strip mining verification is disabled:
    // assert(!LCA_orig->dominates(pred_block) || early->dominates(pred_block)) failed: early is high enough
    int test4(MyClass obj1, MyClass obj2) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                res = obj1.x + j;
                obj2.x = 42;
            }
            for (int j = 0; j < 10_000; ++j) {
            }
        }
        return res;
    }

    public static void main(String[] strArr) {
        TestSplitIfPinnedLoadInStripMinedLoop t = new TestSplitIfPinnedLoadInStripMinedLoop();
        MyClass obj = new MyClass();
        for (int i = 0; i < 10; i++) {
            t.test1();
            int res = t.test2(obj, obj);
            Asserts.assertEquals(res, t.res);
            Asserts.assertEquals(res, 42);
            res = t.test3(obj, obj);
            Asserts.assertEquals(res, t.res);
            Asserts.assertEquals(res, 51);
            res = t.test4(obj, obj);
            Asserts.assertEquals(res, t.res);
            Asserts.assertEquals(res, 51);
        }
    }
}
