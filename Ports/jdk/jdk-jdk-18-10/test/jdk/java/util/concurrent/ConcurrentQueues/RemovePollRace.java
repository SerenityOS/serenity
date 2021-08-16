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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6785442
 * @summary Checks race between poll and remove(Object), while
 * occasionally moonlighting as a microbenchmark.
 * @run main RemovePollRace 1234
 */

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Map;

public class RemovePollRace {
    // Suitable for benchmarking.  Overridden by args[0] for testing.
    int count = 1024 * 1024;

    final Map<String,String> results = new ConcurrentHashMap<>();

    Collection<Queue<Boolean>> concurrentQueues() {
        List<Queue<Boolean>> queues = new ArrayList<>();
        queues.add(new ConcurrentLinkedDeque<Boolean>());
        queues.add(new ConcurrentLinkedQueue<Boolean>());
        queues.add(new ArrayBlockingQueue<Boolean>(count, false));
        queues.add(new ArrayBlockingQueue<Boolean>(count, true));
        queues.add(new LinkedBlockingQueue<Boolean>());
        queues.add(new LinkedBlockingDeque<Boolean>());
        queues.add(new LinkedTransferQueue<Boolean>());

        // Following additional implementations are available from:
        // http://gee.cs.oswego.edu/dl/concurrency-interest/index.html
        // queues.add(new SynchronizedLinkedListQueue<Boolean>());

        // Avoid "first fast, second slow" benchmark effect.
        Collections.shuffle(queues);
        return queues;
    }

    void prettyPrintResults() {
        List<String> classNames = new ArrayList<>(results.keySet());
        Collections.sort(classNames);
        int maxClassNameLength = 0;
        int maxNanosLength = 0;
        for (String name : classNames) {
            if (maxClassNameLength < name.length())
                maxClassNameLength = name.length();
            if (maxNanosLength < results.get(name).length())
                maxNanosLength = results.get(name).length();
        }
        String format = String.format("%%%ds %%%ds nanos/item%%n",
                                      maxClassNameLength, maxNanosLength);
        for (String name : classNames)
            System.out.printf(format, name, results.get(name));
    }

    void test(String[] args) throws Throwable {
        if (args.length > 0)
            count = Integer.valueOf(args[0]);

        // Warmup
        for (Queue<Boolean> queue : concurrentQueues())
            test(queue);
        results.clear();
        for (Queue<Boolean> queue : concurrentQueues())
            test(queue);

        prettyPrintResults();
    }

    void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException e) { unexpected(e); }
    }

    void test(final Queue<Boolean> q) throws Throwable {
        long t0 = System.nanoTime();
        final int SPINS = 5;
        final AtomicLong removes = new AtomicLong(0);
        final AtomicLong polls = new AtomicLong(0);

        // We need at least 3 threads, but we don't want to use too
        // many on massively multi-core systems.
        final int cpus = Runtime.getRuntime().availableProcessors();
        final int threadsToUse = Math.max(3, Math.min(cpus, 16));
        final int adderCount = threadsToUse / 3;
        final int removerCount = adderCount;
        final int pollerCount = removerCount;
        final int threadCount = adderCount + removerCount + pollerCount;

        final CountDownLatch startingGate = new CountDownLatch(1);
        final CountDownLatch addersDone = new CountDownLatch(adderCount);
        final Runnable remover = new Runnable() {
            public void run() {
                await(startingGate);
                int spins = 0;
                for (;;) {
                    boolean quittingTime = (addersDone.getCount() == 0);
                    if (q.remove(Boolean.TRUE))
                        removes.getAndIncrement();
                    else if (quittingTime)
                        break;
                    else if (++spins > SPINS) {
                        Thread.yield();
                        spins = 0;
                    }}}};
        final Runnable poller = new Runnable() {
            public void run() {
                await(startingGate);
                int spins = 0;
                for (;;) {
                    boolean quittingTime = (addersDone.getCount() == 0);
                    if (q.poll() == Boolean.TRUE)
                        polls.getAndIncrement();
                    else if (quittingTime)
                        break;
                    else if (++spins > SPINS) {
                        Thread.yield();
                        spins = 0;
                    }}}};
        final Runnable adder = new Runnable() {
            public void run() {
                await(startingGate);
                for (int i = 0; i < count; i++) {
                    for (;;) {
                        try { q.add(Boolean.TRUE); break; }
                        catch (IllegalStateException e) { Thread.yield(); }
                    }
                }
                addersDone.countDown();
            }};

        final List<Thread> adders   = new ArrayList<>();
        final List<Thread> removers = new ArrayList<>();
        final List<Thread> pollers  = new ArrayList<>();
        for (int i = 0; i < adderCount; i++)
            adders.add(checkedThread(adder));
        for (int i = 0; i < removerCount; i++)
            removers.add(checkedThread(remover));
        for (int i = 0; i < pollerCount; i++)
            pollers.add(checkedThread(poller));

        final List<Thread> allThreads = new ArrayList<>();
        allThreads.addAll(removers);
        allThreads.addAll(pollers);
        allThreads.addAll(adders);

        for (Thread t : allThreads)
            t.start();
        startingGate.countDown();
        for (Thread t : allThreads)
            t.join();

        String className = q.getClass().getSimpleName();
        long elapsed = System.nanoTime() - t0;
        int nanos = (int) ((double) elapsed / (adderCount * count));
        results.put(className, String.valueOf(nanos));
        if (removes.get() + polls.get() != adderCount * count) {
            String msg = String.format
                ("class=%s removes=%s polls=%d count=%d",
                 className, removes.get(), polls.get(), count);
            fail(msg);
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
        new RemovePollRace().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    Thread checkedThread(final Runnable r) {
        return new Thread() {public void run() {
            try {r.run();} catch (Throwable t) {unexpected(t);}}};}
}
