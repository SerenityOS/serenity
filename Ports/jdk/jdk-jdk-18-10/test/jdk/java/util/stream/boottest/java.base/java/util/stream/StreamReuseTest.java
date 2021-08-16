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

import java.util.function.Function;

import static org.testng.Assert.fail;

/**
 * StreamReuseTest
 *
 * @author Brian Goetz
 */
@Test
public class StreamReuseTest {

    private <T, U, E, S extends BaseStream<E, S>, D extends TestData<E, S>> void assertSecondFails(
            D data,
            Function<S, T> first,
            Function<S, U> second,
            Class<? extends Throwable> exception,
            String text) {
        S stream = data.stream();
        T fr = first.apply(stream);
        try {
            U sr = second.apply(stream);
            fail(text + " (seq)");
        }
        catch (Throwable e) {
            if (exception.isAssignableFrom(e.getClass())) {
                // Expected
            }
            else if (e instanceof Error)
                throw (Error) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new AssertionError("Unexpected exception " + e.getClass(), e);
        }

        stream = data.parallelStream();
        fr = first.apply(stream);
        try {
            U sr = second.apply(stream);
            fail(text + " (par)");
        }
        catch (Throwable e) {
            if (exception.isAssignableFrom(e.getClass())) {
                // Expected
            }
            else if (e instanceof Error)
                throw (Error) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new AssertionError("Unexpected exception " + e.getClass(), e);
        }
    }

    // Stream

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTwoStreams(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          (Stream<Integer> s) -> s.map(i -> i), (Stream<Integer> s) -> s.map(i -> i),
                          IllegalStateException.class,
                          "Stream map / map succeeded erroneously");
        assertSecondFails(data,
                          Stream::distinct, (Stream<Integer> s) -> s.map(i -> i),
                          IllegalStateException.class,
                          "Stream distinct / map succeeded erroneously");
        assertSecondFails(data,
                          (Stream<Integer> s) -> s.map(i -> i), Stream::distinct,
                          IllegalStateException.class,
                          "Stream map / distinct succeeded erroneously");
        assertSecondFails(data,
                          Stream::distinct, Stream::distinct,
                          IllegalStateException.class,
                          "Stream distinct / distinct succeeded erroneously");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTwoTerminals(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          Stream::findFirst, Stream::findFirst,
                          IllegalStateException.class,
                          "Stream findFirst / findFirst succeeded erroneously");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTerminalStream(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          Stream::findFirst, (Stream<Integer> s) -> s.map(i -> i),
                          IllegalStateException.class,
                          "Stream findFirst / map succeeded erroneously");
        assertSecondFails(data,
                          (Stream<Integer> s) -> s.map(i -> i), Stream::findFirst,
                          IllegalStateException.class,
                          "Stream map / findFirst succeeded erroneously");
        assertSecondFails(data,
                          Stream::findFirst, Stream::distinct,
                          IllegalStateException.class,
                          "Stream findFirst / distinct succeeded erroneously");
        assertSecondFails(data,
                          Stream::distinct, Stream::findFirst,
                          IllegalStateException.class,
                          "Stream distinct / findFirst succeeded erroneously");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTwoIterators(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          Stream::iterator, Stream::iterator,
                          IllegalStateException.class,
                          "Stream iterator / iterator succeeded erroneously");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTerminalIterator(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          Stream::iterator, Stream::findFirst,
                          IllegalStateException.class,
                          "Stream iterator / findFirst succeeded erroneously");
        assertSecondFails(data,
                          Stream::findFirst, Stream::iterator,
                          IllegalStateException.class,
                          "Stream findFirst / iterator succeeded erroneously");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testStreamIterator(String name, TestData<Integer, Stream<Integer>> data) {
        assertSecondFails(data,
                          Stream::iterator, (Stream<Integer> s) -> s.map(i -> i),
                          IllegalStateException.class,
                          "Stream iterator / map succeeded erroneously");
        assertSecondFails(data,
                          (Stream<Integer> s) -> s.map(i -> i), Stream::iterator,
                          IllegalStateException.class,
                          "Stream map / iterator succeeded erroneously");
        assertSecondFails(data,
                          Stream::iterator, Stream::distinct,
                          IllegalStateException.class,
                          "Stream iterator / distinct succeeded erroneously");
        assertSecondFails(data,
                          Stream::distinct, Stream::iterator,
                          IllegalStateException.class,
                          "Stream distinct / iterator succeeded erroneously");
    }

