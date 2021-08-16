/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6324846
 * @summary Deque implementations must behave isomorphically
 * @author Martin Buchholz
 */

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ChorusLine {
    private interface Tweaker {
        void run(Deque<Integer> deq);
    }

    private static final Tweaker[] tweakers = {
        new Tweaker() { public void run(Deque<Integer> deq) {
            for (int i = 0; i < 7; i++)
                deq.addLast(i);
            deq.removeFirst();
            deq.removeFirst();
            deq.addLast(7);
            deq.addLast(8);
            Iterator<Integer> it = deq.descendingIterator();
            equal(it.next(), 8);
            it.remove();

            try {it.remove();}
            catch (IllegalStateException e) {pass();}
            catch (Throwable t) {unexpected(t);}

            deq.addLast(9);
            it = deq.descendingIterator();
            equal(it.next(), 9);
            equal(it.next(), 7);
            it.remove();

            try {it.remove();}
            catch (IllegalStateException e) {pass();}
            catch (Throwable t) {unexpected(t);}

            equal(it.next(), 6);

            System.out.println(deq);
        }},
        new Tweaker() { public void run(Deque<Integer> deq) {
            deq.clear();
            check(deq.isEmpty());
            check(deq.size() == 0);
            check(! deq.iterator().hasNext());
            check(! deq.descendingIterator().hasNext());

            try {deq.iterator().next(); fail();}
            catch (NoSuchElementException e) {pass();}
            catch (Throwable t) {unexpected(t);}

            try {deq.descendingIterator().next(); fail();}
            catch (NoSuchElementException e) {pass();}
            catch (Throwable t) {unexpected(t);}
        }},
        new Tweaker() { public void run(Deque<Integer> deq) {
            for (int i = 0; i < 11; i++)
                deq.add(i);
            Iterator<Integer> it = deq.iterator();
            equal(it.next(), 0);
            equal(it.next(), 1);
            it.remove();
            deq.addFirst(-1);
            deq.addFirst(-2);
            it = deq.iterator();
            equal(it.next(), -2);
            equal(it.next(), -1);
            equal(it.next(), 0);
            it.remove();

            it = deq.descendingIterator();

            try {it.remove(); fail();}
            catch (IllegalStateException e) {pass();}
            catch (Throwable t) {unexpected(t);}

            equal(it.next(), 10);
            it.remove();

            try {it.remove(); fail();}
            catch (IllegalStateException e) {pass();}
            catch (Throwable t) {unexpected(t);}

            equal(it.next(), 9);
            equal(it.next(), 8);
            it.remove();
            System.out.println(deq);
        }},
        new Tweaker() { public void run(Deque<Integer> deq) {
            while (deq.size() > 1) {
                Iterator<Integer> it = deq.iterator();
                it.next(); it.remove();
                it = deq.descendingIterator();
                it.next(); it.remove();
            }
            System.out.println(deq);
        }}};

    private static void realMain(String[] args) throws Throwable {
        Collection<Deque<Integer>> deqs = new ArrayDeque<>(3);
        deqs.add(new ArrayDeque<Integer>());
        deqs.add(new LinkedList<Integer>());
        deqs.add(new LinkedBlockingDeque<Integer>());
        deqs.add(new ConcurrentLinkedDeque<Integer>());

        equal(deqs);

        for (Tweaker tweaker : tweakers) {
            for (Deque<Integer> deq : deqs)
                tweaker.run(deq);
            equal(deqs);
        }
    }

    private static void equal(Iterable<Deque<Integer>> deqs) {
        Deque<Integer> prev = null;
        for (Deque<Integer> deq : deqs) {
            if (prev != null) {
                equal(prev.isEmpty(), deq.isEmpty());
                equal(prev.size(), deq.size());
                equal(prev.toString(), deq.toString());
            }
            prev = deq;
        }

        Deque<Iterator<Integer>> its = new ArrayDeque<>();
        for (Deque<Integer> deq : deqs)
            its.addLast(deq.iterator());
        equal(its);

        Deque<Iterator<Integer>> dits = new ArrayDeque<>();
        for (Deque<Integer> deq : deqs)
            dits.addLast(deq.descendingIterator());
        equal(dits);
    }

    private static void equal(Deque<Iterator<Integer>> its) {
        Iterator<Integer> it0 = its.remove();
        while (it0.hasNext()) {
            Integer i = it0.next();
            for (Iterator<Integer> it : its)
                equal(it.next(), i);
        }
        for (Iterator<Integer> it : its) {
            check(! it.hasNext());

            try {it.next(); fail();}
            catch (NoSuchElementException e) {pass();}
            catch (Throwable t) {unexpected(t);}
        }
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
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
