/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8078262 8177095
 * @summary Tests correct dominator information after loop peeling.
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.loopopts.TestLoopPeeling::test*
 *      compiler.loopopts.TestLoopPeeling
 */

package compiler.loopopts;

public class TestLoopPeeling {

    public int[] array = new int[100];

    public static void main(String args[]) {
        TestLoopPeeling test = new TestLoopPeeling();
        try {
            test.testArrayAccess1(0, 1);
            test.testArrayAccess2(0);
            test.testArrayAccess3(0, false);
            test.testArrayAllocation(0, 1);
        } catch (Exception e) {
            // Ignore exceptions
        }
    }

    public void testArrayAccess1(int index, int inc) {
        int storeIndex = -1;

        for (; index < 10; index += inc) {
            // This loop invariant check triggers loop peeling because it can
            // be moved out of the loop (see 'IdealLoopTree::policy_peeling').
            if (inc == 42) return;

            // This loop variant usage of LShiftL( ConvI2L( Phi(storeIndex) ) )
            // prevents the split if optimization that would otherwise clone the
            // LShiftL and ConvI2L nodes and assign them to their corresponding array
            // address computation (see 'PhaseIdealLoop::split_if_with_blocks_post').
            if (storeIndex > 0 && array[storeIndex] == 42) return;

            if (index == 42) {
                // This store and the corresponding range check are moved out of the
                // loop and both used after main loop and the peeled iteration exit.
                // For the peeled iteration, storeIndex is always -1 and the ConvI2L
                // is replaced by TOP. However, the range check is not folded because
                // we don't do the split if optimization in PhaseIdealLoop2.
                // As a result, we have a (dead) control path from the peeled iteration
                // to the StoreI but the data path is removed.
                array[storeIndex] = 1;
                return;
            }

            storeIndex++;
        }
    }

    public int testArrayAccess2(int index) {
        // Load1 and the corresponding range check are moved out of the loop
        // and both are used after the main loop and the peeled iteration exit.
        // For the peeled iteration, storeIndex is always Integer.MIN_VALUE and
        // for the main loop it is 0. Hence, the merging phi has type int:<=0.
        // Load1 reads the array at index ConvI2L(CastII(AddI(storeIndex, -1)))
        // where the CastII is range check dependent and has type int:>=0.
        // The CastII gets pushed through the AddI and its type is changed to int:>=1
        // which does not overlap with the input type of storeIndex (int:<=0).
        // The CastII is replaced by TOP causing a cascade of other eliminations.
        // Since the control path through the range check CmpU(AddI(storeIndex, -1))
        // is not eliminated, the graph is in a corrupted state. We fail once we merge
        // with the result of Load2 because we get data from a non-dominating region.
        int storeIndex = Integer.MIN_VALUE;
        for (; index < 10; ++index) {
            if (index == 42) {
                return array[storeIndex-1]; // Load1
            }
            storeIndex = 0;
        }
        return array[42]; // Load2
    }

    public int testArrayAccess3(int index, boolean b) {
        // Same as testArrayAccess2 but manifests as crash in register allocator.
        int storeIndex = Integer.MIN_VALUE;
        for (; index < 10; ++index) {
            if (b) {
                return 0;
            }
            if (index == 42) {
                return array[storeIndex-1]; // Load1
            }
            storeIndex = 0;
        }
        return array[42]; // Load2
    }

    public byte[] testArrayAllocation(int index, int inc) {
        int allocationCount = -1;
        byte[] result;

        for (; index < 10; index += inc) {
            // This loop invariant check triggers loop peeling because it can
            // be moved out of the loop (see 'IdealLoopTree::policy_peeling').
            if (inc == 42) return null;

            if (index == 42) {
                // This allocation and the corresponding size check are moved out of the
                // loop and both used after main loop and the peeled iteration exit.
                // For the peeled iteration, allocationCount is always -1 and the ConvI2L
                // is replaced by TOP. However, the size check is not folded because
                // we don't do the split if optimization in PhaseIdealLoop2.
                // As a result, we have a (dead) control path from the peeled iteration
                // to the allocation but the data path is removed.
                result = new byte[allocationCount];
                return result;
            }

            allocationCount++;
        }
        return null;
    }
}

