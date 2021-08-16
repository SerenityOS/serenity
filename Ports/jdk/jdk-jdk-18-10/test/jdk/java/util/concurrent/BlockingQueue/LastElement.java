/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6215625 7161229
 * @modules java.base/java.util.concurrent:open
 * @summary Check correct behavior when last element is removed.
 * @author Martin Buchholz
 */

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class LastElement {
    void test(String[] args) throws Throwable {
        testQueue(new LinkedBlockingQueue<Integer>());
        testQueue(new LinkedBlockingDeque<Integer>());
        testQueue(new ArrayBlockingQueue<Integer>(10, true));
        testQueue(new ArrayBlockingQueue<Integer>(10, false));
        testQueue(new LinkedTransferQueue<Integer>());
        testQueue(new PriorityBlockingQueue<Integer>());
    }

    void testQueue(BlockingQueue<Integer> q) throws Throwable {
        Integer one = 1;
        Integer two = 2;
        Integer three = 3;

        // remove(Object)
        q.put(one);
        q.put(two);
        check(! q.isEmpty() && q.size() == 2);
        check(q.remove(one));
        check(q.remove(two));
        check(q.isEmpty() && q.size() == 0);
        q.put(three);
        try {check(q.take() == three);}
        catch (Throwable t) {unexpected(t);}
        check(q.isEmpty() && q.size() == 0);
        check(noRetention(q));

        // iterator().remove()
        q.clear();
        q.put(one);
        check(q.offer(two));
        check(! q.isEmpty() && q.size() == 2);
        Iterator<Integer> i = q.iterator();
        check(i.next() == one);
        i.remove();
        check(i.next() == two);
        i.remove();
        check(q.isEmpty() && q.size() == 0);
        q.put(three);
        try {check(q.take() == three);}
        catch (Throwable t) {unexpected(t);}
        check(q.isEmpty() && q.size() == 0);
    }

    boolean noRetention(BlockingQueue<?> q) {
        if (q instanceof PriorityBlockingQueue) {
            PriorityBlockingQueue<?> pbq = (PriorityBlockingQueue) q;
            try {
                java.lang.reflect.Field queue =
                    PriorityBlockingQueue.class.getDeclaredField("queue");
                queue.setAccessible(true);
                Object[] a = (Object[]) queue.get(pbq);
                return a[0] == null;
            }
            catch (NoSuchFieldException e) {
                unexpected(e);
            }
            catch (IllegalAccessException e) {
                // ignore - security manager must be installed
            }
        }
        return true;
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
        new LastElement().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
