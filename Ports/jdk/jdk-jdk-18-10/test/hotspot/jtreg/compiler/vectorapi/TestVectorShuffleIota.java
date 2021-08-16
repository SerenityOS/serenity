/*
 * Copyright (c) 2021, Huawei Technologies Co. Ltd. All rights reserved.
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

package compiler.vectorapi;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;

/*
 * @test
 * @bug 8265907
 * @modules jdk.incubator.vector
 * @run main/othervm compiler.vectorapi.TestVectorShuffleIota
 */

public class TestVectorShuffleIota {
    static final VectorSpecies<Integer> SPECIESi = IntVector.SPECIES_128;

    static final int INVOC_COUNT = 50000;

    static int[] ai = {87, 65, 78, 71};

    static void testShuffleI() {
        IntVector iv = (IntVector) VectorShuffle.iota(SPECIESi, 0, 2, false).toVector();
        iv.intoArray(ai, 0);
    }

    public static void main(String[] args) {
        for (int i = 0; i < INVOC_COUNT; i++) {
            testShuffleI();
        }
        for (int i = 0; i < ai.length; i++) {
            System.out.print(ai[i] + ", ");
        }
        System.out.println();
    }
}
