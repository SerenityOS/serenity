/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorSpecies;

import java.util.Arrays;

import jdk.test.lib.format.ArrayDiff;

/*
 * @test
 * @bug 8259353 8259601
 * @summary VectorReinterpretNode is incorrectly optimized out
 * @modules jdk.incubator.vector
 * @library /test/lib
 *
 * @run main compiler.vectorapi.Test8259353
 */
public class Test8259353 {

    static final VectorSpecies<Byte> SPECIES_128 = ByteVector.SPECIES_128;
    static final VectorSpecies<Byte> SPECIES_64 = ByteVector.SPECIES_64;

    static final byte[] a = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    static final byte[] b = new byte[16];
    static final byte[] c = new byte[16];
    static final byte[] r = {0, 1, 2, 3, 4, 5, 6, 7, 0, 0, 0, 0, 0, 0, 0, 0};

    private static void func() {
        var av = ByteVector.fromArray(SPECIES_128, a, 0);
        var bv = (ByteVector)av.reinterpretShape(SPECIES_64, 0);
        bv.intoArray(b, 0);

        var cv = (ByteVector)bv.reinterpretShape(SPECIES_128, 0);
        cv.intoArray(c, 0);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            func();
        }

        System.out.println("a: " + Arrays.toString(a));
        System.out.println("b: " + Arrays.toString(b));
        System.out.println("c: " + Arrays.toString(c));

        var diff = ArrayDiff.of(b, r);
        if (!diff.areEqual()) {
            throw new AssertionError("b array is unexpected: " + diff.format());
        }

        diff = ArrayDiff.of(c, r);
        if (!diff.areEqual()) {
            throw new AssertionError("c array is unexpected: " + diff.format());
        }
    }
}

