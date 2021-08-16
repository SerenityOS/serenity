/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8236759
 * @summary ShouldNotReachHere in PhaseIdealLoop::verify_strip_mined_scheduling
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm LoadSplitThruPhi
 *
 */

public class LoadSplitThruPhi {

    public static void getPermutations(byte[] inputArray, byte[][] outputArray) {
        int[] indexes = new int[]{0, 2};

        for (int a = 0; a < inputArray.length; a++) {
            int oneIdx = indexes[0]++;
            for (int b = a + 1; b < inputArray.length; b++) {
                int twoIdx = indexes[1]++;
                outputArray[twoIdx][0] = inputArray[a];
                outputArray[twoIdx][1] = inputArray[b];
            }
        }
    }

    public static void main(String[] args) {

        final byte[] inputArray = new byte[]{0, 1};
        final byte[][] outputArray = new byte[3][2];

        for (int i = 0; i < 1000000; i++) {
            getPermutations(inputArray, outputArray);
        }
    }

}
