/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8066070
 * @run testng AddNonComparable
 */

import org.testng.annotations.Test;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class AddNonComparable {

    static <E> void test(Queue<E> queue, Supplier<E> supplier,
                         BiConsumer<? super Queue<E>, Throwable> checker) {
        Throwable x = null;
        try { queue.add(supplier.get()); }
        catch (Throwable e) { x = e; }
        checker.accept(queue, x);
    }

    @Test
    public void queues() {
        test(new PriorityQueue<>(), NonComparable::new,
             (q, e) -> {
                 assertEquals(q.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new PriorityQueue<>(), AComparable::new,
             (q, e) -> {
                 assertEquals(q.size(), 1);
                 assertNull(e);
             });

        test(new PriorityBlockingQueue<>(), NonComparable::new,
             (q, e) -> {
                 assertEquals(q.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new PriorityBlockingQueue<>(), AComparable::new,
             (q, e) -> {
                 assertEquals(q.size(), 1);
                 assertNull(e);
             });
    }

    static <E> void test(SortedSet<E> set, Supplier<E> supplier,
                         BiConsumer<? super SortedSet<E>, Throwable> checker) {
        Throwable x = null;
        try { set.add(supplier.get()); }
        catch (Throwable e) { x = e; }
        checker.accept(set, x);
    }


    @Test
    public void sets() {
        test(new TreeSet<>(), NonComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new TreeSet<>(), AComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 1);
                 assertNull(e);
             });

        test(new ConcurrentSkipListSet<>(), NonComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new ConcurrentSkipListSet<>(), AComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 1);
                 assertNull(e);
             });
    }

    static <K> void test(SortedMap<K,Boolean> map, Supplier<K> supplier,
                         BiConsumer<? super SortedMap<K,Boolean>, Throwable> checker) {
        Throwable x = null;
        try { map.put(supplier.get(), Boolean.TRUE); }
        catch (Throwable e) { x = e; }
        checker.accept(map, x);
    }

    @Test
    public void maps() {
        test(new TreeMap<>(), NonComparable::new,
             (m, e) -> {
                 assertEquals(m.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new TreeMap<>(), AComparable::new,
             (m, e) -> {
                 assertEquals(m.size(), 1);
                 assertNull(e);
             });

        test(new ConcurrentSkipListMap<>(), NonComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 0);
                 assertTrue(e instanceof ClassCastException);
             });
        test(new ConcurrentSkipListMap<>(), AComparable::new,
             (s, e) -> {
                 assertEquals(s.size(), 1);
                 assertNull(e);
             });
    }

    static class NonComparable { }

    static class AComparable implements Comparable<AComparable> {
        @Override public int compareTo(AComparable v) { return 0; }
    }

}
