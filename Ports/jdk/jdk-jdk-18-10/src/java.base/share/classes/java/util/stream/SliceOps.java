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

import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.IntFunction;

/**
 * Factory for instances of a short-circuiting stateful intermediate operations
 * that produce subsequences of their input stream.
 *
 * @since 1.8
 */
final class SliceOps {

    // No instances
    private SliceOps() { }

    /**
     * Calculates the sliced size given the current size, number of elements
     * skip, and the number of elements to limit.
     *
     * @param size the current size
     * @param skip the number of elements to skip, assumed to be >= 0
     * @param limit the number of elements to limit, assumed to be >= 0, with
     *        a value of {@code Long.MAX_VALUE} if there is no limit
     * @return the sliced size
     */
    private static long calcSize(long size, long skip, long limit) {
        return size >= 0 ? Math.max(0, Math.min(size - skip, limit)) : -1;
    }

    /**
     * Calculates the slice fence, which is one past the index of the slice
     * range
     * @param skip the number of elements to skip, assumed to be >= 0
     * @param limit the number of elements to limit, assumed to be >= 0, with
     *        a value of {@code Long.MAX_VALUE} if there is no limit
     * @return the slice fence.
     */
    private static long calcSliceFence(long skip, long limit) {
        long sliceFence = limit >= 0 ? skip + limit : Long.MAX_VALUE;
        // Check for overflow
        return (sliceFence >= 0) ? sliceFence : Long.MAX_VALUE;
    }

    /**
     * Creates a slice spliterator given a stream shape governing the
     * spliterator type.  Requires that the underlying Spliterator
     * be SUBSIZED.
     */
    private static <P_IN> Spliterator<P_IN> sliceSpliterator(StreamShape shape,
                                                             Spliterator<P_IN> s,
                                                             long skip, long limit) {
        assert s.hasCharacteristics(Spliterator.SUBSIZED);
        long sliceFence = calcSliceFence(skip, limit);
        @SuppressWarnings("unchecked")
        Spliterator<P_IN> sliceSpliterator = (Spliterator<P_IN>) switch (shape) {
            case REFERENCE
                -> new StreamSpliterators.SliceSpliterator.OfRef<>(s, skip, sliceFence);
            case INT_VALUE
                -> new StreamSpliterators.SliceSpliterator.OfInt((Spliterator.OfInt) s, skip, sliceFence);
            case LONG_VALUE
                -> new StreamSpliterators.SliceSpliterator.OfLong((Spliterator.OfLong) s, skip, sliceFence);
            case DOUBLE_VALUE
                -> new StreamSpliterators.SliceSpliterator.OfDouble((Spliterator.OfDouble) s, skip, sliceFence);
        };
        return sliceSpliterator;
    }

