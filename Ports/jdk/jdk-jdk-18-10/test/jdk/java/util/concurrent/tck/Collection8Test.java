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

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import junit.framework.Test;

/**
 * Contains tests applicable to all jdk8+ Collection implementations.
 * An extension of CollectionTest.
 */
public class Collection8Test extends JSR166TestCase {
    final CollectionImplementation impl;

    /** Tests are parameterized by a Collection implementation. */
    Collection8Test(CollectionImplementation impl, String methodName) {
        super(methodName);
        this.impl = impl;
    }

    public static Test testSuite(CollectionImplementation impl) {
        return parameterizedTestSuite(Collection8Test.class,
                                      CollectionImplementation.class,
                                      impl);
    }

    Object bomb() {
        return new Object() {
            @Override public boolean equals(Object x) { throw new AssertionError(); }
            @Override public int hashCode() { throw new AssertionError(); }
            @Override public String toString() { throw new AssertionError(); }
        };
    }

    /** Checks properties of empty collections. */
    public void testEmptyMeansEmpty() throws Throwable {
        Collection c = impl.emptyCollection();
        emptyMeansEmpty(c);

        if (c instanceof java.io.Serializable) {
            try {
                emptyMeansEmpty(serialClonePossiblyFailing(c));
            } catch (java.io.NotSerializableException ex) {
                // excusable when we have a serializable wrapper around
                // a non-serializable collection, as can happen with:
                // Vector.subList() => wrapped AbstractList$RandomAccessSubList
                if (testImplementationDetails
                    && (! c.getClass().getName().matches(
                                "java.util.Collections.*")))
                    throw ex;
            }
        }

        Collection clone = cloneableClone(c);
        if (clone != null)
            emptyMeansEmpty(clone);
    }

    void emptyMeansEmpty(Collection c) throws InterruptedException {
        assertTrue(c.isEmpty());
        mustEqual(0, c.size());
        mustEqual("[]", c.toString());
        if (c instanceof List<?>) {
            List x = (List) c;
            mustEqual(1, x.hashCode());
            mustEqual(x, Collections.emptyList());
            mustEqual(Collections.emptyList(), x);
            mustEqual(-1, x.indexOf(impl.makeElement(86)));
            mustEqual(-1, x.lastIndexOf(impl.makeElement(99)));
            assertThrows(
                IndexOutOfBoundsException.class,
                () -> x.get(0),
                () -> x.set(0, impl.makeElement(42)));
        }
        else if (c instanceof Set<?>) {
            mustEqual(0, c.hashCode());
            mustEqual(c, Collections.emptySet());
            mustEqual(Collections.emptySet(), c);
        }
        {
            Object[] a = c.toArray();
            mustEqual(0, a.length);
            assertSame(Object[].class, a.getClass());
        }
        {
            Object[] a = new Object[0];
            assertSame(a, c.toArray(a));
        }
        {
            Item[] a = new Item[0];
            assertSame(a, c.toArray(a));
        }
        {
            Item[] a = { one, two, three};
            assertSame(a, c.toArray(a));
            assertNull(a[0]);
            mustEqual(2, a[1]);
            mustEqual(3, a[2]);
        }
        assertIteratorExhausted(c.iterator());
        Consumer alwaysThrows = e -> { throw new AssertionError(); };
        c.forEach(alwaysThrows);
        c.iterator().forEachRemaining(alwaysThrows);
        c.spliterator().forEachRemaining(alwaysThrows);
        assertFalse(c.spliterator().tryAdvance(alwaysThrows));
        if (c.spliterator().hasCharacteristics(Spliterator.SIZED))
            mustEqual(0, c.spliterator().estimateSize());
        assertFalse(c.contains(bomb()));
        assertFalse(c.remove(bomb()));
        if (c instanceof Queue) {
            Queue q = (Queue) c;
            assertNull(q.peek());
            assertNull(q.poll());
        }
        if (c instanceof Deque) {
            Deque d = (Deque) c;
            assertNull(d.peekFirst());
            assertNull(d.peekLast());
            assertNull(d.pollFirst());
            assertNull(d.pollLast());
            assertIteratorExhausted(d.descendingIterator());
            d.descendingIterator().forEachRemaining(alwaysThrows);
            assertFalse(d.removeFirstOccurrence(bomb()));
            assertFalse(d.removeLastOccurrence(bomb()));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue q = (BlockingQueue) c;
            assertNull(q.poll(randomExpiredTimeout(), randomTimeUnit()));
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque q = (BlockingDeque) c;
            assertNull(q.pollFirst(randomExpiredTimeout(), randomTimeUnit()));
            assertNull(q.pollLast(randomExpiredTimeout(), randomTimeUnit()));
        }
    }

