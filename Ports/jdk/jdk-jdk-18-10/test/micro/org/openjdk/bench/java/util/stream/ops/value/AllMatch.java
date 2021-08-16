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
package org.openjdk.bench.java.util.stream.ops.value;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

/**
 * Benchmark for allMatch() operation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class AllMatch {

    /**
     * Implementation notes:
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     *   - the predicates are always true to avert shortcurcuiting
     */

    @Param("100000")
    private int size;

    private LongPredicate p1, p2, p3;

    @Setup
    public void setup() {
        p1 = new LongPredicate() {
            @Override
            public boolean test(long v) {
                return true;
            }
        };
        p2 = new LongPredicate() {
            @Override
            public boolean test(long v) {
                return true;
            }
        };
        p3 = new LongPredicate() {
            @Override
            public boolean test(long v) {
                return true;
            }
        };
    }

    @Benchmark
    public boolean seq_invoke() {
        return LongStream.range(0, size).allMatch(p1);
    }

    @Benchmark
    public int seq_chain111() {
        int s = 0;
        s += (LongStream.range(0, size).allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).allMatch(p1)) ? 1 : 0;
        return s;
    }

    @Benchmark
    public int seq_chain123() {
        int s = 0;
        s += (LongStream.range(0, size).allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).allMatch(p2)) ? 1 : 0;
        s += (LongStream.range(0, size).allMatch(p3)) ? 1 : 0;
        return s;
    }

    @Benchmark
    public boolean par_invoke() {
        return LongStream.range(0, size).parallel().allMatch(p1);
    }

    @Benchmark
    public int par_chain111() {
        int s = 0;
        s += (LongStream.range(0, size).parallel().allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).parallel().allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).parallel().allMatch(p1)) ? 1 : 0;
        return s;
    }

    @Benchmark
    public int par_chain123() {
        int s = 0;
        s += (LongStream.range(0, size).parallel().allMatch(p1)) ? 1 : 0;
        s += (LongStream.range(0, size).parallel().allMatch(p2)) ? 1 : 0;
        s += (LongStream.range(0, size).parallel().allMatch(p3)) ? 1 : 0;
        return s;
    }

}
