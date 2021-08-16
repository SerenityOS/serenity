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
 * @bug 6445158
 * @summary Basic tests for Phaser
 * @author Chris Hegarty
 * @modules java.management
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class Basic {

    private static void checkTerminated(final Phaser phaser) {
        check(phaser.isTerminated());
        int unarriverParties = phaser.getUnarrivedParties();
        int registeredParties = phaser.getRegisteredParties();
        int phase = phaser.getPhase();
        check(phase < 0);
        equal(phase, phaser.arrive());
        equal(phase, phaser.arriveAndDeregister());
        equal(phase, phaser.arriveAndAwaitAdvance());
        equal(phase, phaser.bulkRegister(10));
        equal(phase, phaser.register());
        try {
            equal(phase, phaser.awaitAdvanceInterruptibly(0));
            equal(phase, phaser.awaitAdvanceInterruptibly(0, 10, SECONDS));
        } catch (Exception ex) {
            unexpected(ex);
        }
        equal(phaser.getUnarrivedParties(), unarriverParties);
        equal(phaser.getRegisteredParties(), registeredParties);
    }

    private static void checkResult(Arriver a, Class<? extends Throwable> c) {
        Throwable t = a.result();
        if (! ((t == null && c == null) || (c != null && c.isInstance(t)))) {
            //      t.printStackTrace();
            fail("Mismatch in thread " +
                 a.getName() + ": " +
                 t + ", " +
                 (c == null ? "<null>" : c.getName()));
        } else {
            pass();
        }
    }

    //----------------------------------------------------------------
    // Mechanism to get all test threads into "running" mode.
    //----------------------------------------------------------------
    private static Phaser startingGate = new Phaser(3);

    private static void toTheStartingGate() {
        try {
            boolean expectNextPhase = (startingGate.getUnarrivedParties() == 1);
            int phase = startingGate.getPhase();
            equal(phase, startingGate.arrive());
            int awaitPhase;
            for (boolean interrupted = false;;) {
                try {
                    awaitPhase = startingGate.awaitAdvanceInterruptibly
                        (phase, 30, SECONDS);
                    if (interrupted) Thread.currentThread().interrupt();
                    break;
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
            }
            if (expectNextPhase) check(awaitPhase == phase + 1);
            else check(awaitPhase == phase || awaitPhase == phase + 1);
            pass();
        } catch (Throwable t) {
            unexpected(t);
            // reset(startingGate);
            throw new Error(t);
        }
    }

    //----------------------------------------------------------------
    // Convenience methods for creating threads that call arrive,
    // awaitAdvance, arriveAndAwaitAdvance, awaitAdvanceInterruptibly
    //----------------------------------------------------------------
    private abstract static class Arriver extends Thread {
        static AtomicInteger count = new AtomicInteger(1);

        Arriver() {
            this("Arriver");
        }

        Arriver(String name) {
            this.setName(name + ":" + count.getAndIncrement());
            this.setDaemon(true);
        }

        private volatile Throwable result;
        private volatile int phase;
        protected void result(Throwable result) { this.result = result; }
        public Throwable result() { return this.result; }
        protected void phase(int phase) { this.phase = phase; }
        public int phase() { return this.phase; }
    }

    private abstract static class Awaiter extends Arriver {
        Awaiter() { super("Awaiter"); }
        Awaiter(String name) { super(name); }
    }

    private static Arriver arriver(final Phaser phaser) {
        return new Arriver() { public void run() {
            toTheStartingGate();

            try { phase(phaser.arrive()); }
            catch (Throwable result) { result(result); }}};
    }

    private static AtomicInteger cycleArriveAwaitAdvance = new AtomicInteger(1);

    private static Awaiter awaiter(final Phaser phaser) {
        return new Awaiter() { public void run() {
            toTheStartingGate();

            try {
                if (cycleArriveAwaitAdvance.getAndIncrement() % 2 == 0)
                    phase(phaser.awaitAdvance(phaser.arrive()));
                else
                    phase(phaser.arriveAndAwaitAdvance());
            } catch (Throwable result) { result(result); }}};
    }

    private static Awaiter awaiter(final Phaser phaser,
                                   final long timeout,
                                   final TimeUnit unit) {
        return new Awaiter("InterruptibleWaiter") { public void run() {
            toTheStartingGate();

            try {
                if (timeout < 0)
                    phase(phaser.awaitAdvanceInterruptibly(phaser.arrive()));
                else
                    phase(phaser.awaitAdvanceInterruptibly(phaser.arrive(),
                                                     timeout,
                                                     unit));
            } catch (Throwable result) { result(result); }}};
    }

    // Returns an infinite lazy list of all possible arriver/awaiter combinations.
    private static Iterator<Arriver> arriverIterator(final Phaser phaser) {
        return new Iterator<Arriver>() {
            int i = 0;
            public boolean hasNext() { return true; }
            public Arriver next() {
                switch ((i++)&7) {
                    case 0: case 4:
                        return arriver(phaser);
                    case 1: case 5:
                        return awaiter(phaser);
                    case 2: case 6: case 7:
                        return awaiter(phaser, -1, SECONDS);
                    default:
                        return awaiter(phaser, 30, SECONDS); }}
            public void remove() {throw new UnsupportedOperationException();}};
    }

    // Returns an infinite lazy list of all possible awaiter only combinations.
    private static Iterator<Awaiter> awaiterIterator(final Phaser phaser) {
        return new Iterator<Awaiter>() {
            int i = 0;
            public boolean hasNext() { return true; }
            public Awaiter next() {
                switch ((i++)&7) {
                    case 1: case 4: case 7:
                        return awaiter(phaser);
                    case 2: case 5:
                        return awaiter(phaser, -1, SECONDS);
                    default:
                        return awaiter(phaser, 30, SECONDS); }}
            public void remove() {throw new UnsupportedOperationException();}};
    }

    static class SimpleTimer {
        long startTime = System.nanoTime();
        long elapsedMillis() {
            long now = System.nanoTime();
            long elapsed = NANOSECONDS.toMillis(now - startTime);
            startTime = now;
            return elapsed;
        }
        void printElapsed() { System.out.println(elapsedMillis() + " ms"); }
    }

    private static void realMain(String[] args) throws Throwable {
        SimpleTimer timer = new SimpleTimer();
        Thread.currentThread().setName("mainThread");

        //----------------------------------------------------------------
        System.out.print("Normal use: ");
        //----------------------------------------------------------------
        try {
            Phaser phaser = new Phaser(3);
            equal(phaser.getRegisteredParties(), 3);
            equal(phaser.getArrivedParties(), 0);
            equal(phaser.getPhase(), 0);
            check(phaser.getRoot().equals(phaser));
            equal(phaser.getParent(), null);
            check(!phaser.isTerminated());

            Iterator<Arriver> arrivers = arriverIterator(phaser);
            int phase = 0;
            for (int i = 0; i < 10; i++) {
                equal(phaser.getPhase(), phase++);
                Arriver a1 = arrivers.next(); a1.start();
                Arriver a2 = arrivers.next(); a2.start();
                toTheStartingGate();
                phaser.arriveAndAwaitAdvance();
                a1.join();
                a2.join();
                checkResult(a1, null);
                checkResult(a2, null);
                check(!phaser.isTerminated());
                equal(phaser.getRegisteredParties(), 3);
                equal(phaser.getArrivedParties(), 0);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("One thread interrupted: ");
        //----------------------------------------------------------------
        try {
            Phaser phaser = new Phaser(3);
            Iterator<Arriver> arrivers = arriverIterator(phaser);
            int phase = phaser.getPhase();
            for (int i = 0; i < 10; i++) {
                check(phaser.getPhase() == phase);
                Awaiter a1 = awaiter(phaser, 30, SECONDS); a1.start();
                Arriver a2 = arrivers.next(); a2.start();
                toTheStartingGate();
                a1.interrupt();
                a1.join();
                phaser.arriveAndAwaitAdvance();
                a2.join();
                checkResult(a1, InterruptedException.class);
                checkResult(a2, null);
                check(!phaser.isTerminated());
                equal(phaser.getRegisteredParties(), 3);
                equal(phaser.getArrivedParties(), 0);
                phase++;
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("Phaser is terminated while threads are waiting: ");
        //----------------------------------------------------------------
        try {
            for (int i = 0; i < 10; i++) {
                Phaser phaser = new Phaser(3);
                Iterator<Awaiter> awaiters = awaiterIterator(phaser);
                Arriver a1 = awaiters.next(); a1.start();
                Arriver a2 = awaiters.next(); a2.start();
                toTheStartingGate();
                while (phaser.getArrivedParties() < 2) Thread.yield();
                equal(0, phaser.getPhase());
                phaser.forceTermination();
                a1.join();
                a2.join();
                equal(0 + Integer.MIN_VALUE, a1.phase);
                equal(0 + Integer.MIN_VALUE, a2.phase);
                int arrivedParties = phaser.getArrivedParties();
                checkTerminated(phaser);
                equal(phaser.getArrivedParties(), arrivedParties);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("Adds new unarrived parties to this phaser: ");
        //----------------------------------------------------------------
        try {
            Phaser phaser = new Phaser(1);
            Iterator<Arriver> arrivers = arriverIterator(phaser);
            LinkedList<Arriver> arriverList = new LinkedList<>();
            int phase = phaser.getPhase();
            for (int i = 1; i < 5; i++) {
                startingGate = new Phaser(1+(3*i));
                check(phaser.getPhase() == phase);
                // register 3 more
                phaser.register(); phaser.register(); phaser.register();
                for (int z=0; z<(3*i); z++) {
                   arriverList.add(arrivers.next());
                }
                for (Arriver arriver : arriverList)
                    arriver.start();

                toTheStartingGate();
                phaser.arriveAndAwaitAdvance();

                for (Arriver arriver : arriverList) {
                    arriver.join();
                    checkResult(arriver, null);
                }
                equal(phaser.getRegisteredParties(), 1 + (3*i));
                equal(phaser.getArrivedParties(), 0);
                arriverList.clear();
                phase++;
            }
            startingGate = new Phaser(3);
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("One thread timed out: ");
        //----------------------------------------------------------------
        try {
            Phaser phaser = new Phaser(3);
            Iterator<Arriver> arrivers = arriverIterator(phaser);
            for (long timeout : new long[] { 0L, 12L }) {
                Awaiter a1 = awaiter(phaser, timeout, MILLISECONDS);
                a1.start();
                Arriver a2 = arrivers.next();
                a2.start();
                toTheStartingGate();
                a1.join();
                checkResult(a1, TimeoutException.class);
                phaser.arrive();
                a2.join();
                checkResult(a2, null);
                check(!phaser.isTerminated());
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("Barrier action completed normally: ");
        //----------------------------------------------------------------
        try {
            final AtomicInteger count = new AtomicInteger(0);
            final Phaser[] kludge = new Phaser[1];
            Phaser phaser = new Phaser(3) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    int countPhase = count.getAndIncrement();
                    equal(countPhase, phase);
                    equal(kludge[0].getPhase(), phase);
                    equal(kludge[0].getRegisteredParties(), registeredParties);
                    if (phase >= 3)
                        return true; // terminate

                    return false;
                }
            };
            kludge[0] = phaser;
            equal(phaser.getRegisteredParties(), 3);
            Iterator<Awaiter> awaiters = awaiterIterator(phaser);
            for (int i = 0; i < 4; i++) {
                Awaiter a1 = awaiters.next(); a1.start();
                Awaiter a2 = awaiters.next(); a2.start();
                toTheStartingGate();
                while (phaser.getArrivedParties() < 2) Thread.yield();
                phaser.arrive();
                a1.join();
                a2.join();
                checkResult(a1, null);
                checkResult(a2, null);
                equal(count.get(), i+1);
                if (i < 3) {
                    check(!phaser.isTerminated());
                    equal(phaser.getRegisteredParties(), 3);
                    equal(phaser.getArrivedParties(), 0);
                    equal(phaser.getUnarrivedParties(), 3);
                    equal(phaser.getPhase(), count.get());
                } else
                    checkTerminated(phaser);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

    }

    //--------------------- Infrastructure ---------------------------

    /**
     * A debugging tool to print stack traces of most threads, as jstack does.
     * Uninteresting threads are filtered out.
     */
    static void dumpTestThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        System.err.println("------ stacktrace dump start ------");
        for (ThreadInfo info : threadMXBean.dumpAllThreads(true, true)) {
            String name = info.getThreadName();
            if ("Signal Dispatcher".equals(name))
                continue;
            if ("Reference Handler".equals(name)
                && info.getLockName().startsWith("java.lang.ref.Reference$Lock"))
                continue;
            if ("Finalizer".equals(name)
                && info.getLockName().startsWith("java.lang.ref.ReferenceQueue$Lock"))
                continue;
            if ("process reaper".equals(name))
                continue;
            if (name != null && name.startsWith("ForkJoinPool.commonPool-worker"))
                continue;
            System.err.print(info);
        }
        System.err.println("------ stacktrace dump end ------");
    }

    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace(); dumpTestThreads();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
