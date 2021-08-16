/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Factory for instances of a takeWhile and dropWhile operations
 * that produce subsequences of their input stream.
 *
 * @since 9
 */
final class WhileOps {

    static final int TAKE_FLAGS = StreamOpFlag.NOT_SIZED | StreamOpFlag.IS_SHORT_CIRCUIT;

    static final int DROP_FLAGS = StreamOpFlag.NOT_SIZED;

    /**
     * Appends a "takeWhile" operation to the provided Stream.
     *
     * @param <T> the type of both input and output elements
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    static <T> Stream<T> makeTakeWhileRef(AbstractPipeline<?, T, ?> upstream,
                                          Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new ReferencePipeline.StatefulOp<T, T>(upstream, StreamShape.REFERENCE, TAKE_FLAGS) {
            @Override
            <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper,
                                                         Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Nodes.castingArray())
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfRef.Taking<>(
                            helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper,
                                              Spliterator<P_IN> spliterator,
                                              IntFunction<T[]> generator) {
                return new TakeWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<T> opWrapSink(int flags, Sink<T> sink) {
                return new Sink.ChainedReference<T, T>(sink) {
                    boolean take = true;

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(T t) {
                        if (take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "takeWhile" operation to the provided IntStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    static IntStream makeTakeWhileInt(AbstractPipeline<?, Integer, ?> upstream,
                                      IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new IntPipeline.StatefulOp<Integer>(upstream, StreamShape.INT_VALUE, TAKE_FLAGS) {
            @Override
            <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper,
                                                               Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Integer[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfInt.Taking(
                            (Spliterator.OfInt) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper,
                                                    Spliterator<P_IN> spliterator,
                                                    IntFunction<Integer[]> generator) {
                return new TakeWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    boolean take = true;

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(int t) {
                        if (take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "takeWhile" operation to the provided LongStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    static LongStream makeTakeWhileLong(AbstractPipeline<?, Long, ?> upstream,
                                        LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new LongPipeline.StatefulOp<Long>(upstream, StreamShape.LONG_VALUE, TAKE_FLAGS) {
            @Override
            <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper,
                                                            Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Long[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfLong.Taking(
                            (Spliterator.OfLong) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper,
                                                 Spliterator<P_IN> spliterator,
                                                 IntFunction<Long[]> generator) {
                return new TakeWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    boolean take = true;

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(long t) {
                        if (take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "takeWhile" operation to the provided DoubleStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    static DoubleStream makeTakeWhileDouble(AbstractPipeline<?, Double, ?> upstream,
                                            DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new DoublePipeline.StatefulOp<Double>(upstream, StreamShape.DOUBLE_VALUE, TAKE_FLAGS) {
            @Override
            <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper,
                                                              Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Double[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfDouble.Taking(
                            (Spliterator.OfDouble) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper,
                                                   Spliterator<P_IN> spliterator,
                                                   IntFunction<Double[]> generator) {
                return new TakeWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    boolean take = true;

                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(double t) {
                        if (take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * A specialization for the dropWhile operation that controls if
     * elements to be dropped are counted and passed downstream.
     * <p>
     * This specialization is utilized by the {@link TakeWhileTask} for
     * pipelines that are ordered.  In such cases elements cannot be dropped
     * until all elements have been collected.
     *
     * @param <T> the type of both input and output elements
     */
    interface DropWhileOp<T> {
        /**
         * Accepts a {@code Sink} which will receive the results of this
         * dropWhile operation, and return a {@code DropWhileSink} which
         * accepts
         * elements and which performs the dropWhile operation passing the
         * results to the provided {@code Sink}.
         *
         * @param sink sink to which elements should be sent after processing
         * @param retainAndCountDroppedElements true if elements to be dropped
         * are counted and passed to the sink, otherwise such elements
         * are actually dropped and not passed to the sink.
         * @return a dropWhile sink
         */
        DropWhileSink<T> opWrapSink(Sink<T> sink, boolean retainAndCountDroppedElements);
    }

