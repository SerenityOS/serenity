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


/**
 * @test
 * @requires vm.compiler2.enabled
 * @bug 8238438
 * @summary Tests to select the memory state of the last load in a load pack in SuperWord::co_locate_pack.
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.superword.CoLocatePackMemoryState::test
 *      -XX:LoopMaxUnroll=16 compiler.loopopts.superword.CoLocatePackMemoryState
 */

package compiler.loopopts.superword;

public class CoLocatePackMemoryState {

    public static final int N = 64;
    public static byte byFld;
    public static int iArr[] = new int[N+1];

    public static void test() {
        // Needs to pick the memory state of the last load for the iArr[i] load pack in SuperWord::co_locate_pack
        // since it is dependent on the iArr[i+1] stores.
        for (int i = 0; i < N; ++i) {
            iArr[i+1] = i;
            iArr[i] -= 15;
            byFld += 35;
        }
    }

    public static void main(String[] strArr) {
        for (int i = 0; i < 2000; i++) {
            for (int j = 0; j < N; j++) {
                iArr[j] = 0;
            }
            test();

            if (iArr[0] != -15) {
                throw new RuntimeException("iArr[0] must be -15 but was " + iArr[0]);
            }
            for (int j = 1; j < N; j++) {
                if (iArr[j] != (j-16)) {
                    throw new RuntimeException("iArr[" + j + "] must be " + (j-16) + " but was " + iArr[j]);
                }
            }
        }
    }
}
