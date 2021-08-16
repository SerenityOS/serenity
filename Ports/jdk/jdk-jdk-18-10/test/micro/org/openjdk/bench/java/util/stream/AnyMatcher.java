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
package org.openjdk.bench.java.util.stream;

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
 * Benchmark for checking different "anyMatch" schemes.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class AnyMatcher {

    /**
     * Implementation notes:
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     *   - all operations have similar semantics
     */

    @Param("100000")
    private int size;

    private LongPredicate op;

    @Setup
    public void setup() {
        op = new LongPredicate() {
            @Override
            public boolean test(long x) {
                return false;
            }
        };
    }

    @Benchmark
    public boolean seq_anyMatch() {
        return LongStream.range(0, size).anyMatch(op);
    }

    @Benchmark
    public boolean seq_filter_findFirst() {
        return LongStream.range(0, size).filter(op).findFirst().isPresent();
    }

    @Benchmark
    public boolean seq_filter_findAny() {
        return LongStream.range(0, size).filter(op).findAny().isPresent();
    }

    @Benchmark
    public boolean par_anyMatch() {
        return LongStream.range(0, size).parallel().anyMatch(op);
    }

    @Benchmark
    public boolean par_filter_findFirst() {
        return LongStream.range(0, size).parallel().filter(op).findFirst().isPresent();
    }

    @Benchmark
    public boolean par_filter_findAny() {
        return LongStream.range(0, size).parallel().filter(op).findAny().isPresent();
    }

}
