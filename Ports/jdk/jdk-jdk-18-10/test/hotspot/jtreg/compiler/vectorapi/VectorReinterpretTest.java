/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/*
 * @test
 * @bug 8259775
 * @summary Incorrect code-gen for VectorReinterpret operation
 * @modules jdk.incubator.vector
 * @run main/othervm -Xbatch compiler.vectorapi.VectorReinterpretTest
 */

public class VectorReinterpretTest {
    static final VectorSpecies<Byte> SPECIES_128 = ByteVector.SPECIES_128;
    static final VectorSpecies<Byte> SPECIES_256 = ByteVector.SPECIES_256;
    static final VectorSpecies<Byte> SPECIES_512 = ByteVector.SPECIES_512;

    static byte[] a = new byte[64];

    private static void test256_128_256() {
        ByteVector av = ByteVector.fromArray(SPECIES_256, a, 0);
        ByteVector bv = (ByteVector)av.reinterpretShape(SPECIES_128, 0);
        ByteVector cv = (ByteVector)bv.reinterpretShape(SPECIES_256, 0);

        if (bv.reduceLanes(VectorOperators.ADD) != 16 ||
            cv.reduceLanes(VectorOperators.ADD) != 16) {
            throw new Error("Failed");
        }
    }

    private static void test512_256_512() {
        ByteVector av = ByteVector.fromArray(SPECIES_512, a, 0);
        ByteVector bv = (ByteVector)av.reinterpretShape(SPECIES_256, 0);
        ByteVector cv = (ByteVector)bv.reinterpretShape(SPECIES_512, 0);

        if (bv.reduceLanes(VectorOperators.ADD) != 32 ||
            cv.reduceLanes(VectorOperators.ADD) != 32) {
            throw new Error("Failed");
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < a.length; i++) {
            a[i] = 1;
        }

        for (int i = 0; i < 100000; i++) {
            test256_128_256();
            test512_256_512();
        }
    }
}