    /**
     * A specialization for a dropWhile sink.
     *
     * @param <T> the type of both input and output elements
     */
    interface DropWhileSink<T> extends Sink<T> {
        /**
         * @return the could of elements that would have been dropped and
         * instead were passed downstream.
         */
        long getDropCount();
    }

    /**
     * Appends a "dropWhile" operation to the provided Stream.
     *
     * @param <T> the type of both input and output elements
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    static <T> Stream<T> makeDropWhileRef(AbstractPipeline<?, T, ?> upstream,
                                          Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);

        class Op extends ReferencePipeline.StatefulOp<T, T> implements DropWhileOp<T> {
            public Op(AbstractPipeline<?, T, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }

            @Override
            <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper,
                                                         Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Nodes.castingArray())
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfRef.Dropping<>(
                            helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper,
                                              Spliterator<P_IN> spliterator,
                                              IntFunction<T[]> generator) {
                return new DropWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<T> opWrapSink(int flags, Sink<T> sink) {
                return opWrapSink(sink, false);
            }

            public DropWhileSink<T> opWrapSink(Sink<T> sink, boolean retainAndCountDroppedElements) {
                class OpSink extends Sink.ChainedReference<T, T> implements DropWhileSink<T> {
                    long dropCount;
                    boolean take;

                    OpSink() {
                        super(sink);
                    }

                    @Override
                    public void accept(T t) {
                        boolean takeElement = take || (take = !predicate.test(t));

                        // If ordered and element is dropped increment index
                        // for possible future truncation
                        if (retainAndCountDroppedElements && !takeElement)
                            dropCount++;

                        // If ordered need to process element, otherwise
                        // skip if element is dropped
                        if (retainAndCountDroppedElements || takeElement)
                            downstream.accept(t);
                    }

                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                return new OpSink();
            }
        }
        return new Op(upstream, StreamShape.REFERENCE, DROP_FLAGS);
    }

    /**
     * Appends a "dropWhile" operation to the provided IntStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    static IntStream makeDropWhileInt(AbstractPipeline<?, Integer, ?> upstream,
                                      IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        class Op extends IntPipeline.StatefulOp<Integer> implements DropWhileOp<Integer> {
            public Op(AbstractPipeline<?, Integer, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }

            @Override
            <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper,
                                                               Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Integer[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfInt.Dropping(
                            (Spliterator.OfInt) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper,
                                                    Spliterator<P_IN> spliterator,
                                                    IntFunction<Integer[]> generator) {
                return new DropWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return opWrapSink(sink, false);
            }

            public DropWhileSink<Integer> opWrapSink(Sink<Integer> sink, boolean retainAndCountDroppedElements) {
                class OpSink extends Sink.ChainedInt<Integer> implements DropWhileSink<Integer> {
                    long dropCount;
                    boolean take;

                    OpSink() {
                        super(sink);
                    }

                    @Override
                    public void accept(int t) {
                        boolean takeElement = take || (take = !predicate.test(t));

                        // If ordered and element is dropped increment index
                        // for possible future truncation
                        if (retainAndCountDroppedElements && !takeElement)
                            dropCount++;

                        // If ordered need to process element, otherwise
                        // skip if element is dropped
                        if (retainAndCountDroppedElements || takeElement)
                            downstream.accept(t);
                    }

                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                return new OpSink();
            }
        }
        return new Op(upstream, StreamShape.INT_VALUE, DROP_FLAGS);
    }

    /**
     * Appends a "dropWhile" operation to the provided LongStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    static LongStream makeDropWhileLong(AbstractPipeline<?, Long, ?> upstream,
                                        LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        class Op extends LongPipeline.StatefulOp<Long> implements DropWhileOp<Long> {
            public Op(AbstractPipeline<?, Long, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }

            @Override
            <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper,
                                                            Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Long[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfLong.Dropping(
                            (Spliterator.OfLong) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper,
                                                 Spliterator<P_IN> spliterator,
                                                 IntFunction<Long[]> generator) {
                return new DropWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return opWrapSink(sink, false);
            }

            public DropWhileSink<Long> opWrapSink(Sink<Long> sink, boolean retainAndCountDroppedElements) {
                class OpSink extends Sink.ChainedLong<Long> implements DropWhileSink<Long> {
                    long dropCount;
                    boolean take;

                    OpSink() {
                        super(sink);
                    }

                    @Override
                    public void accept(long t) {
                        boolean takeElement = take || (take = !predicate.test(t));

                        // If ordered and element is dropped increment index
                        // for possible future truncation
                        if (retainAndCountDroppedElements && !takeElement)
                            dropCount++;

                        // If ordered need to process element, otherwise
                        // skip if element is dropped
                        if (retainAndCountDroppedElements || takeElement)
                            downstream.accept(t);
                    }

                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                return new OpSink();
            }
        }
        return new Op(upstream, StreamShape.LONG_VALUE, DROP_FLAGS);
    }

    /**
     * Appends a "dropWhile" operation to the provided DoubleStream.
     *
     * @param upstream a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    static DoubleStream makeDropWhileDouble(AbstractPipeline<?, Double, ?> upstream,
                                            DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        class Op extends DoublePipeline.StatefulOp<Double> implements DropWhileOp<Double> {
            public Op(AbstractPipeline<?, Double, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }

            @Override
            <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper,
                                                              Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return opEvaluateParallel(helper, spliterator, Double[]::new)
                            .spliterator();
                }
                else {
                    return new UnorderedWhileSpliterator.OfDouble.Dropping(
                            (Spliterator.OfDouble) helper.wrapSpliterator(spliterator), false, predicate);
                }
            }

            @Override
            <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper,
                                                   Spliterator<P_IN> spliterator,
                                                   IntFunction<Double[]> generator) {
                return new DropWhileTask<>(this, helper, spliterator, generator)
                        .invoke();
            }

            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return opWrapSink(sink, false);
            }

            public DropWhileSink<Double> opWrapSink(Sink<Double> sink, boolean retainAndCountDroppedElements) {
                class OpSink extends Sink.ChainedDouble<Double> implements DropWhileSink<Double> {
                    long dropCount;
                    boolean take;

                    OpSink() {
                        super(sink);
                    }

                    @Override
                    public void accept(double t) {
                        boolean takeElement = take || (take = !predicate.test(t));

                        // If ordered and element is dropped increment index
                        // for possible future truncation
                        if (retainAndCountDroppedElements && !takeElement)
                            dropCount++;

                        // If ordered need to process element, otherwise
                        // skip if element is dropped
                        if (retainAndCountDroppedElements || takeElement)
                            downstream.accept(t);
                    }

                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                return new OpSink();
            }
        }
        return new Op(upstream, StreamShape.DOUBLE_VALUE, DROP_FLAGS);
    }

    //

    /**
     * A spliterator supporting takeWhile and dropWhile operations over an
     * underlying spliterator whose covered elements have no encounter order.
     * <p>
     * Concrete subclasses of this spliterator support reference and primitive
     * types for takeWhile and dropWhile.
     * <p>
     * For the takeWhile operation if during traversal taking completes then
     * taking is cancelled globally for the splitting and traversal of all
     * related spliterators.
     * Cancellation is governed by a shared {@link AtomicBoolean} instance.  A
     * spliterator in the process of taking when cancellation occurs will also
     * be cancelled but not necessarily immediately.  To reduce contention on
     * the {@link AtomicBoolean} instance, cancellation make be acted on after
     * a small number of additional elements have been traversed.
     * <p>
     * For the dropWhile operation if during traversal dropping completes for
     * some, but not all elements, then it is cancelled globally for the
     * traversal of all related spliterators (splitting is not cancelled).
     * Cancellation is governed in the same manner as for the takeWhile
     * operation.
     *
     * @param <T> the type of elements returned by this spliterator
     * @param <T_SPLITR> the type of the spliterator
     */
    abstract static class UnorderedWhileSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        // Power of two constant minus one used for modulus of count
        static final int CANCEL_CHECK_COUNT = (1 << 6) - 1;

