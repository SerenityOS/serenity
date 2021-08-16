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
package org.openjdk.bench.vm.lambda.invoke;

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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * evaluates invocation costs.
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Function1 {

    public interface FunctionII {
        int foo(int x);
    }

    public interface FunctionIL {
        int foo(Integer x);
    }

    public interface FunctionLL {
        Integer foo(Integer x);
    }

    private static final int LIMIT = 1024;

    private final int[]     dataI = new int[LIMIT + 1];
    private final Integer[] dataL = new Integer[LIMIT + 1];

    @Setup(Level.Iteration)
    public void setup() {
        for (int i = 0; i < dataI.length; i++) {
            int value = ThreadLocalRandom.current().nextInt(10001,420000); // bypass Integer.cache
            dataI[i] = value;
            dataL[i] = value;
        }
    }



    public int fooInstanceII(int x) {
        return x;
    }

    public static int fooStaticII(int x) {
        return x;
    }

    public int fooInstanceIL(Integer v) {
        return v;
    }

    public static int fooStaticIL(Integer v) {
        return v;
    }

    public Integer fooInstanceLL(Integer v) {
        return v;
    }

    public static Integer fooStaticLL(Integer v) {
        return v;
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void baselineII(Blackhole bh) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(fooInstanceII(dataI[i]));
        }
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void baselineIL(Blackhole bh) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(fooInstanceIL(dataL[i]));
        }
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void baselineLL(Blackhole bh) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(fooInstanceLL(dataL[i]));
        }
    }

    public final FunctionII anonymII =
            new FunctionII() {
                @Override
                public int foo(int x) {
                    return x;
                }
            };

    public final FunctionIL anonymIL =
            new FunctionIL() {
                @Override
                public int foo(Integer v) {
                    return v;
                }
            };

    public final FunctionLL anonymLL =
            new FunctionLL() {
                @Override
                public Integer foo(Integer v) {
                    return v;
                }
            };


    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void innerII(Blackhole bh) {
        processII(bh, anonymII);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void innerIL(Blackhole bh) {
        processIL(bh, anonymIL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void innerLL(Blackhole bh) {
        processLL(bh, anonymLL);
    }

    public final FunctionII lambdaII =  x -> x;

    public final FunctionIL lambdaIL =  v -> v;

    public final FunctionLL lambdaLL =  v -> v;


    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void lambdaII(Blackhole bh) {
        processII(bh, lambdaII);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void lambdaIL(Blackhole bh) {
        processIL(bh, lambdaIL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void lambdaLL(Blackhole bh) {
        processLL(bh, lambdaLL);
    }



    public final FunctionII mref_II2II  = Function1::fooStaticII;
    public final FunctionII mref_II2II_bound = this::fooInstanceII;
    public final FunctionIL mref_II2IL  = Function1::fooStaticII;
    public final FunctionIL mref_II2IL_bound = this::fooInstanceII;
    public final FunctionLL mref_II2LL  = Function1::fooStaticII;
    public final FunctionLL mref_II2LL_bound = this::fooInstanceII;

    public final FunctionII mref_IL2II  = Function1::fooStaticIL;
    public final FunctionII mref_IL2II_bound = this::fooInstanceIL;
    public final FunctionIL mref_IL2IL  = Function1::fooStaticIL;
    public final FunctionIL mref_IL2IL_bound = this::fooInstanceIL;
    public final FunctionLL mref_IL2LL  = Function1::fooStaticIL;
    public final FunctionLL mref_IL2LL_bound = this::fooInstanceIL;

    public final FunctionII mref_LL2II  = Function1::fooStaticLL;
    public final FunctionII mref_LL2II_bound = this::fooInstanceLL;
    public final FunctionIL mref_LL2IL  = Function1::fooStaticLL;
    public final FunctionIL mref_LL2IL_bound = this::fooInstanceLL;
    public final FunctionLL mref_LL2LL  = Function1::fooStaticLL;
    public final FunctionLL mref_LL2LL_bound = this::fooInstanceLL;


    // mref naming
    // sig1_sig2 where:
    // sig1 - signature of the method referenced by method ref
    // sig2 - FuntionalInterface signature

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefII_II(Blackhole bh) {
        processII(bh, mref_II2II);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndII_II(Blackhole bh) {
        processII(bh, mref_II2II_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefII_IL(Blackhole bh) {
        processIL(bh, mref_II2IL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndII_IL(Blackhole bh) {
        processIL(bh, mref_II2IL_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefII_LL(Blackhole bh) {
        processLL(bh, mref_II2LL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndII_LL(Blackhole bh) {
        processLL(bh, mref_II2LL_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefIL_II(Blackhole bh) {
        processII(bh, mref_IL2II);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndIL_II(Blackhole bh) {
        processII(bh, mref_IL2II_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefIL_IL(Blackhole bh) {
        processIL(bh, mref_IL2IL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndIL_IL(Blackhole bh) {
        processIL(bh, mref_IL2IL_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefIL_LL(Blackhole bh) {
        processLL(bh, mref_IL2LL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndIL_LL(Blackhole bh) {
        processLL(bh, mref_IL2LL_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefLL_II(Blackhole bh) {
        processII(bh, mref_LL2II);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndLL_II(Blackhole bh) {
        processII(bh, mref_LL2II_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefLL_IL(Blackhole bh) {
        processIL(bh, mref_LL2IL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndLL_IL(Blackhole bh) {
        processIL(bh, mref_LL2IL_bound);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mrefLL_LL(Blackhole bh) {
        processLL(bh, mref_LL2LL);
    }

    @Benchmark
    @OperationsPerInvocation(LIMIT)
    public void mref_bndLL_LL(Blackhole bh) {
        processLL(bh, mref_LL2LL_bound);
    }


    private void processII(Blackhole bh, FunctionII func) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(func.foo(dataI[i]));
        }
    }

    private void processIL(Blackhole bh, FunctionIL func) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(func.foo(dataL[i]));
        }
    }

    private void processLL(Blackhole bh, FunctionLL func) {
        for (int i = 0; i < LIMIT; i++) {
            bh.consume(func.foo(dataL[i]));
        }
    }

}

