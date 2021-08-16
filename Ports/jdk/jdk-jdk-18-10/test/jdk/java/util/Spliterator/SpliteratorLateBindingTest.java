/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 8148748 8170155
 * @summary Spliterator last-binding tests
 * @run testng SpliteratorLateBindingTest
 */

@Test
public class SpliteratorLateBindingTest extends SpliteratorLateBindingFailFastHelper {

    static Object[][] spliteratorDataProvider;

    @DataProvider(name = "Source")
    public static Object[][] sourceDataProvider() {
        if (spliteratorDataProvider != null) {
            return spliteratorDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        SpliteratorDataBuilder<Integer> db =
                new SpliteratorDataBuilder<>(data, 5, Arrays.asList(1, 2, 3, 4));

        // Collections

        db.addList(ArrayList::new);

        db.addList(LinkedList::new);

        db.addList(Vector::new);

        db.addList(AbstractRandomAccessListImpl::new);

        db.addCollection(HashSet::new);

        db.addCollection(LinkedHashSet::new);

        db.addCollection(TreeSet::new);

        db.addCollection(c -> {
            Stack<Integer> s = new Stack<>();
            s.addAll(c);
            return s;
        });

        db.addCollection(PriorityQueue::new);

        db.addCollection(ArrayDeque::new);

        // Maps

        db.addMap(HashMap::new);

        db.addMap(LinkedHashMap::new);

        db.addMap(IdentityHashMap::new);

        db.addMap(WeakHashMap::new);

        // @@@  Descending maps etc
        db.addMap(TreeMap::new);

        // BitSet

        List<Integer> bits = List.of(0, 1, 2);
        Function<BitSet, Spliterator.OfInt> bitsSource = bs -> bs.stream().spliterator();
        db.add("new BitSet.stream().spliterator() ADD",
               () -> new IntSource<>(toBitSet(bits), bitsSource, bs -> bs.set(3)));
        db.add("new BitSet.stream().spliterator() REMOVE",
               () -> new IntSource<>(toBitSet(bits), bitsSource, bs -> bs.clear(2)));

        // CharSequence

        Function<CharSequence, Spliterator.OfInt> charsSource = sb -> sb.chars().spliterator();
        Function<CharSequence, Spliterator.OfInt> pointsSource = sb -> sb.codePoints().spliterator();

        db.add("new StringBuilder.chars().spliterator() ADD",
               () -> new IntSource<>(new StringBuilder("ABC"), charsSource, bs -> bs.append("D"), true));
        db.add("new StringBuilder.chars().spliterator() REMOVE",
               () -> new IntSource<>(new StringBuilder("ABC"), charsSource, bs -> bs.deleteCharAt(2), true));
        db.add("new StringBuilder.codePoints().spliterator() ADD",
               () -> new IntSource<>(new StringBuilder("ABC"), pointsSource, bs -> bs.append("D"), true));
        db.add("new StringBuilder.codePoints().spliterator() REMOVE",
               () -> new IntSource<>(new StringBuilder("ABC"), pointsSource, bs -> bs.deleteCharAt(2), true));

        db.add("new StringBuffer.chars().spliterator() ADD",
               () -> new IntSource<>(new StringBuffer("ABC"), charsSource, bs -> bs.append("D"), true));
        db.add("new StringBuffer.chars().spliterator() REMOVE",
               () -> new IntSource<>(new StringBuffer("ABC"), charsSource, bs -> bs.deleteCharAt(2), true));
        db.add("new StringBuffer.codePoints().spliterator() ADD",
               () -> new IntSource<>(new StringBuffer("ABC"), pointsSource, bs -> bs.append("D"), true));
        db.add("new StringBuffer.codePoints().spliterator() REMOVE",
               () -> new IntSource<>(new StringBuffer("ABC"), pointsSource, bs -> bs.deleteCharAt(2), true));

        db.add("CharBuffer.wrap().chars().spliterator() ADD",
               () -> new IntSource<>(CharBuffer.wrap("ABCD").limit(3), charsSource, bs -> bs.limit(4), true));
        db.add("CharBuffer.wrap().chars().spliterator() REMOVE",
               () -> new IntSource<>(CharBuffer.wrap("ABCD"), charsSource, bs -> bs.limit(3), true));
        db.add("CharBuffer.wrap().codePoints().spliterator() ADD",
               () -> new IntSource<>(CharBuffer.wrap("ABCD").limit(3), pointsSource, bs -> bs.limit(4), true));
        db.add("CharBuffer.wrap().codePoints().spliterator() REMOVE",
               () -> new IntSource<>(CharBuffer.wrap("ABCD"), pointsSource, bs -> bs.limit(3), true));

        return spliteratorDataProvider = data.toArray(new Object[0][]);
    }


    @DataProvider(name = "Source.Non.Binding.Characteristics")
    public static Object[][] sourceCharacteristicsDataProvider() {
        return Stream.of(sourceDataProvider()).filter(tc -> {
            @SuppressWarnings("unchecked")
            Supplier<Source<?>> s = (Supplier<Source<?>>) tc[1];
            return !s.get().bindOnCharacteristics();
        }).toArray(Object[][]::new);
    }

    static BitSet toBitSet(List<Integer> bits) {
        BitSet bs = new BitSet();
        bits.forEach(bs::set);
        return bs;
    }


    @Test(dataProvider = "Source")
    public <T> void testForEach(String description, Supplier<Source<T>> ss) {
        Source<T> source = ss.get();
        Spliterator<T> s = source.spliterator();

        source.update();

        Set<T> a = new HashSet<>();
        s.forEachRemaining(a::add);

        Set<T> e = new HashSet<>();
        source.spliterator().forEachRemaining(e::add);
        assertEquals(a, e);
    }

    @Test(dataProvider = "Source")
    public <T> void testTryAdvance(String description, Supplier<Source<T>> ss) {
        Source<T> source = ss.get();
        Spliterator<T> s = source.spliterator();

        source.update();

        Set<T> a = new HashSet<>();
        while (s.tryAdvance(a::add)) {
        }

        Set<T> e = new HashSet<>();
        source.spliterator().forEachRemaining(e::add);
        assertEquals(a, e);
    }

    @Test(dataProvider = "Source.Non.Binding.Characteristics")
    public <T> void testCharacteristics(String description, Supplier<Source<T>> ss) {
        Source<T> source = ss.get();
        Spliterator<T> s = source.spliterator();

        s.characteristics();
        source.update();

        Set<T> a = new HashSet<>();
        s.forEachRemaining(a::add);

        Set<T> e = new HashSet<>();
        source.spliterator().forEachRemaining(e::add);
        assertEquals(a, e);
    }
}
