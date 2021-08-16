/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(value = Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ThrowableRuntimeMicros {

    // TestStack will add this number of calls to the call stack
    @Param({"4", "100", "1000"})
    // For more thorough testing, consider:
    // @Param({"4", "10", "100", "256", "1000"})
    public int depth;

    /** Build a call stack of a given size, then run trigger code in it.
      * (Does not account for existing frames higher up in the JMH machinery).
      */
    static class TestStack {
        final long fence;
        long current;
        final Runnable trigger;

        TestStack(long max, Runnable trigger) {
            this.fence = max;
            this.current = 0;
            this.trigger = trigger;
        }

        public void start() {
            one();
        }

        public void one() {
            if (check()) {
                two();
            }
        }

        void two() {
            if (check()) {
                three();
            }
        }

        private void three() {
            if (check()) {
                one();
            }
        }

        boolean check() {
            if (++current == fence) {
                trigger.run();
                return false;
            } else {
                return true;
            }
        }
    }

    @Benchmark
    public void testThrowableInit(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                localBH.consume(new Throwable());
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    @Benchmark
    public void testThrowableGetStackTrace(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                localBH.consume(new Throwable().getStackTrace());
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    @Benchmark
    public void testThrowableSTEtoString(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                Throwable t = new Throwable();
                for (StackTraceElement ste : t.getStackTrace()) {
                    localBH.consume(ste.toString());
                }
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }
}
