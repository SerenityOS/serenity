/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6301085 6192552 6365601
 * @summary Basic tests for asLifoQueue
 * @author  Martin Buchholz
 */

import java.util.*;
import java.util.concurrent.*;

public class AsLifoQueue {

    private static void realMain(String[] args) throws Throwable {
        try {
            Deque<String> deq = new ArrayDeque<String>();
            check(deq.addAll(Arrays.asList("b", "a", "c")));
            equal(deq.toString(), "[b, a, c]");
            check(deq.add("d"));
            equal(deq.toString(), "[b, a, c, d]");
            Queue<String> q = Collections.asLifoQueue(deq);
            check(q.add("e"));
            equal(deq.toString(),"[e, b, a, c, d]");
        } catch (Throwable t) { unexpected(t); }

        // Inspired by an excellent bug report by Jason Mehrens
        try {
            final Queue<String> q =
                Collections.asLifoQueue(new LinkedBlockingDeque<String>(3));
            check(q.isEmpty()); equal(q.size(), 0);
            check(q.add("a")); check(! q.isEmpty()); equal(q.size(), 1);
            check(q.offer("b"));
            check(q.add("c"));
            equal(q.size(), 3);
            check(! q.offer("d"));
            equal(q.size(), 3);
            THROWS(IllegalStateException.class, () -> q.add("d"));
            equal(q.size(), 3);
            equal(q.toString(), "[c, b, a]");
            equal(q.peek(), "c");
            equal(q.element(), "c");
            equal(q.remove(), "c");
            equal(q.poll(), "b");
            equal(q.peek(), "a");
            equal(q.remove(), "a");
            THROWS(NoSuchElementException.class, () -> q.remove());
            equal(q.poll(), null);
            check(q.isEmpty());
            equal(q.size(), 0);
        } catch (Throwable t) { unexpected(t); }

        THROWS(NullPointerException.class, () -> Collections.asLifoQueue(null));
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
    private static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}