        // The underlying spliterator
        final T_SPLITR s;
        // True if no splitting should be performed, if true then
        // this spliterator may be used for an underlying spliterator whose
        // covered elements have an encounter order
        // See use in stream take/dropWhile default methods
        final boolean noSplitting;
        // True when operations are cancelled for all related spliterators
        // For taking, spliterators cannot split or traversed
        // For dropping, spliterators cannot be traversed
        final AtomicBoolean cancel;
        // True while taking or dropping should be performed when traversing
        boolean takeOrDrop = true;
        // The count of elements traversed
        int count;

        UnorderedWhileSpliterator(T_SPLITR s, boolean noSplitting) {
            this.s = s;
            this.noSplitting = noSplitting;
            this.cancel = new AtomicBoolean();
        }

        UnorderedWhileSpliterator(T_SPLITR s, UnorderedWhileSpliterator<T, T_SPLITR> parent) {
            this.s = s;
            this.noSplitting = parent.noSplitting;
            this.cancel = parent.cancel;
        }

        @Override
        public long estimateSize() {
            return s.estimateSize();
        }

        @Override
        public int characteristics() {
            // Size is not known
            return s.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        @Override
        public long getExactSizeIfKnown() {
            return -1L;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return s.getComparator();
        }

        @Override
        public T_SPLITR trySplit() {
            @SuppressWarnings("unchecked")
            T_SPLITR ls = noSplitting ? null : (T_SPLITR) s.trySplit();
            return ls != null ? makeSpliterator(ls) : null;
        }

        boolean checkCancelOnCount() {
            return count != 0 || !cancel.get();
        }

        abstract T_SPLITR makeSpliterator(T_SPLITR s);

        abstract static class OfRef<T> extends UnorderedWhileSpliterator<T, Spliterator<T>> implements Consumer<T> {
            final Predicate<? super T> p;
            T t;

            OfRef(Spliterator<T> s, boolean noSplitting, Predicate<? super T> p) {
                super(s, noSplitting);
                this.p = p;
            }

            OfRef(Spliterator<T> s, OfRef<T> parent) {
                super(s, parent);
                this.p = parent.p;
            }

            @Override
            public void accept(T t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }

            static final class Taking<T> extends OfRef<T> {
                Taking(Spliterator<T> s, boolean noSplitting, Predicate<? super T> p) {
                    super(s, noSplitting, p);
                }

                Taking(Spliterator<T> s, Taking<T> parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    boolean test = true;
                    if (takeOrDrop &&               // If can take
                        checkCancelOnCount() && // and if not cancelled
                        s.tryAdvance(this) &&   // and if advanced one element
                        (test = p.test(t))) {   // and test on element passes
                        action.accept(t);           // then accept element
                        return true;
                    }
                    else {
                        // Taking is finished
                        takeOrDrop = false;
                        // Cancel all further traversal and splitting operations
                        // only if test of element failed (short-circuited)
                        if (!test)
                            cancel.set(true);
                        return false;
                    }
                }

                @Override
                public Spliterator<T> trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }

                @Override
                Spliterator<T> makeSpliterator(Spliterator<T> s) {
                    return new Taking<>(s, this);
                }
            }

            static final class Dropping<T> extends OfRef<T> {
                Dropping(Spliterator<T> s, boolean noSplitting, Predicate<? super T> p) {
                    super(s, noSplitting, p);
                }

                Dropping(Spliterator<T> s, Dropping<T> parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    if (takeOrDrop) {
                        takeOrDrop = false;
                        boolean adv;
                        boolean dropped = false;
                        while ((adv = s.tryAdvance(this)) &&  // If advanced one element
                               checkCancelOnCount() &&        // and if not cancelled
                               p.test(t)) {                   // and test on element passes
                            dropped = true;                   // then drop element
                        }

                        // Report advanced element, if any
                        if (adv) {
                            // Cancel all further dropping if one or more elements
                            // were previously dropped
                            if (dropped)
                                cancel.set(true);
                            action.accept(t);
                        }
                        return adv;
                    }
                    else {
                        return s.tryAdvance(action);
                    }
                }

                @Override
                Spliterator<T> makeSpliterator(Spliterator<T> s) {
                    return new Dropping<>(s, this);
                }
            }
        }

