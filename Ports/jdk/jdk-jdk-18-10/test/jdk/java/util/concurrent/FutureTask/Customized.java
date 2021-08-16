/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6464365
 * @summary Test state transitions; check protected methods are called
 * @author Martin Buchholz
 */

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class Customized {
    static final AtomicLong doneCount = new AtomicLong(0);
    static final AtomicLong setCount = new AtomicLong(0);
    static final AtomicLong setExceptionCount = new AtomicLong(0);

    static void equal(long expected, AtomicLong actual) {
        equal(expected, actual.get());
    }

    static void equalCounts(long x, long y, long z) {
        equal(x, doneCount);
        equal(y, setCount);
        equal(z, setExceptionCount);
    }

    static class MyFutureTask<V> extends FutureTask<V> {
        MyFutureTask(Runnable r, V result) { super(r, result); }
        protected void done() {
            doneCount.getAndIncrement();
            super.done();
        }
        protected void set(V v) {
            setCount.getAndIncrement();
            super.set(v);
        }
        protected void setException(Throwable t) {
            setExceptionCount.getAndIncrement();
            super.setException(t);
        }
        public boolean runAndReset() {
            return super.runAndReset();
        }
    }

    static <V> void checkReady(final FutureTask<V> task) {
        check(! task.isDone());
        check(! task.isCancelled());
        THROWS(TimeoutException.class,
               () -> task.get(0L, TimeUnit.SECONDS));
    }

    static <V> void checkDone(final FutureTask<V> task) {
        try {
            check(task.isDone());
            check(! task.isCancelled());
            check(task.get() != null);
        } catch (Throwable t) { unexpected(t); }
    }

    static <V> void checkCancelled(final FutureTask<V> task) {
        check(task.isDone());
        check(task.isCancelled());
        THROWS(CancellationException.class,
               () -> task.get(0L, TimeUnit.SECONDS),
               () -> task.get());
    }

    static <V> void checkThrew(final FutureTask<V> task) {
        check(task.isDone());
        check(! task.isCancelled());
        THROWS(ExecutionException.class,
               () -> task.get(0L, TimeUnit.SECONDS),
               () -> task.get());
    }

    static <V> void cancel(FutureTask<V> task, boolean mayInterruptIfRunning) {
        task.cancel(mayInterruptIfRunning);
        checkCancelled(task);
    }

    static <V> void run(FutureTask<V> task) {
        boolean isCancelled = task.isCancelled();
        task.run();
        check(task.isDone());
        equal(isCancelled, task.isCancelled());
    }

    static void realMain(String[] args) throws Throwable {
        final Runnable nop = new Runnable() {
                public void run() {}};
        final Runnable bad = new Runnable() {
                public void run() { throw new Error(); }};

        try {
            final MyFutureTask<Long> task = new MyFutureTask<>(nop, 42L);
            checkReady(task);
            equalCounts(0,0,0);
            check(task.runAndReset());
            checkReady(task);
            equalCounts(0,0,0);
            run(task);
            checkDone(task);
            equalCounts(1,1,0);
            equal(42L, task.get());
            run(task);
            checkDone(task);
            equalCounts(1,1,0);
            equal(42L, task.get());
        } catch (Throwable t) { unexpected(t); }

        try {
            final MyFutureTask<Long> task = new MyFutureTask<>(nop, 42L);
            cancel(task, false);
            equalCounts(2,1,0);
            cancel(task, false);
            equalCounts(2,1,0);
            run(task);
            equalCounts(2,1,0);
            check(! task.runAndReset());
        } catch (Throwable t) { unexpected(t); }

        try {
            final MyFutureTask<Long> task = new MyFutureTask<>(bad, 42L);
            checkReady(task);
            run(task);
            checkThrew(task);
            equalCounts(3,1,1);
            run(task);
            equalCounts(3,1,1);
        } catch (Throwable t) { unexpected(t); }

        try {
            final MyFutureTask<Long> task = new MyFutureTask<>(nop, 42L);
            checkReady(task);
            task.set(99L);
            checkDone(task);
            equalCounts(4,2,1);
            run(task);
            equalCounts(4,2,1);
            task.setException(new Throwable());
            checkDone(task);
            equalCounts(4,2,2);
        } catch (Throwable t) { unexpected(t); }

        try {
            final MyFutureTask<Long> task = new MyFutureTask<>(nop, 42L);
            checkReady(task);
            task.setException(new Throwable());
            checkThrew(task);
            equalCounts(5,2,3);
            run(task);
            equalCounts(5,2,3);
            task.set(99L);
            checkThrew(task);
            equalCounts(5,3,3);
        } catch (Throwable t) { unexpected(t); }

        System.out.printf("doneCount=%d%n", doneCount.get());
        System.out.printf("setCount=%d%n", setCount.get());
        System.out.printf("setExceptionCount=%d%n", setExceptionCount.get());
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
