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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @test
 * @summary Unit tests for extension methods on List
 * @bug 8023367 8037106
 * @library ../Collection/testlibrary
 * @build CollectionAsserts CollectionSupplier ExtendsAbstractList
 * @run testng ListDefaults
 */
public class ListDefaults {

    // Suppliers of lists that can support structural modifications
    private static final List<Function<Collection, List>> LIST_STRUCT_MOD_SUPPLIERS = Arrays.asList(
            java.util.ArrayList::new,
            java.util.LinkedList::new,
            java.util.Vector::new,
            java.util.concurrent.CopyOnWriteArrayList::new,
            ExtendsAbstractList::new
    );

    // Suppliers of lists that can support in place modifications
    private static final List<Function<Collection, List>> LIST_SUPPLIERS = Arrays.asList(
            java.util.ArrayList::new,
            java.util.LinkedList::new,
            java.util.Vector::new,
            java.util.concurrent.CopyOnWriteArrayList::new,
            ExtendsAbstractList::new,
            c -> Arrays.asList(c.toArray())
    );

    // Suppliers of lists supporting CMEs
    private static final List<Function<Collection, List>> LIST_CME_SUPPLIERS = Arrays.asList(
            java.util.ArrayList::new,
            java.util.Vector::new
    );

    private static final Predicate<Integer> pEven = x -> 0 == x % 2;
    private static final Predicate<Integer> pOdd = x -> 1 == x % 2;

    private static final Comparator<Integer> BIT_COUNT_COMPARATOR =
            (x, y) -> Integer.bitCount(x) - Integer.bitCount(y);

    private static final Comparator<AtomicInteger> ATOMIC_INTEGER_COMPARATOR =
            (x, y) -> x.intValue() - y.intValue();

    private static final int SIZE = 100;
    private static final int SUBLIST_FROM = 20;
    private static final int SUBLIST_TO = SIZE - 5;
    private static final int SUBLIST_SIZE = SUBLIST_TO - SUBLIST_FROM;

    // call the callback for each recursive subList
    private void trimmedSubList(final List<Integer> list, final Consumer<List<Integer>> callback) {
        int size = list.size();
        if (size > 1) {
            // trim 1 element from both ends
            final List<Integer> subList = list.subList(1, size - 1);
            callback.accept(subList);
            trimmedSubList(subList, callback);
        }
    }

    @DataProvider(name="listProvider", parallel=true)
    public static Object[][] listCases() {
        final List<Object[]> cases = new LinkedList<>();
        cases.add(new Object[] { Collections.emptyList() });
        cases.add(new Object[] { new ArrayList<>() });
        cases.add(new Object[] { new LinkedList<>() });
        cases.add(new Object[] { new Vector<>() });
        cases.add(new Object[] { new Stack<>() });
        cases.add(new Object[] { new CopyOnWriteArrayList<>() });
        cases.add(new Object[] { Arrays.asList() });

        List<Integer> l = Arrays.asList(42);
        cases.add(new Object[] { new ArrayList<>(l) });
        cases.add(new Object[] { new LinkedList<>(l) });
        cases.add(new Object[] { new Vector<>(l) });
        Stack<Integer> s = new Stack<>(); s.addAll(l);
        cases.add(new Object[]{s});
        cases.add(new Object[] { new CopyOnWriteArrayList<>(l) });
        cases.add(new Object[] { l });
        return cases.toArray(new Object[0][cases.size()]);
    }

    @Test(dataProvider = "listProvider")
    public void testProvidedWithNull(final List<Integer> list) {
        try {
            list.forEach(null);
            fail("expected NPE not thrown");
        } catch (NullPointerException npe) {}
        try {
            list.replaceAll(null);
            fail("expected NPE not thrown");
        } catch (NullPointerException npe) {}
        try {
            list.removeIf(null);
            fail("expected NPE not thrown");
        } catch (NullPointerException npe) {}
        try {
            list.sort(null);
        } catch (Throwable t) {
            fail("Exception not expected: " + t);
        }
    }

    @Test
    public void testForEach() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;

