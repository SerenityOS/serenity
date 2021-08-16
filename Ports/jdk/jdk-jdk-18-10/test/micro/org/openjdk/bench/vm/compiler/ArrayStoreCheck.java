/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarking measuring ArrayStoreCheck-performance plus the ability of the optimizer to remove storechecks
 * altogether.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@SuppressWarnings("rawtypes")
@State(Scope.Thread)
public class ArrayStoreCheck {

    /** How large should the test-arrays be. */
    public static final int TESTSIZE = 1000;

    private Vector[] fromVectorArr, toVectorArr;

    private Object[] fromObjectArr, toObjectArr;

    private Object[] fromObject2Arr, toObject2Arr;

    private Object[] fromObject3Arr, toObject3Arr;

    private Object[] fromObject4Arr, toObject4Arr;

    @Setup(Level.Iteration)
    public void createArrays() {
        fromVectorArr = new Vector[TESTSIZE];
        toVectorArr = new Vector[TESTSIZE];

        fromObjectArr = fromVectorArr;
        toObjectArr = toVectorArr;

        /* set every almost 90% of all indices to an object. */
        for (int i = 0; i < TESTSIZE; i++) {
            fromVectorArr[i] = new Vector();
        }
        for (int i = 0; i < TESTSIZE; i += 10) {
            fromVectorArr[i] = null;
        }

        fromObject2Arr = new Vector[TESTSIZE][1][][][];
        toObject2Arr = new Vector[TESTSIZE][1][][][];

        fromObject3Arr = new Stack[TESTSIZE][1][][][];
        toObject3Arr = new Vector[TESTSIZE][1][][][];

        fromObject4Arr = new Object[TESTSIZE];
        toObject4Arr = new Comparable[TESTSIZE];
        /* set every two indices to an object. */
        for (int i = 0; i < TESTSIZE; i += 2) {
            fromObject4Arr[i] = new String("apa?");
        }
    }

    /**
     * Test that loads from an Vector[] and stores in another Vector[]. The local types of the arrays are both Vector[].
     * Hopefully we only will do a runtime check that we are storing in an exact Vector[].
     */
    @Benchmark
    public void testArrayStoreCheckRT1() throws Exception {
        Vector[] localFromArray = fromVectorArr;
        Vector[] localToArray = toVectorArr;

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localFromArray[i];
        }
    }

    /**
     * Test that stores a newly created Vector in a Vector[]. The local type of the array is Vector[]. Hopefully we only
     * will do a runtime check that we are storing in the same Vector[].
     */
    @Benchmark
    public void testArrayStoreCheckRT2() throws Exception {
        Vector[] localToArray = toVectorArr;
        Vector localVector = new Vector();

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localVector;
        }
    }

    /**
     * Test that loads from a Vector[] and stores in the same Vector[]. Hopefully we only will remove the storecheck
     * altogether due to the fact that the arrays are the same (and easily proven that they are they same).
     */
    @Benchmark
    public void testArrayStoreCheckRemove1() throws Exception {
        Vector[] localToArray = toVectorArr;
        for (int i = 2; i < TESTSIZE; i++) {
            localToArray[i] = localToArray[i - 2];
        }
    }

    /**
     * Test that loads from a Vector[] and stores in another Vector[]. The local types of the arrays are both Object[].
     * This should be a tricky case where we statically have no clue what type the arrays actually are of. We should have
     * to do a complex check.
     */
    @Benchmark
    public void testArrayStoreCheckComplex1() throws Exception {
        Object[] localFromArray = fromObjectArr;
        Object[] localToArray = toObjectArr;

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localFromArray[i];
        }
    }

    /**
     * Test that loads from a Vector[][][][] and stores in another Vector[][][][]. The local types of the arrays are both
     * Object[]. This should be a tricky case where we statically have no clue what type the arrays actually are of. We
     * should have to do a complex check. Difference from complex1-test is that the actual types of the arrays are
     * multi-dimensioned.
     */
    @Benchmark
    public void testArrayStoreCheckComplex2() throws Exception {
        Object[] localFromArray = fromObject2Arr;
        Object[] localToArray = toObject2Arr;

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localFromArray[i];
        }
    }

    /**
     * Test that loads from a Stack[][][][] and stores in a Vector[][][][]. The local types of the arrays are both
     * Object[]. This should be a tricky case where we statically have no clue what type the arrays actually are of. We
     * should have to do a complex check. Difference from complex2-test is that the actual types of the from-array is
     * different from the actual type of the to-array.
     */
    @Benchmark
    public void testArrayStoreCheckComplex3() throws Exception {
        Object[] localFromArray = fromObject3Arr;
        Object[] localToArray = toObject3Arr;

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localFromArray[i];
        }
    }

    /**
     * Test that loads from a Object[] and stores in a Comparable[]. The local types of the arrays are both Object[]. This
     * should be a tricky case where we statically have no clue what type the arrays actually are of. We should have to do
     * a complex check. The interesting part with this test is that the destination array is an interface array.
     */
    @Benchmark
    public void testArrayStoreCheckComplex4() throws Exception {
        Object[] localFromArray = fromObject4Arr;
        Object[] localToArray = toObject4Arr;

        for (int i = 0; i < TESTSIZE; i++) {
            localToArray[i] = localFromArray[i];
        }
    }

}
