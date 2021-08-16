/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @run testng IteratorDefaults
 * @summary test extension methods on Iterator
 */
@Test
public class IteratorDefaults {

    private static interface Callback {
        void call(List<Integer> list);
    }

    // call the callback for each recursive subList
    private void trimmedSubList(final List<Integer> list, final Callback callback) {
        int size = list.size();
        if (size > 1) {
            // trim 1 element from both ends
            final List<Integer> subList = list.subList(1, size - 1);
            callback.call(subList);
            trimmedSubList(subList, callback);
        }
    }

    public void testRemoveUnsupported() {
        final Iterator iterator = new Iterator() {
            @Override
            public boolean hasNext() {
                return false;
            }
            @Override
            public Object next() {
                return null;
            }
        };

        try {
            iterator.remove();
            fail("expected UnsupportedOperationException from remove not thrown");
        } catch (UnsupportedOperationException ignore) {
        }
    }

    public void testRemoveOverride() {
        final IteratorWithRemove iterator = new IteratorWithRemove();
        iterator.remove();
        assertTrue(iterator.removed);
    }

    public void testForEach() throws Exception {
        final Integer[] data = new Integer[1000];
        for (int i=0; i < data.length; i++) {
            data[i] = i;
        }
        final List<Integer> source = Arrays.asList(data);

        final String[] iterableCollectionClasses = {
                "java.util.ArrayDeque",
                "java.util.ArrayList",
                "java.util.HashSet",
                "java.util.LinkedHashSet",
                "java.util.LinkedList",
                "java.util.PriorityQueue",
                "java.util.TreeSet",
                "java.util.Vector",
                "java.util.concurrent.ConcurrentLinkedDeque",
                "java.util.concurrent.ConcurrentLinkedQueue",
                "java.util.concurrent.ConcurrentSkipListSet",
                "java.util.concurrent.CopyOnWriteArrayList",
                "java.util.concurrent.CopyOnWriteArraySet",
                "java.util.concurrent.LinkedBlockingDeque",
                "java.util.concurrent.LinkedBlockingQueue",
                "java.util.concurrent.LinkedTransferQueue",
                "java.util.concurrent.PriorityBlockingQueue"
        };

        for (final String iterableClass : iterableCollectionClasses) {
            final Iterable<Integer> iterable =
                    (Iterable<Integer>) Class.forName(iterableClass).newInstance();
            ((Collection<Integer>) iterable).addAll(source);
            final Iterator<Integer> iterator = iterable.iterator();
            final List<Integer> target = new ArrayList<>(source.size());
            iterator.forEachRemaining(target::add);
            if ("java.util.HashSet".equals(iterableClass)) {
                target.sort((x, y) -> x - y);
                assertEquals(target, source);
            } else {
                assertEquals(target, source);
            }

            // verify that for an iterator that has been advanced via next(),
            // forEach starts from the current location, not zero
            final int OFFSET = 5;
            final List<Integer> reference2 = new ArrayList<>(source).subList(OFFSET, source.size());
            final List<Integer> removed2 = new ArrayList<>(OFFSET);
            final Iterator<Integer> iterator2 = iterable.iterator();
            for (int i=0; i < OFFSET; i++) {
                // advance the iterator by OFFSET, saving iterated elements
                removed2.add(iterator2.next());
            }
            final List<Integer> target2 = new ArrayList<>(reference2.size());
            iterator2.forEachRemaining(target2::add);
            if ("java.util.HashSet".equals(iterableClass)) {
                assertEquals(target2.size(), reference2.size());
                target2.addAll(removed2);
                target2.sort((x, y) -> x - y);
                assertEquals(target2, source);
                assertEquals(target2.subList(OFFSET, source.size()), reference2);
            } else {
                assertEquals(target2, reference2);
            }
        }
    }

