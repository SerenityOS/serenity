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

import java.util.concurrent.TimeUnit;
import java.util.function.IntUnaryOperator;

/**
 * evaluates N-morphic invocation costs.
 * N different lambdas each capture 0 variable
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Morph0 {


    private static final int LIMIT = 16536;
    private static final int OPS = 4;
    private static final int OPERATIONS = OPS*LIMIT;

    // <source of functional interface>_N; where N - how many different targets
    private IntUnaryOperator[] inner_1;
    private IntUnaryOperator[] inner_2;
    private IntUnaryOperator[] inner_4;

    private IntUnaryOperator[] lambda_1;
    private IntUnaryOperator[] lambda_2;
    private IntUnaryOperator[] lambda_4;

    private IntUnaryOperator[] unbounded_mref_1;
    private IntUnaryOperator[] unbounded_mref_2;
    private IntUnaryOperator[] unbounded_mref_4;

    private IntUnaryOperator[] bounded_mref_1;
    private IntUnaryOperator[] bounded_mref_2;
    private IntUnaryOperator[] bounded_mref_4;

    @Setup(Level.Trial)
    public void setup() {
        setup_inner();
        setup_lambda();
        setup_unbounded_mref();
        setup_bounded_mref();
    }

    private void setup_inner() {
        inner_4 =  new IntUnaryOperator[] {
            new IntUnaryOperator() {
                @Override
                public int applyAsInt(int x) {
                    return x + 1;
                }
            },
            new IntUnaryOperator() {
                @Override
                public int applyAsInt(int x) {
                    return x + 2;
                }
            },
            new IntUnaryOperator() {
                @Override
                public int applyAsInt(int x) {
                    return x + 3;
                }
            },
            new IntUnaryOperator() {
                @Override
                public int applyAsInt(int x) {
                    return x + 4;
                }
            },
        };
        inner_2 =  new IntUnaryOperator[] { inner_4[0], inner_4[1], inner_4[0], inner_4[1], };
        inner_1 =  new IntUnaryOperator[] { inner_4[0], inner_4[0], inner_4[0], inner_4[0], };
    }

    private void setup_lambda() {
        lambda_4 =  new IntUnaryOperator[] {
                x -> x + 1,
                x -> x + 2,
                x -> x + 3,
                x -> x + 4,
        };
        lambda_2 =  new IntUnaryOperator[] { lambda_4[0], lambda_4[1], lambda_4[0], lambda_4[1], };
        lambda_1 =  new IntUnaryOperator[] { lambda_4[0], lambda_4[0], lambda_4[0], lambda_4[0], };
    }

    public static int func1(int x) {
        return x + 1;
    }

    public static int func2(int x) {
        return x + 2;
    }

    public static int func3(int x) {
        return x + 3;
    }

    public static int func4(int x) {
        return x + 4;
    }

    private void setup_unbounded_mref() {
        unbounded_mref_4 =  new IntUnaryOperator[] {
                Morph0::func1,
                Morph0::func2,
                Morph0::func3,
                Morph0::func4,
        };
        unbounded_mref_2 =  new IntUnaryOperator[] { unbounded_mref_4[0], unbounded_mref_4[1], unbounded_mref_4[0], unbounded_mref_4[1], };
        unbounded_mref_1 =  new IntUnaryOperator[] { unbounded_mref_4[0], unbounded_mref_4[0], unbounded_mref_4[0], unbounded_mref_4[0], };
    }

    public int ifunc1(int x) {
        return x + 1;
    }

    public int ifunc2(int x) {
        return x + 2;
    }

    public int ifunc3(int x) {
        return x + 3;
    }

    public int ifunc4(int x) {
        return x + 4;
    }

    private void setup_bounded_mref() {
        bounded_mref_4 =  new IntUnaryOperator[] {
                this::ifunc1,
                this::ifunc2,
                this::ifunc3,
                this::ifunc4,
        };
        bounded_mref_2 =  new IntUnaryOperator[] { bounded_mref_4[0], bounded_mref_4[1], bounded_mref_4[0], bounded_mref_4[1], };
        bounded_mref_1 =  new IntUnaryOperator[] { bounded_mref_4[0], bounded_mref_4[0], bounded_mref_4[0], bounded_mref_4[0], };
    }

    public void process(Blackhole bh, IntUnaryOperator[] operations) {
        for (int i = 0; i < LIMIT; i++) {
            for (IntUnaryOperator op : operations) {
                bh.consume(op.applyAsInt(i));
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void inner1(Blackhole bh) {
        process(bh, inner_1);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void inner2(Blackhole bh) {
        process(bh, inner_2);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void inner4(Blackhole bh) {
        process(bh, inner_4);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void lambda1(Blackhole bh) {
        process(bh, lambda_1);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void lambda2(Blackhole bh) {
        process(bh, lambda_2);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void lambda4(Blackhole bh) {
        process(bh, lambda_4);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref1(Blackhole bh) {
        process(bh, unbounded_mref_1);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref2(Blackhole bh) {
        process(bh, unbounded_mref_2);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref4(Blackhole bh) {
        process(bh, unbounded_mref_4);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref_bnd1(Blackhole bh) {
        process(bh, bounded_mref_1);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref_bnd2(Blackhole bh) {
        process(bh, bounded_mref_2);
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mref_bnd4(Blackhole bh) {
        process(bh, bounded_mref_4);
    }

}

