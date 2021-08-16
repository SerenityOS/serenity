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

import java.util.stream.*;

import static java.util.stream.LambdaTestHelpers.*;

/**
 * MapOpTest
 *
 * @author Brian Goetz
 */
@Test
public class MapOpTest extends OpTestCase {

    public void testMap() {
        assertCountSum(countTo(0).stream().map(mId), 0, 0);
        assertCountSum(countTo(10).stream().map(mId), 10, 55);
        assertCountSum(countTo(10).stream().map(mZero), 10, 0);
        assertCountSum(countTo(0).stream().map(mDoubler), 0, 0);
        assertCountSum(countTo(10).stream().map(mDoubler), 10, 110);
        assertCountSum(countTo(10).stream().map(mDoubler).map(mDoubler), 10, 220);

        exerciseOps(countTo(0), s -> s.map(LambdaTestHelpers.identity()), countTo(0));
        exerciseOps(countTo(1000), s -> s.map(LambdaTestHelpers.identity()), countTo(1000));
        // @@@ Force cast to integer so output is Stream<Integer> rather an IntStream
        //     this just ensures that no warnings are logged about boxing
        //     when the result is compared with the output
        exerciseOps(countTo(1000), s -> s.map(e -> (Integer) (1000 + e)), range(1001, 2000));
    }

    public void testEveryMapShape() {
        assertCountSum(countTo(1000).stream()
                               .mapToInt(i -> i - 1)
                               .mapToObj(i -> i + 1)
                               .mapToLong(i -> i - 1)
                               .mapToObj(i -> i + 1)
                               .mapToDouble(i -> i - 1)
                               .mapToObj(i -> i + 1)
                               .mapToInt(i -> (int) (double) i)
                               .mapToLong(i -> i)
                               .mapToDouble(i -> i)
                               .mapToLong(i -> (long) i)
                               .mapToInt(i -> (int) i)
                               .mapToObj(i -> i),
                       1000, countTo(1000).stream().mapToInt(i -> i).sum());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        exerciseOpsInt(data, s -> s.map(mId), s -> s.map(e -> e), s -> s.map(e -> e), s -> s.map(e -> e));
        exerciseOpsInt(data, s -> s.map(mZero), s -> s.map(e -> 0), s -> s.map(e -> 0), s -> s.map(e -> 0));
        exerciseOpsInt(data, s -> s.map(mDoubler), s -> s.map(e -> 2*e), s -> s.map(e -> 2*e), s -> s.map(e -> 2*e));
        exerciseOpsInt(data, s -> s.map(LambdaTestHelpers.compose(mId, mDoubler)), s -> s.map(e -> 2*e), s -> s.map(e -> 2*e), s -> s.map(e -> 2*e));
        exerciseOpsInt(data, s -> s.map(LambdaTestHelpers.compose(mDoubler, mDoubler)), s -> s.map(e -> 4*e), s -> s.map(e -> 4*e), s -> s.map(e -> 4*e));
        exerciseOps(data, s -> s.mapToInt(i -> i));
        exerciseOps(data, s -> s.mapToLong(i -> i));
        exerciseOps(data, s -> s.mapToDouble(i -> i));
    }

    //

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOps(String name, TestData.OfInt data) {
        exerciseOps(data, s -> s.mapToObj(i -> i));
        exerciseOps(data, s -> s.map(i -> 0));
        exerciseOps(data, s -> s.map(i -> i * 2));
        exerciseOps(data, s -> s.asLongStream());
        exerciseOps(data, s -> s.asDoubleStream());
        exerciseOps(data, s -> s.boxed());
        exerciseOps(data, s -> s.mapToObj(Integer::toString));
        exerciseOps(data, s -> s.mapToLong(i -> i));
        exerciseOps(data, s -> s.mapToDouble(i -> i));
    }

    //

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, TestData.OfLong data) {
        exerciseOps(data, s -> s.mapToObj(i -> i));
        exerciseOps(data, s -> s.map(i -> 0L));
        exerciseOps(data, s -> s.map(i -> i * 2L));
        exerciseOps(data, s -> s.asDoubleStream());
        exerciseOps(data, s -> s.boxed());
        exerciseOps(data, s -> s.mapToObj(e -> Long.toString(e)));
        exerciseOps(data, s -> s.mapToInt(i -> (int) i));
        exerciseOps(data, s -> s.mapToDouble(i -> i));
    }

    //

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, TestData.OfDouble data) {
        exerciseOps(data, s -> s.mapToObj(i -> i));
        exerciseOps(data, s -> s.map(i -> 0.0));
        exerciseOps(data, s -> s.map(i -> i * 2.0));
        exerciseOps(data, s -> s.boxed());
        exerciseOps(data, s -> s.mapToObj(e -> Double.toString(e)));
        exerciseOps(data, s -> s.mapToLong(i -> (long) i));
        exerciseOps(data, s -> s.mapToInt(i -> (int) i));
    }
}
