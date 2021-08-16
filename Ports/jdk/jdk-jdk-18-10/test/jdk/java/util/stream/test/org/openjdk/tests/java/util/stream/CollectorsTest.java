/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamOpFlagTestHelper;
import java.util.stream.StreamTestDataProvider;
import java.util.stream.TestData;

import org.testng.annotations.Test;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.LambdaTestHelpers.assertContents;
import static java.util.stream.LambdaTestHelpers.assertContentsUnordered;
import static java.util.stream.LambdaTestHelpers.mDoubler;

/*
 * @test
 * @bug 8071600 8144675
 * @summary Test for collectors.
 */
public class CollectorsTest extends OpTestCase {

    private abstract static class CollectorAssertion<T, U> {
        abstract void assertValue(U value,
                                  Supplier<Stream<T>> source,
                                  boolean ordered) throws ReflectiveOperationException;
    }

    static class MappingAssertion<T, V, R> extends CollectorAssertion<T, R> {
        private final Function<T, V> mapper;
        private final CollectorAssertion<V, R> downstream;

        MappingAssertion(Function<T, V> mapper, CollectorAssertion<V, R> downstream) {
            this.mapper = mapper;
            this.downstream = downstream;
        }

        @Override
        void assertValue(R value, Supplier<Stream<T>> source, boolean ordered) throws ReflectiveOperationException {
            downstream.assertValue(value,
                                   () -> source.get().map(mapper),
                                   ordered);
        }
    }

    static class FlatMappingAssertion<T, V, R> extends CollectorAssertion<T, R> {
        private final Function<T, Stream<V>> mapper;
        private final CollectorAssertion<V, R> downstream;

        FlatMappingAssertion(Function<T, Stream<V>> mapper,
                             CollectorAssertion<V, R> downstream) {
            this.mapper = mapper;
            this.downstream = downstream;
        }

        @Override
        void assertValue(R value, Supplier<Stream<T>> source, boolean ordered) throws ReflectiveOperationException {
            downstream.assertValue(value,
                                   () -> source.get().flatMap(mapper),
                                   ordered);
        }
    }

    static class FilteringAssertion<T, R> extends CollectorAssertion<T, R> {
        private final Predicate<T> filter;
        private final CollectorAssertion<T, R> downstream;

        public FilteringAssertion(Predicate<T> filter, CollectorAssertion<T, R> downstream) {
            this.filter = filter;
            this.downstream = downstream;
        }

        @Override
        void assertValue(R value, Supplier<Stream<T>> source, boolean ordered) throws ReflectiveOperationException {
            downstream.assertValue(value,
                                   () -> source.get().filter(filter),
                                   ordered);
        }
    }

    static class GroupingByAssertion<T, K, V, M extends Map<K, ? extends V>> extends CollectorAssertion<T, M> {
        private final Class<? extends Map> clazz;
        private final Function<T, K> classifier;
        private final CollectorAssertion<T,V> downstream;

        GroupingByAssertion(Function<T, K> classifier, Class<? extends Map> clazz,
                            CollectorAssertion<T, V> downstream) {
            this.clazz = clazz;
            this.classifier = classifier;
            this.downstream = downstream;
        }

