/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import junit.framework.Test;

public class CopyOnWriteArraySetTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return CopyOnWriteArraySet.class; }
            public Set emptyCollection() { return new CopyOnWriteArraySet(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return true; }
        }
        return newTestSuite(
                CopyOnWriteArraySetTest.class,
                CollectionTest.testSuite(new Implementation()));
    }

    static CopyOnWriteArraySet<Item> populatedSet(int n) {
        CopyOnWriteArraySet<Item> a = new CopyOnWriteArraySet<>();
        assertTrue(a.isEmpty());
        for (int i = 0; i < n; i++)
            mustAdd(a, i);
        mustEqual(n == 0, a.isEmpty());
        mustEqual(n, a.size());
        return a;
    }

    static CopyOnWriteArraySet<Item> populatedSet(Item[] elements) {
        CopyOnWriteArraySet<Item> a = new CopyOnWriteArraySet<>();
        assertTrue(a.isEmpty());
        for (int i = 0; i < elements.length; i++)
            mustAdd(a, elements[i]);
        assertFalse(a.isEmpty());
        mustEqual(elements.length, a.size());
        return a;
    }

    /**
     * Default-constructed set is empty
     */
    public void testConstructor() {
        CopyOnWriteArraySet<Item> a = new CopyOnWriteArraySet<>();
        assertTrue(a.isEmpty());
    }

    /**
     * Collection-constructed set holds all of its elements
     */
    public void testConstructor3() {
        Item[] items = defaultItems;
        CopyOnWriteArraySet<Item> a = new CopyOnWriteArraySet<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustContain(a, i);
    }

    /**
     * addAll adds each non-duplicate element from the given collection
     */
    public void testAddAll() {
        Set<Item> full = populatedSet(3);
        assertTrue(full.addAll(Arrays.asList(three, four, five)));
        mustEqual(6, full.size());
        assertFalse(full.addAll(Arrays.asList(three, four, five)));
        mustEqual(6, full.size());
    }

    /**
     * addAll adds each non-duplicate element from the given collection
     */
    public void testAddAll2() {
        Set<Item> full = populatedSet(3);
        // "one" is duplicate and will not be added
        assertTrue(full.addAll(Arrays.asList(three, four, one)));
        mustEqual(5, full.size());
        assertFalse(full.addAll(Arrays.asList(three, four, one)));
        mustEqual(5, full.size());
    }

    /**
     * add will not add the element if it already exists in the set
     */
    public void testAdd2() {
        Set<Item> full = populatedSet(3);
        full.add(one);
        mustEqual(3, full.size());
    }

    /**
     * add adds the element when it does not exist in the set
     */
    public void testAdd3() {
        Set<Item> full = populatedSet(3);
        full.add(three);
        mustContain(full, three);
    }

    /**
     * clear removes all elements from the set
     */
    public void testClear() {
        Collection<Item> full = populatedSet(3);
        full.clear();
        mustEqual(0, full.size());
        assertTrue(full.isEmpty());
    }

    /**
     * contains returns true for added elements
     */
    public void testContains() {
        Collection<Item> full = populatedSet(3);
        mustContain(full, one);
        mustNotContain(full, five);
    }

    /**
     * Sets with equal elements are equal
     */
    public void testEquals() {
        CopyOnWriteArraySet<Item> a = populatedSet(3);
        CopyOnWriteArraySet<Item> b = populatedSet(3);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
        mustEqual(a.hashCode(), b.hashCode());
        mustEqual(a.size(), b.size());

        a.add(minusOne);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertTrue(a.containsAll(b));
        assertFalse(b.containsAll(a));
        b.add(minusOne);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
        mustEqual(a.hashCode(), b.hashCode());

        Item x = a.iterator().next();
        a.remove(x);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        assertFalse(a.containsAll(b));
        assertTrue(b.containsAll(a));
        a.add(x);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
        mustEqual(a.hashCode(), b.hashCode());
        mustEqual(a.size(), b.size());

        CopyOnWriteArraySet<Item> empty1 = new CopyOnWriteArraySet<>(Arrays.asList());
        CopyOnWriteArraySet<Item> empty2 = new CopyOnWriteArraySet<>(Arrays.asList());
        assertTrue(empty1.equals(empty1));
        assertTrue(empty1.equals(empty2));

        assertFalse(empty1.equals(a));
        assertFalse(a.equals(empty1));

        assertFalse(a.equals(null));
    }

    /**
     * containsAll returns true for collections with subset of elements
     */
    public void testContainsAll() {
        Collection<Item> full = populatedSet(3);
        assertTrue(full.containsAll(full));
        assertTrue(full.containsAll(Arrays.asList()));
        assertTrue(full.containsAll(Arrays.asList(one)));
        assertTrue(full.containsAll(Arrays.asList(one, two)));
        assertFalse(full.containsAll(Arrays.asList(one, two, six)));
        assertFalse(full.containsAll(Arrays.asList(six)));

        CopyOnWriteArraySet<Item> empty1 = new CopyOnWriteArraySet<>(Arrays.asList());
        CopyOnWriteArraySet<Item> empty2 = new CopyOnWriteArraySet<>(Arrays.asList());
        assertTrue(empty1.containsAll(empty2));
        assertTrue(empty1.containsAll(empty1));
        assertFalse(empty1.containsAll(full));
        assertTrue(full.containsAll(empty1));

        try {
            full.containsAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * isEmpty is true when empty, else false
     */
    public void testIsEmpty() {
        assertTrue(populatedSet(0).isEmpty());
        assertFalse(populatedSet(3).isEmpty());
    }

    /**
     * iterator() returns an iterator containing the elements of the
     * set in insertion order
     */
    public void testIterator() {
        Collection<Item> empty = new CopyOnWriteArraySet<>();
        assertFalse(empty.iterator().hasNext());
        try {
            empty.iterator().next();
            shouldThrow();
        } catch (NoSuchElementException success) {}

        Item[] elements = seqItems(SIZE);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        Iterator<? extends Item> it = full.iterator();
        for (int j = 0; j < SIZE; j++) {
            assertTrue(it.hasNext());
            mustEqual(elements[j], it.next());
        }
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(new CopyOnWriteArraySet<Item>().iterator());
    }

    /**
     * iterator remove is unsupported
     */
    public void testIteratorRemove() {
        Collection<Item> full = populatedSet(3);
        Iterator<? extends Item> it = full.iterator();
        it.next();
        try {
            it.remove();
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
    }

    /**
     * toString holds toString of elements
     */
    public void testToString() {
        mustEqual("[]", new CopyOnWriteArraySet<Item>().toString());
        Collection<Item> full = populatedSet(3);
        String s = full.toString();
        for (int i = 0; i < 3; ++i)
            assertTrue(s.contains(String.valueOf(i)));
        mustEqual(new ArrayList<Item>(full).toString(),
                     full.toString());
    }

    /**
     * removeAll removes all elements from the given collection
     */
    public void testRemoveAll() {
        Set<Item> full = populatedSet(3);
        assertTrue(full.removeAll(Arrays.asList(one, two)));
        mustEqual(1, full.size());
        assertFalse(full.removeAll(Arrays.asList(one, two)));
        mustEqual(1, full.size());
    }

    /**
     * remove removes an element
     */
    public void testRemove() {
        Collection<Item> full = populatedSet(3);
        full.remove(one);
        mustNotContain(full, one);
        mustEqual(2, full.size());
    }

    /**
     * size returns the number of elements
     */
    public void testSize() {
        Collection<Item> empty = new CopyOnWriteArraySet<>();
        Collection<Item> full = populatedSet(3);
        mustEqual(3, full.size());
        mustEqual(0, empty.size());
    }

    /**
     * toArray() returns an Object array containing all elements from
     * the set in insertion order
     */
    public void testToArray() {
        Object[] a = new CopyOnWriteArraySet<>().toArray();
        assertTrue(Arrays.equals(new Object[0], a));
        assertSame(Object[].class, a.getClass());

        Item[] elements = seqItems(SIZE);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        assertTrue(Arrays.equals(elements, full.toArray()));
        assertSame(Object[].class, full.toArray().getClass());
    }

    /**
     * toArray(Item array) returns an Item array containing all
     * elements from the set in insertion order
     */
    public void testToArray2() {
        Collection<Item> empty = new CopyOnWriteArraySet<>();
        Item[] a;

        a = new Item[0];
        assertSame(a, empty.toArray(a));

        a = new Item[SIZE / 2];
        Arrays.fill(a, fortytwo);
        assertSame(a, empty.toArray(a));
        assertNull(a[0]);
        for (int i = 1; i < a.length; i++)
            mustEqual(42, a[i]);

        Item[] elements = seqItems(SIZE);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        Arrays.fill(a, fortytwo);
        assertTrue(Arrays.equals(elements, full.toArray(a)));
        for (int i = 0; i < a.length; i++)
            mustEqual(42, a[i]);
        assertSame(Item[].class, full.toArray(a).getClass());

        a = new Item[SIZE];
        Arrays.fill(a, fortytwo);
        assertSame(a, full.toArray(a));
        assertTrue(Arrays.equals(elements, a));

        a = new Item[2 * SIZE];
        Arrays.fill(a, fortytwo);
        assertSame(a, full.toArray(a));
        assertTrue(Arrays.equals(elements, Arrays.copyOf(a, SIZE)));
        assertNull(a[SIZE]);
        for (int i = SIZE + 1; i < a.length; i++)
            mustEqual(42, a[i]);
    }

    /**
     * toArray throws an ArrayStoreException when the given array can
     * not store the objects inside the set
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_ArrayStoreException() {
        CopyOnWriteArraySet<Item> c = new CopyOnWriteArraySet<>();
        c.add(one);
        c.add(two);
        try {
            c.toArray(new Long[5]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * A deserialized/reserialized set equals original
     */
    public void testSerialization() throws Exception {
        Set<Item> x = populatedSet(SIZE);
        Set<Item> y = serialClone(x);

        assertNotSame(y, x);
        mustEqual(x.size(), y.size());
        mustEqual(x.toString(), y.toString());
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        mustEqual(x, y);
        mustEqual(y, x);
    }

    /**
     * addAll is idempotent
     */
    public void testAddAll_idempotent() throws Exception {
        Set<Item> x = populatedSet(SIZE);
        Set<Item> y = new CopyOnWriteArraySet<>(x);
        y.addAll(x);
        mustEqual(x, y);
        mustEqual(y, x);
    }

}
