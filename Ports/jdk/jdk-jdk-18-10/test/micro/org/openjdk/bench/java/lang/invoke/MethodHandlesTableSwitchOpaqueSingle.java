/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class MethodHandlesTableSwitchOpaqueSingle {

    // Switch combinator test for a single input index, but opaquely fed in, so the JIT
    // does not see it as a constant.

    private static final MethodType callType = MethodType.methodType(int.class, int.class);

    private static final MutableCallSite cs = new MutableCallSite(callType);
    private static final MethodHandle target = cs.dynamicInvoker();

    private static final MethodHandle MH_DEFAULT;
    private static final MethodHandle MH_PAYLOAD;

    static {
        try {
            MH_DEFAULT = MethodHandles.lookup().findStatic(MethodHandlesTableSwitchOpaqueSingle.class, "defaultCase",
                    MethodType.methodType(int.class, int.class));
            MH_PAYLOAD = MethodHandles.lookup().findStatic(MethodHandlesTableSwitchOpaqueSingle.class, "payload",
                    MethodType.methodType(int.class, int.class, int.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Using batch size since we really need a per-invocation setup
    // but the measured code is too fast. Using JMH batch size doesn't work
    // since there is no way to do a batch-level setup as well.
    private static final int BATCH_SIZE = 1_000_000;

    @Param({
        "5",
        "10",
        "25"
    })
    public int numCases;

    public int input;

    @Setup(Level.Trial)
    public void setupTrial() throws Throwable {
        MethodHandle[] cases = IntStream.range(0, numCases)
                .mapToObj(i -> MethodHandles.insertArguments(MH_PAYLOAD, 1, i))
                .toArray(MethodHandle[]::new);
        MethodHandle switcher = MethodHandles.tableSwitch(MH_DEFAULT, cases);
        cs.setTarget(switcher);

        input = ThreadLocalRandom.current().nextInt(numCases);
    }

    private static int payload(int dropped, int constant) {
        return constant;
    }

    private static int defaultCase(int x) {
        throw new IllegalStateException();
    }

    @Benchmark
    public void testSwitch(Blackhole bh) throws Throwable {
        for (int i = 0; i < BATCH_SIZE; i++) {
            bh.consume((int) target.invokeExact(input));
        }
    }

}