        abstract static class OfInt extends UnorderedWhileSpliterator<Integer, Spliterator.OfInt> implements IntConsumer, Spliterator.OfInt {
            final IntPredicate p;
            int t;

            OfInt(Spliterator.OfInt s, boolean noSplitting, IntPredicate p) {
                super(s, noSplitting);
                this.p = p;
            }

            OfInt(Spliterator.OfInt s, UnorderedWhileSpliterator.OfInt parent) {
                super(s, parent);
                this.p = parent.p;
            }

            @Override
            public void accept(int t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }

            static final class Taking extends UnorderedWhileSpliterator.OfInt {
                Taking(Spliterator.OfInt s, boolean noSplitting, IntPredicate p) {
                    super(s, noSplitting, p);
                }

                Taking(Spliterator.OfInt s, UnorderedWhileSpliterator.OfInt parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(IntConsumer action) {
                    boolean test = true;
                    if (takeOrDrop &&               // If can take
                        checkCancelOnCount() && // and if not cancelled
                        s.tryAdvance(this) &&   // and if advanced one element
                        (test = p.test(t))) {   // and test on element passes
                        action.accept(t);           // then accept element
                        return true;
                    }
                    else {
                        // Taking is finished
                        takeOrDrop = false;
                        // Cancel all further traversal and splitting operations
                        // only if test of element failed (short-circuited)
                        if (!test)
                            cancel.set(true);
                        return false;
                    }
                }

                @Override
                public Spliterator.OfInt trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }

