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
package org.openjdk.tests.java.util.stream;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.*;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.LambdaTestHelpers.*;

/**
 * TeeOpTest
 */
@Test(groups = { "serialization-hostile" })
public class TeeOpTest extends OpTestCase {

    public void testTee() {
        List<Integer> copy = new ArrayList<>();

        assertCountSum(countTo(0).stream().peek(copy::add), 0, 0);
        assertCountSum(copy.iterator(), 0, 0);

        copy.clear();
        assertCountSum(countTo(10).stream().peek(copy::add), 10, 55);
        assertCountSum(copy.iterator(), 10, 55);

        copy.clear();
        assertCountSum(countTo(10).stream().map(mDoubler).peek(copy::add), 10, 110);
        assertCountSum(copy.iterator(), 10, 110);
    }

    static class AbstractRecordingConsumer<T> {
        List<T> list;

        void before(TestData<T, ?> td) {
            // Tee block can be called concurrently
            list = Collections.synchronizedList(new ArrayList<>());
        }

        void after(TestData<T, ?> td) {
            // No guarantees in parallel tests that calls to tee block will
            // be in the encounter order, if defined, of the data
            // @@@ Consider passing more meta-data about evaluation
            assertContentsUnordered(list, td.into(new ArrayList<T>()));
        }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, final TestData.OfRef<Integer> data) {
        class RecordingConsumer extends AbstractRecordingConsumer<Integer> implements Consumer<Integer> {
            public void accept(Integer t) {
                list.add(t);
            }
        }
        final RecordingConsumer b = new RecordingConsumer();

        withData(data)
                .stream(s -> s.peek(b))
                .before(b::before)
                .after(b::after)
                .exercise();
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOps(String name, final TestData.OfInt data) {
        class RecordingConsumer extends AbstractRecordingConsumer<Integer> implements IntConsumer {
            public void accept(int t) {
                list.add(t);
            }
        }
        final RecordingConsumer b = new RecordingConsumer();

        withData(data)
                .stream(s -> s.peek(b))
                .before(b::before)
                .after(b::after)
                .exercise();
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, final TestData.OfLong data) {
        class RecordingConsumer extends AbstractRecordingConsumer<Long> implements LongConsumer {
            public void accept(long t) {
                list.add(t);
            }
        }
        final RecordingConsumer b = new RecordingConsumer();

        withData(data)
                .stream(s -> s.peek(b))
                .before(b::before)
                .after(b::after)
                .exercise();
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, final TestData.OfDouble data) {
        class RecordingConsumer extends AbstractRecordingConsumer<Double> implements DoubleConsumer {
            public void accept(double t) {
                list.add(t);
            }
        }
        final RecordingConsumer b = new RecordingConsumer();

        withData(data)
                .stream(s -> s.peek(b))
                .before(b::before)
                .after(b::after)
                .exercise();
    }
}
