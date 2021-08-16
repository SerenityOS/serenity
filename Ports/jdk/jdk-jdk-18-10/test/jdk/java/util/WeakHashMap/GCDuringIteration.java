/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6499848
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main GCDuringIteration
 * @summary Check that iterators work properly in the presence of
 *          concurrent finalization and removal of elements.
 * @key randomness
 */

import jdk.test.lib.RandomFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GCDuringIteration {

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

    // A class with the traditional pessimal hashCode implementation,
    // to ensure that all instances end up in the same bucket.
    static class Foo { public int hashCode() { return 42; }}

    <K,V> void put(Map<K,V> map, K k, V v) {
        check(! map.containsKey(k));
        equal(map.get(k), null);
        equal(map.put(k, v), null);
        equal(map.get(k), v);
        check(map.containsKey(k));
        equal(map.put(k, v), v);
        equal(map.get(k), v);
        check(map.containsKey(k));
        check(! map.isEmpty());
        equal(map.keySet().iterator().next(), k);
        equal(map.values().iterator().next(), v);
    }

    static final Random rnd = RandomFactory.getRandom();

    void checkIterator(final Iterator<Map.Entry<Foo, Integer>> it, int first) {
        for (int i = first; i >= 0; --i) {
            if (rnd.nextBoolean()) check(it.hasNext());
            equal(it.next().getValue(), i);
        }
        if (rnd.nextBoolean()) {
            try {
                it.next();
                throw new AssertionError("should throw");
            } catch (NoSuchElementException success) {}
        }

        if (rnd.nextBoolean())
            check(! it.hasNext());
    }

    <K,V> V firstValue(Map<K,V> map) {
        return map.values().iterator().next();
    }

    void test(String[] args) throws Throwable {
        final int n = 10;
        // Create array of strong refs
        final Foo[] foos = new Foo[2*n];
        final Map<Foo,Integer> map = new WeakHashMap<>(foos.length);
        check(map.isEmpty());
        equal(map.size(), 0);

        for (int i = 0; i < foos.length; i++) {
            Foo foo = new Foo();
            foos[i] = foo;
            put(map, foo, i);
        }
        equal(map.size(), foos.length);

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            foos[first] = null;
            gcAwait(() -> map.size() == first);
            checkIterator(it, first-1);
            equal(map.size(), first);
            equal(firstValue(map), first-1);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.next();          // protects first entry
            System.out.println(map.values());
            int oldSize = map.size();
            foos[first] = null;
            forceFullGc();
            equal(map.size(), oldSize);
            System.out.println(map.values());
            checkIterator(it, first-1);
            // first entry no longer protected
            gcAwait(() -> map.size() == first);
            equal(firstValue(map), first-1);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.next();          // protects first entry
            System.out.println(map.values());
            foos[first] = foos[first-1] = null;
            gcAwait(() -> map.size() == first);
            equal(firstValue(map), first);
            System.out.println(map.values());
            checkIterator(it, first-2);
            // first entry no longer protected
            gcAwait(() -> map.size() == first-1);
            equal(firstValue(map), first-2);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.next();          // protects first entry
            it.hasNext();       // protects second entry
            System.out.println(map.values());
            int oldSize = map.size();
            foos[first] = foos[first-1] = null;
            forceFullGc();
            equal(map.size(), oldSize);
            equal(firstValue(map), first);
            System.out.println(map.values());
            checkIterator(it, first-1);
            // first entry no longer protected
            gcAwait(() -> map.size() == first-1);
            equal(firstValue(map), first-2);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.next();          // protects first entry
            System.out.println(map.values());
            equal(map.size(), first+1);
            foos[first] = foos[first-1] = null;
            gcAwait(() -> map.size() == first);
            it.remove();
            equal(firstValue(map), first-2);
            equal(map.size(), first-1);
            System.out.println(map.values());
            checkIterator(it, first-2);
            // first entry no longer protected
            gcAwait(() -> map.size() == first-1);
            equal(firstValue(map), first-2);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.next();          // protects first entry
            it.remove();
            it.hasNext();       // protects second entry
            System.out.println(map.values());
            equal(map.size(), first);
            foos[first] = foos[first-1] = null;
            forceFullGc();
            equal(firstValue(map), first-1);
            equal(map.size(), first);
            System.out.println(map.values());
            checkIterator(it, first-1);
            gcAwait(() -> map.size() == first-1);
            equal(firstValue(map), first-2);
        }

        {
            int first = firstValue(map);
            final Iterator<Map.Entry<Foo,Integer>> it = map.entrySet().iterator();
            it.hasNext();       // protects first entry
            Arrays.fill(foos, null);
            gcAwait(() -> map.size() == 1);
            System.out.println(map.values());
            equal(it.next().getValue(), first);
            check(! it.hasNext());
            gcAwait(() -> map.size() == 0);
            check(map.isEmpty());
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new GCDuringIteration().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
