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
import java.util.concurrent.atomic.AtomicLongArray;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicLongArrayTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicLongArrayTest.class);
    }

    /**
     * constructor creates array of given size with all elements zero
     */
    public void testConstructor() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++)
            assertEquals(0, aa.get(i));
    }

    /**
     * constructor with null array throws NPE
     */
    public void testConstructor2NPE() {
        try {
            long[] a = null;
            new AtomicLongArray(a);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * constructor with array is of same size and has all elements
     */
    public void testConstructor2() {
        long[] a = { 17L, 3L, -42L, 99L, -7L };
        AtomicLongArray aa = new AtomicLongArray(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], aa.get(i));
    }

    /**
     * get and set for out of bound indices throw IndexOutOfBoundsException
     */
    @SuppressWarnings("deprecation")
    public void testIndexing() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int index : new int[] { -1, SIZE }) {
            try {
                aa.get(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.set(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.lazySet(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndSet(index, 1, 2);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSet(index, 1, 2);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.getAndAdd(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.addAndGet(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
        }
    }

    /**
     * get returns the last value set at index
     */
    public void testGetSet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.get(i));
            aa.set(i, 2);
            assertEquals(2, aa.get(i));
            aa.set(i, -3);
            assertEquals(-3, aa.get(i));
        }
    }

    /**
     * get returns the last value lazySet at index by same thread
     */
    public void testGetLazySet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.lazySet(i, 1);
            assertEquals(1, aa.get(i));
            aa.lazySet(i, 2);
            assertEquals(2, aa.get(i));
            aa.lazySet(i, -3);
            assertEquals(-3, aa.get(i));
        }
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertTrue(aa.compareAndSet(i, 1, 2));
            assertTrue(aa.compareAndSet(i, 2, -4));
            assertEquals(-4, aa.get(i));
            assertFalse(aa.compareAndSet(i, -5, 7));
            assertEquals(-4, aa.get(i));
            assertTrue(aa.compareAndSet(i, -4, 7));
            assertEquals(7, aa.get(i));
        }
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws InterruptedException {
        final AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!a.compareAndSet(0, 2, 3))
                    Thread.yield();
            }});

        t.start();
        assertTrue(a.compareAndSet(0, 1, 2));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertEquals(3, a.get(0));
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    @SuppressWarnings("deprecation")
    public void testWeakCompareAndSet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            do {} while (!aa.weakCompareAndSet(i, 1, 2));
            do {} while (!aa.weakCompareAndSet(i, 2, -4));
            assertEquals(-4, aa.get(i));
            do {} while (!aa.weakCompareAndSet(i, -4, 7));
            assertEquals(7, aa.get(i));
        }
    }

    /**
     * getAndSet returns previous value and sets to given value at given index
     */
    public void testGetAndSet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndSet(i, 0));
            assertEquals(0, aa.getAndSet(i, -10));
            assertEquals(-10, aa.getAndSet(i, 1));
        }
    }

    /**
     * getAndAdd returns previous value and adds given value
     */
    public void testGetAndAdd() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndAdd(i, 2));
            assertEquals(3, aa.get(i));
            assertEquals(3, aa.getAndAdd(i, -4));
            assertEquals(-1, aa.get(i));
        }
    }

    /**
     * getAndDecrement returns previous value and decrements
     */
    public void testGetAndDecrement() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndDecrement(i));
            assertEquals(0, aa.getAndDecrement(i));
            assertEquals(-1, aa.getAndDecrement(i));
        }
    }

    /**
     * getAndIncrement returns previous value and increments
     */
    public void testGetAndIncrement() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndIncrement(i));
            assertEquals(2, aa.get(i));
            aa.set(i, -2);
            assertEquals(-2, aa.getAndIncrement(i));
            assertEquals(-1, aa.getAndIncrement(i));
            assertEquals(0, aa.getAndIncrement(i));
            assertEquals(1, aa.get(i));
        }
    }

    /**
     * addAndGet adds given value to current, and returns current value
     */
    public void testAddAndGet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(3, aa.addAndGet(i, 2));
            assertEquals(3, aa.get(i));
            assertEquals(-1, aa.addAndGet(i, -4));
            assertEquals(-1, aa.get(i));
        }
    }

    /**
     * decrementAndGet decrements and returns current value
     */
    public void testDecrementAndGet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(0, aa.decrementAndGet(i));
            assertEquals(-1, aa.decrementAndGet(i));
            assertEquals(-2, aa.decrementAndGet(i));
            assertEquals(-2, aa.get(i));
        }
    }

    /**
     * incrementAndGet increments and returns current value
     */
    public void testIncrementAndGet() {
        AtomicLongArray aa = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(2, aa.incrementAndGet(i));
            assertEquals(2, aa.get(i));
            aa.set(i, -2);
            assertEquals(-1, aa.incrementAndGet(i));
            assertEquals(0, aa.incrementAndGet(i));
            assertEquals(1, aa.incrementAndGet(i));
            assertEquals(1, aa.get(i));
        }
    }

    class Counter extends CheckedRunnable {
        final AtomicLongArray aa;
        int decs;
        Counter(AtomicLongArray a) { aa = a; }
        public void realRun() {
            for (;;) {
                boolean done = true;
                for (int i = 0; i < aa.length(); i++) {
                    long v = aa.get(i);
                    assertTrue(v >= 0);
                    if (v != 0) {
                        done = false;
                        if (aa.compareAndSet(i, v, v - 1))
                            decs++;
                    }
                }
                if (done)
                    break;
            }
        }
    }

    /**
     * Multiple threads using same array of counters successfully
     * update a number of times equal to total count
     */
    public void testCountingInMultipleThreads() throws InterruptedException {
        final AtomicLongArray aa = new AtomicLongArray(SIZE);
        long countdown = 10000;
        for (int i = 0; i < SIZE; i++)
            aa.set(i, countdown);
        Counter c1 = new Counter(aa);
        Counter c2 = new Counter(aa);
        Thread t1 = newStartedThread(c1);
        Thread t2 = newStartedThread(c2);
        t1.join();
        t2.join();
        assertEquals(c1.decs + c2.decs, SIZE * countdown);
    }

    /**
     * a deserialized/reserialized array holds same values in same order
     */
    public void testSerialization() throws Exception {
        AtomicLongArray x = new AtomicLongArray(SIZE);
        for (int i = 0; i < SIZE; i++)
            x.set(i, -i);
        AtomicLongArray y = serialClone(x);
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
        long[] a = { 17, 3, -42, 99, -7 };
        AtomicLongArray aa = new AtomicLongArray(a);
        assertEquals(Arrays.toString(a), aa.toString());
    }

}
