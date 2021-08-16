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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.LambdaTestHelpers.countTo;

@Test
public class FlagOpTest extends OpTestCase {

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testFlagsPassThrough(String name, TestData<Integer, Stream<Integer>> data) {

        @SuppressWarnings({"unchecked", "rawtypes"})
        TestFlagPassThroughOp<Integer>[] ops = new TestFlagPassThroughOp[3];
        ops[0] = new TestFlagPassThroughOp<>();
        ops[1] = new TestFlagPassThroughOp<>();
        ops[2] = new TestFlagPassThroughOp<>();

        ops[0].set(null, ops[1]);
        ops[1].set(ops[0], ops[2]);
        ops[2].set(ops[1], null);

        withData(data).ops(ops).exercise();
    }

    static class TestFlagPassThroughOp<T> extends FlagDeclaringOp<T> {
        TestFlagPassThroughOp<T> upstream;
        TestFlagPassThroughOp<T> downstream;

        TestFlagPassThroughOp() {
            super(0);
        }

        void set(TestFlagPassThroughOp<T> upstream, TestFlagPassThroughOp<T> downstream)  {
            this.upstream = upstream;
            this.downstream = downstream;
        }

        int wrapFlags;

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Sink<T> opWrapSink(int flags, boolean parallel, Sink sink) {
            this.wrapFlags = flags;

            if (downstream != null) {
                assertTrue(flags == downstream.wrapFlags);
            }

            return sink;
        }
    }

