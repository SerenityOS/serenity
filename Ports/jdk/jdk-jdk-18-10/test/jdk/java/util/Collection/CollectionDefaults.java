/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.util.SortedSet;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @test
 * @summary Unit tests for extension methods on Collection
 * @library testlibrary
 * @build CollectionAsserts CollectionSupplier ExtendsAbstractSet ExtendsAbstractCollection
 * @run testng CollectionDefaults
 */
public class CollectionDefaults {

    public static final Predicate<Integer> pEven = x -> 0 == x % 2;
    public static final Predicate<Integer> pOdd = x -> 1 == x % 2;

    private static final int SIZE = 100;

    private static final List<Function<Collection<Integer>, Collection<Integer>>> TEST_SUPPLIERS = Arrays.asList(
            // Collection
            ExtendsAbstractCollection<Integer>::new,
            java.util.ArrayDeque<Integer>::new,
            java.util.concurrent.ConcurrentLinkedDeque<Integer>::new,
            java.util.concurrent.ConcurrentLinkedQueue<Integer>::new,
            java.util.concurrent.LinkedBlockingDeque<Integer>::new,
            java.util.concurrent.LinkedBlockingQueue<Integer>::new,
            java.util.concurrent.LinkedTransferQueue<Integer>::new,
            (coll) -> new java.util.concurrent.ArrayBlockingQueue<Integer>(
                3 * SIZE, false, coll),

            // Lists
            java.util.ArrayList<Integer>::new,
            java.util.LinkedList<Integer>::new,
            java.util.Vector<Integer>::new,
            java.util.concurrent.CopyOnWriteArrayList<Integer>::new,
            ExtendsAbstractList<Integer>::new,

            // Sets
            java.util.HashSet<Integer>::new,
            java.util.LinkedHashSet<Integer>::new,
            java.util.TreeSet<Integer>::new,
            java.util.concurrent.ConcurrentSkipListSet<Integer>::new,
            java.util.concurrent.CopyOnWriteArraySet<Integer>::new,
            ExtendsAbstractSet<Integer>::new
       );

    @DataProvider(name="setProvider", parallel=true)
    public static Iterator<Object[]> setCases() {
        final List<Object[]> cases = new LinkedList<>();
        cases.add(new Object[] { new HashSet<>() });
        cases.add(new Object[] { new LinkedHashSet<>() });
        cases.add(new Object[] { new TreeSet<>() });
        cases.add(new Object[] { new java.util.concurrent.ConcurrentSkipListSet<>() });
        cases.add(new Object[] { new java.util.concurrent.CopyOnWriteArraySet<>() });

        cases.add(new Object[] { new ExtendsAbstractSet<>() });

        cases.add(new Object[] { Collections.newSetFromMap(new HashMap<>()) });
        cases.add(new Object[] { Collections.newSetFromMap(new LinkedHashMap<>()) });
        cases.add(new Object[] { Collections.newSetFromMap(new TreeMap<>()) });
        cases.add(new Object[] { Collections.newSetFromMap(new ConcurrentHashMap<>()) });
        cases.add(new Object[] { Collections.newSetFromMap(new ConcurrentSkipListMap<>()) });

        cases.add(new Object[] { new HashSet<Integer>(){{add(42);}} });
        cases.add(new Object[] { new ExtendsAbstractSet<Integer>(){{add(42);}} });
        cases.add(new Object[] { new LinkedHashSet<Integer>(){{add(42);}} });
        cases.add(new Object[] { new TreeSet<Integer>(){{add(42);}} });
        return cases.iterator();
    }

    @Test(dataProvider = "setProvider")
    public void testProvidedWithNull(final Set<Integer> set) {
        try {
            set.forEach(null);
            fail("expected NPE not thrown");
        } catch (NullPointerException expected) { // expected
        }
        try {
            set.removeIf(null);
            fail("expected NPE not thrown");
        } catch (NullPointerException expected) { // expected
        }
    }

    @Test
    public void testForEach() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<Collection<Integer>> supplier = new CollectionSupplier(TEST_SUPPLIERS, SIZE);

        for (final CollectionSupplier.TestCase<Collection<Integer>> test : supplier.get()) {
            final Collection<Integer> original = test.expected;
            final Collection<Integer> set = test.collection;

            try {
                set.forEach(null);
                fail("expected NPE not thrown");
            } catch (NullPointerException expected) { // expected
            }
            if (set instanceof Set && !((set instanceof SortedSet) || (set instanceof LinkedHashSet))) {
                CollectionAsserts.assertContentsUnordered(set, original, test.toString());
            } else {
                CollectionAsserts.assertContents(set, original, test.toString());
            }

            final List<Integer> actual = new LinkedList<>();
            set.forEach(actual::add);
            if (set instanceof Set && !((set instanceof SortedSet) || (set instanceof LinkedHashSet))) {
                CollectionAsserts.assertContentsUnordered(actual, set, test.toString());
                CollectionAsserts.assertContentsUnordered(actual, original, test.toString());
            } else {
                CollectionAsserts.assertContents(actual, set, test.toString());
                CollectionAsserts.assertContents(actual, original, test.toString());
            }
        }
    }

    @Test
    public void testRemoveIf() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<Collection<Integer>> supplier = new CollectionSupplier(TEST_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<Collection<Integer>> test : supplier.get()) {
            final Collection<Integer> original = test.expected;
            final Collection<Integer> set = test.collection;

            try {
                set.removeIf(null);
                fail("expected NPE not thrown");
            } catch (NullPointerException expected) { // expected
            }
            if (set instanceof Set && !((set instanceof SortedSet) || (set instanceof LinkedHashSet))) {
                CollectionAsserts.assertContentsUnordered(set, original, test.toString());
            } else {
                CollectionAsserts.assertContents(set, original, test.toString());
            }

            set.removeIf(pEven);
            for (int i : set) {
                assertTrue((i % 2) == 1);
            }
            for (int i : original) {
                if (i % 2 == 1) {
                    assertTrue(set.contains(i));
                }
            }
            set.removeIf(pOdd);
            assertTrue(set.isEmpty());
        }
    }
}
