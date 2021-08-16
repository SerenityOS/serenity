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
 * Detecting trailing whitespace is a very common problem that many programmers
 * have solved, but it's surprisingly difficult to avoid O(N^2) performance
 * when the input contains a long run of consecutive whitespace.  For
 * example, attempts to trim such whitespace caused a Stack Exchange outage.
 * https://stackstatus.net/post/147710624694/outage-postmortem-july-20-2016
 *
 * We use "[ \t]" as our definition of whitespace (easy, but not too easy!).
 *
 * The use of Matcher#find (instead of Matcher#matches) is very convenient, but
 * introduces an implicit O(N) loop over the input, or alternatively, a
 * non-possessive "^.*?" prefix in the regex.  In order for the entire search
 * operation to not be O(N^2), most of the regex match operations while
 * scanning the input need to be O(1), which may require the use of less-obvious
 * constructs like lookbehind.  The use of possessive quantifiers in the regex
 * itself is sadly **insufficient**.
 *
 * When the subpattern following a possessive quantifier is as cheap as the
 * subpattern governed by the quantifier (e.g. \s++$), the possessive quantifier
 * gives you at most 2x speedup, reducing two linear scans to one.
 *
 * An explicit loop with find() using two matchers and possessive quantifiers is
 * the most efficient, since there is no backtracking.  But that cannot work with
 * simple APIs that take a regex as an argument, like grep(1) does.
 *
 * Here's a way to compare the per-char cost:
 *
 * (cd $(git rev-parse --show-toplevel) && for size in 16 256 4096; do make test TEST='micro:java.util.regex.Trim' MICRO="FORK=1;WARMUP_ITER=1;ITER=4;OPTIONS=-opi $size -p size=$size" |& perl -ne 'print if /^Benchmark/ .. /^Finished running test/'; done)
 *
 * some jdk17 numbers:
 *
 * Benchmark                    (size)  Mode  Cnt     Score    Error  Units
 * Trim.find_loop_two_matchers    1024  avgt    8     2.252 ?  0.013  ns/op
 * Trim.find_loop_usePattern      1024  avgt    8     2.328 ?  0.116  ns/op
 * Trim.lookBehind_find           1024  avgt    8    21.740 ?  0.040  ns/op
 * Trim.possessive2_find          1024  avgt    8  7151.592 ? 17.860  ns/op
 * Trim.possessive2_matches       1024  avgt    8     2.625 ?  0.008  ns/op
 * Trim.possessive3_find          1024  avgt    8    28.532 ?  1.889  ns/op
 * Trim.possessive_find           1024  avgt    8  3113.776 ?  9.996  ns/op
 * Trim.simple_find               1024  avgt    8  4199.480 ? 13.410  ns/op
 *
 * TODO: why is simple_find faster than possessive_find, for size below 512 ?
 *
 * (cd $(git rev-parse --show-toplevel) && for size in 128 256 512 1024 2048; do make test TEST='micro:java.util.regex.Trim.\\\(simple_find\\\|possessive_find\\\)' MICRO="FORK=2;WARMUP_ITER=1;ITER=4;OPTIONS=-opi $size -p size=$size" |& perl -ne 'print if /^Benchmark/ .. /^Finished running test/'; done)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 4)
@State(Scope.Benchmark)
public class Trim {
    /** Run length of non-matching consecutive whitespace chars. */
    @Param({"16", "256", "4096"})
    int size;

    /** String containing long interior run of whitespace */
    public String noMatch;

    public Pattern whitespaceRunPattern;
    public Pattern eolPattern;

    public Pattern simplePattern;
    public Pattern possessivePattern;
    public Pattern possessivePattern2;
    public Pattern possessivePattern3;
    public Pattern lookBehindPattern;

    Pattern compile(String regex) {
        Pattern pat = Pattern.compile(regex);
        // ad hoc correctness checking
        if (pat.matcher(noMatch).find()) {
            throw new AssertionError("unexpected matching: " + regex);
        }
        return pat;
    }

    @Setup(Level.Trial)
    public void setup() {
        noMatch = "xx" + " \t".repeat(size) + "yy";

        simplePattern = compile("[ \t]+$");
        possessivePattern = compile("[ \t]++$");
        possessivePattern2 = compile("(.*+[^ \t]|^)([ \t]++)$");
        possessivePattern3 = compile("(?:[^ \t]|^)([ \t]++)$");
        lookBehindPattern = compile("(?<![ \t])[ \t]++$");

        whitespaceRunPattern = Pattern.compile("[ \t]++");
        eolPattern = Pattern.compile("$", Pattern.MULTILINE);

        // more ad hoc correctness checking
        if (possessive2_matches()) throw new AssertionError();
        if (find_loop_two_matchers()) throw new AssertionError();
        if (find_loop_usePattern()) throw new AssertionError();
    }

    @Benchmark
    public boolean simple_find() {
        return simplePattern.matcher(noMatch).find();
    }

    @Benchmark
    public boolean possessive_find() {
        return possessivePattern.matcher(noMatch).find();
    }

    @Benchmark
    public boolean possessive2_find() {
        return possessivePattern2.matcher(noMatch).find();
    }

    @Benchmark
    public boolean possessive2_matches() {
        return possessivePattern2.matcher(noMatch).matches();
    }

    @Benchmark
    public boolean possessive3_find() {
        return possessivePattern3.matcher(noMatch).find();
    }

    @Benchmark
    public boolean lookBehind_find() {
        return lookBehindPattern.matcher(noMatch).find();
    }

    @Benchmark
    public boolean find_loop_two_matchers() {
        Matcher m = whitespaceRunPattern.matcher(noMatch);
        int endOfString = m.regionEnd();
        while (m.find()) {
            if (eolPattern.matcher(noMatch).region(m.end(), endOfString).lookingAt())
                return true;
        }
        return false;
    }

    @Benchmark
    public boolean find_loop_usePattern() {
        Matcher m = whitespaceRunPattern.matcher(noMatch);
        int endOfString = m.regionEnd();
        while (m.find()) {
            m.region(m.end(), endOfString);
            m.usePattern(eolPattern);
            if (m.lookingAt())
                return true;
            m.usePattern(whitespaceRunPattern);
        }
        return false;
    }

}