    public void testForEachSubList() throws Exception {
        final Integer[] data = new Integer[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        final List<Integer> source = Arrays.asList(data);
        final String[] listClasses = {
                "java.util.ArrayList",
                "java.util.LinkedList",
                "java.util.Vector",
                "java.util.concurrent.CopyOnWriteArrayList"
        };
        for (final String listClass : listClasses) {
            final List<Integer> list =
                    (List<Integer>) Class.forName(listClass).newInstance();
            list.addAll(source);
            trimmedSubList(list, new Callback() {
                @Override
                public void call(final List<Integer> list) {
                    if (list.size() < 1) {
                        return;
                    }
                    final List<Integer> target = new ArrayList<>(list.size());
                    final ListIterator<Integer> iterator = list.listIterator();
                    assertTrue(iterator.hasNext());
                    assertFalse(iterator.hasPrevious());
                    assertEquals(iterator.nextIndex(), 0);
                    assertEquals(iterator.previousIndex(), -1);

                    iterator.forEachRemaining(target::add);
                    assertEquals(target, list);

                    assertFalse(iterator.hasNext());
                    assertTrue(iterator.hasPrevious());
                    assertEquals(iterator.nextIndex(), list.size());
                    assertEquals(iterator.previousIndex(), list.size() - 1);

                    try {
                        iterator.next();
                        fail(listClass + " iterator advanced beyond end");
                    } catch (NoSuchElementException ignore) {
                    }
                }
            });
        }
    }

    public void testOptimizedForEach() throws Exception {
        final Integer[] data = new Integer[1000 * 1000];
        for (int i=0; i < data.length; i++) {
            data[i] = i;
        }
        final List<Integer> source = Arrays.asList(data);

        final String[] listClasses = {
                "java.util.ArrayList",
                "java.util.LinkedList",
                "java.util.Vector",
                "java.util.concurrent.CopyOnWriteArrayList"
        };

        final int OFFSET = 3;
        final List<Integer> target = new ArrayList<>(source);
        for (final String listClass : listClasses) {
            final List<Integer> list =
                    (List<Integer>) Class.forName(listClass).newInstance();
            list.addAll(source);
            final ListIterator<Integer> iterator = list.listIterator();
            assertFalse(iterator.hasPrevious());
            for (int i=0; i < OFFSET; i++) {
                iterator.next();
            }
            assertTrue(iterator.hasNext());
            assertTrue(iterator.hasPrevious());
            assertEquals(iterator.nextIndex(), OFFSET);
            assertEquals(iterator.previousIndex(), OFFSET - 1);

            iterator.forEachRemaining(e -> {
                target.set(e, e + 1);
            });
            for (int i=OFFSET; i < data.length; i++) {
                assertEquals(target.get(i).intValue(), source.get(i)+1);
            }

            assertFalse(iterator.hasNext());
            assertTrue(iterator.hasPrevious());
            assertEquals(iterator.nextIndex(), data.length);
            assertEquals(iterator.previousIndex(), data.length - 1);

            // CopyOnWriteArrayList.listIterator().remove() is unsupported
            if (!"java.util.concurrent.CopyOnWriteArrayList".equals(listClass)) {
                for (int i = data.length - 1; i >= 0; i--) {
                    iterator.remove(); // must not throw
                    if (i > 0) {
                        iterator.previous();
                    }
                }
                assertTrue(list.isEmpty());
            }

            try {
                iterator.next();
                fail(listClass + " iterator advanced beyond end");
            } catch (NoSuchElementException ignore) {
            }
        }
    }

    @Test(enabled = false)
    public void compareForEachPerformance() throws Exception {
        final Integer[] data = new Integer[1000 * 100];
        for (int i=0; i < data.length; i++) {
            data[i] = i;
        }
        final List<Integer> source = Arrays.asList(data);

        final String[] iterableCollectionClasses = {
                "java.util.ArrayList", // warmup, results discarded
                "java.util.ArrayDeque",
                "java.util.ArrayList",
                "java.util.HashSet",
                "java.util.LinkedHashSet",
                "java.util.LinkedList",
                "java.util.PriorityQueue",
                "java.util.TreeSet",
                "java.util.Vector",
                "java.util.concurrent.ConcurrentLinkedDeque",
                "java.util.concurrent.ConcurrentLinkedQueue",
                "java.util.concurrent.ConcurrentSkipListSet",
                "java.util.concurrent.CopyOnWriteArrayList",
                "java.util.concurrent.CopyOnWriteArraySet",
                "java.util.concurrent.LinkedBlockingDeque",
                "java.util.concurrent.LinkedBlockingQueue",
                "java.util.concurrent.LinkedTransferQueue",
                "java.util.concurrent.PriorityBlockingQueue"
        };

        boolean warmup = true;
        final int ITERATIONS = 10;
        final Integer[] target = new Integer[source.size()];
        for (final String iterableClass : iterableCollectionClasses) {
            final Class<? extends Collection<Integer>> type =
                    (Class<? extends Collection<Integer>>) Class.forName(iterableClass);
            final Constructor<? extends Collection<Integer>> copyConstructor =
                    type.getConstructor(Collection.class);
            final Iterable<Integer> iterable = copyConstructor.newInstance(source);
            final Iterable<Integer> reference =
                    Collections.unmodifiableCollection((Collection<Integer>) iterable);

            for (int i=0; i < ITERATIONS; i++) {
                final Iterator<Integer> iterator = reference.iterator();
                final long forEachStart = System.nanoTime();
                iterator.forEachRemaining(x -> {target[x.intValue()] = x;});
                final long forEachEnd = System.nanoTime();

                final Iterator<Integer> iterator2 = reference.iterator();
                Integer x;
                final long iteratorStart = System.nanoTime();
                while (iterator2.hasNext()) {
                    x = iterator2.next();
                    target[x.intValue()] = x;
                }
                final long iteratorEnd = System.nanoTime();

                if (warmup) { continue; } // warmup, discard results
                final long forEachTime = forEachEnd - forEachStart;
                final long iteratorTime = iteratorEnd - iteratorStart;
                final long speedup = iteratorTime - forEachTime;
                System.out.print(iterableClass);
                System.out.print(" iterator: ");
                System.out.print(iteratorTime);
                System.out.print(", forEach: ");
                System.out.print(forEachTime);
                System.out.print(", speedup: ");
                System.out.print(speedup);
                System.out.print(" (");
                System.out.print((speedup * 100) / iteratorTime);
                System.out.print("%)\n");
            }
            if (warmup) { warmup = false; }
            System.out.println();
        }
    }

    @Test(enabled = false)
    public void compareSubListForEachPerformance() throws Exception {
        final Integer[] data = new Integer[1000 * 100];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        final List<Integer> source = Arrays.asList(data);

        final String[] listClasses = {
                "java.util.ArrayList", // warmup, results discarded
                "java.util.ArrayList",
                "java.util.LinkedList",
                "java.util.Vector",
                "java.util.concurrent.CopyOnWriteArrayList"
        };

        boolean warmup = true;
        final int ITERATIONS = 10;
        final Integer[] target = new Integer[source.size()];
        for (final String listClass : listClasses) {
            final Class<? extends List<Integer >> type =
                    (Class<? extends List<Integer>>) Class.forName(listClass);
            final Constructor<? extends List<Integer >> copyConstructor =
                    type.getConstructor(Collection.class);
            final List<Integer> iterable = copyConstructor.newInstance(source);
            final List<Integer> reference = Collections.unmodifiableList(iterable);

            for (int i = 0; i < ITERATIONS; i++) {
                final Iterator<Integer> iterator = reference.subList(42, reference.size() - 37).iterator();
                final long forEachStart = System.nanoTime();
                iterator.forEachRemaining(x -> {
                    target[x.intValue()] = x;
                });
                final long forEachEnd = System.nanoTime();

                final Iterator<Integer> iterator2 = reference.iterator();
                Integer x;
                final long iteratorStart = System.nanoTime();
                while (iterator2.hasNext()) {
                    x = iterator2.next();
                    target[x.intValue()] = x;
                }
                final long iteratorEnd = System.nanoTime();

                if (warmup) { continue; } // warmup, discard results
                final long forEachTime = forEachEnd - forEachStart;
                final long iteratorTime = iteratorEnd - iteratorStart;
                final long speedup = iteratorTime - forEachTime;
                System.out.print(listClass);
                System.out.print(" iterator: ");
                System.out.print(iteratorTime);
                System.out.print(", forEach: ");
                System.out.print(forEachTime);
                System.out.print(", speedup: ");
                System.out.print(speedup);
                System.out.print(" (");
                System.out.print((speedup * 100) / iteratorTime);
                System.out.print("%)\n");
            }
            if (warmup) { warmup = false; }
            System.out.println();
        }
    }

    static class IteratorWithRemove implements Iterator {

        public boolean removed;

        IteratorWithRemove() {
            removed = false;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }

        @Override
        public void remove() {
            removed = true;
        }
    }
}
