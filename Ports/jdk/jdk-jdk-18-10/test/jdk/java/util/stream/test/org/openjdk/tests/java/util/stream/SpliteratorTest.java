/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;
import org.testng.annotations.Test;

import java.util.function.Supplier;
import java.util.Spliterator;
import java.util.SpliteratorTestHelper;
import java.util.stream.*;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

/**
 * SpliteratorTest
 *
 * @author Brian Goetz
 */
@Test
public class SpliteratorTest {

    @Test(dataProvider = "Spliterator<Integer>", dataProviderClass = StreamTestDataProvider.class )
    public void testSpliterator(String name, Supplier<Spliterator<Integer>> supplier) {
        SpliteratorTestHelper.testSpliterator(supplier);
    }

    @Test(dataProvider = "IntSpliterator", dataProviderClass = IntStreamTestDataProvider.class )
    public void testIntSpliterator(String name, Supplier<Spliterator.OfInt> supplier) {
        SpliteratorTestHelper.testIntSpliterator(supplier);
    }

    @Test(dataProvider = "LongSpliterator", dataProviderClass = LongStreamTestDataProvider.class )
    public void testLongSpliterator(String name, Supplier<Spliterator.OfLong> supplier) {
        SpliteratorTestHelper.testLongSpliterator(supplier);
    }

    @Test(dataProvider = "DoubleSpliterator", dataProviderClass = DoubleStreamTestDataProvider.class )
    public void testDoubleSpliterator(String name, Supplier<Spliterator.OfDouble> supplier) {
        SpliteratorTestHelper.testDoubleSpliterator(supplier);
    }

    @Test(dataProvider = "SegmentSpliterator", dataProviderClass = SegmentTestDataProvider.class )
    public void testSegmentSpliterator(String name, SequenceLayout layout, SpliteratorTestHelper.ContentAsserter<MemorySegment> contentAsserter) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(layout, scope);
            SegmentTestDataProvider.initSegment(segment);
            SpliteratorTestHelper.testSpliterator(() -> segment.spliterator(layout),
                    SegmentTestDataProvider::segmentCopier, contentAsserter);
        }
    }
}
