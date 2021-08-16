/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import static java.util.stream.Collectors.*;

public final class DefaultMethodStreams {

    static {
        // Verify that default methods are not overridden
        verify(DefaultMethodRefStream.class);
        verify(DefaultMethodIntStream.class);
        verify(DefaultMethodLongStream.class);
        verify(DefaultMethodDoubleStream.class);
    }

    static void verify(Class<?> del) {
        // Find the stream interface
        Class<?> s = Stream.of(del.getInterfaces())
                .filter(c -> BaseStream.class.isAssignableFrom(c))
                .findFirst().get();

        // Get all default methods on the stream class
        Set<String> dms = Stream.of(s.getMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> !m.isBridge())
                .filter(Method::isDefault)
                .map(Method::getName)
                .collect(toSet());

        // Get all methods on the delegating class
        Set<String> ims = Stream.of(del.getMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getDeclaringClass() == del)
                .map(Method::getName)
                .collect(toSet());

        if (ims.stream().anyMatch(dms::contains)) {
            throw new AssertionError(String.format("%s overrides default methods of %s\n", del, s));
        }
    }

    /**
     * Creates a stream that for the next operation either delegates to
     * a default method on {@link Stream}, if present for that operation,
     * otherwise delegates to an underlying stream.
     *
     * @param s the underlying stream to be delegated to for non-default
     * methods.
     * @param <T> the type of the stream elements
     * @return the delegating stream
     */
    public static <T> Stream<T> delegateTo(Stream<T> s) {
        return new DefaultMethodRefStream<>(s);
    }

    /**
     * Creates a stream that for the next operation either delegates to
     * a default method on {@link IntStream}, if present for that operation,
     * otherwise delegates to an underlying stream.
     *
     * @param s the underlying stream to be delegated to for non-default
     * methods.
     * @return the delegating stream
     */
    public static IntStream delegateTo(IntStream s) {
        return new DefaultMethodIntStream(s);
    }

    /**
     * Creates a stream that for the next operation either delegates to
     * a default method on {@link LongStream}, if present for that operation,
     * otherwise delegates to an underlying stream.
     *
     * @param s the underlying stream to be delegated to for non-default
     * methods.
     * @return the delegating stream
     */
    public static LongStream delegateTo(LongStream s) {
        return new DefaultMethodLongStream(s);
    }

    /**
     * Creates a stream that for the next operation either delegates to
     * a default method on {@link DoubleStream}, if present for that operation,
     * otherwise delegates to an underlying stream.
     *
     * @param s the underlying stream to be delegated to for non-default
     * methods.
     * @return the delegating stream
     */
    public static DoubleStream delegateTo(DoubleStream s) {
        return new DefaultMethodDoubleStream(s);
    }

    /**
     * A stream that delegates the next operation to a default method, if
     * present, or to the same operation of an underlying stream.
     *
     * @param <T> the type of the stream elements
     */
    static final class DefaultMethodRefStream<T> implements Stream<T> {
        final Stream<T> s;

        DefaultMethodRefStream(Stream<T> s) {
            this.s = s;
        }


        // Delegating non-default methods

        @Override
        public Stream<T> filter(Predicate<? super T> predicate) {
            return s.filter(predicate);
        }

        @Override
        public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
            return s.map(mapper);
        }

        @Override
        public IntStream mapToInt(ToIntFunction<? super T> mapper) {
            return s.mapToInt(mapper);
        }

        @Override
        public LongStream mapToLong(ToLongFunction<? super T> mapper) {
            return s.mapToLong(mapper);
        }

