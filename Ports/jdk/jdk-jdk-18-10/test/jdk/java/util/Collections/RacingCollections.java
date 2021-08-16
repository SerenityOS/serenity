/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6360946 6360948
 * @summary Test various operations on concurrently mutating collections
 * @author Martin Buchholz
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
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

import static java.util.Collections.asLifoQueue;
import static java.util.Collections.checkedList;
import static java.util.Collections.checkedMap;
import static java.util.Collections.checkedSet;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public class RacingCollections {
    /**
     * How long to run each "race" (in milliseconds).
     * Turn this up to some higher value like 1000 for stress testing:
     * java -Dmillis=1000 RacingCollections
     */
    static final long defaultWorkTimeMillis = Long.getLong("millis", 10L);

    /**
     * Whether to print debug information.
     */
    static final boolean debug = Boolean.getBoolean("debug");

    static final Integer one = 1;
    static final Integer two = 2;

    /**
     * A thread that mutates an object forever, alternating between
     * being empty and containing singleton "two"
     */
    static class Frobber extends CheckedThread {
        volatile boolean done = false;
        boolean keepGoing(int i) { return (i % 128 != 0) || ! done; }

        final Object elLoco;
        Frobber(Object elLoco) {
            this.elLoco = elLoco;
            this.start();
        }

        @SuppressWarnings("unchecked")
        void clear(Object o) {
            if (o instanceof Collection)
                ((Collection<?>)o).clear();
            else
                ((Map<?,?>)o).clear();
        }

        @SuppressWarnings("unchecked")
        void realRun() {
            // Mutate elLoco wildly forever, checking occasionally for "done"
            clear(elLoco);
            if (elLoco instanceof List) {
                List<Integer> l = (List<Integer>) elLoco;
                for (int i = 0; keepGoing(i); i++) {
                    switch (i%2) {
                    case 0: l.add(two);    break;
                    case 1: l.add(0, two); break;
                    }
                    switch (i%2) {
                    case 0: l.remove(two); break;
                    case 1: l.remove(0);   break;
                    }}}
            else if (elLoco instanceof Deque) {
                Deque<Integer> q = (Deque<Integer>) elLoco;
                for (int i = 0; keepGoing(i); i++) {
                    switch (i%6) {
                    case 0: q.add(two);        break;
                    case 1: q.addFirst(two);   break;
                    case 2: q.addLast(two);    break;
                    case 3: q.offer(two);      break;
                    case 4: q.offerFirst(two); break;
                    case 5: q.offerLast(two);  break;
                    }
                    switch (i%6) {
                    case 0: q.remove(two);     break;
                    case 1: q.removeFirst();   break;
                    case 2: q.removeLast();    break;
                    case 3: q.poll();          break;
                    case 4: q.pollFirst();     break;
                    case 5: q.pollLast();      break;
                    }}}
            else if (elLoco instanceof Queue) {
                Queue<Integer> q = (Queue<Integer>) elLoco;
                for (int i = 0; keepGoing(i); i++) {
                    switch (i%2) {
                    case 0: q.add(two);    break;
                    case 1: q.offer(two);  break;
                    }
                    switch (i%2) {
                    case 0: q.remove(two); break;
                    case 1: q.poll();      break;
                    }}}
            else if (elLoco instanceof Map) {
                Map<Integer, Boolean> m = (Map<Integer, Boolean>) elLoco;
                for (int i = 0; keepGoing(i); i++) {
                    m.put(two, true);
                    m.remove(two);
                }}
            else if (elLoco instanceof Collection) {
                Collection<Integer> c = (Collection<Integer>) elLoco;
                for (int i = 0; keepGoing(i); i++) {
                    c.add(two);
                    c.remove(two);
                }}
            else { throw new Error("Huh? " + elLoco); }
        }

        void enoughAlready() {
            done = true;
            try { join(); } catch (Throwable t) { unexpected(t); }
        }
    }

    private static void checkEqualSanity(Object theRock, Object elLoco) {
        //equal(theRock, theRock);
        equal(elLoco, elLoco);

        // It would be nice someday to have theRock and elLoco never "equal",
        // although the meaning of "equal" for mutating collections
        // is a bit fuzzy.  Uncomment when/if we fix:
        // 6374942: Improve thread safety of collection .equals() methods
        //notEqual(theRock, elLoco);
        //notEqual(elLoco, theRock);

        notEqual(theRock.toString(), elLoco.toString());
    }

    static class Looper {
        final long quittingTime;
        int i = 0;
        Looper() { this(defaultWorkTimeMillis); }
        Looper(long workTimeMillis) {
            quittingTime = System.nanoTime() + workTimeMillis * 1024 * 1024;
        }
        boolean keepGoing() {
            return (i++ % 128 != 0) || (System.nanoTime() - quittingTime < 0);
        }
    }

    private static void frob(Object theRock, Object elLoco) {
        Frobber frobber = new Frobber(elLoco);
        try {
            if (theRock instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Integer> c = (Collection<Integer>) theRock;
                if (! c.contains(one))
                    c.add(one);
            } else {
                @SuppressWarnings("unchecked")
                Map<Integer, Boolean> m = (Map<Integer, Boolean>) theRock;
                if (! m.containsKey(one))
                    m.put(one, true);
            }
            for (Looper looper = new Looper(); looper.keepGoing(); )
                checkEqualSanity(theRock, elLoco);
        }
        catch (Throwable t) { unexpected(t); }
        finally { frobber.enoughAlready(); }
    }

    private static List<Map<Integer, Boolean>> newConcurrentMaps() {
        List<Map<Integer, Boolean>> list = new ArrayList<>();
        list.add(new ConcurrentHashMap<Integer, Boolean>());
        list.add(new ConcurrentSkipListMap<Integer, Boolean>());
        return list;
    }

    private static List<Map<Integer, Boolean>> maps() {
        List<Map<Integer, Boolean>> list = newConcurrentMaps();
        list.add(new Hashtable<Integer, Boolean>());
        list.add(new HashMap<Integer, Boolean>());
        list.add(new TreeMap<Integer, Boolean>());
        Comparator<Integer> cmp = new Comparator<>() {
            public int compare(Integer x, Integer y) {
                return x - y;
            }};
        list.add(new TreeMap<Integer, Boolean>(Collections.reverseOrder(cmp)));
        return list;
    }

    private static List<Set<Integer>> newConcurrentSets() {
        List<Set<Integer>> list = new ArrayList<>();
        list.add(new ConcurrentSkipListSet<Integer>());
        list.add(new CopyOnWriteArraySet<Integer>());
        return list;
    }

    private static List<Set<Integer>> newSets() {
        List<Set<Integer>> list = newConcurrentSets();
        list.add(new HashSet<Integer>());
        list.add(new TreeSet<Integer>());
        list.add(new TreeSet<Integer>(Collections.reverseOrder()));
        return list;
    }

    private static List<List<Integer>> newConcurrentLists() {
        List<List<Integer>> list = new ArrayList<>();
        list.add(new CopyOnWriteArrayList<Integer>());
        return list;
    }

    private static List<List<Integer>> newLists() {
        List<List<Integer>> list = newConcurrentLists();
        list.add(new Vector<Integer>());
        list.add(new ArrayList<Integer>());
        return list;
    }

    private static List<Queue<Integer>> newConcurrentQueues() {
        List<Queue<Integer>> list = new ArrayList<>(newConcurrentDeques());
        list.add(new ArrayBlockingQueue<Integer>(10));
        list.add(new LinkedBlockingQueue<Integer>(10));
        list.add(new LinkedTransferQueue<Integer>());
        list.add(new ConcurrentLinkedQueue<Integer>());
        return list;
    }

    private static List<Queue<Integer>> newQueues() {
        List<Queue<Integer>> list = new ArrayList<>(newDeques());
        list.add(new LinkedBlockingQueue<Integer>(10));
        return list;
    }

    private static List<Deque<Integer>> newConcurrentDeques() {
        List<Deque<Integer>> list = new ArrayList<>();
        list.add(new LinkedBlockingDeque<Integer>(10));
        list.add(new ConcurrentLinkedDeque<Integer>());
        return list;
    }

    private static List<Deque<Integer>> newDeques() {
        List<Deque<Integer>> list = newConcurrentDeques();
        list.add(new ArrayDeque<Integer>());
        list.add(new LinkedList<Integer>());
        return list;
    }

    private static void describe(Class<?> k, Object x, Object y) {
        if (debug)
            System.out.printf("%s: %s, %s%n", k.getSimpleName(),
                              x.getClass().getSimpleName(),
                              y.getClass().getSimpleName());
    }

    private static void realMain(String[] args) {
        for (Map<Integer, Boolean> x : maps())
            for (Map<Integer, Boolean> y : newConcurrentMaps()) {
                describe(Map.class, x, y);
                x.put(one, true);
                frob(x, y);
                frob(unmodifiableMap(x), y);
                frob(synchronizedMap(x), y);
                frob(x, synchronizedMap(y));
                frob(checkedMap(x, Integer.class, Boolean.class), y);
                frob(x, checkedMap(y, Integer.class, Boolean.class));
                x.clear();
                frob(newSetFromMap(x), newSetFromMap(y));
                frob(x.keySet(), newSetFromMap(y));
            }

        for (Set<Integer> x : newSets())
            for (Set<Integer> y : newConcurrentSets()) {
                describe(Set.class, x, y);
                frob(x, y);
                frob(unmodifiableSet(x), y);
                frob(synchronizedSet(x), y);
                frob(x, synchronizedSet(y));
                frob(checkedSet(x, Integer.class), y);
                frob(x, checkedSet(y, Integer.class));
            }

        for (List<Integer> x : newLists())
            for (List<Integer> y : newConcurrentLists()) {
                describe(List.class, x, y);
                frob(x, y);
                frob(unmodifiableList(x), y);
                frob(synchronizedList(x), y);
                frob(x, synchronizedList(y));
                frob(checkedList(x, Integer.class), y);
                frob(x, checkedList(y, Integer.class));
            }

        for (Queue<Integer> x : newQueues())
            for (Queue<Integer> y : newConcurrentQueues()) {
                describe(Queue.class, x, y);
                frob(x, y);
            }

        for (Deque<Integer> x : newDeques())
            for (Deque<Integer> y : newConcurrentDeques()) {
                describe(Deque.class, x, y);
                frob(asLifoQueue(x), y);
                frob(x, asLifoQueue(y));
            }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static String toString(Object x) {
        return ((x instanceof Collection) || (x instanceof Map)) ?
            x.getClass().getName() : x.toString();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(toString(x) + " not equal to " + toString(y));}
    static void notEqual(Object x, Object y) {
        if (x == null ? y == null : x.equals(y))
            fail(toString(x) + " equal to " + toString(y));
        else pass();}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    private abstract static class CheckedThread extends Thread {
        abstract void realRun() throws Throwable;
        public void run() {
            try { realRun(); } catch (Throwable t) { unexpected(t); }}}
}