                @Override
                Spliterator.OfInt makeSpliterator(Spliterator.OfInt s) {
                    return new Taking(s, this);
                }
            }

            static final class Dropping extends UnorderedWhileSpliterator.OfInt {
                Dropping(Spliterator.OfInt s, boolean noSplitting, IntPredicate p) {
                    super(s, noSplitting, p);
                }

                Dropping(Spliterator.OfInt s, UnorderedWhileSpliterator.OfInt parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(IntConsumer action) {
                    if (takeOrDrop) {
                        takeOrDrop = false;
                        boolean adv;
                        boolean dropped = false;
                        while ((adv = s.tryAdvance(this)) &&  // If advanced one element
                               checkCancelOnCount() &&        // and if not cancelled
                               p.test(t)) {                   // and test on element passes
                            dropped = true;                   // then drop element
                        }

                        // Report advanced element, if any
                        if (adv) {
                            // Cancel all further dropping if one or more elements
                            // were previously dropped
                            if (dropped)
                                cancel.set(true);
                            action.accept(t);
                        }
                        return adv;
                    }
                    else {
                        return s.tryAdvance(action);
                    }
                }

                @Override
                Spliterator.OfInt makeSpliterator(Spliterator.OfInt s) {
                    return new Dropping(s, this);
                }
            }
        }

        abstract static class OfLong extends UnorderedWhileSpliterator<Long, Spliterator.OfLong> implements LongConsumer, Spliterator.OfLong {
            final LongPredicate p;
            long t;

            OfLong(Spliterator.OfLong s, boolean noSplitting, LongPredicate p) {
                super(s, noSplitting);
                this.p = p;
            }

            OfLong(Spliterator.OfLong s, UnorderedWhileSpliterator.OfLong parent) {
                super(s, parent);
                this.p = parent.p;
            }

            @Override
            public void accept(long t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }

            static final class Taking extends UnorderedWhileSpliterator.OfLong {
                Taking(Spliterator.OfLong s, boolean noSplitting, LongPredicate p) {
                    super(s, noSplitting, p);
                }

                Taking(Spliterator.OfLong s, UnorderedWhileSpliterator.OfLong parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(LongConsumer action) {
                    boolean test = true;
                    if (takeOrDrop &&               // If can take
                        checkCancelOnCount() && // and if not cancelled
                        s.tryAdvance(this) &&   // and if advanced one element
                        (test = p.test(t))) {   // and test on element passes
                        action.accept(t);           // then accept element
                        return true;
                    }
                    else {
                        // Taking is finished
                        takeOrDrop = false;
                        // Cancel all further traversal and splitting operations
                        // only if test of element failed (short-circuited)
                        if (!test)
                            cancel.set(true);
                        return false;
                    }
                }

                @Override
                public Spliterator.OfLong trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }

