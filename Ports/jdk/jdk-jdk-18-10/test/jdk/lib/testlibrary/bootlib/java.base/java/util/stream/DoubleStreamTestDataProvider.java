/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;

import java.util.*;
import java.util.Spliterators;
import java.util.function.Supplier;

/** TestNG DataProvider for double-valued streams */
public class DoubleStreamTestDataProvider {
    private static final double[] to0 = new double[0];
    private static final double[] to1 = new double[1];
    private static final double[] to10 = new double[10];
    private static final double[] to100 = new double[100];
    private static final double[] to1000 = new double[1000];
    private static final double[] reversed = new double[100];
    private static final double[] ones = new double[100];
    private static final double[] twice = new double[200];
    private static final double[] pseudoRandom;

    private static final Object[][] testData;
    private static final Object[][] testSmallData;
    private static final Object[][] spliteratorTestData;

    static {
        double[][] arrays = {to0, to1, to10, to100, to1000};
        for (double[] arr : arrays) {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i;
            }
        }
        for (int i = 0; i < reversed.length; i++) {
            reversed[i] = reversed.length - i;
        }
        for (int i = 0; i < ones.length; i++) {
            ones[i] = 1;
        }
        System.arraycopy(to100, 0, twice, 0, to100.length);
        System.arraycopy(to100, 0, twice, to100.length, to100.length);
        pseudoRandom = new double[LambdaTestHelpers.LONG_STRING.length()];
        for (int i = 0; i < LambdaTestHelpers.LONG_STRING.length(); i++) {
            pseudoRandom[i] = (double) LambdaTestHelpers.LONG_STRING.charAt(i);
        }
    }

    static final Object[][] arrays = {
            {"empty", to0},
            {"0..1", to1},
            {"0..10", to10},
            {"0..100", to100},
            {"0..1000", to1000},
            {"100x[1]", ones},
            {"2x[0..100]", twice},
            {"reverse 0..100", reversed},
            {"pseudorandom", pseudoRandom}
    };

    static {
        {
            List<Object[]> listSmall = new ArrayList<>();
            List<Object[]> list1000 = new ArrayList<>();
            List<Object[]> list = null;
            for (Object[] data : arrays) {
                final Object name = data[0];
                final double[] doubles = (double[]) data[1];

                list = doubles.length >= 1000 ? list1000 : listSmall;

                list.add(new Object[]{"array:" + name,
                        TestData.Factory.ofArray("array:" + name, doubles)});

                SpinedBuffer.OfDouble isl = new SpinedBuffer.OfDouble();
                for (double i : doubles) {
                    isl.accept(i);
                }
                list.add(new Object[]{"SpinedList:" + name,
                        TestData.Factory.ofSpinedBuffer("SpinedList:" + name, isl)});
            }
            testSmallData = listSmall.toArray(new Object[0][]);
            list1000.addAll(listSmall);
            testData = list1000.toArray(new Object[0][]);
        }

        {
            List<Object[]> spliterators = new ArrayList<>();
            for (Object[] data : arrays) {
                final Object name = data[0];
                final double[] doubles = (double[]) data[1];

                SpinedBuffer.OfDouble isl = new SpinedBuffer.OfDouble();
                for (double i : doubles) {
                    isl.accept(i);
                }

                spliterators.add(splitDescr("Arrays.s(array):" + name,
                                            () -> Arrays.spliterator(doubles)));
                spliterators.add(splitDescr("Arrays.s(array,o,l):" + name,
                                            () -> Arrays.spliterator(doubles, 0, doubles.length / 2)));

                spliterators.add(splitDescr("SpinedBuffer.s():" + name,
                                            () -> isl.spliterator()));

                spliterators.add(splitDescr("Primitives.s(SpinedBuffer.iterator(), size):" + name,
                                            () -> Spliterators.spliterator(isl.iterator(), doubles.length, 0)));
                spliterators.add(splitDescr("Primitives.s(SpinedBuffer.iterator()):" + name,
                                            () -> Spliterators.spliteratorUnknownSize(isl.iterator(), 0)));
                spliterators.add(splitDescr("DoubleStream.iterate(0,x->x<l;x->x+1):" + name,
                                            () -> DoubleStream.iterate(0.0, x -> x < doubles.length, x -> x + 1.0)
                                                              .spliterator()));
                // Need more!
            }
            spliteratorTestData = spliterators.toArray(new Object[0][]);
        }

    }

    static <T> Object[] splitDescr(String description, Supplier<Spliterator.OfDouble> s) {
        return new Object[] { description, s };
    }

    // Return an array of ( String name, DoubleStreamTestData )
    @DataProvider(name = "DoubleStreamTestData")
    public static Object[][] makeDoubleStreamTestData() {
        return testData;
    }

    @DataProvider(name = "DoubleStreamTestData.small")
    public static Object[][] makeSmallDoubleStreamTestData() {
        return testSmallData;
    }

    // returns an array of (String name, Supplier<PrimitiveSpliterator<Double>>)
    @DataProvider(name = "DoubleSpliterator")
    public static Object[][] spliteratorProvider() {
        return spliteratorTestData;
    }
}