    // IntStream

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testTwoStreams(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          (IntStream s) -> s.mapToObj(i -> i), (IntStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "IntStream map / map succeeded erroneously");
        assertSecondFails(data,
                          IntStream::distinct, (IntStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "IntStream distinct / map succeeded erroneously");
        assertSecondFails(data,
                          (IntStream s) -> s.mapToObj(i -> i), IntStream::distinct,
                          IllegalStateException.class,
                          "IntStream map / distinct succeeded erroneously");
        assertSecondFails(data,
                          IntStream::distinct, IntStream::distinct,
                          IllegalStateException.class,
                          "IntStream distinct / distinct succeeded erroneously");
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testTwoTerminals(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          IntStream::sum, IntStream::sum,
                          IllegalStateException.class,
                          "IntStream sum / sum succeeded erroneously");
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testTerminalStream(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          IntStream::sum, (IntStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "IntStream sum / map succeeded erroneously");
        assertSecondFails(data,
                          (IntStream s) -> s.mapToObj(i -> i), IntStream::sum,
                          IllegalStateException.class,
                          "IntStream map / sum succeeded erroneously");
        assertSecondFails(data,
                          IntStream::sum, IntStream::distinct,
                          IllegalStateException.class,
                          "IntStream sum / distinct succeeded erroneously");
        assertSecondFails(data,
                          IntStream::distinct, IntStream::sum,
                          IllegalStateException.class,
                          "IntStream distinct / sum succeeded erroneously");
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testTwoIterators(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          IntStream::iterator, IntStream::iterator,
                          IllegalStateException.class,
                          "IntStream iterator / iterator succeeded erroneously");
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testTerminalIterator(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          IntStream::iterator, IntStream::sum,
                          IllegalStateException.class,
                          "IntStream iterator / sum succeeded erroneously");
        assertSecondFails(data,
                          IntStream::sum, IntStream::iterator,
                          IllegalStateException.class,
                          "Stream sum / iterator succeeded erroneously");
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testStreamIterator(String name, TestData.OfInt data) {
        assertSecondFails(data,
                          IntStream::iterator, (IntStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "IntStream iterator / map succeeded erroneously");
        assertSecondFails(data,
                          (IntStream s) -> s.mapToObj(i -> i), IntStream::iterator,
                          IllegalStateException.class,
                          "IntStream map / iterator succeeded erroneously");
        assertSecondFails(data,
                          IntStream::iterator, IntStream::distinct,
                          IllegalStateException.class,
                          "IntStream iterator / distinct succeeded erroneously");
        assertSecondFails(data,
                          IntStream::distinct, IntStream::iterator,
                          IllegalStateException.class,
                          "IntStream distinct / iterator succeeded erroneously");
    }

    // LongStream

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testTwoStreams(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          (LongStream s) -> s.mapToObj(i -> i), (LongStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "LongStream map / map succeeded erroneously");
        assertSecondFails(data,
                          LongStream::distinct, (LongStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "LongStream distinct / map succeeded erroneously");
        assertSecondFails(data,
                          (LongStream s) -> s.mapToObj(i -> i), LongStream::distinct,
                          IllegalStateException.class,
                          "LongStream map / distinct succeeded erroneously");
        assertSecondFails(data,
                          LongStream::distinct, LongStream::distinct,
                          IllegalStateException.class,
                          "LongStream distinct / distinct succeeded erroneously");
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testTwoTerminals(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          LongStream::sum, LongStream::sum,
                          IllegalStateException.class,
                          "LongStream sum / sum succeeded erroneously");
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testTerminalStream(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          LongStream::sum, (LongStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "LongStream sum / map succeeded erroneously");
        assertSecondFails(data,
                          (LongStream s) -> s.mapToObj(i -> i), LongStream::sum,
                          IllegalStateException.class,
                          "LongStream map / sum succeeded erroneously");
        assertSecondFails(data,
                          LongStream::sum, LongStream::distinct,
                          IllegalStateException.class,
                          "LongStream sum / distinct succeeded erroneously");
        assertSecondFails(data,
                          LongStream::distinct, LongStream::sum,
                          IllegalStateException.class,
                          "LongStream distinct / sum succeeded erroneously");
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testTwoIterators(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          LongStream::iterator, LongStream::iterator,
                          IllegalStateException.class,
                          "LongStream iterator / iterator succeeded erroneously");
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testTerminalIterator(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          LongStream::iterator, LongStream::sum,
                          IllegalStateException.class,
                          "LongStream iterator / sum succeeded erroneously");
        assertSecondFails(data,
                          LongStream::sum, LongStream::iterator,
                          IllegalStateException.class,
                          "Stream sum / iterator succeeded erroneously");
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testStreamIterator(String name, TestData.OfLong data) {
        assertSecondFails(data,
                          LongStream::iterator, (LongStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "LongStream iterator / map succeeded erroneously");
        assertSecondFails(data,
                          (LongStream s) -> s.mapToObj(i -> i), LongStream::iterator,
                          IllegalStateException.class,
                          "LongStream map / iterator succeeded erroneously");
        assertSecondFails(data,
                          LongStream::iterator, LongStream::distinct,
                          IllegalStateException.class,
                          "LongStream iterator / distinct succeeded erroneously");
        assertSecondFails(data,
                          LongStream::distinct, LongStream::iterator,
                          IllegalStateException.class,
                          "LongStream distinct / iterator succeeded erroneously");
    }

    // DoubleStream

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testTwoStreams(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          (DoubleStream s) -> s.mapToObj(i -> i), (DoubleStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "DoubleStream map / map succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::distinct, (DoubleStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "DoubleStream distinct / map succeeded erroneously");
        assertSecondFails(data,
                          (DoubleStream s) -> s.mapToObj(i -> i), DoubleStream::distinct,
                          IllegalStateException.class,
                          "DoubleStream map / distinct succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::distinct, DoubleStream::distinct,
                          IllegalStateException.class,
                          "DoubleStream distinct / distinct succeeded erroneously");
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testTwoTerminals(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          DoubleStream::sum, DoubleStream::sum,
                          IllegalStateException.class,
                          "DoubleStream sum / sum succeeded erroneously");
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testTerminalStream(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          DoubleStream::sum, (DoubleStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "DoubleStream sum / map succeeded erroneously");
        assertSecondFails(data,
                          (DoubleStream s) -> s.mapToObj(i -> i), DoubleStream::sum,
                          IllegalStateException.class,
                          "DoubleStream map / sum succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::sum, DoubleStream::distinct,
                          IllegalStateException.class,
                          "DoubleStream sum / distinct succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::distinct, DoubleStream::sum,
                          IllegalStateException.class,
                          "DoubleStream distinct / sum succeeded erroneously");
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testTwoIterators(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          DoubleStream::iterator, DoubleStream::iterator,
                          IllegalStateException.class,
                          "DoubleStream iterator / iterator succeeded erroneously");
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testTerminalIterator(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          DoubleStream::iterator, DoubleStream::sum,
                          IllegalStateException.class,
                          "DoubleStream iterator / sum succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::sum, DoubleStream::iterator,
                          IllegalStateException.class,
                          "Stream sum / iterator succeeded erroneously");
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testStreamIterator(String name, TestData.OfDouble data) {
        assertSecondFails(data,
                          DoubleStream::iterator, (DoubleStream s) -> s.mapToObj(i -> i),
                          IllegalStateException.class,
                          "DoubleStream iterator / map succeeded erroneously");
        assertSecondFails(data,
                          (DoubleStream s) -> s.mapToObj(i -> i), DoubleStream::iterator,
                          IllegalStateException.class,
                          "DoubleStream map / iterator succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::iterator, DoubleStream::distinct,
                          IllegalStateException.class,
                          "DoubleStream iterator / distinct succeeded erroneously");
        assertSecondFails(data,
                          DoubleStream::distinct, DoubleStream::iterator,
                          IllegalStateException.class,
                          "DoubleStream distinct / iterator succeeded erroneously");
    }
}
