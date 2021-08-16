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

/*
 * @test
 * @bug 8267375
 * @requires os.arch == "aarch64" & vm.debug == true & vm.compiler2.enabled
 * @modules jdk.incubator.vector
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.vectorapi.TestVectorInsertByte::* -XX:PrintIdealGraphLevel=3 -XX:PrintIdealGraphFile=TestVectorInsertByte.xml compiler.vectorapi.TestVectorInsertByte
 */

public class TestVectorInsertByte {
    static final VectorSpecies<Byte> SPECIESb = ByteVector.SPECIES_MAX;

    static final int INVOC_COUNT = 50000;
    static final int size = SPECIESb.length();

    static byte[] ab = new byte[size];
    static byte[] rb = new byte[size];

    static void init() {
        for (int i = 0; i < size; i++) {
            ab[i] = (byte) (size - 1 - i);
        }
    }

    static void testByteVectorInsert() {
        ByteVector av = ByteVector.fromArray(SPECIESb, ab, 0);
        av = av.withLane(0, (byte) (0));
        av.intoArray(rb, 0);
    }

    public static void main(String[] args) {
        init();
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            testByteVectorInsert();
        }
    }
}
