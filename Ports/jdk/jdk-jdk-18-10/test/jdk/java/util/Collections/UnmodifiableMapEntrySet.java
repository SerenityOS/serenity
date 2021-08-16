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

/**
 * @test
 * @run testng UnmodifiableMapEntrySet
 * @summary Unit tests for wrapping classes should delegate to default methods
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static org.testng.Assert.assertEquals;

@Test(groups = "unit")
public class UnmodifiableMapEntrySet {
    static Object[][] collections;

    static <M extends Map<Integer, Integer>> M fillMap(int size, M m) {
        for (int i = 0; i < size; i++) {
            m.put(i, i);
        }
        return m;
    }

    @DataProvider(name="maps")
    static Object[][] mapCases() {
        if (collections != null) {
            return collections;
        }

        List<Object[]> cases = new ArrayList<>();
        for (int size : new int[] {1, 2, 16}) {
            cases.add(new Object[] {
                    String.format("new HashMap(%d)", size),
                    (Supplier<Map<Integer, Integer>>)
                    () -> Collections.unmodifiableMap(fillMap(size, new HashMap<>())) });
            cases.add(new Object[] {
                    String.format("new TreeMap(%d)", size),
                    (Supplier<Map<Integer, Integer>>)
                    () -> Collections.unmodifiableSortedMap(fillMap(size, new TreeMap<>())) });
        }

        return cases.toArray(new Object[0][]);
    }

    static class EntryConsumer implements Consumer<Map.Entry<Integer, Integer>> {
        int updates;
        @Override
        public void accept(Map.Entry<Integer, Integer> me) {
            try {
                me.setValue(Integer.MAX_VALUE);
                updates++;
            } catch (UnsupportedOperationException e) {
            }
        }

        void assertNoUpdates() {
            assertEquals(updates, 0, "Updates to entries");
        }
    }

    void testWithEntryConsumer(Consumer<EntryConsumer> c) {
        EntryConsumer ec = new EntryConsumer();
        c.accept(ec);
        ec.assertNoUpdates();
    }

    @Test(dataProvider = "maps")
    public void testForEach(String d, Supplier<Map<Integer, Integer>> ms) {
        testWithEntryConsumer(
                ec -> ms.get().entrySet().forEach(ec));
    }

    @Test(dataProvider = "maps")
    public void testIteratorForEachRemaining(String d, Supplier<Map<Integer, Integer>> ms) {
        testWithEntryConsumer(
                ec -> ms.get().entrySet().iterator().forEachRemaining(ec));
    }

    @Test(dataProvider = "maps")
    public void testIteratorNext(String d, Supplier<Map<Integer, Integer>> ms) {
        testWithEntryConsumer(ec -> {
            for (Map.Entry<Integer, Integer> me : ms.get().entrySet()) {
                ec.accept(me);
            }
        });
    }

    @Test(dataProvider = "maps")
    public void testSpliteratorForEachRemaining(String d, Supplier<Map<Integer, Integer>> ms) {
        testSpliterator(
                ms.get().entrySet()::spliterator,
                // Higher order function returning a consumer that
                // traverses all spliterator elements using an EntryConsumer
                s -> ec -> s.forEachRemaining(ec));
    }

    @Test(dataProvider = "maps")
    public void testSpliteratorTryAdvance(String d, Supplier<Map<Integer, Integer>> ms) {
        testSpliterator(
                ms.get().entrySet()::spliterator,
                // Higher order function returning a consumer that
                // traverses all spliterator elements using an EntryConsumer
                s -> ec -> { while (s.tryAdvance(ec)); });
    }

    void testSpliterator(Supplier<Spliterator<Map.Entry<Integer, Integer>>> ss,
                         // Higher order function that given a spliterator returns a
                         // consumer for that spliterator which traverses elements
                         // using an EntryConsumer
                         Function<Spliterator<Map.Entry<Integer, Integer>>, Consumer<EntryConsumer>> sc) {
        testWithEntryConsumer(sc.apply(ss.get()));

        Spliterator<Map.Entry<Integer, Integer>> s = ss.get();
        Spliterator<Map.Entry<Integer, Integer>> split = s.trySplit();
        if (split != null) {
            testWithEntryConsumer(sc.apply(split));
            testWithEntryConsumer(sc.apply(s));
        }
    }

    @Test(dataProvider = "maps")
    public void testStreamForEach(String d, Supplier<Map<Integer, Integer>> ms) {
        testWithEntryConsumer(ec -> ms.get().entrySet().stream().forEach(ec));
    }

    @Test(dataProvider = "maps")
    public void testParallelStreamForEach(String d, Supplier<Map<Integer, Integer>> ms) {
        testWithEntryConsumer(ec -> ms.get().entrySet().parallelStream().forEach(ec));
    }
}