    /**
     * Appends a "slice" operation to the provided stream.  The slice operation
     * may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param <T> the type of both input and output elements
     * @param upstream a reference stream with element type T
     * @param skip the number of elements to skip.  Must be >= 0.
     * @param limit the maximum size of the resulting stream, or -1 if no limit
     *        is to be imposed
     */
    public static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> upstream,
                                        long skip, long limit) {
        if (skip < 0)
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        long normalizedLimit = limit >= 0 ? limit : Long.MAX_VALUE;

        return new ReferencePipeline.StatefulOp<T, T>(upstream, StreamShape.REFERENCE,
                                                      flags(limit)) {
            @Override
            long exactOutputSize(long previousSize) {
                return calcSize(previousSize, skip, normalizedLimit);
            }

            Spliterator<T> unorderedSkipLimitSpliterator(Spliterator<T> s,
                                                         long skip, long limit, long sizeIfKnown) {
                if (skip <= sizeIfKnown) {
                    // Use just the limit if the number of elements
                    // to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                return new StreamSpliterators.UnorderedSliceSpliterator.OfRef<>(s, skip, limit);
            }

            @Override
            <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    return new StreamSpliterators.SliceSpliterator.OfRef<>(
                            helper.wrapSpliterator(spliterator),
                            skip,
                            calcSliceFence(skip, limit));
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return unorderedSkipLimitSpliterator(
                            helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                }
                else {
                    // @@@ OOMEs will occur for LongStream.range(0, Long.MAX_VALUE).filter(i -> true).limit(n)
                    //     when n * parallelismLevel is sufficiently large.
                    //     Need to adjust the target size of splitting for the
                    //     SliceTask from say (size / k) to say min(size / k, 1 << 14)
                    //     This will limit the size of the buffers created at the leaf nodes
                    //     cancellation will be more aggressive cancelling later tasks
                    //     if the target slice size has been reached from a given task,
                    //     cancellation should also clear local results if any
                    return new SliceTask<>(this, helper, spliterator, Nodes.castingArray(), skip, limit).
                            invoke().spliterator();
                }
            }

            @Override
            <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper,
                                              Spliterator<P_IN> spliterator,
                                              IntFunction<T[]> generator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // Because the pipeline is SIZED the slice spliterator
                    // can be created from the source, this requires matching
                    // to shape of the source, and is potentially more efficient
                    // than creating the slice spliterator from the pipeline
                    // wrapping spliterator
                    Spliterator<P_IN> s = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    return Nodes.collect(helper, s, true, generator);
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    Spliterator<T> s =  unorderedSkipLimitSpliterator(
                            helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                    // Collect using this pipeline, which is empty and therefore
                    // can be used with the pipeline wrapping spliterator
                    // Note that we cannot create a slice spliterator from
                    // the source spliterator if the pipeline is not SIZED
                    return Nodes.collect(this, s, true, generator);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, generator, skip, limit).
                            invoke();
                }
            }

            @Override
            Sink<T> opWrapSink(int flags, Sink<T> sink) {
                return new Sink.ChainedReference<>(sink) {
                    long n = skip;
                    long m = normalizedLimit;

                    @Override
                    public void begin(long size) {
                        downstream.begin(calcSize(size, skip, m));
                    }

                    @Override
                    public void accept(T t) {
                        if (n == 0) {
                            if (m > 0) {
                                m--;
                                downstream.accept(t);
                            }
                        }
                        else {
                            n--;
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return m == 0 || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "slice" operation to the provided IntStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream An IntStream
     * @param skip The number of elements to skip.  Must be >= 0.
     * @param limit The maximum size of the resulting stream, or -1 if no limit
     *        is to be imposed
     */
    public static IntStream makeInt(AbstractPipeline<?, Integer, ?> upstream,
                                    long skip, long limit) {
        if (skip < 0)
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        long normalizedLimit = limit >= 0 ? limit : Long.MAX_VALUE;

        return new IntPipeline.StatefulOp<Integer>(upstream, StreamShape.INT_VALUE,
                                                   flags(limit)) {
            @Override
            long exactOutputSize(long previousSize) {
                return calcSize(previousSize, skip, normalizedLimit);
            }

            Spliterator.OfInt unorderedSkipLimitSpliterator(
                    Spliterator.OfInt s, long skip, long limit, long sizeIfKnown) {
                if (skip <= sizeIfKnown) {
                    // Use just the limit if the number of elements
                    // to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                return new StreamSpliterators.UnorderedSliceSpliterator.OfInt(s, skip, limit);
            }

            @Override
            <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper,
                                                               Spliterator<P_IN> spliterator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    return new StreamSpliterators.SliceSpliterator.OfInt(
                            (Spliterator.OfInt) helper.wrapSpliterator(spliterator),
                            skip,
                            calcSliceFence(skip, limit));
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return unorderedSkipLimitSpliterator(
                            (Spliterator.OfInt) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, Integer[]::new, skip, limit).
                            invoke().spliterator();
                }
            }

            @Override
            <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper,
                                                    Spliterator<P_IN> spliterator,
                                                    IntFunction<Integer[]> generator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // Because the pipeline is SIZED the slice spliterator
                    // can be created from the source, this requires matching
                    // to shape of the source, and is potentially more efficient
                    // than creating the slice spliterator from the pipeline
                    // wrapping spliterator
                    Spliterator<P_IN> s = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    return Nodes.collectInt(helper, s, true);
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    Spliterator.OfInt s =  unorderedSkipLimitSpliterator(
                            (Spliterator.OfInt) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                    // Collect using this pipeline, which is empty and therefore
                    // can be used with the pipeline wrapping spliterator
                    // Note that we cannot create a slice spliterator from
                    // the source spliterator if the pipeline is not SIZED
                    return Nodes.collectInt(this, s, true);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, generator, skip, limit).
                            invoke();
                }
            }

            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<>(sink) {
                    long n = skip;
                    long m = normalizedLimit;

                    @Override
                    public void begin(long size) {
                        downstream.begin(calcSize(size, skip, m));
                    }

                    @Override
                    public void accept(int t) {
                        if (n == 0) {
                            if (m > 0) {
                                m--;
                                downstream.accept(t);
                            }
                        }
                        else {
                            n--;
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return m == 0 || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "slice" operation to the provided LongStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream A LongStream
     * @param skip The number of elements to skip.  Must be >= 0.
     * @param limit The maximum size of the resulting stream, or -1 if no limit
     *        is to be imposed
     */
    public static LongStream makeLong(AbstractPipeline<?, Long, ?> upstream,
                                      long skip, long limit) {
        if (skip < 0)
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        long normalizedLimit = limit >= 0 ? limit : Long.MAX_VALUE;

        return new LongPipeline.StatefulOp<Long>(upstream, StreamShape.LONG_VALUE,
                                                 flags(limit)) {
            @Override
            long exactOutputSize(long previousSize) {
                return calcSize(previousSize, skip, normalizedLimit);
            }

            Spliterator.OfLong unorderedSkipLimitSpliterator(
                    Spliterator.OfLong s, long skip, long limit, long sizeIfKnown) {
                if (skip <= sizeIfKnown) {
                    // Use just the limit if the number of elements
                    // to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                return new StreamSpliterators.UnorderedSliceSpliterator.OfLong(s, skip, limit);
            }

            @Override
            <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper,
                                                            Spliterator<P_IN> spliterator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    return new StreamSpliterators.SliceSpliterator.OfLong(
                            (Spliterator.OfLong) helper.wrapSpliterator(spliterator),
                            skip,
                            calcSliceFence(skip, limit));
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return unorderedSkipLimitSpliterator(
                            (Spliterator.OfLong) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, Long[]::new, skip, limit).
                            invoke().spliterator();
                }
            }

            @Override
            <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper,
                                                 Spliterator<P_IN> spliterator,
                                                 IntFunction<Long[]> generator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // Because the pipeline is SIZED the slice spliterator
                    // can be created from the source, this requires matching
                    // to shape of the source, and is potentially more efficient
                    // than creating the slice spliterator from the pipeline
                    // wrapping spliterator
                    Spliterator<P_IN> s = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    return Nodes.collectLong(helper, s, true);
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    Spliterator.OfLong s =  unorderedSkipLimitSpliterator(
                            (Spliterator.OfLong) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                    // Collect using this pipeline, which is empty and therefore
                    // can be used with the pipeline wrapping spliterator
                    // Note that we cannot create a slice spliterator from
                    // the source spliterator if the pipeline is not SIZED
                    return Nodes.collectLong(this, s, true);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, generator, skip, limit).
                            invoke();
                }
            }

            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<>(sink) {
                    long n = skip;
                    long m = normalizedLimit;

                    @Override
                    public void begin(long size) {
                        downstream.begin(calcSize(size, skip, m));
                    }

                    @Override
                    public void accept(long t) {
                        if (n == 0) {
                            if (m > 0) {
                                m--;
                                downstream.accept(t);
                            }
                        }
                        else {
                            n--;
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return m == 0 || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    /**
     * Appends a "slice" operation to the provided DoubleStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream A DoubleStream
     * @param skip The number of elements to skip.  Must be >= 0.
     * @param limit The maximum size of the resulting stream, or -1 if no limit
     *        is to be imposed
     */
    public static DoubleStream makeDouble(AbstractPipeline<?, Double, ?> upstream,
                                          long skip, long limit) {
        if (skip < 0)
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        long normalizedLimit = limit >= 0 ? limit : Long.MAX_VALUE;

        return new DoublePipeline.StatefulOp<Double>(upstream, StreamShape.DOUBLE_VALUE,
                                                     flags(limit)) {
            @Override
            long exactOutputSize(long previousSize) {
                return calcSize(previousSize, skip, normalizedLimit);
            }

            Spliterator.OfDouble unorderedSkipLimitSpliterator(
                    Spliterator.OfDouble s, long skip, long limit, long sizeIfKnown) {
                if (skip <= sizeIfKnown) {
                    // Use just the limit if the number of elements
                    // to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                return new StreamSpliterators.UnorderedSliceSpliterator.OfDouble(s, skip, limit);
            }

            @Override
            <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper,
                                                              Spliterator<P_IN> spliterator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    return new StreamSpliterators.SliceSpliterator.OfDouble(
                            (Spliterator.OfDouble) helper.wrapSpliterator(spliterator),
                            skip,
                            calcSliceFence(skip, limit));
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return unorderedSkipLimitSpliterator(
                            (Spliterator.OfDouble) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, Double[]::new, skip, limit).
                            invoke().spliterator();
                }
            }

            @Override
            <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper,
                                                   Spliterator<P_IN> spliterator,
                                                   IntFunction<Double[]> generator) {
                long size = helper.exactOutputSizeIfKnown(spliterator);
                if (size > 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // Because the pipeline is SIZED the slice spliterator
                    // can be created from the source, this requires matching
                    // to shape of the source, and is potentially more efficient
                    // than creating the slice spliterator from the pipeline
                    // wrapping spliterator
                    Spliterator<P_IN> s = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    return Nodes.collectDouble(helper, s, true);
                } else if (!StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    Spliterator.OfDouble s =  unorderedSkipLimitSpliterator(
                            (Spliterator.OfDouble) helper.wrapSpliterator(spliterator),
                            skip, limit, size);
                    // Collect using this pipeline, which is empty and therefore
                    // can be used with the pipeline wrapping spliterator
                    // Note that we cannot create a slice spliterator from
                    // the source spliterator if the pipeline is not SIZED
                    return Nodes.collectDouble(this, s, true);
                }
                else {
                    return new SliceTask<>(this, helper, spliterator, generator, skip, limit).
                            invoke();
                }
            }

            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<>(sink) {
                    long n = skip;
                    long m = normalizedLimit;

                    @Override
                    public void begin(long size) {
                        downstream.begin(calcSize(size, skip, m));
                    }

                    @Override
                    public void accept(double t) {
                        if (n == 0) {
                            if (m > 0) {
                                m--;
                                downstream.accept(t);
                            }
                        }
                        else {
                            n--;
                        }
                    }

                    @Override
                    public boolean cancellationRequested() {
                        return m == 0 || downstream.cancellationRequested();
                    }
                };
            }
        };
    }

    private static int flags(long limit) {
        return StreamOpFlag.IS_SIZE_ADJUSTING | ((limit != -1) ? StreamOpFlag.IS_SHORT_CIRCUIT : 0);
    }

    /**
     * {@code ForkJoinTask} implementing slice computation.
     *
     * @param <P_IN> Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    @SuppressWarnings("serial")
    private static final class SliceTask<P_IN, P_OUT>
            extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, SliceTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> op;
        private final IntFunction<P_OUT[]> generator;
        private final long targetOffset, targetSize;
        private long thisNodeSize;

        private volatile boolean completed;

        SliceTask(AbstractPipeline<P_OUT, P_OUT, ?> op,
                  PipelineHelper<P_OUT> helper,
                  Spliterator<P_IN> spliterator,
                  IntFunction<P_OUT[]> generator,
                  long offset, long size) {
            super(helper, spliterator);
            this.op = op;
            this.generator = generator;
            this.targetOffset = offset;
            this.targetSize = size;
        }

        SliceTask(SliceTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
            this.generator = parent.generator;
            this.targetOffset = parent.targetOffset;
            this.targetSize = parent.targetSize;
        }

        @Override
        protected SliceTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new SliceTask<>(this, spliterator);
        }

        @Override
        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(op.getOutputShape());
        }

        @Override
        protected final Node<P_OUT> doLeaf() {
            if (isRoot()) {
                long sizeIfKnown = StreamOpFlag.SIZED.isPreserved(op.sourceOrOpFlags)
                                   ? op.exactOutputSizeIfKnown(spliterator)
                                   : -1;
                final Node.Builder<P_OUT> nb = op.makeNodeBuilder(sizeIfKnown, generator);
                Sink<P_OUT> opSink = op.opWrapSink(helper.getStreamAndOpFlags(), nb);
                helper.copyIntoWithCancel(helper.wrapSink(opSink), spliterator);
                // There is no need to truncate since the op performs the
                // skipping and limiting of elements
                return nb.build();
            }
            else {
                final Node.Builder<P_OUT> nb = op.makeNodeBuilder(-1, generator);
                if (targetOffset == 0) { // limit only
                    Sink<P_OUT> opSink = op.opWrapSink(helper.getStreamAndOpFlags(), nb);
                    helper.copyIntoWithCancel(helper.wrapSink(opSink), spliterator);
                }
                else {
                    helper.wrapAndCopyInto(nb, spliterator);
                }
                Node<P_OUT> node = nb.build();
                thisNodeSize = node.count();
                completed = true;
                spliterator = null;
                return node;
            }
        }

        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            if (!isLeaf()) {
                Node<P_OUT> result;
                thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                if (canceled) {
                    thisNodeSize = 0;
                    result = getEmptyResult();
                }
                else if (thisNodeSize == 0)
                    result = getEmptyResult();
                else if (leftChild.thisNodeSize == 0)
                    result = rightChild.getLocalResult();
                else {
                    result = Nodes.conc(op.getOutputShape(),
                                        leftChild.getLocalResult(), rightChild.getLocalResult());
                }
                setLocalResult(isRoot() ? doTruncate(result) : result);
                completed = true;
            }
            if (targetSize >= 0
                && !isRoot()
                && isLeftCompleted(targetOffset + targetSize))
                    cancelLaterNodes();

            super.onCompletion(caller);
        }

        @Override
        protected void cancel() {
            super.cancel();
            if (completed)
                setLocalResult(getEmptyResult());
        }

        private Node<P_OUT> doTruncate(Node<P_OUT> input) {
            long to = targetSize >= 0 ? Math.min(input.count(), targetOffset + targetSize) : thisNodeSize;
            return input.truncate(targetOffset, to, generator);
        }

        /**
         * Determine if the number of completed elements in this node and nodes
         * to the left of this node is greater than or equal to the target size.
         *
         * @param target the target size
         * @return true if the number of elements is greater than or equal to
         *         the target size, otherwise false.
         */
        private boolean isLeftCompleted(long target) {
            long size = completed ? thisNodeSize : completedSize(target);
            if (size >= target)
                return true;
            for (SliceTask<P_IN, P_OUT> parent = getParent(), node = this;
                 parent != null;
                 node = parent, parent = parent.getParent()) {
                if (node == parent.rightChild) {
                    SliceTask<P_IN, P_OUT> left = parent.leftChild;
                    if (left != null) {
                        size += left.completedSize(target);
                        if (size >= target)
                            return true;
                    }
                }
            }
            return size >= target;
        }

        /**
         * Compute the number of completed elements in this node.
         * <p>
         * Computation terminates if all nodes have been processed or the
         * number of completed elements is greater than or equal to the target
         * size.
         *
         * @param target the target size
         * @return the number of completed elements
         */
        private long completedSize(long target) {
            if (completed)
                return thisNodeSize;
            else {
                SliceTask<P_IN, P_OUT> left = leftChild;
                SliceTask<P_IN, P_OUT> right = rightChild;
                if (left == null || right == null) {
                    // must be completed
                    return thisNodeSize;
                }
                else {
                    long leftSize = left.completedSize(target);
                    return (leftSize >= target) ? leftSize : leftSize + right.completedSize(target);
                }
            }
        }
    }
}