                @Override
                Spliterator.OfLong makeSpliterator(Spliterator.OfLong s) {
                    return new Taking(s, this);
                }
            }

            static final class Dropping extends UnorderedWhileSpliterator.OfLong {
                Dropping(Spliterator.OfLong s, boolean noSplitting, LongPredicate p) {
                    super(s, noSplitting, p);
                }

                Dropping(Spliterator.OfLong s, UnorderedWhileSpliterator.OfLong parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(LongConsumer action) {
                    if (takeOrDrop) {
                        takeOrDrop = false;
                        boolean adv;
                        boolean dropped = false;
                        while ((adv = s.tryAdvance(this)) &&  // If advanced one element
                               checkCancelOnCount() &&        // and if not cancelled
                               p.test(t)) {                   // and test on element passes
                            dropped = true;                   // then drop element
                        }

                        // Report advanced element, if any
                        if (adv) {
                            // Cancel all further dropping if one or more elements
                            // were previously dropped
                            if (dropped)
                                cancel.set(true);
                            action.accept(t);
                        }
                        return adv;
                    }
                    else {
                        return s.tryAdvance(action);
                    }
                }

                @Override
                Spliterator.OfLong makeSpliterator(Spliterator.OfLong s) {
                    return new Dropping(s, this);
                }
            }
        }

        abstract static class OfDouble extends UnorderedWhileSpliterator<Double, Spliterator.OfDouble> implements DoubleConsumer, Spliterator.OfDouble {
            final DoublePredicate p;
            double t;

            OfDouble(Spliterator.OfDouble s, boolean noSplitting, DoublePredicate p) {
                super(s, noSplitting);
                this.p = p;
            }

            OfDouble(Spliterator.OfDouble s, UnorderedWhileSpliterator.OfDouble parent) {
                super(s, parent);
                this.p = parent.p;
            }

            @Override
            public void accept(double t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }

            static final class Taking extends UnorderedWhileSpliterator.OfDouble {
                Taking(Spliterator.OfDouble s, boolean noSplitting, DoublePredicate p) {
                    super(s, noSplitting, p);
                }

                Taking(Spliterator.OfDouble s, UnorderedWhileSpliterator.OfDouble parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(DoubleConsumer action) {
                    boolean test = true;
                    if (takeOrDrop &&               // If can take
                        checkCancelOnCount() && // and if not cancelled
                        s.tryAdvance(this) &&   // and if advanced one element
                        (test = p.test(t))) {   // and test on element passes
                        action.accept(t);           // then accept element
                        return true;
                    }
                    else {
                        // Taking is finished
                        takeOrDrop = false;
                        // Cancel all further traversal and splitting operations
                        // only if test of element failed (short-circuited)
                        if (!test)
                            cancel.set(true);
                        return false;
                    }
                }

                @Override
                public Spliterator.OfDouble trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }

                @Override
                Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble s) {
                    return new Taking(s, this);
                }
            }

            static final class Dropping extends UnorderedWhileSpliterator.OfDouble {
                Dropping(Spliterator.OfDouble s, boolean noSplitting, DoublePredicate p) {
                    super(s, noSplitting, p);
                }

                Dropping(Spliterator.OfDouble s, UnorderedWhileSpliterator.OfDouble parent) {
                    super(s, parent);
                }

                @Override
                public boolean tryAdvance(DoubleConsumer action) {
                    if (takeOrDrop) {
                        takeOrDrop = false;
                        boolean adv;
                        boolean dropped = false;
                        while ((adv = s.tryAdvance(this)) &&  // If advanced one element
                               checkCancelOnCount() &&        // and if not cancelled
                               p.test(t)) {                   // and test on element passes
                            dropped = true;                   // then drop element
                        }

                        // Report advanced element, if any
                        if (adv) {
                            // Cancel all further dropping if one or more elements
                            // were previously dropped
                            if (dropped)
                                cancel.set(true);
                            action.accept(t);
                        }
                        return adv;
                    }
                    else {
                        return s.tryAdvance(action);
                    }
                }

