/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Base class for streams test cases.  Provides 'exercise' methods for taking
 * lambdas that construct and modify streams, and evaluates them in different
 * ways and asserts that they produce equivalent results.
 */
@Test
public abstract class OpTestCase extends LoggingTestCase {

    private final Map<StreamShape, Set<? extends BaseStreamTestScenario>> testScenarios;

    protected OpTestCase() {
        testScenarios = new EnumMap<>(StreamShape.class);
        testScenarios.put(StreamShape.REFERENCE, Collections.unmodifiableSet(EnumSet.allOf(StreamTestScenario.class)));
        testScenarios.put(StreamShape.INT_VALUE, Collections.unmodifiableSet(EnumSet.allOf(IntStreamTestScenario.class)));
        testScenarios.put(StreamShape.LONG_VALUE, Collections.unmodifiableSet(EnumSet.allOf(LongStreamTestScenario.class)));
        testScenarios.put(StreamShape.DOUBLE_VALUE, Collections.unmodifiableSet(EnumSet.allOf(DoubleStreamTestScenario.class)));
    }

    @SuppressWarnings("rawtypes")
    public static int getStreamFlags(BaseStream s) {
        return ((AbstractPipeline) s).getStreamFlags();
    }

    /**
     * An asserter for results produced when exercising of stream or terminal
     * tests.
     *
     * @param <R> the type of result to assert on
     */
    public interface ResultAsserter<R> {
        /**
         * Assert a result produced when exercising of stream or terminal
         * test.
         *
         * @param actual the actual result
         * @param expected the expected result
         * @param isOrdered true if the pipeline is ordered
         * @param isParallel true if the pipeline is parallel
         */
        void assertResult(R actual, R expected, boolean isOrdered, boolean isParallel);
    }

    // Exercise stream operations

    public interface BaseStreamTestScenario {
        StreamShape getShape();

        boolean isParallel();

        boolean isOrdered();

        default <T, S_IN extends BaseStream<T, S_IN>>
        S_IN getStream(TestData<T, S_IN> data) {
            return isParallel()
                   ? data.parallelStream()
                   : data.stream();
        }

        <T, U, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
        void run(TestData<T, S_IN> data, Consumer<U> b, Function<S_IN, S_OUT> m);
    }

    protected <T, U, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
    Collection<U> exerciseOps(TestData<T, S_IN> data, Function<S_IN, S_OUT> m) {
        return withData(data).stream(m).exercise();
    }

    // Run multiple versions of exercise(), returning the result of the first, and asserting that others return the same result
    // If the first version is s -> s.foo(), can be used with s -> s.mapToInt(i -> i).foo().mapToObj(i -> i) to test all shape variants
    @SafeVarargs
    protected final<T, U, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
    Collection<U> exerciseOpsMulti(TestData<T, S_IN> data,
                                   Function<S_IN, S_OUT>... ms) {
        Collection<U> result = null;
        for (Function<S_IN, S_OUT> m : ms) {
            if (result == null)
                result = withData(data).stream(m).exercise();
            else {
                Collection<U> r2 = withData(data).stream(m).exercise();
                assertEquals(result, r2);
            }
        }
        return result;
    }

