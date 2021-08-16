/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020156 8020009 8022326 8012913 8024405 8024408 8071477
 * @run testng SpliteratorCharacteristics
 */

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.testng.Assert.*;

@Test
public class SpliteratorCharacteristics {

    public void testSpliteratorFromCharSequence() {
        class CharSequenceImpl implements CharSequence {
            final String s;

            public CharSequenceImpl(String s) {
                this.s = s;
            }

            @Override
            public int length() {
                return s.length();
            }

            @Override
            public char charAt(int index) {
                return s.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return s.subSequence(start, end);
            }

            @Override
            public String toString() {
                return s;
            }
        }

        CharSequence cs = "A";
        Spliterator.OfInt s = cs.chars().spliterator();
        assertCharacteristics(s, Spliterator.IMMUTABLE | Spliterator.ORDERED |
                                 Spliterator.SIZED | Spliterator.SUBSIZED);
        assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        s = cs.codePoints().spliterator();
        assertCharacteristics(s, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        assertHasNotCharacteristics(s, Spliterator.CONCURRENT);

        for (CharSequence c : Arrays.asList(new CharSequenceImpl("A"),
                                             new StringBuilder("A"),
                                             new StringBuffer("A"))) {
            s = cs.chars().spliterator();
            assertCharacteristics(s, Spliterator.ORDERED |
                                     Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
            s = cs.codePoints().spliterator();
            assertCharacteristics(s, Spliterator.ORDERED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }
    }

    public void testSpliteratorFromCollection() {
        List<Integer> l = Arrays.asList(1, 2, 3, 4);

        {
            Spliterator<?> s = Spliterators.spliterator(l, 0);
            assertCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(l, Spliterator.CONCURRENT);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(l.iterator(), 1, 0);
            assertCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(l.iterator(), 1, Spliterator.CONCURRENT);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(l.iterator(), 0);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(
                    l.iterator(), Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }
    }

    public void testSpliteratorOfIntFromIterator() {
        Supplier<PrimitiveIterator.OfInt> si = () -> IntStream.of(1, 2, 3, 4).iterator();

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, 0);
            assertCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, Spliterator.CONCURRENT);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(si.get(), 0);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(
                    si.get(), Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }
    }

    public void testSpliteratorOfLongFromIterator() {
        Supplier<PrimitiveIterator.OfLong> si = () -> LongStream.of(1, 2, 3, 4).iterator();

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, 0);
            assertCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, Spliterator.CONCURRENT);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(si.get(), 0);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(
                    si.get(), Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }
    }

