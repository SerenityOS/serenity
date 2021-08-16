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
 * @bug 8005697
 * @summary Basic tests for StampedLock
 * @library /test/lib
 * @author Chris Hegarty
 */

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Iterator;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import jdk.test.lib.Utils;

public class Basic {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static void checkResult(Locker l, Class<? extends Throwable> c) {
        Throwable t = l.thrown();
        if (! ((t == null && c == null) || (c != null && c.isInstance(t)))) {
            fail("Mismatch in thread " +
                 l.getName() + ": " +
                 t + ", " +
                 (c == null ? "<null>" : c.getName()));
        }

        if (c == null)
            check(l.stamp() != 0L);  // must have acquired the lock
        else
            check(l.stamp() == 0L);  // must NOT have acquired the lock
    }

    //----------------------------------------------------------------
    // Mechanism to get all test threads into "running" mode.
    //----------------------------------------------------------------
    static void toTheStartingGate(Phaser gate) {
        try {
            gate.arriveAndAwaitAdvance();
        } catch (Throwable t) {
            unexpected(t);
        }
    }

    abstract static class Locker extends Thread {
        static AtomicInteger count = new AtomicInteger(1);
        private volatile Throwable thrown;
        private volatile long stamp;
        protected void thrown(Throwable thrown) { this.thrown = thrown; }
        public Throwable thrown() { return thrown; }
        protected void stamp(long stamp) { this.stamp = stamp; }
        public long stamp() { return stamp; }

        Locker() {
            this("Locker");
        }

        Locker(String name) {
            this.setName(name + ":" + count.getAndIncrement());
            this.setDaemon(true);
        }
    }

    abstract static class Reader extends Locker {
        Reader() { super("Reader"); }
        Reader(String name) { super(name); }
    }

