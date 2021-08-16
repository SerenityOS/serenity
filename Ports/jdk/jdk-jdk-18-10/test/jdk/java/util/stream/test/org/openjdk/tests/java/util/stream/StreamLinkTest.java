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

import java.util.stream.*;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Test
public class StreamLinkTest extends OpTestCase {

    private <S> Function<S, S> apply(int n, Function<S, S> f) {
        return s -> {
            for (int i = 0; i < n; i++) {
                s = f.apply(s);
            }
            return s;
        };
    }

    private List<Integer> sizes = Arrays.asList(0, 1, 2, 3, 4, 5, 255, 1000);

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testManyStreams(String name, TestData.OfRef<Integer> data) {
        for (int n : sizes) {
            setContext("n", n);
            List<Integer> expected = data.stream().map(e -> (Integer) (e + n)).collect(Collectors.toList());

            withData(data).
                    stream(apply(n, (Stream<Integer> s) -> s.map(e -> (Integer) (e + 1)))).
                    expectedResult(expected).
                    exercise();
        }
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntManyStreams(String name, TestData.OfInt data) {
        for (int n : sizes) {
            setContext("n", n);
            int[] expected = data.stream().map(e -> e + n).toArray();

            withData(data).
                    stream(apply(n, (IntStream s) -> s.map(e -> e + 1))).
                    expectedResult(expected).
                    exercise();
        }
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongManyStreams(String name, TestData.OfLong data) {
        for (int n : sizes) {
            setContext("n", n);
            long[] expected = data.stream().map(e -> e + n).toArray();

            withData(data).
                    stream(apply(n, (LongStream s) -> s.map(e -> e + 1L))).
                    expectedResult(expected).
                    exercise();
        }
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleManyStreams(String name, TestData.OfDouble data) {
        for (int n : sizes) {
            setContext("n", n);
            double[] expected = data.stream().map(e -> accumulate(e, n)).toArray();

            withData(data).
                    stream(apply(n, (DoubleStream s) -> s.map(e -> e + 1.0))).
                    expectedResult(expected).
                    exercise();
        }
    }
    private double accumulate(double e, int n) {
        while (n-- > 0) {
            e = e + 1.0;
        }
        return e;
    }
}
