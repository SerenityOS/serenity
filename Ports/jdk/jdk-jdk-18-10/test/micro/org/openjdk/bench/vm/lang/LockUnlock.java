/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.vm.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import java.util.concurrent.TimeUnit;


/**
 * Benchmark class for simple lock unlock tests. Nothing big should ever go into this class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class LockUnlock {

    @Param("100")
    private int innerCount;

    public Object lockObject1;
    public Object lockObject2;
    public int factorial;
    public int dummyInt1;
    public int dummyInt2;

    @Setup
    public void setup() {
        lockObject1 = new Object();
        lockObject2 = new Object();
        dummyInt1 = 47;
        dummyInt2 = 11; // anything
    }

    /** Perform a synchronized on a local object within a loop. */
    @Benchmark
    public void testSimpleLockUnlock() {
        Object localObject = lockObject1;
        for (int i = 0; i < innerCount; i++) {
            synchronized (localObject) {
                dummyInt1++;
                dummyInt2++;
            }
        }
    }

    /** Perform a recursive synchronized on a local object within a loop. */
    @Benchmark
    public void testRecursiveLockUnlock() {
        Object localObject = lockObject1;
        for (int i = 0; i < innerCount; i++) {
            synchronized (localObject) {
                synchronized (localObject) {
                    dummyInt1++;
                    dummyInt2++;
                }
            }
        }
    }

    /** Perform two synchronized after each other on the same local object. */
    @Benchmark
    public void testSerialLockUnlock() {
        Object localObject = lockObject1;
        for (int i = 0; i < innerCount; i++) {
            synchronized (localObject) {
                dummyInt1++;
            }
            synchronized (localObject) {
                dummyInt2++;
            }
        }
    }

    /**
     * Performs recursive synchronizations on the same local object.
     * <p/>
     * Result is 3628800
     */
    @Benchmark
    public void testRecursiveSynchronization() {
        factorial = fact(10);
    }

    private synchronized int fact(int n) {
        if (n == 0) {
            return 1;
        } else {
            return fact(n - 1) * n;
        }
    }

    /**
     * With two threads lockObject1 will be contended so should be
     * inflated.
     */
    @Threads(2)
    @Benchmark
    public void testContendedLock() {
        synchronized (lockObject1) {
            dummyInt1++;
        }
    }
}
