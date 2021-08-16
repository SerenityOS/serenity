/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233032
 * @summary Tests SuperWord::co_locate_pack() involving a load pack that relies on a sandwiched and moved StoreF node.
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:UseAVX=1
 *      -XX:CompileCommand=compileonly,compiler.loopopts.superword.CoLocatePack::test
 *      compiler.loopopts.superword.CoLocatePack
 */
package compiler.loopopts.superword;

public class CoLocatePack {

    public static long lFld = 10;
    public static float fFld = 11.2f;
    public int iFld = 12;

    public void test() {
        int iArr[] = new int[200];
        float fArr[] = new float[200];

        /*
         * The IR for this loop contains the following StoreF chain after unrolling once:
         * StoreF 1 -> StoreF 2 -> StoreF 3 -> StoreF 4 -> StoreF 5 -> StoreF 6
         *
         * The superword algorithm creates a pack for [ StoreF 2 and 5 ] and one for [ StoreF 3 and 6 ]
         * (The pack [ StoreF 1 and 4 ] is filtered out). As a result, StoreF 3 and 4 are sandwiched between
         * StoreF 2 and 5. SuperWord::co_locate_pack() will move both after StoreF 5 to remove any dependencies
         * within the pack:
         * StoreF 1 -> [ StoreF 2 -> StoreF 5 ] -> StoreF 3 -> StoreF 4 -> StoreF 6
         *
         * Afterwards, StoreF 4 is moved before StoreF 3 to remove any dependency within [ StoreF 3 -> StoreF 6 ]
         * The resulting chain looks like this:
         * StoreF 1 -> [ StoreF 2 -> StoreF 5 ] -> StoreF 4 -> [ StoreF 3 -> StoreF 6 ]
         *
         * When later processing a load pack depending on StoreF 4 and 5, the first and last memory state of the load pack are
         * determined by using the bb indices. However, those were not updated before when moving nodes around and
         * bb_idx(4) < bb_idx(5) still holds even though they swapped positions in the IR. Therefore, it wrongly uses the memory
         * state of the first load (StoreF 5) in the pack as the last memory state. As a result, the graph walk always starts
         * following the input of StoreF 5 (which should actually be StoreF 4) and will move beyond a loop phi as the stop
         * condition is never met for a node having another memory state than the first one of the load pack. Eventually a
         * bb index for a node outside of the loop is read resulting in an assertion failure.
         *
         * The fix uses a different approach to find the first and last memory state of a load pack without depending on bb indices.
         */
        for (int i = 5; i < 169; i++) {
            fArr[i + 1] += ((long)(fFld) | 1); // StoreF 1/4
            iFld += lFld;
            fArr[i - 1] -= 20; // StoreF 2/5
            fFld += i;
            fArr[i + 1] -= -117; // StoreF 3/6

            int j = 10;
            do {
            } while (--j > 0);

            iArr[i] += 11;
        }
    }

    public static void main(String[] strArr) {
        CoLocatePack _instance = new CoLocatePack();
        for (int i = 0; i < 1000; i++ ) {
            _instance.test();
        }
    }
}
