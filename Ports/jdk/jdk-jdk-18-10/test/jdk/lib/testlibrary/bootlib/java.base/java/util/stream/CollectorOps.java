/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import org.testng.Assert;

import java.util.Spliterator;
import java.util.function.IntFunction;

/** Test helper class for java.util.stream test framework */
public final class CollectorOps {
    private CollectorOps() { }

    public static <E_IN> StatefulTestOp<E_IN> collector() {
        return new StatefulCollector<>(0, StreamShape.REFERENCE);
    }

    /* Utility classes for collecting output of intermediate pipeline stages */
    public static class StatefulCollector<E_IN> implements StatefulTestOp<E_IN> {
        private final int opFlags;
        private final StreamShape inputShape;

        public StatefulCollector(int opFlags, StreamShape inputShape) {
            this.opFlags = opFlags;
            this.inputShape = inputShape;
        }

        @Override
        public StreamShape inputShape() {
            return inputShape;
        }

        @Override
        public StreamShape outputShape() {
            return inputShape;
        }

        @Override
        public int opGetFlags() {
            return opFlags;
        }

        @Override
        public Sink<E_IN> opWrapSink(int flags, boolean parallel, Sink<E_IN> sink) {
            return sink;
        }

        @Override
        public <P_IN> Node<E_IN> opEvaluateParallel(PipelineHelper<E_IN> helper,
                                                    Spliterator<P_IN> spliterator,
                                                    IntFunction<E_IN[]> generator) {
            return helper.evaluate(spliterator, false, generator);
        }
    }

    public static class TestParallelSizedOp<T> extends StatefulCollector<T> {
        public TestParallelSizedOp() {
            this(StreamShape.REFERENCE);
        }

        protected TestParallelSizedOp(StreamShape shape) {
            super(0, shape);
        }

        @Override
        public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper,
                                                 Spliterator<P_IN> spliterator,
                                                 IntFunction<T[]> generator) {
            int flags = helper.getStreamAndOpFlags();

            Assert.assertTrue(StreamOpFlag.SIZED.isKnown(flags));
            return super.opEvaluateParallel(helper, spliterator, generator);
        }

        public static class OfInt extends TestParallelSizedOp<Integer> {
            public OfInt() {
                super(StreamShape.INT_VALUE);
            }
        }

        public static class OfLong extends TestParallelSizedOp<Long> {
            public OfLong() {
                super(StreamShape.LONG_VALUE);
            }
        }

        public static class OfDouble extends TestParallelSizedOp<Double> {
            public OfDouble() {
                super(StreamShape.DOUBLE_VALUE);
            }
        }
    }
}
