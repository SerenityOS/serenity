/*
 * Copyright 2020 Google Inc.  All Rights Reserved.
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
package org.openjdk.bench.java.util.regex;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Benchmarks of Patterns that exhibit O(2^N) performance due to catastrophic
 * backtracking, **when implemented naively**.
 *
 * See: jdk/test/java/util/regex/RegExTest.java#expoBacktracking
 * commit b45ea8903ec290ab194d9ebe040bc43edd5dd0a3
 * Author: Xueming Shen <sherman@openjdk.org>
 * Date:   Tue May 10 21:19:25 2016 -0700
 *
 * Here's a way to compare the per-char cost:
 *
 * (cd $(git rev-parse --show-toplevel) && for size in 16 128 1024; do make test TEST='micro:java.util.regex.Exponential' MICRO="FORK=1;WARMUP_ITER=1;ITER=4;OPTIONS=-opi $size -p size=$size" |& perl -ne 'print if /^Benchmark/ .. /^Finished running test/'; done)
 *
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 4)
@State(Scope.Benchmark)
public class Exponential {
    /** Run length of non-matching consecutive whitespace chars. */
    @Param({"16", "128", "1024"})
    // 2048+ runs into StackOverflowError; see JDK-8260866
    int size;

    public String justXs;
    public String notJustXs;

    // Patterns that match justXs but not notJustXs
    public Pattern pat1;
    public Pattern pat2;
    public Pattern pat3;
    public Pattern pat4;

    Pattern compile(String regex) {
        Pattern pat = Pattern.compile(regex);
        // ad hoc correctness checking
        if (!  pat.matcher(justXs).matches()
            || pat.matcher(notJustXs).matches()) {
            throw new AssertionError("unexpected matching: " + regex);
        }
        return pat;
    }

    @Setup(Level.Trial)
    public void setup() {
        justXs = "X".repeat(size);
        notJustXs = justXs + "!";

        // Will (or should) the engine optimize (?:X|X) to X ?
        pat1 = compile("(?:X|X)*");

        // Tougher to optimize than pat1
        pat2 = compile("(?:[XY]|[XZ])*");

        pat3 = compile("(X+)+");

        pat4 = compile("^(X+)+$");
     }

    /** O(N) */
    @Benchmark
    public boolean pat1_justXs() {
        return pat1.matcher(justXs).matches();
    }

    /** O(N) */
    @Benchmark
    public boolean pat1_notJustXs() {
        return pat1.matcher(notJustXs).matches();
    }

    /** O(N) */
    @Benchmark
    public boolean pat2_justXs() {
        return pat2.matcher(justXs).matches();
    }

    /** O(N) */
    @Benchmark
    public boolean pat2_notJustXs() {
        return pat2.matcher(notJustXs).matches();
    }

    /** O(1) - very surprising! */
    @Benchmark
    public boolean pat3_justXs() {
        return pat3.matcher(justXs).matches();
    }

    /** O(N^2) - surprising!  O(N) seems very achievable. */
    @Benchmark
    public boolean pat3_notJustXs() {
        return pat3.matcher(notJustXs).matches();
    }

    /** O(1) - very surprising! */
    @Benchmark
    public boolean pat4_justXs() {
        return pat4.matcher(justXs).matches();
    }

    /** O(N^2) - surprising!  O(N) seems very achievable. */
    @Benchmark
    public boolean pat4_notJustXs() {
        return pat4.matcher(notJustXs).matches();
    }

}
