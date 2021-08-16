/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.List;
import java.util.SpliteratorTestHelper;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;

public class SegmentTestDataProvider {

    static boolean compareSegmentsByte(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Byte> mapper = MemoryAccess::getByte;
        List<Byte> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Byte> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static boolean compareSegmentsChar(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Character> mapper = MemoryAccess::getChar;
        List<Character> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Character> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static boolean compareSegmentsShort(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Short> mapper = MemoryAccess::getShort;
        List<Short> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Short> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static boolean compareSegmentsInt(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Integer> mapper = MemoryAccess::getInt;
        List<Integer> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Integer> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static boolean compareSegmentsLong(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Long> mapper = MemoryAccess::getLong;
        List<Long> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Long> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static boolean compareSegmentsFloat(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Float> mapper = MemoryAccess::getFloat;
        List<Float> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Float> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static Consumer<MemorySegment> segmentCopier(Consumer<MemorySegment> input) {
        return segment -> {
            MemorySegment dest = MemorySegment.ofArray(new byte[(int)segment.byteSize()]);
            dest.copyFrom(segment);
            input.accept(dest);
        };
    }

    static boolean compareSegmentsDouble(Collection<MemorySegment> segments1, Collection<MemorySegment> segments2, boolean isOrdered) {
        Function<MemorySegment, Double> mapper = MemoryAccess::getDouble;
        List<Double> list1 = segments1.stream()
                .map(mapper)
                .collect(Collectors.toList());
        List<Double> list2 = segments2.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return list1.equals(list2);
    }

    static void initSegment(MemorySegment segment) {
        for (int i = 0 ; i < segment.byteSize() ; i++) {
            MemoryAccess.setByte(segment, (byte)i);
        }
    }

    static Object[][] spliteratorTestData = {
            { "bytes", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_BYTE), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsByte },
            { "chars", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_CHAR), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsChar },
            { "shorts", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_SHORT), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsShort },
            { "ints", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_INT), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsInt },
            { "longs", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_LONG), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsLong },
            { "floats", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_FLOAT), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsFloat },
            { "doubles", MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_DOUBLE), (SpliteratorTestHelper.ContentAsserter<MemorySegment>)SegmentTestDataProvider::compareSegmentsDouble },
    };

    // returns an array of (String name, Supplier<Spliterator<MemorySegment>>, ContentAsserter<MemorySegment>)
    @DataProvider(name = "SegmentSpliterator")
    public static Object[][] spliteratorProvider() {
        return spliteratorTestData;
    }
}
