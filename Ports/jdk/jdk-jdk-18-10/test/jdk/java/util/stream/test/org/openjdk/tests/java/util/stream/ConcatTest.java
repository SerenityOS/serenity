/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.util.stream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.LambdaTestHelpers.*;
import static org.testng.Assert.*;

@Test
public class ConcatTest {
    private static Object[][] cases;

    static {
        List<Integer> part1 = Arrays.asList(5, 3, 4, 1, 2, 6, 2, 4);
        List<Integer> part2 = Arrays.asList(8, 8, 6, 6, 9, 7, 10, 9);
        List<Integer> p1p2 = Arrays.asList(5, 3, 4, 1, 2, 6, 2, 4, 8, 8, 6, 6, 9, 7, 10, 9);
        List<Integer> p2p1 = Arrays.asList(8, 8, 6, 6, 9, 7, 10, 9, 5, 3, 4, 1, 2, 6, 2, 4);
        List<Integer> empty = new LinkedList<>(); // To be ordered
        assertTrue(empty.isEmpty());
        LinkedHashSet<Integer> distinctP1 = new LinkedHashSet<>(part1);
        LinkedHashSet<Integer> distinctP2 = new LinkedHashSet<>(part2);
        TreeSet<Integer> sortedP1 = new TreeSet<>(part1);
        TreeSet<Integer> sortedP2 = new TreeSet<>(part2);

        cases = new Object[][] {
            { "regular", part1, part2, p1p2 },
            { "reverse regular", part2, part1, p2p1 },
            { "front distinct", distinctP1, part2, Arrays.asList(5, 3, 4, 1, 2, 6, 8, 8, 6, 6, 9, 7, 10, 9) },
            { "back distinct", part1, distinctP2, Arrays.asList(5, 3, 4, 1, 2, 6, 2, 4, 8, 6, 9, 7, 10) },
            { "both distinct", distinctP1, distinctP2, Arrays.asList(5, 3, 4, 1, 2, 6, 8, 6, 9, 7, 10) },
            { "front sorted", sortedP1, part2, Arrays.asList(1, 2, 3, 4, 5, 6, 8, 8, 6, 6, 9, 7, 10, 9) },
            { "back sorted", part1, sortedP2, Arrays.asList(5, 3, 4, 1, 2, 6, 2, 4, 6, 7, 8, 9, 10) },
            { "both sorted", sortedP1, sortedP2, Arrays.asList(1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10) },
            { "reverse both sorted", sortedP2, sortedP1, Arrays.asList(6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6) },
            { "empty something", empty, part1, part1 },
            { "something empty", part1, empty, part1 },
            { "empty empty", empty, empty, empty }
        };
    }

    @DataProvider(name = "cases")
    private static Object[][] getCases() {
        return cases;
    }

    @Factory(dataProvider = "cases")
    public static Object[] createTests(String scenario, Collection<Integer> c1, Collection<Integer> c2, Collection<Integer> expected) {
        return new Object[] {
            new ConcatTest(scenario, c1, c2, expected)
        };
    }

    protected final String scenario;
    protected final Collection<Integer> c1;
    protected final Collection<Integer> c2;
    protected final Collection<Integer> expected;