    // Run multiple versions of exercise() for an Integer stream, returning the result of the first, and asserting that others return the same result
    // Automates the conversion between Stream<Integer> and {Int,Long,Double}Stream and back, so client sites look like you are passing the same
    // lambda four times, but in fact they are four different lambdas since they are transforming four different kinds of streams
    protected final
    Collection<Integer> exerciseOpsInt(TestData.OfRef<Integer> data,
                                       Function<Stream<Integer>, Stream<Integer>> mRef,
                                       Function<IntStream, IntStream> mInt,
                                       Function<LongStream, LongStream> mLong,
                                       Function<DoubleStream, DoubleStream> mDouble) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Function<Stream<Integer>, Stream<Integer>>[] ms = new Function[4];
        ms[0] = mRef;
        ms[1] = s -> mInt.apply(s.mapToInt(e -> e)).mapToObj(e -> e);
        ms[2] = s -> mLong.apply(s.mapToLong(e -> e)).mapToObj(e -> (int) e);
        ms[3] = s -> mDouble.apply(s.mapToDouble(e -> e)).mapToObj(e -> (int) e);
        return exerciseOpsMulti(data, ms);
    }

    // Run multiple versions of exercise() with multiple terminal operations for all kinds of stream, , and asserting against the expected result
    // If the first version is s -> s.foo(), can be used with s -> s.mapToInt(i -> i).foo().mapToObj(i -> i) to test all shape variants
    protected final<T, U, R, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>>
    void exerciseTerminalOpsMulti(TestData<T, S_IN> data,
                                  R expected,
                                  Map<String, Function<S_IN, S_OUT>> streams,
                                  Map<String, Function<S_OUT, R>> terminals) {
        for (Map.Entry<String, Function<S_IN, S_OUT>> se : streams.entrySet()) {
            setContext("Intermediate stream", se.getKey());
            for (Map.Entry<String, Function<S_OUT, R>> te : terminals.entrySet()) {
                setContext("Terminal stream", te.getKey());
                withData(data)
                        .terminal(se.getValue(), te.getValue())
                        .expectedResult(expected)
                        .exercise();

            }
        }
    }

    // Run multiple versions of exercise() with multiple terminal operation for all kinds of stream, and asserting against the expected result
    // Automates the conversion between Stream<Integer> and {Int,Long,Double}Stream and back, so client sites look like you are passing the same
    // lambda four times, but in fact they are four different lambdas since they are transforming four different kinds of streams
    protected final
    void exerciseTerminalOpsInt(TestData<Integer, Stream<Integer>> data,
                                Collection<Integer> expected,
                                String desc,
                                Function<Stream<Integer>, Stream<Integer>> mRef,
                                Function<IntStream, IntStream> mInt,
                                Function<LongStream, LongStream> mLong,
                                Function<DoubleStream, DoubleStream> mDouble,
                                Map<String, Function<Stream<Integer>, Collection<Integer>>> terminals) {

        Map<String, Function<Stream<Integer>, Stream<Integer>>> m = new HashMap<>();
        m.put("Ref " + desc, mRef);
        m.put("Int " + desc, s -> mInt.apply(s.mapToInt(e -> e)).mapToObj(e -> e));
        m.put("Long " + desc, s -> mLong.apply(s.mapToLong(e -> e)).mapToObj(e -> (int) e));
        m.put("Double " + desc, s -> mDouble.apply(s.mapToDouble(e -> e)).mapToObj(e -> (int) e));

        exerciseTerminalOpsMulti(data, expected, m, terminals);
    }


    protected <T, U, S_OUT extends BaseStream<U, S_OUT>>
    Collection<U> exerciseOps(Collection<T> data, Function<Stream<T>, S_OUT> m) {
        TestData.OfRef<T> data1 = TestData.Factory.ofCollection("Collection of type " + data.getClass().getName(), data);
        return withData(data1).stream(m).exercise();
    }

    protected <T, U, S_OUT extends BaseStream<U, S_OUT>, I extends Iterable<U>>
    Collection<U> exerciseOps(Collection<T> data, Function<Stream<T>, S_OUT> m, I expected) {
        TestData.OfRef<T> data1 = TestData.Factory.ofCollection("Collection of type " + data.getClass().getName(), data);
        return withData(data1).stream(m).expectedResult(expected).exercise();
    }

    @SuppressWarnings("unchecked")
    protected <U, S_OUT extends BaseStream<U, S_OUT>>
    Collection<U> exerciseOps(int[] data, Function<IntStream, S_OUT> m) {
        return withData(TestData.Factory.ofArray("int array", data)).stream(m).exercise();
    }

    protected Collection<Integer> exerciseOps(int[] data, Function<IntStream, IntStream> m, int[] expected) {
        TestData.OfInt data1 = TestData.Factory.ofArray("int array", data);
        return withData(data1).stream(m).expectedResult(expected).exercise();
    }

    protected <T, S_IN extends BaseStream<T, S_IN>> DataStreamBuilder<T, S_IN> withData(TestData<T, S_IN> data) {
        Objects.requireNonNull(data);
        return new DataStreamBuilder<>(data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public class DataStreamBuilder<T, S_IN extends BaseStream<T, S_IN>> {
        final TestData<T, S_IN> data;

        private DataStreamBuilder(TestData<T, S_IN> data) {
            this.data = Objects.requireNonNull(data);
        }

        public <U, S_OUT extends BaseStream<U, S_OUT>>
        ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> ops(IntermediateTestOp... ops) {
            return new ExerciseDataStreamBuilder<>(data, (S_IN s) -> (S_OUT) chain(s, ops));
        }

        public <U, S_OUT extends BaseStream<U, S_OUT>> ExerciseDataStreamBuilder<T, U, S_IN, S_OUT>
        stream(Function<S_IN, S_OUT> m) {
            return new ExerciseDataStreamBuilder<>(data, m);
        }

        public <U, S_OUT extends BaseStream<U, S_OUT>> ExerciseDataStreamBuilder<T, U, S_IN, S_OUT>
        stream(Function<S_IN, S_OUT> m, IntermediateTestOp<U, U> additionalOp) {
            return new ExerciseDataStreamBuilder<>(data, s -> (S_OUT) chain(m.apply(s), additionalOp));
        }

        public <R> ExerciseDataTerminalBuilder<T, T, R, S_IN, S_IN>
        terminal(Function<S_IN, R> terminalF) {
            return new ExerciseDataTerminalBuilder<>(data, s -> s, terminalF);
        }

        public <U, R, S_OUT extends BaseStream<U, S_OUT>> ExerciseDataTerminalBuilder<T, U, R, S_IN, S_OUT>
        terminal(Function<S_IN, S_OUT> streamF, Function<S_OUT, R> terminalF) {
            return new ExerciseDataTerminalBuilder<>(data, streamF, terminalF);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public class ExerciseDataStreamBuilder<T, U, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>> {
        final TestData<T, S_IN> data;
        final Function<S_IN, S_OUT> m;
        final StreamShape shape;

        Set<BaseStreamTestScenario> testSet = new HashSet<>();

        Collection<U> refResult;

        Consumer<TestData<T, S_IN>> before = LambdaTestHelpers.bEmpty;

        Consumer<TestData<T, S_IN>> after = LambdaTestHelpers.bEmpty;

        ResultAsserter<Iterable<U>> resultAsserter = (act, exp, ord, par) -> {
            if (par & !ord) {
                LambdaTestHelpers.assertContentsUnordered(act, exp);
            }
            else {
                LambdaTestHelpers.assertContentsEqual(act, exp);
            }
        };

        private ExerciseDataStreamBuilder(TestData<T, S_IN> data, Function<S_IN, S_OUT> m) {
            this.data = data;

            this.m = Objects.requireNonNull(m);

            this.shape = ((AbstractPipeline<?, U, ?>) m.apply(data.stream())).getOutputShape();

            // Have to initiate from the output shape of the last stream
            // This means the stream mapper is required first rather than last
            testSet.addAll(testScenarios.get(shape));
        }

        //

        public <I extends Iterable<U>> ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> expectedResult(I expectedResult) {
            List<U> l = new ArrayList<>();
            expectedResult.forEach(l::add);
            refResult = l;
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> expectedResult(int[] expectedResult) {
            List l = new ArrayList();
            for (int anExpectedResult : expectedResult) {
                l.add(anExpectedResult);
            }
            refResult = l;
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> expectedResult(long[] expectedResult) {
            List l = new ArrayList();
            for (long anExpectedResult : expectedResult) {
                l.add(anExpectedResult);
            }
            refResult = l;
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> expectedResult(double[] expectedResult) {
            List l = new ArrayList();
            for (double anExpectedResult : expectedResult) {
                l.add(anExpectedResult);
            }
            refResult = l;
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> before(Consumer<TestData<T, S_IN>> before) {
            this.before = Objects.requireNonNull(before);
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> after(Consumer<TestData<T, S_IN>> after) {
            this.after = Objects.requireNonNull(after);
            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> without(BaseStreamTestScenario... tests) {
            return without(Arrays.asList(tests));
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> without(Collection<? extends BaseStreamTestScenario> tests) {
            for (BaseStreamTestScenario ts : tests) {
                if (ts.getShape() == shape) {
                    testSet.remove(ts);
                }
            }

            if (testSet.isEmpty()) {
                throw new IllegalStateException("Test scenario set is empty");
            }

            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> with(BaseStreamTestScenario... tests) {
            return with(Arrays.asList(tests));
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> with(Collection<? extends BaseStreamTestScenario> tests) {
            testSet = new HashSet<>();

            for (BaseStreamTestScenario ts : tests) {
                if (ts.getShape() == shape) {
                    testSet.add(ts);
                }
            }

            if (testSet.isEmpty()) {
                throw new IllegalStateException("Test scenario set is empty");
            }

            return this;
        }

        public ExerciseDataStreamBuilder<T, U, S_IN, S_OUT> resultAsserter(ResultAsserter<Iterable<U>> resultAsserter) {
            this.resultAsserter = resultAsserter;
            return this;
        }

        // Build method

        public Collection<U> exercise() {
            final boolean isStreamOrdered;
            if (refResult == null) {
                // Induce the reference result
                before.accept(data);
                try (S_OUT sOut = m.apply(data.stream())) {
                    isStreamOrdered = StreamOpFlag.ORDERED.isKnown(((AbstractPipeline) sOut).getStreamFlags());
                    Node<U> refNodeResult = ((AbstractPipeline<?, U, ?>) sOut).evaluateToArrayNode(size -> (U[]) new Object[size]);
                    refResult = LambdaTestHelpers.toBoxedList(refNodeResult.spliterator());
                }
                after.accept(data);
            }
            else {
                try (S_OUT sOut = m.apply(data.stream())) {
                    isStreamOrdered = StreamOpFlag.ORDERED.isKnown(((AbstractPipeline) sOut).getStreamFlags());
                }
            }

            List<Error> errors = new ArrayList<>();
            for (BaseStreamTestScenario test : testSet) {
                try {
                    before.accept(data);

                    List<U> result = new ArrayList<>();
                    test.run(data, LambdaTestHelpers.<U>toBoxingConsumer(result::add), m);

                    Runnable asserter = () -> resultAsserter.assertResult(result, refResult, isStreamOrdered && test.isOrdered(), test.isParallel());

                    if (refResult.size() > 1000) {
                        LambdaTestHelpers.launderAssertion(
                                asserter,
                                () -> String.format("%n%s: [actual size=%d] != [expected size=%d]", test, result.size(), refResult.size()));
                    }
                    else {
                        LambdaTestHelpers.launderAssertion(
                                asserter,
                                () -> String.format("%n%s: [actual] %s != [expected] %s", test, result, refResult));
                    }

                    after.accept(data);
                } catch (Throwable t) {
                    errors.add(new Error(String.format("%s: %s", test, t), t));
                }
            }

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (Error t : errors) {
                    sb.append(i++).append(": ");
                    if (t instanceof AssertionError) {
                        sb.append(t).append("\n");
                    }
                    else {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);

                        t.getCause().printStackTrace(pw);
                        pw.flush();
                        sb.append(t).append("\n").append(sw);
                    }
                }
                sb.append("--");

                fail(String.format("%d failure(s) for test data: %s\n%s", i - 1, data.toString(), sb));
            }

            return refResult;
        }
    }

    // Exercise terminal operations

    interface BaseTerminalTestScenario<U, R, S_OUT extends BaseStream<U, S_OUT>> {
        boolean requiresSingleStageSource();

        boolean requiresParallelSource();

        default R run(Function<S_OUT, R> terminalF, S_OUT source, StreamShape shape) {
            return terminalF.apply(source);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    enum TerminalTestScenario implements BaseTerminalTestScenario {
        SINGLE_SEQUENTIAL(true, false),

        SINGLE_SEQUENTIAL_SHORT_CIRCUIT(true, false) {
            @Override
            public Object run(Function terminalF, BaseStream source, StreamShape shape) {
                source = (BaseStream) chain(source, new ShortCircuitOp(shape));
                return terminalF.apply(source);
            }
        },

        SINGLE_PARALLEL(true, true),

        ALL_SEQUENTIAL(false, false),

        ALL_SEQUENTIAL_SHORT_CIRCUIT(false, false) {
            @Override
            public Object run(Function terminalF, BaseStream source, StreamShape shape) {
                source = (BaseStream) chain(source, new ShortCircuitOp(shape));
                return terminalF.apply(source);
            }
        },

        ALL_PARALLEL(false, true),

        ALL_PARALLEL_SEQUENTIAL(false, false) {
            @Override
            public Object run(Function terminalF, BaseStream source, StreamShape shape) {
                return terminalF.apply(source.sequential());
            }
        },
        ;

        private final boolean requiresSingleStageSource;
        private final boolean isParallel;

        TerminalTestScenario(boolean requiresSingleStageSource, boolean isParallel) {
            this.requiresSingleStageSource = requiresSingleStageSource;
            this.isParallel = isParallel;
        }

        @Override
        public boolean requiresSingleStageSource() {
            return requiresSingleStageSource;
        }

        @Override
        public boolean requiresParallelSource() {
            return isParallel;
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public class ExerciseDataTerminalBuilder<T, U, R, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>> {
        final TestData<T, S_IN> data;
        final Function<S_IN, S_OUT> streamF;
        final Function<S_OUT, R> terminalF;

        R refResult;

        ResultAsserter<R> resultAsserter = (act, exp, ord, par) -> LambdaTestHelpers.assertContentsEqual(act, exp);

        private ExerciseDataTerminalBuilder(TestData<T, S_IN> data, Function<S_IN, S_OUT> streamF, Function<S_OUT, R> terminalF) {
            this.data = data;
            this.streamF = Objects.requireNonNull(streamF);
            this.terminalF = Objects.requireNonNull(terminalF);
        }

        //

        public ExerciseDataTerminalBuilder<T, U, R, S_IN, S_OUT> expectedResult(R expectedResult) {
            this.refResult = expectedResult;
            return this;
        }

        public ExerciseDataTerminalBuilder<T, U, R, S_IN, S_OUT> equalator(BiConsumer<R, R> equalityAsserter) {
            resultAsserter = (act, exp, ord, par) -> equalityAsserter.accept(act, exp);
            return this;
        }

        public ExerciseDataTerminalBuilder<T, U, R, S_IN, S_OUT> resultAsserter(ResultAsserter<R> resultAsserter) {
            this.resultAsserter = resultAsserter;
            return this;
        }

        // Build method

        public R exercise() {
            boolean isOrdered;
            StreamShape shape;
            Node<U> node;
            try (S_OUT out = streamF.apply(data.stream()).sequential()) {
                AbstractPipeline ap = (AbstractPipeline) out;
                isOrdered = StreamOpFlag.ORDERED.isKnown(ap.getStreamFlags());
                shape = ap.getOutputShape();
                // Sequentially collect the output that will be input to the terminal op
                node = ap.evaluateToArrayNode(size -> (U[]) new Object[size]);
            }

            EnumSet<TerminalTestScenario> tests = EnumSet.allOf(TerminalTestScenario.class);
            if (refResult == null) {
                // Induce the reference result
                S_OUT source = (S_OUT) createPipeline(shape, node.spliterator(),
                                                      StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SIZED,
                                                      false);

                refResult = (R) TerminalTestScenario.SINGLE_SEQUENTIAL.run(terminalF, source, shape);
                tests.remove(TerminalTestScenario.SINGLE_SEQUENTIAL);
            }

            for (BaseTerminalTestScenario test : tests) {
                S_OUT source;
                if (test.requiresSingleStageSource()) {
                    source = (S_OUT) createPipeline(shape, node.spliterator(),
                                                    StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SIZED,
                                                    test.requiresParallelSource());
                }
                else {
                    source = streamF.apply(test.requiresParallelSource()
                                           ? data.parallelStream() : data.stream());
                }

                R result;
                try (source) {
                    result = (R) test.run(terminalF, source, shape);
                }
                LambdaTestHelpers.launderAssertion(
                        () -> resultAsserter.assertResult(result, refResult, isOrdered, test.requiresParallelSource()),
                        () -> String.format("%s: %s != %s", test, refResult, result));
            }

            return refResult;
        }

        AbstractPipeline createPipeline(StreamShape shape, Spliterator s, int flags, boolean parallel) {
            switch (shape) {
                case REFERENCE:    return new ReferencePipeline.Head<>(s, flags, parallel);
                case INT_VALUE:    return new IntPipeline.Head(s, flags, parallel);
                case LONG_VALUE:   return new LongPipeline.Head(s, flags, parallel);
                case DOUBLE_VALUE: return new DoublePipeline.Head(s, flags, parallel);
                default: throw new IllegalStateException("Unknown shape: " + shape);
            }
        }
    }

    protected <T, R> R exerciseTerminalOps(Collection<T> data, Function<Stream<T>, R> m, R expected) {
        TestData.OfRef<T> data1
                = TestData.Factory.ofCollection("Collection of type " + data.getClass().getName(), data);
        return withData(data1).terminal(m).expectedResult(expected).exercise();
    }

    protected <T, R, S_IN extends BaseStream<T, S_IN>> R
    exerciseTerminalOps(TestData<T, S_IN> data,
                        Function<S_IN, R> terminalF) {
        return withData(data).terminal(terminalF).exercise();
    }

    protected <T, U, R, S_IN extends BaseStream<T, S_IN>, S_OUT extends BaseStream<U, S_OUT>> R
    exerciseTerminalOps(TestData<T, S_IN> data,
                        Function<S_IN, S_OUT> streamF,
                        Function<S_OUT, R> terminalF) {
        return withData(data).terminal(streamF, terminalF).exercise();
    }

    //

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> AbstractPipeline<?, T, ?> chain(AbstractPipeline upstream, IntermediateTestOp<?, T> op) {
        return (AbstractPipeline<?, T, ?>) IntermediateTestOp.chain(upstream, op);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AbstractPipeline<?, ?, ?> chain(AbstractPipeline pipe, IntermediateTestOp... ops) {
        for (IntermediateTestOp op : ops)
            pipe = chain(pipe, op);
        return pipe;
    }

    @SuppressWarnings("rawtypes")
    private static <T> AbstractPipeline<?, T, ?> chain(BaseStream pipe, IntermediateTestOp<?, T> op) {
        return chain((AbstractPipeline) pipe, op);
    }

    @SuppressWarnings("rawtypes")
    public static AbstractPipeline<?, ?, ?> chain(BaseStream pipe, IntermediateTestOp... ops) {
        return chain((AbstractPipeline) pipe, ops);
    }

    // Test data

    static class ShortCircuitOp<T> implements StatelessTestOp<T,T> {
        private final StreamShape shape;

        ShortCircuitOp(StreamShape shape) {
            this.shape = shape;
        }

        @Override
        public Sink<T> opWrapSink(int flags, boolean parallel, Sink<T> sink) {
            return sink;
        }

        @Override
        public int opGetFlags() {
            return StreamOpFlag.IS_SHORT_CIRCUIT;
        }

        @Override
        public StreamShape outputShape() {
            return shape;
        }

        @Override
        public StreamShape inputShape() {
            return shape;
        }
    }
}
