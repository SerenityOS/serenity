/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * Utility methods for operating on and creating streams.
 *
 * <p>Unless otherwise stated, streams are created as sequential streams.  A
 * sequential stream can be transformed into a parallel stream by calling the
 * {@code parallel()} method on the created stream.
 *
 * @since 1.8
 */
final class Streams {

    private Streams() {
        throw new Error("no instances");
    }

    /**
     * An {@code int} range spliterator.
     */
    static final class RangeIntSpliterator implements Spliterator.OfInt {
        // Can never be greater that upTo, this avoids overflow if upper bound
        // is Integer.MAX_VALUE
        // All elements are traversed if from == upTo & last == 0
        private int from;
        private final int upTo;
        // 1 if the range is closed and the last element has not been traversed
        // Otherwise, 0 if the range is open, or is a closed range and all
        // elements have been traversed
        private int last;

        RangeIntSpliterator(int from, int upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }

        private RangeIntSpliterator(int from, int upTo, int last) {
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);

            final int i = from;
            if (i < upTo) {
                from++;
                consumer.accept(i);
                return true;
            }
            else if (last > 0) {
                last = 0;
                consumer.accept(i);
                return true;
            }
            return false;
        }

        @Override
        @IntrinsicCandidate
        public void forEachRemaining(IntConsumer consumer) {
            Objects.requireNonNull(consumer);

            int i = from;
            final int hUpTo = upTo;
            int hLast = last;
            from = upTo;
            last = 0;
            while (i < hUpTo) {
                consumer.accept(i++);
            }
            if (hLast > 0) {
                // Last element of closed range
                consumer.accept(i);
            }
        }

