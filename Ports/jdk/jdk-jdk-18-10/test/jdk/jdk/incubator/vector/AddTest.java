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

/**
 * @test
 * @modules jdk.incubator.vector
 * @requires vm.compiler2.enabled
 */

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

public class AddTest {
    static final VectorSpecies<Float> SPECIES =
            FloatVector.SPECIES_256;

    static final int SIZE = 1024;
    static float[] a = new float[SIZE];
    static float[] b = new float[SIZE];
    static float[] c = new float[SIZE];

    static {
        for (int i = 0; i < SIZE; i++) {
            a[i] = 1f;
            b[i] = 2f;
        }
    }

    static void workload() {
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            FloatVector av = FloatVector.fromArray(SPECIES, a, i);
            FloatVector bv = FloatVector.fromArray(SPECIES, b, i);
            av.add(bv).intoArray(c, i);
        }
    }

    static final int[] IDENTITY_INDEX_MAPPING = IntStream.range(0, SPECIES.length()).toArray();

    static void workloadIndexMapped() {
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            FloatVector av = FloatVector.fromArray(SPECIES, a, i, IDENTITY_INDEX_MAPPING, 0);
            FloatVector bv = FloatVector.fromArray(SPECIES, b, i, IDENTITY_INDEX_MAPPING, 0);
            av.add(bv).intoArray(c, i, IDENTITY_INDEX_MAPPING, 0);
        }
    }

    public static void main(String args[]) {
        for (int i = 0; i < 30_0000; i++) {
            workload();
        }
        for (int i = 0; i < a.length; i++) {
            if (c[i] != a[i] + b[i])
                throw new AssertionError();
        }

        Arrays.fill(c, 0.0f);

        for (int i = 0; i < 30_0000; i++) {
            workloadIndexMapped();
        }
        for (int i = 0; i < a.length; i++) {
            if (c[i] != a[i] + b[i])
                throw new AssertionError();
        }
    }
}
