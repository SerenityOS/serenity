/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

/**
 * LambdaTestHelpers -- assertion methods and useful objects for lambda test cases
 */
public class LambdaTestHelpers {
    public static final String LONG_STRING = "When in the Course of human events it becomes necessary for one people to dissolve the political bands which have connected them with another and to assume among the powers of the earth, the separate and equal station to which the Laws of Nature and of Nature's God entitle them, a decent respect to the opinions of mankind requires that they should declare the causes which impel them to the separation.";

    @SuppressWarnings("rawtypes")
    public static final Consumer bEmpty = x -> {  };
    @SuppressWarnings("rawtypes")
    public static final IntConsumer bIntEmpty = x -> {  };
    @SuppressWarnings("rawtypes")
    public static final BiConsumer bBiEmpty = (x,y) -> { };
    @SuppressWarnings("rawtypes")
    public static final Consumer bHashCode = x -> { Objects.hashCode(x); };
    @SuppressWarnings("rawtypes")
    public static final BiConsumer bBiHashCode = (x,y) -> { Objects.hash(x, y); };
    public static final Function<Integer, Integer> mZero = x -> 0;
    public static final Function<Integer, Integer> mId = x -> x;
    public static final Function<Integer, Integer> mDoubler = x -> x * 2;
    public static final Function<Integer, Stream<Integer>> mfId = e -> Collections.singletonList(e).stream();
    public static final Function<Integer, Stream<Integer>> mfNull = e -> Collections.<Integer>emptyList().stream();
    public static final Function<Integer, Stream<Integer>> mfLt = e -> {
        List<Integer> l = new ArrayList<>();
        for (int i=0; i<e; i++)
            l.add(i);
        return l.stream();
    };
    public static final ToIntFunction<Integer> imDoubler = x -> x * 2;
    public static final ToLongFunction<Long> lmDoubler = x -> x * 2;
    public static final ToDoubleFunction<Double> dmDoubler = x -> x * 2;
    public static final Predicate<Integer> pFalse = x -> false;
    public static final Predicate<Integer> pTrue = x -> true;
    public static final Predicate<Integer> pEven = x -> 0 == x % 2;
    public static final Predicate<Integer> pOdd = x -> 1 == x % 2;
    public static final IntPredicate ipFalse = x -> false;
    public static final IntPredicate ipTrue = x -> true;
    public static final IntPredicate ipEven = x -> 0 == x % 2;
    public static final IntPredicate ipOdd = x -> 1 == x % 2;
    public static final LongPredicate lpFalse = x -> false;
    public static final LongPredicate lpTrue = x -> true;
    public static final LongPredicate lpEven = x -> 0 == x % 2;
    public static final LongPredicate lpOdd = x -> 1 == x % 2;
    public static final DoublePredicate dpFalse = x -> false;
    public static final DoublePredicate dpTrue = x -> true;
    public static final DoublePredicate dpEven = x -> 0 == ((long) x) % 2;
    public static final DoublePredicate dpOdd = x -> 1 == ((long) x) % 2;
    public static final BinaryOperator<Integer> rPlus = (x, y) -> x+y;
    public static final BinaryOperator<Integer> rMax = (x, y) -> Math.max(x, y);
    public static final BinaryOperator<Integer> rMin = (x, y) -> Math.min(x,y);
    public static final IntBinaryOperator irPlus = (x, y) -> x+y;
    public static final IntBinaryOperator irMax = (x, y) -> Math.max(x, y);
    public static final IntBinaryOperator irMin = (x, y) -> Math.min(x,y);
    public static final IntUnaryOperator irDoubler = x -> x * 2;
    public static final LongBinaryOperator lrPlus = (x, y) -> x+y;
    public static final DoubleBinaryOperator drPlus = (x, y) -> x+y;
    public static final Comparator<Integer> cInteger = (a, b) -> Integer.compare(a, b);
    public static final BiPredicate<?, ?> bipFalse = (x, y) -> false;
    public static final BiPredicate<?, ?> bipTrue = (x, y) -> true;
    public static final BiPredicate<Integer, Integer> bipBothEven = (x, y) -> 0 == (x % 2 + y % 2);
    public static final BiPredicate<Integer, Integer> bipBothOdd = (x, y) -> 2 == (x % 2 + y % 2);
    public static final BiPredicate<?, ?> bipSameString = (x, y) -> String.valueOf(x).equals(String.valueOf(y));

