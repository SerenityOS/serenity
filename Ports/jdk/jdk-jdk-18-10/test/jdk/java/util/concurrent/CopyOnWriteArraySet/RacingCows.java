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
 * @bug 6330307 6355645
 * @summary Check for race conditions in COWArray classes
 * @author Martin Buchholz
 */

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class RacingCows {
    private static void realMain(String[] args) throws Throwable {
        final int iterations = 100000;
        final Integer two = Integer.valueOf(2);
        final Integer three = Integer.valueOf(3);

        //------------ CopyOnWriteArraySet -------------------------------
        final Set<Integer> s1 = new CopyOnWriteArraySet<>();
        final Set<Integer> s2 = new CopyOnWriteArraySet<>();
        s1.add(1);

        final Thread t1 = new CheckedThread() { public void realRun() {
            for (int i = 0; i < iterations; i++) {
                s2.add(two);
                s2.remove(two);
            }}};
        t1.start();

        for (int i = 0; i < iterations; i++) {
            check(! s1.equals(s2));
            check(! s2.equals(s1));
        }
        t1.join();

        //------------ CopyOnWriteArrayList ------------------------------
        final List<Integer> l1 = new CopyOnWriteArrayList<>();
        final List<Integer> l2 = new CopyOnWriteArrayList<>();
        final List<Integer> l3 = new CopyOnWriteArrayList<>();
        l1.add(1);

        final Thread t2 = new CheckedThread() { public void realRun() {
            for (int i = 0; i < iterations; i++) {
                switch (i%2) {
                case 0: l2.add(two);    break;
                case 1: l2.add(0, two); break;
                }
                switch (i%3) {
                case 0: l2.remove(two); break;
                case 1: l2.remove(0);   break;
                case 2: l2.clear();     break;
                }}}};
        t2.start();

        final Thread t3 = new CheckedThread() { public void realRun() {
            l3.add(three);
            for (int i = 0; i < iterations; i++) {
                switch (i%2) {
                case 0: l3.add(two);    break;
                case 1: l3.add(0, two); break;
                }
                switch (i%2) {
                case 0: l3.remove(two); break;
                case 1: l3.remove(0);   break;
                }}}};
        t3.start();

        for (int i = 0; i < iterations; i++) {
            check(! l1.equals(l2));
            check(! l2.equals(l1));

            // CopyOnWriteArrayList(mutatingCollection)
            try { new CopyOnWriteArrayList<Integer>(l2); }
            catch (Throwable t) { unexpected(t); }

            // addAllAbsent(mutatingCollection)
            try { new CopyOnWriteArrayList<Integer>().addAllAbsent(l3); }
            catch (Throwable t) { unexpected(t); }

            // addAll(mutatingCollection)
            try { new CopyOnWriteArrayList<Integer>().addAll(l3); }
            catch (Throwable t) { unexpected(t); }

            // addAll(int, mutatingCollection)
            try { new CopyOnWriteArrayList<Integer>().addAll(0,l3); }
            catch (Throwable t) { unexpected(t); }
        }
        t2.join();
        t3.join();
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
    private abstract static class CheckedThread extends Thread {
        public abstract void realRun() throws Throwable;
        public void run() {
            try { realRun(); } catch (Throwable t) { unexpected(t); }}}
}
