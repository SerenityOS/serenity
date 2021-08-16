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
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;

/**
 * evaluates invocation costs in case of long recursive chains
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AckermannL {

    // ackermann(1,1748)+ ackermann(2,1897)+ ackermann(3,8); == 9999999 calls
    public static final int Y1 = 1748;
    public static final int Y2 = 1897;
    public static final int Y3 = 8;

    public static Integer ack(Integer x, Integer y) {
        return x == 0 ?
                y + 1 :
                (y == 0 ?
                        ack(x - 1, 1) :
                        ack(x - 1, ack(x, y - 1)));
    }

    @Benchmark
    @OperationsPerInvocation(9999999)
    public Integer func() {
        return ack(1, Y1) + ack(2, Y2) + ack(3, Y3);
    }

    public static final BinaryOperator<Integer> inner_ack =
            new BinaryOperator<Integer>() {
                @Override
                public Integer apply(Integer x, Integer y) {
                    return x == 0 ?
                            y + 1 :
                            (y == 0 ?
                                    inner_ack.apply(x - 1, 1) :
                                    inner_ack.apply(x - 1, inner_ack.apply(x, y - 1)));
                }
            };

    @Benchmark
    @OperationsPerInvocation(9999999)
    public Integer inner() {
        return inner_ack.apply(1, Y1) + inner_ack.apply(2, Y2) + inner_ack.apply(3, Y3);
    }

    public static final BinaryOperator<Integer> lambda_ack =
            (x, y) -> x == 0 ?
                    y + 1 :
                    (y == 0 ?
                            AckermannL.lambda_ack.apply(x - 1, 1) :
                            AckermannL.lambda_ack.apply(x - 1, AckermannL.lambda_ack.apply(x, y - 1)));


    @Benchmark
    @OperationsPerInvocation(9999999)
    public Integer lambda() {
        return lambda_ack.apply(1, Y1) + lambda_ack.apply(2, Y2) + lambda_ack.apply(3, Y3);
    }

    public static final BinaryOperator<Integer> mref_ack = AckermannL::mref_ack_helper;

    public static Integer mref_ack_helper(Integer x, Integer y) {
        return x == 0 ?
                y + 1 :
                (y == 0 ?
                        mref_ack.apply(x - 1, 1) :
                        mref_ack.apply(x - 1, mref_ack.apply(x, y - 1)));
    }

    @Benchmark
    @OperationsPerInvocation(9999999)
    public Integer mref() {
        return mref_ack.apply(1, Y1) + mref_ack.apply(2, Y2) + mref_ack.apply(3, Y3);
    }

    public static final BinaryOperator<Integer> mref_ackIII = AckermannL::mref_ack_helperIII;

    public static int mref_ack_helperIII(int x, int y) {
        return x == 0 ?
                y + 1 :
                (y == 0 ?
                        mref_ackIII.apply(x - 1, 1) :
                        mref_ackIII.apply(x - 1, mref_ackIII.apply(x, y - 1)));
    }

    @Benchmark
    @OperationsPerInvocation(9999999)
    public Integer mrefIII() {
        return mref_ackIII.apply(1, Y1) + mref_ackIII.apply(2, Y2) + mref_ackIII.apply(3, Y3);
    }

}

