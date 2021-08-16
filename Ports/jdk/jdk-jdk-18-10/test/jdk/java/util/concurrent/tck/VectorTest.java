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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import junit.framework.Test;

public class VectorTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return Vector.class; }
            public List emptyCollection() { return new Vector(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return true; }
        }
        class SubListImplementation extends Implementation {
            @SuppressWarnings("unchecked")
            public List emptyCollection() {
                List list = super.emptyCollection();
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                if (rnd.nextBoolean())
                    list.add(makeElement(rnd.nextInt()));
                int i = rnd.nextInt(list.size() + 1);
                return list.subList(i, i);
            }
        }
        return newTestSuite(
                VectorTest.class,
                CollectionTest.testSuite(new Implementation()),
                CollectionTest.testSuite(new SubListImplementation()));
    }

    static Vector<Item> populatedList(int n) {
        Vector<Item> list = new Vector<>();
        assertTrue(list.isEmpty());
        for (int i = 0; i < n; i++)
            mustAdd(list, i);
        mustEqual(n <= 0, list.isEmpty());
        mustEqual(n, list.size());
        return list;
    }

    /**
     * addAll adds each element from the given collection, including duplicates
     */
    public void testAddAll() {
        List<Item> list = populatedList(3);
        assertTrue(list.addAll(Arrays.asList(three, four, five)));
        mustEqual(6, list.size());
        assertTrue(list.addAll(Arrays.asList(three, four, five)));
        mustEqual(9, list.size());
    }

    /**
     * clear removes all elements from the list
     */
    public void testClear() {
        List<Item> list = populatedList(SIZE);
        list.clear();
        mustEqual(0, list.size());
    }

    /**
     * Cloned list is equal
     */
    public void testClone() {
        Vector<Item> l1 = populatedList(SIZE);
        @SuppressWarnings("unchecked")
        Vector<Item> l2 = (Vector<Item>)(l1.clone());
        mustEqual(l1, l2);
        l1.clear();
        assertFalse(l1.equals(l2));
    }

    /**
     * contains is true for added elements
     */
    public void testContains() {
        List<Item> list = populatedList(3);
        mustContain(list, one);
        mustNotContain(list, five);
    }

    /**
     * adding at an index places it in the indicated index
     */
    public void testAddIndex() {
        List<Item> list = populatedList(3);
        list.add(0, minusOne);
        mustEqual(4, list.size());
        mustEqual(minusOne, list.get(0));
        mustEqual(zero, list.get(1));

        list.add(2, minusTwo);
        mustEqual(5, list.size());
        mustEqual(minusTwo, list.get(2));
        mustEqual(two, list.get(4));
    }

    /**
     * lists with same elements are equal and have same hashCode
     */
    public void testEquals() {
        List<Item> a = populatedList(3);
        List<Item> b = populatedList(3);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
        mustEqual(a.hashCode(), b.hashCode());
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

        assertFalse(a.equals(null));
    }

    /**
     * containsAll returns true for collections with subset of elements
     */
    public void testContainsAll() {
        List<Item> list = populatedList(3);
        assertTrue(list.containsAll(Arrays.asList()));
        assertTrue(list.containsAll(Arrays.asList(one)));
        assertTrue(list.containsAll(Arrays.asList(one, two)));
        assertFalse(list.containsAll(Arrays.asList(one, two, six)));
        assertFalse(list.containsAll(Arrays.asList(six)));

        try {
            list.containsAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * get returns the value at the given index
     */
    public void testGet() {
        List<Item> list = populatedList(3);
        mustEqual(0, list.get(0));
    }

    /**
     * indexOf(Object) returns the index of the first occurrence of the
     * specified element in this list, or -1 if this list does not
     * contain the element
     */
    public void testIndexOf() {
        List<Item> list = populatedList(3);
        mustEqual(-1, list.indexOf(minusTen));
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Item I = itemFor(i);
            mustEqual(i, list.indexOf(I));
            mustEqual(i, list.subList(0, size).indexOf(I));
            mustEqual(i, list.subList(0, i + 1).indexOf(I));
            mustEqual(-1, list.subList(0, i).indexOf(I));
            mustEqual(0, list.subList(i, size).indexOf(I));
            mustEqual(-1, list.subList(i + 1, size).indexOf(I));
        }

        list.add(one);
        mustEqual(1, list.indexOf(one));
        mustEqual(1, list.subList(0, size + 1).indexOf(one));
        mustEqual(0, list.subList(1, size + 1).indexOf(one));
        mustEqual(size - 2, list.subList(2, size + 1).indexOf(one));
        mustEqual(0, list.subList(size, size + 1).indexOf(one));
        mustEqual(-1, list.subList(size + 1, size + 1).indexOf(one));
    }

    /**
     * indexOf(E, int) returns the index of the first occurrence of the
     * specified element in this list, searching forwards from index,
     * or returns -1 if the element is not found
     */
    public void testIndexOf2() {
        Vector<Item> list = populatedList(3);
        int size = list.size();
        mustEqual(-1, list.indexOf(minusTen, 0));

        // we might expect IOOBE, but spec says otherwise
        mustEqual(-1, list.indexOf(zero, size));
        mustEqual(-1, list.indexOf(zero, Integer.MAX_VALUE));

        assertThrows(
            IndexOutOfBoundsException.class,
            () -> list.indexOf(zero, -1),
            () -> list.indexOf(zero, Integer.MIN_VALUE));

        for (int i = 0; i < size; i++) {
            Item I = itemFor(i);
            mustEqual(i, list.indexOf(I, 0));
            mustEqual(i, list.indexOf(I, i));
            mustEqual(-1, list.indexOf(I, i + 1));
        }

        list.add(one);
        mustEqual(1, list.indexOf(one, 0));
        mustEqual(1, list.indexOf(one, 1));
        mustEqual(size, list.indexOf(one, 2));
        mustEqual(size, list.indexOf(one, size));
    }

    /**
     * isEmpty returns true when empty, else false
     */
    public void testIsEmpty() {
        List<Item> empty = new Vector<>();
        assertTrue(empty.isEmpty());
        assertTrue(empty.subList(0, 0).isEmpty());

        List<Item> full = populatedList(SIZE);
        assertFalse(full.isEmpty());
        assertTrue(full.subList(0, 0).isEmpty());
        assertTrue(full.subList(SIZE, SIZE).isEmpty());
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        Collection<Item> c = new Vector<>();
        assertIteratorExhausted(c.iterator());
    }

    /**
     * lastIndexOf(Object) returns the index of the last occurrence of
     * the specified element in this list, or -1 if this list does not
     * contain the element
     */
    public void testLastIndexOf1() {
        List<Item> list = populatedList(3);
        mustEqual(-1, list.lastIndexOf(minusTen));
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Item I = itemFor(i);
            mustEqual(i, list.lastIndexOf(I));
            mustEqual(i, list.subList(0, size).lastIndexOf(I));
            mustEqual(i, list.subList(0, i + 1).lastIndexOf(I));
            mustEqual(-1, list.subList(0, i).lastIndexOf(I));
            mustEqual(0, list.subList(i, size).lastIndexOf(I));
            mustEqual(-1, list.subList(i + 1, size).lastIndexOf(I));
        }

        list.add(one);
        mustEqual(size, list.lastIndexOf(one));
        mustEqual(size, list.subList(0, size + 1).lastIndexOf(one));
        mustEqual(1, list.subList(0, size).lastIndexOf(one));
        mustEqual(0, list.subList(1, 2).lastIndexOf(one));
        mustEqual(-1, list.subList(0, 1).indexOf(one));
    }

    /**
     * lastIndexOf(E, int) returns the index of the last occurrence of the
     * specified element in this list, searching backwards from index, or
     * returns -1 if the element is not found
     */
    public void testLastIndexOf2() {
        Vector<Item> list = populatedList(3);

        // we might expect IOOBE, but spec says otherwise
        mustEqual(-1, list.lastIndexOf(zero, -1));

        int size = list.size();
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> list.lastIndexOf(zero, size),
            () -> list.lastIndexOf(zero, Integer.MAX_VALUE));

        for (int i = 0; i < size; i++) {
            Item I = itemFor(i);
            mustEqual(i, list.lastIndexOf(I, i));
            mustEqual(list.indexOf(I), list.lastIndexOf(I, i));
            if (i > 0)
                mustEqual(-1, list.lastIndexOf(I, i - 1));
        }
        list.add(one);
        list.add(three);
        mustEqual(1, list.lastIndexOf(one, 1));
        mustEqual(1, list.lastIndexOf(one, 2));
        mustEqual(3, list.lastIndexOf(one, 3));
        mustEqual(3, list.lastIndexOf(one, 4));
        mustEqual(-1, list.lastIndexOf(three, 3));
    }

    /**
     * size returns the number of elements
     */
    public void testSize() {
        List<Item> empty = new Vector<>();
        mustEqual(0, empty.size());
        mustEqual(0, empty.subList(0, 0).size());

        List<Item> full = populatedList(SIZE);
        mustEqual(SIZE, full.size());
        mustEqual(0, full.subList(0, 0).size());
        mustEqual(0, full.subList(SIZE, SIZE).size());
    }

    /**
     * sublists contains elements at indexes offset from their base
     */
    public void testSubList() {
        List<Item> a = populatedList(10);
        assertTrue(a.subList(1,1).isEmpty());
        for (int j = 0; j < 9; ++j) {
            for (int i = j ; i < 10; ++i) {
                List<Item> b = a.subList(j,i);
                for (int k = j; k < i; ++k) {
                    mustEqual(k, b.get(k-j));
                }
            }
        }

        List<Item> s = a.subList(2, 5);
        mustEqual(3, s.size());
        s.set(2, minusOne);
        mustEqual(a.get(4), minusOne);
        s.clear();
        mustEqual(7, a.size());

        assertThrows(
            IndexOutOfBoundsException.class,
            () -> s.get(0),
            () -> s.set(0, fortytwo));
    }

    /**
     * toArray throws an ArrayStoreException when the given array
     * can not store the objects inside the list
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_ArrayStoreException() {
        List<Item> list = new Vector<>();
        // Items are not auto-converted to Longs
        list.add(eightysix);
        list.add(ninetynine);
        assertThrows(
            ArrayStoreException.class,
            () -> list.toArray(new Long[0]),
            () -> list.toArray(new Long[5]));
    }

    @SuppressWarnings("unchecked")
    void testIndexOutOfBoundsException(List list) {
        int size = list.size();
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> list.get(-1),
            () -> list.get(size),
            () -> list.set(-1, "qwerty"),
            () -> list.set(size, "qwerty"),
            () -> list.add(-1, "qwerty"),
            () -> list.add(size + 1, "qwerty"),
            () -> list.remove(-1),
            () -> list.remove(size),
            () -> list.addAll(-1, Collections.emptyList()),
            () -> list.addAll(size + 1, Collections.emptyList()),
            () -> list.listIterator(-1),
            () -> list.listIterator(size + 1),
            () -> list.subList(-1, size),
            () -> list.subList(0, size + 1));

        // Conversely, operations that must not throw
        list.addAll(0, Collections.emptyList());
        list.addAll(size, Collections.emptyList());
        list.add(0, "qwerty");
        list.add(list.size(), "qwerty");
        list.get(0);
        list.get(list.size() - 1);
        list.set(0, "azerty");
        list.set(list.size() - 1, "azerty");
        list.listIterator(0);
        list.listIterator(list.size());
        list.subList(0, list.size());
        list.remove(list.size() - 1);
    }

    /**
     * IndexOutOfBoundsException is thrown when specified
     */
    public void testIndexOutOfBoundsException() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Item> x = populatedList(rnd.nextInt(5));
        testIndexOutOfBoundsException(x);

        int start = rnd.nextInt(x.size() + 1);
        int end = rnd.nextInt(start, x.size() + 1);

        // Vector#subList spec deviates slightly from List#subList spec
        assertThrows(
            IllegalArgumentException.class,
            () -> x.subList(start, start - 1));

        List<Item> subList = x.subList(start, end);
        testIndexOutOfBoundsException(x);
    }

    /**
     * a deserialized/reserialized list equals original
     */
    public void testSerialization() throws Exception {
        List<Item> x = populatedList(SIZE);
        List<Item> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x.toString(), y.toString());
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        mustEqual(x, y);
        mustEqual(y, x);
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.remove(0), y.remove(0));
        }
        assertTrue(y.isEmpty());
    }

    /**
     * tests for setSize()
     */
    public void testSetSize() {
        final Vector<Item> v = new Vector<>();
        for (int n : new int[] { 100, 5, 50 }) {
            v.setSize(n);
            mustEqual(n, v.size());
            assertNull(v.get(0));
            assertNull(v.get(n - 1));
            assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> v.setSize(-1));
            mustEqual(n, v.size());
            assertNull(v.get(0));
            assertNull(v.get(n - 1));
        }
    }

}
