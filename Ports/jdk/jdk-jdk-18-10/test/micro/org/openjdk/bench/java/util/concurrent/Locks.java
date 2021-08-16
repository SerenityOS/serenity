/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util.concurrent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class Locks {

    private ReentrantLock reentrantLock;
    private ReentrantLock fairReentrantLock;
    private ReentrantReadWriteLock reentrantRWLock;
    private ReentrantReadWriteLock fairReentrantRWLock;
    private Semaphore semaphore;
    private Semaphore fairSemaphore;
    private Lock reentrantWriteLock;
    private Mutex mutex;

    @Setup
    public void setup() {
        reentrantLock = new ReentrantLock(false);
        fairReentrantLock = new ReentrantLock(true);
        reentrantRWLock = new ReentrantReadWriteLock(false);
        fairReentrantRWLock = new ReentrantReadWriteLock(true);
        semaphore = new Semaphore(1, false);
        fairSemaphore = new Semaphore(1, true);
        reentrantWriteLock = new ReentrantReadWriteLock(false).writeLock();
        mutex = new Mutex();
    }

    @Benchmark
    public void testSynchronizedBlock() {
        synchronized (this) {
            Blackhole.consumeCPU(10);
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testFairReentrantLock() {
        fairReentrantLock.lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            fairReentrantLock.unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testReentrantLock() {
        reentrantLock.lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            reentrantLock.unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testFairReentrantReadWriteLock() {
        fairReentrantRWLock.readLock().lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            fairReentrantRWLock.readLock().unlock();
        }
        fairReentrantRWLock.writeLock().lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            fairReentrantRWLock.writeLock().unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testReentrantReadWriteLock() {
        reentrantRWLock.readLock().lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            reentrantRWLock.readLock().unlock();
        }
        reentrantRWLock.writeLock().lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            reentrantRWLock.writeLock().unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testReentrantWriteLock() {
        reentrantWriteLock.lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            reentrantWriteLock.unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testFairSemaphore() throws InterruptedException {
        fairSemaphore.acquire();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            fairSemaphore.release();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testSemaphore() throws InterruptedException {
        semaphore.acquire();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            semaphore.release();
        }
        Blackhole.consumeCPU(5);
    }

    @Benchmark
    public void testAbstractQueueSynchronizer() {
        mutex.lock();
        try {
            Blackhole.consumeCPU(10);
        } finally {
            mutex.unlock();
        }
        Blackhole.consumeCPU(5);
    }

    @SuppressWarnings("serial")
    private final class Mutex extends AbstractQueuedSynchronizer implements Lock, java.io.Serializable {

        @Override
        public boolean isHeldExclusively() {
            return getState() == 1;
        }

        @Override
        public boolean tryAcquire(int acquires) {
            return compareAndSetState(0, 1);
        }

        @Override
        public boolean tryRelease(int releases) {
            setState(0);
            return true;
        }

        @Override
        public Condition newCondition() {
            return new ConditionObject();
        }

        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }

        @Override
        public void lock() {
            acquire(1);
        }

        @Override
        public boolean tryLock() {
            return tryAcquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return tryAcquireNanos(1, unit.toNanos(timeout));
        }

        @Override
        public void unlock() {
            release(1);
        }
    }
}
