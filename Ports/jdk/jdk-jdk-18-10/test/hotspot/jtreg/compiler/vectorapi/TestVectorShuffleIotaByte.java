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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;

import org.testng.Assert;
import org.testng.annotations.Test;


/*
 * @test
 * @bug 8265956
 * @modules jdk.incubator.vector
 * @run testng/othervm compiler.vectorapi.TestVectorShuffleIotaByte
 */

@Test
public class TestVectorShuffleIotaByte {
    static final VectorSpecies<Byte> SPECIESb_64 = ByteVector.SPECIES_64;
    static final VectorSpecies<Byte> SPECIESb_128 = ByteVector.SPECIES_128;
    static final VectorSpecies<Byte> SPECIESb_256 = ByteVector.SPECIES_256;
    static final VectorSpecies<Byte> SPECIESb_512 = ByteVector.SPECIES_512;

    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 50000);

    static final byte[] ab_64 = {41, 45, 59, 46, 115, 101, 103, 97};
    static final byte[] ab_128 = {112, 32, 116, 117, 111, 104, 116, 105, 119, 32, 107, 111, 111, 98, 32, 97};
    static final byte[] ab_256 = {32, 101, 107, 105, 108, 32, 115, 105, 32, 117, 111, 121, 32, 116, 117, 111,
                                  104, 116, 105, 119, 32, 121, 97, 100, 32, 121, 114, 101, 118, 69, 32, 46};
    static final byte[] ab_512 = {103, 110, 97, 117, 72, 32, 71, 78, 65, 87, 32, 45, 45, 33, 117, 111, 121, 32,
                                  103, 110, 105, 115, 115, 105, 77, 32, 46, 117, 111, 121, 32, 111, 116, 32, 114,
                                  101, 116, 116, 101, 108, 32, 100, 114, 105, 104, 116, 32, 121, 109, 32, 115, 105,
                                  32, 115, 105, 104, 116, 44, 121, 116, 101, 101, 119, 83};

    static final byte[] expected_64 = {1, 3, 5, 7, -7, -5, -3, -1};
    static final byte[] expected_128 = {1, 3, 5, 7, 9, 11, 13, 15, -15, -13, -11, -9, -7, -5, -3, -1};
    static final byte[] expected_256 = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31,
                                        -31, -29, -27, -25, -23, -21, -19, -17, -15, -13, -11, -9, -7, -5, -3, -1};
    static final byte[] expected_512 = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31,
                                        33, 35, 37, 39, 41, 43, 45, 47, 49, 51, 53, 55, 57, 59, 61, 63,
                                        -63, -61, -59, -57, -55, -53, -51, -49, -47, -45, -43, -41, -39, -37, -35, -33,
                                        -31, -29, -27, -25, -23, -21, -19, -17, -15, -13, -11, -9, -7, -5, -3, -1};

    static void testShuffleIota_64() {
        ByteVector bv1 = (ByteVector) VectorShuffle.iota(SPECIESb_64, 1, 2, false).toVector();
        bv1.intoArray(ab_64, 0);
    }

    static void testShuffleIota_128() {
        ByteVector bv2 = (ByteVector) VectorShuffle.iota(SPECIESb_128, 1, 2, false).toVector();
        bv2.intoArray(ab_128, 0);
    }

    static void testShuffleIota_256() {
        ByteVector bv3 = (ByteVector) VectorShuffle.iota(SPECIESb_256, 1, 2, false).toVector();
        bv3.intoArray(ab_256, 0);
    }

    static void testShuffleIota_512() {
        ByteVector bv4 = (ByteVector) VectorShuffle.iota(SPECIESb_512, 1, 2, false).toVector();
        bv4.intoArray(ab_512, 0);
    }

    @Test
    static void testIota_64() {
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            testShuffleIota_64();
        }
        Assert.assertEquals(ab_64, expected_64);
    }

    @Test
    static void testIota_128() {
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            testShuffleIota_128();
        }
        Assert.assertEquals(ab_128, expected_128);
    }

    @Test
    static void testIota_256() {
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            testShuffleIota_256();
        }
        Assert.assertEquals(ab_256, expected_256);
    }

    @Test
    static void testIota_512() {
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            testShuffleIota_512();
        }
        Assert.assertEquals(ab_512, expected_512);
    }
}
