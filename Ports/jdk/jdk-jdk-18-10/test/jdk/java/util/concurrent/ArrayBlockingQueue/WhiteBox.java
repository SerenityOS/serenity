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
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @modules java.base/java.util.concurrent:open
 * @run testng WhiteBox
 * @summary White box tests of implementation details
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

@Test
public class WhiteBox {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final MethodHandles.Lookup lookup = MethodHandles.lookup();
    final VarHandle ITRS, ITEMS, TAKEINDEX, PUTINDEX, COUNT, HEAD, NEXT, PREVTAKEINDEX;

    WhiteBox() throws ReflectiveOperationException {
        Class<?> qClass = ArrayBlockingQueue.class;
        Class<?> itrClass  = Class.forName(qClass.getName() + "$Itr");
        Class<?> itrsClass = Class.forName(qClass.getName() + "$Itrs");
        Class<?> nodeClass = Class.forName(itrsClass.getName() + "$Node");
        ITRS      = findVarHandle(qClass, "itrs", itrsClass);
        ITEMS     = findVarHandle(qClass, "items", Object[].class);
        TAKEINDEX = findVarHandle(qClass, "takeIndex", int.class);
        PUTINDEX  = findVarHandle(qClass, "putIndex", int.class);
        COUNT     = findVarHandle(qClass, "count", int.class);
        HEAD      = findVarHandle(itrsClass, "head", nodeClass);
        NEXT      = findVarHandle(nodeClass, "next", nodeClass);
        PREVTAKEINDEX = findVarHandle(itrClass, "prevTakeIndex", int.class);
    }