        @Override
        void assertValue(M map,
                         Supplier<Stream<T>> source,
                         boolean ordered) throws ReflectiveOperationException {
            if (!clazz.isAssignableFrom(map.getClass()))
                fail(String.format("Class mismatch in GroupingByAssertion: %s, %s", clazz, map.getClass()));
            assertContentsUnordered(map.keySet(), source.get().map(classifier).collect(toSet()));
            for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
                K key = entry.getKey();
                downstream.assertValue(entry.getValue(),
                                       () -> source.get().filter(e -> classifier.apply(e).equals(key)),
                                       ordered);
            }
        }
    }

    static class ToMapAssertion<T, K, V, M extends Map<K,V>> extends CollectorAssertion<T, M> {
        private final Class<? extends Map> clazz;
        private final Function<T, K> keyFn;
        private final Function<T, V> valueFn;
        private final BinaryOperator<V> mergeFn;

        ToMapAssertion(Function<T, K> keyFn,
                       Function<T, V> valueFn,
                       BinaryOperator<V> mergeFn,
                       Class<? extends Map> clazz) {
            this.clazz = clazz;
            this.keyFn = keyFn;
            this.valueFn = valueFn;
            this.mergeFn = mergeFn;
        }

        @Override
        void assertValue(M map, Supplier<Stream<T>> source, boolean ordered) throws ReflectiveOperationException {
            if (!clazz.isAssignableFrom(map.getClass()))
                fail(String.format("Class mismatch in ToMapAssertion: %s, %s", clazz, map.getClass()));
            Set<K> uniqueKeys = source.get().map(keyFn).collect(toSet());
            assertEquals(uniqueKeys, map.keySet());
            source.get().forEach(t -> {
                K key = keyFn.apply(t);
                V v = source.get()
                            .filter(e -> key.equals(keyFn.apply(e)))
                            .map(valueFn)
                            .reduce(mergeFn)
                            .get();
                assertEquals(map.get(key), v);
            });
        }
    }

    static class PartitioningByAssertion<T, D> extends CollectorAssertion<T, Map<Boolean,D>> {
        private final Predicate<T> predicate;
        private final CollectorAssertion<T,D> downstream;

        PartitioningByAssertion(Predicate<T> predicate, CollectorAssertion<T, D> downstream) {
            this.predicate = predicate;
            this.downstream = downstream;
        }

        @Override
        void assertValue(Map<Boolean, D> map,
                         Supplier<Stream<T>> source,
                         boolean ordered) throws ReflectiveOperationException {
            if (!Map.class.isAssignableFrom(map.getClass()))
                fail(String.format("Class mismatch in PartitioningByAssertion: %s", map.getClass()));
            assertEquals(2, map.size());
            downstream.assertValue(map.get(true), () -> source.get().filter(predicate), ordered);
            downstream.assertValue(map.get(false), () -> source.get().filter(predicate.negate()), ordered);
        }
    }

    static class ToListAssertion<T> extends CollectorAssertion<T, List<T>> {
        @Override
        void assertValue(List<T> value, Supplier<Stream<T>> source, boolean ordered)
                throws ReflectiveOperationException {
            if (!List.class.isAssignableFrom(value.getClass()))
                fail(String.format("Class mismatch in ToListAssertion: %s", value.getClass()));
            Stream<T> stream = source.get();
            List<T> result = new ArrayList<>();
            for (Iterator<T> it = stream.iterator(); it.hasNext(); ) // avoid capturing result::add
                result.add(it.next());
            if (StreamOpFlagTestHelper.isStreamOrdered(stream) && ordered)
                assertContents(value, result);
            else
                assertContentsUnordered(value, result);
        }
    }

    static class ToCollectionAssertion<T> extends CollectorAssertion<T, Collection<T>> {
        private final Class<? extends Collection> clazz;
        private final boolean targetOrdered;

        ToCollectionAssertion(Class<? extends Collection> clazz, boolean targetOrdered) {
            this.clazz = clazz;
            this.targetOrdered = targetOrdered;
        }

        @Override
        void assertValue(Collection<T> value, Supplier<Stream<T>> source, boolean ordered)
                throws ReflectiveOperationException {
            if (!clazz.isAssignableFrom(value.getClass()))
                fail(String.format("Class mismatch in ToCollectionAssertion: %s, %s", clazz, value.getClass()));
            Stream<T> stream = source.get();
            Collection<T> result = clazz.newInstance();
            for (Iterator<T> it = stream.iterator(); it.hasNext(); ) // avoid capturing result::add
                result.add(it.next());
            if (StreamOpFlagTestHelper.isStreamOrdered(stream) && targetOrdered && ordered)
                assertContents(value, result);
            else
                assertContentsUnordered(value, result);
        }
    }

    static class ReducingAssertion<T, U> extends CollectorAssertion<T, U> {
        private final U identity;
        private final Function<T, U> mapper;
        private final BinaryOperator<U> reducer;

        ReducingAssertion(U identity, Function<T, U> mapper, BinaryOperator<U> reducer) {
            this.identity = identity;
            this.mapper = mapper;
            this.reducer = reducer;
        }

        @Override
        void assertValue(U value, Supplier<Stream<T>> source, boolean ordered)
                throws ReflectiveOperationException {
            Optional<U> reduced = source.get().map(mapper).reduce(reducer);
            if (value == null)
                assertTrue(!reduced.isPresent());
            else if (!reduced.isPresent()) {
                assertEquals(value, identity);
            }
            else {
                assertEquals(value, reduced.get());
            }
        }
    }

    static class TeeingAssertion<T, R1, R2, RR> extends CollectorAssertion<T, RR> {
        private final Collector<T, ?, R1> c1;
        private final Collector<T, ?, R2> c2;
        private final BiFunction<? super R1, ? super R2, ? extends RR> finisher;

        TeeingAssertion(Collector<T, ?, R1> c1, Collector<T, ?, R2> c2,
                               BiFunction<? super R1, ? super R2, ? extends RR> finisher) {
            this.c1 = c1;
            this.c2 = c2;
            this.finisher = finisher;
        }

        @Override
        void assertValue(RR value, Supplier<Stream<T>> source, boolean ordered) {
            R1 r1 = source.get().collect(c1);
            R2 r2 = source.get().collect(c2);
            RR expected = finisher.apply(r1, r2);
            assertEquals(value, expected);
        }
    }

    private <T> ResultAsserter<T> mapTabulationAsserter(boolean ordered) {
        return (act, exp, ord, par) -> {
            if (par && (!ordered || !ord)) {
                CollectorsTest.nestedMapEqualityAssertion(act, exp);
            }
            else {
                LambdaTestHelpers.assertContentsEqual(act, exp);
            }
        };
    }

    private<T, M extends Map>
    void exerciseMapCollection(TestData<T, Stream<T>> data,
                               Collector<T, ?, ? extends M> collector,
                               CollectorAssertion<T, M> assertion)
            throws ReflectiveOperationException {
        boolean ordered = !collector.characteristics().contains(Collector.Characteristics.UNORDERED);

        M m = withData(data)
                .terminal(s -> s.collect(collector))
                .resultAsserter(mapTabulationAsserter(ordered))
                .exercise();
        assertion.assertValue(m, () -> data.stream(), ordered);

        m = withData(data)
                .terminal(s -> s.unordered().collect(collector))
                .resultAsserter(mapTabulationAsserter(ordered))
                .exercise();
        assertion.assertValue(m, () -> data.stream(), false);
    }

    private static void nestedMapEqualityAssertion(Object o1, Object o2) {
        if (o1 instanceof Map) {
            Map m1 = (Map) o1;
            Map m2 = (Map) o2;
            assertContentsUnordered(m1.keySet(), m2.keySet());
            for (Object k : m1.keySet())
                nestedMapEqualityAssertion(m1.get(k), m2.get(k));
        }
        else if (o1 instanceof Collection) {
            assertContentsUnordered(((Collection) o1), ((Collection) o2));
        }
        else
            assertEquals(o1, o2);
    }

    private<T, R> void assertCollect(TestData.OfRef<T> data,
                                     Collector<T, ?, R> collector,
                                     Function<Stream<T>, R> streamReduction) {
        R check = streamReduction.apply(data.stream());
        withData(data).terminal(s -> s.collect(collector)).expectedResult(check).exercise();
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testReducing(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        assertCollect(data, Collectors.reducing(0, Integer::sum),
                      s -> s.reduce(0, Integer::sum));
        assertCollect(data, Collectors.reducing(Integer.MAX_VALUE, Integer::min),
                      s -> s.min(Integer::compare).orElse(Integer.MAX_VALUE));
        assertCollect(data, Collectors.reducing(Integer.MIN_VALUE, Integer::max),
                      s -> s.max(Integer::compare).orElse(Integer.MIN_VALUE));

        assertCollect(data, Collectors.reducing(Integer::sum),
                      s -> s.reduce(Integer::sum));
        assertCollect(data, Collectors.minBy(Comparator.naturalOrder()),
                      s -> s.min(Integer::compare));
        assertCollect(data, Collectors.maxBy(Comparator.naturalOrder()),
                      s -> s.max(Integer::compare));

        assertCollect(data, Collectors.reducing(0, x -> x*2, Integer::sum),
                      s -> s.map(x -> x*2).reduce(0, Integer::sum));

        assertCollect(data, Collectors.summingLong(x -> x * 2L),
                      s -> s.map(x -> x*2L).reduce(0L, Long::sum));
        assertCollect(data, Collectors.summingInt(x -> x * 2),
                      s -> s.map(x -> x*2).reduce(0, Integer::sum));
        assertCollect(data, Collectors.summingDouble(x -> x * 2.0d),
                      s -> s.map(x -> x * 2.0d).reduce(0.0d, Double::sum));

        assertCollect(data, Collectors.averagingInt(x -> x * 2),
                      s -> s.mapToInt(x -> x * 2).average().orElse(0));
        assertCollect(data, Collectors.averagingLong(x -> x * 2),
                      s -> s.mapToLong(x -> x * 2).average().orElse(0));
        assertCollect(data, Collectors.averagingDouble(x -> x * 2),
                      s -> s.mapToDouble(x -> x * 2).average().orElse(0));

        // Test explicit Collector.of
        Collector<Integer, long[], Double> avg2xint = Collector.of(() -> new long[2],
                                                                   (a, b) -> {
                                                                       a[0] += b * 2;
                                                                       a[1]++;
                                                                   },
                                                                   (a, b) -> {
                                                                       a[0] += b[0];
                                                                       a[1] += b[1];
                                                                       return a;
                                                                   },
                                                                   a -> a[1] == 0 ? 0.0d : (double) a[0] / a[1]);
        assertCollect(data, avg2xint,
                      s -> s.mapToInt(x -> x * 2).average().orElse(0));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testJoining(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        withData(data)
                .terminal(s -> s.map(Object::toString).collect(Collectors.joining()))
                .expectedResult(join(data, ""))
                .exercise();

        Collector<String, StringBuilder, String> likeJoining = Collector.of(StringBuilder::new, StringBuilder::append, (sb1, sb2) -> sb1.append(sb2.toString()), StringBuilder::toString);
        withData(data)
                .terminal(s -> s.map(Object::toString).collect(likeJoining))
                .expectedResult(join(data, ""))
                .exercise();

        withData(data)
                .terminal(s -> s.map(Object::toString).collect(Collectors.joining(",")))
                .expectedResult(join(data, ","))
                .exercise();

        withData(data)
                .terminal(s -> s.map(Object::toString).collect(Collectors.joining(",", "[", "]")))
                .expectedResult("[" + join(data, ",") + "]")
                .exercise();

        withData(data)
                .terminal(s -> s.map(Object::toString)
                                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                .toString())
                .expectedResult(join(data, ""))
                .exercise();

        withData(data)
                .terminal(s -> s.map(Object::toString)
                                .collect(() -> new StringJoiner(","),
                                         (sj, cs) -> sj.add(cs),
                                         (j1, j2) -> j1.merge(j2))
                                .toString())
                .expectedResult(join(data, ","))
                .exercise();

        withData(data)
                .terminal(s -> s.map(Object::toString)
                                .collect(() -> new StringJoiner(",", "[", "]"),
                                         (sj, cs) -> sj.add(cs),
                                         (j1, j2) -> j1.merge(j2))
                                .toString())
                .expectedResult("[" + join(data, ",") + "]")
                .exercise();
    }

    private<T> String join(TestData.OfRef<T> data, String delim) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T i : data) {
            if (!first)
                sb.append(delim);
            sb.append(i.toString());
            first = false;
        }
        return sb.toString();
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testSimpleToMap(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> keyFn = i -> i * 2;
        Function<Integer, Integer> valueFn = i -> i * 4;

        List<Integer> dataAsList = Arrays.asList(data.stream().toArray(Integer[]::new));
        Set<Integer> dataAsSet = new HashSet<>(dataAsList);

        BinaryOperator<Integer> sum = Integer::sum;
        for (BinaryOperator<Integer> op : Arrays.asList((u, v) -> u,
                                                        (u, v) -> v,
                                                        sum)) {
            try {
                exerciseMapCollection(data, toMap(keyFn, valueFn),
                                      new ToMapAssertion<>(keyFn, valueFn, op, HashMap.class));
                if (dataAsList.size() != dataAsSet.size())
                    fail("Expected ISE on input with duplicates");
            }
            catch (IllegalStateException e) {
                if (dataAsList.size() == dataAsSet.size())
                    fail("Expected no ISE on input without duplicates");
            }

            exerciseMapCollection(data, toMap(keyFn, valueFn, op),
                                  new ToMapAssertion<>(keyFn, valueFn, op, HashMap.class));

            exerciseMapCollection(data, toMap(keyFn, valueFn, op, TreeMap::new),
                                  new ToMapAssertion<>(keyFn, valueFn, op, TreeMap.class));
        }

        // For concurrent maps, only use commutative merge functions
        try {
            exerciseMapCollection(data, toConcurrentMap(keyFn, valueFn),
                                  new ToMapAssertion<>(keyFn, valueFn, sum, ConcurrentHashMap.class));
            if (dataAsList.size() != dataAsSet.size())
                fail("Expected ISE on input with duplicates");
        }
        catch (IllegalStateException e) {
            if (dataAsList.size() == dataAsSet.size())
                fail("Expected no ISE on input without duplicates");
        }

        exerciseMapCollection(data, toConcurrentMap(keyFn, valueFn, sum),
                              new ToMapAssertion<>(keyFn, valueFn, sum, ConcurrentHashMap.class));

        exerciseMapCollection(data, toConcurrentMap(keyFn, valueFn, sum, ConcurrentSkipListMap::new),
                              new ToMapAssertion<>(keyFn, valueFn, sum, ConcurrentSkipListMap.class));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testSimpleGroupingBy(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 3;

        // Single-level groupBy
        exerciseMapCollection(data, groupingBy(classifier),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new ToListAssertion<>()));
        exerciseMapCollection(data, groupingByConcurrent(classifier),
                              new GroupingByAssertion<>(classifier, ConcurrentHashMap.class,
                                                        new ToListAssertion<>()));

        // With explicit constructors
        exerciseMapCollection(data,
                              groupingBy(classifier, TreeMap::new, toCollection(HashSet::new)),
                              new GroupingByAssertion<>(classifier, TreeMap.class,
                                                        new ToCollectionAssertion<Integer>(HashSet.class, false)));
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, ConcurrentSkipListMap::new,
                                                   toCollection(HashSet::new)),
                              new GroupingByAssertion<>(classifier, ConcurrentSkipListMap.class,
                                                        new ToCollectionAssertion<Integer>(HashSet.class, false)));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testGroupingByWithMapping(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 3;
        Function<Integer, Integer> mapper = i -> i * 2;

        exerciseMapCollection(data,
                              groupingBy(classifier, mapping(mapper, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new MappingAssertion<>(mapper,
                                                                               new ToListAssertion<>())));
    }

    @Test(groups = { "serialization-hostile" })
    public void testFlatMappingClose() {
        Function<Integer, Integer> classifier = i -> i;
        AtomicInteger ai = new AtomicInteger();
        Function<Integer, Stream<Integer>> flatMapper = i -> Stream.of(i, i).onClose(ai::getAndIncrement);
        Map<Integer, List<Integer>> m = Stream.of(1, 2).collect(groupingBy(classifier, flatMapping(flatMapper, toList())));
        assertEquals(m.size(), ai.get());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testGroupingByWithFlatMapping(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 3;
        Function<Integer, Stream<Integer>> flatMapperByNull = i -> null;
        Function<Integer, Stream<Integer>> flatMapperBy0 = i -> Stream.empty();
        Function<Integer, Stream<Integer>> flatMapperBy2 = i -> Stream.of(i, i);

        exerciseMapCollection(data,
                              groupingBy(classifier, flatMapping(flatMapperByNull, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FlatMappingAssertion<>(flatMapperBy0,
                                                                                   new ToListAssertion<>())));
        exerciseMapCollection(data,
                              groupingBy(classifier, flatMapping(flatMapperBy0, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FlatMappingAssertion<>(flatMapperBy0,
                                                                                   new ToListAssertion<>())));
        exerciseMapCollection(data,
                              groupingBy(classifier, flatMapping(flatMapperBy2, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FlatMappingAssertion<>(flatMapperBy2,
                                                                                   new ToListAssertion<>())));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testGroupingByWithFiltering(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 3;
        Predicate<Integer> filteringByMod2 = i -> i % 2 == 0;
        Predicate<Integer> filteringByUnder100 = i -> i % 2 < 100;
        Predicate<Integer> filteringByTrue = i -> true;
        Predicate<Integer> filteringByFalse = i -> false;

        exerciseMapCollection(data,
                              groupingBy(classifier, filtering(filteringByMod2, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FilteringAssertion<>(filteringByMod2,
                                                                                   new ToListAssertion<>())));
        exerciseMapCollection(data,
                              groupingBy(classifier, filtering(filteringByUnder100, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FilteringAssertion<>(filteringByUnder100,
                                                                                   new ToListAssertion<>())));
        exerciseMapCollection(data,
                              groupingBy(classifier, filtering(filteringByTrue, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FilteringAssertion<>(filteringByTrue,
                                                                                   new ToListAssertion<>())));
        exerciseMapCollection(data,
                              groupingBy(classifier, filtering(filteringByFalse, toList())),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new FilteringAssertion<>(filteringByFalse,
                                                                                   new ToListAssertion<>())));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTwoLevelGroupingBy(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 6;
        Function<Integer, Integer> classifier2 = i -> i % 23;

        // Two-level groupBy
        exerciseMapCollection(data,
                              groupingBy(classifier, groupingBy(classifier2)),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new GroupingByAssertion<>(classifier2, HashMap.class,
                                                                                  new ToListAssertion<>())));
        // with concurrent as upstream
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, groupingBy(classifier2)),
                              new GroupingByAssertion<>(classifier, ConcurrentHashMap.class,
                                                        new GroupingByAssertion<>(classifier2, HashMap.class,
                                                                                  new ToListAssertion<>())));
        // with concurrent as downstream
        exerciseMapCollection(data,
                              groupingBy(classifier, groupingByConcurrent(classifier2)),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new GroupingByAssertion<>(classifier2, ConcurrentHashMap.class,
                                                                                  new ToListAssertion<>())));
        // with concurrent as upstream and downstream
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, groupingByConcurrent(classifier2)),
                              new GroupingByAssertion<>(classifier, ConcurrentHashMap.class,
                                                        new GroupingByAssertion<>(classifier2, ConcurrentHashMap.class,
                                                                                  new ToListAssertion<>())));

        // With explicit constructors
        exerciseMapCollection(data,
                              groupingBy(classifier, TreeMap::new, groupingBy(classifier2, TreeMap::new, toCollection(HashSet::new))),
                              new GroupingByAssertion<>(classifier, TreeMap.class,
                                                        new GroupingByAssertion<>(classifier2, TreeMap.class,
                                                                                  new ToCollectionAssertion<Integer>(HashSet.class, false))));
        // with concurrent as upstream
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, ConcurrentSkipListMap::new, groupingBy(classifier2, TreeMap::new, toList())),
                              new GroupingByAssertion<>(classifier, ConcurrentSkipListMap.class,
                                                        new GroupingByAssertion<>(classifier2, TreeMap.class,
                                                                                  new ToListAssertion<>())));
        // with concurrent as downstream
        exerciseMapCollection(data,
                              groupingBy(classifier, TreeMap::new, groupingByConcurrent(classifier2, ConcurrentSkipListMap::new, toList())),
                              new GroupingByAssertion<>(classifier, TreeMap.class,
                                                        new GroupingByAssertion<>(classifier2, ConcurrentSkipListMap.class,
                                                                                  new ToListAssertion<>())));
        // with concurrent as upstream and downstream
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, ConcurrentSkipListMap::new, groupingByConcurrent(classifier2, ConcurrentSkipListMap::new, toList())),
                              new GroupingByAssertion<>(classifier, ConcurrentSkipListMap.class,
                                                        new GroupingByAssertion<>(classifier2, ConcurrentSkipListMap.class,
                                                                                  new ToListAssertion<>())));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testGroupubgByWithReducing(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Function<Integer, Integer> classifier = i -> i % 3;

        // Single-level simple reduce
        exerciseMapCollection(data,
                              groupingBy(classifier, reducing(0, Integer::sum)),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new ReducingAssertion<>(0, LambdaTestHelpers.identity(), Integer::sum)));
        // with concurrent
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, reducing(0, Integer::sum)),
                              new GroupingByAssertion<>(classifier, ConcurrentHashMap.class,
                                                        new ReducingAssertion<>(0, LambdaTestHelpers.identity(), Integer::sum)));

        // With explicit constructors
        exerciseMapCollection(data,
                              groupingBy(classifier, TreeMap::new, reducing(0, Integer::sum)),
                              new GroupingByAssertion<>(classifier, TreeMap.class,
                                                        new ReducingAssertion<>(0, LambdaTestHelpers.identity(), Integer::sum)));
        // with concurrent
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, ConcurrentSkipListMap::new, reducing(0, Integer::sum)),
                              new GroupingByAssertion<>(classifier, ConcurrentSkipListMap.class,
                                                        new ReducingAssertion<>(0, LambdaTestHelpers.identity(), Integer::sum)));

        // Single-level map-reduce
        exerciseMapCollection(data,
                              groupingBy(classifier, reducing(0, mDoubler, Integer::sum)),
                              new GroupingByAssertion<>(classifier, HashMap.class,
                                                        new ReducingAssertion<>(0, mDoubler, Integer::sum)));
        // with concurrent
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, reducing(0, mDoubler, Integer::sum)),
                              new GroupingByAssertion<>(classifier, ConcurrentHashMap.class,
                                                        new ReducingAssertion<>(0, mDoubler, Integer::sum)));

        // With explicit constructors
        exerciseMapCollection(data,
                              groupingBy(classifier, TreeMap::new, reducing(0, mDoubler, Integer::sum)),
                              new GroupingByAssertion<>(classifier, TreeMap.class,
                                                        new ReducingAssertion<>(0, mDoubler, Integer::sum)));
        // with concurrent
        exerciseMapCollection(data,
                              groupingByConcurrent(classifier, ConcurrentSkipListMap::new, reducing(0, mDoubler, Integer::sum)),
                              new GroupingByAssertion<>(classifier, ConcurrentSkipListMap.class,
                                                        new ReducingAssertion<>(0, mDoubler, Integer::sum)));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testSimplePartitioningBy(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Predicate<Integer> classifier = i -> i % 3 == 0;

        // Single-level partition to downstream List
        exerciseMapCollection(data,
                              partitioningBy(classifier),
                              new PartitioningByAssertion<>(classifier, new ToListAssertion<>()));
        exerciseMapCollection(data,
                              partitioningBy(classifier, toList()),
                              new PartitioningByAssertion<>(classifier, new ToListAssertion<>()));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTwoLevelPartitioningBy(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Predicate<Integer> classifier = i -> i % 3 == 0;
        Predicate<Integer> classifier2 = i -> i % 7 == 0;

        // Two level partition
        exerciseMapCollection(data,
                              partitioningBy(classifier, partitioningBy(classifier2)),
                              new PartitioningByAssertion<>(classifier,
                                                            new PartitioningByAssertion(classifier2, new ToListAssertion<>())));

        // Two level partition with reduce
        exerciseMapCollection(data,
                              partitioningBy(classifier, reducing(0, Integer::sum)),
                              new PartitioningByAssertion<>(classifier,
                                                            new ReducingAssertion<>(0, LambdaTestHelpers.identity(), Integer::sum)));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testComposeFinisher(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        List<Integer> asList = exerciseTerminalOps(data, s -> s.collect(toList()));
        List<Integer> asImmutableList = exerciseTerminalOps(data, s -> s.collect(collectingAndThen(toList(), Collections::unmodifiableList)));
        assertEquals(asList, asImmutableList);
        try {
            asImmutableList.add(0);
            fail("Expecting immutable result");
        }
        catch (UnsupportedOperationException ignored) { }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testTeeing(String name, TestData.OfRef<Integer> data) throws ReflectiveOperationException {
        Collector<Integer, ?, Long> summing = Collectors.summingLong(Integer::valueOf);
        Collector<Integer, ?, Long> counting = Collectors.counting();
        Collector<Integer, ?, Integer> min = collectingAndThen(Collectors.<Integer>minBy(Comparator.naturalOrder()),
                opt -> opt.orElse(Integer.MAX_VALUE));
        Collector<Integer, ?, Integer> max = collectingAndThen(Collectors.<Integer>maxBy(Comparator.naturalOrder()),
                opt -> opt.orElse(Integer.MIN_VALUE));
        Collector<Integer, ?, String> joining = mapping(String::valueOf, Collectors.joining(", ", "[", "]"));

        Collector<Integer, ?, Map.Entry<Long, Long>> sumAndCount = Collectors.teeing(summing, counting, Map::entry);
        Collector<Integer, ?, Map.Entry<Integer, Integer>> minAndMax = Collectors.teeing(min, max, Map::entry);
        Collector<Integer, ?, Double> averaging = Collectors.teeing(summing, counting,
                (sum, count) -> ((double)sum) / count);
        Collector<Integer, ?, String> summaryStatistics = Collectors.teeing(sumAndCount, minAndMax,
                (sumCountEntry, minMaxEntry) -> new IntSummaryStatistics(
                        sumCountEntry.getValue(), minMaxEntry.getKey(),
                        minMaxEntry.getValue(), sumCountEntry.getKey()).toString());
        Collector<Integer, ?, String> countAndContent = Collectors.teeing(counting, joining,
                (count, content) -> count+": "+content);

        assertCollect(data, sumAndCount, stream -> {
            List<Integer> list = stream.collect(toList());
            return Map.entry(list.stream().mapToLong(Integer::intValue).sum(), (long) list.size());
        });
        assertCollect(data, averaging, stream -> stream.mapToInt(Integer::intValue).average().orElse(Double.NaN));
        assertCollect(data, summaryStatistics,
                stream -> stream.mapToInt(Integer::intValue).summaryStatistics().toString());
        assertCollect(data, countAndContent, stream -> {
            List<Integer> list = stream.collect(toList());
            return list.size()+": "+list;
        });

        Function<Integer, Integer> classifier = i -> i % 3;
        exerciseMapCollection(data, groupingBy(classifier, sumAndCount),
                new GroupingByAssertion<>(classifier, Map.class,
                        new TeeingAssertion<>(summing, counting, Map::entry)));
    }
}