        @Override
        public long estimateSize() {
            // Ensure ranges of size > Integer.MAX_VALUE report the correct size
            return ((long) upTo) - from + last;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.IMMUTABLE | Spliterator.NONNULL |
                   Spliterator.DISTINCT | Spliterator.SORTED;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            long size = estimateSize();
            return size <= 1
                   ? null
                   // Left split always has a half-open range
                   : new RangeIntSpliterator(from, from = from + splitPoint(size), 0);
        }

        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as IntStream.range(0, Integer.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final int BALANCED_SPLIT_THRESHOLD = 1 << 24;

        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final int RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;

        private int splitPoint(long size) {
            int d = (size < BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            // Cast to int is safe since:
            //   2 <= size < 2^32
            //   2 <= d <= 8
            return (int) (size / d);
        }
    }

    /**
     * A {@code long} range spliterator.
     *
     * This implementation cannot be used for ranges whose size is greater
     * than Long.MAX_VALUE
     */
    static final class RangeLongSpliterator implements Spliterator.OfLong {
        // Can never be greater that upTo, this avoids overflow if upper bound
        // is Long.MAX_VALUE
        // All elements are traversed if from == upTo & last == 0
        private long from;
        private final long upTo;
        // 1 if the range is closed and the last element has not been traversed
        // Otherwise, 0 if the range is open, or is a closed range and all
        // elements have been traversed
        private int last;

        RangeLongSpliterator(long from, long upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }

        private RangeLongSpliterator(long from, long upTo, int last) {
            assert upTo - from + last > 0;
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }

        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);

            final long i = from;
            if (i < upTo) {
                from++;
                consumer.accept(i);
                return true;
            }
            else if (last > 0) {
                last = 0;
                consumer.accept(i);
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(LongConsumer consumer) {
            Objects.requireNonNull(consumer);

            long i = from;
            final long hUpTo = upTo;
            int hLast = last;
            from = upTo;
            last = 0;
            while (i < hUpTo) {
                consumer.accept(i++);
            }
            if (hLast > 0) {
                // Last element of closed range
                consumer.accept(i);
            }
        }

        @Override
        public long estimateSize() {
            return upTo - from + last;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.IMMUTABLE | Spliterator.NONNULL |
                   Spliterator.DISTINCT | Spliterator.SORTED;
        }

        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }

        @Override
        public Spliterator.OfLong trySplit() {
            long size = estimateSize();
            return size <= 1
                   ? null
                   // Left split always has a half-open range
                   : new RangeLongSpliterator(from, from = from + splitPoint(size), 0);
        }

        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as LongStream.range(0, Long.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final long BALANCED_SPLIT_THRESHOLD = 1 << 24;

        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final long RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;

        private long splitPoint(long size) {
            long d = (size < BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            // 2 <= size <= Long.MAX_VALUE
            return size / d;
        }
    }

    private abstract static class AbstractStreamBuilderImpl<T, S extends Spliterator<T>> implements Spliterator<T> {
        // >= 0 when building, < 0 when built
        // -1 == no elements
        // -2 == one element, held by first
        // -3 == two or more elements, held by buffer
        int count;

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public S trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return -count - 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED |
                   Spliterator.ORDERED | Spliterator.IMMUTABLE;
        }
    }

    static final class StreamBuilderImpl<T>
            extends AbstractStreamBuilderImpl<T, Spliterator<T>>
            implements Stream.Builder<T> {
        // The first element in the stream
        // valid if count == 1
        T first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer<T> buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        StreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        StreamBuilderImpl(T t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(T t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer<>();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        public Stream.Builder<T> add(T t) {
            accept(t);
            return this;
        }

        @Override
        public Stream<T> build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.stream(this, false) : StreamSupport.stream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class IntStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Integer, Spliterator.OfInt>
            implements IntStream.Builder, Spliterator.OfInt {
        // The first element in the stream
        // valid if count == 1
        int first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfInt buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        IntStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        IntStreamBuilderImpl(int t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(int t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfInt();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public IntStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.intStream(this, false) : StreamSupport.intStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class LongStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Long, Spliterator.OfLong>
            implements LongStream.Builder, Spliterator.OfLong {
        // The first element in the stream
        // valid if count == 1
        long first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfLong buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        LongStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        LongStreamBuilderImpl(long t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(long t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfLong();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public LongStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.longStream(this, false) : StreamSupport.longStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    static final class DoubleStreamBuilderImpl
            extends AbstractStreamBuilderImpl<Double, Spliterator.OfDouble>
            implements DoubleStream.Builder, Spliterator.OfDouble {
        // The first element in the stream
        // valid if count == 1
        double first;

        // The first and subsequent elements in the stream
        // non-null if count == 2
        SpinedBuffer.OfDouble buffer;

        /**
         * Constructor for building a stream of 0 or more elements.
         */
        DoubleStreamBuilderImpl() { }

        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        DoubleStreamBuilderImpl(double t) {
            first = t;
            count = -2;
        }

        // StreamBuilder implementation

        @Override
        public void accept(double t) {
            if (count == 0) {
                first = t;
                count++;
            }
            else if (count > 0) {
                if (buffer == null) {
                    buffer = new SpinedBuffer.OfDouble();
                    buffer.accept(first);
                    count++;
                }

                buffer.accept(t);
            }
            else {
                throw new IllegalStateException();
            }
        }

        @Override
        public DoubleStream build() {
            int c = count;
            if (c >= 0) {
                // Switch count to negative value signalling the builder is built
                count = -count - 1;
                // Use this spliterator if 0 or 1 elements, otherwise use
                // the spliterator of the spined buffer
                return (c < 2) ? StreamSupport.doubleStream(this, false) : StreamSupport.doubleStream(buffer.spliterator(), false);
            }

            throw new IllegalStateException();
        }

        // Spliterator implementation for 0 or 1 element
        // count == -1 for no elements
        // count == -2 for one element held by first

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);

            if (count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }

    abstract static class ConcatSpliterator<T, T_SPLITR extends Spliterator<T>>
            implements Spliterator<T> {
        protected final T_SPLITR aSpliterator;
        protected final T_SPLITR bSpliterator;
        // True when no split has occurred, otherwise false
        boolean beforeSplit;
        // Never read after splitting
        final boolean unsized;

        public ConcatSpliterator(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
            this.aSpliterator = aSpliterator;
            this.bSpliterator = bSpliterator;
            beforeSplit = true;
            // The spliterator is known to be unsized before splitting if the
            // sum of the estimates overflows.
            unsized = aSpliterator.estimateSize() + bSpliterator.estimateSize() < 0;
        }

        @Override
        public T_SPLITR trySplit() {
            @SuppressWarnings("unchecked")
            T_SPLITR ret = beforeSplit ? aSpliterator : (T_SPLITR) bSpliterator.trySplit();
            beforeSplit = false;
            return ret;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            boolean hasNext;
            if (beforeSplit) {
                hasNext = aSpliterator.tryAdvance(consumer);
                if (!hasNext) {
                    beforeSplit = false;
                    hasNext = bSpliterator.tryAdvance(consumer);
                }
            }
            else
                hasNext = bSpliterator.tryAdvance(consumer);
            return hasNext;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            if (beforeSplit)
                aSpliterator.forEachRemaining(consumer);
            bSpliterator.forEachRemaining(consumer);
        }

        @Override
        public long estimateSize() {
            if (beforeSplit) {
                // If one or both estimates are Long.MAX_VALUE then the sum
                // will either be Long.MAX_VALUE or overflow to a negative value
                long size = aSpliterator.estimateSize() + bSpliterator.estimateSize();
                return (size >= 0) ? size : Long.MAX_VALUE;
            }
            else {
                return bSpliterator.estimateSize();
            }
        }

        @Override
        public int characteristics() {
            if (beforeSplit) {
                // Concatenation loses DISTINCT and SORTED characteristics
                return aSpliterator.characteristics() & bSpliterator.characteristics()
                       & ~(Spliterator.DISTINCT | Spliterator.SORTED
                           | (unsized ? Spliterator.SIZED | Spliterator.SUBSIZED : 0));
            }
            else {
                return bSpliterator.characteristics();
            }
        }

        @Override
        public Comparator<? super T> getComparator() {
            if (beforeSplit)
                throw new IllegalStateException();
            return bSpliterator.getComparator();
        }

        static class OfRef<T> extends ConcatSpliterator<T, Spliterator<T>> {
            OfRef(Spliterator<T> aSpliterator, Spliterator<T> bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        private abstract static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
                extends ConcatSpliterator<T, T_SPLITR>
                implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            private OfPrimitive(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
                super(aSpliterator, bSpliterator);
            }

            @Override
            public boolean tryAdvance(T_CONS action) {
                boolean hasNext;
                if (beforeSplit) {
                    hasNext = aSpliterator.tryAdvance(action);
                    if (!hasNext) {
                        beforeSplit = false;
                        hasNext = bSpliterator.tryAdvance(action);
                    }
                }
                else
                    hasNext = bSpliterator.tryAdvance(action);
                return hasNext;
            }

            @Override
            public void forEachRemaining(T_CONS action) {
                if (beforeSplit)
                    aSpliterator.forEachRemaining(action);
                bSpliterator.forEachRemaining(action);
            }
        }

        static class OfInt
                extends ConcatSpliterator.OfPrimitive<Integer, IntConsumer, Spliterator.OfInt>
                implements Spliterator.OfInt {
            OfInt(Spliterator.OfInt aSpliterator, Spliterator.OfInt bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        static class OfLong
                extends ConcatSpliterator.OfPrimitive<Long, LongConsumer, Spliterator.OfLong>
                implements Spliterator.OfLong {
            OfLong(Spliterator.OfLong aSpliterator, Spliterator.OfLong bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }

        static class OfDouble
                extends ConcatSpliterator.OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble>
                implements Spliterator.OfDouble {
            OfDouble(Spliterator.OfDouble aSpliterator, Spliterator.OfDouble bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
    }

    /**
     * Given two Runnables, return a Runnable that executes both in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composeWithExceptions(Runnable a, Runnable b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.run();
                }
                catch (Throwable e1) {
                    try {
                        b.run();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.run();
            }
        };
    }

    /**
     * Given two streams, return a Runnable that
     * executes both of their {@link BaseStream#close} methods in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composedClose(BaseStream<?, ?> a, BaseStream<?, ?> b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.close();
                }
                catch (Throwable e1) {
                    try {
                        b.close();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.close();
            }
        };
    }
}