    public void testSpliteratorOfDoubleFromIterator() {
        Supplier<PrimitiveIterator.OfDouble> si = () -> DoubleStream.of(1, 2, 3, 4).iterator();

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, 0);
            assertCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliterator(si.get(), 1, Spliterator.CONCURRENT);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
            assertCharacteristics(s, Spliterator.CONCURRENT);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(si.get(), 0);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        {
            Spliterator<?> s = Spliterators.spliteratorUnknownSize(
                    si.get(), Spliterator.SIZED | Spliterator.SUBSIZED);
            assertHasNotCharacteristics(s, Spliterator.SIZED | Spliterator.SUBSIZED);
        }
    }

    //

    public void testHashMap() {
        assertMapCharacteristics(new HashMap<>(),
                                 Spliterator.SIZED | Spliterator.DISTINCT);
    }

    public void testWeakHashMap() {
        assertMapCharacteristics(new WeakHashMap<>(),
                                 Spliterator.DISTINCT);
    }

    public void testHashSet() {
        assertSetCharacteristics(new HashSet<>(),
                                 Spliterator.SIZED | Spliterator.DISTINCT);
    }

    public void testLinkedHashMap() {
        assertMapCharacteristics(new LinkedHashMap<>(),
                                 Spliterator.SIZED | Spliterator.DISTINCT |
                                 Spliterator.ORDERED);
    }

    public void testLinkedHashSet() {
        assertSetCharacteristics(new LinkedHashSet<>(),
                                 Spliterator.SIZED | Spliterator.DISTINCT |
                                 Spliterator.ORDERED);
    }

    public void testTreeMap() {
        assertSortedMapCharacteristics(new TreeMap<>(),
                                       Spliterator.SIZED | Spliterator.DISTINCT |
                                       Spliterator.SORTED | Spliterator.ORDERED);
    }

    public void testTreeMapWithComparator() {
        assertSortedMapCharacteristics(new TreeMap<>(Comparator.reverseOrder()),
                                       Spliterator.SIZED | Spliterator.DISTINCT |
                                       Spliterator.SORTED | Spliterator.ORDERED);
    }

    public void testTreeSet() {
        assertSortedSetCharacteristics(new TreeSet<>(),
                                       Spliterator.SIZED | Spliterator.DISTINCT |
                                       Spliterator.SORTED | Spliterator.ORDERED);
    }

    public void testTreeSetWithComparator() {
        assertSortedSetCharacteristics(new TreeSet<>(Comparator.reverseOrder()),
                                       Spliterator.SIZED | Spliterator.DISTINCT |
                                       Spliterator.SORTED | Spliterator.ORDERED);
    }

    public void testConcurrentSkipListMap() {
        assertSortedMapCharacteristics(new ConcurrentSkipListMap<>(),
                                       Spliterator.CONCURRENT | Spliterator.NONNULL |
                                       Spliterator.DISTINCT | Spliterator.SORTED |
                                       Spliterator.ORDERED);
    }

    public void testConcurrentSkipListMapWithComparator() {
        assertSortedMapCharacteristics(new ConcurrentSkipListMap<>(Comparator.<Integer>reverseOrder()),
                                       Spliterator.CONCURRENT | Spliterator.NONNULL |
                                       Spliterator.DISTINCT | Spliterator.SORTED |
                                       Spliterator.ORDERED);
    }

    public void testConcurrentSkipListSet() {
        assertSortedSetCharacteristics(new ConcurrentSkipListSet<>(),
                                       Spliterator.CONCURRENT | Spliterator.NONNULL |
                                       Spliterator.DISTINCT | Spliterator.SORTED |
                                       Spliterator.ORDERED);
    }

    public void testConcurrentSkipListSetWithComparator() {
        assertSortedSetCharacteristics(new ConcurrentSkipListSet<>(Comparator.reverseOrder()),
                                       Spliterator.CONCURRENT | Spliterator.NONNULL |
                                       Spliterator.DISTINCT | Spliterator.SORTED |
                                       Spliterator.ORDERED);
    }


    //


    void assertMapCharacteristics(Map<Integer, String> m, int keyCharacteristics) {
        assertMapCharacteristics(m, keyCharacteristics, 0);
    }

    void assertMapCharacteristics(Map<Integer, String> m, int keyCharacteristics, int notValueCharacteristics) {
        initMap(m);

        assertCharacteristics(m.keySet(), keyCharacteristics);

        assertCharacteristics(m.values(),
                              keyCharacteristics & ~(Spliterator.DISTINCT | notValueCharacteristics));

        assertCharacteristics(m.entrySet(), keyCharacteristics);

        if ((keyCharacteristics & Spliterator.SORTED) == 0) {
            assertISEComparator(m.keySet());
            assertISEComparator(m.values());
            assertISEComparator(m.entrySet());
        }
    }

    void assertSetCharacteristics(Set<Integer> s, int keyCharacteristics) {
        initSet(s);

        assertCharacteristics(s, keyCharacteristics);

        if ((keyCharacteristics & Spliterator.SORTED) == 0) {
            assertISEComparator(s);
        }
    }

    void assertSortedMapCharacteristics(SortedMap<Integer, String> m, int keyCharacteristics) {
        assertMapCharacteristics(m, keyCharacteristics, Spliterator.SORTED);

        Set<Integer> keys = m.keySet();
        if (m.comparator() != null) {
            assertNotNullComparator(keys);
        }
        else {
            assertNullComparator(keys);
        }

        assertISEComparator(m.values());

        assertNotNullComparator(m.entrySet());
    }

    void assertSortedSetCharacteristics(SortedSet<Integer> s, int keyCharacteristics) {
        assertSetCharacteristics(s, keyCharacteristics);

        if (s.comparator() != null) {
            assertNotNullComparator(s);
        }
        else {
            assertNullComparator(s);
        }
    }

    void initMap(Map<Integer, String> m) {
        m.put(1, "4");
        m.put(2, "3");
        m.put(3, "2");
        m.put(4, "1");
    }

    void initSet(Set<Integer> s) {
        s.addAll(Arrays.asList(1, 2, 3, 4));
    }

    void assertCharacteristics(Collection<?> c, int expectedCharacteristics) {
        assertCharacteristics(c.spliterator(), expectedCharacteristics);
    }

    void assertCharacteristics(Spliterator<?> s, int expectedCharacteristics) {
        assertTrue(s.hasCharacteristics(expectedCharacteristics),
                   "Spliterator characteristics");
    }

    void assertHasNotCharacteristics(Spliterator<?> s, int expectedCharacteristics) {
        assertFalse(s.hasCharacteristics(expectedCharacteristics),
                    "Spliterator characteristics");
    }

    void assertNullComparator(Collection<?> c) {
        assertNull(c.spliterator().getComparator(),
                   "Comparator of Spliterator of Collection");
    }

    void assertNotNullComparator(Collection<?> c) {
        assertNotNull(c.spliterator().getComparator(),
                      "Comparator of Spliterator of Collection");
    }

    void assertISEComparator(Collection<?> c) {
        assertISEComparator(c.spliterator());
    }

    void assertISEComparator(Spliterator<?> s) {
        boolean caught = false;
        try {
            s.getComparator();
        }
        catch (IllegalStateException e) {
            caught = true;
        }
        assertTrue(caught, "Throwing IllegalStateException");
    }
}
