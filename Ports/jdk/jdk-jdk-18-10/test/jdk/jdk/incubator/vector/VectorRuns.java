/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.vector.*;

import java.util.stream.IntStream;

/**
 * @test
 * @modules jdk.incubator.vector
 */

public class VectorRuns {

    public static void main(String[] args) {

        for (int i = 1; i < 1024; i++) {
            int[] a = IntStream.range(0, 1024).toArray();
            a[i] = -1;
            countRunAscending(a);

            assertEquals(countRunAscending(a), i);
            assertEquals(countRunAscendingVector(a), i);
        }
    }

    static void assertEquals(Object a, Object b) {
        if (!a.equals(b)) throw new AssertionError(a + " " + b);
    }

    // Count run of a[0] >  a[1] >  a[2] >  ...
    static int countRunAscending(int[] a) {
        int r = 1;
        if (r >= a.length)
            return a.length;

        while (r < a.length && a[r - 1] <= a[r]) {
            r++;
        }
        return r;
    }


    static int countRunAscendingVector(int[] a) {
        VectorSpecies<Integer> species = IntVector.SPECIES_256;

        int r = 1;
        if (r >= a.length)
            return a.length;

        int length = a.length & (species.length() - 1);
        if (length == a.length) length -= species.length();
        while (r < length) {
            IntVector vl = IntVector.fromArray(species, a, r - 1);
            IntVector vr = IntVector.fromArray(species, a, r);
            VectorMask<Integer> m = vl.compare(VectorOperators.GT, vr);
            if (m.anyTrue())
                return r + Long.numberOfTrailingZeros(m.toLong());
            r += species.length();
        }

        while (r < a.length && a[r - 1] <= a[r]) {
            r++;
        }
        return r;
    }
}
