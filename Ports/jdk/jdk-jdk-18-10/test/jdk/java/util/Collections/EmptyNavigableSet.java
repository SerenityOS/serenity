/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4533691 7129185
 * @summary Unit test for Collections.emptyNavigableSet
 * @run testng EmptyNavigableSet
 */

import org.testng.Assert;
import org.testng.Assert.ThrowingRunnable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class EmptyNavigableSet {

    public static <T> void assertInstance(T actual, Class<? extends T> expected) {
        assertInstance(actual, expected, null);
    }

    public static <T> void assertInstance(T actual, Class<? extends T> expected, String message) {
        assertTrue(expected.isInstance(actual), ((null != message) ? message : "")
            + " " + (actual == null ? "<null>" : actual.getClass().getSimpleName()) + " != " + expected.getSimpleName() + ". ");
    }

    public static <T extends Throwable> void assertEmptyNavigableSet(Object obj) {
        assertInstance(obj, NavigableSet.class);
        assertTrue(((NavigableSet)obj).isEmpty() && (((NavigableSet)obj).size() == 0));
    }

    public static <T extends Throwable> void assertEmptyNavigableSet(Object obj, String message) {
        assertInstance(obj, NavigableSet.class, message);
        assertTrue(((NavigableSet)obj).isEmpty() && (((NavigableSet)obj).size() == 0),
            ((null != message) ? message : "") + " Not empty. ");
    }

    private <T extends Throwable> void assertThrows(Class<T> throwableClass,
                                                    ThrowingRunnable runnable,
                                                    String message) {
        try {
            Assert.assertThrows(throwableClass, runnable);
        } catch (AssertionError e) {
            throw new AssertionError(String.format("%s%n%s",
                    ((null != message) ? message : ""), e.getMessage()), e);
        }
    }

    private void assertThrowsCCE(ThrowingRunnable r, String s) {
        assertThrows(ClassCastException.class, r, s);
    }

    private void assertThrowsNPE(ThrowingRunnable r, String s) {
        assertThrows(NullPointerException.class, r, s);
    }

    private void assertThrowsIAE(ThrowingRunnable r, String s) {
        assertThrows(IllegalArgumentException.class, r, s);
    }

    private void assertThrowsNSEE(ThrowingRunnable r, String s) {
        assertThrows(NoSuchElementException.class, r, s);
    }

    public static final boolean isDescending(SortedSet<?> set) {
        if (null == set.comparator()) {
            // natural order
            return false;
        }

        if (Collections.reverseOrder() == set.comparator()) {
            // reverse natural order.
            return true;
        }

        if (set.comparator().equals(Collections.reverseOrder(Collections.reverseOrder(set.comparator())))) {
            // it's a Collections.reverseOrder(Comparator).
            return true;
        }

        throw new IllegalStateException("can't determine ordering for " + set);
    }

    /**
     * Tests that the comparator is {@code null}.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testComparatorIsNull(String description, NavigableSet<?> navigableSet) {
        Comparator comparator = navigableSet.comparator();

        assertTrue(comparator == null || comparator == Collections.reverseOrder(), description + ": Comparator (" + comparator + ") is not null.");
    }

    /**
     * Tests that contains requires Comparable
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testContainsRequiresComparable(String description, NavigableSet<?> navigableSet) {
        assertThrowsCCE(
            () -> navigableSet.contains(new Object()),
            description + ": Comparable should be required");
    }

    /**
     * Tests that the contains method returns {@code false}.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testContains(String description, NavigableSet<?> navigableSet) {
        assertFalse(navigableSet.contains(new Integer(1)),
            description + ": Should not contain any elements.");
    }

    /**
     * Tests that the containsAll method returns {@code false}.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testContainsAll(String description, NavigableSet<?> navigableSet) {
        TreeSet treeSet = new TreeSet();
        treeSet.add("1");
        treeSet.add("2");
        treeSet.add("3");

        assertFalse(navigableSet.containsAll(treeSet), "Should not contain any elements.");
    }

    /**
     * Tests that the iterator is empty.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testEmptyIterator(String description, NavigableSet<?> navigableSet) {
        assertFalse(navigableSet.iterator().hasNext(), "The iterator is not empty.");
    }

    /**
     * Tests that the set is empty.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testIsEmpty(String description, NavigableSet<?> navigableSet) {
        assertTrue(navigableSet.isEmpty(), "The set is not empty.");
    }

    /**
     * Tests that the first() method throws NoSuchElementException
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testFirst(String description, NavigableSet<?> navigableSet) {
        assertThrowsNSEE(navigableSet::first, description);
    }

    /**
     * Tests the headSet() method.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testHeadSet(String description, NavigableSet navigableSet) {
        assertThrowsNPE(
            () -> { NavigableSet ns = navigableSet.headSet(null, false); },
            description + ": Must throw NullPointerException for null element");

        assertThrowsCCE(
            () -> { NavigableSet ns = navigableSet.headSet(new Object(), true); },
            description + ": Must throw ClassCastException for non-Comparable element");

        NavigableSet ns = navigableSet.headSet("1", false);

        assertEmptyNavigableSet(ns, description + ": Returned value is not empty navigable set.");
    }

    /**
     * Tests that the last() method throws NoSuchElementException
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testLast(String description, NavigableSet<?> navigableSet) {
        assertThrowsNSEE(navigableSet::last, description);
    }

    /**
     * Tests that the size is 0.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testSizeIsZero(String description, NavigableSet<?> navigableSet) {
        assertTrue(0 == navigableSet.size(), "The size of the set is not 0.");
    }

    /**
     * Tests the subSet() method.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testSubSet(String description, NavigableSet navigableSet) {
        assertThrowsNPE(
            () -> {
                SortedSet ss = navigableSet.subSet(null, BigInteger.TEN);
            },
            description + ": Must throw NullPointerException for null element");

        assertThrowsNPE(
            () -> {
                SortedSet ss = navigableSet.subSet(BigInteger.ZERO, null);
            },
            description + ": Must throw NullPointerException for null element");

        assertThrowsNPE(
            () -> {
                SortedSet ss = navigableSet.subSet(null, null);
            },
            description + ": Must throw NullPointerException for null element");

        Object obj1 = new Object();
        Object obj2 = new Object();

        assertThrowsCCE(
            () -> {
                SortedSet ss = navigableSet.subSet(obj1, BigInteger.TEN);
            },
            description + ": Must throw ClassCastException for parameter which is not Comparable.");

        assertThrowsCCE(
            () -> {
                SortedSet ss = navigableSet.subSet(BigInteger.ZERO, obj2);
            },
            description + ": Must throw ClassCastException for parameter which is not Comparable.");

        assertThrowsCCE(
            () -> {
                SortedSet ss = navigableSet.subSet(obj1, obj2);
            },
            description + ": Must throw ClassCastException for parameter which is not Comparable.");

        // minimal range
        navigableSet.subSet(BigInteger.ZERO, false, BigInteger.ZERO, false);
        navigableSet.subSet(BigInteger.ZERO, false, BigInteger.ZERO, true);
        navigableSet.subSet(BigInteger.ZERO, true, BigInteger.ZERO, false);
        navigableSet.subSet(BigInteger.ZERO, true, BigInteger.ZERO, true);

        Object first = isDescending(navigableSet) ? BigInteger.TEN : BigInteger.ZERO;
        Object last = (BigInteger.ZERO == first) ? BigInteger.TEN : BigInteger.ZERO;

            assertThrowsIAE(
                () -> navigableSet.subSet(last, true, first, false),
                description
                + ": Must throw IllegalArgumentException when fromElement is not less than toElement.");

        navigableSet.subSet(first, true, last, false);
    }

    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testSubSetRanges(String description, NavigableSet navigableSet) {
        Object first = isDescending(navigableSet) ? BigInteger.TEN : BigInteger.ZERO;
        Object last = (BigInteger.ZERO == first) ? BigInteger.TEN : BigInteger.ZERO;

        NavigableSet subSet = navigableSet.subSet(first, true, last, true);

        // same subset
        subSet.subSet(first, true, last, true);

        // slightly smaller
        NavigableSet ns = subSet.subSet(first, false, last, false);
        // slight expansion
        assertThrowsIAE(
            () -> ns.subSet(first, true, last, true),
            description + ": Expansion should not be allowed");

        // much smaller
        subSet.subSet(first, false, BigInteger.ONE, false);
    }

    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testheadSetRanges(String description, NavigableSet navigableSet) {
        NavigableSet subSet = navigableSet.headSet(BigInteger.ONE, true);

        // same subset
        subSet.headSet(BigInteger.ONE, true);

        // slightly smaller
        NavigableSet ns = subSet.headSet(BigInteger.ONE, false);

        // slight expansion
        assertThrowsIAE(
            () -> ns.headSet(BigInteger.ONE, true),
            description + ": Expansion should not be allowed");

        // much smaller
        subSet.headSet(isDescending(subSet) ? BigInteger.TEN : BigInteger.ZERO, true);
    }

    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testTailSetRanges(String description, NavigableSet navigableSet) {
        NavigableSet subSet = navigableSet.tailSet(BigInteger.ONE, true);

        // same subset
        subSet.tailSet(BigInteger.ONE, true);

        // slightly smaller
        NavigableSet ns = subSet.tailSet(BigInteger.ONE, false);

        // slight expansion
        assertThrowsIAE(
            () -> ns.tailSet(BigInteger.ONE, true),
            description + ": Expansion should not be allowed");

        // much smaller
        subSet.tailSet(isDescending(subSet) ? BigInteger.ZERO : BigInteger.TEN, false);
    }

    /**
     * Tests the tailSet() method.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testTailSet(String description, NavigableSet navigableSet) {
        assertThrowsNPE(
            () -> navigableSet.tailSet(null),
            description + ": Must throw NullPointerException for null element");

        assertThrowsCCE(
            () -> navigableSet.tailSet(new Object()),
            description);

        NavigableSet ss = navigableSet.tailSet("1", true);

        assertEmptyNavigableSet(ss, description + ": Returned value is not empty navigable set.");
    }

    /**
     * Tests that the array has a size of 0.
     */
    @Test(dataProvider = "NavigableSet<?>", dataProviderClass = EmptyNavigableSet.class)
    public void testToArray(String description, NavigableSet<?> navigableSet) {
        Object[] emptyNavigableSetArray = navigableSet.toArray();

        assertTrue(emptyNavigableSetArray.length == 0, "Returned non-empty Array.");

        emptyNavigableSetArray = new Object[20];

        Object[] result = navigableSet.toArray(emptyNavigableSetArray);

        assertSame(emptyNavigableSetArray, result);

        assertNull(result[0]);
    }

    @DataProvider(name = "NavigableSet<?>", parallel = true)
    public static Iterator<Object[]> navigableSetsProvider() {
        return makeNavigableSets().iterator();
    }

    public static Collection<Object[]> makeNavigableSets() {
        return Arrays.asList(
            new Object[]{"UnmodifiableNavigableSet(TreeSet)", Collections.unmodifiableNavigableSet(new TreeSet())},
            new Object[]{"UnmodifiableNavigableSet(TreeSet.descendingSet()", Collections.unmodifiableNavigableSet(new TreeSet().descendingSet())},
            new Object[]{"UnmodifiableNavigableSet(TreeSet.descendingSet().descendingSet()", Collections.unmodifiableNavigableSet(new TreeSet().descendingSet().descendingSet())},
            new Object[]{"emptyNavigableSet()", Collections.emptyNavigableSet()},
            new Object[]{"emptyNavigableSet().descendingSet()", Collections.emptyNavigableSet().descendingSet()},
            new Object[]{"emptyNavigableSet().descendingSet().descendingSet()", Collections.emptyNavigableSet().descendingSet().descendingSet()}
        );
    }
}