                @Override
                Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble s) {
                    return new Dropping(s, this);
                }
            }
        }
    }


    //

    /**
     * {@code ForkJoinTask} implementing takeWhile computation.
     * <p>
     * If the pipeline has encounter order then all tasks to the right of
     * a task where traversal was short-circuited are cancelled.
     * The results of completed (and cancelled) tasks are discarded.
     * The result of merging a short-circuited left task and right task (which
     * may or may not be short-circuited) is that left task.
     * <p>
     * If the pipeline has no encounter order then all tasks to the right of
     * a task where traversal was short-circuited are cancelled.
     * The results of completed (and possibly cancelled) tasks are not
     * discarded, as there is no need to throw away computed results.
     * The result of merging does not change if a left task was
     * short-circuited.
     * No attempt is made, once a leaf task stopped taking, for it to cancel
     * all other tasks, and further more, short-circuit the computation with its
     * result.
     *
     * @param <P_IN> Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    @SuppressWarnings("serial")
    private static final class TakeWhileTask<P_IN, P_OUT>
            extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, TakeWhileTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> op;
        private final IntFunction<P_OUT[]> generator;
        private final boolean isOrdered;
        private long thisNodeSize;
        // True if a short-circuited
        private boolean shortCircuited;
        // True if completed, must be set after the local result
        private volatile boolean completed;

        TakeWhileTask(AbstractPipeline<P_OUT, P_OUT, ?> op,
                      PipelineHelper<P_OUT> helper,
                      Spliterator<P_IN> spliterator,
                      IntFunction<P_OUT[]> generator) {
            super(helper, spliterator);
            this.op = op;
            this.generator = generator;
            this.isOrdered = StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags());
        }

        TakeWhileTask(TakeWhileTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
            this.generator = parent.generator;
            this.isOrdered = parent.isOrdered;
        }

        @Override
        protected TakeWhileTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new TakeWhileTask<>(this, spliterator);
        }

        @Override
        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(op.getOutputShape());
        }

        @Override
        protected final Node<P_OUT> doLeaf() {
            Node.Builder<P_OUT> builder = helper.makeNodeBuilder(-1, generator);
            Sink<P_OUT> s = op.opWrapSink(helper.getStreamAndOpFlags(), builder);

            if (shortCircuited = helper.copyIntoWithCancel(helper.wrapSink(s), spliterator)) {
                // Cancel later nodes if the predicate returned false
                // during traversal
                cancelLaterNodes();
            }

            Node<P_OUT> node = builder.build();
            thisNodeSize = node.count();
            return node;
        }

        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            if (!isLeaf()) {
                Node<P_OUT> result;
                shortCircuited = leftChild.shortCircuited | rightChild.shortCircuited;
                if (isOrdered && canceled) {
                    thisNodeSize = 0;
                    result = getEmptyResult();
                }
                else if (isOrdered && leftChild.shortCircuited) {
                    // If taking finished on the left node then
                    // use the left node result
                    thisNodeSize = leftChild.thisNodeSize;
                    result = leftChild.getLocalResult();
                }
                else {
                    thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                    result = merge();
                }

                setLocalResult(result);
            }

            completed = true;
            super.onCompletion(caller);
        }

        Node<P_OUT> merge() {
            if (leftChild.thisNodeSize == 0) {
                // If the left node size is 0 then
                // use the right node result
                return rightChild.getLocalResult();
            }
            else if (rightChild.thisNodeSize == 0) {
                // If the right node size is 0 then
                // use the left node result
                return leftChild.getLocalResult();
            }
            else {
                // Combine the left and right nodes
                return Nodes.conc(op.getOutputShape(),
                                  leftChild.getLocalResult(), rightChild.getLocalResult());
            }
        }

        @Override
        protected void cancel() {
            super.cancel();
            if (isOrdered && completed)
                // If the task is completed then clear the result, if any
                // to aid GC
                setLocalResult(getEmptyResult());
        }
    }

    /**
     * {@code ForkJoinTask} implementing dropWhile computation.
     * <p>
     * If the pipeline has encounter order then each leaf task will not
     * drop elements but will obtain a count of the elements that would have
     * been otherwise dropped. That count is used as an index to track
     * elements to be dropped. Merging will update the index so it corresponds
     * to the index that is the end of the global prefix of elements to be
     * dropped. The root is truncated according to that index.
     * <p>
     * If the pipeline has no encounter order then each leaf task will drop
     * elements. Leaf tasks are ordinarily merged. No truncation of the root
     * node is required.
     * No attempt is made, once a leaf task stopped dropping, for it to cancel
     * all other tasks, and further more, short-circuit the computation with
     * its result.
     *
     * @param <P_IN> Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    @SuppressWarnings("serial")
    private static final class DropWhileTask<P_IN, P_OUT>
            extends AbstractTask<P_IN, P_OUT, Node<P_OUT>, DropWhileTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> op;
        private final IntFunction<P_OUT[]> generator;
        private final boolean isOrdered;
        private long thisNodeSize;
        // The index from which elements of the node should be taken
        // i.e. the node should be truncated from [takeIndex, thisNodeSize)
        // Equivalent to the count of dropped elements
        private long index;

        DropWhileTask(AbstractPipeline<P_OUT, P_OUT, ?> op,
                      PipelineHelper<P_OUT> helper,
                      Spliterator<P_IN> spliterator,
                      IntFunction<P_OUT[]> generator) {
            super(helper, spliterator);
            assert op instanceof DropWhileOp;
            this.op = op;
            this.generator = generator;
            this.isOrdered = StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags());
        }

        DropWhileTask(DropWhileTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
            this.generator = parent.generator;
            this.isOrdered = parent.isOrdered;
        }

        @Override
        protected DropWhileTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new DropWhileTask<>(this, spliterator);
        }

        @Override
        protected final Node<P_OUT> doLeaf() {
            boolean isChild = !isRoot();
            // If this not the root and pipeline is ordered and size is known
            // then pre-size the builder
            long sizeIfKnown = isChild && isOrdered && StreamOpFlag.SIZED.isPreserved(op.sourceOrOpFlags)
                               ? op.exactOutputSizeIfKnown(spliterator)
                               : -1;
            Node.Builder<P_OUT> builder = helper.makeNodeBuilder(sizeIfKnown, generator);
            @SuppressWarnings("unchecked")
            DropWhileOp<P_OUT> dropOp = (DropWhileOp<P_OUT>) op;
            // If this leaf is the root then there is no merging on completion
            // and there is no need to retain dropped elements
            DropWhileSink<P_OUT> s = dropOp.opWrapSink(builder, isOrdered && isChild);
            helper.wrapAndCopyInto(s, spliterator);

            Node<P_OUT> node = builder.build();
            thisNodeSize = node.count();
            index = s.getDropCount();
            return node;
        }

        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            if (!isLeaf()) {
                if (isOrdered) {
                    index = leftChild.index;
                    // If a contiguous sequence of dropped elements
                    // include those of the right node, if any
                    if (index == leftChild.thisNodeSize)
                        index += rightChild.index;
                }

                thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                Node<P_OUT> result = merge();
                setLocalResult(isRoot() ? doTruncate(result) : result);
            }

            super.onCompletion(caller);
        }

        private Node<P_OUT> merge() {
            if (leftChild.thisNodeSize == 0) {
                // If the left node size is 0 then
                // use the right node result
                return rightChild.getLocalResult();
            }
            else if (rightChild.thisNodeSize == 0) {
                // If the right node size is 0 then
                // use the left node result
                return leftChild.getLocalResult();
            }
            else {
                // Combine the left and right nodes
                return Nodes.conc(op.getOutputShape(),
                                  leftChild.getLocalResult(), rightChild.getLocalResult());
            }
        }

        private Node<P_OUT> doTruncate(Node<P_OUT> input) {
            return isOrdered
                   ? input.truncate(index, input.count(), generator)
                   : input;
        }
    }
}
