/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8005698
 * @library /lib/testlibrary/bootlib
 * @build java.base/java.util.SpliteratorTestHelper
 * @run testng SpliteratorCollisions
 * @summary Spliterator traversing and splitting hash maps containing colliding hashes
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.SpliteratorTestHelper;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class SpliteratorCollisions extends SpliteratorTestHelper {

    private static final List<Integer> SIZES = Arrays.asList(0, 1, 10, 100, 1000);

    private static class SpliteratorDataBuilder<T> {
        List<Object[]> data;
        List<T> exp;
        Map<T, T> mExp;

        SpliteratorDataBuilder(List<Object[]> data, List<T> exp) {
            this.data = data;
            this.exp = exp;
            this.mExp = createMap(exp);
        }

        Map<T, T> createMap(List<T> l) {
            Map<T, T> m = new LinkedHashMap<>();
            for (T t : l) {
                m.put(t, t);
            }
            return m;
        }

        void add(String description, Collection<?> expected, Supplier<Spliterator<?>> s) {
            description = joiner(description).toString();
            data.add(new Object[]{description, expected, s});
        }

        void add(String description, Supplier<Spliterator<?>> s) {
            add(description, exp, s);
        }

        void addCollection(Function<Collection<T>, ? extends Collection<T>> c) {
            add("new " + c.apply(Collections.<T>emptyList()).getClass().getName() + ".spliterator()",
                () -> c.apply(exp).spliterator());
        }

        void addList(Function<Collection<T>, ? extends List<T>> l) {
            // @@@ If collection is instance of List then add sub-list tests
            addCollection(l);
        }

        void addMap(Function<Map<T, T>, ? extends Map<T, T>> m) {
            String description = "new " + m.apply(Collections.<T, T>emptyMap()).getClass().getName();
            add(description + ".keySet().spliterator()", () -> m.apply(mExp).keySet().spliterator());
            add(description + ".values().spliterator()", () -> m.apply(mExp).values().spliterator());
            add(description + ".entrySet().spliterator()", mExp.entrySet(), () -> m.apply(mExp).entrySet().spliterator());
        }

        StringBuilder joiner(String description) {
            return new StringBuilder(description).
                    append(" {").
                    append("size=").append(exp.size()).
                    append("}");
        }
    }

    static Object[][] spliteratorDataProvider;

    @DataProvider(name = "HashableIntSpliterator")
    public static Object[][] spliteratorDataProvider() {
        if (spliteratorDataProvider != null) {
            return spliteratorDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            List<HashableInteger> exp = listIntRange(size, false);
            SpliteratorDataBuilder<HashableInteger> db = new SpliteratorDataBuilder<>(data, exp);

            // Maps
            db.addMap(HashMap::new);
            db.addMap(LinkedHashMap::new);

            // Collections that use HashMap
            db.addCollection(HashSet::new);
            db.addCollection(LinkedHashSet::new);
            db.addCollection(TreeSet::new);
        }
        return spliteratorDataProvider = data.toArray(new Object[0][]);
    }

    static Object[][] spliteratorDataProviderWithNull;

    @DataProvider(name = "HashableIntSpliteratorWithNull")
    public static Object[][] spliteratorNullDataProvider() {
        if (spliteratorDataProviderWithNull != null) {
            return spliteratorDataProviderWithNull;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            List<HashableInteger> exp = listIntRange(size, true);
            SpliteratorDataBuilder<HashableInteger> db = new SpliteratorDataBuilder<>(data, exp);

            // Maps
            db.addMap(HashMap::new);
            db.addMap(LinkedHashMap::new);
            // TODO: add this back in if we decide to keep TreeBin in WeakHashMap
            //db.addMap(WeakHashMap::new);

            // Collections that use HashMap
            db.addCollection(HashSet::new);
            db.addCollection(LinkedHashSet::new);
//            db.addCollection(TreeSet::new);

        }
        return spliteratorDataProviderWithNull = data.toArray(new Object[0][]);
    }

    static final class HashableInteger implements Comparable<HashableInteger> {

        final int value;
        final int hashmask; //yes duplication

        HashableInteger(int value, int hashmask) {
            this.value = value;
            this.hashmask = hashmask;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HashableInteger) {
                HashableInteger other = (HashableInteger) obj;

                return other.value == value;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return value % hashmask;
        }

        @Override
        public int compareTo(HashableInteger o) {
            return value - o.value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    private static List<HashableInteger> listIntRange(int upTo, boolean withNull) {
        List<HashableInteger> exp = new ArrayList<>();
        if (withNull) {
            exp.add(null);
        }
        for (int i = 0; i < upTo; i++) {
            exp.add(new HashableInteger(i, 10));
        }
        return Collections.unmodifiableList(exp);
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testNullPointerException(String description,
                                  Collection<HashableInteger> exp,
                                  Supplier<Spliterator<HashableInteger>> s) {
        assertThrowsNPE(() -> s.get().forEachRemaining(null));
        assertThrowsNPE(() -> s.get().tryAdvance(null));
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testNullPointerExceptionWithNull(String description,
                                          Collection<HashableInteger> exp,
                                          Supplier<Spliterator<HashableInteger>> s) {
        assertThrowsNPE(() -> s.get().forEachRemaining(null));
        assertThrowsNPE(() -> s.get().tryAdvance(null));
    }


    @Test(dataProvider = "HashableIntSpliterator")
    void testForEach(String description,
                     Collection<HashableInteger> exp,
                     Supplier<Spliterator<HashableInteger>> s) {
        testForEach(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testForEachWithNull(String description,
                             Collection<HashableInteger> exp,
                             Supplier<Spliterator<HashableInteger>> s) {
        testForEach(exp, s, UnaryOperator.identity());
    }


    @Test(dataProvider = "HashableIntSpliterator")
    void testTryAdvance(String description,
                        Collection<HashableInteger> exp,
                        Supplier<Spliterator<HashableInteger>> s) {
        testTryAdvance(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testTryAdvanceWithNull(String description,
                                Collection<HashableInteger> exp,
                                Supplier<Spliterator<HashableInteger>> s) {
        testTryAdvance(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testMixedTryAdvanceForEach(String description,
                                    Collection<HashableInteger> exp,
                                    Supplier<Spliterator<HashableInteger>> s) {
        testMixedTryAdvanceForEach(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testMixedTryAdvanceForEachWithNull(String description,
                                            Collection<HashableInteger> exp,
                                            Supplier<Spliterator<HashableInteger>> s) {
        testMixedTryAdvanceForEach(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testMixedTraverseAndSplit(String description,
                                   Collection<HashableInteger> exp,
                                   Supplier<Spliterator<HashableInteger>> s) {
        testMixedTraverseAndSplit(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testMixedTraverseAndSplitWithNull(String description,
                                           Collection<HashableInteger> exp,
                                           Supplier<Spliterator<HashableInteger>> s) {
        testMixedTraverseAndSplit(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testSplitAfterFullTraversal(String description,
                                     Collection<HashableInteger> exp,
                                     Supplier<Spliterator<HashableInteger>> s) {
        testSplitAfterFullTraversal(s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testSplitAfterFullTraversalWithNull(String description,
                                             Collection<HashableInteger> exp,
                                             Supplier<Spliterator<HashableInteger>> s) {
        testSplitAfterFullTraversal(s, UnaryOperator.identity());
    }


    @Test(dataProvider = "HashableIntSpliterator")
    void testSplitOnce(String description,
                       Collection<HashableInteger> exp,
                       Supplier<Spliterator<HashableInteger>> s) {
        testSplitOnce(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testSplitOnceWithNull(String description,
                               Collection<HashableInteger> exp,
                               Supplier<Spliterator<HashableInteger>> s) {
        testSplitOnce(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testSplitSixDeep(String description,
                          Collection<HashableInteger> exp,
                          Supplier<Spliterator<HashableInteger>> s) {
        testSplitSixDeep(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testSplitSixDeepWithNull(String description,
                                  Collection<HashableInteger> exp,
                                  Supplier<Spliterator<HashableInteger>> s) {
        testSplitSixDeep(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliterator")
    void testSplitUntilNull(String description,
                            Collection<HashableInteger> exp,
                            Supplier<Spliterator<HashableInteger>> s) {
        testSplitUntilNull(exp, s, UnaryOperator.identity());
    }

    @Test(dataProvider = "HashableIntSpliteratorWithNull")
    void testSplitUntilNullWithNull(String description,
                                    Collection<HashableInteger> exp,
                                    Supplier<Spliterator<HashableInteger>> s) {
        testSplitUntilNull(exp, s, UnaryOperator.identity());
    }

}
