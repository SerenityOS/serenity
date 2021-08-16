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
package org.openjdk.bench.java.util.stream.ops.ref;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.LongStream;

/**
 * Benchmark for noneMatch() operation.
 * Focuses on short-circuiting behavior.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class NoneMatchShort {

    /**
     * Implementation notes:
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     *   - test the predicate which will become true closer to start, in the middle, and closer to the end
     */

    @Param("100000")
    private int size;

    @Param("100")
    private int offset;

    private Predicate<Long> pMid, pStart, pEnd;

    @Setup
    public void setup() {
        pStart = new Predicate<Long>() {
            @Override
            public boolean test(Long v) {
                return v > offset;
            }
        };
        pMid = new Predicate<Long>() {
            @Override
            public boolean test(Long v) {
                return v > size / 2;
            }
        };
        pEnd = new Predicate<Long>() {
            @Override
            public boolean test(Long v) {
                return v > size - offset;
            }
        };
    }

    @Benchmark
    public boolean seq_start() {
        return LongStream.range(0, size).boxed().noneMatch(pStart);
    }

    @Benchmark
    public boolean seq_mid() {
        return LongStream.range(0, size).boxed().noneMatch(pMid);
    }

    @Benchmark
    public boolean seq_end() {
        return LongStream.range(0, size).boxed().noneMatch(pEnd);
    }

    @Benchmark
    public boolean par_start() {
        return LongStream.range(0, size).parallel().boxed().noneMatch(pStart);
    }

    @Benchmark
    public boolean par_mid() {
        return LongStream.range(0, size).parallel().boxed().noneMatch(pMid);
    }

    @Benchmark
    public boolean par_end() {
        return LongStream.range(0, size).parallel().boxed().noneMatch(pEnd);
    }

}
