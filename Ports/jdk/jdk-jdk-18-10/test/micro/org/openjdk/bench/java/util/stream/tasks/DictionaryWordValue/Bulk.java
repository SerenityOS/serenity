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
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;


/**
 * Bulk scenario: searching max "wordValue" through the dictionary (array of strings).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Bulk {

    @Setup(Level.Trial)
    public void loadData() {
        // cause classload and problem initialization
        DictionaryProblem.class.getName();
    }

    @Benchmark
    public int hm_seq() {
        int max = 0;
        for (String s : DictionaryProblem.get()) {
            int v = DictionaryProblem.wordValue(s);
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    @Benchmark
    public int hm_par() {
        return new Task(DictionaryProblem.get(), 0, DictionaryProblem.get().length).invoke();
    }

    @Benchmark
    public int bulk_seq_inner() {
        return Arrays.stream(DictionaryProblem.get())
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(String s) {
                        return DictionaryProblem.wordValue(s);
                    }
                })
                .reduce(0, new BinaryOperator<Integer>() {
                    @Override
                    public Integer apply(Integer l, Integer r) {
                        return l > r ? l : r;
                    }
                });
    }

    @Benchmark
    public int bulk_par_inner() {
        return Arrays.stream(DictionaryProblem.get()).parallel()
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(String s) {
                        return DictionaryProblem.wordValue(s);
                    }
                })
                .reduce(0, new BinaryOperator<Integer>() {
                    @Override
                    public Integer apply(Integer l, Integer r) {
                        return l > r ? l : r;
                    }
                });
    }


    public static class Task extends RecursiveTask<Integer> {
        private static final int FORK_LIMIT = 500;

        private final String[] words;
        private final int start, end;

        Task(String[] w, int start, int end) {
            this.words = w;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            int size = end - start;
            if (size > FORK_LIMIT) {
                int mid = start + size / 2;
                Task t1 = new Task(words, start, mid);
                Task t2 = new Task(words, mid, end);
                t1.fork();
                Integer v2 = t2.invoke();
                Integer v1 = t1.join();
                return Math.max(v1, v2);
            } else {
                int max = 0;
                for (int i = start; i < end; i++) {
                    int v = DictionaryProblem.wordValue(words[i]);
                    if (v > max) {
                        max = v;
                    }
                }
                return max;
            }
        }
    }


}
