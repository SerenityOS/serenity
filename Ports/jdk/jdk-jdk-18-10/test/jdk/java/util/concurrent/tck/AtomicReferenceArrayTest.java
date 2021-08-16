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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicReferenceArrayTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicReferenceArrayTest.class);
    }

    /**
     * constructor creates array of given size with all elements null
     */
    public void testConstructor() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            assertNull(aa.get(i));
        }
    }

    /**
     * constructor with null array throws NPE
     */
    public void testConstructor2NPE() {
        try {
            Integer[] a = null;
            new AtomicReferenceArray<Integer>(a);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * constructor with array is of same size and has all elements
     */
    public void testConstructor2() {
        Item[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], aa.get(i));
    }

    /**
     * Initialize AtomicReferenceArray<Class> with SubClass[]
     */
    public void testConstructorSubClassArray() {
        Item[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Number> aa = new AtomicReferenceArray<>(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++) {
            assertSame(a[i], aa.get(i));
            Long x = Long.valueOf(i);
            aa.set(i, x);
            assertSame(x, aa.get(i));
        }
    }

    /**
     * get and set for out of bound indices throw IndexOutOfBoundsException
     */
    @SuppressWarnings("deprecation")
    public void testIndexing() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<>(SIZE);
        for (int index : new int[] { -1, SIZE }) {
            try {
                aa.get(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.set(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.lazySet(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndSet(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSet(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
        }
    }

    /**
     * get returns the last value set at index
     */
    public void testGetSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertSame(one, aa.get(i));
            aa.set(i, two);
            assertSame(two, aa.get(i));
            aa.set(i, minusThree);
            assertSame(minusThree, aa.get(i));
        }
    }

    /**
     * get returns the last value lazySet at index by same thread
     */
    public void testGetLazySet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.lazySet(i, one);
            assertSame(one, aa.get(i));
            aa.lazySet(i, two);
            assertSame(two, aa.get(i));
            aa.lazySet(i, minusThree);
            assertSame(minusThree, aa.get(i));
        }
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertTrue(aa.compareAndSet(i, one, two));
            assertTrue(aa.compareAndSet(i, two, minusFour));
            assertSame(minusFour, aa.get(i));
            assertFalse(aa.compareAndSet(i, minusFive, seven));
            assertSame(minusFour, aa.get(i));
            assertTrue(aa.compareAndSet(i, minusFour, seven));
            assertSame(seven, aa.get(i));
        }
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws InterruptedException {
        final AtomicReferenceArray<Item> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!a.compareAndSet(0, two, three))
                    Thread.yield();
            }});

        t.start();
        assertTrue(a.compareAndSet(0, one, two));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(three, a.get(0));
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    @SuppressWarnings("deprecation")
    public void testWeakCompareAndSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSet(i, one, two));
            do {} while (!aa.weakCompareAndSet(i, two, minusFour));
            assertSame(minusFour, aa.get(i));
            do {} while (!aa.weakCompareAndSet(i, minusFour, seven));
            assertSame(seven, aa.get(i));
        }
    }

    /**
     * getAndSet returns previous value and sets to given value at given index
     */
    public void testGetAndSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertSame(one, aa.getAndSet(i, zero));
            assertSame(zero, aa.getAndSet(i, minusTen));
            assertSame(minusTen, aa.getAndSet(i, one));
        }
    }

    /**
     * a deserialized/reserialized array holds same values in same order
     */
    public void testSerialization() throws Exception {
        AtomicReferenceArray<Item> x = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            x.set(i, minusOne);
        }
        AtomicReferenceArray<Item> y = serialClone(x);
        assertNotSame(x, y);
        assertEquals(x.length(), y.length());
        for (int i = 0; i < SIZE; i++) {
            assertEquals(x.get(i), y.get(i));
        }
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        Item[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(a);
        assertEquals(Arrays.toString(a), aa.toString());
    }

}