    static Reader reader(final StampedLock sl, final Phaser gate) {
        return new Reader() { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            stamp(sl.readLock());
            try {
                check(sl.validate(stamp()));
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
            } finally { sl.unlockRead(stamp()); } }};
    }

    static Reader readerView(final StampedLock sl, final Phaser gate) {
        return new Reader("ReaderView") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            final Lock rl = sl.asReadLock();
            rl.lock();
            try {
                stamp(1L);   // got the lock
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
            } finally { rl.unlock(); } }};
    }

    static Reader reader(StampedLock sl, Phaser gate, boolean view) {
        return view ? readerView(sl, gate) : reader(sl, gate);
    }

    static Reader interruptibleReader(final StampedLock sl,
                                      final long timeout,
                                      final TimeUnit unit,
                                      final Phaser gate) {
        return new Reader("InterruptibleReader") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            try {
                if (timeout < 0)
                    stamp(sl.readLockInterruptibly());
                else
                    stamp(sl.tryReadLock(timeout, unit));
                check(sl.validate(stamp()));
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
            } catch (Throwable x) { thrown(x);
            } finally { if (stamp() != 0L) sl.unlockRead(stamp()); } }};
    }

    static Reader interruptibleReaderView(final StampedLock sl,
                                          final long timeout,
                                          final TimeUnit unit,
                                          final Phaser gate) {
        return new Reader("InterruptibleReaderView") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            final Lock rl = sl.asReadLock();

            try {
                if (timeout < 0)
                    rl.lockInterruptibly();
                else
                    rl.tryLock(timeout, unit);
                stamp(1L);  // got the lock
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
            } catch (Throwable x) { thrown(x);
            } finally { if (stamp() != 0L) rl.unlock(); } }};
    }

    static Reader interruptibleReader(final StampedLock sl,
                                      final long timeout,
                                      final TimeUnit unit,
                                      final Phaser gate,
                                      final boolean view) {
        return view ? interruptibleReaderView(sl, timeout, unit, gate)
                    : interruptibleReader(sl, timeout, unit, gate);
    }

    abstract static class Writer extends Locker {
        Writer() { super("Writer"); }
        Writer(String name) { super(name); }
    }

    static Writer writer(final StampedLock sl, final Phaser gate) {
        return new Writer() { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            try {
                stamp(sl.writeLock());
                check(sl.validate(stamp()));
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
            } finally { sl.unlockWrite(stamp()); } }};
    }

    static Writer writerView(final StampedLock sl, final Phaser gate) {
        return new Writer("WriterView") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            Lock wl = sl.asWriteLock();
            wl.lock();
            try {
                stamp(1L);  // got the lock
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
            } finally { wl.unlock(); } }};
    }

    static Writer writer(StampedLock sl, Phaser gate, boolean view) {
        return view ? writerView(sl, gate) : writer(sl, gate);
    }

    static Writer interruptibleWriter(final StampedLock sl,
                                      final long timeout,
                                      final TimeUnit unit,
                                      final Phaser gate) {
        return new Writer("InterruptibleWriter") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            try {
                if (timeout < 0)
                    stamp(sl.writeLockInterruptibly());
                else
                    stamp(sl.tryWriteLock(timeout, unit));
                check(sl.validate(stamp()));
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
            } catch (Throwable x) { thrown(x);
            } finally { if (stamp() != 0L) sl.unlockWrite(stamp()); } }};
    }

    static Writer interruptibleWriterView(final StampedLock sl,
                                          final long timeout,
                                          final TimeUnit unit,
                                          final Phaser gate) {
        return new Writer("InterruptibleWriterView") { public void run() {
            if (gate != null ) toTheStartingGate(gate);
            Lock wl = sl.asWriteLock();
            try {
                if (timeout < 0)
                    wl.lockInterruptibly();
                else
                    wl.tryLock(timeout, unit);
                stamp(1L);  // got the lock
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
            } catch (Throwable x) { thrown(x);
            } finally { if (stamp() != 0L) wl.unlock(); } }};
    }

    static Writer interruptibleWriter(final StampedLock sl,
                                      final long timeout,
                                      final TimeUnit unit,
                                      final Phaser gate,
                                      final boolean view) {
        return view ? interruptibleWriterView(sl, timeout, unit, gate)
                    : interruptibleWriter(sl, timeout, unit, gate);
    }

    // Returns an infinite lazy list of all possible reader combinations.
    static Iterator<Reader> readerIterator(final StampedLock sl,
                                           final Phaser gate) {
        return new Iterator<Reader>() {
            int i = 0;
            boolean view = false;
            public boolean hasNext() { return true; }
            public Reader next() {
                switch ((i++)&7) {
                    case 1: case 4: case 7:
                        return reader(sl, gate, view ^= true);
                    case 2: case 5:
                        return interruptibleReader(sl, -1, SECONDS, gate, view ^= true);
                    default:
                        return interruptibleReader(sl, LONG_DELAY_MS, MILLISECONDS, gate, view ^= true); }}
            public void remove() {throw new UnsupportedOperationException();}};
    }

    // Returns an infinite lazy list of all possible writer combinations.
    static Iterator<Writer> writerIterator(final StampedLock sl,
                                           final Phaser gate) {
        return new Iterator<Writer>() {
            int i = 0;
            boolean view = false;
            public boolean hasNext() { return true; }
            public Writer next() {
                switch ((i++)&7) {
                    case 1: case 4: case 7:
                        return writer(sl, gate, view ^= true);
                    case 2: case 5:
                        return interruptibleWriter(sl, -1, SECONDS, gate, view ^= true);
                    default:
                        return interruptibleWriter(sl, LONG_DELAY_MS, MILLISECONDS, gate, view ^= true); }}
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

    static void waitForThreadToBlock(Thread thread) {
        for (long startTime = 0;;) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING ||
                state == Thread.State.TIMED_WAITING)
                break;
            if (startTime == 0) startTime = System.nanoTime();
            else if (System.nanoTime() - startTime > 10L * 1000L * 1000L * 1000L)
                throw new AssertionError("timed out waiting for thread to block");
        }
    }

    private static void realMain(String[] args) throws Throwable {
        SimpleTimer timer = new SimpleTimer();

        //----------------------------------------------------------------
        System.out.print("Some basic sanity: ");
        //----------------------------------------------------------------
        try {
            final StampedLock sl = new StampedLock();
            check(!sl.isReadLocked());
            check(!sl.isWriteLocked());
            long stamp = sl.tryOptimisticRead();
            check(stamp != 0L);
            check(sl.validate(stamp));
            check(!sl.validate(0));

            stamp = sl.writeLock();
            try {
                check(sl.validate(stamp));
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
                check(sl.tryReadLock() == 0L);
                check(sl.tryReadLock(1, MILLISECONDS) == 0L);
                check(sl.tryOptimisticRead() == 0L);
                check(sl.tryWriteLock() == 0L);
                check(sl.tryWriteLock(1, MILLISECONDS) == 0L);
                check(!sl.tryUnlockRead());
                check(sl.tryConvertToWriteLock(stamp) == stamp);
                try {
                    sl.unlockRead(stamp);
                    fail("Expected unlockRead to throw when not holding read lock");
                } catch (IllegalMonitorStateException x) {
                    pass();
                }
                check(sl.validate(stamp));
            } finally {
                sl.unlockWrite(stamp);
            }
            check(!sl.isWriteLocked());

            stamp = sl.readLock();
            try {
                check(sl.validate(stamp));
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(sl.tryOptimisticRead() != 0L);
                check(sl.tryWriteLock() == 0L);
                check(sl.tryWriteLock(1, MILLISECONDS) == 0L);
                check(!sl.tryUnlockWrite());
                check(sl.tryConvertToReadLock(stamp) == stamp);
                try {
                    sl.unlockWrite(stamp);
                    fail("Expected unlockWrite to throw when not holding read lock");
                } catch (IllegalMonitorStateException x) {
                    pass();
                }
                check(sl.validate(stamp));
            } finally {
                sl.unlockRead(stamp);
            }
            check(!sl.isReadLocked());

            stamp = sl.tryReadLock(1, MILLISECONDS);
            try {
                check(stamp != 0L);
            } finally {
                sl.unlockRead(stamp);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("Multiple writers single reader: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();
            int nThreads = 10;
            Phaser gate = new Phaser(nThreads + 2);
            Iterator<Writer> writers = writerIterator(sl, gate);
            Iterator<Reader> readers = readerIterator(sl, gate);
            for (int i = 0; i < 2; i++) {
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                check(sl.tryOptimisticRead() != 0L);
                Locker[] wThreads = new Locker[nThreads];
                for (int j=0; j<nThreads; j++)
                    wThreads[j] = writers.next();
                for (int j=0; j<nThreads; j++)
                    wThreads[j].start();
                Reader reader = readers.next(); reader.start();
                toTheStartingGate(gate);
                reader.join();
                for (int j=0; j<nThreads; j++)
                    wThreads[j].join();
                for (int j=0; j<nThreads; j++)
                    checkResult(wThreads[j], null);
                checkResult(reader, null);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("Multiple readers single writer: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();
            int nThreads = 10;
            Phaser gate = new Phaser(nThreads + 2);
            Iterator<Writer> writers = writerIterator(sl, gate);
            Iterator<Reader> readers = readerIterator(sl, gate);
            for (int i = 0; i < 2; i++) {
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                check(sl.tryOptimisticRead() != 0L);
                Locker[] rThreads = new Locker[nThreads];
                for (int j=0; j<nThreads; j++)
                    rThreads[j] = readers.next();
                for (int j=0; j<nThreads; j++)
                    rThreads[j].start();
                Writer writer = writers.next(); writer.start();
                toTheStartingGate(gate);
                writer.join();
                for (int j=0; j<nThreads; j++)
                    rThreads[j].join();
                for (int j=0; j<nThreads; j++)
                    checkResult(rThreads[j], null);
                checkResult(writer, null);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("thread interrupted: ");
        //----------------------------------------------------------------
        try {
            // We test interrupting both before and after trying to acquire
            boolean view = false;
            StampedLock sl = new StampedLock();
            for (long timeout : new long[] { -1L, LONG_DELAY_MS, -1L, LONG_DELAY_MS }) {
                long stamp;
                Thread.State state;

                stamp = sl.writeLock();
                try {
                    Reader r = interruptibleReader(sl, timeout, MILLISECONDS, null, view);
                    r.start();
                    r.interrupt();
                    r.join();
                    checkResult(r, InterruptedException.class);
                } finally {
                    sl.unlockWrite(stamp);
                }

                stamp = sl.writeLock();
                try {
                    Reader r = interruptibleReader(sl, timeout, MILLISECONDS, null, view);
                    r.start();
                    waitForThreadToBlock(r);
                    r.interrupt();
                    r.join();
                    checkResult(r, InterruptedException.class);
                } finally {
                    sl.unlockWrite(stamp);
                }

                stamp = sl.readLock();
                try {
                    Writer w = interruptibleWriter(sl, timeout, MILLISECONDS, null, view);
                    w.start();
                    w.interrupt();
                    w.join();
                    checkResult(w, InterruptedException.class);
                } finally {
                    sl.unlockRead(stamp);
                }

                stamp = sl.readLock();
                try {
                    Writer w = interruptibleWriter(sl, timeout, MILLISECONDS, null, view);
                    w.start();
                    waitForThreadToBlock(w);
                    w.interrupt();
                    w.join();
                    checkResult(w, InterruptedException.class);
                } finally {
                    sl.unlockRead(stamp);
                }

                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                check(sl.tryOptimisticRead() != 0L);
                if (timeout == LONG_DELAY_MS)
                    view = true;
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("timeout: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();
            for (long timeout : new long[] { 0L, 5L }) {
                long stamp = sl.writeLock();
                try {
                    check(sl.tryReadLock(timeout, MILLISECONDS) == 0L);
                } finally {
                    sl.unlockWrite(stamp);
                }
                stamp = sl.readLock();
                try {
                    check(sl.tryWriteLock(timeout, MILLISECONDS) == 0L);
                } finally {
                    sl.unlockRead(stamp);
                }
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                check(sl.tryOptimisticRead() != 0L);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("optimistic read: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();
            Iterator<Writer> writers = writerIterator(sl, null);
            Iterator<Reader> readers = readerIterator(sl, null);
            for (int i = 0; i < 10; i++) {
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                long stamp = sl.tryOptimisticRead();
                check(stamp != 0L);
                check(sl.tryConvertToOptimisticRead(stamp) == stamp);
                Reader r = readers.next(); r.start();
                r.join();
                checkResult(r, null);
                check(sl.validate(stamp));
                check(sl.tryConvertToOptimisticRead(stamp) == stamp);
                Writer w = writers.next(); w.start();
                w.join();
                checkResult(w, null);
                check(sl.validate(stamp) == false);
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("convert: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();
            for (int i = 0; i < 2; i++) {
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(!sl.tryUnlockRead());
                check(!sl.tryUnlockWrite());
                long stamp = sl.tryOptimisticRead();
                check(stamp != 0L);
                check((stamp = sl.tryConvertToReadLock(stamp)) != 0L);
                check(sl.validate(stamp));
                check(sl.isReadLocked());
                check(sl.tryWriteLock() == 0L);
                check(sl.tryWriteLock(1L, MILLISECONDS) == 0L);
                check((stamp = sl.tryConvertToWriteLock(stamp)) != 0L);
                check(sl.validate(stamp));
                check(!sl.isReadLocked());
                check(sl.isWriteLocked());
                check(sl.tryReadLock(1L, MILLISECONDS) == 0L);
                if (i != 0) {
                    sl.unlockWrite(stamp);
                    continue;
                }
                // convert down
                check((stamp = sl.tryConvertToReadLock(stamp)) != 0L);
                check(sl.validate(stamp));
                check(sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(sl.tryWriteLock() == 0L);
                check(sl.tryWriteLock(1L, MILLISECONDS) == 0L);
                check((stamp = sl.tryConvertToOptimisticRead(stamp)) != 0L);
                check(sl.validate(stamp));
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());
                check(sl.validate(stamp));
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();

        //----------------------------------------------------------------
        System.out.print("views: ");
        //----------------------------------------------------------------
        try {
            StampedLock sl = new StampedLock();

            Lock rl = sl.asReadLock();
            Lock wl = sl.asWriteLock();
            for (int i = 0; i < 2; i++) {
                rl.lock();
                try {
                    check(sl.isReadLocked());
                    check(!sl.isWriteLocked());
                    check(sl.tryWriteLock() == 0L);
                    check(sl.tryWriteLock(1L, MILLISECONDS) == 0L);
                } finally {
                    rl.unlock();
                }
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());

                wl.lock();
                try {
                    check(!sl.isReadLocked());
                    check(sl.isWriteLocked());
                    check(sl.tryWriteLock() == 0L);
                    check(sl.tryWriteLock(1L, MILLISECONDS) == 0L);
                } finally {
                    wl.unlock();
                }
                check(!sl.isReadLocked());
                check(!sl.isWriteLocked());

                ReadWriteLock rwl = sl.asReadWriteLock();
                rl = rwl.readLock();
                wl = rwl.writeLock();
            }
        } catch (Throwable t) { unexpected(t); }
        timer.printElapsed();
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