    VarHandle findVarHandle(Class<?> recv, String name, Class<?> type)
        throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(recv, lookup)
            .findVarHandle(recv, name, type);
    }

    Object itrs(ArrayBlockingQueue q) { return ITRS.get(q); }
    Object[] items(ArrayBlockingQueue q) { return (Object[]) ITEMS.get(q); }
    int takeIndex(ArrayBlockingQueue q) { return (int) TAKEINDEX.get(q); }
    int putIndex(ArrayBlockingQueue q) { return (int) PUTINDEX.get(q); }
    int count(ArrayBlockingQueue q) { return (int) COUNT.get(q); }
    Object head(Object itrs) { return HEAD.get(itrs); }
    Object next(Object node) { return NEXT.get(node); }
    int prevTakeIndex(Iterator itr) { return (int) PREVTAKEINDEX.get(itr); }

    boolean isDetached(Iterator it) {
        return prevTakeIndex(it) < 0;
    }

    void assertIteratorExhausted(Iterator it) {
        if (rnd.nextBoolean()) {
            assertTrue(!it.hasNext());
            assertTrue(isDetached(it));
        }
        if (rnd.nextBoolean()) {
            it.forEachRemaining(e -> { throw new AssertionError(); });
            assertTrue(isDetached(it));
        }
        if (rnd.nextBoolean())
            try { it.next(); fail("should throw"); }
            catch (NoSuchElementException success) {}
    }

    List<Iterator> trackedIterators(ArrayBlockingQueue q) {
        List<Iterator> its = new ArrayList<>();
        Object itrs = itrs(q);
        if (itrs != null) {
            for (Object p = head(itrs); p != null; p = next(p))
                its.add(((WeakReference<Iterator>)(p)).get());
            Collections.reverse(its);
        }
        return its;
    }

    List<Iterator> attachedIterators(ArrayBlockingQueue q) {
        List<Iterator> its = new ArrayList<>();
        Object itrs = itrs(q);
        if (itrs != null) {
            for (Object p = head(itrs); p != null; p = next(p)) {
                Iterator it = ((WeakReference<Iterator>)(p)).get();
                if (it != null && !isDetached(it))
                    its.add(it);
            }
            Collections.reverse(its);
        }
        return its;
    }

    void assertRemoveThrowsISE(Iterator it) {
        if (rnd.nextBoolean())
            try { it.remove(); fail("should throw"); }
            catch (IllegalStateException success) {}
    }

    void assertRemoveHasNoEffect(Iterator it, Collection c) {
        if (rnd.nextBoolean()) {
            int size = c.size();
            it.remove(); // no effect
            assertEquals(c.size(), size);
            assertRemoveThrowsISE(it);
        }
    }

    void checkIterationSanity(Queue q) {
        if (rnd.nextBoolean())
            return;
        int size = q.size();
        Object[] a = q.toArray();
        Object[] b = new Object[size+2];
        Arrays.fill(b, Boolean.TRUE);
        Object[] c = q.toArray(b);
        assertEquals(a.length, size);
        assertSame(b, c);
        assertNull(b[size]);
        assertSame(b[size+1], Boolean.TRUE);
        assertEquals(q.toString(), Arrays.toString(a));
        Integer[] xx = null, yy = null;
        if (size > 0) {
            xx = new Integer[size - 1];
            Arrays.fill(xx, 42);
            yy = ((Queue<Integer>)q).toArray(xx);
            for (Integer zz : xx)
                assertEquals(42, (int) zz);
        }
        Iterator it = q.iterator();
        for (int i = 0; i < size; i++) {
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            Object x = it.next();
            assertSame(x, a[i]);
            assertSame(x, b[i]);
            assertSame(x, yy[i]);
        }
        if (rnd.nextBoolean()) assertTrue(!it.hasNext());
    }

    /**
     * Instead of having putIndex (and takeIndex) at the initial
     * default of 0, move them to a random location.
     */
    void randomizePutIndex(ArrayBlockingQueue q) {
        assertTrue(q.isEmpty());
        int capacity = q.remainingCapacity();
        int n = rnd.nextInt(capacity + 1);
        int putIndex = putIndex(q);
        for (int i = n; i-->0; ) q.add(Boolean.TRUE);
        for (int i = n; i-->0; ) q.remove();
        assertEquals(putIndex(q), (putIndex + n) % items(q).length);
    }

    /** No guarantees, but effective in practice. */
    static void forceFullGc() {
        long timeoutMillis = 1000L;
        CountDownLatch finalized = new CountDownLatch(1);
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        WeakReference<Object> ref = new WeakReference<>(
            new Object() { protected void finalize() { finalized.countDown(); }},
            queue);
        try {
            for (int tries = 3; tries--> 0; ) {
                System.gc();
                if (finalized.await(timeoutMillis, MILLISECONDS)
                    && queue.remove(timeoutMillis) != null
                    && ref.get() == null) {
                    System.runFinalization(); // try to pick up stragglers
                    return;
                }
                timeoutMillis *= 4;
            }
        } catch (InterruptedException unexpected) {
            throw new AssertionError("unexpected InterruptedException");
        }
        throw new AssertionError("failed to do a \"full\" gc");
    }

    static void gcAwait(BooleanSupplier s) {
        for (int i = 0; i < 10; i++) {
            if (s.getAsBoolean())
                return;
            forceFullGc();
        }
        throw new AssertionError("failed to satisfy condition");
    }

    public void clear_willClearItrs() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(2, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            assertTrue(q.add(i));
        assertNull(itrs(q));
        for (int i = 0; i < capacity; i++) {
            its.add(q.iterator());
            assertEquals(trackedIterators(q), its);
            q.poll();
            q.add(capacity + i);
        }
        q.clear();
        assertNull(itrs(q));
        int j = 0;
        for (Iterator it : its) {
            assertTrue(isDetached(it));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            if (rnd.nextBoolean()) {
                assertEquals(it.next(), j);
                assertIteratorExhausted(it);
            }
            j++;
        }
    }

    public void queueEmptyingWillClearItrs() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(2, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            q.add(i);
        assertNull(itrs(q));
        for (int i = 0; i < capacity; i++) {
            its.add(q.iterator());
            assertEquals(trackedIterators(q), its);
            q.poll();
            q.add(capacity+i);
        }
        for (int i = 0; i < capacity; i++)
            q.poll();
        assertNull(itrs(q));
        int j = 0;
        for (Iterator it : its) {
            assertTrue(isDetached(it));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            if (rnd.nextBoolean()) {
                assertEquals(it.next(), j);
                assertIteratorExhausted(it);
            }
            j++;
        }
    }

    public void advancing2CyclesWillRemoveIterators() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(2, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            q.add(i);
        assertNull(itrs(q));
        for (int i = capacity; i < 3 * capacity; i++) {
            its.add(q.iterator());
            assertEquals(trackedIterators(q), its);
            q.poll();
            q.add(i);
        }
        for (int i = 3 * capacity; i < 4 * capacity; i++) {
            assertEquals(trackedIterators(q), its.subList(capacity,2*capacity));
            q.poll();
            q.add(i);
        }
        assertNull(itrs(q));
        int j = 0;
        for (Iterator it : its) {
            assertTrue(isDetached(it));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            if (rnd.nextBoolean()) {
                assertEquals(it.next(), j);
                assertIteratorExhausted(it);
            }
            j++;
        }
    }

    /**
     * Interior removal of elements used by an iterator will cause it
     * to be untracked.
     */
    public void interiorRemovalOfElementsUsedByIterator() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        q.add(0);
        for (int i = 1; i < 2 * capacity; i++) {
            q.add(i);
            Integer[] elts = { -1, -2, -3 };
            for (Integer elt : elts) q.add(elt);
            assertEquals(q.remove(), i - 1);
            Iterator it = q.iterator();
            assertEquals(it.next(), i);
            assertEquals(it.next(), elts[0]);
            Collections.shuffle(Arrays.asList(elts));
            assertTrue(q.remove(elts[0]));
            assertTrue(q.remove(elts[1]));
            assertEquals(trackedIterators(q), Collections.singletonList(it));
            assertTrue(q.remove(elts[2]));
            assertNull(itrs(q));
            assertEquals(it.next(), -2);
            assertIteratorExhausted(it);
            assertTrue(isDetached(it));
        }
    }

    public void iteratorsOnEmptyQueue() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(1, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        for (int i = 0; i < 4; i++) {
            Iterator it = q.iterator();
            assertNull(itrs(q));
            assertIteratorExhausted(it);
            assertTrue(isDetached(it));
            assertRemoveThrowsISE(it);
        }
    }

    public void interiorRemovalOfIteratorsLastElement() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(3, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            q.add(i);
        for (int i = 0; i < capacity; i++) {
            Iterator it = q.iterator();
            its.add(it);
            for (int j = 0; j < i; j++)
                assertEquals(j, it.next());
            assertEquals(attachedIterators(q), its);
        }
        q.remove(capacity - 1);
        assertEquals(attachedIterators(q), its);
        for (int i = 1; i < capacity - 1; i++) {
            q.remove(capacity - i - 1);
            Iterator it = its.get(capacity - i);
            assertTrue(isDetached(it));
            assertEquals(attachedIterators(q), its.subList(0, capacity - i));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            assertEquals(it.next(), capacity - i);
            assertIteratorExhausted(it);
        }
        assertEquals(attachedIterators(q), its.subList(0, 2));
        q.remove(0);
        assertTrue(q.isEmpty());
        assertNull(itrs(q));
        Iterator it = its.get(0);
        assertEquals(it.next(), 0);
        assertRemoveHasNoEffect(it, q);
        assertIteratorExhausted(it);
        assertTrue(isDetached(it));
        assertRemoveHasNoEffect(its.get(1), q);
    }

    /**
     * Checks "interior" removal of alternating elements, straddling 2 cycles
     */
    public void interiorRemovalOfAlternatingElements() {
        boolean fair = rnd.nextBoolean();
        int capacity = 2 * rnd.nextInt(2, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        List<Iterator> its = new ArrayList<>();
        // Move takeIndex to middle
        for (int i = 0; i < capacity/2; i++) {
            assertTrue(q.add(i));
            assertEquals(q.poll(), i);
        }
        assertEquals(takeIndex(q), capacity/2);
        for (int i = 0; i < capacity; i++)
            q.add(i);
        for (int i = 0; i < capacity; i++) {
            Iterator it = q.iterator();
            its.add(it);
            for (int j = 0; j < i; j++)
                assertEquals(j, it.next());
            assertEquals(attachedIterators(q), its);
        }

        // Remove all even elements, in either direction, using
        // q.remove(), or iterator.remove()
        switch (rnd.nextInt(3)) {
        case 0:
            for (int i = 0; i < capacity; i+=2) assertTrue(q.remove(i));
            break;
        case 1:
            for (int i = capacity - 2; i >= 0; i-=2) assertTrue(q.remove(i));
            break;
        case 2:
            Iterator it = q.iterator();
            while (it.hasNext()) {
                int i = (Integer) it.next();
                if ((i & 1) == 0)
                    it.remove();
            }
            break;
        default: throw new AssertionError();
        }
        assertEquals(attachedIterators(q), its);

        for (int i = 0; i < capacity; i++) {
            Iterator it = its.get(i);
            boolean even = ((i & 1) == 0);
            if (even) {
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertEquals(i, it.next());
                for (int j = i+1; j < capacity; j += 2)
                    assertEquals(j, it.next());
                assertTrue(!isDetached(it));
                assertTrue(!it.hasNext());
                assertTrue(isDetached(it));
            } else { /* odd */
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertRemoveHasNoEffect(it, q);
                assertEquals(i, it.next());
                for (int j = i+2; j < capacity; j += 2)
                    assertEquals(j, it.next());
                assertTrue(!isDetached(it));
                assertTrue(!it.hasNext());
                assertTrue(isDetached(it));
            }
        }
        assertEquals(trackedIterators(q), Collections.emptyList());
        assertNull(itrs(q));
    }

    public void garbageCollectionOfUnreachableIterators() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(1, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++) q.add(i);
        for (int i = 0; i < capacity; i++) its.add(q.iterator());
        assertEquals(attachedIterators(q), its);
        its = null;
        gcAwait(() -> {
            List<Iterator> trackedIterators = trackedIterators(q);
            assertEquals(trackedIterators.size(), capacity);
            for (Iterator x : trackedIterators)
                if (x != null) return false;
            return true;
        });
        Iterator it = q.iterator(); //
        assertEquals(trackedIterators(q), Collections.singletonList(it));
    }

    public void garbageCollectionOfUnreachableIteratorsWithRandomlyRetainedSubset() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        List<Iterator> its = new ArrayList<>();
        List<Iterator> retained = new ArrayList<>();
        final int size = 1 + rnd.nextInt(capacity);
        for (int i = 0; i < size; i++) q.add(i);
        for (int i = 0; i < size; i++) its.add(q.iterator());
        assertEquals(attachedIterators(q), its);
        // Leave sufficient gaps in retained
        for (int i = 0; i < size; i+= 2+rnd.nextInt(3))
            retained.add(its.get(i));
        its = null;
        gcAwait(() -> {
            List<Iterator> trackedIterators = trackedIterators(q);
            assertEquals(trackedIterators.size(), size);
            for (Iterator it : trackedIterators)
                if ((it != null) ^ retained.contains(it)) return false;
            return true;
        });
        Iterator it = q.iterator(); // trigger another sweep
        retained.add(it);
        assertEquals(trackedIterators(q), retained);
    }

    /**
     * Checks incremental sweeping of unreachable iterators.
     * Excessively white box?!
     */
    public void incrementalSweepingOfUnreachableIterators() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        final int SHORT_SWEEP_PROBES = 4;
        final int LONG_SWEEP_PROBES = 16;
        final int PROBE_HOP = LONG_SWEEP_PROBES + 6 * SHORT_SWEEP_PROBES;
        final int PROBE_HOP_COUNT = 10;
        // Expect around 8 sweeps per PROBE_HOP
        final int SWEEPS_PER_PROBE_HOP = 8;
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            q.add(i);
        for (int i = 0; i < PROBE_HOP_COUNT * PROBE_HOP; i++)
            its.add(q.iterator());
        assertEquals(attachedIterators(q), its);
        // make some garbage, separated by PROBE_HOP
        for (int i = 0; i < its.size(); i += PROBE_HOP)
            its.set(i, null);
        its.removeIf(it -> it == null);
        forceFullGc();
        int retries;
        for (retries = 0;
             trackedIterators(q).contains(null) && retries < 1000;
             retries++)
            // one round of sweeping
            its.add(q.iterator());
        assertTrue(retries >= PROBE_HOP_COUNT * (SWEEPS_PER_PROBE_HOP - 2));
        assertTrue(retries <= PROBE_HOP_COUNT * (SWEEPS_PER_PROBE_HOP + 2));
        assertEquals(trackedIterators(q), its);
    }

    public void Iterator_remove_safetyWhileInDetachedMode() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity/2; i++) {
            q.add(i);
            q.remove();
        }
        assertEquals(takeIndex(q), capacity/2);
        for (int i = 0; i < capacity; i++)
            q.add(i);
        for (int i = 0; i < capacity; i++) {
            Iterator it = q.iterator();
            its.add(it);
            for (int j = 0; j < i; j++)
                assertEquals(j, it.next());
        }
        assertEquals(attachedIterators(q), its);
        for (int i = capacity - 1; i >= 0; i--) {
            Iterator it = its.get(i);
            assertEquals(i, it.next()); // last element
            assertTrue(!isDetached(it));
            assertTrue(!it.hasNext()); // first hasNext failure
            assertTrue(isDetached(it));
            int size = q.size();
            assertTrue(q.contains(i));
            switch (rnd.nextInt(3)) {
            case 0:
                it.remove();
                assertTrue(!q.contains(i));
                assertEquals(q.size(), size - 1);
                break;
            case 1:
                // replace i with impostor
                if (q.remainingCapacity() == 0) {
                    assertTrue(q.remove(i));
                    assertTrue(q.add(-1));
                } else {
                    assertTrue(q.add(-1));
                    assertTrue(q.remove(i));
                }
                it.remove(); // should have no effect
                assertEquals(size, q.size());
                assertTrue(q.contains(-1));
                assertTrue(q.remove(-1));
                break;
            case 2:
                // replace i with true impostor
                if (i != 0) {
                    assertTrue(q.remove(i));
                    assertTrue(q.add(i));
                }
                it.remove();
                assertTrue(!q.contains(i));
                assertEquals(q.size(), size - 1);
                break;
            default: throw new AssertionError();
            }
            assertRemoveThrowsISE(it);
            assertTrue(isDetached(it));
            assertTrue(!trackedIterators(q).contains(it));
        }
        assertTrue(q.isEmpty());
        assertNull(itrs(q));
        for (Iterator it : its)
            assertIteratorExhausted(it);
    }

    /**
     * Checks dequeues bypassing iterators' current positions.
     */
    public void dequeuesBypassingIteratorCurrentPositions() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        Queue<Iterator> its0 = new ArrayDeque<>();
        Queue<Iterator> itsMid = new ArrayDeque<>();
        List<Iterator> its = new ArrayList<>();
        for (int i = 0; i < capacity; i++)
            q.add(i);
        for (int i = 0; i < 2 * capacity + 1; i++) {
            Iterator it = q.iterator();
            its.add(it);
            its0.add(it);
        }
        for (int i = 0; i < 2 * capacity + 1; i++) {
            Iterator it = q.iterator();
            for (int j = 0; j < capacity/2; j++)
                assertEquals(j, it.next());
            its.add(it);
            itsMid.add(it);
        }
        for (int i = capacity; i < 3 * capacity; i++) {
            Iterator it;

            it = its0.remove();
            assertRemoveThrowsISE(it);
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            assertEquals(0, it.next());
            int victim = i - capacity;
            for (int j = victim + (victim == 0 ? 1 : 0); j < i; j++) {
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertEquals(j, it.next());
            }
            assertIteratorExhausted(it);

            it = itsMid.remove();
            if (victim >= capacity/2)
                assertRemoveHasNoEffect(it, q);
            assertEquals(capacity/2, it.next());
            if (victim > capacity/2)
                assertRemoveHasNoEffect(it, q);
            for (int j = Math.max(victim, capacity/2 + 1); j < i; j++) {
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertEquals(j, it.next());
            }
            assertIteratorExhausted(it);

            if (rnd.nextBoolean()) {
                assertEquals(victim, q.remove());
            } else {
                ArrayList list = new ArrayList(1);
                q.drainTo(list, 1);
                assertEquals(list.size(), 1);
                assertEquals(victim, list.get(0));
            }
            assertTrue(q.add(i));
        }
        // takeIndex has wrapped twice.
        Iterator it0 = its0.remove();
        Iterator itMid = itsMid.remove();
        assertTrue(isDetached(it0));
        assertTrue(isDetached(itMid));
        if (rnd.nextBoolean()) assertTrue(it0.hasNext());
        if (rnd.nextBoolean()) assertTrue(itMid.hasNext());
        assertRemoveThrowsISE(it0);
        assertRemoveHasNoEffect(itMid, q);
        if (rnd.nextBoolean()) assertEquals(0, it0.next());
        if (rnd.nextBoolean()) assertEquals(capacity/2, itMid.next());
        assertTrue(isDetached(it0));
        assertTrue(isDetached(itMid));
        assertEquals(capacity, q.size());
        assertEquals(0, q.remainingCapacity());
    }

    /**
     * Checks collective sanity of iteration, toArray() and toString().
     */
    public void collectiveSanity() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(10, 20);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        for (int i = 0; i < capacity; i++) {
            checkIterationSanity(q);
            assertEquals(capacity, q.size() + q.remainingCapacity());
            q.add(i);
        }
        for (int i = 0; i < (capacity + (capacity >> 1)); i++) {
            checkIterationSanity(q);
            assertEquals(capacity, q.size() + q.remainingCapacity());
            assertEquals(i, q.peek());
            assertEquals(i, q.poll());
            checkIterationSanity(q);
            assertEquals(capacity, q.size() + q.remainingCapacity());
            q.add(capacity + i);
        }
        for (int i = 0; i < capacity; i++) {
            checkIterationSanity(q);
            assertEquals(capacity, q.size() + q.remainingCapacity());
            int expected = i + capacity + (capacity >> 1);
            assertEquals(expected, q.peek());
            assertEquals(expected, q.poll());
        }
        checkIterationSanity(q);
    }

    public void iteratorsDetachedWhenExhaustedAndLastRetRemoved() {
        boolean fair = rnd.nextBoolean();
        int capacity = rnd.nextInt(2, 10);
        ArrayBlockingQueue q = new ArrayBlockingQueue(capacity, fair);
        randomizePutIndex(q);
        int size = rnd.nextInt(1, capacity + 1);
        for (int i = 0; i < size; i++) q.add(i);
        Iterator it = q.iterator();
        for (int i = 0; i < size - 1; i++) assertEquals(i, it.next());
        assertEquals(trackedIterators(q), Collections.singletonList(it));
        assertFalse(isDetached(it));
        switch (rnd.nextInt(2)) {
        case 0: assertTrue(q.remove(size - 1)); break;
        case 1: assertTrue(q.removeIf(e -> e.equals(size - 1))); break;
        default: throw new AssertionError();
        }
        assertEquals(size - 1, it.next()); // should trigger detach
        assertNull(itrs(q));
        assertTrue(isDetached(it));
        assertRemoveHasNoEffect(it, q);
    }
}
