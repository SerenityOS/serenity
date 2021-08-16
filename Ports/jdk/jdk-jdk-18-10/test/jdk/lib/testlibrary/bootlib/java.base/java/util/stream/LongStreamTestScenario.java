/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.SpliteratorTestHelper;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * Test scenarios for long streams.
 *
 * Each scenario is provided with a data source, a function that maps a fresh
 * stream (as provided by the data source) to a new stream, and a sink to
 * receive results.  Each scenario describes a different way of computing the
 * stream contents.  The test driver will ensure that all scenarios produce
 * the same output (modulo allowable differences in ordering).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum LongStreamTestScenario implements OpTestCase.BaseStreamTestScenario {

    STREAM_FOR_EACH(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            LongStream s = m.apply(source);
            if (s.isParallel()) {
                s = s.sequential();
            }
            s.forEach(b);
        }
    },

    STREAM_TO_ARRAY(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            for (long t : m.apply(source).toArray()) {
                b.accept(t);
            }
        }
    },

    STREAM_ITERATOR(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            for (PrimitiveIterator.OfLong seqIter = m.apply(source).iterator(); seqIter.hasNext(); )
                b.accept(seqIter.nextLong());
        }
    },

    // Wrap as stream, and spliterate then iterate in pull mode
    STREAM_SPLITERATOR(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            for (Spliterator.OfLong spl = m.apply(source).spliterator(); spl.tryAdvance(b); ) {
            }
        }
    },

    // Wrap as stream, spliterate, then split a few times mixing advances with forEach
    STREAM_SPLITERATOR_WITH_MIXED_TRAVERSE_AND_SPLIT(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            SpliteratorTestHelper.mixedTraverseAndSplit(b, m.apply(source).spliterator());
        }
    },

    // Wrap as stream, and spliterate then iterate in pull mode
    STREAM_SPLITERATOR_FOREACH(false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            m.apply(source).spliterator().forEachRemaining(b);
        }
    },

    PAR_STREAM_SEQUENTIAL_FOR_EACH(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            m.apply(source).sequential().forEach(b);
        }
    },

    // Wrap as parallel stream + forEachOrdered
    PAR_STREAM_FOR_EACH_ORDERED(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            // @@@ Want to explicitly select ordered equalator
            m.apply(source).forEachOrdered(b);
        }
    },

    // Wrap as stream, and spliterate then iterate sequentially
    PAR_STREAM_SPLITERATOR(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            for (Spliterator.OfLong spl = m.apply(source).spliterator(); spl.tryAdvance(b); ) {
            }
        }
    },

    // Wrap as stream, and spliterate then iterate sequentially
    PAR_STREAM_SPLITERATOR_FOREACH(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            m.apply(source).spliterator().forEachRemaining(b);
        }
    },

    PAR_STREAM_TO_ARRAY(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            for (long t : m.apply(source).toArray())
                b.accept(t);
        }
    },

    // Wrap as parallel stream, get the spliterator, wrap as a stream + toArray
    PAR_STREAM_SPLITERATOR_STREAM_TO_ARRAY(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            LongStream s = m.apply(source);
            Spliterator.OfLong sp = s.spliterator();
            LongStream ss = StreamSupport.longStream(() -> sp,
                                                     StreamOpFlag.toCharacteristics(OpTestCase.getStreamFlags(s))
                                                     | (sp.getExactSizeIfKnown() < 0 ? 0 : Spliterator.SIZED), true);
            for (long t : ss.toArray())
                b.accept(t);
        }
    },

    PAR_STREAM_TO_ARRAY_CLEAR_SIZED(true) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            S_IN pipe1 = (S_IN) OpTestCase.chain(source,
                                                 new FlagDeclaringOp(StreamOpFlag.NOT_SIZED, data.getShape()));
            LongStream pipe2 = m.apply(pipe1);

            for (long t : pipe2.toArray())
                b.accept(t);
        }
    },

    // Wrap as parallel stream + forEach synchronizing
    PAR_STREAM_FOR_EACH(true, false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            m.apply(source).forEach(e -> {
                synchronized (data) {
                    b.accept(e);
                }
            });
        }
    },

    // Wrap as parallel stream + forEach synchronizing and clear SIZED flag
    PAR_STREAM_FOR_EACH_CLEAR_SIZED(true, false) {
        <T, S_IN extends BaseStream<T, S_IN>>
        void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m) {
            S_IN pipe1 = (S_IN) OpTestCase.chain(source,
                                                 new FlagDeclaringOp(StreamOpFlag.NOT_SIZED, data.getShape()));
            m.apply(pipe1).forEach(e -> {
                synchronized (data) {
                    b.accept(e);
                }
            });
        }
    },
    ;

    // The set of scenarios that clean the SIZED flag
    public static final Set<LongStreamTestScenario> CLEAR_SIZED_SCENARIOS = Collections.unmodifiableSet(
            EnumSet.of(PAR_STREAM_TO_ARRAY_CLEAR_SIZED, PAR_STREAM_FOR_EACH_CLEAR_SIZED));

    private boolean isParallel;

    private final boolean isOrdered;

    LongStreamTestScenario(boolean isParallel) {
        this(isParallel, true);
    }

    LongStreamTestScenario(boolean isParallel, boolean isOrdered) {
        this.isParallel = isParallel;
        this.isOrdered = isOrdered;
    }

    public StreamShape getShape() {
        return StreamShape.LONG_VALUE;
    }

    public boolean isParallel() {
        return isParallel;
    }

    public boolean isOrdered() {
        return isOrdered;
    }

    public <T, U, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
    void run(TestData<T, S_IN> data, Consumer<U> b, Function<S_IN, S_OUT> m) {
        try (S_IN source = getStream(data)) {
            run(data, source, (LongConsumer) b, (Function<S_IN, LongStream>) m);
        }
    }

    abstract <T, S_IN extends BaseStream<T, S_IN>>
    void run(TestData<T, S_IN> data, S_IN source, LongConsumer b, Function<S_IN, LongStream> m);

}
