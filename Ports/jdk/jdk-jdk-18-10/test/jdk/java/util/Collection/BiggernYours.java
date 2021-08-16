/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6415641 6377302
 * @summary Concurrent collections are permitted to lie about their size
 * @author Martin Buchholz
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressWarnings("unchecked")
public class BiggernYours {
    static final Random rnd = new Random(18675309);

    static void compareCollections(Collection c1, Collection c2) {
        Object[] c1Array = c1.toArray();
        Object[] c2Array = c2.toArray();

        check(c1Array.length == c2Array.length);
        for (Object aC1 : c1Array) {
            boolean found = false;
            for (Object aC2 : c2Array) {
                if (Objects.equals(aC1, aC2)) {
                    found = true;
                    break;
                }
            }

            if (!found)
                fail(aC1 + " not found in " + Arrays.toString(c2Array));
        }
    }

    static void compareMaps(Map m1, Map m2) {
        compareCollections(m1.keySet(),
                           m2.keySet());
        compareCollections(m1.values(),
                           m2.values());
        compareCollections(m1.entrySet(),
                           m2.entrySet());
    }

    static void compareNavigableMaps(NavigableMap m1, NavigableMap m2) {
        compareMaps(m1, m2);
        compareMaps(m1.descendingMap(),
                    m2.descendingMap());
        compareMaps(m1.tailMap(Integer.MIN_VALUE),
                    m2.tailMap(Integer.MIN_VALUE));
        compareMaps(m1.headMap(Integer.MAX_VALUE),
                    m2.headMap(Integer.MAX_VALUE));
    }

    static void compareNavigableSets(NavigableSet s1, NavigableSet s2) {
        compareCollections(s1, s2);
        compareCollections(s1.descendingSet(),
                           s2.descendingSet());
        compareCollections(s1.tailSet(Integer.MIN_VALUE),
                           s2.tailSet(Integer.MIN_VALUE));
    }

    abstract static class MapFrobber { abstract void frob(Map m); }
    abstract static class SetFrobber { abstract void frob(Set s); }
    abstract static class ColFrobber { abstract void frob(Collection c); }

    static ColFrobber adder(final int i) {
        return new ColFrobber() {void frob(Collection c) { c.add(i); }};
    }

    static final ColFrobber[] adders =
    { adder(1), adder(3), adder(2) };

    static MapFrobber putter(final int k, final int v) {
        return new MapFrobber() {void frob(Map m) { m.put(k,v); }};
    }

    static final MapFrobber[] putters =
    { putter(1, -2), putter(3, -6), putter(2, -4) };

    static void unexpected(Throwable t, Object suspect) {
        System.out.println(suspect.getClass());
        unexpected(t);
    }

    static void testCollections(Collection c1, Collection c2) {
        try {
            compareCollections(c1, c2);
            for (ColFrobber adder : adders) {
                for (Collection c : new Collection[]{c1, c2})
                    adder.frob(c);
                compareCollections(c1, c2);
            }
        } catch (Throwable t) { unexpected(t, c1); }
    }

    static void testNavigableSets(NavigableSet s1, NavigableSet s2) {
        try {
            compareNavigableSets(s1, s2);
            for (ColFrobber adder : adders) {
                for (Set s : new Set[]{s1, s2})
                    adder.frob(s);
                compareNavigableSets(s1, s2);
            }
        } catch (Throwable t) { unexpected(t, s1); }
    }

    static void testMaps(Map m1, Map m2) {
        try {
            compareMaps(m1, m2);
            for (MapFrobber putter : putters) {
                for (Map m : new Map[]{m1, m2})
                    putter.frob(m);
                compareMaps(m1, m2);
            }
        } catch (Throwable t) { unexpected(t, m1); }
    }

    static void testNavigableMaps(NavigableMap m1, NavigableMap m2) {
        try {
            compareNavigableMaps(m1, m2);
            for (MapFrobber putter : putters) {
                for (Map m : new Map[]{m1, m2})
                    putter.frob(m);
                compareNavigableMaps(m1, m2);
            }
        } catch (Throwable t) { unexpected(t, m1); }
    }

    static int randomize(int size) { return rnd.nextInt(size + 2); }

    @SuppressWarnings("serial")
    private static void realMain(String[] args) {
        testNavigableMaps(
            new ConcurrentSkipListMap(),
            new ConcurrentSkipListMap() {
                public int size() {return randomize(super.size());}});

        testNavigableSets(
            new ConcurrentSkipListSet(),
            new ConcurrentSkipListSet() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new CopyOnWriteArraySet(),
            new CopyOnWriteArraySet() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new CopyOnWriteArrayList(),
            new CopyOnWriteArrayList() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new TreeSet(),
            new TreeSet() {
                public int size() {return randomize(super.size());}});

        testMaps(
            new ConcurrentHashMap(),
            new ConcurrentHashMap() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new ConcurrentLinkedDeque(),
            new ConcurrentLinkedDeque() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new ConcurrentLinkedQueue(),
            new ConcurrentLinkedQueue() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new LinkedTransferQueue(),
            new LinkedTransferQueue() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new LinkedBlockingQueue(),
            new LinkedBlockingQueue() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new LinkedBlockingDeque(),
            new LinkedBlockingDeque() {
                public int size() {return randomize(super.size());}});

        testCollections(
            new ArrayBlockingQueue(5),
            new ArrayBlockingQueue(5) {
                public int size() {return randomize(super.size());}});

        testCollections(
            new PriorityBlockingQueue(5),
            new PriorityBlockingQueue(5) {
                public int size() {return randomize(super.size());}});
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    static void arrayEqual(Object[] x, Object[] y) {
        if (x == null ? y == null : Arrays.equals(x, y)) pass();
        else fail(Arrays.toString(x) + " not equal to " + Arrays.toString(y));}
    public static void main(String[] args) {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    private abstract static class CheckedThread extends Thread {
        abstract void realRun() throws Throwable;
        public void run() {
            try {realRun();} catch (Throwable t) {unexpected(t);}}}
}