        @Override
        public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
            return s.mapToDouble(mapper);
        }

        @Override
        public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
            return s.flatMap(mapper);
        }

        @Override
        public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
            return s.flatMapToInt(mapper);
        }

        @Override
        public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
            return s.flatMapToLong(mapper);
        }

        @Override
        public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
            return s.flatMapToDouble(mapper);
        }

        @Override
        public Stream<T> distinct() {
            return s.distinct();
        }

        @Override
        public Stream<T> sorted() {
            return s.sorted();
        }

        @Override
        public Stream<T> sorted(Comparator<? super T> comparator) {
            return s.sorted(comparator);
        }

        @Override
        public Stream<T> peek(Consumer<? super T> action) {
            return s.peek(action);
        }

        @Override
        public Stream<T> limit(long maxSize) {
            return s.limit(maxSize);
        }

        @Override
        public Stream<T> skip(long n) {
            return s.skip(n);
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            s.forEach(action);
        }

        @Override
        public void forEachOrdered(Consumer<? super T> action) {
            s.forEachOrdered(action);
        }

        @Override
        public Object[] toArray() {
            return s.toArray();
        }

        @Override
        public <A> A[] toArray(IntFunction<A[]> generator) {
            return s.toArray(generator);
        }

        @Override
        public T reduce(T identity, BinaryOperator<T> accumulator) {
            return s.reduce(identity, accumulator);
        }

        @Override
        public Optional<T> reduce(BinaryOperator<T> accumulator) {
            return s.reduce(accumulator);
        }

        @Override
        public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
            return s.reduce(identity, accumulator, combiner);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
            return s.collect(supplier, accumulator, combiner);
        }

        @Override
        public <R, A> R collect(Collector<? super T, A, R> collector) {
            return s.collect(collector);
        }

        @Override
        public Optional<T> min(Comparator<? super T> comparator) {
            return s.min(comparator);
        }

        @Override
        public Optional<T> max(Comparator<? super T> comparator) {
            return s.max(comparator);
        }

        @Override
        public long count() {
            return s.count();
        }

        @Override
        public boolean anyMatch(Predicate<? super T> predicate) {
            return s.anyMatch(predicate);
        }

        @Override
        public boolean allMatch(Predicate<? super T> predicate) {
            return s.allMatch(predicate);
        }

        @Override
        public boolean noneMatch(Predicate<? super T> predicate) {
            return s.noneMatch(predicate);
        }

        @Override
        public Optional<T> findFirst() {
            return s.findFirst();
        }

        @Override
        public Optional<T> findAny() {
            return s.findAny();
        }

        @Override
        public Iterator<T> iterator() {
            return s.iterator();
        }

        @Override
        public Spliterator<T> spliterator() {
            return s.spliterator();
        }

        @Override
        public boolean isParallel() {
            return s.isParallel();
        }

        @Override
        public Stream<T> sequential() {
            return s.sequential();
        }

        @Override
        public Stream<T> parallel() {
            return s.parallel();
        }

        @Override
        public Stream<T> unordered() {
            return s.unordered();
        }

        @Override
        public Stream<T> onClose(Runnable closeHandler) {
            return s.onClose(closeHandler);
        }

        @Override
        public void close() {
            s.close();
        }
    }

    static final class DefaultMethodIntStream implements IntStream {
        final IntStream s;

        public DefaultMethodIntStream(IntStream s) {
            this.s = s;
        }


        // Delegating non-default methods

        @Override
        public IntStream filter(IntPredicate predicate) {
            return s.filter(predicate);
        }

        @Override
        public IntStream map(IntUnaryOperator mapper) {
            return s.map(mapper);
        }

        @Override
        public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
            return s.mapToObj(mapper);
        }

        @Override
        public LongStream mapToLong(IntToLongFunction mapper) {
            return s.mapToLong(mapper);
        }

        @Override
        public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
            return s.mapToDouble(mapper);
        }

        @Override
        public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
            return s.flatMap(mapper);
        }

        @Override
        public IntStream distinct() {
            return s.distinct();
        }

        @Override
        public IntStream sorted() {
            return s.sorted();
        }

        @Override
        public IntStream peek(IntConsumer action) {
            return s.peek(action);
        }

        @Override
        public IntStream limit(long maxSize) {
            return s.limit(maxSize);
        }

        @Override
        public IntStream skip(long n) {
            return s.skip(n);
        }

        @Override
        public void forEach(IntConsumer action) {
            s.forEach(action);
        }

        @Override
        public void forEachOrdered(IntConsumer action) {
            s.forEachOrdered(action);
        }

        @Override
        public int[] toArray() {
            return s.toArray();
        }

        @Override
        public int reduce(int identity, IntBinaryOperator op) {
            return s.reduce(identity, op);
        }

        @Override
        public OptionalInt reduce(IntBinaryOperator op) {
            return s.reduce(op);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return s.collect(supplier, accumulator, combiner);
        }

        @Override
        public int sum() {
            return s.sum();
        }

        @Override
        public OptionalInt min() {
            return s.min();
        }

        @Override
        public OptionalInt max() {
            return s.max();
        }

        @Override
        public long count() {
            return s.count();
        }

        @Override
        public OptionalDouble average() {
            return s.average();
        }

        @Override
        public IntSummaryStatistics summaryStatistics() {
            return s.summaryStatistics();
        }

        @Override
        public boolean anyMatch(IntPredicate predicate) {
            return s.anyMatch(predicate);
        }

        @Override
        public boolean allMatch(IntPredicate predicate) {
            return s.allMatch(predicate);
        }

        @Override
        public boolean noneMatch(IntPredicate predicate) {
            return s.noneMatch(predicate);
        }

        @Override
        public OptionalInt findFirst() {
            return s.findFirst();
        }

        @Override
        public OptionalInt findAny() {
            return s.findAny();
        }

        @Override
        public LongStream asLongStream() {
            return s.asLongStream();
        }

        @Override
        public DoubleStream asDoubleStream() {
            return s.asDoubleStream();
        }

        @Override
        public Stream<Integer> boxed() {
            return s.boxed();
        }

        @Override
        public IntStream sequential() {
            return s.sequential();
        }

        @Override
        public IntStream parallel() {
            return s.parallel();
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return s.iterator();
        }

        @Override
        public Spliterator.OfInt spliterator() {
            return s.spliterator();
        }

        @Override
        public boolean isParallel() {
            return s.isParallel();
        }

        @Override
        public IntStream unordered() {
            return s.unordered();
        }

        @Override
        public IntStream onClose(Runnable closeHandler) {
            return s.onClose(closeHandler);
        }

        @Override
        public void close() {
            s.close();
        }
    }

    static final class DefaultMethodLongStream implements LongStream {
        final LongStream s;

        public DefaultMethodLongStream(LongStream s) {
            this.s = s;
        }


        // Delegating non-default methods

        @Override
        public void forEach(LongConsumer action) {
            s.forEach(action);
        }

        @Override
        public LongStream filter(LongPredicate predicate) {
            return s.filter(predicate);
        }

        @Override
        public LongStream map(LongUnaryOperator mapper) {
            return s.map(mapper);
        }

        @Override
        public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
            return s.mapToObj(mapper);
        }

        @Override
        public IntStream mapToInt(LongToIntFunction mapper) {
            return s.mapToInt(mapper);
        }

        @Override
        public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
            return s.mapToDouble(mapper);
        }

        @Override
        public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
            return s.flatMap(mapper);
        }

        @Override
        public LongStream distinct() {
            return s.distinct();
        }

        @Override
        public LongStream sorted() {
            return s.sorted();
        }

        @Override
        public LongStream peek(LongConsumer action) {
            return s.peek(action);
        }

        @Override
        public LongStream limit(long maxSize) {
            return s.limit(maxSize);
        }

        @Override
        public LongStream skip(long n) {
            return s.skip(n);
        }

        @Override
        public void forEachOrdered(LongConsumer action) {
            s.forEachOrdered(action);
        }

        @Override
        public long[] toArray() {
            return s.toArray();
        }

        @Override
        public long reduce(long identity, LongBinaryOperator op) {
            return s.reduce(identity, op);
        }

        @Override
        public OptionalLong reduce(LongBinaryOperator op) {
            return s.reduce(op);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return s.collect(supplier, accumulator, combiner);
        }

        @Override
        public long sum() {
            return s.sum();
        }

        @Override
        public OptionalLong min() {
            return s.min();
        }

        @Override
        public OptionalLong max() {
            return s.max();
        }

        @Override
        public long count() {
            return s.count();
        }

        @Override
        public OptionalDouble average() {
            return s.average();
        }

        @Override
        public LongSummaryStatistics summaryStatistics() {
            return s.summaryStatistics();
        }

        @Override
        public boolean anyMatch(LongPredicate predicate) {
            return s.anyMatch(predicate);
        }

        @Override
        public boolean allMatch(LongPredicate predicate) {
            return s.allMatch(predicate);
        }

        @Override
        public boolean noneMatch(LongPredicate predicate) {
            return s.noneMatch(predicate);
        }

        @Override
        public OptionalLong findFirst() {
            return s.findFirst();
        }

        @Override
        public OptionalLong findAny() {
            return s.findAny();
        }

        @Override
        public DoubleStream asDoubleStream() {
            return s.asDoubleStream();
        }

        @Override
        public Stream<Long> boxed() {
            return s.boxed();
        }

        @Override
        public LongStream sequential() {
            return s.sequential();
        }

        @Override
        public LongStream parallel() {
            return s.parallel();
        }

        @Override
        public PrimitiveIterator.OfLong iterator() {
            return s.iterator();
        }

        @Override
        public Spliterator.OfLong spliterator() {
            return s.spliterator();
        }

        @Override
        public boolean isParallel() {
            return s.isParallel();
        }

        @Override
        public LongStream unordered() {
            return s.unordered();
        }

        @Override
        public LongStream onClose(Runnable closeHandler) {
            return s.onClose(closeHandler);
        }

        @Override
        public void close() {
            s.close();
        }
    }

    static final class DefaultMethodDoubleStream implements DoubleStream {
        final DoubleStream s;

        public DefaultMethodDoubleStream(DoubleStream s) {
            this.s = s;
        }

        @Override
        public DoubleStream filter(DoublePredicate predicate) {
            return s.filter(predicate);
        }

        @Override
        public DoubleStream map(DoubleUnaryOperator mapper) {
            return s.map(mapper);
        }

        @Override
        public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
            return s.mapToObj(mapper);
        }

        @Override
        public IntStream mapToInt(DoubleToIntFunction mapper) {
            return s.mapToInt(mapper);
        }

        @Override
        public LongStream mapToLong(DoubleToLongFunction mapper) {
            return s.mapToLong(mapper);
        }

        @Override
        public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
            return s.flatMap(mapper);
        }

        @Override
        public DoubleStream distinct() {
            return s.distinct();
        }

        @Override
        public DoubleStream sorted() {
            return s.sorted();
        }

        @Override
        public DoubleStream peek(DoubleConsumer action) {
            return s.peek(action);
        }

        @Override
        public DoubleStream limit(long maxSize) {
            return s.limit(maxSize);
        }

        @Override
        public DoubleStream skip(long n) {
            return s.skip(n);
        }

        @Override
        public void forEach(DoubleConsumer action) {
            s.forEach(action);
        }

        @Override
        public void forEachOrdered(DoubleConsumer action) {
            s.forEachOrdered(action);
        }

        @Override
        public double[] toArray() {
            return s.toArray();
        }

        @Override
        public double reduce(double identity, DoubleBinaryOperator op) {
            return s.reduce(identity, op);
        }

        @Override
        public OptionalDouble reduce(DoubleBinaryOperator op) {
            return s.reduce(op);
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            return s.collect(supplier, accumulator, combiner);
        }

        @Override
        public double sum() {
            return s.sum();
        }

        @Override
        public OptionalDouble min() {
            return s.min();
        }

        @Override
        public OptionalDouble max() {
            return s.max();
        }

        @Override
        public long count() {
            return s.count();
        }

        @Override
        public OptionalDouble average() {
            return s.average();
        }

        @Override
        public DoubleSummaryStatistics summaryStatistics() {
            return s.summaryStatistics();
        }

        @Override
        public boolean anyMatch(DoublePredicate predicate) {
            return s.anyMatch(predicate);
        }

        @Override
        public boolean allMatch(DoublePredicate predicate) {
            return s.allMatch(predicate);
        }

        @Override
        public boolean noneMatch(DoublePredicate predicate) {
            return s.noneMatch(predicate);
        }

        @Override
        public OptionalDouble findFirst() {
            return s.findFirst();
        }

        @Override
        public OptionalDouble findAny() {
            return s.findAny();
        }

        @Override
        public Stream<Double> boxed() {
            return s.boxed();
        }

        @Override
        public DoubleStream sequential() {
            return s.sequential();
        }

        @Override
        public DoubleStream parallel() {
            return s.parallel();
        }

        @Override
        public PrimitiveIterator.OfDouble iterator() {
            return s.iterator();
        }

        @Override
        public Spliterator.OfDouble spliterator() {
            return s.spliterator();
        }

        @Override
        public boolean isParallel() {
            return s.isParallel();
        }

        @Override
        public DoubleStream unordered() {
            return s.unordered();
        }

        @Override
        public DoubleStream onClose(Runnable closeHandler) {
            return s.onClose(closeHandler);
        }

        @Override
        public void close() {
            s.close();
        }
    }
}
