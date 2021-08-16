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
import java.util.regex.Pattern;

/**
 * Abusing regexes for fun primality testing.
 * Famous among regex enthusiasts.
 * https://stackoverflow.com/q/3296050/625403
 *
 * Prime numbers exhibit O(N^2) performance with all variants, due to exhaustive
 * backtracking.
 *
 * Powers of two exhibit O(N) performance with all variants, with reluctant
 * quantifiers doing somewhat better.
 *
 * Here's a way to compare the per-input-char cost:
 *
 * (cd $(git rev-parse --show-toplevel) && for n in 16 17 256 257 4096 4099; do make test TEST='micro:java.util.regex.Primality' MICRO="FORK=1;WARMUP_ITER=1;ITER=4;OPTIONS=-opi $n -p n=$n" |& perl -ne 'print if /^Benchmark/ .. /^Finished running test/'; done)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 4)
@State(Scope.Benchmark)
public class Primality {
    /** Number to be primality tested. */
    @Param({"16", "17", "256", "257", "4096", "4099"})
    //  "64", "67", "1024", "1031", "16384", "16411"})
    int n;

    /** Unary numeral representation of int n */
    public String unary;

    // Patterns that match composite numbers represented as unary numerals.
    public Pattern reluctant1;
    public Pattern reluctant2;
    public Pattern greedy1;
    public Pattern greedy2;

    Pattern compile(String regex) {
        Pattern pat = Pattern.compile(regex);
        // ad hoc correctness checking
        boolean isPrime1 = ! pat.matcher(unary).matches();
        boolean isPrime2 = java.math.BigInteger.valueOf(n).isProbablePrime(100);
        if (isPrime1 != isPrime2) {
            throw new AssertionError("regex=" + regex + ", n=" + n);
        }
        return pat;
    }

    @Setup(Level.Trial)
    public void setup() {
        unary = "1".repeat(n);

        reluctant1 = compile("^(11+?)\\1+$");
        reluctant2 = compile("^(1{2,}?)\\1+$");
        greedy1 = compile("^(11+)\\1+$");
        greedy2 = compile("^(1{2,})\\1+$");
    }

    @Benchmark
    public boolean reluctant1() {
        return reluctant1.matcher(unary).matches();
    }

    @Benchmark
    public boolean reluctant2() {
        return reluctant2.matcher(unary).matches();
    }

    @Benchmark
    public boolean greedy1() {
        return greedy1.matcher(unary).matches();
    }

    @Benchmark
    public boolean greedy2() {
        return greedy2.matcher(unary).matches();
    }
}