    public static final IntFunction<Integer[]> integerArrayGenerator = s -> new Integer[s];

    public static final IntFunction<Object[]> objectArrayGenerator = s -> new Object[s];

    public static final Function<String, Stream<Character>> flattenChars = string -> {
        List<Character> l = new ArrayList<>();
        for (int i=0; i<string.length(); i++)
            l.add(string.charAt(i));
        return l.stream();
    };

    public static final Function<String, IntStream> flattenInt
            = string -> IntStream.range(0, string.length()).map(string::charAt);

    public static <T, R> Function<T, R> forPredicate(Predicate<? super T> predicate, R forTrue, R forFalse) {
        Objects.requireNonNull(predicate);

        return t -> predicate.test(t) ? forTrue : forFalse;
    }

    public static <T> Function<T, T> identity() {
        return t -> t;
    }

    public static<V, T, R> Function<V, R> compose(Function<? super T, ? extends R> after, Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> after.apply(before.apply(v));
    }

    public static List<Integer> empty() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(null);
        return list;
    }

    public static List<Integer> countTo(int n) {
        return range(1, n);
    }

    public static List<Integer> range(int l, int u) {
        ArrayList<Integer> list = new ArrayList<>(u - l + 1);
        for (int i=l; i<=u; i++) {
            list.add(i);
        }
        return list;
    }

    public static List<Integer> repeat(int value, int n) {
        ArrayList<Integer> list = new ArrayList<>(n);
        for (int i=1; i<=n; i++) {
            list.add(value);
        }
        return list;
    }

    public static List<Double> asDoubles(List<Integer> integers) {
        ArrayList<Double> list = new ArrayList<>();
        for (Integer i : integers) {
            list.add((double) i);
        }
        return list;
    }

    public static List<Long> asLongs(List<Integer> integers) {
        ArrayList<Long> list = new ArrayList<>();
        for (Integer i : integers) {
            list.add((long) i);
        }
        return list;
    }

    public static void assertCountSum(Stream<? super Integer> it, int count, int sum) {
        assertCountSum(it.iterator(), count, sum);
    }

    public static void assertCountSum(Iterable<? super Integer> it, int count, int sum) {
        assertCountSum(it.iterator(), count, sum);
    }

    public static void assertCountSum(Iterator<? super Integer> it, int count, int sum) {
        int c = 0;
        int s = 0;
        while (it.hasNext()) {
            int i = (Integer) it.next();
            c++;
            s += i;
        }

        assertEquals(c, count);
        assertEquals(s, sum);
    }

    public static void assertConcat(Iterator<Character> it, String result) {
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next());
        }

        assertEquals(result, sb.toString());
    }

    public static<T extends Comparable<? super T>> void assertSorted(Iterator<T> i) {
        i = toBoxedList(i).iterator();

        if (!i.hasNext())
            return;
        T last = i.next();
        while (i.hasNext()) {
            T t = i.next();
            assertTrue(last.compareTo(t) <= 0);
            assertTrue(t.compareTo(last) >= 0);
            last = t;
        }
    }

    public static<T> void assertSorted(Iterator<T> i, Comparator<? super T> comp) {
        if (i instanceof PrimitiveIterator.OfInt
                || i instanceof PrimitiveIterator.OfDouble
                || i instanceof PrimitiveIterator.OfLong) {
            i = toBoxedList(i).iterator();
        }

        if (!i.hasNext())
            return;
        T last = i.next();
        while (i.hasNext()) {
            T t = i.next();
            assertTrue(comp.compare(last, t) <= 0);
            assertTrue(comp.compare(t, last) >= 0);
            last = t;
        }
    }

    public static<T extends Comparable<? super T>> void assertSorted(Iterable<T> iter) {
        assertSorted(iter.iterator());
    }

    public static<T> void assertSorted(Iterable<T> iter, Comparator<? super T> comp) {
        assertSorted(iter.iterator(), comp);
    }

    public static <T> void assertUnique(Iterable<T> iter) {
        assertUnique(iter.iterator());
    }

    public static<T> void assertUnique(Iterator<T> iter) {
        if (!iter.hasNext()) {
            return;
        }

        if (iter instanceof PrimitiveIterator.OfInt
            || iter instanceof PrimitiveIterator.OfDouble
            || iter instanceof PrimitiveIterator.OfLong) {
            iter = toBoxedList(iter).iterator();
        }

        Set<T> uniq = new HashSet<>();
        while(iter.hasNext()) {
            T each = iter.next();
            assertTrue(!uniq.contains(each), "Not unique");
            uniq.add(each);
        }
    }

    public static<T> void assertContents(Iterable<T> actual, Iterable<T> expected) {
        if (actual instanceof Collection && expected instanceof Collection) {
            assertIterableEquals(actual, expected);
        } else {
            assertContents(actual.iterator(), expected.iterator());
        }
    }

    public static<T> void assertContents(Iterator<T> actual, Iterator<T> expected) {
        assertEquals(toBoxedList(actual), toBoxedList(expected));
    }

    // Workaround excessive String creation in inner loop in org.testng.Assert.assertEquals(Iterable<?>, Iterable<?>)
    static public void assertIterableEquals(Iterable<?> actual, Iterable<?> expected) {
        if(actual == expected) {
            return;
        }

        if(actual == null || expected == null) {
            fail("Iterables not equal: expected: " + expected + " and actual: " + actual);
        }

        assertIteratorsEquals(actual.iterator(), expected.iterator());
    }

    // Workaround excessive String creation in inner loop in org.testng.Assert.assertEquals(Iterator<?>, Iterator<?>)
    static public void assertIteratorsEquals(Iterator<?> actual, Iterator<?> expected) {
        if (actual == expected) {
            return;
        }

        if (actual == null || expected == null) {
            fail("Iterators not equal: expected: " + expected + " and actual: " + actual);
        }

        while (actual.hasNext() && expected.hasNext()) {
            Object e = expected.next();
            Object a = actual.next();
            assertEquals(a, e, "Iterator contents differ");
        }

        if(actual.hasNext()) {
            fail("Actual iterator returned more elements than the expected iterator.");
        } else if(expected.hasNext()) {
            fail("Expected iterator returned more elements than the actual iterator.");
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static<T> void assertContents(Iterator<T> actual, T... expected) {
        assertContents(actual, Arrays.asList(expected).iterator());
    }

    /**
     * The all consuming consumer (rampant capitalist) that can accepting a reference or any primitive value.
     */
    private static interface OmnivorousConsumer<T>
            extends Consumer<T>, IntConsumer, LongConsumer, DoubleConsumer { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static<T> Consumer<T> toBoxingConsumer(Consumer<? super T> c) {
        return (Consumer<T>) new OmnivorousConsumer() {
            @Override
            public void accept(Object t) {
                c.accept((T) t);
            }

            @Override
            public void accept(int t) {
                accept((Object) t);
            }

            @Override
            public void accept(long t) {
                accept((Object) t);
            }

            @Override
            public void accept(double t) {
                accept((Object) t);
            }
        };
    }

    /**
     * Convert an iterator to a list using forEach with an implementation of
     * {@link java.util.stream.LambdaTestHelpers.OmnivorousConsumer}.
     *
     * This ensures equality comparisons for test results do not trip
     * the boxing trip-wires.
     */
    private static<T> List<T> toBoxedList(Iterator<T> it) {
        List<T> l = new ArrayList<>();
        it.forEachRemaining(toBoxingConsumer(l::add));
        return l;
    }

    /**
     * Convert a spliterator to a list using forEach with an implementation of
     * {@link java.util.stream.LambdaTestHelpers.OmnivorousConsumer}.
     *
     * This ensures equality comparisons for test results do not trip
     * the boxing trip-wires.
     */
    public static<T> List<T> toBoxedList(Spliterator<T> sp) {
        List<T> l = new ArrayList<>();
        sp.forEachRemaining(toBoxingConsumer(l::add));
        return l;
    }

    /**
     * Convert an iterator to a multi-set, represented as a Map, using forEach with an implementation of
     * {@link java.util.stream.LambdaTestHelpers.OmnivorousConsumer}.
     *
     * This ensures equality comparisons for test results do not trip
     * the boxing trip-wires.
     */
    @SuppressWarnings("unchecked")
    private static<T> Map<T, Integer> toBoxedMultiset(Iterator<T> it) {
        Map<Object, Integer> result = new HashMap<>();

        it.forEachRemaining(toBoxingConsumer(o -> {
                if (result.containsKey(o))
                    result.put(o, result.get(o) + 1);
                else
                    result.put(o, 1);
            }));

        return (Map<T, Integer>) result;
    }

    @SuppressWarnings("unchecked")
    public static<T> Map<T, Integer> toBoxedMultiset(Spliterator<T> it) {
        Map<Object, Integer> result = new HashMap<>();

        it.forEachRemaining(toBoxingConsumer(o -> {
                if (result.containsKey(o))
                    result.put(o, result.get(o) + 1);
                else
                    result.put(o, 1);
            }));

        return (Map<T, Integer>) result;
    }

    @SuppressWarnings("unchecked")
    public static void assertContentsEqual(Object a, Object b) {
        if (a instanceof Iterable && b instanceof Iterable)
            assertContents((Iterable) a, (Iterable) b);
        else
            assertEquals(a, b);
    }

    public static<T> void assertContentsUnordered(Iterable<T> actual, Iterable<T> expected) {
        assertContentsUnordered(actual.iterator(), expected.iterator());
    }

    public static<T> void assertContentsUnordered(Iterator<T> actual, Iterator<T> expected) {
        assertEquals(toBoxedMultiset(actual), toBoxedMultiset(expected));
    }

    public static<T> void assertContains(Optional<T> actual, Iterator<T> it) {
        actual.ifPresentOrElse(r -> {
            boolean contained = false;
            while (!contained && it.hasNext()) {
                contained = Objects.equals(r, it.next());
            }
            assertTrue(contained, "Not found: "+r);
        }, () -> assertFalse(it.hasNext()));
    }

    public static void launderAssertion(Runnable r, Supplier<String> additionalInfo) {
        try {
            r.run();
        }
        catch (AssertionError ae) {
            AssertionError cloned = new AssertionError(ae.getMessage() + String.format("%n%s", additionalInfo.get()));
            cloned.setStackTrace(ae.getStackTrace());
            if (ae.getCause() != null)
                cloned.initCause(ae.getCause());
            throw cloned;
        }
    }

    public static <T, S extends BaseStream<T, S>>
    List<Function<S, S>> permuteStreamFunctions(List<Function<S, S>> opFunctions) {
        List<List<Function<S, S>>> opFunctionPermutations = perm(opFunctions);

        List<Function<S, S>> appliedFunctions = new ArrayList<>();
        for (List<Function<S, S>> fs : opFunctionPermutations) {
            Function<S, S> applied = s -> {
                for (Function<S, S> f : fs) {
                    s = f.apply(s);
                }
                return s;
            };
            appliedFunctions.add(applied);
        }

        return appliedFunctions;
    }

    private static <T> List<T> sub(List<T> l, int index) {
        List<T> subL = new ArrayList<>(l);
        subL.remove(index);
        return subL;
    }

    public static <T> List<List<T>> perm(List<T> l) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            for (List<T> perm : perm(sub(l, i))) {
                perm.add(0, l.get(i));
                result.add(perm);
            }
        }
        result.add(new ArrayList<T>());

        return result;
    }

    public static String flagsToString(int flags) {
        StringJoiner sj = new StringJoiner(", ", "StreamOpFlag[", "]");
        if (StreamOpFlag.DISTINCT.isKnown(flags)) sj.add("IS_DISTINCT");
        if (StreamOpFlag.ORDERED.isKnown(flags)) sj.add("IS_ORDERED");
        if (StreamOpFlag.SIZED.isKnown(flags)) sj.add("IS_SIZED");
        if (StreamOpFlag.SORTED.isKnown(flags)) sj.add("IS_SORTED");
        if (StreamOpFlag.SHORT_CIRCUIT.isKnown(flags)) sj.add("IS_SHORT_CIRCUIT");
        return sj.toString();
    }
}
