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
package org.openjdk.bench.java.util.stream.tasks.DictionaryWordValue;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Bulk scenario: searching max "wordValue" through the dictionary (array of strings).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Lambda {

    @Setup(Level.Trial)
    public void loadData() {
        // cause classload and problem initialization
        DictionaryProblem.class.getName();
    }

    public static Integer maxInt(Integer l, Integer r) {
        return l > r ? l : r;
    }

    @Benchmark
    public int bulk_seq_lambda() {
        return Arrays.stream(DictionaryProblem.get())
                .map(s -> DictionaryProblem.wordValue(s))
                .reduce(0, (l, r) -> l > r ? l : r);
    }

    @Benchmark
    public int bulk_seq_mref() {
        return Arrays.stream(DictionaryProblem.get())
                .map(DictionaryProblem::wordValue)
                .reduce(0, Lambda::maxInt);
    }

    @Benchmark
    public int bulk_par_lambda() {
        return Arrays.stream(DictionaryProblem.get()).parallel()
                .map(s -> DictionaryProblem.wordValue(s))
                .reduce(0, (l, r) -> l > r ? l : r);
    }

    @Benchmark
    public int bulk_par_mref() {
        return Arrays.stream(DictionaryProblem.get()).parallel()
                .map(DictionaryProblem::wordValue)
                .reduce(0, Lambda::maxInt);
    }

}