    public ConcatTest(String scenario, Collection<Integer> c1, Collection<Integer> c2, Collection<Integer> expected) {
        this.scenario = scenario;
        this.c1 = c1;
        this.c2 = c2;
        this.expected = expected;

        // verify prerequisite
        Stream<Integer> s1s = c1.stream();
        Stream<Integer> s2s = c2.stream();
        Stream<Integer> s1p = c1.parallelStream();
        Stream<Integer> s2p = c2.parallelStream();
        assertTrue(s1p.isParallel());
        assertTrue(s2p.isParallel());
        assertFalse(s1s.isParallel());
        assertFalse(s2s.isParallel());

        assertTrue(s1s.spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertTrue(s1p.spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertTrue(s2s.spliterator().hasCharacteristics(Spliterator.ORDERED));
        assertTrue(s2p.spliterator().hasCharacteristics(Spliterator.ORDERED));
    }

    private <T> void assertConcatContent(Spliterator<T> sp, boolean ordered, Spliterator<T> expected) {
        // concat stream cannot guarantee uniqueness
        assertFalse(sp.hasCharacteristics(Spliterator.DISTINCT), scenario);
        // concat stream cannot guarantee sorted
        assertFalse(sp.hasCharacteristics(Spliterator.SORTED), scenario);
        // concat stream is ordered if both are ordered
        assertEquals(sp.hasCharacteristics(Spliterator.ORDERED), ordered, scenario);

        // Verify elements
        if (ordered) {
            assertEquals(toBoxedList(sp),
                         toBoxedList(expected),
                         scenario);
        } else {
            assertEquals(toBoxedMultiset(sp),
                         toBoxedMultiset(expected),
                         scenario);
        }
    }

    private void assertRefConcat(Stream<Integer> s1, Stream<Integer> s2, boolean parallel, boolean ordered) {
        Stream<Integer> result = Stream.concat(s1, s2);
        assertEquals(result.isParallel(), parallel);
        assertConcatContent(result.spliterator(), ordered, expected.spliterator());
    }

    private void assertIntConcat(Stream<Integer> s1, Stream<Integer> s2, boolean parallel, boolean ordered) {
        IntStream result = IntStream.concat(s1.mapToInt(Integer::intValue),
                                            s2.mapToInt(Integer::intValue));
        assertEquals(result.isParallel(), parallel);
        assertConcatContent(result.spliterator(), ordered,
                            expected.stream().mapToInt(Integer::intValue).spliterator());
    }

    private void assertLongConcat(Stream<Integer> s1, Stream<Integer> s2, boolean parallel, boolean ordered) {
        LongStream result = LongStream.concat(s1.mapToLong(Integer::longValue),
                                              s2.mapToLong(Integer::longValue));
        assertEquals(result.isParallel(), parallel);
        assertConcatContent(result.spliterator(), ordered,
                            expected.stream().mapToLong(Integer::longValue).spliterator());
    }

    private void assertDoubleConcat(Stream<Integer> s1, Stream<Integer> s2, boolean parallel, boolean ordered) {
        DoubleStream result = DoubleStream.concat(s1.mapToDouble(Integer::doubleValue),
                                                  s2.mapToDouble(Integer::doubleValue));
        assertEquals(result.isParallel(), parallel);
        assertConcatContent(result.spliterator(), ordered,
                            expected.stream().mapToDouble(Integer::doubleValue).spliterator());
    }

    public void testRefConcat() {
        // sequential + sequential -> sequential
        assertRefConcat(c1.stream(), c2.stream(), false, true);
        // parallel + parallel -> parallel
        assertRefConcat(c1.parallelStream(), c2.parallelStream(), true, true);
        // sequential + parallel -> parallel
        assertRefConcat(c1.stream(), c2.parallelStream(), true, true);
        // parallel + sequential -> parallel
        assertRefConcat(c1.parallelStream(), c2.stream(), true, true);

        // not ordered
        assertRefConcat(c1.stream().unordered(), c2.stream(), false, false);
        assertRefConcat(c1.stream(), c2.stream().unordered(), false, false);
        assertRefConcat(c1.parallelStream().unordered(), c2.stream().unordered(), true, false);
    }

    public void testIntConcat() {
        // sequential + sequential -> sequential
        assertIntConcat(c1.stream(), c2.stream(), false, true);
        // parallel + parallel -> parallel
        assertIntConcat(c1.parallelStream(), c2.parallelStream(), true, true);
        // sequential + parallel -> parallel
        assertIntConcat(c1.stream(), c2.parallelStream(), true, true);
        // parallel + sequential -> parallel
        assertIntConcat(c1.parallelStream(), c2.stream(), true, true);

        // not ordered
        assertIntConcat(c1.stream().unordered(), c2.stream(), false, false);
        assertIntConcat(c1.stream(), c2.stream().unordered(), false, false);
        assertIntConcat(c1.parallelStream().unordered(), c2.stream().unordered(), true, false);
    }

    public void testLongConcat() {
        // sequential + sequential -> sequential
        assertLongConcat(c1.stream(), c2.stream(), false, true);
        // parallel + parallel -> parallel
        assertLongConcat(c1.parallelStream(), c2.parallelStream(), true, true);
        // sequential + parallel -> parallel
        assertLongConcat(c1.stream(), c2.parallelStream(), true, true);
        // parallel + sequential -> parallel
        assertLongConcat(c1.parallelStream(), c2.stream(), true, true);

        // not ordered
        assertLongConcat(c1.stream().unordered(), c2.stream(), false, false);
        assertLongConcat(c1.stream(), c2.stream().unordered(), false, false);
        assertLongConcat(c1.parallelStream().unordered(), c2.stream().unordered(), true, false);
    }

    public void testDoubleConcat() {
        // sequential + sequential -> sequential
        assertDoubleConcat(c1.stream(), c2.stream(), false, true);
        // parallel + parallel -> parallel
        assertDoubleConcat(c1.parallelStream(), c2.parallelStream(), true, true);
        // sequential + parallel -> parallel
        assertDoubleConcat(c1.stream(), c2.parallelStream(), true, true);
        // parallel + sequential -> parallel
        assertDoubleConcat(c1.parallelStream(), c2.stream(), true, true);

        // not ordered
        assertDoubleConcat(c1.stream().unordered(), c2.stream(), false, false);
        assertDoubleConcat(c1.stream(), c2.stream().unordered(), false, false);
        assertDoubleConcat(c1.parallelStream().unordered(), c2.stream().unordered(), true, false);
    }
}
