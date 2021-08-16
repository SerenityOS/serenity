/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Thread)
public class TreeMapUpdate {
    @Param({"TreeMap", "descendingMap", "tailMap"})
    public String mode;

    @Param({"10", "1000", "100000"})
    public int size;

    @Param({"true", "false"})
    public boolean comparator;

    @Param({"true", "false"})
    public boolean preFill;

    @Param({"0"})
    public long seed;

    private Supplier<TreeMap<Integer, Integer>> supplier;

    private UnaryOperator<NavigableMap<Integer, Integer>> transformer;

    private Integer[] keys;

    @Setup
    public void setUp() {
        switch(mode) {
            case "TreeMap":
                transformer = map -> map;
                break;
            case "descendingMap":
                transformer = map -> map.descendingMap();
                break;
            case "tailMap":
                transformer = map -> map.tailMap(0, true);
                break;
            default:
                throw new IllegalStateException(mode);
        }
        supplier = comparator ? () -> new TreeMap<>(Comparator.reverseOrder()) : TreeMap::new;
        keys = IntStream.range(0, size).boxed().toArray(Integer[]::new);
        Random rnd = seed == 0 ? new Random() : new Random(seed);
        Collections.shuffle(Arrays.asList(keys, rnd));
        if (preFill) {
            TreeMap<Integer, Integer> template = Arrays.stream(keys)
                .collect(Collectors.toMap(Function.identity(), Function.identity(), (a, b) -> a, supplier));
            supplier = () -> new TreeMap<>(template);
        }
    }

    @Benchmark
    public Map<Integer, Integer> baseline() {
        // Just create map (empty or pre-filled)
        return transformer.apply(supplier.get());
    }

    @Benchmark
    public Map<Integer, Integer> put(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.put(key, key));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> putIfAbsent(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.putIfAbsent(key, key));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> computeIfAbsent(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.computeIfAbsent(key, k -> k));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> compute(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.compute(key, (k, old) -> k));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> computeIfPresent(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.computeIfPresent(key, (k, old) -> k));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> merge(Blackhole bh) {
        Map<Integer, Integer> map = transformer.apply(supplier.get());
        Integer[] keys = this.keys;
        for (Integer key : keys) {
            bh.consume(map.merge(key, key, (k1, k2) -> k1));
        }
        return map;
    }
}
