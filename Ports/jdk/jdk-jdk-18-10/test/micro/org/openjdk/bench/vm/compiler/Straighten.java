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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Various small benchmarks testing how well the optimizer straightens code.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Straighten {

    private int[] intArr;
    private long[] longArr;

    @Setup
    public void setupSubclass() {
        int TEST_SIZE = 300;

        Random r = new Random(453543);

        /*
         * initialize arrays with some values between 0 and 7.
         */
        intArr = new int[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            intArr[i] = r.nextInt(8);
        }

        longArr = new long[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            longArr[i] = (long) r.nextInt(8);
        }

    }

    private int innerCandidate1int(int i) {
        int j = 0;

        if (i == 5) {
            /* we know that i isn't 4 and is less than 7 here. */
            j++;
        }
        if (i == 4) {
            /* we know that i is less than 7 here. */
            j += 2;
        }
        if (i < 7) {
            /* we know that i is less than 7, so it isn't greater than 7. */
            j += 4;
        }
        if (i > 7) {
            j += 8;
        }
        return j;
    }

    /** Tests how well serial constant integer compares are straightened. */
    @Benchmark
    public int testStraighten1int() throws Exception {
        int dummy = 0;
        int[] arr = intArr;
        for (int i : arr) {
            dummy += innerCandidate1int(i);
        }
        return dummy;
    }

    private long innerCandidate1long(long l) {
        long j = 0;

        if (l == 5) {
            /* we know that l isn't 4 and is less than 7 here. */
            j++;
        }
        if (l == 4) {
            /* we know that l is less than 7 here. */
            j += 2;
        }
        if (l < 7) {
            /* we know that l is less than 7, so it isn't greater than 7. */
            j += 4;
        }
        if (l > 7) {
            j += 8;
        }
        return j;
    }

    /** Tests how well serial constant long compares are straightened. */
    @Benchmark
    public int testStraighten1long() throws Exception {
        int dummy = 0;
        long[] arr = longArr;
        for (long l : arr) {
            dummy += innerCandidate1long(l);
        }
        return dummy;
    }

    private int innerCandidate2int(int i) {
        int j;

        if (i < 5) {
            /* j becomes 3, so it should be straightened to j == 3 case below. */
            j = 3;
        } else {
            /* j becomes 4, so it should be straightened to j == 4 case below. */
            j = 4;
        }

        if (j == 4) {
            i += 2;
        }
        if (j == 3) {
            i += 4;
        }
        return i;
    }

    /** Tests how well constant integer definitions are straightened. */
    @Benchmark
    public int testStraighten2int() throws Exception {
        int[] arr = intArr;
        int dummy = 0;
        for (int i : arr) {
            dummy += innerCandidate2int(i);
        }
        return dummy;
    }

    private long innerCandidate2long(long l) {
        long j;

        if (l < 5) {
            /* j becomes 3, so it should be straightened to j == 3 case below. */
            j = 3;
        } else {
            /* j becomes 4, so it should be straightened to j == 4 case below. */
            j = 4;
        }

        if (j == 4) {
            l += 2;
        }
        if (j == 3) {
            l += 4;
        }
        return l;
    }

    /** Tests how well constant long definitions are straightened. */
    @Benchmark
    public int testStraighten2long() throws Exception {
        int dummy = 0;
        long[] arr = longArr;
        for (long l : arr) {
            dummy += innerCandidate2long(l);
        }
        return dummy;
    }

    private int innerCandidate3int(int i, int j) {
        int k = 0;

        if (i == j) {
            k++;
        }
        if (i != j) {
            k += 2;
        }
        if (i < j) {
            k += 4;
        }
        return k;
    }

    /**
     * Tests how well variable integer compares are straightened.
     */
    @Benchmark
    public int testStraighten3int() throws Exception {
        int dummy = 0;
        int[] arr = intArr;
        for (int i = 0; i < arr.length - 1; i++) {
            dummy += innerCandidate3int(arr[i], arr[i + 1]);
        }
        return dummy;
    }

    private long innerCandidate3long(long i, long j) {
        long k = 0;

        if (i == j) {
            k++;
        }
        if (i != j) {
            k += 2;
        }
        if (i < j) {
            k += 4;
        }
        return k;
    }

    /** Tests how well variable long compares are straightened. */
    @Benchmark
    public int testStraighten3long() throws Exception {
        int dummy = 0;
        long[] arr = longArr;
        for (int i = 0; i < arr.length - 1; i++) {
            dummy += innerCandidate3long(arr[i], arr[i + 1]);
        }
        return dummy;
    }

}
