/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class HashMapBench {
    private Supplier<Map<Integer, Integer>> mapSupplier;
    private Map<Integer, Integer> bigMapToAdd;

    @Param("1000000")
    private int size;

    @Param
    private MapType mapType;

    public enum MapType {
        HASH_MAP,
        LINKED_HASH_MAP,
    }

    @Setup
    public void setup() {
        switch (mapType) {
        case HASH_MAP:
            mapSupplier = () -> new HashMap<>();
            break;
        case LINKED_HASH_MAP:
            mapSupplier = () -> new LinkedHashMap<>();
            break;
        default:
            throw new AssertionError();
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.bigMapToAdd = IntStream.range(0, size).boxed()
            .collect(toMap(i -> 7 + i * 128, i -> rnd.nextInt()));
    }

    @Benchmark
    public int putAllWithBigMapToNonEmptyMap() {
        Map<Integer, Integer> map = mapSupplier.get();
        map.put(-1, -1);
        map.putAll(bigMapToAdd);
        return map.size();
    }

    @Benchmark
    public int putAllWithBigMapToEmptyMap() {
        Map<Integer, Integer> map = mapSupplier.get();
        map.putAll(bigMapToAdd);
        return map.size();
    }

    @Benchmark
    public int put() {
        Map<Integer, Integer> map = mapSupplier.get();
        for (int k : bigMapToAdd.keySet()) {
            map.put(k, bigMapToAdd.get(k));
        }
        return map.size();
    }
}
