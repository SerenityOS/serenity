/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.java.util.stream.tasks.PhoneCode;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.openjdk.bench.java.util.stream.tasks.PhoneCode.PhoneCodeProblem.wordsForNumber;

/**
 * This benchmark compare various strategies solving the phone code problem.
 * The result should offer some insights on strength/drawbacks of underlying
 * implementation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MINUTES)
@State(Scope.Benchmark)
public class Bulk {
    // several method choke up with 6-digit problem
    private final static int SIZE = 5;
    private Stream<String> join(String head,
                                String tail,
                                Function<String, Stream<String>> encoder)
    {
        Stream<String> s = wordsForNumber(head).stream();

        if (! tail.isEmpty()) {
            s = s.flatMap(h -> encoder.apply(tail).map(t -> h + " " + t));
        }

        return s;
    }

    private Stream<String> encode_par1(String number) {
        return IntStream.range(1, number.length() + 1)
                      .parallel()
                      .boxed()
                      .flatMap(i -> join(number.substring(0, i),
                              number.substring(i),
                              this::encode_par1));
    }

    private Stream<String> encode_par2(String number) {
        return IntStream.range(1, number.length() + 1)
                      .boxed()
                      .parallel()
                      .flatMap(i -> join(number.substring(0, i),
                                         number.substring(i),
                                         this::encode_par2));
    }

    private Stream<String> encode_ser(String number) {
        return IntStream.range(1, number.length() + 1)
                      .boxed()
                      .flatMap(i -> join(number.substring(0, i),
                                         number.substring(i),
                                         this::encode_ser));
    }

    private Stream<String> encode_loop_concat(String number) {
        if (number.isEmpty()) {
            return Stream.empty();
        }
        // full number
        Stream<String> s = wordsForNumber(number).stream();
        // split
        for (int i = 1; i < number.length(); i++) {
            s = Stream.concat(s, join(number.substring(0, i),
                                       number.substring(i),
                                       this::encode_loop_concat));
        }

        return s;
    }

    private Collection<String> encode_loop_collect(String number) {
        if (number.isEmpty()) {
            return Collections.emptySet();
        }

        Collection<String> rv = new HashSet<>();

        for (int i = 1; i <= number.length(); i++) {
            join(number.substring(0, i),
                 number.substring(i),
                 s -> encode_loop_collect(s).stream()).forEach(rv::add);
        }

        return rv;
    }

    private Collection<String> encode_inline(String number) {
        if (number.isEmpty()) {
            return Collections.emptySet();
        }

        Collection<String> rv = new HashSet<>();

        for (int i = 1; i < number.length(); i++) {
            String front = number.substring(0, i);
            String rest = number.substring(i);
            wordsForNumber(front).stream()
                .flatMap(h -> encode_inline(rest).stream().map(t -> h + " " + t))
                .forEach(rv::add);
        }

        rv.addAll(wordsForNumber(number));

        return rv;
    }

    @Benchmark
    public int bulk_par_range_concurrent() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_par1)
                               .collect(Collectors.toConcurrentMap(
                                    Function.identity(),
                                    Function.identity(),
                                    (l, r) -> l))
                               .keySet()
                               .size();
    }

    @Benchmark
    public int bulk_par_boxed_range_concurrent() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_par2)
                               .collect(Collectors.toConcurrentMap(
                                       Function.identity(),
                                       Function.identity(),
                                       (l, r) -> l))
                               .keySet()
                               .size();
    }

    @Benchmark
    public int bulk_par_range() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_par1)
                               .collect(Collectors.toSet())
                               .size();
    }

    @Benchmark
    public int bulk_par_boxed_range() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_par2)
                               .collect(Collectors.toSet())
                               .size();
    }

    @Benchmark
    public int bulk_ser_range() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_ser)
                               .collect(Collectors.toSet())
                               .size();
    }

    @Benchmark
    public int bulk_ser_loop_concat() {
        // force collect
        return PhoneCodeProblem.get(SIZE)
                               .flatMap(this::encode_loop_concat)
                               .collect(Collectors.toSet())
                               .size();
    }

    @Benchmark
    public int bulk_ser_loop_collect() {
        return PhoneCodeProblem.get(SIZE)
                               .map(this::encode_loop_collect)
                               .map(Collection::size)
                               .reduce(0, Integer::sum);
    }

    @Benchmark
    public int bulk_ser_inline() {
        return PhoneCodeProblem.get(SIZE)
                               .map(this::encode_inline)
                               .map(Collection::size)
                               .reduce(0, Integer::sum);
    }
}
