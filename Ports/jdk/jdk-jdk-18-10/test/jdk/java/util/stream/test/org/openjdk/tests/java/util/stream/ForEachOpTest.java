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

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.stream.LambdaTestHelpers.countTo;

/**
 * ForEachOpTest
 */
@Test
public class ForEachOpTest extends OpTestCase {

    @Test(groups = { "serialization-hostile" })
    public void testForEach() {
        exerciseTerminalOps(countTo(10),
                            s -> {
                                AtomicInteger count = new AtomicInteger(0);
                                s.forEach(e -> count.incrementAndGet());
                                return count.get();
                            },
                            10);

        exerciseTerminalOps(countTo(10),
                            s -> {
                                AtomicInteger sum = new AtomicInteger(0);
                                s.forEach(sum::addAndGet);
                                return sum.get();
                            },
                            55);
    }

    private <U> ResultAsserter<List<U>> resultAsserter() {
        return (act, exp, ord, par) -> {
            if (par) {
                LambdaTestHelpers.assertContentsUnordered(act, exp);
            }
            else {
                LambdaTestHelpers.assertContents(act, exp);
            }
        };
    }

    @Test(groups = { "serialization-hostile" })
    public void testForEachOrdered() {
        List<Integer> input = countTo(10000);
        TestData.OfRef<Integer> data = TestData.Factory.ofCollection("[1, 10000]", input);

        Function<Stream<Integer>, List<Integer>> terminalFunc = s -> {
            List<Integer> l = new ArrayList<>();
            s.forEachOrdered(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                expectedResult(input).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(LambdaTestHelpers.identity()), terminalFunc).
                expectedResult(input).
                exercise();
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testForEach(String name, TestData.OfRef<Integer> data) {
        Function<Stream<Integer>, List<Integer>> terminalFunc = s -> {
            List<Integer> l = Collections.synchronizedList(new ArrayList<>());
            s.forEach(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(LambdaTestHelpers.identity()), terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();
    }

    //

    @Test(groups = { "serialization-hostile" })
    public void testIntForEachOrdered() {
        List<Integer> input = countTo(10000);
        TestData.OfInt data = TestData.Factory.ofIntSupplier("[1, 10000]",
                                                             () -> IntStream.range(1, 10001));

        Function<IntStream, List<Integer>> terminalFunc = s -> {
            List<Integer> l = new ArrayList<>();
            s.forEachOrdered(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                expectedResult(input).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                expectedResult(input).
                exercise();
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntForEach(String name, TestData.OfInt data) {
        Function<IntStream, List<Integer>> terminalFunc = s -> {
            List<Integer> l = Collections.synchronizedList(new ArrayList<Integer>());
            s.forEach(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();
    }

    //

    @Test(groups = { "serialization-hostile" })
    public void testLongForEachOrdered() {
        List<Integer> input = countTo(10000);
        TestData.OfLong data = TestData.Factory.ofLongSupplier("[1, 10000]",
                                                               () -> LongStream.range(1, 10001));

        Function<LongStream, List<Integer>> terminalFunc = s -> {
            List<Integer> l = new ArrayList<>();
            s.forEachOrdered(e -> l.add((int) e));
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                expectedResult(input).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                expectedResult(input).
                exercise();
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, TestData.OfLong data) {
        Function<LongStream, List<Long>> terminalFunc = s -> {
            List<Long> l = Collections.synchronizedList(new ArrayList<Long>());
            s.forEach(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();
    }

    //

    @Test(groups = { "serialization-hostile" })
    public void testDoubleForEachOrdered() {
        List<Integer> input = countTo(10000);
        TestData.OfDouble data = TestData.Factory.ofDoubleSupplier("[1, 10000]",
                                                                   () -> IntStream.range(1, 10001).asDoubleStream());

        Function<DoubleStream, List<Integer>> terminalFunc = s -> {
            List<Integer> l = new ArrayList<>();
            s.forEachOrdered(e -> l.add((int) e));
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                expectedResult(input).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                expectedResult(input).
                exercise();
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, TestData.OfDouble data) {
        Function<DoubleStream, List<Double>> terminalFunc = s -> {
            List<Double> l = Collections.synchronizedList(new ArrayList<Double>());
            s.forEach(l::add);
            return l;
        };

        // Test head
        withData(data).
                terminal(terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();

        // Test multiple stages
        withData(data).
                terminal(s -> s.map(i -> i), terminalFunc).
                resultAsserter(resultAsserter()).
                exercise();
    }

}