    public void testFlagsClearAllSet() {
        int clearAllFlags = 0;
        for (StreamOpFlag f : EnumSet.allOf(StreamOpFlag.class)) {
            if (f.isStreamFlag()) {
                clearAllFlags |= f.clear();
            }
        }

        EnumSet<StreamOpFlag> known = EnumSet.noneOf(StreamOpFlag.class);
        EnumSet<StreamOpFlag> notKnown = StreamOpFlagTestHelper.allStreamFlags();

        List<FlagDeclaringOp<Integer>> ops = new ArrayList<>();
        ops.add(new FlagDeclaringOp<>(clearAllFlags));
        for (StreamOpFlag f : StreamOpFlagTestHelper.allStreamFlags()) {
            if (f.canSet(StreamOpFlag.Type.OP)) {
                ops.add(new TestFlagExpectedOp<>(f.set(),
                                             known.clone(),
                                             EnumSet.noneOf(StreamOpFlag.class),
                                             notKnown.clone()));
                known.add(f);
                notKnown.remove(f);
            }
        }
        ops.add(new TestFlagExpectedOp<>(0,
                                         known.clone(),
                                         EnumSet.noneOf(StreamOpFlag.class),
                                         notKnown.clone()));

        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofArray("Array", countTo(10).toArray(new Integer[0]));
        @SuppressWarnings("rawtypes")
        FlagDeclaringOp[] opsArray = ops.toArray(new FlagDeclaringOp[ops.size()]);

        withData(data).ops(opsArray).
                without(StreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    public void testFlagsSetAllClear() {
        EnumSet<StreamOpFlag> known = StreamOpFlagTestHelper.allStreamFlags();
        int setAllFlags = 0;
        for (StreamOpFlag f : EnumSet.allOf(StreamOpFlag.class)) {
            if (f.isStreamFlag()) {
                if (f.canSet(StreamOpFlag.Type.OP)) {
                    setAllFlags |= f.set();
                } else {
                    known.remove(f);
                }
            }
        }

        EnumSet<StreamOpFlag> notKnown = EnumSet.noneOf(StreamOpFlag.class);

        List<FlagDeclaringOp<Integer>> ops = new ArrayList<>();
        ops.add(new FlagDeclaringOp<>(setAllFlags));
        for (StreamOpFlag f : StreamOpFlagTestHelper.allStreamFlags()) {
            ops.add(new TestFlagExpectedOp<>(f.clear(),
                                             known.clone(),
                                             EnumSet.noneOf(StreamOpFlag.class),
                                             notKnown.clone()));
            known.remove(f);
            notKnown.add(f);
        }
        ops.add(new TestFlagExpectedOp<>(0,
                                         known.clone(),
                                         EnumSet.noneOf(StreamOpFlag.class),
                                         notKnown.clone()));

        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofArray("Array", countTo(10).toArray(new Integer[0]));
        @SuppressWarnings("rawtypes")
        FlagDeclaringOp[] opsArray = ops.toArray(new FlagDeclaringOp[ops.size()]);


        withData(data).ops(opsArray).
                without(StreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    public void testFlagsParallelCollect() {
        testFlagsSetSequence(CollectorOps::collector);
    }

    private void testFlagsSetSequence(Supplier<StatefulTestOp<Integer>> cf) {
        EnumSet<StreamOpFlag> known = EnumSet.of(StreamOpFlag.ORDERED, StreamOpFlag.SIZED);
        EnumSet<StreamOpFlag> preserve = EnumSet.of(StreamOpFlag.DISTINCT, StreamOpFlag.SORTED);

        List<IntermediateTestOp<Integer, Integer>> ops = new ArrayList<>();
        for (StreamOpFlag f : EnumSet.of(StreamOpFlag.DISTINCT, StreamOpFlag.SORTED)) {
            ops.add(cf.get());
            ops.add(new TestFlagExpectedOp<>(f.set(),
                                             known.clone(),
                                             preserve.clone(),
                                             EnumSet.noneOf(StreamOpFlag.class)));
            known.add(f);
            preserve.remove(f);
        }
        ops.add(cf.get());
        ops.add(new TestFlagExpectedOp<>(0,
                                         known.clone(),
                                         preserve.clone(),
                                         EnumSet.noneOf(StreamOpFlag.class)));

        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofArray("Array", countTo(10).toArray(new Integer[0]));
        @SuppressWarnings("rawtypes")
        IntermediateTestOp[] opsArray = ops.toArray(new IntermediateTestOp[ops.size()]);

        withData(data).ops(opsArray).
                without(StreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }


    public void testFlagsClearParallelCollect() {
        testFlagsClearSequence(CollectorOps::collector);
    }

    protected void testFlagsClearSequence(Supplier<StatefulTestOp<Integer>> cf) {
        EnumSet<StreamOpFlag> known = EnumSet.of(StreamOpFlag.ORDERED, StreamOpFlag.SIZED);
        EnumSet<StreamOpFlag> preserve = EnumSet.of(StreamOpFlag.DISTINCT, StreamOpFlag.SORTED);
        EnumSet<StreamOpFlag> notKnown = EnumSet.noneOf(StreamOpFlag.class);

        List<IntermediateTestOp<Integer, Integer>> ops = new ArrayList<>();
        for (StreamOpFlag f : EnumSet.of(StreamOpFlag.ORDERED, StreamOpFlag.DISTINCT, StreamOpFlag.SORTED)) {
            ops.add(cf.get());
            ops.add(new TestFlagExpectedOp<>(f.clear(),
                                             known.clone(),
                                             preserve.clone(),
                                             notKnown.clone()));
            known.remove(f);
            preserve.remove(f);
            notKnown.add(f);
        }
        ops.add(cf.get());
        ops.add(new TestFlagExpectedOp<>(0,
                                         known.clone(),
                                         preserve.clone(),
                                         notKnown.clone()));

        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofArray("Array", countTo(10).toArray(new Integer[0]));
        @SuppressWarnings("rawtypes")
        IntermediateTestOp[] opsArray = ops.toArray(new IntermediateTestOp[ops.size()]);

        withData(data).ops(opsArray).
                without(StreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    public void testFlagsSizedOrderedParallelCollect() {
        EnumSet<StreamOpFlag> parKnown = EnumSet.of(StreamOpFlag.SIZED);
        EnumSet<StreamOpFlag> serKnown = parKnown.clone();

        List<IntermediateTestOp<Integer, Integer>> ops = new ArrayList<>();
        for (StreamOpFlag f : parKnown) {
            ops.add(CollectorOps.collector());
            ops.add(new ParSerTestFlagExpectedOp<>(f.clear(),
                                             parKnown,
                                             serKnown));
            serKnown.remove(f);
        }
        ops.add(CollectorOps.collector());
        ops.add(new ParSerTestFlagExpectedOp<>(0,
                                         parKnown,
                                         EnumSet.noneOf(StreamOpFlag.class)));

        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofArray("Array", countTo(10).toArray(new Integer[0]));
        @SuppressWarnings("rawtypes")
        IntermediateTestOp[] opsArray = ops.toArray(new IntermediateTestOp[ops.size()]);

        withData(data).ops(opsArray).exercise();
    }

    static class ParSerTestFlagExpectedOp<T> extends FlagDeclaringOp<T> {
        final EnumSet<StreamOpFlag> parKnown;
        final EnumSet<StreamOpFlag> serKnown;

        ParSerTestFlagExpectedOp(int flags, EnumSet<StreamOpFlag> known, EnumSet<StreamOpFlag> serKnown) {
            super(flags);
            this.parKnown = known;
            this.serKnown = serKnown;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Sink<T> opWrapSink(int flags, boolean parallel, Sink upstream) {
            assertFlags(flags, parallel);
            return upstream;
        }

        protected void assertFlags(int flags, boolean parallel) {
            if (parallel) {
                for (StreamOpFlag f : parKnown) {
                    Assert.assertTrue(f.isKnown(flags), String.format("Flag %s is not known, but should be known.", f.toString()));
                }

            } else {
                for (StreamOpFlag f : serKnown) {
                    Assert.assertTrue(f.isKnown(flags), String.format("Flag %s is not known, but should be known.", f.toString()));
                }

            }
        }
    }
}
