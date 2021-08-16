/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util;

import java.util.function.*;
import java.util.stream.LambdaTestHelpers;

import static org.testng.Assert.*;

/**
 * Assertion methods for spliterators, to be called from other tests
 */
public class SpliteratorTestHelper {

    public interface ContentAsserter<T> {
        void assertContents(Collection<T> actual, Collection<T> expected, boolean isOrdered);
    }

    private static ContentAsserter<Object> DEFAULT_CONTENT_ASSERTER
            = SpliteratorTestHelper::assertContents;

    @SuppressWarnings("unchecked")
    private static <T> ContentAsserter<T> defaultContentAsserter() {
        return (ContentAsserter<T>) DEFAULT_CONTENT_ASSERTER;
    }

    public static void testSpliterator(Supplier<Spliterator<Integer>> supplier) {
        testSpliterator(supplier, defaultContentAsserter());
    }

    public static void testSpliterator(Supplier<Spliterator<Integer>> supplier,
                                       ContentAsserter<Integer> asserter) {
        testSpliterator(supplier, (Consumer<Integer> b) -> b, asserter);
    }

    public static void testIntSpliterator(Supplier<Spliterator.OfInt> supplier) {
        testIntSpliterator(supplier, defaultContentAsserter());
    }

    public static void testIntSpliterator(Supplier<Spliterator.OfInt> supplier,
                                          ContentAsserter<Integer> asserter) {
        testSpliterator(supplier, intBoxingConsumer(), asserter);
    }

    public static void testLongSpliterator(Supplier<Spliterator.OfLong> supplier) {
        testLongSpliterator(supplier, defaultContentAsserter());
    }

    public static void testLongSpliterator(Supplier<Spliterator.OfLong> supplier,
                                           ContentAsserter<Long> asserter) {
        testSpliterator(supplier, longBoxingConsumer(), asserter);
    }

    public static void testDoubleSpliterator(Supplier<Spliterator.OfDouble> supplier) {
        testDoubleSpliterator(supplier, defaultContentAsserter());
    }

    public static void testDoubleSpliterator(Supplier<Spliterator.OfDouble> supplier,
                                             ContentAsserter<Double> asserter) {
        testSpliterator(supplier, doubleBoxingConsumer(), asserter);
    }

