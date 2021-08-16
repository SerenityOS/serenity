/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.Exception;
import java.lang.Integer;
import java.lang.Iterable;
import java.lang.Override;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.testng.TestException;

import static org.testng.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @library
 * @summary A Supplier of test cases for Collection tests
 */
public final class CollectionSupplier<C extends Collection<Integer>> implements Supplier<Iterable<CollectionSupplier.TestCase<C>>> {

    private final List<Function<Collection<Integer>, C>> suppliers;
    private final int size;

    /**
     * A Collection test case.
     */
    public static final class TestCase<C extends Collection<Integer>> {
        /**
         * The name of the test case.
         */
        public final String name;

        /**
         * The supplier of a collection
         */
        public Function<Collection<Integer>, C> supplier;

        /**
         * Unmodifiable reference collection, useful for comparisons.
         */
        public final List<Integer> expected;

        /**
         * A modifiable test collection.
         */
        public final C collection;

        /**
         * Create a Collection test case.
         *
         * @param name name of the test case
         * @param collection the modifiable test collection
         */
        public TestCase(String name, Function<Collection<Integer>, C> supplier, C collection) {
            this.name = name;
            this.supplier = supplier;
            this.expected = Collections.unmodifiableList(
                Arrays.asList(collection.toArray(new Integer[0])));
            this.collection = collection;
        }

        @Override
        public String toString() {
            return name + " " + collection.getClass().toString();
        }
    }

    /**
     * Shuffle a list using a PRNG with known seed for repeatability
     *
     * @param list the list to be shuffled
     */
    public static <E> void shuffle(final List<E> list) {
        // PRNG with known seed for repeatable tests
        final Random prng = new Random(13);
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            // random index in interval [i, size)
            final int j = i + prng.nextInt(size - i);
            // swap elements at indices i & j
            final E e = list.get(i);
            list.set(i, list.get(j));
            list.set(j, e);
        }
    }

    /**
     * Create a {@code CollectionSupplier} that creates instances of specified
     * collection suppliers of the specified size.
     *
     * @param suppliers the suppliers names that supply {@code Collection}
     *        instances
     * @param size the desired size of each collection
     */
    public CollectionSupplier(List<Function<Collection<Integer>, C>> suppliers, int size) {
        this.suppliers = suppliers;
        this.size = size;
    }

    @Override
    public Iterable<TestCase<C>> get() {
        final Collection<TestCase<C>> cases = new LinkedList<>();
        for (final Function<Collection<Integer>, C> supplier : suppliers)
            try {
                cases.add(new TestCase<>("empty", supplier, supplier.apply(Collections.emptyList())));

                cases.add(new TestCase<>("single", supplier, supplier.apply(Arrays.asList(42))));

                final Collection<Integer> regular = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    regular.add(i);
                }
                cases.add(new TestCase<>("regular", supplier, supplier.apply(regular)));

                final Collection<Integer> reverse = new ArrayList<>();
                for (int i = size; i >= 0; i--) {
                    reverse.add(i);
                }
                cases.add(new TestCase<>("reverse", supplier, supplier.apply(reverse)));

                final Collection<Integer> odds = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    odds.add((i * 2) + 1);
                }
                cases.add(new TestCase<>("odds", supplier, supplier.apply(odds)));

                final Collection<Integer> evens = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    evens.add(i * 2);
                }
                cases.add(new TestCase<>("evens", supplier, supplier.apply(evens)));

                final Collection<Integer> fibonacci = new ArrayList<>();
                int prev2 = 0;
                int prev1 = 1;
                for (int i = 0; i < size; i++) {
                    final int n = prev1 + prev2;
                    if (n < 0) { // stop on overflow
                        break;
                    }
                    fibonacci.add(n);
                    prev2 = prev1;
                    prev1 = n;
                }
                cases.add(new TestCase<>("fibonacci", supplier, supplier.apply(fibonacci)));


                boolean isStructurallyModifiable = false;
                try {
                    C t = supplier.apply(Collections.emptyList());
                    t.add(1);
                    isStructurallyModifiable = true;
                } catch (UnsupportedOperationException e) { }

                if (!isStructurallyModifiable)
                    continue;


                // variants where the size of the backing storage != reported size
                // created by removing half of the elements
                final C emptyWithSlack = supplier.apply(Collections.emptyList());
                emptyWithSlack.add(42);
                assertTrue(emptyWithSlack.remove(42));
                cases.add(new TestCase<>("emptyWithSlack", supplier, emptyWithSlack));

                final C singleWithSlack = supplier.apply(Collections.emptyList());
                singleWithSlack.add(42);
                singleWithSlack.add(43);
                assertTrue(singleWithSlack.remove(43));
                cases.add(new TestCase<>("singleWithSlack", supplier, singleWithSlack));

                final C regularWithSlack = supplier.apply(Collections.emptyList());
                for (int i = 0; i < (2 * size); i++) {
                    regularWithSlack.add(i);
                }
                assertTrue(regularWithSlack.removeIf(x -> x < size));
                cases.add(new TestCase<>("regularWithSlack", supplier, regularWithSlack));

                final C reverseWithSlack = supplier.apply(Collections.emptyList());
                for (int i = 2 * size; i >= 0; i--) {
                    reverseWithSlack.add(i);
                }
                assertTrue(reverseWithSlack.removeIf(x -> x < size));
                cases.add(new TestCase<>("reverseWithSlack", supplier, reverseWithSlack));

                final C oddsWithSlack = supplier.apply(Collections.emptyList());
                for (int i = 0; i < 2 * size; i++) {
                    oddsWithSlack.add((i * 2) + 1);
                }
                assertTrue(oddsWithSlack.removeIf(x -> x >= size));
                cases.add(new TestCase<>("oddsWithSlack", supplier, oddsWithSlack));

                final C evensWithSlack = supplier.apply(Collections.emptyList());
                for (int i = 0; i < 2 * size; i++) {
                    evensWithSlack.add(i * 2);
                }
                assertTrue(evensWithSlack.removeIf(x -> x >= size));
                cases.add(new TestCase<>("evensWithSlack", supplier, evensWithSlack));

                final C fibonacciWithSlack = supplier.apply(Collections.emptyList());
                prev2 = 0;
                prev1 = 1;
                for (int i = 0; i < size; i++) {
                    final int n = prev1 + prev2;
                    if (n < 0) { // stop on overflow
                        break;
                    }
                    fibonacciWithSlack.add(n);
                    prev2 = prev1;
                    prev1 = n;
                }
                assertTrue(fibonacciWithSlack.removeIf(x -> x < 20));
                cases.add(new TestCase<>("fibonacciWithSlack", supplier, fibonacciWithSlack));
            }
            catch (Exception failed) {
                throw new TestException(failed);
            }

        return cases;
    }

}
