/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * StreamTestDataProvider
 *
 * @author Brian Goetz
 */
/** TestNG DataProvider for ref-valued streams */
public class StreamTestDataProvider {
    private static final Integer[] to0 = new Integer[0];
    private static final Integer[] to1 = new Integer[1];
    private static final Integer[] to10 = new Integer[10];
    private static final Integer[] to100 = new Integer[100];
    private static final Integer[] to1000 = new Integer[1000];
    private static final Integer[] reversed = new Integer[100];
    private static final Integer[] ones = new Integer[100];
    private static final Integer[] twice = new Integer[200];
    private static final Integer[] pseudoRandom;

    private static final Object[][] testData;
    private static final Object[][] testSmallData;
    private static final Object[][] testMiniData;
    private static final Object[][] withNullTestData;
    private static final Object[][] spliteratorTestData;

    static {
        Integer[][] arrays = {to0, to1, to10, to100, to1000};
        for (Integer[] arr : arrays) {
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
        pseudoRandom = new Integer[LambdaTestHelpers.LONG_STRING.length()];
        for (int i = 0; i < LambdaTestHelpers.LONG_STRING.length(); i++) {
            pseudoRandom[i] = (int) LambdaTestHelpers.LONG_STRING.charAt(i);
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
            List<Object[]> listMini = new ArrayList<>();
            List<Object[]> listSmall = new ArrayList<>();
            List<Object[]> list1000 = new ArrayList<>();
            List<Object[]> list = null;
            for (Object[] data : arrays) {
                final Object name = data[0];
                final Integer[] ints = (Integer[])data[1];
                final List<Integer> intsAsList = Arrays.asList(ints);

                list = ints.length >= 1000 ? list1000 : (ints.length >= 100 ? listSmall : listMini);

                list.add(arrayDataDescr("array:" + name, ints));
                list.add(collectionDataDescr("ArrayList.asList:" + name, intsAsList));
                list.add(collectionDataDescr("ArrayList:" + name, new ArrayList<>(intsAsList)));
                list.add(streamDataDescr("DelegatingStream(ArrayList):" + name,
                                         () -> new ArrayList<>(intsAsList).stream()));
                List<Integer> aList = new ArrayList<>(intsAsList);
                if (LambdaTestMode.isNormalMode()) {
                    // Only include sub-lists for normal test execution mode
                    // This data is serialization-hostile since the state of the
                    // deserialized sub-list will be out of sync with the
                    // enclosing list.
                    list.add(collectionDataDescr("ArrayList.Sublist:" + name,
                                                 (ints.length) <= 1 ? aList.subList(0, 0) : aList.subList(1, ints.length / 2)));
                }
                list.add(collectionDataDescr("LinkedList:" + name, new LinkedList<>(intsAsList)));
                list.add(collectionDataDescr("HashSet:" + name, new HashSet<>(intsAsList)));
                list.add(collectionDataDescr("LinkedHashSet:" + name, new LinkedHashSet<>(intsAsList)));
                list.add(collectionDataDescr("TreeSet:" + name, new TreeSet<>(intsAsList)));
                SpinedBuffer<Integer> spinedBuffer = new SpinedBuffer<>();
                intsAsList.forEach(spinedBuffer);
                list.add(sbDataDescr("SpinedBuffer:" + name, spinedBuffer));

                // @@@ Add more
            }
            testMiniData = listMini.toArray(new Object[0][]);
            listSmall.addAll(listMini);
            testSmallData = listSmall.toArray(new Object[0][]);
            list1000.addAll(listSmall);
            testData = list1000.toArray(new Object[0][]);
        }

        // Simple combination of numbers and null values, probably excessive but may catch
        // errors for initialization/termination/sequence
        // @@@ This is separate from the other data for now until nulls are consistently supported by
        // all operations
        {
            List<Object[]> list = new ArrayList<>();
            int size = 5;
            for (int i = 0; i < (1 << size) - 2; i++) {
                Integer[] content = new Integer[size];
                for (int e = 0; e < size; e++) {
                    content[e] = (i & (1 << e)) > 0 ? e + 1 : null;
                }

                // ORDERED
                list.add(arrayDataDescr("array:" + i, content));
                // not ORDERED, DISTINCT
                list.add(collectionDataDescr("HashSet:" + i, new HashSet<>(Arrays.asList(content))));
            }

            withNullTestData = list.toArray(new Object[0][]);
        }

        {
            List<Object[]> spliterators = new ArrayList<>();
            for (Object[] data : arrays) {
                final Object name = data[0];
                final Integer[] ints = (Integer[])data[1];

                spliterators.add(splitDescr("Arrays.s(array):" + name,
                                            () -> Arrays.spliterator(ints)));
                spliterators.add(splitDescr("arrays.s(array,o,l):" + name,
                                            () -> Arrays.spliterator(ints, 0, ints.length/2)));
                spliterators.add(splitDescr("SpinedBuffer.s():" + name,
                                            () -> {
                                                SpinedBuffer<Integer> sb = new SpinedBuffer<>();
                                                for (Integer i : ints)
                                                    sb.accept(i);
                                                return sb.spliterator();
                                            }));
                spliterators.add(splitDescr("Iterators.s(Arrays.s(array).iterator(), size):" + name,
                                            () -> Spliterators.spliterator(Arrays.asList(ints).iterator(), ints.length, 0)));
                spliterators.add(splitDescr("Iterators.s(Arrays.s(array).iterator()):" + name,
                                            () -> Spliterators.spliteratorUnknownSize(Arrays.asList(ints).iterator(), 0)));
                spliterators.add(splitDescr("Stream.iterate(0,x->x<l,x->x+1): " + name,
                                            () -> Stream.iterate(0, x -> x < ints.length, x -> x + 1).spliterator()));
                // @@@ Add map and collection spliterators when spliterator() is exposed on Collection or Iterable
            }
            spliteratorTestData = spliterators.toArray(new Object[0][]);
        }
    }

    static <T> Object[] arrayDataDescr(String description, T[] data) {
        return new Object[] { description, TestData.Factory.ofArray(description, data)};
    }

    static <T> Object[] streamDataDescr(String description, Supplier<Stream<T>> supplier) {
        return new Object[] { description, TestData.Factory.ofSupplier(description, supplier)};
    }

    static <T> Object[] collectionDataDescr(String description, Collection<T> data) {
        return new Object[] { description, TestData.Factory.ofCollection(description, data)};
    }

    static <T> Object[] sbDataDescr(String description, SpinedBuffer<T> data) {
        return new Object[] { description, TestData.Factory.ofSpinedBuffer(description, data)};
    }

    static <T> Object[] splitDescr(String description, Supplier<Spliterator<T>> ss) {
        return new Object[] { description, ss };
    }

    // Return an array of ( String name, StreamTestData<Integer> )
    @DataProvider(name = "StreamTestData<Integer>")
    public static Object[][] makeStreamTestData() {
        return testData;
    }

    @DataProvider(name = "StreamTestData<Integer>.small")
    public static Object[][] makeSmallStreamTestData() {
        return testSmallData;
    }

    @DataProvider(name = "StreamTestData<Integer>.mini")
    public static Object[][] makeMiniStreamTestData() {
        return testMiniData;
    }

    @DataProvider(name = "withNull:StreamTestData<Integer>")
    public static Object[][] makeStreamWithNullTestData() {
        return withNullTestData;
    }

    // returns an array of (String name, Supplier<Spliterator<Integer>>)
    @DataProvider(name = "Spliterator<Integer>")
    public static Object[][] spliteratorProvider() {
        return spliteratorTestData;
    }
}
