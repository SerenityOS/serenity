/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.util.stream;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.DefaultMethodStreams;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.LongStream;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamTestDataProvider;
import java.util.stream.TestData;

/*
 * @test
 * @bug 8071597 8193856
 * @run main/timeout=240
 */
@Test
public class WhileOpTest extends OpTestCase {

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testTakeWhileOps(String name, TestData.OfRef<Integer> data) {
        for (int size : sizes(data.size())) {
            setContext("takeWhile", size);

            testWhileMulti(data,
                           whileResultAsserter(data, WhileOp.Take, e -> e < size),
                           s -> s.takeWhile(e -> e < size),
                           s -> s.takeWhile(e -> e < size),
                           s -> s.takeWhile(e -> e < size),
                           s -> s.takeWhile(e -> e < size));


            testWhileMulti(data,
                           whileResultAsserter(data, WhileOp.Take, e -> e < size / 2),
                           s -> s.takeWhile(e -> e < size).takeWhile(e -> e < size / 2),
                           s -> s.takeWhile(e -> e < size).takeWhile(e -> e < size / 2),
                           s -> s.takeWhile(e -> e < size).takeWhile(e -> e < size / 2),
                           s -> s.takeWhile(e -> e < size).takeWhile(e -> e < size / 2));
        }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testDropWhileOps(String name, TestData.OfRef<Integer> data) {
        for (int size : sizes(data.size())) {
            setContext("dropWhile", size);

            testWhileMulti(data,
                           whileResultAsserter(data, WhileOp.Drop, e -> e < size),
                           s -> s.dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size));

            testWhileMulti(data,
                           whileResultAsserter(data, WhileOp.Drop, e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).dropWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).dropWhile(e -> e < size));
        }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testDropTakeWhileOps(String name, TestData.OfRef<Integer> data) {
        for (int size : sizes(data.size())) {
            setContext("dropWhile", size);

            testWhileMulti(data,
                           whileResultAsserter(data, WhileOp.Undefined, null),
                           s -> s.dropWhile(e -> e < size / 2).takeWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).takeWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).takeWhile(e -> e < size),
                           s -> s.dropWhile(e -> e < size / 2).takeWhile(e -> e < size));
        }
    }

    /**
     * While operation type to be asserted on
     */
    enum WhileOp {
        /**
         * The takeWhile operation
         */
        Take,
        /**
         * The dropWhile operation
         */
        Drop,
        /**
         * The operation(s) are undefined
         */
        Undefined
    }

    /**
     * Create a result asserter for takeWhile or dropWhile operations.
     * <p>
     * If the stream pipeline consists of the takeWhile operation
     * ({@link WhileOp#Take}) or the dropWhile operation ({@link WhileOp#Drop})
     * then specific assertions can be made on the actual result based on the
     * input elements, {@code inputData}, and whether those elements match the
     * predicate, {@code p}, of the operation.
     * <p>
     * If the input elements have an encounter order then the actual result
     * is asserted against the result of operating sequentially on input
     * elements given the predicate and in accordance with the operation
     * semantics. (The actual result whether produced sequentially or in
     * parallel should the same.)
     * <p>
     * If the input elements have no encounter order then an actual result
     * is, for practical purposes, considered non-deterministic.
     * Consider an input list of lists that contains all possible permutations
     * of the input elements, and a output list of lists that is the result of
     * applying the pipeline with the operation sequentially to each input
     * list.
     * Any list in the output lists is a valid result. It's not practical to
     * test in such a manner.
     * For a takeWhile operation the following assertions can be made if
     * only some of the input elements match the predicate (i.e. taking will
     * short-circuit the pipeline):
     * <ol>
     * <li>The set of output elements is a subset of the set of matching
     * input elements</li>
     * <li>The set of output elements and the set of non-matching input
     * element are disjoint</li>
     * </ol>
     * For a dropWhile operation the following assertions can be made:
     * <ol>
     * <li>The set of non-matching input elements is a subset of the set of
     * output elements</li>
     * <li>The set of matching output elements is a subset of the set of
     * matching input elements</li>
     * </ol>
     *
     * @param inputData the elements input into the stream pipeline
     * @param op the operation of the stream pipeline, one of takeWhile,
     * dropWhile, or an undefined set of operations (possibly including
     * two or more takeWhile and/or dropWhile operations, or because
     * the predicate is not stateless).
     * @param p the stateless predicate applied to the operation, ignored if
     * the
     * operation is {@link WhileOp#Undefined}.
     * @param <T> the type of elements
     * @return a result asserter
     */
    private <T> ResultAsserter<Iterable<T>> whileResultAsserter(Iterable<T> inputData,
                                                                WhileOp op,
                                                                Predicate<? super T> p) {
        return (act, exp, ord, par) -> {
            if (par & !ord) {
                List<T> input = new ArrayList<>();
                inputData.forEach(input::add);

                List<T> output = new ArrayList<>();
                act.forEach(output::add);

                if (op == WhileOp.Take) {
                    List<T> matchingInput = new ArrayList<>();
                    List<T> nonMatchingInput = new ArrayList<>();
                    input.forEach(t -> {
                        if (p.test(t))
                            matchingInput.add(t);
                        else
                            nonMatchingInput.add(t);
                    });

                    // If some, not all, elements are taken
                    if (matchingInput.size() < input.size()) {
                        assertTrue(output.size() <= matchingInput.size(),
                                   "Output is larger than the matching input");

                        // The output must be a subset of the matching input
                        assertTrue(matchingInput.containsAll(output),
                                   "Output is not a subset of the matching input");

                        // The output must not contain any non matching elements
                        for (T nonMatching : nonMatchingInput) {
                            assertFalse(output.contains(nonMatching),
                                        "Output and non-matching input are not disjoint");
                        }
                    }
                }
                else if (op == WhileOp.Drop) {
                    List<T> matchingInput = new ArrayList<>();
                    List<T> nonMatchingInput = new ArrayList<>();
                    input.forEach(t -> {
                        if (p.test(t))
                            matchingInput.add(t);
                        else
                            nonMatchingInput.add(t);
                    });

                    // The non matching input must be a subset of output
                    assertTrue(output.containsAll(nonMatchingInput),
                               "Non-matching input is not a subset of the output");

                    // The matching output must be a subset of the matching input
                    List<T> matchingOutput = new ArrayList<>();
                    output.forEach(i -> {
                        if (p.test(i))
                            matchingOutput.add(i);
                    });
                    assertTrue(matchingInput.containsAll(matchingOutput),
                               "Matching output is not a subset of matching input");
                }

                // Note: if there is a combination of takeWhile and dropWhile then specific
                // assertions cannot be performed.
                // All that can be reliably asserted is the output is a subset of the input

                assertTrue(input.containsAll(output));
            }
            else {
                // For specific operations derive expected result from the input
                if (op == WhileOp.Take) {
                    List<T> takeInput = new ArrayList<>();
                    for (T t : inputData) {
                        if (p.test(t))
                            takeInput.add(t);
                        else
                            break;
                    }

                    LambdaTestHelpers.assertContents(act, takeInput);
                }
                else if (op == WhileOp.Drop) {
                    List<T> dropInput = new ArrayList<>();
                    for (T t : inputData) {
                        if (dropInput.size() > 0 || !p.test(t))
                            dropInput.add(t);
                    }

                    LambdaTestHelpers.assertContents(act, dropInput);
                }

                LambdaTestHelpers.assertContents(act, exp);
            }
        };
    }

    private Collection<Integer> sizes(int s) {
        Set<Integer> sizes = new LinkedHashSet<>();

        sizes.add(0);
        sizes.add(1);
        sizes.add(s / 4);
        sizes.add(s / 2);
        sizes.add(3 * s / 4);
        sizes.add(Math.max(0, s - 1));
        sizes.add(s);
        sizes.add(Integer.MAX_VALUE);

        return sizes;
    }

    private void testWhileMulti(TestData.OfRef<Integer> data,
                                ResultAsserter<Iterable<Integer>> ra,
                                Function<Stream<Integer>, Stream<Integer>> mRef,
                                Function<IntStream, IntStream> mInt,
                                Function<LongStream, LongStream> mLong,
                                Function<DoubleStream, DoubleStream> mDouble) {
        Map<String, Function<Stream<Integer>, Stream<Integer>>> ms = new HashMap<>();
        ms.put("Ref", mRef);
        ms.put("Int", s -> mInt.apply(s.mapToInt(e -> e)).mapToObj(e -> e));
        ms.put("Long", s -> mLong.apply(s.mapToLong(e -> e)).mapToObj(e -> (int) e));
        ms.put("Double", s -> mDouble.apply(s.mapToDouble(e -> e)).mapToObj(e -> (int) e));
        ms.put("Ref using defaults", s -> mRef.apply(DefaultMethodStreams.delegateTo(s)));
        ms.put("Int using defaults", s -> mInt.apply(DefaultMethodStreams.delegateTo(s.mapToInt(e -> e))).mapToObj(e -> e));
        ms.put("Long using defaults", s -> mLong.apply(DefaultMethodStreams.delegateTo(s.mapToLong(e -> e))).mapToObj(e -> (int) e));
        ms.put("Double using defaults", s -> mDouble.apply(DefaultMethodStreams.delegateTo(s.mapToDouble(e -> e))).mapToObj(e -> (int) e));

        testWhileMulti(data, ra, ms);
    }

    private final void testWhileMulti(TestData.OfRef<Integer> data,
                                      ResultAsserter<Iterable<Integer>> ra,
                                      Map<String, Function<Stream<Integer>, Stream<Integer>>> ms) {
        for (Map.Entry<String, Function<Stream<Integer>, Stream<Integer>>> e : ms.entrySet()) {
            setContext("shape", e.getKey());

            withData(data)
                    .stream(e.getValue())
                    .resultAsserter(ra)
                    .exercise();
        }
    }

    @Test(groups = { "serialization-hostile" })
    public void testRefDefaultClose() {
        AtomicBoolean isClosed = new AtomicBoolean();
        Stream<Integer> s = Stream.of(1, 2, 3).onClose(() -> isClosed.set(true));
        try (Stream<Integer> ds = DefaultMethodStreams.delegateTo(s).takeWhile(e -> e < 3)) {
            ds.count();
        }
        assertTrue(isClosed.get());
    }

    @Test(groups = { "serialization-hostile" })
    public void testIntDefaultClose() {
        AtomicBoolean isClosed = new AtomicBoolean();
        IntStream s = IntStream.of(1, 2, 3).onClose(() -> isClosed.set(true));
        try (IntStream ds = DefaultMethodStreams.delegateTo(s).takeWhile(e -> e < 3)) {
            ds.count();
        }
        assertTrue(isClosed.get());
    }

    @Test(groups = { "serialization-hostile" })
    public void testLongDefaultClose() {
        AtomicBoolean isClosed = new AtomicBoolean();
        LongStream s = LongStream.of(1, 2, 3).onClose(() -> isClosed.set(true));
        try (LongStream ds = DefaultMethodStreams.delegateTo(s).takeWhile(e -> e < 3)) {
            ds.count();
        }
        assertTrue(isClosed.get());
    }

    @Test(groups = { "serialization-hostile" })
    public void testDoubleDefaultClose() {
        AtomicBoolean isClosed = new AtomicBoolean();
        DoubleStream s = DoubleStream.of(1, 2, 3).onClose(() -> isClosed.set(true));
        try (DoubleStream ds = DefaultMethodStreams.delegateTo(s).takeWhile(e -> e < 3)) {
            ds.count();
        }
        assertTrue(isClosed.get());
    }

    @Test(groups = { "serialization-hostile" })
    public void testFlatMapThenTake() {
        TestData.OfRef<Integer> range = TestData.Factory.ofSupplier(
                "range", () -> IntStream.range(0, 100).boxed());

        exerciseOpsMulti(range,
                         // Reference result
                         s -> s.takeWhile(e -> e != 50),
                         // For other results collect into array,
                         // stream the single array (not the elements),
                         // then flat map to stream the array elements
                         s -> Stream.<Integer[]>of(s.toArray(Integer[]::new)).
                                 flatMap(Stream::of).
                                 takeWhile(e -> e != 50),
                         s -> Stream.of(s.mapToInt(e -> e).toArray()).
                                 flatMapToInt(IntStream::of).
                                 takeWhile(e -> e != 50).
                                 mapToObj(e -> e),
                         s -> Stream.of(s.mapToLong(e -> e).toArray()).
                                 flatMapToLong(LongStream::of).
                                 takeWhile(e -> e != 50L).
                                 mapToObj(e -> (int) e),
                         s -> Stream.of(s.mapToDouble(e -> e).toArray()).
                                 flatMapToDouble(DoubleStream::of).
                                 takeWhile(e -> e != 50.0).
                                 mapToObj(e -> (int) e)
                         );
    }
}