    public void testNullPointerExceptions() throws InterruptedException {
        Collection c = impl.emptyCollection();
        Collection nullCollection = null;
        assertThrows(
            NullPointerException.class,
            () -> c.addAll(nullCollection),
            () -> c.containsAll(nullCollection),
            () -> c.retainAll(nullCollection),
            () -> c.removeAll(nullCollection),
            () -> c.removeIf(null),
            () -> c.forEach(null),
            () -> c.iterator().forEachRemaining(null),
            () -> c.spliterator().forEachRemaining(null),
            () -> c.spliterator().tryAdvance(null),
            () -> c.toArray((Object[])null));

        if (!impl.permitsNulls()) {
            assertThrows(
                NullPointerException.class,
                () -> c.add(null));
        }
        if (!impl.permitsNulls() && c instanceof Queue) {
            Queue q = (Queue) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offer(null));
        }
        if (!impl.permitsNulls() && c instanceof Deque) {
            Deque d = (Deque) c;
            assertThrows(
                NullPointerException.class,
                () -> d.addFirst(null),
                () -> d.addLast(null),
                () -> d.offerFirst(null),
                () -> d.offerLast(null),
                () -> d.push(null),
                () -> d.descendingIterator().forEachRemaining(null));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue q = (BlockingQueue) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offer(null, 1L, HOURS),
                () -> q.put(null));
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque q = (BlockingDeque) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offerFirst(null, 1L, HOURS),
                () -> q.offerLast(null, 1L, HOURS),
                () -> q.putFirst(null),
                () -> q.putLast(null));
        }
    }

    public void testNoSuchElementExceptions() {
        Collection c = impl.emptyCollection();
        assertThrows(
            NoSuchElementException.class,
            () -> c.iterator().next());

        if (c instanceof Queue) {
            Queue q = (Queue) c;
            assertThrows(
                NoSuchElementException.class,
                () -> q.element(),
                () -> q.remove());
        }
        if (c instanceof Deque) {
            Deque d = (Deque) c;
            assertThrows(
                NoSuchElementException.class,
                () -> d.getFirst(),
                () -> d.getLast(),
                () -> d.removeFirst(),
                () -> d.removeLast(),
                () -> d.pop(),
                () -> d.descendingIterator().next());
        }
        if (c instanceof List) {
            List x = (List) c;
            assertThrows(
                NoSuchElementException.class,
                () -> x.iterator().next(),
                () -> x.listIterator().next(),
                () -> x.listIterator(0).next(),
                () -> x.listIterator().previous(),
                () -> x.listIterator(0).previous());
        }
    }

    public void testRemoveIf() {
        Collection c = impl.emptyCollection();
        boolean ordered =
            c.spliterator().hasCharacteristics(Spliterator.ORDERED);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        AtomicReference threwAt = new AtomicReference(null);
        List orig = rnd.nextBoolean()
            ? new ArrayList(c)
            : Arrays.asList(c.toArray());

        // Merely creating an iterator can change ArrayBlockingQueue behavior
        Iterator it = rnd.nextBoolean() ? c.iterator() : null;

        ArrayList survivors = new ArrayList();
        ArrayList accepts = new ArrayList();
        ArrayList rejects = new ArrayList();

        Predicate randomPredicate = e -> {
            assertNull(threwAt.get());
            switch (rnd.nextInt(3)) {
            case 0: accepts.add(e); return true;
            case 1: rejects.add(e); return false;
            case 2: threwAt.set(e); throw new ArithmeticException();
            default: throw new AssertionError();
            }
        };
        try {
            try {
                boolean modified = c.removeIf(randomPredicate);
                assertNull(threwAt.get());
                mustEqual(modified, accepts.size() > 0);
                mustEqual(modified, rejects.size() != n);
                mustEqual(accepts.size() + rejects.size(), n);
                if (ordered) {
                    mustEqual(rejects,
                                 Arrays.asList(c.toArray()));
                } else {
                    mustEqual(new HashSet(rejects),
                                 new HashSet(Arrays.asList(c.toArray())));
                }
            } catch (ArithmeticException ok) {
                assertNotNull(threwAt.get());
                assertTrue(c.contains(threwAt.get()));
            }
            if (it != null && impl.isConcurrent())
                // check for weakly consistent iterator
                while (it.hasNext()) assertTrue(orig.contains(it.next()));
            switch (rnd.nextInt(4)) {
            case 0: survivors.addAll(c); break;
            case 1: survivors.addAll(Arrays.asList(c.toArray())); break;
            case 2: c.forEach(survivors::add); break;
            case 3: for (Object e : c) survivors.add(e); break;
            }
            assertTrue(orig.containsAll(accepts));
            assertTrue(orig.containsAll(rejects));
            assertTrue(orig.containsAll(survivors));
            assertTrue(orig.containsAll(c));
            assertTrue(c.containsAll(rejects));
            assertTrue(c.containsAll(survivors));
            assertTrue(survivors.containsAll(rejects));
            if (threwAt.get() == null) {
                mustEqual(n - accepts.size(), c.size());
                for (Object x : accepts) assertFalse(c.contains(x));
            } else {
                // Two acceptable behaviors: entire removeIf call is one
                // transaction, or each element processed is one transaction.
                assertTrue(n == c.size() || n == c.size() + accepts.size());
                int k = 0;
                for (Object x : accepts) if (c.contains(x)) k++;
                assertTrue(k == accepts.size() || k == 0);
            }
        } catch (Throwable ex) {
            System.err.println(impl.klazz());
            // c is at risk of corruption if we got here, so be lenient
            try { System.err.printf("c=%s%n", c); }
            catch (Throwable t) { t.printStackTrace(); }
            System.err.printf("n=%d%n", n);
            System.err.printf("orig=%s%n", orig);
            System.err.printf("accepts=%s%n", accepts);
            System.err.printf("rejects=%s%n", rejects);
            System.err.printf("survivors=%s%n", survivors);
            System.err.printf("threwAt=%s%n", threwAt.get());
            throw ex;
        }
    }

    /**
     * All elements removed in the middle of CONCURRENT traversal.
     */
    public void testElementRemovalDuringTraversal() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        ArrayList copy = new ArrayList();
        for (int i = 0; i < n; i++) {
            Object x = impl.makeElement(i);
            copy.add(x);
            c.add(x);
        }
        ArrayList iterated = new ArrayList();
        ArrayList spliterated = new ArrayList();
        Spliterator s = c.spliterator();
        Iterator it = c.iterator();
        for (int i = rnd.nextInt(n + 1); --i >= 0; ) {
            assertTrue(s.tryAdvance(spliterated::add));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            iterated.add(it.next());
        }
        Consumer alwaysThrows = e -> { throw new AssertionError(); };
        if (s.hasCharacteristics(Spliterator.CONCURRENT)) {
            c.clear();          // TODO: many more removal methods
            if (testImplementationDetails
                && !(c instanceof java.util.concurrent.ArrayBlockingQueue)) {
                if (rnd.nextBoolean())
                    assertFalse(s.tryAdvance(alwaysThrows));
                else
                    s.forEachRemaining(alwaysThrows);
            }
            if (it.hasNext()) iterated.add(it.next());
            if (rnd.nextBoolean()) assertIteratorExhausted(it);
        }
        assertTrue(copy.containsAll(iterated));
        assertTrue(copy.containsAll(spliterated));
    }

    /**
     * Some elements randomly disappear in the middle of traversal.
     */
    public void testRandomElementRemovalDuringTraversal() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        ArrayList copy = new ArrayList();
        for (int i = 0; i < n; i++) {
            Object x = impl.makeElement(i);
            copy.add(x);
            c.add(x);
        }
        ArrayList iterated = new ArrayList();
        ArrayList spliterated = new ArrayList();
        ArrayList removed = new ArrayList();
        Spliterator s = c.spliterator();
        Iterator it = c.iterator();
        if (! (s.hasCharacteristics(Spliterator.CONCURRENT) ||
               s.hasCharacteristics(Spliterator.IMMUTABLE)))
            return;
        for (int i = rnd.nextInt(n + 1); --i >= 0; ) {
            assertTrue(s.tryAdvance(e -> {}));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            it.next();
        }
        // TODO: many more removal methods
        if (rnd.nextBoolean()) {
            for (Iterator z = c.iterator(); z.hasNext(); ) {
                Object e = z.next();
                if (rnd.nextBoolean()) {
                    try {
                        z.remove();
                    } catch (UnsupportedOperationException ok) { return; }
                    removed.add(e);
                }
            }
        } else {
            Predicate randomlyRemove = e -> {
                if (rnd.nextBoolean()) { removed.add(e); return true; }
                else return false;
            };
            c.removeIf(randomlyRemove);
        }
        s.forEachRemaining(spliterated::add);
        while (it.hasNext())
            iterated.add(it.next());
        assertTrue(copy.containsAll(iterated));
        assertTrue(copy.containsAll(spliterated));
        assertTrue(copy.containsAll(removed));
        if (s.hasCharacteristics(Spliterator.CONCURRENT)) {
            ArrayList iteratedAndRemoved = new ArrayList(iterated);
            ArrayList spliteratedAndRemoved = new ArrayList(spliterated);
            iteratedAndRemoved.retainAll(removed);
            spliteratedAndRemoved.retainAll(removed);
            assertTrue(iteratedAndRemoved.size() <= 1);
            assertTrue(spliteratedAndRemoved.size() <= 1);
            if (testImplementationDetails
                && !(c instanceof java.util.concurrent.ArrayBlockingQueue))
                assertTrue(spliteratedAndRemoved.isEmpty());
        }
    }

    /**
     * Various ways of traversing a collection yield same elements
     */
    public void testTraversalEquivalence() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        ArrayList iterated = new ArrayList();
        ArrayList iteratedForEachRemaining = new ArrayList();
        ArrayList tryAdvanced = new ArrayList();
        ArrayList spliterated = new ArrayList();
        ArrayList splitonced = new ArrayList();
        ArrayList forEached = new ArrayList();
        ArrayList streamForEached = new ArrayList();
        ConcurrentLinkedQueue parallelStreamForEached = new ConcurrentLinkedQueue();
        ArrayList removeIfed = new ArrayList();
        for (Object x : c) iterated.add(x);
        c.iterator().forEachRemaining(iteratedForEachRemaining::add);
        for (Spliterator s = c.spliterator();
             s.tryAdvance(tryAdvanced::add); ) {}
        c.spliterator().forEachRemaining(spliterated::add);
        {                       // trySplit returns "strict prefix"
            Spliterator s1 = c.spliterator(), s2 = s1.trySplit();
            if (s2 != null) s2.forEachRemaining(splitonced::add);
            s1.forEachRemaining(splitonced::add);
        }
        c.forEach(forEached::add);
        c.stream().forEach(streamForEached::add);
        c.parallelStream().forEach(parallelStreamForEached::add);
        c.removeIf(e -> { removeIfed.add(e); return false; });
        boolean ordered =
            c.spliterator().hasCharacteristics(Spliterator.ORDERED);
        if (c instanceof List || c instanceof Deque)
            assertTrue(ordered);
        HashSet cset = new HashSet(c);
        mustEqual(cset, new HashSet(parallelStreamForEached));
        if (ordered) {
            mustEqual(iterated, iteratedForEachRemaining);
            mustEqual(iterated, tryAdvanced);
            mustEqual(iterated, spliterated);
            mustEqual(iterated, splitonced);
            mustEqual(iterated, forEached);
            mustEqual(iterated, streamForEached);
            mustEqual(iterated, removeIfed);
        } else {
            mustEqual(cset, new HashSet(iterated));
            mustEqual(cset, new HashSet(iteratedForEachRemaining));
            mustEqual(cset, new HashSet(tryAdvanced));
            mustEqual(cset, new HashSet(spliterated));
            mustEqual(cset, new HashSet(splitonced));
            mustEqual(cset, new HashSet(forEached));
            mustEqual(cset, new HashSet(streamForEached));
            mustEqual(cset, new HashSet(removeIfed));
        }
        if (c instanceof Deque) {
            Deque d = (Deque) c;
            ArrayList descending = new ArrayList();
            ArrayList descendingForEachRemaining = new ArrayList();
            for (Iterator it = d.descendingIterator(); it.hasNext(); )
                descending.add(it.next());
            d.descendingIterator().forEachRemaining(
                e -> descendingForEachRemaining.add(e));
            Collections.reverse(descending);
            Collections.reverse(descendingForEachRemaining);
            mustEqual(iterated, descending);
            mustEqual(iterated, descendingForEachRemaining);
        }
    }

    /**
     * Iterator.forEachRemaining has same behavior as Iterator's
     * default implementation.
     */
    public void testForEachRemainingConsistentWithDefaultImplementation() {
        Collection c = impl.emptyCollection();
        if (!testImplementationDetails
            || c.getClass() == java.util.LinkedList.class)
            return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = 1 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        ArrayList iterated = new ArrayList();
        ArrayList iteratedForEachRemaining = new ArrayList();
        Iterator it1 = c.iterator();
        Iterator it2 = c.iterator();
        assertTrue(it1.hasNext());
        assertTrue(it2.hasNext());
        c.clear();
        Object r1, r2;
        try {
            while (it1.hasNext()) iterated.add(it1.next());
            r1 = iterated;
        } catch (ConcurrentModificationException ex) {
            r1 = ConcurrentModificationException.class;
            assertFalse(impl.isConcurrent());
        }
        try {
            it2.forEachRemaining(iteratedForEachRemaining::add);
            r2 = iteratedForEachRemaining;
        } catch (ConcurrentModificationException ex) {
            r2 = ConcurrentModificationException.class;
            assertFalse(impl.isConcurrent());
        }
        mustEqual(r1, r2);
    }

    /**
     * Calling Iterator#remove() after Iterator#forEachRemaining
     * should (maybe) remove last element
     */
    public void testRemoveAfterForEachRemaining() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        ArrayList copy = new ArrayList();
        boolean ordered = c.spliterator().hasCharacteristics(Spliterator.ORDERED);
        testCollection: {
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                Object x = impl.makeElement(i);
                c.add(x);
                copy.add(x);
            }
            Iterator it = c.iterator();
            if (ordered) {
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                mustEqual(impl.makeElement(0), it.next());
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                mustEqual(impl.makeElement(1), it.next());
            } else {
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertTrue(copy.contains(it.next()));
                if (rnd.nextBoolean()) assertTrue(it.hasNext());
                assertTrue(copy.contains(it.next()));
            }
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            it.forEachRemaining(
                e -> {
                    assertTrue(c.contains(e));
                    assertTrue(copy.contains(e));});
            if (testImplementationDetails) {
                if (c instanceof java.util.concurrent.ArrayBlockingQueue) {
                    assertIteratorExhausted(it);
                } else {
                    try { it.remove(); }
                    catch (UnsupportedOperationException ok) {
                        break testCollection;
                    }
                    mustEqual(n - 1, c.size());
                    if (ordered) {
                        for (int i = 0; i < n - 1; i++)
                            assertTrue(c.contains(impl.makeElement(i)));
                        assertFalse(c.contains(impl.makeElement(n - 1)));
                    }
                }
            }
        }
        if (c instanceof Deque) {
            Deque d = (Deque) impl.emptyCollection();
            assertTrue(ordered);
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) d.add(impl.makeElement(i));
            Iterator it = d.descendingIterator();
            assertTrue(it.hasNext());
            mustEqual(impl.makeElement(n - 1), it.next());
            assertTrue(it.hasNext());
            mustEqual(impl.makeElement(n - 2), it.next());
            it.forEachRemaining(e -> assertTrue(c.contains(e)));
            if (testImplementationDetails) {
                it.remove();
                mustEqual(n - 1, d.size());
                for (int i = 1; i < n; i++)
                    assertTrue(d.contains(impl.makeElement(i)));
                assertFalse(d.contains(impl.makeElement(0)));
            }
        }
    }

    /**
     * stream().forEach returns elements in the collection
     */
    public void testStreamForEach() throws Throwable {
        final Collection c = impl.emptyCollection();
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = o -> found.add(o);
        c.stream().forEach(spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        c.stream().forEach(spy);
        mustEqual(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        c.stream().forEach(spy);
        mustEqual(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        c.stream().forEach(spy);
        assertTrue(found.isEmpty());
    }

    public void testStreamForEachConcurrentStressTest() throws Throwable {
        if (!impl.isConcurrent()) return;
        final Collection c = impl.emptyCollection();
        final long testDurationMillis = timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object elt = impl.makeElement(1);
        final Future<?> f1, f2;
        final ExecutorService pool = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(pool, done)) {
            final CountDownLatch threadsStarted = new CountDownLatch(2);
            Runnable checkElt = () -> {
                threadsStarted.countDown();
                while (!done.get())
                    c.stream().forEach(x -> assertSame(x, elt)); };
            Runnable addRemove = () -> {
                threadsStarted.countDown();
                while (!done.get()) {
                    assertTrue(c.add(elt));
                    assertTrue(c.remove(elt));
                }};
            f1 = pool.submit(checkElt);
            f2 = pool.submit(addRemove);
            Thread.sleep(testDurationMillis);
        }
        assertNull(f1.get(0L, MILLISECONDS));
        assertNull(f2.get(0L, MILLISECONDS));
    }

    /**
     * collection.forEach returns elements in the collection
     */
    public void testForEach() throws Throwable {
        final Collection c = impl.emptyCollection();
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = o -> found.add(o);
        c.forEach(spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        c.forEach(spy);
        mustEqual(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        c.forEach(spy);
        mustEqual(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        c.forEach(spy);
        assertTrue(found.isEmpty());
    }

    /** TODO: promote to a common utility */
    static <T> T chooseOne(T ... ts) {
        return ts[ThreadLocalRandom.current().nextInt(ts.length)];
    }

    /** TODO: more random adders and removers */
    static <E> Runnable adderRemover(Collection<E> c, E e) {
        return chooseOne(
            () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                assertTrue(c.remove(e));
                assertFalse(c.contains(e));
            },
            () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                assertTrue(c.removeIf(x -> x == e));
                assertFalse(c.contains(e));
            },
            () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                for (Iterator it = c.iterator();; )
                    if (it.next() == e) {
                        try { it.remove(); }
                        catch (UnsupportedOperationException ok) {
                            c.remove(e);
                        }
                        break;
                    }
                assertFalse(c.contains(e));
            });
    }

    /**
     * Concurrent Spliterators, once exhausted, stay exhausted.
     */
    public void testStickySpliteratorExhaustion() throws Throwable {
        if (!impl.isConcurrent()) return;
        if (!testImplementationDetails) return;
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final Consumer alwaysThrows = e -> { throw new AssertionError(); };
        final Collection c = impl.emptyCollection();
        final Spliterator s = c.spliterator();
        if (rnd.nextBoolean()) {
            assertFalse(s.tryAdvance(alwaysThrows));
        } else {
            s.forEachRemaining(alwaysThrows);
        }
        final Object one = impl.makeElement(1);
        // Spliterator should not notice added element
        c.add(one);
        if (rnd.nextBoolean()) {
            assertFalse(s.tryAdvance(alwaysThrows));
        } else {
            s.forEachRemaining(alwaysThrows);
        }
    }

    /**
     * Motley crew of threads concurrently randomly hammer the collection.
     */
    public void testDetectRaces() throws Throwable {
        if (!impl.isConcurrent()) return;
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final Collection c = impl.emptyCollection();
        final long testDurationMillis
            = expensiveTests ? LONG_DELAY_MS : timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object one = impl.makeElement(1);
        final Object two = impl.makeElement(2);
        final Consumer checkSanity = x -> assertTrue(x == one || x == two);
        final Consumer<Object[]> checkArraySanity = array -> {
            // assertTrue(array.length <= 2); // duplicates are permitted
            for (Object x : array) assertTrue(x == one || x == two);
        };
        final Object[] emptyArray =
            (Object[]) java.lang.reflect.Array.newInstance(one.getClass(), 0);
        final List<Future<?>> futures;
        final Phaser threadsStarted = new Phaser(1); // register this thread
        final Runnable[] frobbers = {
            () -> c.forEach(checkSanity),
            () -> c.stream().forEach(checkSanity),
            () -> c.parallelStream().forEach(checkSanity),
            () -> c.spliterator().trySplit(),
            () -> {
                Spliterator s = c.spliterator();
                s.tryAdvance(checkSanity);
                s.trySplit();
            },
            () -> {
                Spliterator s = c.spliterator();
                do {} while (s.tryAdvance(checkSanity));
            },
            () -> { for (Object x : c) checkSanity.accept(x); },
            () -> checkArraySanity.accept(c.toArray()),
            () -> checkArraySanity.accept(c.toArray(emptyArray)),
            () -> {
                Object[] a = new Object[5];
                Object three = impl.makeElement(3);
                Arrays.fill(a, 0, a.length, three);
                Object[] x = c.toArray(a);
                if (x == a)
                    for (int i = 0; i < a.length && a[i] != null; i++)
                        checkSanity.accept(a[i]);
                    // A careful reading of the spec does not support:
                    // for (i++; i < a.length; i++) assertSame(three, a[i]);
                else
                    checkArraySanity.accept(x);
                },
            adderRemover(c, one),
            adderRemover(c, two),
        };
        final List<Runnable> tasks =
            Arrays.stream(frobbers)
            .filter(task -> rnd.nextBoolean()) // random subset
            .map(task -> (Runnable) () -> {
                     threadsStarted.arriveAndAwaitAdvance();
                     while (!done.get())
                         task.run();
                 })
            .collect(Collectors.toList());
        final ExecutorService pool = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(pool, done)) {
            threadsStarted.bulkRegister(tasks.size());
            futures = tasks.stream()
                .map(pool::submit)
                .collect(Collectors.toList());
            threadsStarted.arriveAndDeregister();
            Thread.sleep(testDurationMillis);
        }
        for (Future future : futures)
            assertNull(future.get(0L, MILLISECONDS));
    }

    /**
     * Spliterators are either IMMUTABLE or truly late-binding or, if
     * concurrent, use the same "late-binding style" of returning
     * elements added between creation and first use.
     */
    public void testLateBindingStyle() {
        if (!testImplementationDetails) return;
        if (impl.klazz() == ArrayList.class) return; // for jdk8
        // Immutable (snapshot) spliterators are exempt
        if (impl.emptyCollection().spliterator()
            .hasCharacteristics(Spliterator.IMMUTABLE))
            return;
        final Object one = impl.makeElement(1);
        {
            final Collection c = impl.emptyCollection();
            final Spliterator split = c.spliterator();
            c.add(one);
            assertTrue(split.tryAdvance(e -> { assertSame(e, one); }));
            assertFalse(split.tryAdvance(e -> { throw new AssertionError(); }));
            assertTrue(c.contains(one));
        }
        {
            final AtomicLong count = new AtomicLong(0);
            final Collection c = impl.emptyCollection();
            final Spliterator split = c.spliterator();
            c.add(one);
            split.forEachRemaining(
                e -> { assertSame(e, one); count.getAndIncrement(); });
            mustEqual(1L, count.get());
            assertFalse(split.tryAdvance(e -> { throw new AssertionError(); }));
            assertTrue(c.contains(one));
        }
    }

    /**
     * Spliterator.getComparator throws IllegalStateException iff the
     * spliterator does not report SORTED.
     */
    public void testGetComparator_IllegalStateException() {
        Collection c = impl.emptyCollection();
        Spliterator s = c.spliterator();
        boolean reportsSorted = s.hasCharacteristics(Spliterator.SORTED);
        try {
            s.getComparator();
            assertTrue(reportsSorted);
        } catch (IllegalStateException ex) {
            assertFalse(reportsSorted);
        }
    }

    public void testCollectionCopies() throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Collection c = impl.emptyCollection();
        for (int n = rnd.nextInt(4); n--> 0; )
            c.add(impl.makeElement(rnd.nextInt()));
        mustEqual(c, c);
        if (c instanceof List)
            assertCollectionsEquals(c, new ArrayList(c));
        else if (c instanceof Set)
            assertCollectionsEquals(c, new HashSet(c));
        else if (c instanceof Deque)
            assertCollectionsEquivalent(c, new ArrayDeque(c));

        Collection clone = cloneableClone(c);
        if (clone != null) {
            assertSame(c.getClass(), clone.getClass());
            assertCollectionsEquivalent(c, clone);
        }
        try {
            Collection serialClone = serialClonePossiblyFailing(c);
            assertSame(c.getClass(), serialClone.getClass());
            assertCollectionsEquivalent(c, serialClone);
        } catch (java.io.NotSerializableException acceptable) {}
    }

    /**
     * TODO: move out of limbo
     * 8203662: remove increment of modCount from ArrayList and Vector replaceAll()
     */
    public void DISABLED_testReplaceAllIsNotStructuralModification() {
        Collection c = impl.emptyCollection();
        if (!(c instanceof List))
            return;
        List list = (List) c;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int n = rnd.nextInt(2, 10); n--> 0; )
            list.add(impl.makeElement(rnd.nextInt()));
        ArrayList copy = new ArrayList(list);
        int size = list.size(), half = size / 2;
        Iterator it = list.iterator();
        for (int i = 0; i < half; i++)
            mustEqual(it.next(), copy.get(i));
        list.replaceAll(n -> n);
        // ConcurrentModificationException must not be thrown here.
        for (int i = half; i < size; i++)
            mustEqual(it.next(), copy.get(i));
    }

//     public void testCollection8DebugFail() {
//         fail(impl.klazz().getSimpleName());
//     }
}
