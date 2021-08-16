/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6332435 8221168
 * @summary Basic tests for CountDownLatch
 * @library /test/lib
 * @author Seetharam Avadhanam, Martin Buchholz
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import jdk.test.lib.Utils;

public class Basic {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    abstract static class Awaiter extends Thread {
        volatile Throwable exception;
        volatile boolean interrupted;
        abstract void realRun() throws Exception;
        public final void run() {
            try { realRun(); }
            catch (Throwable ex) { exception = ex; }
            interrupted = Thread.interrupted();
        };
    }

    static Awaiter awaiter(CountDownLatch latch,
                           CountDownLatch gate) {
        return new Awaiter() {
            public void realRun() throws InterruptedException {
                gate.countDown();
                latch.await();
            }};
    }

    static Awaiter awaiter(CountDownLatch latch,
                           CountDownLatch gate,
                           long timeoutMillis) {
        return new Awaiter() {
            public void realRun() throws InterruptedException {
                gate.countDown();
                latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            }};
    }

    static Supplier<Awaiter> randomAwaiterSupplier(
            CountDownLatch latch, CountDownLatch gate) {
        return () -> (ThreadLocalRandom.current().nextBoolean())
            ? awaiter(latch, gate)
            : awaiter(latch, gate, LONG_DELAY_MS);
    }

    //----------------------------------------------------------------
    // Normal use
    //----------------------------------------------------------------
    public static void normalUse() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s = randomAwaiterSupplier(latch, gate);
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            gate.await();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (Awaiter awaiter : a)
            checkException(awaiter, null);
    }

    //----------------------------------------------------------------
    // One thread interrupted
    //----------------------------------------------------------------
    public static void threadInterrupted() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s = randomAwaiterSupplier(latch, gate);
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            gate.await();
            a[count-1].interrupt();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (int i = 0; i < a.length; i++) {
            Awaiter awaiter = a[i];
            Throwable ex = awaiter.exception;
            if ((i % 4) == 3 && !awaiter.interrupted)
                checkException(awaiter, InterruptedException.class);
            else
                checkException(awaiter, null);
        }
    }

    //----------------------------------------------------------------
    // One thread timed out
    //----------------------------------------------------------------
    public static void timeOut() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        long[] timeout = { 0L, 5L, 10L };

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s = randomAwaiterSupplier(latch, gate);
            a[count] = awaiter(latch, gate, timeout[i]); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            a[count] = s.get(); a[count++].start();
            gate.await();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (Awaiter awaiter : a)
            checkException(awaiter, null);
    }

    public static void main(String[] args) throws Throwable {
        try {
            normalUse();
        } catch (Throwable ex) { fail(ex); }
        try {
            threadInterrupted();
        } catch (Throwable ex) { fail(ex); }
        try {
            timeOut();
        } catch (Throwable ex) { fail(ex); }

        if (failures.get() > 0L)
            throw new AssertionError(failures.get() + " failures");
    }

    static final AtomicInteger failures = new AtomicInteger(0);

    static void fail(String msg) {
        fail(new AssertionError(msg));
    }

    static void fail(Throwable t) {
        t.printStackTrace();
        failures.getAndIncrement();
    }

    static void checkCount(CountDownLatch b, int expected) {
        if (b.getCount() != expected)
            fail("Count = " + b.getCount() +
                 ", expected = " + expected);
    }

    static void checkException(Awaiter awaiter, Class<? extends Throwable> c) {
        Throwable ex = awaiter.exception;
        if (! ((ex == null && c == null) || c.isInstance(ex)))
            fail("Expected: " + c + ", got: " + ex);
    }
}
