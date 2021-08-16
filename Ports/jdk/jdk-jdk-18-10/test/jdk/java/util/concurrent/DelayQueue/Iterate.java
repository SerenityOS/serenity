/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6355327
 * @summary DelayQueue iterators should support concurrent modification
 * @author Martin Buchholz
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Iterate {
    private static class Godot implements Delayed {
        public long getDelay(TimeUnit unit) {return Long.MAX_VALUE;}
        public int compareTo(Delayed other) {return 0;}
    }

    private static void realMain(String[] args) throws Throwable {
        Godot[] godots = new Godot[] { new Godot(), new Godot(), new Godot() };
        DelayQueue<Godot> q = new DelayQueue<>(Arrays.asList(godots));
        Iterator<Godot> it = q.iterator();
        q.clear();
        check(it.hasNext());
        equal(it.next(), godots[0]);
        it.remove();
        check(q.isEmpty());

        q.addAll(Arrays.asList(godots));
        it = q.iterator();
        check(it.hasNext());
        it.next();
        equal(it.next(), godots[1]);
        it.remove();
        equal(q.size(), 2);
        check(q.contains(godots[0]));
        check(q.contains(godots[2]));
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