    public static UnaryOperator<Consumer<Integer>> intBoxingConsumer() {
        class BoxingAdapter implements Consumer<Integer>, IntConsumer {
            private final Consumer<Integer> b;

            BoxingAdapter(Consumer<Integer> b) {
                this.b = b;
            }

            @Override
            public void accept(Integer value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(int value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    public static UnaryOperator<Consumer<Long>> longBoxingConsumer() {
        class BoxingAdapter implements Consumer<Long>, LongConsumer {
            private final Consumer<Long> b;

            BoxingAdapter(Consumer<Long> b) {
                this.b = b;
            }

            @Override
            public void accept(Long value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(long value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    public static UnaryOperator<Consumer<Double>> doubleBoxingConsumer() {
        class BoxingAdapter implements Consumer<Double>, DoubleConsumer {
            private final Consumer<Double> b;

            BoxingAdapter(Consumer<Double> b) {
                this.b = b;
            }

            @Override
            public void accept(Double value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(double value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    public static <T, S extends Spliterator<T>> void testSpliterator(Supplier<S> supplier,
                                                              UnaryOperator<Consumer<T>> boxingAdapter,
                                                              ContentAsserter<T> asserter) {
        ArrayList<T> fromForEach = new ArrayList<>();
        Spliterator<T> spliterator = supplier.get();
        Consumer<T> addToFromForEach = boxingAdapter.apply(fromForEach::add);
        spliterator.forEachRemaining(addToFromForEach);

        Collection<T> exp = Collections.unmodifiableList(fromForEach);

        testNullPointerException(supplier);
        testForEach(exp, supplier, boxingAdapter, asserter);
        testTryAdvance(exp, supplier, boxingAdapter, asserter);
        testMixedTryAdvanceForEach(exp, supplier, boxingAdapter, asserter);
        testMixedTraverseAndSplit(exp, supplier, boxingAdapter, asserter);
        testSplitAfterFullTraversal(supplier, boxingAdapter);
        testSplitOnce(exp, supplier, boxingAdapter, asserter);
        testSplitSixDeep(exp, supplier, boxingAdapter, asserter);
        testSplitUntilNull(exp, supplier, boxingAdapter, asserter);
    }

    public static <T, S extends Spliterator<T>> void testForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testForEach(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testTryAdvance(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testTryAdvance(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testMixedTryAdvanceForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testMixedTryAdvanceForEach(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testMixedTraverseAndSplit(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testMixedTraverseAndSplit(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testSplitOnce(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testSplitOnce(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testSplitSixDeep(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testSplitSixDeep(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    public static <T, S extends Spliterator<T>> void testSplitUntilNull(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        testSplitUntilNull(exp, supplier, boxingAdapter, defaultContentAsserter());
    }

    private static <T, S extends Spliterator<T>> void testNullPointerException(Supplier<S> s) {
        S sp = s.get();
        // Have to check instances and use casts to avoid tripwire messages and
        // directly test the primitive methods
        if (sp instanceof Spliterator.OfInt) {
            Spliterator.OfInt psp = (Spliterator.OfInt) sp;
            assertThrowsNPE(() -> psp.forEachRemaining((IntConsumer) null));
            assertThrowsNPE(() -> psp.tryAdvance((IntConsumer) null));
        }
        else if (sp instanceof Spliterator.OfLong) {
            Spliterator.OfLong psp = (Spliterator.OfLong) sp;
            assertThrowsNPE(() -> psp.forEachRemaining((LongConsumer) null));
            assertThrowsNPE(() -> psp.tryAdvance((LongConsumer) null));
        }
        else if (sp instanceof Spliterator.OfDouble) {
            Spliterator.OfDouble psp = (Spliterator.OfDouble) sp;
            assertThrowsNPE(() -> psp.forEachRemaining((DoubleConsumer) null));
            assertThrowsNPE(() -> psp.tryAdvance((DoubleConsumer) null));
        }
        else {
            assertThrowsNPE(() -> sp.forEachRemaining(null));
            assertThrowsNPE(() -> sp.tryAdvance(null));
        }
    }

    private static <T, S extends Spliterator<T>> void testForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        ArrayList<T> fromForEach = new ArrayList<>();
        spliterator = supplier.get();
        Consumer<T> addToFromForEach = boxingAdapter.apply(fromForEach::add);
        spliterator.forEachRemaining(addToFromForEach);

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(
                e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(
                e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        // assert that size, tryAdvance, and forEach are consistent
        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, exp.size());
        }
        if (exp.contains(null)) {
            assertTrue(fromForEach.contains(null));
        }
        assertEquals(fromForEach.size(), exp.size());

        asserter.assertContents(fromForEach, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testTryAdvance(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        spliterator = supplier.get();
        ArrayList<T> fromTryAdvance = new ArrayList<>();
        Consumer<T> addToFromTryAdvance = boxingAdapter.apply(fromTryAdvance::add);
        while (spliterator.tryAdvance(addToFromTryAdvance)) { }

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(
                e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(
                e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        // assert that size, tryAdvance, and forEach are consistent
        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, exp.size());
        }
        assertEquals(fromTryAdvance.size(), exp.size());

        asserter.assertContents(fromTryAdvance, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testMixedTryAdvanceForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        // tryAdvance first few elements, then forEach rest
        ArrayList<T> dest = new ArrayList<>();
        spliterator = supplier.get();
        Consumer<T> addToDest = boxingAdapter.apply(dest::add);
        for (int i = 0; i < 10 && spliterator.tryAdvance(addToDest); i++) { }
        spliterator.forEachRemaining(addToDest);

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(
                e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(
                e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, dest.size());
        }
        assertEquals(dest.size(), exp.size());

        asserter.assertContents(dest, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testMixedTraverseAndSplit(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        // tryAdvance first few elements, then forEach rest
        ArrayList<T> dest = new ArrayList<>();
        spliterator = supplier.get();
        Consumer<T> b = boxingAdapter.apply(dest::add);

        Spliterator<T> spl1, spl2, spl3;
        spliterator.tryAdvance(b);
        spl2 = spliterator.trySplit();
        if (spl2 != null) {
            spl2.tryAdvance(b);
            spl1 = spl2.trySplit();
            if (spl1 != null) {
                spl1.tryAdvance(b);
                spl1.forEachRemaining(b);
            }
            spl2.tryAdvance(b);
            spl2.forEachRemaining(b);
        }
        spliterator.tryAdvance(b);
        spl3 = spliterator.trySplit();
        if (spl3 != null) {
            spl3.tryAdvance(b);
            spl3.forEachRemaining(b);
        }
        spliterator.tryAdvance(b);
        spliterator.forEachRemaining(b);

        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, dest.size());
        }
        assertEquals(dest.size(), exp.size());

        asserter.assertContents(dest, exp, isOrdered);
    }

    public static <T, S extends Spliterator<T>> void testSplitAfterFullTraversal(
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        // Full traversal using tryAdvance
        Spliterator<T> spliterator = supplier.get();
        while (spliterator.tryAdvance(boxingAdapter.apply(e -> { }))) { }
        Spliterator<T> split = spliterator.trySplit();
        assertNull(split);

        // Full traversal using forEach
        spliterator = supplier.get();
        spliterator.forEachRemaining(boxingAdapter.apply(e -> { }));
        split = spliterator.trySplit();
        assertNull(split);

        // Full traversal using tryAdvance then forEach
        spliterator = supplier.get();
        spliterator.tryAdvance(boxingAdapter.apply(e -> { }));
        spliterator.forEachRemaining(boxingAdapter.apply(e -> { }));
        split = spliterator.trySplit();
        assertNull(split);
    }

    private static <T, S extends Spliterator<T>> void testSplitOnce(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        ArrayList<T> fromSplit = new ArrayList<>();
        Spliterator<T> s1 = supplier.get();
        Spliterator<T> s2 = s1.trySplit();
        long s1Size = s1.getExactSizeIfKnown();
        long s2Size = (s2 != null) ? s2.getExactSizeIfKnown() : 0;
        Consumer<T> addToFromSplit = boxingAdapter.apply(fromSplit::add);
        if (s2 != null)
            s2.forEachRemaining(addToFromSplit);
        s1.forEachRemaining(addToFromSplit);

        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, fromSplit.size());
            if (s1Size >= 0 && s2Size >= 0)
                assertEquals(sizeIfKnown, s1Size + s2Size);
        }

        asserter.assertContents(fromSplit, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testSplitSixDeep(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        S spliterator = supplier.get();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        for (int depth=0; depth < 6; depth++) {
            List<T> dest = new ArrayList<>();
            spliterator = supplier.get();

            assertSpliterator(spliterator);

            // verify splitting with forEach
            splitSixDeepVisitor(depth, 0, dest, spliterator, boxingAdapter, spliterator.characteristics(), false);
            asserter.assertContents(dest, exp, isOrdered);

            // verify splitting with tryAdvance
            dest.clear();
            spliterator = supplier.get();
            splitSixDeepVisitor(depth, 0, dest, spliterator, boxingAdapter, spliterator.characteristics(), true);
            asserter.assertContents(dest, exp, isOrdered);
        }
    }

    private static <T, S extends Spliterator<T>>
    void splitSixDeepVisitor(int depth, int curLevel,
                             List<T> dest, S spliterator, UnaryOperator<Consumer<T>> boxingAdapter,
                             int rootCharacteristics, boolean useTryAdvance) {
        if (curLevel < depth) {
            long beforeSize = spliterator.getExactSizeIfKnown();
            Spliterator<T> split = spliterator.trySplit();
            if (split != null) {
                assertSpliterator(split, rootCharacteristics);
                assertSpliterator(spliterator, rootCharacteristics);

                if ((rootCharacteristics & Spliterator.SUBSIZED) != 0 &&
                    (rootCharacteristics & Spliterator.SIZED) != 0) {
                    assertEquals(beforeSize, split.estimateSize() + spliterator.estimateSize());
                }
                splitSixDeepVisitor(depth, curLevel + 1, dest, split, boxingAdapter, rootCharacteristics, useTryAdvance);
            }
            splitSixDeepVisitor(depth, curLevel + 1, dest, spliterator, boxingAdapter, rootCharacteristics, useTryAdvance);
        }
        else {
            long sizeIfKnown = spliterator.getExactSizeIfKnown();
            if (useTryAdvance) {
                Consumer<T> addToDest = boxingAdapter.apply(dest::add);
                int count = 0;
                while (spliterator.tryAdvance(addToDest)) {
                    ++count;
                }

                if (sizeIfKnown >= 0)
                    assertEquals(sizeIfKnown, count);

                // Assert that forEach now produces no elements
                spliterator.forEachRemaining(boxingAdapter.apply(
                        e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));

                Spliterator<T> split = spliterator.trySplit();
                assertNull(split);
            }
            else {
                List<T> leafDest = new ArrayList<>();
                Consumer<T> addToLeafDest = boxingAdapter.apply(leafDest::add);
                spliterator.forEachRemaining(addToLeafDest);

                if (sizeIfKnown >= 0)
                    assertEquals(sizeIfKnown, leafDest.size());

                // Assert that forEach now produces no elements
                spliterator.tryAdvance(boxingAdapter.apply(
                        e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

                Spliterator<T> split = spliterator.trySplit();
                assertNull(split);

                dest.addAll(leafDest);
            }
        }
    }

    private static <T, S extends Spliterator<T>> void testSplitUntilNull(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter,
            ContentAsserter<T> asserter) {
        Spliterator<T> s = supplier.get();
        boolean isOrdered = s.hasCharacteristics(Spliterator.ORDERED);
        assertSpliterator(s);

        List<T> splits = new ArrayList<>();
        Consumer<T> c = boxingAdapter.apply(splits::add);

        testSplitUntilNull(new SplitNode<T>(c, s));
        asserter.assertContents(splits, exp, isOrdered);
    }

    private static class SplitNode<T> {
        // Constant for every node
        final Consumer<T> c;
        final int rootCharacteristics;

        final Spliterator<T> s;

        SplitNode(Consumer<T> c, Spliterator<T> s) {
            this(c, s.characteristics(), s);
        }

        private SplitNode(Consumer<T> c, int rootCharacteristics, Spliterator<T> s) {
            this.c = c;
            this.rootCharacteristics = rootCharacteristics;
            this.s = s;
        }

        SplitNode<T> fromSplit(Spliterator<T> split) {
            return new SplitNode<>(c, rootCharacteristics, split);
        }
    }

    /**
     * Set the maximum stack capacity to 0.25MB. This should be more than enough to detect a bad spliterator
     * while not unduly disrupting test infrastructure given the test data sizes that are used are small.
     * Note that j.u.c.ForkJoinPool sets the max queue size to 64M (1 << 26).
     */
    private static final int MAXIMUM_STACK_CAPACITY = 1 << 18; // 0.25MB

    private static <T> void testSplitUntilNull(SplitNode<T> e) {
        // Use an explicit stack to avoid a StackOverflowException when testing a Spliterator
        // that when repeatedly split produces a right-balanced (and maybe degenerate) tree, or
        // for a spliterator that is badly behaved.
        Deque<SplitNode<T>> stack = new ArrayDeque<>();
        stack.push(e);

        int iteration = 0;
        while (!stack.isEmpty()) {
            assertTrue(iteration++ < MAXIMUM_STACK_CAPACITY, "Exceeded maximum stack modification count of 1 << 18");

            e = stack.pop();
            Spliterator<T> parentAndRightSplit = e.s;

            long parentEstimateSize = parentAndRightSplit.estimateSize();
            assertTrue(parentEstimateSize >= 0,
                       String.format("Split size estimate %d < 0", parentEstimateSize));

            long parentSize = parentAndRightSplit.getExactSizeIfKnown();
            Spliterator<T> leftSplit = parentAndRightSplit.trySplit();
            if (leftSplit == null) {
                parentAndRightSplit.forEachRemaining(e.c);
                continue;
            }

            assertSpliterator(leftSplit, e.rootCharacteristics);
            assertSpliterator(parentAndRightSplit, e.rootCharacteristics);

            if (parentEstimateSize != Long.MAX_VALUE && leftSplit.estimateSize() > 0
                && parentAndRightSplit.estimateSize() > 0) {
                assertTrue(leftSplit.estimateSize() < parentEstimateSize,
                           String.format("Left split size estimate %d >= parent split size estimate %d",
                                         leftSplit.estimateSize(), parentEstimateSize));
                assertTrue(parentAndRightSplit.estimateSize() < parentEstimateSize,
                           String.format("Right split size estimate %d >= parent split size estimate %d",
                                         leftSplit.estimateSize(), parentEstimateSize));
            }
            else {
                assertTrue(leftSplit.estimateSize() <= parentEstimateSize,
                           String.format("Left split size estimate %d > parent split size estimate %d",
                                         leftSplit.estimateSize(), parentEstimateSize));
                assertTrue(parentAndRightSplit.estimateSize() <= parentEstimateSize,
                           String.format("Right split size estimate %d > parent split size estimate %d",
                                         leftSplit.estimateSize(), parentEstimateSize));
            }

            long leftSize = leftSplit.getExactSizeIfKnown();
            long rightSize = parentAndRightSplit.getExactSizeIfKnown();
            if (parentSize >= 0 && leftSize >= 0 && rightSize >= 0)
                assertEquals(parentSize, leftSize + rightSize,
                             String.format("exact left split size %d + exact right split size %d != parent exact split size %d",
                                           leftSize, rightSize, parentSize));

            // Add right side to stack first so left side is popped off first
            stack.push(e.fromSplit(parentAndRightSplit));
            stack.push(e.fromSplit(leftSplit));
        }
    }

    private static void assertSpliterator(Spliterator<?> s, int rootCharacteristics) {
        if ((rootCharacteristics & Spliterator.SUBSIZED) != 0) {
            assertTrue(s.hasCharacteristics(Spliterator.SUBSIZED),
                       "Child split is not SUBSIZED when root split is SUBSIZED");
        }
        assertSpliterator(s);
    }

    private static void assertSpliterator(Spliterator<?> s) {
        if (s.hasCharacteristics(Spliterator.SUBSIZED)) {
            assertTrue(s.hasCharacteristics(Spliterator.SIZED));
        }
        if (s.hasCharacteristics(Spliterator.SIZED)) {
            assertTrue(s.estimateSize() != Long.MAX_VALUE);
            assertTrue(s.getExactSizeIfKnown() >= 0);
        }
        try {
            s.getComparator();
            assertTrue(s.hasCharacteristics(Spliterator.SORTED));
        } catch (IllegalStateException e) {
            assertFalse(s.hasCharacteristics(Spliterator.SORTED));
        }
    }

    private static<T> void assertContents(Collection<T> actual, Collection<T> expected, boolean isOrdered) {
        if (isOrdered) {
            assertEquals(actual, expected);
        }
        else {
            LambdaTestHelpers.assertContentsUnordered(actual, expected);
        }
    }

    public static void assertThrowsNPE(ThrowingRunnable r) {
        assertThrows(NullPointerException.class, r);
    }

    public static<U> void mixedTraverseAndSplit(Consumer<U> b, Spliterator<U> splTop) {
        Spliterator<U> spl1, spl2, spl3;
        splTop.tryAdvance(b);
        spl2 = splTop.trySplit();
        if (spl2 != null) {
            spl2.tryAdvance(b);
            spl1 = spl2.trySplit();
            if (spl1 != null) {
                spl1.tryAdvance(b);
                spl1.forEachRemaining(b);
            }
            spl2.tryAdvance(b);
            spl2.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        spl3 = splTop.trySplit();
        if (spl3 != null) {
            spl3.tryAdvance(b);
            spl3.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        splTop.forEachRemaining(b);
    }

    public static void mixedTraverseAndSplit(IntConsumer b, Spliterator.OfInt splTop) {
        Spliterator.OfInt spl1, spl2, spl3;
        splTop.tryAdvance(b);
        spl2 = splTop.trySplit();
        if (spl2 != null) {
            spl2.tryAdvance(b);
            spl1 = spl2.trySplit();
            if (spl1 != null) {
                spl1.tryAdvance(b);
                spl1.forEachRemaining(b);
            }
            spl2.tryAdvance(b);
            spl2.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        spl3 = splTop.trySplit();
        if (spl3 != null) {
            spl3.tryAdvance(b);
            spl3.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        splTop.forEachRemaining(b);
    }

    public static void mixedTraverseAndSplit(LongConsumer b, Spliterator.OfLong splTop) {
        Spliterator.OfLong spl1, spl2, spl3;
        splTop.tryAdvance(b);
        spl2 = splTop.trySplit();
        if (spl2 != null) {
            spl2.tryAdvance(b);
            spl1 = spl2.trySplit();
            if (spl1 != null) {
                spl1.tryAdvance(b);
                spl1.forEachRemaining(b);
            }
            spl2.tryAdvance(b);
            spl2.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        spl3 = splTop.trySplit();
        if (spl3 != null) {
            spl3.tryAdvance(b);
            spl3.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        splTop.forEachRemaining(b);
    }

    public static void mixedTraverseAndSplit(DoubleConsumer b, Spliterator.OfDouble splTop) {
        Spliterator.OfDouble spl1, spl2, spl3;
        splTop.tryAdvance(b);
        spl2 = splTop.trySplit();
        if (spl2 != null) {
            spl2.tryAdvance(b);
            spl1 = spl2.trySplit();
            if (spl1 != null) {
                spl1.tryAdvance(b);
                spl1.forEachRemaining(b);
            }
            spl2.tryAdvance(b);
            spl2.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        spl3 = splTop.trySplit();
        if (spl3 != null) {
            spl3.tryAdvance(b);
            spl3.forEachRemaining(b);
        }
        splTop.tryAdvance(b);
        splTop.forEachRemaining(b);
    }

}
