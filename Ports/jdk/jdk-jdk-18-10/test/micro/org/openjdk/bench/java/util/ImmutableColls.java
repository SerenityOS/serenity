/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Micros for the various collections implemented in
 * java.util.ImmutableCollections
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class ImmutableColls {

    public static String[] STRINGS = {"hi", "all", "of", "you"};

    public static List<String> l0 = List.of();
    public static List<String> l1 = List.of(Arrays.copyOf(STRINGS, 1));
    public static List<String> l2 = List.of(Arrays.copyOf(STRINGS, 2));
    public static List<String> l3 = List.of(Arrays.copyOf(STRINGS, 3));
    public static List<String> l4 = List.of(Arrays.copyOf(STRINGS, 4));

    public static Set<String> s0 = Set.copyOf(l0);
    public static Set<String> s1 = Set.copyOf(l1);
    public static Set<String> s2 = Set.copyOf(l2);
    public static Set<String> s3 = Set.copyOf(l3);
    public static Set<String> s4 = Set.copyOf(l4);

    public static Map<String, String> m0 = Map.of();
    public static Map<String, String> m1 = Map.of(STRINGS[0], STRINGS[0]);
    public static Map<String, String> m2 = Map.of(STRINGS[0], STRINGS[0],
                                                  STRINGS[1], STRINGS[1]);
    public static Map<String, String> m3 = Map.of(STRINGS[0], STRINGS[0],
                                                  STRINGS[1], STRINGS[1],
                                                  STRINGS[2], STRINGS[2]);
    public static Map<String, String> m4 = Map.of(STRINGS[0], STRINGS[0],
                                                  STRINGS[1], STRINGS[1],
                                                  STRINGS[2], STRINGS[2],
                                                  STRINGS[3], STRINGS[3]);

    public static List<String> a0 = new ArrayList<>(l0);
    public static List<String> a1 = new ArrayList<>(l1);
    public static List<String> a2 = new ArrayList<>(l2);
    public static List<String> a3 = new ArrayList<>(l3);
    public static List<String> a4 = new ArrayList<>(l4);

    public static final List<String> fl0 = List.of();
    public static final List<String> fl1 = List.copyOf(l1);
    public static final List<String> fl2 = List.copyOf(l2);
    public static final List<String> fl3 = List.copyOf(l3);
    public static final List<String> fl4 = List.copyOf(l4);

    public static final List<String> fsl0 = fl0.subList(0, 0);
    public static final List<String> fsl1 = fl2.subList(1, 2);
    public static final List<String> fsl2 = fl3.subList(0, 2);
    public static final List<String> fsl3 = fl4.subList(0, 3);

    public static final Set<String> fs0 = Set.copyOf(l0);
    public static final Set<String> fs1 = Set.copyOf(l1);
    public static final Set<String> fs2 = Set.copyOf(l2);
    public static final Set<String> fs3 = Set.copyOf(l3);
    public static final Set<String> fs4 = Set.copyOf(l4);

    public static final Map<String, String> fm0 = Map.copyOf(m0);
    public static final Map<String, String> fm1 = Map.copyOf(m1);
    public static final Map<String, String> fm2 = Map.copyOf(m2);
    public static final Map<String, String> fm3 = Map.copyOf(m3);
    public static final Map<String, String> fm4 = Map.copyOf(m4);

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void constructLists(Blackhole bh) {
        bh.consume(List.of(STRINGS[0]));
        bh.consume(List.of(STRINGS[0], STRINGS[1]));
        bh.consume(List.of(STRINGS[0], STRINGS[1], STRINGS[2]));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void constructSets(Blackhole bh) {
        bh.consume(Set.of(STRINGS[0]));
        bh.consume(Set.of(STRINGS[0], STRINGS[1]));
        bh.consume(Set.of(STRINGS[0], STRINGS[1], STRINGS[2]));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int sumSizesList() {
        return sizeOf(l0) +
                sizeOf(l1) +
                sizeOf(l2) +
                sizeOf(l3) +
                sizeOf(l4);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int sumSizesFinalList() {
        return sizeOf(fl0) +
                sizeOf(fl1) +
                sizeOf(fl2) +
                sizeOf(fl3) +
                sizeOf(fl4);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int sumSizesSet() {
        return sizeOf(s0) +
                sizeOf(s1) +
                sizeOf(s2) +
                sizeOf(s3) +
                sizeOf(s4);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int sumSizesFinalSet() {
        return sizeOf2(fs0) +
                sizeOf2(fs1) +
                sizeOf2(fs2) +
                sizeOf2(fs3) +
                sizeOf2(fs4);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean emptyFinalSet() {
        return fs0.isEmpty() &
                fs1.isEmpty() &
                fs2.isEmpty() &
                fs3.isEmpty() &
                fs4.isEmpty();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean emptyFinalList() {
        return fl0.isEmpty() &
                fl1.isEmpty() &
                fl2.isEmpty() &
                fl3.isEmpty() &
                fl4.isEmpty();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean emptyFinalMap() {
        return fm0.isEmpty() &
                fm1.isEmpty() &
                fm2.isEmpty() &
                fm3.isEmpty() &
                fm4.isEmpty();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean containsFinalSet() {
        return fs0.contains("hi") &
                fs1.contains("hi") &
                fs2.contains("hi") &
                fs3.contains("hi") &
                fs4.contains("hi");
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean containsFinalList() {
        return fl0.contains("hi") &
                fl1.contains("hi") &
                fl2.contains("hi") &
                fl3.contains("hi") &
                fl4.contains("hi");
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean containsKeyFinalMap() {
        return fm0.containsKey("hi") &
                fm1.containsKey("hi") &
                fm2.containsKey("hi") &
                fm3.containsKey("hi") &
                fm4.containsKey("hi");
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean containsValueFinalMap() {
        return fm0.containsValue("hi") &
                fm1.containsValue("hi") &
                fm2.containsValue("hi") &
                fm3.containsValue("hi") &
                fm4.containsValue("hi");
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void getOrDefault(Blackhole bh) {
        bh.consume(fm4.getOrDefault("hi", "test"));
        bh.consume(fm4.getOrDefault("not_in_this_map", "test"));
    }

    public int sizeOf(List<String> list) {
        return list.size();
    }

    /**
     * Same as sizeOf(List), but duplicated to avoid any
     * potential profile pollution with tests using
     * sizeOf.
     */
    public int sizeOf2(List<String> list) {
        return list.size();
    }

    public int sizeOf(Set<String> set) {
        return set.size();
    }

    /**
     * Same as sizeOf(Set), but duplicated to avoid any
     * potential profile pollution with tests using
     * sizeOf.
     */
    public int sizeOf2(Set<String> set) {
        return set.size();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int getFromList() {
        return get(l1, 0).length() +
                get(l2, 1).length() +
                get(l3, 2).length() +
                get(l4, 3).length();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int finalGetFromList() {
        return get(fl1, 0).length() +
                get(fl2, 1).length() +
                get(fl3, 2).length() +
                get(fl4, 3).length();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toArrayFromSet(Blackhole bh) {
        bh.consume(fs4.toArray());
        bh.consume(s1.toArray());
        bh.consume(s3.toArray());
        bh.consume(fs2.toArray());
        bh.consume(s0.toArray());
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void iterateOverSet(Blackhole bh) {
        iterateSet(bh, fs4);
        iterateSet(bh, s1);
        iterateSet(bh, s3);
        iterateSet(bh, fs2);
        iterateSet(bh, s0);
    }

    public void iterateSet(Blackhole bh, Set<String> coll) {
        for (String s : coll) {
            bh.consume(s);
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toArrayFromMap(Blackhole bh) {
        bh.consume(fm4.entrySet().toArray());
        bh.consume(m1.entrySet().toArray());
        bh.consume(m3.entrySet().toArray());
        bh.consume(fm2.entrySet().toArray());
        bh.consume(m0.entrySet().toArray());
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toArrayFromList(Blackhole bh) {
        bh.consume(fl4.toArray());
        bh.consume(fl1.toArray());
        bh.consume(l3.toArray());
        bh.consume(l0.toArray());
        bh.consume(fsl3.toArray());
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toTypedArrayFromSet(Blackhole bh) {
        bh.consume(fs4.toArray(new String[0]));
        bh.consume(s1.toArray(new String[0]));
        bh.consume(s3.toArray(new String[0]));
        bh.consume(fs2.toArray(new String[0]));
        bh.consume(s0.toArray(new String[0]));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toTypedArrayFromMap(Blackhole bh) {
        bh.consume(fm4.entrySet().toArray(new Map.Entry[0]));
        bh.consume(m1.entrySet().toArray(new Map.Entry[0]));
        bh.consume(m3.entrySet().toArray(new Map.Entry[0]));
        bh.consume(fm2.entrySet().toArray(new Map.Entry[0]));
        bh.consume(m0.entrySet().toArray(new Map.Entry[0]));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void toTypedArrayFromList(Blackhole bh) {
        bh.consume(fl4.toArray(new String[0]));
        bh.consume(fl1.toArray(new String[0]));
        bh.consume(l3.toArray(new String[0]));
        bh.consume(l0.toArray(new String[0]));
        bh.consume(fsl3.toArray(new String[0]));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void copyOfLists(Blackhole bh) {
        bh.consume(List.copyOf(fl1));
        bh.consume(List.copyOf(fl4));
        bh.consume(List.copyOf(fsl2));
        bh.consume(List.copyOf(fsl3));
    }

    public String get2(List<String> list, int idx) {
        return list.get(idx);
    }

    public String get(List<String> list, int idx) {
        return list.get(idx);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public Set<String> createSetOf() {
        return Set.of(STRINGS);
    }
}
