/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6612102
 * @summary Test Map implementations for mutual compatibility
 * @key randomness
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Based on the strange scenario required to reproduce
 * (coll) IdentityHashMap.iterator().remove() might decrement size twice
 *
 * It would be good to add more "Lockstep-style" tests to this file.
 */
public class LockStep {
    void mapsEqual(Map m1, Map m2) {
        equal(m1, m2);
        equal(m2, m1);
        equal(m1.size(), m2.size());
        equal(m1.isEmpty(), m2.isEmpty());
        equal(m1.keySet(), m2.keySet());
        equal(m2.keySet(), m1.keySet());
    }

    void mapsEqual(List<Map> maps) {
        Map first = maps.get(0);
        for (Map map : maps)
            mapsEqual(first, map);
    }

    void put(List<Map> maps, Object key, Object val) {
        for (Map map : maps)
            map.put(key, val);
        mapsEqual(maps);
    }

    void removeLastTwo(List<Map> maps) {
        Map first = maps.get(0);
        int size = first.size();
        Iterator fit = first.keySet().iterator();
        for (int j = 0; j < size - 2; j++)
            fit.next();
        Object x1 = fit.next();
        Object x2 = fit.next();

        for (Map map : maps) {
            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                Object x = it.next();
                if (x == x1 || x == x2)
                    it.remove();
            }
        }
        mapsEqual(maps);
    }

    void remove(Map m, Iterator it) {
        int size = m.size();
        it.remove();
        if (m.size() != size-1)
            throw new Error(String.format("Incorrect size!%nmap=%s, size=%d%n",
                                          m.toString(), m.size()));
    }

    void test(String[] args) throws Throwable {
        final int iterations = 100;
        final Random r = new Random();

        for (int i = 0; i < iterations; i++) {
            List<Map> maps = List.of(
                new IdentityHashMap(11),
                new HashMap(16),
                new LinkedHashMap(16),
                new WeakHashMap(16),
                new Hashtable(16),
                new TreeMap(),
                new ConcurrentHashMap(16),
                new ConcurrentSkipListMap(),
                Collections.checkedMap(new HashMap(16), Integer.class, Integer.class),
                Collections.checkedSortedMap(new TreeMap(), Integer.class, Integer.class),
                Collections.checkedNavigableMap(new TreeMap(), Integer.class, Integer.class),
                Collections.synchronizedMap(new HashMap(16)),
                Collections.synchronizedSortedMap(new TreeMap()),
                Collections.synchronizedNavigableMap(new TreeMap()));

            for (int j = 0; j < 10; j++)
                put(maps, r.nextInt(100), r.nextInt(100));
            removeLastTwo(maps);
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
        new LockStep().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
