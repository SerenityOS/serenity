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
 * @bug 6394004
 * @summary Test ForgetMeNot implementation feature (and more)
 * @author Martin Buchholz
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

public class ForgetMeNot {
    private static void checkQ(PriorityQueue<Integer> q, Integer...elts) {
        check(Arrays.equals(q.toArray(), elts));
    }

    private static void noMoreElements(final Iterator<Integer> it) {
        for (int j = 0; j < 2; j++) {
            THROWS(NoSuchElementException.class, () -> it.next());
            check(! it.hasNext());
        }
    }

    private static void removeIsCurrentlyIllegal(final Iterator<Integer> it) {
        for (int j = 0; j < 2; j++) {
            THROWS(IllegalStateException.class, () -> it.remove());
        }
    }

    private static void remove(Iterator<Integer> it,
                               Queue<Integer> q) {
        int size = q.size();
        it.remove();
        removeIsCurrentlyIllegal(it);
        equal(size, q.size()+1);
    }

    private static void realMain(String[] args) throws Throwable {
        final PriorityQueue<Integer> q = new PriorityQueue<>();
        Iterator<Integer> it;

        //----------------------------------------------------------------
        // Empty
        //----------------------------------------------------------------
        checkQ(q);
        check(q.isEmpty());
        check(! q.contains(1));
        it = q.iterator();
        removeIsCurrentlyIllegal(it);
        noMoreElements(it);
        q.clear();
        check(q.isEmpty());

        //----------------------------------------------------------------
        // Singleton
        //----------------------------------------------------------------
        q.add(1);
        checkQ(q, 1);
        check(! q.isEmpty());
        check(q.contains(1));
        it = q.iterator();
        removeIsCurrentlyIllegal(it);
        check(it.hasNext());
        equal(it.next(), 1);
        noMoreElements(it);
        remove(it, q);
        check(q.isEmpty());
        noMoreElements(it);
        checkQ(q);
        q.clear();

        //----------------------------------------------------------------
        // @see PriorityQueue.forgetMeNot
        //----------------------------------------------------------------
        final Integer[] a = {0, 4, 1, 6, 7, 2, 3}; // Carefully chosen!
        q.addAll(Arrays.asList(a));
        checkQ(q, a);
        it = q.iterator();
        checkQ(q, a);
        removeIsCurrentlyIllegal(it);
        checkQ(q, a);
        check(it.hasNext());
        removeIsCurrentlyIllegal(it);
        checkQ(q, a);
        check(it.hasNext());
        equal(it.next(), 0);
        equal(it.next(), 4);
        equal(it.next(), 1);
        equal(it.next(), 6);
        check(it.hasNext());
        checkQ(q, a);
        remove(it, q);
        checkQ(q, 0, 3, 1, 4, 7, 2);
        check(it.hasNext());
        removeIsCurrentlyIllegal(it);
        equal(it.next(), 7);
        remove(it, q);
        checkQ(q, 0, 2, 1, 4, 3);
        check(it.hasNext());
        removeIsCurrentlyIllegal(it);
        check(it.hasNext());
        equal(it.next(), 3);
        equal(it.next(), 2);
        check(! it.hasNext());
        remove(it, q);
        checkQ(q, 0, 3, 1, 4);
        check(! it.hasNext());
        noMoreElements(it);
        removeIsCurrentlyIllegal(it);
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
    interface Fun {void f() throws Throwable;}
    static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}
