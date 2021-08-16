/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@State(Scope.Thread)
public class HashMapToArray {

    @Param({"0", "1", "10", "1000", "100000"})
    public int size;

    @Param({"HashMap", "LinkedHashMap"})
    public String mapType;

    private Map<Integer, Integer> map;

    @Setup
    public void setup() {
        switch (mapType) {
            case "HashMap":
                map = new HashMap<>();
                break;
            case "LinkedHashMap":
                map = new LinkedHashMap<>();
                break;
            default:
                throw new IllegalStateException();
        }
        for (int i = 0; i < size; i++) {
            map.put(i, i * i);
        }
    }

    @Benchmark
    public Object[] testKeySetToArray() {
        return map.keySet().toArray();
    }

    @Benchmark
    public Object[] testKeySetToArrayTyped() {
        return map.keySet().toArray(new Integer[0]);
    }

    @Benchmark
    public Object[] testValuesToArray() {
        return map.values().toArray();
    }

    @Benchmark
    public Object[] testValuesToArrayTyped() {
        return map.values().toArray(new Integer[0]);
    }
}
