/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Martin Buchholz with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @library /test/lib
 * @run main DoneTimedGetLoops 300
 * @summary isDone returning true guarantees that subsequent timed get
 * will never throw TimeoutException.
 */

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jdk.test.lib.Utils;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class DoneTimedGetLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    final long testDurationMillisDefault = 10_000L;
    final long testDurationMillis;

    static class PublicFutureTask extends FutureTask<Boolean> {
        static final Runnable noop = new Runnable() { public void run() {} };
        PublicFutureTask() { super(noop, null); }
        public void set(Boolean v) { super.set(v); }
        public void setException(Throwable t) { super.setException(t); }
    }

    DoneTimedGetLoops(String[] args) {
        testDurationMillis = (args.length > 0) ?
            Long.valueOf(args[0]) : testDurationMillisDefault;
    }

    void test(String[] args) throws Throwable {
        final long testDurationNanos = testDurationMillis * 1000L * 1000L;
        final long quittingTimeNanos = System.nanoTime() + testDurationNanos;

        final AtomicReference<PublicFutureTask> normalRef
            = new AtomicReference<>();
        final AtomicReference<PublicFutureTask> abnormalRef
            = new AtomicReference<>();

        final Throwable throwable = new Throwable();

        abstract class CheckedThread extends Thread {
            CheckedThread(String name) {
                super(name);
                setDaemon(true);
                start();
            }
            /** Polls for quitting time. */
            protected boolean quittingTime() {
                return System.nanoTime() - quittingTimeNanos > 0;
            }
            /** Polls occasionally for quitting time. */
            protected boolean quittingTime(long i) {
                return (i % 1024) == 0 && quittingTime();
            }
            protected abstract void realRun() throws Exception;
            public void run() {
                try { realRun(); } catch (Throwable t) { unexpected(t); }
            }
        }

        Thread setter = new CheckedThread("setter") {
            protected void realRun() {
                while (! quittingTime()) {
                    PublicFutureTask future = new PublicFutureTask();
                    normalRef.set(future);
                    future.set(Boolean.TRUE);
                }}};

        Thread setterException = new CheckedThread("setterException") {
            protected void realRun() {
                while (! quittingTime()) {
                    PublicFutureTask future = new PublicFutureTask();
                    abnormalRef.set(future);
                    future.setException(throwable);
                }}};

        Thread doneTimedGetNormal = new CheckedThread("doneTimedGetNormal") {
            protected void realRun() throws Exception {
                while (! quittingTime()) {
                    PublicFutureTask future = normalRef.get();
                    if (future != null) {
                        while (!future.isDone())
                            ;
                        check(future.get(0L, TimeUnit.HOURS) == Boolean.TRUE);
                    }}}};

        Thread doneTimedGetAbnormal = new CheckedThread("doneTimedGetAbnormal") {
            protected void realRun() throws Exception {
                while (! quittingTime()) {
                    PublicFutureTask future = abnormalRef.get();
                    if (future != null) {
                        while (!future.isDone())
                            ;
                        try { future.get(0L, TimeUnit.HOURS); fail(); }
                        catch (ExecutionException t) {
                            check(t.getCause() == throwable);
                        }
                    }}}};

        for (Thread thread : new Thread[] {
                 setter,
                 setterException,
                 doneTimedGetNormal,
                 doneTimedGetAbnormal }) {
            thread.join(LONG_DELAY_MS + testDurationMillis);
            if (thread.isAlive()) {
                System.err.printf("Hung thread: %s%n", thread.getName());
                failed++;
                for (StackTraceElement e : thread.getStackTrace())
                    System.err.println(e);
                thread.join(LONG_DELAY_MS);
            }
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
        new DoneTimedGetLoops(args).instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
