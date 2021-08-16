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

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamOpFlag.*;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

@Test
public class StreamOpFlagsTest {

    public void testNullCombine() {
        int sourceFlags = StreamOpFlag.IS_SIZED;

        assertEquals(sourceFlags, toStreamFlags(combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE)));
    }

    public void testInitialOpFlagsFromSourceFlags() {
        List<StreamOpFlag> flags = new ArrayList<>(StreamOpFlagTestHelper.allStreamFlags());
        for (int i = 0; i < (1 << flags.size()); i++)  {
            int sourceFlags = 0;
            for (int f = 0; f < flags.size(); f++) {
                if ((i & (1 << f)) != 0)  {
                    sourceFlags |= flags.get(f).set();
                }
            }

            int opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            assertEquals(opsFlags, (~(sourceFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE);
        }
    }

    public void testSameCombine() {
        for (StreamOpFlag f : StreamOpFlagTestHelper.allStreamFlags()) {
            int sourceFlags = f.set();
            int opsFlags;

            opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            opsFlags = combineOpFlags(f.set(), opsFlags);
            assertEquals(sourceFlags, toStreamFlags(opsFlags));
        }
    }

    public void testOpClear() {
        for (StreamOpFlag f : StreamOpFlagTestHelper.allStreamFlags()) {
            // Clear when source not set
            int sourceFlags = 0;
            int opsFlags;

            opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            opsFlags = combineOpFlags(f.clear(), opsFlags);
            assertEquals(sourceFlags, toStreamFlags(opsFlags));

            // Clear when source set
            sourceFlags = f.set();

            opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            opsFlags = combineOpFlags(f.clear(), opsFlags);
            assertEquals(0, toStreamFlags(opsFlags));
        }
    }

    public void testOpInject() {
        for (StreamOpFlag f : StreamOpFlagTestHelper.allStreamFlags()) {
            // Set when source not set
            int sourceFlags = 0;
            int opsFlags;

            opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            opsFlags = combineOpFlags(f.set(), opsFlags);
            assertEquals(f.set(), toStreamFlags(opsFlags));

            // Set when source set
            sourceFlags = f.set();

            opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
            opsFlags = combineOpFlags(f.set(), opsFlags);
            assertEquals(sourceFlags, toStreamFlags(opsFlags));
        }
    }

    public void testPairSet() {
        List<Integer> sourceFlagsList
                = StreamOpFlagTestHelper.allStreamFlags().stream().map(StreamOpFlag::set).collect(toList());
        sourceFlagsList.add(0, 0);

        for (int sourceFlags : sourceFlagsList) {
            for (StreamOpFlag f1 : StreamOpFlagTestHelper.allStreamFlags()) {
                for (StreamOpFlag f2 : StreamOpFlagTestHelper.allStreamFlags()) {
                    int opsFlags;

                    opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
                    opsFlags = combineOpFlags(f1.set(), opsFlags);
                    opsFlags = combineOpFlags(f2.set(), opsFlags);
                    assertEquals(sourceFlags | f1.set() | f2.set(), toStreamFlags(opsFlags));
                }
            }
        }
    }

    public void testPairSetAndClear() {
        List<Integer> sourceFlagsList
                = StreamOpFlagTestHelper.allStreamFlags().stream().map(StreamOpFlag::set).collect(toList());
        sourceFlagsList.add(0, 0);

        for (int sourceFlags : sourceFlagsList) {
            for (StreamOpFlag f1 : StreamOpFlagTestHelper.allStreamFlags())  {
                for (StreamOpFlag f2 : StreamOpFlagTestHelper.allStreamFlags()) {
                    int opsFlags;

                    opsFlags = combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
                    opsFlags = combineOpFlags(f1.set(), opsFlags);
                    opsFlags = combineOpFlags(f2.clear(), opsFlags);
                    if (f1 == f2)
                        assertEquals((f2.set() == sourceFlags) ? 0 : sourceFlags, toStreamFlags(opsFlags));
                    else
                        assertEquals((f2.set() == sourceFlags) ? f1.set() : sourceFlags | f1.set(), toStreamFlags(opsFlags));
                }
            }
        }
    }

    public void testShortCircuit() {
        int opsFlags = combineOpFlags(0, StreamOpFlag.INITIAL_OPS_VALUE);
        assertFalse(StreamOpFlag.SHORT_CIRCUIT.isKnown(opsFlags));

        opsFlags = combineOpFlags(StreamOpFlag.IS_SHORT_CIRCUIT, opsFlags);
        assertTrue(StreamOpFlag.SHORT_CIRCUIT.isKnown(opsFlags));

        opsFlags = combineOpFlags(0, opsFlags);
        assertTrue(StreamOpFlag.SHORT_CIRCUIT.isKnown(opsFlags));
    }

    public void testApplySourceFlags() {
        int sourceFlags = StreamOpFlag.IS_SIZED | StreamOpFlag.IS_DISTINCT;

        List<Integer> ops = Arrays.asList(StreamOpFlag.NOT_SIZED, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);

        int opsFlags = StreamOpFlag.combineOpFlags(sourceFlags, StreamOpFlag.INITIAL_OPS_VALUE);
        for (int opFlags : ops) {
            opsFlags = combineOpFlags(opFlags, opsFlags);
        }
        assertFalse(StreamOpFlag.SIZED.isKnown(opsFlags));
        assertTrue(StreamOpFlag.SIZED.isCleared(opsFlags));
        assertFalse(StreamOpFlag.SIZED.isPreserved(opsFlags));
        assertTrue(StreamOpFlag.DISTINCT.isKnown(opsFlags));
        assertFalse(StreamOpFlag.DISTINCT.isCleared(opsFlags));
        assertFalse(StreamOpFlag.DISTINCT.isPreserved(opsFlags));
        assertTrue(StreamOpFlag.SORTED.isKnown(opsFlags));
        assertFalse(StreamOpFlag.SORTED.isCleared(opsFlags));
        assertFalse(StreamOpFlag.SORTED.isPreserved(opsFlags));
        assertTrue(StreamOpFlag.ORDERED.isKnown(opsFlags));
        assertFalse(StreamOpFlag.ORDERED.isCleared(opsFlags));
        assertFalse(StreamOpFlag.ORDERED.isPreserved(opsFlags));

        int streamFlags = toStreamFlags(opsFlags);
        assertFalse(StreamOpFlag.SIZED.isKnown(streamFlags));
        assertTrue(StreamOpFlag.DISTINCT.isKnown(streamFlags));
        assertTrue(StreamOpFlag.SORTED.isKnown(streamFlags));
        assertTrue(StreamOpFlag.ORDERED.isKnown(streamFlags));
    }

    public void testSpliteratorMask() {
        assertSpliteratorMask(StreamOpFlag.DISTINCT.set(), StreamOpFlag.IS_DISTINCT);
        assertSpliteratorMask(StreamOpFlag.DISTINCT.clear(), 0);

        assertSpliteratorMask(StreamOpFlag.SORTED.set(), StreamOpFlag.IS_SORTED);
        assertSpliteratorMask(StreamOpFlag.SORTED.clear(), 0);

        assertSpliteratorMask(StreamOpFlag.ORDERED.set(), StreamOpFlag.IS_ORDERED);
        assertSpliteratorMask(StreamOpFlag.ORDERED.clear(), 0);

        assertSpliteratorMask(StreamOpFlag.SIZED.set(), StreamOpFlag.IS_SIZED);
        assertSpliteratorMask(StreamOpFlag.SIZED.clear(), 0);

        assertSpliteratorMask(StreamOpFlag.SHORT_CIRCUIT.set(), 0);
        assertSpliteratorMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertSpliteratorMask(int actual, int expected) {
        assertEquals(actual & StreamOpFlag.SPLITERATOR_CHARACTERISTICS_MASK, expected);
    }

    public void testStreamMask() {
        assertStreamMask(StreamOpFlag.DISTINCT.set(), StreamOpFlag.IS_DISTINCT);
        assertStreamMask(StreamOpFlag.DISTINCT.clear(), 0);

        assertStreamMask(StreamOpFlag.SORTED.set(), StreamOpFlag.IS_SORTED);
        assertStreamMask(StreamOpFlag.SORTED.clear(), 0);

        assertStreamMask(StreamOpFlag.ORDERED.set(), StreamOpFlag.IS_ORDERED);
        assertStreamMask(StreamOpFlag.ORDERED.clear(), 0);

        assertStreamMask(StreamOpFlag.SIZED.set(), StreamOpFlag.IS_SIZED);
        assertStreamMask(StreamOpFlag.SIZED.clear(), 0);

        assertStreamMask(StreamOpFlag.SHORT_CIRCUIT.set(), 0);
        assertStreamMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertStreamMask(int actual, int expected) {
        assertEquals(actual & StreamOpFlag.STREAM_MASK, expected);
    }

    public void testOpMask() {
        assertOpMask(StreamOpFlag.DISTINCT.set(), StreamOpFlag.IS_DISTINCT);
        assertOpMask(StreamOpFlag.DISTINCT.clear(), StreamOpFlag.NOT_DISTINCT);

        assertOpMask(StreamOpFlag.SORTED.set(), StreamOpFlag.IS_SORTED);
        assertOpMask(StreamOpFlag.SORTED.clear(), StreamOpFlag.NOT_SORTED);

        assertOpMask(StreamOpFlag.ORDERED.set(), StreamOpFlag.IS_ORDERED);
        assertOpMask(StreamOpFlag.ORDERED.clear(), StreamOpFlag.NOT_ORDERED);

        assertOpMask(StreamOpFlag.SIZED.set(), 0);
        assertOpMask(StreamOpFlag.SIZED.clear(), StreamOpFlag.NOT_SIZED);

        assertOpMask(StreamOpFlag.SHORT_CIRCUIT.set(), StreamOpFlag.IS_SHORT_CIRCUIT);
        assertOpMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertOpMask(int actual, int expected) {
        assertEquals(actual & StreamOpFlag.OP_MASK, expected);
    }

    public void testTerminalOpMask() {
        assertTerminalOpMask(StreamOpFlag.DISTINCT.set(), 0);
        assertTerminalOpMask(StreamOpFlag.DISTINCT.clear(), 0);

        assertTerminalOpMask(StreamOpFlag.SORTED.set(), 0);
        assertTerminalOpMask(StreamOpFlag.SORTED.clear(), 0);

        assertTerminalOpMask(StreamOpFlag.ORDERED.set(), 0);
        assertTerminalOpMask(StreamOpFlag.ORDERED.clear(), StreamOpFlag.NOT_ORDERED);

        assertTerminalOpMask(StreamOpFlag.SIZED.set(), 0);
        assertTerminalOpMask(StreamOpFlag.SIZED.clear(), 0);

        assertTerminalOpMask(StreamOpFlag.SHORT_CIRCUIT.set(), StreamOpFlag.IS_SHORT_CIRCUIT);
        assertTerminalOpMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertTerminalOpMask(int actual, int expected) {
        assertEquals(actual & StreamOpFlag.TERMINAL_OP_MASK, expected);
    }

    public void testUpstreamTerminalOpMask() {
        assertUpstreamTerminalOpMask(StreamOpFlag.DISTINCT.set(), 0);
        assertUpstreamTerminalOpMask(StreamOpFlag.DISTINCT.clear(), 0);

        assertUpstreamTerminalOpMask(StreamOpFlag.SORTED.set(), 0);
        assertUpstreamTerminalOpMask(StreamOpFlag.SORTED.clear(), 0);

        assertUpstreamTerminalOpMask(StreamOpFlag.ORDERED.set(), 0);
        assertUpstreamTerminalOpMask(StreamOpFlag.ORDERED.clear(), StreamOpFlag.NOT_ORDERED);

        assertUpstreamTerminalOpMask(StreamOpFlag.SIZED.set(), 0);
        assertUpstreamTerminalOpMask(StreamOpFlag.SIZED.clear(), 0);

        assertUpstreamTerminalOpMask(StreamOpFlag.SHORT_CIRCUIT.set(), 0);
        assertUpstreamTerminalOpMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertUpstreamTerminalOpMask(int actual, int expected) {
        assertEquals(actual & StreamOpFlag.UPSTREAM_TERMINAL_OP_MASK, expected);
    }

    public void testSpliteratorCharacteristics() {
        assertEquals(Spliterator.DISTINCT, StreamOpFlag.IS_DISTINCT);
        assertEquals(Spliterator.SORTED, StreamOpFlag.IS_SORTED);
        assertEquals(Spliterator.ORDERED, StreamOpFlag.IS_ORDERED);
        assertEquals(Spliterator.SIZED, StreamOpFlag.IS_SIZED);

        List<Integer> others = Arrays.asList(Spliterator.NONNULL, Spliterator.IMMUTABLE,
                                             Spliterator.CONCURRENT, Spliterator.SUBSIZED);
        for (int c : others) {
            assertNotEquals(c, StreamOpFlag.IS_SHORT_CIRCUIT);
        }
    }

    public void testSpliteratorCharacteristicsMask() {
        assertSpliteratorCharacteristicsMask(StreamOpFlag.DISTINCT.set(), StreamOpFlag.IS_DISTINCT);
        assertSpliteratorCharacteristicsMask(StreamOpFlag.DISTINCT.clear(), 0);

        assertSpliteratorCharacteristicsMask(StreamOpFlag.SORTED.set(), StreamOpFlag.IS_SORTED);
        assertSpliteratorCharacteristicsMask(StreamOpFlag.SORTED.clear(), 0);

        assertSpliteratorCharacteristicsMask(StreamOpFlag.ORDERED.set(), StreamOpFlag.IS_ORDERED);
        assertSpliteratorCharacteristicsMask(StreamOpFlag.ORDERED.clear(), 0);

        assertSpliteratorCharacteristicsMask(StreamOpFlag.SIZED.set(), StreamOpFlag.IS_SIZED);
        assertSpliteratorCharacteristicsMask(StreamOpFlag.SIZED.clear(), 0);

        assertSpliteratorCharacteristicsMask(StreamOpFlag.SHORT_CIRCUIT.set(), 0);
        assertSpliteratorCharacteristicsMask(StreamOpFlag.SHORT_CIRCUIT.clear(), 0);
    }

    private void assertSpliteratorCharacteristicsMask(int actual, int expected) {
        assertEquals(StreamOpFlag.fromCharacteristics(actual), expected);
    }

    public void testSpliteratorSorted() {
        class SortedEmptySpliterator implements Spliterator<Object> {
            final Comparator<Object> c;

            SortedEmptySpliterator(Comparator<Object> c) {
                this.c = c;
            }

            @Override
            public Spliterator<Object> trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Object> action) {
                return false;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return Spliterator.SORTED;
            }

            @Override
            public Comparator<? super Object> getComparator() {
                return c;
            }
        };

        {
            int flags = StreamOpFlag.fromCharacteristics(new SortedEmptySpliterator(null));
            assertEquals(flags, StreamOpFlag.IS_SORTED);
        }

        {
            int flags = StreamOpFlag.fromCharacteristics(new SortedEmptySpliterator((a, b) -> 0));
            assertEquals(flags, 0);
        }
    }
}