            try {
                list.forEach(null);
                fail("expected NPE not thrown");
            } catch (NullPointerException npe) {}
            CollectionAsserts.assertContents(list, original);

            final List<Integer> actual = new LinkedList<>();
            list.forEach(actual::add);
            CollectionAsserts.assertContents(actual, list);
            CollectionAsserts.assertContents(actual, original);

            if (original.size() > SUBLIST_SIZE) {
                final List<Integer> subList = original.subList(SUBLIST_FROM, SUBLIST_TO);
                final List<Integer> actualSubList = new LinkedList<>();
                subList.forEach(actualSubList::add);
                assertEquals(actualSubList.size(), SUBLIST_SIZE);
                for (int i = 0; i < SUBLIST_SIZE; i++) {
                    assertEquals(actualSubList.get(i), original.get(i + SUBLIST_FROM));
                }
            }

            trimmedSubList(list, l -> {
                    final List<Integer> a = new LinkedList<>();
                    l.forEach(a::add);
                    CollectionAsserts.assertContents(a, l);
                });
        }
    }

    @Test
    public void testRemoveIf() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_STRUCT_MOD_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;

            try {
                list.removeIf(null);
                fail("expected NPE not thrown");
            } catch (NullPointerException npe) {}
            CollectionAsserts.assertContents(list, original);

            final AtomicInteger offset = new AtomicInteger(1);
            while (list.size() > 0) {
                removeFirst(original, list, offset);
            }
        }

        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;
            list.removeIf(pOdd);
            for (int i : list) {
                assertTrue((i % 2) == 0);
            }
            for (int i : original) {
                if (i % 2 == 0) {
                    assertTrue(list.contains(i));
                }
            }
            list.removeIf(pEven);
            assertTrue(list.isEmpty());
        }

        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;
            final List<Integer> listCopy = new ArrayList<>(list);
            if (original.size() > SUBLIST_SIZE) {
                final List<Integer> subList = list.subList(SUBLIST_FROM, SUBLIST_TO);
                final List<Integer> subListCopy = new ArrayList<>(subList);
                listCopy.removeAll(subList);
                subList.removeIf(pOdd);
                for (int i : subList) {
                    assertTrue((i % 2) == 0);
                }
                for (int i : subListCopy) {
                    if (i % 2 == 0) {
                        assertTrue(subList.contains(i));
                    } else {
                        assertFalse(subList.contains(i));
                    }
                }
                subList.removeIf(pEven);
                assertTrue(subList.isEmpty());
                // elements outside the view should remain
                CollectionAsserts.assertContents(list, listCopy);
            }
        }

        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;
            trimmedSubList(list, l -> {
                final List<Integer> copy = new ArrayList<>(l);
                l.removeIf(pOdd);
                for (int i : l) {
                    assertTrue((i % 2) == 0);
                }
                for (int i : copy) {
                    if (i % 2 == 0) {
                        assertTrue(l.contains(i));
                    } else {
                        assertFalse(l.contains(i));
                    }
                }
            });
        }
    }

    // remove the first element
    private void removeFirst(final List<Integer> original, final List<Integer> list, final AtomicInteger offset) {
        final AtomicBoolean first = new AtomicBoolean(true);
        list.removeIf(x -> first.getAndSet(false));
        CollectionAsserts.assertContents(original.subList(offset.getAndIncrement(), original.size()), list);
    }

    @Test
    public void testReplaceAll() {
        final int scale = 3;
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;

            try {
                list.replaceAll(null);
                fail("expected NPE not thrown");
            } catch (NullPointerException npe) {}
            CollectionAsserts.assertContents(list, original);

            list.replaceAll(x -> scale * x);
            for (int i = 0; i < original.size(); i++) {
                assertTrue(list.get(i) == (scale * original.get(i)), "mismatch at index " + i);
            }

            if (original.size() > SUBLIST_SIZE) {
                final List<Integer> subList = list.subList(SUBLIST_FROM, SUBLIST_TO);
                subList.replaceAll(x -> x + 1);
                // verify elements in view [from, to) were replaced
                for (int i = 0; i < SUBLIST_SIZE; i++) {
                    assertTrue(subList.get(i) == ((scale * original.get(i + SUBLIST_FROM)) + 1),
                            "mismatch at sublist index " + i);
                }
                // verify that elements [0, from) remain unmodified
                for (int i = 0; i < SUBLIST_FROM; i++) {
                    assertTrue(list.get(i) == (scale * original.get(i)),
                            "mismatch at original index " + i);
                }
                // verify that elements [to, size) remain unmodified
                for (int i = SUBLIST_TO; i < list.size(); i++) {
                    assertTrue(list.get(i) == (scale * original.get(i)),
                            "mismatch at original index " + i);
                }
            }
        }

        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;
            trimmedSubList(list, l -> {
                final List<Integer> copy = new ArrayList<>(l);
                final int offset = 5;
                l.replaceAll(x -> offset + x);
                for (int i = 0; i < copy.size(); i++) {
                    assertTrue(l.get(i) == (offset + copy.get(i)), "mismatch at index " + i);
                }
            });
        }
    }

    @Test
    public void testSort() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> original = test.expected;
            final List<Integer> list = test.collection;
            CollectionSupplier.shuffle(list);
            list.sort(Integer::compare);
            CollectionAsserts.assertSorted(list, Integer::compare);
            if (test.name.startsWith("reverse")) {
                Collections.reverse(list);
            }
            CollectionAsserts.assertContents(list, original);

            CollectionSupplier.shuffle(list);
            list.sort(null);
            CollectionAsserts.assertSorted(list, Comparator.naturalOrder());
            if (test.name.startsWith("reverse")) {
                Collections.reverse(list);
            }
            CollectionAsserts.assertContents(list, original);

            CollectionSupplier.shuffle(list);
            list.sort(Comparator.naturalOrder());
            CollectionAsserts.assertSorted(list, Comparator.naturalOrder());
            if (test.name.startsWith("reverse")) {
                Collections.reverse(list);
            }
            CollectionAsserts.assertContents(list, original);

            CollectionSupplier.shuffle(list);
            list.sort(Comparator.reverseOrder());
            CollectionAsserts.assertSorted(list, Comparator.reverseOrder());
            if (!test.name.startsWith("reverse")) {
                Collections.reverse(list);
            }
            CollectionAsserts.assertContents(list, original);

            CollectionSupplier.shuffle(list);
            list.sort(BIT_COUNT_COMPARATOR);
            CollectionAsserts.assertSorted(list, BIT_COUNT_COMPARATOR);
            // check sort by verifying that bitCount increases and never drops
            int minBitCount = 0;
            for (final Integer i : list) {
                int bitCount = Integer.bitCount(i);
                assertTrue(bitCount >= minBitCount);
                minBitCount = bitCount;
            }

            // Reuse the supplier to store AtomicInteger instead of Integer
            // Hence the use of raw type and cast
            List<AtomicInteger> incomparablesData = new ArrayList<>();
            for (int i = 0; i < test.expected.size(); i++) {
                incomparablesData.add(new AtomicInteger(i));
            }
            Function f = test.supplier;
            @SuppressWarnings("unchecked")
            List<AtomicInteger> incomparables = (List<AtomicInteger>) f.apply(incomparablesData);

            CollectionSupplier.shuffle(incomparables);
            incomparables.sort(ATOMIC_INTEGER_COMPARATOR);
            for (int i = 0; i < test.expected.size(); i++) {
                assertEquals(i, incomparables.get(i).intValue());
            }


            if (original.size() > SUBLIST_SIZE) {
                final List<Integer> copy = new ArrayList<>(list);
                final List<Integer> subList = list.subList(SUBLIST_FROM, SUBLIST_TO);
                CollectionSupplier.shuffle(subList);
                subList.sort(Comparator.naturalOrder());
                CollectionAsserts.assertSorted(subList, Comparator.naturalOrder());
                // verify that elements [0, from) remain unmodified
                for (int i = 0; i < SUBLIST_FROM; i++) {
                    assertTrue(list.get(i) == copy.get(i),
                            "mismatch at index " + i);
                }
                // verify that elements [to, size) remain unmodified
                for (int i = SUBLIST_TO; i < list.size(); i++) {
                    assertTrue(list.get(i) == copy.get(i),
                            "mismatch at index " + i);
                }
            }
        }

        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;
            trimmedSubList(list, l -> {
                CollectionSupplier.shuffle(l);
                l.sort(Comparator.naturalOrder());
                CollectionAsserts.assertSorted(l, Comparator.naturalOrder());
            });
        }
    }

    @Test
    public void testForEachThrowsCME() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_CME_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;

            if (list.size() <= 1) {
                continue;
            }
            boolean gotException = false;
            try {
                // bad predicate that modifies its list, should throw CME
                list.forEach(list::add);
            } catch (ConcurrentModificationException cme) {
                gotException = true;
            }
            if (!gotException) {
                fail("expected CME was not thrown from " + test);
            }
        }
    }

    @Test
    public void testRemoveIfThrowsCME() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_CME_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;

            if (list.size() <= 1) {
                continue;
            }
            boolean gotException = false;
            try {
                // bad predicate that modifies its list, should throw CME
                list.removeIf(list::add);
            } catch (ConcurrentModificationException cme) {
                gotException = true;
            }
            if (!gotException) {
                fail("expected CME was not thrown from " + test);
            }
        }
    }

    @Test
    public void testReplaceAllThrowsCME() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_CME_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;

            if (list.size() <= 1) {
                continue;
            }
            boolean gotException = false;
            try {
                // bad predicate that modifies its list, should throw CME
                list.replaceAll(x -> {int n = 3 * x; list.add(n); return n;});
            } catch (ConcurrentModificationException cme) {
                gotException = true;
            }
            if (!gotException) {
                fail("expected CME was not thrown from " + test);
            }
        }
    }

    @Test
    public void testSortThrowsCME() {
        @SuppressWarnings("unchecked")
        final CollectionSupplier<List<Integer>> supplier = new CollectionSupplier(LIST_CME_SUPPLIERS, SIZE);
        for (final CollectionSupplier.TestCase<List<Integer>> test : supplier.get()) {
            final List<Integer> list = test.collection;

            if (list.size() <= 1) {
                continue;
            }
            boolean gotException = false;
            try {
                // bad predicate that modifies its list, should throw CME
                list.sort((x, y) -> {list.add(x); return x - y;});
            } catch (ConcurrentModificationException cme) {
                gotException = true;
            }
            if (!gotException) {
                fail("expected CME was not thrown from " + test);
            }
        }
    }

    private static final List<Integer> SLICED_EXPECTED = Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8, 9);
    private static final List<Integer> SLICED_EXPECTED2 = Arrays.asList(0, 1, 2, 5, 6, 7, 8, 9);

    @DataProvider(name="shortIntListProvider", parallel=true)
    public static Object[][] intListCases() {
        final Integer[] DATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        final List<Object[]> cases = new LinkedList<>();
        cases.add(new Object[] { new ArrayList<>(Arrays.asList(DATA)) });
        cases.add(new Object[] { new LinkedList<>(Arrays.asList(DATA)) });
        cases.add(new Object[] { new Vector<>(Arrays.asList(DATA)) });
        cases.add(new Object[] { new CopyOnWriteArrayList<>(Arrays.asList(DATA)) });
        cases.add(new Object[] { new ExtendsAbstractList<>(Arrays.asList(DATA)) });
        return cases.toArray(new Object[0][cases.size()]);
    }

    @Test(dataProvider = "shortIntListProvider")
    public void testRemoveIfFromSlice(final List<Integer> list) {
        final List<Integer> sublist = list.subList(3, 6);
        assertTrue(sublist.removeIf(x -> x == 4));
        CollectionAsserts.assertContents(list, SLICED_EXPECTED);

        final List<Integer> sublist2 = list.subList(2, 5);
        assertTrue(sublist2.removeIf(x -> x == 3));
        CollectionAsserts.assertContents(list, SLICED_EXPECTED2);
    }
}
