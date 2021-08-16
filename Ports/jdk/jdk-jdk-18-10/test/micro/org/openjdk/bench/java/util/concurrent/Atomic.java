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
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class Atomic {

    public AtomicInteger aInteger;
    public AtomicLong aLong;
    public AtomicBoolean aBool;

    public Object testObject1;
    public Object testObject2;
    public AtomicReference<Object> aReference;

    /**
     * The test variables are allocated every iteration so you can assume they are initialized to get similar behaviour
     * across iterations
     */
    @Setup(Level.Iteration)
    public void setupIteration() {
        testObject1 = new Object();
        testObject2 = new Object();
        aInteger = new AtomicInteger(0);
        aBool = new AtomicBoolean(false);
        aReference = new AtomicReference<>(testObject1);
        aLong = new AtomicLong(0);
    }


    /** Always swap in value. This test should be compiled into a CAS */
    @Benchmark
    @OperationsPerInvocation(2)
    public void testAtomicIntegerAlways(Blackhole bh) {
        bh.consume(aInteger.compareAndSet(0, 2));
        bh.consume(aInteger.compareAndSet(2, 0));
    }

    /** Never write a value just return the old one. This test should be compiled into a CAS */
    @Benchmark
    public void testAtomicIntegerNever(Blackhole bh) {
        bh.consume(aInteger.compareAndSet(1, 3));
    }

    /** Flips an atomic boolean on and off */
    @Benchmark
    @OperationsPerInvocation(2)
    public void testAtomicBooleanFlip(Blackhole bh) {
        bh.consume(aBool.getAndSet(true));
        bh.consume(aBool.getAndSet(false));
    }

    /** Writes same value over and over */
    @Benchmark
    public void testAtomicBooleanSame(Blackhole bh) {
        bh.consume(aBool.getAndSet(true));
    }

    /** Increment and get over multiple threads */
    @Benchmark
    public void testAtomicIntegerGetAndIncrement(Blackhole bh) {
        bh.consume(aInteger.getAndIncrement());
    }

    /** Increment and get over multiple threads */
    @Benchmark
    public void testAtomicLongGetAndIncrement(Blackhole bh) {
        bh.consume(aLong.getAndIncrement());
    }

    /** Swap a few references */
    @Benchmark
    @OperationsPerInvocation(2)
    public void testAtomicReference(Blackhole bh) {
        bh.consume(aReference.compareAndSet(testObject1, testObject2));
        bh.consume(aReference.compareAndSet(testObject2, testObject1));
    }
}
