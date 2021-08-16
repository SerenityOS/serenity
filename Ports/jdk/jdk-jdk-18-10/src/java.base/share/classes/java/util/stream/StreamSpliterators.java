/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Spliterator implementations for wrapping and delegating spliterators, used
 * in the implementation of the {@link Stream#spliterator()} method.
 *
 * @since 1.8
 */
class StreamSpliterators {

    /**
     * Abstract wrapping spliterator that binds to the spliterator of a
     * pipeline helper on first operation.
     *
     * <p>This spliterator is not late-binding and will bind to the source
     * spliterator when first operated on.
     *
     * <p>A wrapping spliterator produced from a sequential stream
     * cannot be split if there are stateful operations present.
     */
    private abstract static class AbstractWrappingSpliterator<P_IN, P_OUT,
                                                              T_BUFFER extends AbstractSpinedBuffer>
            implements Spliterator<P_OUT> {

        // @@@ Detect if stateful operations are present or not
        //     If not then can split otherwise cannot

        /**
         * True if this spliterator supports splitting
         */
        final boolean isParallel;

        final PipelineHelper<P_OUT> ph;

        /**
         * Supplier for the source spliterator.  Client provides either a
         * spliterator or a supplier.
         */
        private Supplier<Spliterator<P_IN>> spliteratorSupplier;

        /**
         * Source spliterator.  Either provided from client or obtained from
         * supplier.
         */
        Spliterator<P_IN> spliterator;

        /**
         * Sink chain for the downstream stages of the pipeline, ultimately
         * leading to the buffer. Used during partial traversal.
         */
        Sink<P_IN> bufferSink;

        /**
         * A function that advances one element of the spliterator, pushing
         * it to bufferSink.  Returns whether any elements were processed.
         * Used during partial traversal.
         */
        BooleanSupplier pusher;

        /** Next element to consume from the buffer, used during partial traversal */
        long nextToConsume;

        /** Buffer into which elements are pushed.  Used during partial traversal. */
        T_BUFFER buffer;

        /**
         * True if full traversal has occurred (with possible cancellation).
         * If doing a partial traversal, there may be still elements in buffer.
         */
        boolean finished;

        /**
         * Construct an AbstractWrappingSpliterator from a
         * {@code Supplier<Spliterator>}.
         */
        AbstractWrappingSpliterator(PipelineHelper<P_OUT> ph,
                                    Supplier<Spliterator<P_IN>> spliteratorSupplier,
                                    boolean parallel) {
            this.ph = ph;
            this.spliteratorSupplier = spliteratorSupplier;
            this.spliterator = null;
            this.isParallel = parallel;
        }

        /**
         * Construct an AbstractWrappingSpliterator from a
         * {@code Spliterator}.
         */
        AbstractWrappingSpliterator(PipelineHelper<P_OUT> ph,
                                    Spliterator<P_IN> spliterator,
                                    boolean parallel) {
            this.ph = ph;
            this.spliteratorSupplier = null;
            this.spliterator = spliterator;
            this.isParallel = parallel;
        }

        /**
         * Called before advancing to set up spliterator, if needed.
         */
        final void init() {
            if (spliterator == null) {
                spliterator = spliteratorSupplier.get();
                spliteratorSupplier = null;
            }
        }

        /**
         * Get an element from the source, pushing it into the sink chain,
         * setting up the buffer if needed
         * @return whether there are elements to consume from the buffer
         */
        final boolean doAdvance() {
            if (buffer == null) {
                if (finished)
                    return false;

                init();
                initPartialTraversalState();
                nextToConsume = 0;
                bufferSink.begin(spliterator.getExactSizeIfKnown());
                return fillBuffer();
            }
            else {
                ++nextToConsume;
                boolean hasNext = nextToConsume < buffer.count();
                if (!hasNext) {
                    nextToConsume = 0;
                    buffer.clear();
                    hasNext = fillBuffer();
                }
                return hasNext;
            }
        }

        /**
         * Invokes the shape-specific constructor with the provided arguments
         * and returns the result.
         */
        abstract AbstractWrappingSpliterator<P_IN, P_OUT, ?> wrap(Spliterator<P_IN> s);

        /**
         * Initializes buffer, sink chain, and pusher for a shape-specific
         * implementation.
         */
        abstract void initPartialTraversalState();

        @Override
        public Spliterator<P_OUT> trySplit() {
            if (isParallel && buffer == null && !finished) {
                init();

                Spliterator<P_IN> split = spliterator.trySplit();
                return (split == null) ? null : wrap(split);
            }
            else
                return null;
        }

        /**
         * If the buffer is empty, push elements into the sink chain until
         * the source is empty or cancellation is requested.
         * @return whether there are elements to consume from the buffer
         */
        private boolean fillBuffer() {
            while (buffer.count() == 0) {
                if (bufferSink.cancellationRequested() || !pusher.getAsBoolean()) {
                    if (finished)
                        return false;
                    else {
                        bufferSink.end(); // might trigger more elements
                        finished = true;
                    }
                }
            }
            return true;
        }

        @Override
        public final long estimateSize() {
            long exactSizeIfKnown = getExactSizeIfKnown();
            // Use the estimate of the wrapped spliterator
            // Note this may not be accurate if there are filter/flatMap
            // operations filtering or adding elements to the stream
            return exactSizeIfKnown == -1 ? spliterator.estimateSize() : exactSizeIfKnown;
        }

        @Override
        public final long getExactSizeIfKnown() {
            init();
            return ph.exactOutputSizeIfKnown(spliterator);
        }

        @Override
        public final int characteristics() {
            init();

            // Get the characteristics from the pipeline
            int c = StreamOpFlag.toCharacteristics(StreamOpFlag.toStreamFlags(ph.getStreamAndOpFlags()));

            // Mask off the size and uniform characteristics and replace with
            // those of the spliterator
            // Note that a non-uniform spliterator can change from something
            // with an exact size to an estimate for a sub-split, for example
            // with HashSet where the size is known at the top level spliterator
            // but for sub-splits only an estimate is known
            if ((c & Spliterator.SIZED) != 0) {
                c &= ~(Spliterator.SIZED | Spliterator.SUBSIZED);
                c |= (spliterator.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED));
            }

            return c;
        }

        @Override
        public Comparator<? super P_OUT> getComparator() {
            if (!hasCharacteristics(SORTED))
                throw new IllegalStateException();
            return null;
        }

        @Override
        public final String toString() {
            return String.format("%s[%s]", getClass().getName(), spliterator);
        }
    }

    static final class WrappingSpliterator<P_IN, P_OUT>
            extends AbstractWrappingSpliterator<P_IN, P_OUT, SpinedBuffer<P_OUT>> {

        WrappingSpliterator(PipelineHelper<P_OUT> ph,
                            Supplier<Spliterator<P_IN>> supplier,
                            boolean parallel) {
            super(ph, supplier, parallel);
        }

        WrappingSpliterator(PipelineHelper<P_OUT> ph,
                            Spliterator<P_IN> spliterator,
                            boolean parallel) {
            super(ph, spliterator, parallel);
        }

        @Override
        WrappingSpliterator<P_IN, P_OUT> wrap(Spliterator<P_IN> s) {
            return new WrappingSpliterator<>(ph, s, isParallel);
        }

        @Override
        void initPartialTraversalState() {
            SpinedBuffer<P_OUT> b = new SpinedBuffer<>();
            buffer = b;
            bufferSink = ph.wrapSink(b::accept);
            pusher = () -> spliterator.tryAdvance(bufferSink);
        }

        @Override
        public boolean tryAdvance(Consumer<? super P_OUT> consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext)
                consumer.accept(buffer.get(nextToConsume));
            return hasNext;
        }

        @Override
        public void forEachRemaining(Consumer<? super P_OUT> consumer) {
            if (buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                init();

                ph.wrapAndCopyInto((Sink<P_OUT>) consumer::accept, spliterator);
                finished = true;
            }
            else {
                do { } while (tryAdvance(consumer));
            }
        }
    }

    static final class IntWrappingSpliterator<P_IN>
            extends AbstractWrappingSpliterator<P_IN, Integer, SpinedBuffer.OfInt>
            implements Spliterator.OfInt {

        IntWrappingSpliterator(PipelineHelper<Integer> ph,
                               Supplier<Spliterator<P_IN>> supplier,
                               boolean parallel) {
            super(ph, supplier, parallel);
        }

        IntWrappingSpliterator(PipelineHelper<Integer> ph,
                               Spliterator<P_IN> spliterator,
                               boolean parallel) {
            super(ph, spliterator, parallel);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Integer, ?> wrap(Spliterator<P_IN> s) {
            return new IntWrappingSpliterator<>(ph, s, isParallel);
        }

        @Override
        void initPartialTraversalState() {
            SpinedBuffer.OfInt b = new SpinedBuffer.OfInt();
            buffer = b;
            bufferSink = ph.wrapSink((Sink.OfInt) b::accept);
            pusher = () -> spliterator.tryAdvance(bufferSink);
        }

        @Override
        public Spliterator.OfInt trySplit() {
            return (Spliterator.OfInt) super.trySplit();
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext)
                consumer.accept(buffer.get(nextToConsume));
            return hasNext;
        }

        @Override
        public void forEachRemaining(IntConsumer consumer) {
            if (buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                init();

                ph.wrapAndCopyInto((Sink.OfInt) consumer::accept, spliterator);
                finished = true;
            }
            else {
                do { } while (tryAdvance(consumer));
            }
        }
    }

    static final class LongWrappingSpliterator<P_IN>
            extends AbstractWrappingSpliterator<P_IN, Long, SpinedBuffer.OfLong>
            implements Spliterator.OfLong {

        LongWrappingSpliterator(PipelineHelper<Long> ph,
                                Supplier<Spliterator<P_IN>> supplier,
                                boolean parallel) {
            super(ph, supplier, parallel);
        }

        LongWrappingSpliterator(PipelineHelper<Long> ph,
                                Spliterator<P_IN> spliterator,
                                boolean parallel) {
            super(ph, spliterator, parallel);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Long, ?> wrap(Spliterator<P_IN> s) {
            return new LongWrappingSpliterator<>(ph, s, isParallel);
        }

        @Override
        void initPartialTraversalState() {
            SpinedBuffer.OfLong b = new SpinedBuffer.OfLong();
            buffer = b;
            bufferSink = ph.wrapSink((Sink.OfLong) b::accept);
            pusher = () -> spliterator.tryAdvance(bufferSink);
        }

        @Override
        public Spliterator.OfLong trySplit() {
            return (Spliterator.OfLong) super.trySplit();
        }

        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext)
                consumer.accept(buffer.get(nextToConsume));
            return hasNext;
        }

        @Override
        public void forEachRemaining(LongConsumer consumer) {
            if (buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                init();

                ph.wrapAndCopyInto((Sink.OfLong) consumer::accept, spliterator);
                finished = true;
            }
            else {
                do { } while (tryAdvance(consumer));
            }
        }
    }

    static final class DoubleWrappingSpliterator<P_IN>
            extends AbstractWrappingSpliterator<P_IN, Double, SpinedBuffer.OfDouble>
            implements Spliterator.OfDouble {

        DoubleWrappingSpliterator(PipelineHelper<Double> ph,
                                  Supplier<Spliterator<P_IN>> supplier,
                                  boolean parallel) {
            super(ph, supplier, parallel);
        }

        DoubleWrappingSpliterator(PipelineHelper<Double> ph,
                                  Spliterator<P_IN> spliterator,
                                  boolean parallel) {
            super(ph, spliterator, parallel);
        }

        @Override
        AbstractWrappingSpliterator<P_IN, Double, ?> wrap(Spliterator<P_IN> s) {
            return new DoubleWrappingSpliterator<>(ph, s, isParallel);
        }

        @Override
        void initPartialTraversalState() {
            SpinedBuffer.OfDouble b = new SpinedBuffer.OfDouble();
            buffer = b;
            bufferSink = ph.wrapSink((Sink.OfDouble) b::accept);
            pusher = () -> spliterator.tryAdvance(bufferSink);
        }

        @Override
        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }

        @Override
        public boolean tryAdvance(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext)
                consumer.accept(buffer.get(nextToConsume));
            return hasNext;
        }

        @Override
        public void forEachRemaining(DoubleConsumer consumer) {
            if (buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                init();

                ph.wrapAndCopyInto((Sink.OfDouble) consumer::accept, spliterator);
                finished = true;
            }
            else {
                do { } while (tryAdvance(consumer));
            }
        }
    }

    /**
     * Spliterator implementation that delegates to an underlying spliterator,
     * acquiring the spliterator from a {@code Supplier<Spliterator>} on the
     * first call to any spliterator method.
     * @param <T>
     */
    static class DelegatingSpliterator<T, T_SPLITR extends Spliterator<T>>
            implements Spliterator<T> {
        private final Supplier<? extends T_SPLITR> supplier;

        private T_SPLITR s;

        DelegatingSpliterator(Supplier<? extends T_SPLITR> supplier) {
            this.supplier = supplier;
        }

        T_SPLITR get() {
            if (s == null) {
                s = supplier.get();
            }
            return s;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T_SPLITR trySplit() {
            return (T_SPLITR) get().trySplit();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            return get().tryAdvance(consumer);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            get().forEachRemaining(consumer);
        }

        @Override
        public long estimateSize() {
            return get().estimateSize();
        }

        @Override
        public int characteristics() {
            return get().characteristics();
        }

        @Override
        public Comparator<? super T> getComparator() {
            return get().getComparator();
        }

        @Override
        public long getExactSizeIfKnown() {
            return get().getExactSizeIfKnown();
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + get() + "]";
        }

        static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
            extends DelegatingSpliterator<T, T_SPLITR>
            implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            OfPrimitive(Supplier<? extends T_SPLITR> supplier) {
                super(supplier);
            }

            @Override
            public boolean tryAdvance(T_CONS consumer) {
                return get().tryAdvance(consumer);
            }

            @Override
            public void forEachRemaining(T_CONS consumer) {
                get().forEachRemaining(consumer);
            }
        }

        static final class OfInt
                extends OfPrimitive<Integer, IntConsumer, Spliterator.OfInt>
                implements Spliterator.OfInt {

            OfInt(Supplier<Spliterator.OfInt> supplier) {
                super(supplier);
            }
        }

        static final class OfLong
                extends OfPrimitive<Long, LongConsumer, Spliterator.OfLong>
                implements Spliterator.OfLong {

            OfLong(Supplier<Spliterator.OfLong> supplier) {
                super(supplier);
            }
        }

        static final class OfDouble
                extends OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble>
                implements Spliterator.OfDouble {

            OfDouble(Supplier<Spliterator.OfDouble> supplier) {
                super(supplier);
            }
        }
    }

    /**
     * A slice Spliterator from a source Spliterator that reports
     * {@code SUBSIZED}.
     *
     */
    abstract static class SliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        // The start index of the slice
        final long sliceOrigin;
        // One past the last index of the slice
        final long sliceFence;

        // The spliterator to slice
        T_SPLITR s;
        // current (absolute) index, modified on advance/split
        long index;
        // one past last (absolute) index or sliceFence, which ever is smaller
        long fence;

        SliceSpliterator(T_SPLITR s, long sliceOrigin, long sliceFence, long origin, long fence) {
            assert s.hasCharacteristics(Spliterator.SUBSIZED);
            this.s = s;
            this.sliceOrigin = sliceOrigin;
            this.sliceFence = sliceFence;
            this.index = origin;
            this.fence = fence;
        }

        protected abstract T_SPLITR makeSpliterator(T_SPLITR s, long sliceOrigin, long sliceFence, long origin, long fence);

        public T_SPLITR trySplit() {
            if (sliceOrigin >= fence)
                return null;

            if (index >= fence)
                return null;

            // Keep splitting until the left and right splits intersect with the slice
            // thereby ensuring the size estimate decreases.
            // This also avoids creating empty spliterators which can result in
            // existing and additionally created F/J tasks that perform
            // redundant work on no elements.
            while (true) {
                @SuppressWarnings("unchecked")
                T_SPLITR leftSplit = (T_SPLITR) s.trySplit();
                if (leftSplit == null)
                    return null;

                long leftSplitFenceUnbounded = index + leftSplit.estimateSize();
                long leftSplitFence = Math.min(leftSplitFenceUnbounded, sliceFence);
                if (sliceOrigin >= leftSplitFence) {
                    // The left split does not intersect with, and is to the left of, the slice
                    // The right split does intersect
                    // Discard the left split and split further with the right split
                    index = leftSplitFence;
                }
                else if (leftSplitFence >= sliceFence) {
                    // The right split does not intersect with, and is to the right of, the slice
                    // The left split does intersect
                    // Discard the right split and split further with the left split
                    s = leftSplit;
                    fence = leftSplitFence;
                }
                else if (index >= sliceOrigin && leftSplitFenceUnbounded <= sliceFence) {
                    // The left split is contained within the slice, return the underlying left split
                    // Right split is contained within or intersects with the slice
                    index = leftSplitFence;
                    return leftSplit;
                } else {
                    // The left split intersects with the slice
                    // Right split is contained within or intersects with the slice
                    return makeSpliterator(leftSplit, sliceOrigin, sliceFence, index, index = leftSplitFence);
                }
            }
        }

        public long estimateSize() {
            return (sliceOrigin < fence)
                   ? fence - Math.max(sliceOrigin, index) : 0;
        }

        public int characteristics() {
            return s.characteristics();
        }

        static final class OfRef<T>
                extends SliceSpliterator<T, Spliterator<T>>
                implements Spliterator<T> {

            OfRef(Spliterator<T> s, long sliceOrigin, long sliceFence) {
                this(s, sliceOrigin, sliceFence, 0, Math.min(s.estimateSize(), sliceFence));
            }

            private OfRef(Spliterator<T> s,
                          long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> s,
                                                     long sliceOrigin, long sliceFence,
                                                     long origin, long fence) {
                return new OfRef<>(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);

                if (sliceOrigin >= fence)
                    return false;

                while (sliceOrigin > index) {
                    s.tryAdvance(e -> {});
                    index++;
                }

                if (index >= fence)
                    return false;

                index++;
                return s.tryAdvance(action);
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);

                if (sliceOrigin >= fence)
                    return;

                if (index >= fence)
                    return;

                if (index >= sliceOrigin && (index + s.estimateSize()) <= sliceFence) {
                    // The spliterator is contained within the slice
                    s.forEachRemaining(action);
                    index = fence;
                } else {
                    // The spliterator intersects with the slice
                    while (sliceOrigin > index) {
                        s.tryAdvance(e -> {});
                        index++;
                    }
                    // Traverse elements up to the fence
                    for (;index < fence; index++) {
                        s.tryAdvance(action);
                    }
                }
            }
        }

        abstract static class OfPrimitive<T,
                T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>,
                T_CONS>
                extends SliceSpliterator<T, T_SPLITR>
                implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {

            OfPrimitive(T_SPLITR s, long sliceOrigin, long sliceFence) {
                this(s, sliceOrigin, sliceFence, 0, Math.min(s.estimateSize(), sliceFence));
            }

            private OfPrimitive(T_SPLITR s,
                                long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            public boolean tryAdvance(T_CONS action) {
                Objects.requireNonNull(action);

                if (sliceOrigin >= fence)
                    return false;

                while (sliceOrigin > index) {
                    s.tryAdvance(emptyConsumer());
                    index++;
                }

                if (index >= fence)
                    return false;

                index++;
                return s.tryAdvance(action);
            }

            @Override
            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);

                if (sliceOrigin >= fence)
                    return;

                if (index >= fence)
                    return;

                if (index >= sliceOrigin && (index + s.estimateSize()) <= sliceFence) {
                    // The spliterator is contained within the slice
                    s.forEachRemaining(action);
                    index = fence;
                } else {
                    // The spliterator intersects with the slice
                    while (sliceOrigin > index) {
                        s.tryAdvance(emptyConsumer());
                        index++;
                    }
                    // Traverse elements up to the fence
                    for (;index < fence; index++) {
                        s.tryAdvance(action);
                    }
                }
            }

            protected abstract T_CONS emptyConsumer();
        }

        static final class OfInt extends OfPrimitive<Integer, Spliterator.OfInt, IntConsumer>
                implements Spliterator.OfInt {
            OfInt(Spliterator.OfInt s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfInt(Spliterator.OfInt s,
                  long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt s,
                                                        long sliceOrigin, long sliceFence,
                                                        long origin, long fence) {
                return new SliceSpliterator.OfInt(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected IntConsumer emptyConsumer() {
                return e -> {};
            }
        }

        static final class OfLong extends OfPrimitive<Long, Spliterator.OfLong, LongConsumer>
                implements Spliterator.OfLong {
            OfLong(Spliterator.OfLong s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfLong(Spliterator.OfLong s,
                   long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong s,
                                                         long sliceOrigin, long sliceFence,
                                                         long origin, long fence) {
                return new SliceSpliterator.OfLong(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected LongConsumer emptyConsumer() {
                return e -> {};
            }
        }

        static final class OfDouble extends OfPrimitive<Double, Spliterator.OfDouble, DoubleConsumer>
                implements Spliterator.OfDouble {
            OfDouble(Spliterator.OfDouble s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfDouble(Spliterator.OfDouble s,
                     long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble s,
                                                           long sliceOrigin, long sliceFence,
                                                           long origin, long fence) {
                return new SliceSpliterator.OfDouble(s, sliceOrigin, sliceFence, origin, fence);
            }

            @Override
            protected DoubleConsumer emptyConsumer() {
                return e -> {};
            }
        }
    }

    /**
     * A slice Spliterator that does not preserve order, if any, of a source
     * Spliterator.
     *
     * Note: The source spliterator may report {@code ORDERED} since that
     * spliterator be the result of a previous pipeline stage that was
     * collected to a {@code Node}. It is the order of the pipeline stage
     * that governs whether this slice spliterator is to be used or not.
     */
    abstract static class UnorderedSliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        static final int CHUNK_SIZE = 1 << 7;

        // The spliterator to slice
        protected final T_SPLITR s;
        protected final boolean unlimited;
        protected final int chunkSize;
        private final long skipThreshold;
        private final AtomicLong permits;

        UnorderedSliceSpliterator(T_SPLITR s, long skip, long limit) {
            this.s = s;
            this.unlimited = limit < 0;
            this.skipThreshold = limit >= 0 ? limit : 0;
            this.chunkSize = limit >= 0 ? (int)Math.min(CHUNK_SIZE,
                                                        ((skip + limit) / AbstractTask.getLeafTarget()) + 1) : CHUNK_SIZE;
            this.permits = new AtomicLong(limit >= 0 ? skip + limit : skip);
        }

        UnorderedSliceSpliterator(T_SPLITR s,
                                  UnorderedSliceSpliterator<T, T_SPLITR> parent) {
            this.s = s;
            this.unlimited = parent.unlimited;
            this.permits = parent.permits;
            this.skipThreshold = parent.skipThreshold;
            this.chunkSize = parent.chunkSize;
        }

        /**
         * Acquire permission to skip or process elements.  The caller must
         * first acquire the elements, then consult this method for guidance
         * as to what to do with the data.
         *
         * <p>We use an {@code AtomicLong} to atomically maintain a counter,
         * which is initialized as skip+limit if we are limiting, or skip only
         * if we are not limiting.  The user should consult the method
         * {@code checkPermits()} before acquiring data elements.
         *
         * @param numElements the number of elements the caller has in hand
         * @return the number of elements that should be processed; any
         * remaining elements should be discarded.
         */
        protected final long acquirePermits(long numElements) {
            long remainingPermits;
            long grabbing;
            // permits never increase, and don't decrease below zero
            assert numElements > 0;
            do {
                remainingPermits = permits.get();
                if (remainingPermits == 0)
                    return unlimited ? numElements : 0;
                grabbing = Math.min(remainingPermits, numElements);
            } while (grabbing > 0 &&
                     !permits.compareAndSet(remainingPermits, remainingPermits - grabbing));

            if (unlimited)
                return Math.max(numElements - grabbing, 0);
            else if (remainingPermits > skipThreshold)
                return Math.max(grabbing - (remainingPermits - skipThreshold), 0);
            else
                return grabbing;
        }

        enum PermitStatus { NO_MORE, MAYBE_MORE, UNLIMITED }

        /** Call to check if permits might be available before acquiring data */
        protected final PermitStatus permitStatus() {
            if (permits.get() > 0)
                return PermitStatus.MAYBE_MORE;
            else
                return unlimited ?  PermitStatus.UNLIMITED : PermitStatus.NO_MORE;
        }

        public final T_SPLITR trySplit() {
            // Stop splitting when there are no more limit permits
            if (permits.get() == 0)
                return null;
            @SuppressWarnings("unchecked")
            T_SPLITR split = (T_SPLITR) s.trySplit();
            return split == null ? null : makeSpliterator(split);
        }

        protected abstract T_SPLITR makeSpliterator(T_SPLITR s);

        public final long estimateSize() {
            return s.estimateSize();
        }

        public final int characteristics() {
            return s.characteristics() &
                   ~(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED);
        }

        static final class OfRef<T> extends UnorderedSliceSpliterator<T, Spliterator<T>>
                implements Spliterator<T>, Consumer<T> {
            T tmpSlot;

            OfRef(Spliterator<T> s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfRef(Spliterator<T> s, OfRef<T> parent) {
                super(s, parent);
            }

            @Override
            public final void accept(T t) {
                tmpSlot = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);

                while (permitStatus() != PermitStatus.NO_MORE) {
                    if (!s.tryAdvance(this))
                        return false;
                    else if (acquirePermits(1) == 1) {
                        action.accept(tmpSlot);
                        tmpSlot = null;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);

                ArrayBuffer.OfRef<T> sb = null;
                PermitStatus permitStatus;
                while ((permitStatus = permitStatus()) != PermitStatus.NO_MORE) {
                    if (permitStatus == PermitStatus.MAYBE_MORE) {
                        // Optimistically traverse elements up to a threshold of chunkSize
                        if (sb == null)
                            sb = new ArrayBuffer.OfRef<>(chunkSize);
                        else
                            sb.reset();
                        long permitsRequested = 0;
                        do { } while (s.tryAdvance(sb) && ++permitsRequested < chunkSize);
                        if (permitsRequested == 0)
                            return;
                        sb.forEach(action, acquirePermits(permitsRequested));
                    }
                    else {
                        // Must be UNLIMITED; let 'er rip
                        s.forEachRemaining(action);
                        return;
                    }
                }
            }

            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> s) {
                return new UnorderedSliceSpliterator.OfRef<>(s, this);
            }
        }

        /**
         * Concrete sub-types must also be an instance of type {@code T_CONS}.
         *
         * @param <T_BUFF> the type of the spined buffer. Must also be a type of
         *        {@code T_CONS}.
         */
        abstract static class OfPrimitive<
                T,
                T_CONS,
                T_BUFF extends ArrayBuffer.OfPrimitive<T_CONS>,
                T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
                extends UnorderedSliceSpliterator<T, T_SPLITR>
                implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            OfPrimitive(T_SPLITR s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfPrimitive(T_SPLITR s, UnorderedSliceSpliterator.OfPrimitive<T, T_CONS, T_BUFF, T_SPLITR> parent) {
                super(s, parent);
            }

            @Override
            public boolean tryAdvance(T_CONS action) {
                Objects.requireNonNull(action);
                @SuppressWarnings("unchecked")
                T_CONS consumer = (T_CONS) this;

                while (permitStatus() != PermitStatus.NO_MORE) {
                    if (!s.tryAdvance(consumer))
                        return false;
                    else if (acquirePermits(1) == 1) {
                        acceptConsumed(action);
                        return true;
                    }
                }
                return false;
            }

            protected abstract void acceptConsumed(T_CONS action);

            @Override
            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);

                T_BUFF sb = null;
                PermitStatus permitStatus;
                while ((permitStatus = permitStatus()) != PermitStatus.NO_MORE) {
                    if (permitStatus == PermitStatus.MAYBE_MORE) {
                        // Optimistically traverse elements up to a threshold of chunkSize
                        if (sb == null)
                            sb = bufferCreate(chunkSize);
                        else
                            sb.reset();
                        @SuppressWarnings("unchecked")
                        T_CONS sbc = (T_CONS) sb;
                        long permitsRequested = 0;
                        do { } while (s.tryAdvance(sbc) && ++permitsRequested < chunkSize);
                        if (permitsRequested == 0)
                            return;
                        sb.forEach(action, acquirePermits(permitsRequested));
                    }
                    else {
                        // Must be UNLIMITED; let 'er rip
                        s.forEachRemaining(action);
                        return;
                    }
                }
            }

            protected abstract T_BUFF bufferCreate(int initialCapacity);
        }

        static final class OfInt
                extends OfPrimitive<Integer, IntConsumer, ArrayBuffer.OfInt, Spliterator.OfInt>
                implements Spliterator.OfInt, IntConsumer {

            int tmpValue;

            OfInt(Spliterator.OfInt s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfInt(Spliterator.OfInt s, UnorderedSliceSpliterator.OfInt parent) {
                super(s, parent);
            }

            @Override
            public void accept(int value) {
                tmpValue = value;
            }

            @Override
            protected void acceptConsumed(IntConsumer action) {
                action.accept(tmpValue);
            }

            @Override
            protected ArrayBuffer.OfInt bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfInt(initialCapacity);
            }

            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt s) {
                return new UnorderedSliceSpliterator.OfInt(s, this);
            }
        }

        static final class OfLong
                extends OfPrimitive<Long, LongConsumer, ArrayBuffer.OfLong, Spliterator.OfLong>
                implements Spliterator.OfLong, LongConsumer {

            long tmpValue;

            OfLong(Spliterator.OfLong s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfLong(Spliterator.OfLong s, UnorderedSliceSpliterator.OfLong parent) {
                super(s, parent);
            }

            @Override
            public void accept(long value) {
                tmpValue = value;
            }

            @Override
            protected void acceptConsumed(LongConsumer action) {
                action.accept(tmpValue);
            }

            @Override
            protected ArrayBuffer.OfLong bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfLong(initialCapacity);
            }

            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong s) {
                return new UnorderedSliceSpliterator.OfLong(s, this);
            }
        }

        static final class OfDouble
                extends OfPrimitive<Double, DoubleConsumer, ArrayBuffer.OfDouble, Spliterator.OfDouble>
                implements Spliterator.OfDouble, DoubleConsumer {

            double tmpValue;

            OfDouble(Spliterator.OfDouble s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfDouble(Spliterator.OfDouble s, UnorderedSliceSpliterator.OfDouble parent) {
                super(s, parent);
            }

            @Override
            public void accept(double value) {
                tmpValue = value;
            }

            @Override
            protected void acceptConsumed(DoubleConsumer action) {
                action.accept(tmpValue);
            }

            @Override
            protected ArrayBuffer.OfDouble bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfDouble(initialCapacity);
            }

            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble s) {
                return new UnorderedSliceSpliterator.OfDouble(s, this);
            }
        }
    }

    /**
     * A wrapping spliterator that only reports distinct elements of the
     * underlying spliterator. Does not preserve size and encounter order.
     */
    static final class DistinctSpliterator<T> implements Spliterator<T>, Consumer<T> {

        // The value to represent null in the ConcurrentHashMap
        private static final Object NULL_VALUE = new Object();

        // The underlying spliterator
        private final Spliterator<T> s;

        // ConcurrentHashMap holding distinct elements as keys
        private final ConcurrentHashMap<T, Boolean> seen;

        // Temporary element, only used with tryAdvance
        private T tmpSlot;

        DistinctSpliterator(Spliterator<T> s) {
            this(s, new ConcurrentHashMap<>());
        }

        private DistinctSpliterator(Spliterator<T> s, ConcurrentHashMap<T, Boolean> seen) {
            this.s = s;
            this.seen = seen;
        }

        @Override
        public void accept(T t) {
            this.tmpSlot = t;
        }

        @SuppressWarnings("unchecked")
        private T mapNull(T t) {
            return t != null ? t : (T) NULL_VALUE;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while (s.tryAdvance(this)) {
                if (seen.putIfAbsent(mapNull(tmpSlot), Boolean.TRUE) == null) {
                    action.accept(tmpSlot);
                    tmpSlot = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            s.forEachRemaining(t -> {
                if (seen.putIfAbsent(mapNull(t), Boolean.TRUE) == null) {
                    action.accept(t);
                }
            });
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<T> split = s.trySplit();
            return (split != null) ? new DistinctSpliterator<>(split, seen) : null;
        }

        @Override
        public long estimateSize() {
            return s.estimateSize();
        }

        @Override
        public int characteristics() {
            return (s.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED |
                                            Spliterator.SORTED | Spliterator.ORDERED))
                   | Spliterator.DISTINCT;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return s.getComparator();
        }
    }

    /**
     * A Spliterator that infinitely supplies elements in no particular order.
     *
     * <p>Splitting divides the estimated size in two and stops when the
     * estimate size is 0.
     *
     * <p>The {@code forEachRemaining} method if invoked will never terminate.
     * The {@code tryAdvance} method always returns true.
     *
     */
    abstract static class InfiniteSupplyingSpliterator<T> implements Spliterator<T> {
        long estimate;

        protected InfiniteSupplyingSpliterator(long estimate) {
            this.estimate = estimate;
        }

        @Override
        public long estimateSize() {
            return estimate;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE;
        }

        static final class OfRef<T> extends InfiniteSupplyingSpliterator<T> {
            final Supplier<? extends T> s;

            OfRef(long size, Supplier<? extends T> s) {
                super(size);
                this.s = s;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);

                action.accept(s.get());
                return true;
            }

            @Override
            public Spliterator<T> trySplit() {
                if (estimate == 0)
                    return null;
                return new InfiniteSupplyingSpliterator.OfRef<>(estimate >>>= 1, s);
            }
        }

        static final class OfInt extends InfiniteSupplyingSpliterator<Integer>
                implements Spliterator.OfInt {
            final IntSupplier s;

            OfInt(long size, IntSupplier s) {
                super(size);
                this.s = s;
            }

            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);

                action.accept(s.getAsInt());
                return true;
            }

            @Override
            public Spliterator.OfInt trySplit() {
                if (estimate == 0)
                    return null;
                return new InfiniteSupplyingSpliterator.OfInt(estimate = estimate >>> 1, s);
            }
        }

        static final class OfLong extends InfiniteSupplyingSpliterator<Long>
                implements Spliterator.OfLong {
            final LongSupplier s;

            OfLong(long size, LongSupplier s) {
                super(size);
                this.s = s;
            }

            @Override
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action);

                action.accept(s.getAsLong());
                return true;
            }

            @Override
            public Spliterator.OfLong trySplit() {
                if (estimate == 0)
                    return null;
                return new InfiniteSupplyingSpliterator.OfLong(estimate = estimate >>> 1, s);
            }
        }

        static final class OfDouble extends InfiniteSupplyingSpliterator<Double>
                implements Spliterator.OfDouble {
            final DoubleSupplier s;

            OfDouble(long size, DoubleSupplier s) {
                super(size);
                this.s = s;
            }

            @Override
            public boolean tryAdvance(DoubleConsumer action) {
                Objects.requireNonNull(action);

                action.accept(s.getAsDouble());
                return true;
            }

            @Override
            public Spliterator.OfDouble trySplit() {
                if (estimate == 0)
                    return null;
                return new InfiniteSupplyingSpliterator.OfDouble(estimate = estimate >>> 1, s);
            }
        }
    }

    // @@@ Consolidate with Node.Builder
    abstract static class ArrayBuffer {
        int index;

        void reset() {
            index = 0;
        }

        static final class OfRef<T> extends ArrayBuffer implements Consumer<T> {
            final Object[] array;

            OfRef(int size) {
                this.array = new Object[size];
            }

            @Override
            public void accept(T t) {
                array[index++] = t;
            }

            public void forEach(Consumer<? super T> action, long fence) {
                for (int i = 0; i < fence; i++) {
                    @SuppressWarnings("unchecked")
                    T t = (T) array[i];
                    action.accept(t);
                }
            }
        }

        abstract static class OfPrimitive<T_CONS> extends ArrayBuffer {
            int index;

            @Override
            void reset() {
                index = 0;
            }

            abstract void forEach(T_CONS action, long fence);
        }

        static final class OfInt extends OfPrimitive<IntConsumer>
                implements IntConsumer {
            final int[] array;

            OfInt(int size) {
                this.array = new int[size];
            }

            @Override
            public void accept(int t) {
                array[index++] = t;
            }

            @Override
            public void forEach(IntConsumer action, long fence) {
                for (int i = 0; i < fence; i++) {
                    action.accept(array[i]);
                }
            }
        }

        static final class OfLong extends OfPrimitive<LongConsumer>
                implements LongConsumer {
            final long[] array;

            OfLong(int size) {
                this.array = new long[size];
            }

            @Override
            public void accept(long t) {
                array[index++] = t;
            }

            @Override
            public void forEach(LongConsumer action, long fence) {
                for (int i = 0; i < fence; i++) {
                    action.accept(array[i]);
                }
            }
        }

        static final class OfDouble extends OfPrimitive<DoubleConsumer>
                implements DoubleConsumer {
            final double[] array;

            OfDouble(int size) {
                this.array = new double[size];
            }

            @Override
            public void accept(double t) {
                array[index++] = t;
            }

            @Override
            void forEach(DoubleConsumer action, long fence) {
                for (int i = 0; i < fence; i++) {
                    action.accept(array[i]);
                }
            }
        }
    }
}

