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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Describes a test data set for use in stream tests */
public interface TestData<T, S extends BaseStream<T, S>>
        extends Iterable<T> {

    default int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Iterator<T> iterator() {
        return Spliterators.iterator(spliterator());
    }

    Spliterator<T> spliterator();

    default boolean isOrdered() {
        return spliterator().hasCharacteristics(Spliterator.ORDERED);
    }

    StreamShape getShape();

    default <A extends Collection<? super T>> A into(A target) {
        spliterator().forEachRemaining(target::add);
        return target;
    }

    S stream();

    S parallelStream();

    public interface OfRef<T> extends TestData<T, Stream<T>> { }

    public interface OfInt extends TestData<Integer, IntStream> { }

    public interface OfLong extends TestData<Long, LongStream> { }

    public interface OfDouble extends TestData<Double, DoubleStream> { }

    // @@@ Temporary garbage class to avoid triggering bugs with lambdas in static methods in interfaces
    public static class Factory {
        public static <T> OfRef<T> ofArray(String name, T[] array) {
            return new AbstractTestData.RefTestData<>(name, array, Arrays::stream, a -> Arrays.stream(a).parallel(),
                                                      Arrays::spliterator, a -> a.length);
        }

        public static <T> OfRef<T> ofCollection(String name, Collection<T> collection) {
            return new AbstractTestData.RefTestData<>(name, collection, Collection::stream, Collection::parallelStream,
                                                      Collection::spliterator, Collection::size);
        }

        public static <T> OfRef<T> ofSpinedBuffer(String name, SpinedBuffer<T> buffer) {
            return new AbstractTestData.RefTestData<>(name, buffer,
                                                      b -> StreamSupport.stream(b.spliterator(), false),
                                                      b -> StreamSupport.stream(b.spliterator(), true),
                                                      SpinedBuffer::spliterator,
                                                      b -> (int) b.count());
        }

        public static <T> OfRef<T> ofSupplier(String name, Supplier<Stream<T>> supplier) {
            return new AbstractTestData.RefTestData<>(name, supplier,
                                                      Supplier::get,
                                                      s -> s.get().parallel(),
                                                      s -> s.get().spliterator(),
                                                      s -> (int) s.get().spliterator().getExactSizeIfKnown());
        }

        public static <T> OfRef<T> ofRefNode(String name, Node<T> node) {
            return new AbstractTestData.RefTestData<>(name, node,
                                                      n -> StreamSupport.stream(n::spliterator, Spliterator.SIZED | Spliterator.ORDERED, false),
                                                      n -> StreamSupport.stream(n::spliterator, Spliterator.SIZED | Spliterator.ORDERED, true),
                                                      Node::spliterator,
                                                      n -> (int) n.count());
        }

        // int factories
        public static <T> OfInt ofArray(String name, int[] array) {
            return new AbstractTestData.IntTestData<>(name, array, Arrays::stream, a -> Arrays.stream(a).parallel(),
                                                      Arrays::spliterator, a -> a.length);
        }

        public static OfInt ofSpinedBuffer(String name, SpinedBuffer.OfInt buffer) {
            return new AbstractTestData.IntTestData<>(name, buffer,
                                                      b -> StreamSupport.intStream(b.spliterator(), false),
                                                      b -> StreamSupport.intStream(b.spliterator(), true),
                                                      SpinedBuffer.OfInt::spliterator,
                                                      b -> (int) b.count());
        }

        public static OfInt ofIntSupplier(String name, Supplier<IntStream> supplier) {
            return new AbstractTestData.IntTestData<>(name, supplier,
                                                      Supplier::get,
                                                      s -> s.get().parallel(),
                                                      s -> s.get().spliterator(),
                                                      s -> (int) s.get().spliterator().getExactSizeIfKnown());
        }

        public static OfInt ofNode(String name, Node.OfInt node) {
            int characteristics = Spliterator.SIZED | Spliterator.ORDERED;
            return new AbstractTestData.IntTestData<>(name, node,
                                                      n -> StreamSupport.intStream(n::spliterator, characteristics, false),
                                                      n -> StreamSupport.intStream(n::spliterator, characteristics, true),
                                                      Node.OfInt::spliterator,
                                                      n -> (int) n.count());
        }

        // long factories
        public static <T> OfLong ofArray(String name, long[] array) {
            return new AbstractTestData.LongTestData<>(name, array, Arrays::stream, a -> Arrays.stream(a).parallel(),
                                                       Arrays::spliterator, a -> a.length);
        }

        public static OfLong ofSpinedBuffer(String name, SpinedBuffer.OfLong buffer) {
            return new AbstractTestData.LongTestData<>(name, buffer,
                                                      b -> StreamSupport.longStream(b.spliterator(), false),
                                                      b -> StreamSupport.longStream(b.spliterator(), true),
                                                      SpinedBuffer.OfLong::spliterator,
                                                      b -> (int) b.count());
        }

        public static OfLong ofLongSupplier(String name, Supplier<LongStream> supplier) {
            return new AbstractTestData.LongTestData<>(name, supplier,
                                                      Supplier::get,
                                                      s -> s.get().parallel(),
                                                      s -> s.get().spliterator(),
                                                      s -> (int) s.get().spliterator().getExactSizeIfKnown());
        }

        public static OfLong ofNode(String name, Node.OfLong node) {
            int characteristics = Spliterator.SIZED | Spliterator.ORDERED;
            return new AbstractTestData.LongTestData<>(name, node,
                                                      n -> StreamSupport.longStream(n::spliterator, characteristics, false),
                                                      n -> StreamSupport.longStream(n::spliterator, characteristics, true),
                                                      Node.OfLong::spliterator,
                                                      n -> (int) n.count());
        }

        // double factories
        public static <T> OfDouble ofArray(String name, double[] array) {
            return new AbstractTestData.DoubleTestData<>(name, array, Arrays::stream, a -> Arrays.stream(a).parallel(),
                                                         Arrays::spliterator, a -> a.length);
        }

        public static OfDouble ofSpinedBuffer(String name, SpinedBuffer.OfDouble buffer) {
            return new AbstractTestData.DoubleTestData<>(name, buffer,
                                                         b -> StreamSupport.doubleStream(b.spliterator(), false),
                                                         b -> StreamSupport.doubleStream(b.spliterator(), true),
                                                         SpinedBuffer.OfDouble::spliterator,
                                                         b -> (int) b.count());
        }

        public static OfDouble ofDoubleSupplier(String name, Supplier<DoubleStream> supplier) {
            return new AbstractTestData.DoubleTestData<>(name, supplier,
                                                         Supplier::get,
                                                         s -> s.get().parallel(),
                                                         s -> s.get().spliterator(),
                                                         s -> (int) s.get().spliterator().getExactSizeIfKnown());
        }

        public static OfDouble ofNode(String name, Node.OfDouble node) {
            int characteristics = Spliterator.SIZED | Spliterator.ORDERED;
            return new AbstractTestData.DoubleTestData<>(name, node,
                                                         n -> StreamSupport.doubleStream(n::spliterator, characteristics, false),
                                                         n -> StreamSupport.doubleStream(n::spliterator, characteristics, true),
                                                         Node.OfDouble::spliterator,
                                                         n -> (int) n.count());
        }
    }


    abstract class AbstractTestData<T, S extends BaseStream<T, S>,
            T_STATE,
                                    T_SPLITR extends Spliterator<T>>
            implements TestData<T, S> {
        private final String name;
        private final StreamShape shape;
        protected final T_STATE state;
        private final ToIntFunction<T_STATE> sizeFn;
        private final Function<T_STATE, S> streamFn;
        private final Function<T_STATE, S> parStreamFn;
        private final Function<T_STATE, T_SPLITR> splitrFn;

        AbstractTestData(String name,
                         StreamShape shape,
                         T_STATE state,
                         Function<T_STATE, S> streamFn,
                         Function<T_STATE, S> parStreamFn,
                         Function<T_STATE, T_SPLITR> splitrFn,
                         ToIntFunction<T_STATE> sizeFn) {
            this.name = name;
            this.shape = shape;
            this.state = state;
            this.streamFn = streamFn;
            this.parStreamFn = parStreamFn;
            this.splitrFn = splitrFn;
            this.sizeFn = sizeFn;
        }

        @Override
        public StreamShape getShape() {
            return shape;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + name + "]";
        }

        @Override
        public int size() {
            return sizeFn.applyAsInt(state);
        }

        @Override
        public T_SPLITR spliterator() {
            return splitrFn.apply(state);
        }

        @Override
        public S stream() {
            return streamFn.apply(state);
        }

        @Override
        public S parallelStream() {
            return parStreamFn.apply(state);
        }

        public static class RefTestData<T, I>
                extends AbstractTestData<T, Stream<T>, I, Spliterator<T>>
                implements TestData.OfRef<T> {

            protected RefTestData(String name,
                                  I state,
                                  Function<I, Stream<T>> streamFn,
                                  Function<I, Stream<T>> parStreamFn,
                                  Function<I, Spliterator<T>> splitrFn,
                                  ToIntFunction<I> sizeFn) {
                super(name, StreamShape.REFERENCE, state, streamFn, parStreamFn, splitrFn, sizeFn);
            }

        }

        static class IntTestData<I>
                extends AbstractTestData<Integer, IntStream, I, Spliterator.OfInt>
                implements TestData.OfInt {

            protected IntTestData(String name,
                                  I state,
                                  Function<I, IntStream> streamFn,
                                  Function<I, IntStream> parStreamFn,
                                  Function<I, Spliterator.OfInt> splitrFn,
                                  ToIntFunction<I> sizeFn) {
                super(name, StreamShape.INT_VALUE, state, streamFn, parStreamFn, splitrFn, sizeFn);
            }

            @Override
            public PrimitiveIterator.OfInt iterator() {
                return Spliterators.iterator(spliterator());
            }

            @Override
            public <A extends Collection<? super Integer>> A into(A target) {
                spliterator().forEachRemaining((IntConsumer) target::add);
                return target;
            }
        }

        static class LongTestData<I>
                extends AbstractTestData<Long, LongStream, I, Spliterator.OfLong>
                implements TestData.OfLong {

            protected LongTestData(String name,
                                   I state,
                                   Function<I, LongStream> streamFn,
                                   Function<I, LongStream> parStreamFn,
                                   Function<I, Spliterator.OfLong> splitrFn,
                                   ToIntFunction<I> sizeFn) {
                super(name, StreamShape.LONG_VALUE, state, streamFn, parStreamFn, splitrFn, sizeFn);
            }

            @Override
            public PrimitiveIterator.OfLong iterator() {
                return Spliterators.iterator(spliterator());
            }

            @Override
            public <A extends Collection<? super Long>> A into(A target) {
                spliterator().forEachRemaining((LongConsumer) target::add);
                return target;
            }
        }

        static class DoubleTestData<I>
                extends AbstractTestData<Double, DoubleStream, I, Spliterator.OfDouble>
                implements OfDouble {

            protected DoubleTestData(String name,
                                     I state,
                                     Function<I, DoubleStream> streamFn,
                                     Function<I, DoubleStream> parStreamFn,
                                     Function<I, Spliterator.OfDouble> splitrFn,
                                     ToIntFunction<I> sizeFn) {
                super(name, StreamShape.DOUBLE_VALUE, state, streamFn, parStreamFn, splitrFn, sizeFn);
            }

            @Override
            public PrimitiveIterator.OfDouble iterator() {
                return Spliterators.iterator(spliterator());
            }

            @Override
            public <A extends Collection<? super Double>> A into(A target) {
                spliterator().forEachRemaining((DoubleConsumer) target::add);
                return target;
            }
        }
    }
}
