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
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorSpecies;

/*
 * @test
 * @bug 8260339
 * @summary StoreVectorNode is not considered with -XX:+OptimizeFill
 * @modules jdk.incubator.vector
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+OptimizeFill compiler.vectorapi.TestLoopStoreVector
 */

public class TestLoopStoreVector {
    static final VectorSpecies<Integer> SPECIESi = IntVector.SPECIES_PREFERRED;
    static final VectorSpecies<Long> SPECIESl = LongVector.SPECIES_PREFERRED;

    static final int INVOC_COUNT = 5000;
    static final int size = 64;

    static int[] ai = {20, 21, 02, 14, 83, 119, 101, 101, 116, 121, 44, 32,
                       73, 32, 76, 79, 86, 69, 32, 89, 79, 85, 32, 102, 111,
                       114, 101, 118, 101, 114, 33, 32, 32, 32, 45, 45, 32,
                       32, 32, 66, 121, 32, 87, 97, 110, 103, 72, 117, 97,
                       110, 103,46, 76, 105, 102, 101, 32, 105, 115, 32, 116,
                       104, 101, 32};
    static long[] al = {102, 108, 111, 119, 101, 114, 32, 102, 111, 114, 32,
                        119, 104, 105, 99, 104, 32, 108, 111, 118, 101, 32,
                        105, 115, 32, 116, 104, 101, 32, 104, 111, 110, 101,
                        121, 46, 32, 87, 101, 32, 119, 105, 108, 108, 32, 115,
                        116, 105, 99, 107, 32, 116, 111, 103, 101, 116, 104,
                        101, 114, 32, 33, 33, 33, 33, 32};

    public static void testVectorCastL2I(long[] input, int[] output, VectorSpecies<Long> speciesl, VectorSpecies<Integer> speciesi) {
        LongVector av = LongVector.fromArray(speciesl, input, 0);
        IntVector bv = (IntVector) av.castShape(speciesi, 0);
        bv.intoArray(output, 0);
    }

    public static int test0() {
        for (int i = 0; i < 1000; i++) {
            testVectorCastL2I(al, ai, SPECIESl, SPECIESi);
        }
        return 0;
    }

    public static void main(String[] args) {
        for (int i = 0; i < INVOC_COUNT; i++) {
            test0();
        }
        for (int i = 0; i < 64; i++) {
            System.out.print(ai[i] + " ");
        }
        System.out.println("");
    }
}
