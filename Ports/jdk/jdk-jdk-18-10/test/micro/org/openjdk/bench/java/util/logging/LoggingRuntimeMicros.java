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
package org.openjdk.bench.java.util.logging;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogRecord;
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
public class LoggingRuntimeMicros {

    // TestStack will add this number of calls to the call stack
    @Param({"4", "100", "1000"})
    // For more thorough testing, consider:
    // @Param({"4", "10", "100", "256", "1000"})
    public int depth;

    /** Logging handler for testing logging calls. */
    @State(value = Scope.Thread) // create a separate one for each worker thread
    public static class TestHandler extends java.util.logging.Handler {
        private final static AtomicInteger serialNum = new AtomicInteger(0);

        private final java.util.logging.Logger logger;
        private volatile LogRecord record;

        public TestHandler() {
            // Each instance uses its own logger
            logger = java.util.logging.Logger.getLogger("StackWalkBench" + serialNum.incrementAndGet());
            logger.setUseParentHandlers(false);
            logger.addHandler(this);
        }

        @Override
        public void publish(LogRecord record) {
            record.getSourceMethodName();
            this.record = record;
        }

        private LogRecord reset() {
            LogRecord record = this.record;
            this.record = null;
            return record;
        }

        public final LogRecord testInferCaller(String msg) {
            logger.info(msg);
            LogRecord rec = this.reset();
            if (!"testInferCaller".equals(rec.getSourceMethodName())) {
                throw new RuntimeException("bad caller: "
                        + rec.getSourceClassName() + "."
                        + rec.getSourceMethodName());
            }
            return rec;
        }

        public final LogRecord testLogp(String msg) {
            logger.logp(java.util.logging.Level.INFO, "foo", "bar", msg);
            LogRecord rec = this.reset();
            if (!"bar".equals(rec.getSourceMethodName())) {
                throw new RuntimeException("bad caller: "
                        + rec.getSourceClassName() + "."
                        + rec.getSourceMethodName());
            }
            return rec;
        }
        @Override public void flush() {}
        @Override public void close() throws SecurityException {}
    }

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
    public void testLoggingInferCaller(TestHandler handler, Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                localBH.consume(handler.testInferCaller("test"));
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }

    @Benchmark
    public void testLoggingLogp(TestHandler handler, Blackhole bh) {
        final Blackhole localBH = bh;
        final boolean[] done = {false};
        new TestStack(depth, new Runnable() {
            public void run() {
                localBH.consume(handler.testLogp("test"));
                done[0] = true;
            }
        }).start();
        if (!done[0]) {
            throw new RuntimeException();
        }
    }
}
