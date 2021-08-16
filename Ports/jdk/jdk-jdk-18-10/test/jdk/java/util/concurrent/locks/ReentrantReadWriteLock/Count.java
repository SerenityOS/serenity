/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6207928 6328220 6378321 6625723
 * @summary Recursive lock invariant sanity checks
 * @library /test/lib
 * @author Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jdk.test.lib.Utils;

// I am the Cownt, and I lahve to cownt.
public class Count {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    final Random rnd = new Random();

    void lock(Lock lock) {
        try {
            switch (rnd.nextInt(4)) {
            case 0: lock.lock(); break;
            case 1: lock.lockInterruptibly(); break;
            case 2: check(lock.tryLock()); break;
            case 3: check(lock.tryLock(45, TimeUnit.MINUTES)); break;
            }
        } catch (Throwable t) { unexpected(t); }
    }

    void test(String[] args) throws Throwable {
        for (boolean fair : new boolean[] { true, false })
            for (boolean serialClone : new boolean[] { true, false }) {
                testReentrantLocks(fair, serialClone);
                testConcurrentReadLocks(fair, serialClone);
            }
    }

    void testConcurrentReadLocks(final boolean fair,
                                 final boolean serialClone) throws Throwable {
        final int nThreads = 10;
        final CyclicBarrier barrier = new CyclicBarrier(nThreads);
        final ExecutorService es = Executors.newFixedThreadPool(nThreads);
        final ReentrantReadWriteLock rwl = serialClone ?
            serialClone(new ReentrantReadWriteLock(fair)) :
            new ReentrantReadWriteLock(fair);
        for (int i = 0; i < nThreads; i++) {
            es.submit(new Runnable() { public void run() {
                try {
                    int n = 5;
                    for (int i = 0; i < n; i++) {
                        barrier.await();
                        equal(rwl.getReadHoldCount(), i);
                        equal(rwl.getWriteHoldCount(), 0);
                        check(! rwl.isWriteLocked());
                        equal(rwl.getReadLockCount(), nThreads * i);
                        barrier.await();
                        lock(rwl.readLock());
                    }
                    for (int i = 0; i < n; i++) {
                        rwl.readLock().unlock();
                        barrier.await();
                        equal(rwl.getReadHoldCount(), n-i-1);
                        equal(rwl.getReadLockCount(), nThreads*(n-i-1));
                        equal(rwl.getWriteHoldCount(), 0);
                        check(! rwl.isWriteLocked());
                        barrier.await();
                    }
                    THROWS(IllegalMonitorStateException.class,
                           new F(){void f(){rwl.readLock().unlock();}},
                           new F(){void f(){rwl.writeLock().unlock();}});
                    barrier.await();
                } catch (Throwable t) { unexpected(t); }}});}
        es.shutdown();
        check(es.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
    }

    void testReentrantLocks(final boolean fair,
                            final boolean serialClone) throws Throwable {
        final ReentrantLock rl = serialClone ?
            serialClone(new ReentrantLock(fair)) :
            new ReentrantLock(fair);
        final ReentrantReadWriteLock rwl = serialClone ?
            serialClone(new ReentrantReadWriteLock(fair)) :
            new ReentrantReadWriteLock(fair);
        final int depth = 10;
        equal(rl.isFair(), fair);
        equal(rwl.isFair(), fair);
        check(! rl.isLocked());
        check(! rwl.isWriteLocked());
        check(! rl.isHeldByCurrentThread());
        check(! rwl.isWriteLockedByCurrentThread());
        check(! rwl.writeLock().isHeldByCurrentThread());

        for (int i = 0; i < depth; i++) {
            equal(rl.getHoldCount(), i);
            equal(rwl.getReadLockCount(), i);
            equal(rwl.getReadHoldCount(), i);
            equal(rwl.getWriteHoldCount(), i);
            equal(rwl.writeLock().getHoldCount(), i);
            equal(rl.isLocked(), i > 0);
            equal(rwl.isWriteLocked(), i > 0);
            lock(rl);
            lock(rwl.writeLock());
            lock(rwl.readLock());
        }

        for (int i = depth; i > 0; i--) {
            check(! rl.hasQueuedThreads());
            check(! rwl.hasQueuedThreads());
            check(! rl.hasQueuedThread(Thread.currentThread()));
            check(! rwl.hasQueuedThread(Thread.currentThread()));
            check(rl.isLocked());
            check(rwl.isWriteLocked());
            check(rl.isHeldByCurrentThread());
            check(rwl.isWriteLockedByCurrentThread());
            check(rwl.writeLock().isHeldByCurrentThread());
            equal(rl.getQueueLength(), 0);
            equal(rwl.getQueueLength(), 0);
            equal(rwl.getReadLockCount(), i);
            equal(rl.getHoldCount(), i);
            equal(rwl.getReadHoldCount(), i);
            equal(rwl.getWriteHoldCount(), i);
            equal(rwl.writeLock().getHoldCount(), i);
            rwl.readLock().unlock();
            rwl.writeLock().unlock();
            rl.unlock();
        }
        THROWS(IllegalMonitorStateException.class,
               new F(){void f(){rl.unlock();}},
               new F(){void f(){rwl.readLock().unlock();}},
               new F(){void f(){rwl.writeLock().unlock();}});
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
        new Count().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
    void THROWS(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}

    static byte[] serializedForm(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }}
    static Object readObject(byte[] bytes)
        throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(bytes);
        return new ObjectInputStream(is).readObject();}
    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try { return (T) readObject(serializedForm(obj)); }
        catch (Exception e) { throw new RuntimeException(e); }}
}
