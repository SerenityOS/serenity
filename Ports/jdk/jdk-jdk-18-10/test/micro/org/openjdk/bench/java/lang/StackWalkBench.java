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
package org.openjdk.bench.java.lang;

import java.lang.StackWalker.StackFrame;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for java.lang.StackWalker
 */
@State(value=Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class StackWalkBench {
    private static final StackWalker WALKER_DEFAULT = StackWalker.getInstance();

    private static final StackWalker WALKER_CLASS =
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // TestStack will add this number of calls to the call stack
    @Param({"4", "100", "1000"})
    // For more thorough testing, consider:
    // @Param({"4", "10", "100", "256", "1000"})
    public int depth;

    // Only used by swFilterCallerClass, to specify (roughly) how far back the
    // call stack the target class will be found.  Not needed by other
    // benchmarks, so not a @Param by default.
    // @Param({"4"})
    public int mark = 4;

    /** Build a call stack of a given size, then run trigger code in it.
      * (Does not account for existing frames higher up in the JMH machinery).
      */
    public static class TestStack {
        final long fence;
        long current;
        final Runnable trigger;

        public TestStack(long max, Runnable trigger) {
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

    /* Class to look for when testing filtering */
    static class TestMarker {
        public void call(MarkedTestStack test) {
            test.marked();
        }
    }

    /** Call stack to test filtering.
     *  TestMarker will make a call on the stack.
     */
    static class MarkedTestStack extends TestStack {
        long mark;

        /**
         * @param mark How far back the stack should the TestMarker be found?
         */
        public MarkedTestStack(long max, long mark, Runnable trigger) {
            super(max, trigger);
            if (mark > max) {
                throw new IllegalArgumentException("mark must be <= max");
            }
            this.mark = max - mark; // Count backwards from the completed call stack
        }
        @Override
        public void start() {
            if (mark == 0) {
                mark();
            } else {
                super.one();
            }
        }
        @Override
        boolean check() {
           if (++current == mark) {
               mark();
               return false;
           } else if (current == fence) {
              trigger.run();
              return false;
           } else {
               return true;
           }
        }
        void mark() {
            new TestMarker().call(this);
        }
        public void marked() {
            if (current < fence) {
                if (check()) {
                    one();
                }
            } else {
                trigger.run();
            }
        }
    }

    /**
     * StackWalker.forEach() with default options
     */
    @Benchmark
    public void forEach_DefaultOpts(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                WALKER_DEFAULT.forEach(localBH::consume);
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use Stackwalker.walk() to fetch class names
     */
    @Benchmark
    public void walk_ClassNames(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                WALKER_DEFAULT.walk(s -> {
                    s.map(StackFrame::getClassName).forEach(localBH::consume);
                    return null;
                });
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use Stackwalker.walk() to fetch method names
     */
    @Benchmark
    public void walk_MethodNames(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                WALKER_DEFAULT.walk( s -> {
                    s.map(StackFrame::getMethodName).forEach(localBH::consume);
                    return null;
                });
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use Stackwalker.walk() to fetch declaring class instances
     */
    @Benchmark
    public void walk_DeclaringClass(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                WALKER_CLASS.walk(s -> {
                    s.map(StackFrame::getDeclaringClass).forEach(localBH::consume);
                    return null;
                });
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use StackWalker.walk() to fetch StackTraceElements
     */
    @Benchmark
    public void walk_StackTraceElements(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                WALKER_DEFAULT.walk(s -> {
                    s.map(StackFrame::toStackTraceElement).forEach(localBH::consume);
                    return null;
                });
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * StackWalker.getCallerClass()
     */
    @Benchmark
    public void getCallerClass(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                localBH.consume(WALKER_CLASS.getCallerClass());
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use StackWalker.walk() to filter the StackFrames, looking for the
     * TestMarker class, which will be (approximately) 'mark' calls back up the
     * call stack.
     */
    @Benchmark
    public void walk_filterCallerClass(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};

        new MarkedTestStack(depth, mark, new Runnable() {
            public void run() {
                // To be comparable with Reflection.getCallerClass(), return the Class object
                WALKER_CLASS.walk(s -> {
                    localBH.consume(s.filter(f -> TestMarker.class.equals(f.getDeclaringClass())).findFirst().get().getDeclaringClass());
                    return null;
                });
                done[0] = true;
            }
        }).start();

        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    /**
     * Use StackWalker.walk() to filter the StackFrames, looking for the
     * TestMarker class, which will be (approximately) depth/2 calls back up the
     * call stack.
     */
    @Benchmark
    public void walk_filterCallerClassHalfStack(Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};

        new MarkedTestStack(depth, depth / 2, new Runnable() {
            public void run() {
                // To be comparable with Reflection.getCallerClass(), return the Class object
                WALKER_CLASS.walk(s -> {
                    localBH.consume(s.filter((f) -> TestMarker.class.equals(f.getDeclaringClass())).findFirst().get().getDeclaringClass());
                    return null;
                });
                done[0] = true;
            }
        }).start();

        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    // TODO: add swConsumeFramesWithReflection
    // TODO: add swFilterOutStreamClasses

//    // This benchmark is for collecting performance counter data
//    static PerfCounter streamTime = PerfCounter.newPerfCounter("jdk.stackwalk.testStreamsElapsedTime");
//    static PerfCounter  numStream = PerfCounter.newPerfCounter("jdk.stackwalk.numTestStreams");
//    // @Benchmark
//    public void swStkFrmsTimed(Blackhole bh) {
//        final Blackhole localBH = bh;
//        final boolean[] done = {false};
//        new TestStack(depth, new Runnable() {
//            public void run() {
//                long t0 = System.nanoTime();
//                WALKER_DEFAULT.forEach(localBH::consume);
//                streamTime.addElapsedTimeFrom(t0);
//                numStream.increment();
//                done[0] = true;
//            }
//        }).start();
//        if (!done[0]) {
//            throw new RuntimeException();
//        }
//    }
}
