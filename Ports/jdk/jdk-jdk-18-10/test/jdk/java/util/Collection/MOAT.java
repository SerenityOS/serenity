/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     6207984 6272521 6192552 6269713 6197726 6260652 5073546 4137464
 *          4155650 4216399 4294891 6282555 6318622 6355327 6383475 6420753
 *          6431845 4802633 6570566 6570575 6570631 6570924 6691185 6691215
 *          4802647 7123424 8024709 8193128
 * @summary Run many tests on many Collection and Map implementations
 * @author  Martin Buchholz
 * @modules java.base/java.util:open
 * @run main MOAT
 * @key randomness
 */

/* Mother Of All (Collection) Tests
 *
 * Testing of collection classes is often spotty, because many tests
 * need to be performed on many implementations, but the onus on
 * writing the tests falls on the engineer introducing the new
 * implementation.
 *
 * The idea of this mega-test is that:
 *
 * An engineer adding a new collection implementation could simply add
 * their new implementation to a list of implementations in this
 * test's main method.  Any general purpose Collection<Integer> or
 * Map<Integer,Integer> class is appropriate.
 *
 * An engineer fixing a regression could add their regression test here and
 * simultaneously test all other implementations.
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import static java.util.Collections.*;
import java.lang.reflect.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MOAT {
    // Collections under test must not be initialized to contain this value,
    // and maps under test must not contain this value as a key.
    // It's used as a sentinel for absent-element testing.
    static final int ABSENT_VALUE = 778347983;

    static final Integer[] integerArray;
    static {
        Integer[] ia = new Integer[20];
        // fill with 1..20 inclusive
        for (int i = 0; i < ia.length; i++) {
            ia[i] = i + 1;
        }
        integerArray = ia;
    }

    public static void realMain(String[] args) {

        testCollection(new NewAbstractCollection<Integer>());
        testCollection(new NewAbstractSet<Integer>());
        testCollection(new LinkedHashSet<Integer>());
        testCollection(new HashSet<Integer>());
        testCollection(new Vector<Integer>());
        testCollection(new Vector<Integer>().subList(0,0));
        testCollection(new ArrayDeque<Integer>());
        testCollection(new ArrayList<Integer>());
        testCollection(new ArrayList<Integer>().subList(0,0));
        testCollection(new LinkedList<Integer>());
        testCollection(new LinkedList<Integer>().subList(0,0));
        testCollection(new TreeSet<Integer>());
        testCollection(Collections.checkedList(new ArrayList<Integer>(), Integer.class));
        testCollection(Collections.synchronizedList(new ArrayList<Integer>()));
        testCollection(Collections.checkedSet(new HashSet<Integer>(), Integer.class));
        testCollection(Collections.checkedSortedSet(new TreeSet<Integer>(), Integer.class));
        testCollection(Collections.checkedNavigableSet(new TreeSet<Integer>(), Integer.class));
        testCollection(Collections.synchronizedSet(new HashSet<Integer>()));
        testCollection(Collections.synchronizedSortedSet(new TreeSet<Integer>()));
        testCollection(Collections.synchronizedNavigableSet(new TreeSet<Integer>()));

        testCollection(new CopyOnWriteArrayList<Integer>());
        testCollection(new CopyOnWriteArrayList<Integer>().subList(0,0));
        testCollection(new CopyOnWriteArraySet<Integer>());
        testCollection(new PriorityQueue<Integer>());
        testCollection(new PriorityBlockingQueue<Integer>());
        testCollection(new ArrayBlockingQueue<Integer>(20));
        testCollection(new LinkedBlockingQueue<Integer>(20));
        testCollection(new LinkedBlockingDeque<Integer>(20));
        testCollection(new ConcurrentLinkedDeque<Integer>());
        testCollection(new ConcurrentLinkedQueue<Integer>());
        testCollection(new LinkedTransferQueue<Integer>());
        testCollection(new ConcurrentSkipListSet<Integer>());
        testCollection(Arrays.asList(new Integer(42)));
        testCollection(Arrays.asList(1,2,3));
        testCollection(nCopies(25,1));
        testImmutableList(nCopies(25,1));

        testMap(new HashMap<Integer,Integer>());
        testMap(new LinkedHashMap<Integer,Integer>());

        // TODO: Add reliable support for WeakHashMap.
        // This test is subject to very rare failures because the GC
        // may remove unreferenced-keys from the map at any time.
        // testMap(new WeakHashMap<Integer,Integer>());

        testMap(new IdentityHashMap<Integer,Integer>());
        testMap(new TreeMap<Integer,Integer>());
        testMap(new Hashtable<Integer,Integer>());
        testMap(new ConcurrentHashMap<Integer,Integer>(10, 0.5f));
        testMap(new ConcurrentSkipListMap<Integer,Integer>());
        testMap(Collections.checkedMap(new HashMap<Integer,Integer>(), Integer.class, Integer.class));
        testMap(Collections.checkedSortedMap(new TreeMap<Integer,Integer>(), Integer.class, Integer.class));
        testMap(Collections.checkedNavigableMap(new TreeMap<Integer,Integer>(), Integer.class, Integer.class));
        testMap(Collections.synchronizedMap(new HashMap<Integer,Integer>()));
        testMap(Collections.synchronizedSortedMap(new TreeMap<Integer,Integer>()));
        testMap(Collections.synchronizedNavigableMap(new TreeMap<Integer,Integer>()));

        // Unmodifiable wrappers
        testImmutableSet(unmodifiableSet(new HashSet<>(Arrays.asList(1,2,3))));
        testImmutableList(unmodifiableList(Arrays.asList(1,2,3)));
        testImmutableMap(unmodifiableMap(Collections.singletonMap(1,2)));
        testCollMutatorsAlwaysThrow(unmodifiableSet(new HashSet<>(Arrays.asList(1,2,3))));
        testCollMutatorsAlwaysThrow(unmodifiableSet(Collections.emptySet()));
        testEmptyCollMutatorsAlwaysThrow(unmodifiableSet(Collections.emptySet()));
        testListMutatorsAlwaysThrow(unmodifiableList(Arrays.asList(1,2,3)));
        testListMutatorsAlwaysThrow(unmodifiableList(Collections.emptyList()));
        testEmptyListMutatorsAlwaysThrow(unmodifiableList(Collections.emptyList()));
        testMapMutatorsAlwaysThrow(unmodifiableMap(Collections.singletonMap(1,2)));
        testMapMutatorsAlwaysThrow(unmodifiableMap(Collections.emptyMap()));
        testEmptyMapMutatorsAlwaysThrow(unmodifiableMap(Collections.emptyMap()));

        // Empty collections
        final List<Integer> emptyArray = Arrays.asList(new Integer[]{});
        testCollection(emptyArray);
        testEmptyList(emptyArray);
        THROWS(IndexOutOfBoundsException.class, () -> emptyArray.set(0,1));
        THROWS(UnsupportedOperationException.class, () -> emptyArray.add(0,1));

        List<Integer> noOne = nCopies(0,1);
        testCollection(noOne);
        testEmptyList(noOne);
        testImmutableList(noOne);

        Set<Integer> emptySet = emptySet();
        testCollection(emptySet);
        testEmptySet(emptySet);
        testEmptySet(EMPTY_SET);
        testEmptySet(Collections.emptySet());
        testEmptySet(Collections.emptySortedSet());
        testEmptySet(Collections.emptyNavigableSet());
        testImmutableSet(emptySet);

        List<Integer> emptyList = emptyList();
        testCollection(emptyList);
        testEmptyList(emptyList);
        testEmptyList(EMPTY_LIST);
        testEmptyList(Collections.emptyList());
        testImmutableList(emptyList);

        Map<Integer,Integer> emptyMap = emptyMap();
        testMap(emptyMap);
        testEmptyMap(emptyMap);
        testEmptyMap(EMPTY_MAP);
        testEmptyMap(Collections.emptyMap());
        testEmptyMap(Collections.emptySortedMap());
        testEmptyMap(Collections.emptyNavigableMap());
        testImmutableMap(emptyMap);
        testImmutableMap(Collections.emptyMap());
        testImmutableMap(Collections.emptySortedMap());
        testImmutableMap(Collections.emptyNavigableMap());

        // Singleton collections
        Set<Integer> singletonSet = singleton(1);
        equal(singletonSet.size(), 1);
        testCollection(singletonSet);
        testImmutableSet(singletonSet);

        List<Integer> singletonList = singletonList(1);
        equal(singletonList.size(), 1);
        testCollection(singletonList);
        testImmutableList(singletonList);
        testImmutableList(singletonList.subList(0,1));
        testImmutableList(singletonList.subList(0,1).subList(0,1));
        testEmptyList(singletonList.subList(0,0));
        testEmptyList(singletonList.subList(0,0).subList(0,0));

        Map<Integer,Integer> singletonMap = singletonMap(1,2);
        equal(singletonMap.size(), 1);
        testMap(singletonMap);
        testImmutableMap(singletonMap);

        // Immutable List
        testEmptyList(List.of());
        testEmptyList(List.of().subList(0,0));
        testListMutatorsAlwaysThrow(List.of());
        testListMutatorsAlwaysThrow(List.<Integer>of().subList(0,0));
        testEmptyListMutatorsAlwaysThrow(List.of());
        testEmptyListMutatorsAlwaysThrow(List.<Integer>of().subList(0,0));
        for (List<Integer> list : Arrays.asList(
                List.<Integer>of(),
                List.of(1),
                List.of(1, 2),
                List.of(1, 2, 3),
                List.of(1, 2, 3, 4),
                List.of(1, 2, 3, 4, 5),
                List.of(1, 2, 3, 4, 5, 6),
                List.of(1, 2, 3, 4, 5, 6, 7),
                List.of(1, 2, 3, 4, 5, 6, 7, 8),
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                List.of(integerArray),
                Stream.<Integer>empty().toList(),
                Stream.of(1).toList(),
                Stream.of(1, 2).toList(),
                Stream.of(1, 2, 3).toList(),
                Stream.of(1, 2, 3, 4).toList(),
                Stream.of((Integer)null).toList(),
                Stream.of(1, null).toList(),
                Stream.of(1, null, 3).toList(),
                Stream.of(1, null, 3, 4).toList())) {
            testCollection(list);
            testImmutableList(list);
            testListMutatorsAlwaysThrow(list);
            if (list.size() >= 1) {
                // test subLists
                List<Integer> headList = list.subList(0, list.size() - 1);
                List<Integer> tailList = list.subList(1, list.size());
                testCollection(headList);
                testCollection(tailList);
                testImmutableList(headList);
                testImmutableList(tailList);
                testListMutatorsAlwaysThrow(headList);
                testListMutatorsAlwaysThrow(tailList);
            }
        }

        List<Integer> listCopy = List.copyOf(Arrays.asList(1, 2, 3));
        testCollection(listCopy);
        testImmutableList(listCopy);
        testListMutatorsAlwaysThrow(listCopy);

        List<Integer> listCollected = Stream.of(1, 2, 3).collect(Collectors.toUnmodifiableList());
        equal(listCollected, List.of(1, 2, 3));
        testCollection(listCollected);
        testImmutableList(listCollected);
        testListMutatorsAlwaysThrow(listCollected);

        // List indexOf / lastIndexOf

        // 0 element
        System.out.println("testListIndexOf size 0");
        testListIndexOf(-1, -1);

        System.out.println("testListIndexOf size 1");
        testListIndexOf(-1, -1, 0);
        testListIndexOf(0, 0, 1);

        System.out.println("testListIndexOf size 2");
        testListIndexOf(-1, -1, 0, 0);
        testListIndexOf(0, 0, 1, 0);
        testListIndexOf(0, 1, 1, 1);
        testListIndexOf(1, 1, 0, 1);


        System.out.println("testListIndexOf size 3");
        testListIndexOf(-1, -1, 0, 0, 0);
        testListIndexOf(0, 0, 1, 0, 0);
        testListIndexOf(0, 1, 1, 1, 0);
        testListIndexOf(1, 2, 0, 1, 1);
        testListIndexOf(2, 2, 0, 0, 1);

        System.out.println("testListIndexOf size N");
        testListIndexOf(-1, -1, 0, 0, 0, 0, 0, 0, 0);
        testListIndexOf(2, 6, 0, 0, 1, 0, 1, 0, 1);
        testListIndexOf(4, 4, 0, 0, 0, 0, 1, 0, 0);
        testListIndexOf(0, 6, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 7, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 9, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 10, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 11, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(0, 12, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        testListIndexOf(12, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
        testListIndexOf(-1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        // Immutable Set
        testEmptySet(Set.of());
        testCollMutatorsAlwaysThrow(Set.of());
        testEmptyCollMutatorsAlwaysThrow(Set.of());
        for (Set<Integer> set : Arrays.asList(
                Set.<Integer>of(),
                Set.of(1),
                Set.of(1, 2),
                Set.of(1, 2, 3),
                Set.of(1, 2, 3, 4),
                Set.of(1, 2, 3, 4, 5),
                Set.of(1, 2, 3, 4, 5, 6),
                Set.of(1, 2, 3, 4, 5, 6, 7),
                Set.of(1, 2, 3, 4, 5, 6, 7, 8),
                Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                Set.of(integerArray))) {
            testCollection(set);
            testImmutableSet(set);
            testCollMutatorsAlwaysThrow(set);
        }

        Set<Integer> setCopy = Set.copyOf(Arrays.asList(1, 2, 3));
        testCollection(setCopy);
        testImmutableSet(setCopy);
        testCollMutatorsAlwaysThrow(setCopy);

        Set<Integer> setCollected = Stream.of(1, 1, 2, 3, 2, 3)
                                          .collect(Collectors.toUnmodifiableSet());
        equal(setCollected, Set.of(1, 2, 3));
        testCollection(setCollected);
        testImmutableSet(setCollected);
        testCollMutatorsAlwaysThrow(setCollected);

        // Immutable Map

        @SuppressWarnings("unchecked")
        Map.Entry<Integer,Integer>[] ea = (Map.Entry<Integer,Integer>[])new Map.Entry<?,?>[20];
        for (int i = 0; i < ea.length; i++) {
            ea[i] = Map.entry(i+1, i+101);
        }

        testEmptyMap(Map.of());
        testMapMutatorsAlwaysThrow(Map.of());
        testEmptyMapMutatorsAlwaysThrow(Map.of());
        for (Map<Integer,Integer> map : Arrays.asList(
                Map.<Integer,Integer>of(),
                Map.of(1, 101),
                Map.of(1, 101, 2, 202),
                Map.of(1, 101, 2, 202, 3, 303),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505, 6, 606),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505, 6, 606, 7, 707),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505, 6, 606, 7, 707, 8, 808),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505, 6, 606, 7, 707, 8, 808, 9, 909),
                Map.of(1, 101, 2, 202, 3, 303, 4, 404, 5, 505, 6, 606, 7, 707, 8, 808, 9, 909, 10, 1010),
                Map.ofEntries(ea))) {
            testMap(map);
            testImmutableMap(map);
            testMapMutatorsAlwaysThrow(map);
        }

        Map<Integer,Integer> mapCopy = Map.copyOf(new HashMap<>(Map.of(1, 101, 2, 202, 3, 303)));
        testMap(mapCopy);
        testImmutableMap(mapCopy);
        testMapMutatorsAlwaysThrow(mapCopy);

        Map<Integer,Integer> mapCollected1 =
            Stream.of(1, 2, 3)
                  .collect(Collectors.toUnmodifiableMap(i -> i, i -> 101 * i));
        equal(mapCollected1, Map.of(1, 101, 2, 202, 3, 303));
        testMap(mapCollected1);
        testImmutableMap(mapCollected1);
        testMapMutatorsAlwaysThrow(mapCollected1);

        try {
            Stream.of(1, 1, 2, 3, 2, 3)
                  .collect(Collectors.toUnmodifiableMap(i -> i, i -> 101 * i));
            fail("duplicates should have thrown an exception");
        } catch (IllegalStateException ise) {
            pass();
        }

        Map<Integer,Integer> mapCollected2 =
            Stream.of(1, 1, 2, 3, 2, 3)
                  .collect(Collectors.toUnmodifiableMap(i -> i, i -> 101 * i, Integer::sum));
        equal(mapCollected2, Map.of(1, 202, 2, 404, 3, 606));
        testMap(mapCollected2);
        testImmutableMap(mapCollected2);
        testMapMutatorsAlwaysThrow(mapCollected2);
    }

    private static void checkContainsSelf(Collection<Integer> c) {
        check(c.containsAll(c));
        check(c.containsAll(Arrays.asList(c.toArray())));
        check(c.containsAll(Arrays.asList(c.toArray(new Integer[0]))));
    }

    private static void checkContainsEmpty(Collection<Integer> c) {
        check(c.containsAll(new ArrayList<Integer>()));
    }

    private static void checkUnique(Set<Integer> s) {
        for (Integer i : s) {
            int count = 0;
            for (Integer j : s) {
                if (Objects.equals(i,j))
                    ++count;
            }
            check(count == 1);
        }
    }

    private static <T> void testEmptyCollection(Collection<T> c) {
        check(c.isEmpty());
        equal(c.size(), 0);
        equal(c.toString(),"[]");
        equal(c.toArray().length, 0);
        equal(c.toArray(new Object[0]).length, 0);
        equal(c.toArray(Object[]::new).length, 0);
        check(c.toArray(new Object[]{42})[0] == null);

        Object[] a = new Object[1]; a[0] = Boolean.TRUE;
        equal(c.toArray(a), a);
        equal(a[0], null);
        testEmptyIterator(c.iterator());
    }

    static <T> void testEmptyIterator(final Iterator<T> it) {
        if (rnd.nextBoolean())
            check(! it.hasNext());

        THROWS(NoSuchElementException.class, () -> it.next());

        try { it.remove(); }
        catch (IllegalStateException ignored) { pass(); }
        catch (UnsupportedOperationException ignored) { pass(); }
        catch (Throwable t) { unexpected(t); }

        if (rnd.nextBoolean())
            check(! it.hasNext());
    }

    private static void testEmptyList(List<?> c) {
        testEmptyCollection(c);
        equal(c.hashCode(), 1);
        equal2(c, Collections.<Integer>emptyList());
    }

    private static <T> void testEmptySet(Set<T> c) {
        testEmptyCollection(c);
        equal(c.hashCode(), 0);
        equal2(c, Collections.<Integer>emptySet());
        if (c instanceof NavigableSet<?>)
            testEmptyIterator(((NavigableSet<T>)c).descendingIterator());
    }

    private static void testImmutableCollection(final Collection<Integer> c) {
        THROWS(UnsupportedOperationException.class,
               () -> c.add(99),
               () -> c.addAll(singleton(99)));
        if (! c.isEmpty()) {
            final Integer first = c.iterator().next();
            THROWS(UnsupportedOperationException.class,
                   () -> c.clear(),
                   () -> c.remove(first),
                   () -> c.removeAll(singleton(first)),
                   () -> c.retainAll(emptyList()));
        }
    }

    private static void testImmutableSet(final Set<Integer> c) {
        testImmutableCollection(c);
    }

    private static void testImmutableList(final List<Integer> c) {
        testList(c);
        testImmutableCollection(c);
        THROWS(UnsupportedOperationException.class,
               () -> c.set(0,42),
               () -> c.add(0,42),
               () -> c.addAll(0,singleton(86)));
        if (! c.isEmpty())
            THROWS(UnsupportedOperationException.class,
                   () -> { Iterator<Integer> it = c.iterator();
                           it.next();
                           it.remove(); },
                   () -> { ListIterator<Integer> it = c.listIterator();
                           it.next();
                           it.remove(); });
    }

    /**
     * Test that calling a mutator always throws UOE, even if the mutator
     * wouldn't actually do anything, given its arguments.
     *
     * @param c the collection instance to test
     */
    private static void testCollMutatorsAlwaysThrow(Collection<Integer> c) {
        THROWS(UnsupportedOperationException.class,
                () -> c.addAll(Collections.emptyList()),
                () -> c.remove(ABSENT_VALUE),
                () -> c.removeAll(Collections.emptyList()),
                () -> c.removeIf(x -> false),
                () -> c.retainAll(c));
    }

    /**
     * Test that calling a mutator always throws UOE, even if the mutator
     * wouldn't actually do anything on an empty collection.
     *
     * @param c the collection instance to test, must be empty
     */
    private static void testEmptyCollMutatorsAlwaysThrow(Collection<Integer> c) {
        if (! c.isEmpty()) {
            fail("collection is not empty");
        }
        THROWS(UnsupportedOperationException.class,
                () -> c.clear());
    }

    /**
     * As above, for a list.
     *
     * @param c the list instance to test
     */
    private static void testListMutatorsAlwaysThrow(List<Integer> c) {
        testCollMutatorsAlwaysThrow(c);
        THROWS(UnsupportedOperationException.class,
                () -> c.addAll(0, Collections.emptyList()));
    }

    /**
     * As above, for an empty list.
     *
     * @param c the list instance to test, must be empty
     */
    private static void testEmptyListMutatorsAlwaysThrow(List<Integer> c) {
        if (! c.isEmpty()) {
            fail("list is not empty");
        }
        testEmptyCollMutatorsAlwaysThrow(c);
        THROWS(UnsupportedOperationException.class,
                () -> c.replaceAll(x -> x),
                () -> c.sort(null));
    }

    /**
     * As above, for a map.
     *
     * @param m the map instance to test
     */
    private static void testMapMutatorsAlwaysThrow(Map<Integer,Integer> m) {
        THROWS(UnsupportedOperationException.class,
                () -> m.compute(ABSENT_VALUE, (k, v) -> null),
                () -> m.computeIfAbsent(ABSENT_VALUE, k -> null),
                () -> m.computeIfPresent(ABSENT_VALUE, (k, v) -> null),
                () -> m.merge(ABSENT_VALUE, 0, (k, v) -> null),
                () -> m.putAll(Collections.emptyMap()),
                () -> m.remove(ABSENT_VALUE),
                () -> m.remove(ABSENT_VALUE, 0),
                () -> m.replace(ABSENT_VALUE, 0),
                () -> m.replace(ABSENT_VALUE, 0, 1));
    }

    /**
     * As above, for an empty map.
     *
     * @param map the map instance to test, must be empty
     */
    private static void testEmptyMapMutatorsAlwaysThrow(Map<Integer,Integer> m) {
        if (! m.isEmpty()) {
            fail("map is not empty");
        }
        THROWS(UnsupportedOperationException.class,
                () -> m.clear(),
                () -> m.replaceAll((k, v) -> v));
    }

    private static void clear(Collection<Integer> c) {
        try { c.clear(); }
        catch (Throwable t) { unexpected(t); }
        testEmptyCollection(c);
    }

    private static <K,V> void testEmptyMap(final Map<K,V> m) {
        check(m.isEmpty());
        equal(m.size(), 0);
        equal(m.toString(),"{}");
        testEmptySet(m.keySet());
        testEmptySet(m.entrySet());
        testEmptyCollection(m.values());

        try { check(! m.containsValue(null)); }
        catch (NullPointerException ignored) { /* OK */ }
        try { check(! m.containsKey(null)); }
        catch (NullPointerException ignored) { /* OK */ }
        check(! m.containsValue(1));
        check(! m.containsKey(1));
    }

    private static void testImmutableMap(final Map<Integer,Integer> m) {
        THROWS(UnsupportedOperationException.class,
               () -> m.put(1,1),
               () -> m.putAll(singletonMap(1,1)));
        if (! m.isEmpty()) {
            final Integer first = m.keySet().iterator().next();
            THROWS(UnsupportedOperationException.class,
                   () -> m.remove(first),
                   () -> m.clear());
            final Map.Entry<Integer,Integer> me
                = m.entrySet().iterator().next();
            Integer key = me.getKey();
            Integer val = me.getValue();
            THROWS(UnsupportedOperationException.class,
                   () -> me.setValue(3));
            equal(key, me.getKey());
            equal(val, me.getValue());
        }
        testImmutableSet(m.keySet());
        testImmutableCollection(m.values());
        //testImmutableSet(m.entrySet());
    }

    private static void clear(Map<?,?> m) {
        try { m.clear(); }
        catch (Throwable t) { unexpected(t); }
        testEmptyMap(m);
    }

    private static void oneElement(Collection<Integer> c) {
        clear(c);
        try {
            check(c.add(-42));
            equal(c.toString(), "[-42]");
            if (c instanceof Set) check(! c.add(-42));
        } catch (Throwable t) { unexpected(t); }
        check(! c.isEmpty()); check(c.size() == 1);
    }

    private static boolean supportsAdd(Collection<Integer> c) {
        try { check(c.add(ABSENT_VALUE)); }
        catch (UnsupportedOperationException t) { return false; }
        catch (Throwable t) { unexpected(t); }

        try {
            check(c.contains(ABSENT_VALUE));
            check(c.remove(ABSENT_VALUE));
        } catch (Throwable t) { unexpected(t); }
        return true;
    }

    private static boolean supportsRemove(Collection<Integer> c) {
        try { check(! c.remove(ABSENT_VALUE)); }
        catch (UnsupportedOperationException t) { return false; }
        catch (Throwable t) { unexpected(t); }
        return true;
    }

    // 6260652: (coll) Arrays.asList(x).toArray().getClass()
    //          should be Object[].class
    // Fixed in jdk9, but not jdk8 ...
    static final boolean needToWorkAround6260652 =
        Arrays.asList("").toArray().getClass() != Object[].class;

    private static void checkFunctionalInvariants(Collection<Integer> c) {
        try {
            checkContainsSelf(c);
            checkContainsEmpty(c);
            check(c.size() != 0 ^ c.isEmpty());
            check(! c.contains(ABSENT_VALUE));

            {
                int size = 0;
                for (Integer i : c) size++;
                check(c.size() == size);
            }

            if (c instanceof Set) {
                checkUnique((Set<Integer>)c);
            }

            check(c.toArray().length == c.size());
            check(c.toArray().getClass() == Object[].class
                  ||
                  (needToWorkAround6260652 &&
                   c.getClass().getName().equals("java.util.Arrays$ArrayList")));
            for (int size : new int[]{0,1,c.size(), c.size()+1}) {
                Integer[] a = c.toArray(new Integer[size]);
                check((size > c.size()) || a.length == c.size());
                int i = 0; for (Integer j : c) check(a[i++] == j);
                check((size <= c.size()) || (a[c.size()] == null));
                check(a.getClass() == Integer[].class);
            }

            {
                Integer[] a = c.toArray(Integer[]::new);
                equal(c.size(), a.length);
                check(a.getClass() == Integer[].class);
                check(Arrays.equals(c.toArray(new Integer[0]), a));
            }

            check(c.equals(c));
            if (c instanceof Serializable) {
                //System.out.printf("Serializing %s%n", c.getClass().getName());
                try {
                    Object clone = serialClone(c);
                    equal(c instanceof Serializable,
                          clone instanceof Serializable);
                    equal(c instanceof RandomAccess,
                          clone instanceof RandomAccess);
                    if ((c instanceof List) || (c instanceof Set))
                        equal(c, clone);
                    else
                        equal(new HashSet<Integer>(c),
                              new HashSet<Integer>(serialClone(c)));
                } catch (Error xxx) {
                    if (! (xxx.getCause() instanceof NotSerializableException))
                        throw xxx;
                }
            }
        }
        catch (Throwable t) { unexpected(t); }
    }

    //----------------------------------------------------------------
    // If add(null) succeeds, contains(null) & remove(null) should succeed
    //----------------------------------------------------------------
    private static void testNullElement(Collection<Integer> c) {

        try {
            check(c.add(null));
            if (c.size() == 1)
                equal(c.toString(), "[null]");
            try {
                checkFunctionalInvariants(c);
                check(c.contains(null));
                check(c.remove(null));
            }
            catch (Throwable t) { unexpected(t); }
        }
        catch (NullPointerException e) { /* OK */ }
        catch (Throwable t) { unexpected(t); }
    }

    //----------------------------------------------------------------
    // If add("x") succeeds, contains("x") & remove("x") should succeed
    //----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static void testStringElement(Collection<Integer> c) {
        Collection x = (Collection)c; // Make type-unsafe
        try {
            check(x.add("x"));
            try {
                check(x.contains("x"));
                check(x.remove("x"));
            } catch (Throwable t) { unexpected(t); }
        }
        catch (ClassCastException e) { /* OK */ }
        catch (Throwable t) { unexpected(t); }
    }

    private static void testConcurrentCollection(Collection<Integer> c) {
        try {
            c.add(1);
            Iterator<Integer> it = c.iterator();
            check(it.hasNext());
            clear(c);
            check(it.next() instanceof Integer); // No CME
            check(c.isEmpty());
        }
        catch (Throwable t) { unexpected(t); }
    }

    private static void testQueue(Queue<Integer> q) {
        q.clear();
        for (int i = 0; i < 5; i++) {
            testQueueAddRemove(q, null);
            testQueueAddRemove(q, 537);
            q.add(i);
        }
        equal(q.size(), 5);
        checkFunctionalInvariants(q);
        q.poll();
        equal(q.size(), 4);
        checkFunctionalInvariants(q);
        if ((q instanceof LinkedBlockingQueue)   ||
            (q instanceof LinkedBlockingDeque)   ||
            (q instanceof ConcurrentLinkedDeque) ||
            (q instanceof ConcurrentLinkedQueue)) {
            testQueueIteratorRemove(q);
        }
    }

    private static void testQueueAddRemove(final Queue<Integer> q,
                                           final Integer e) {
        final List<Integer> originalContents = new ArrayList<>(q);
        final boolean isEmpty = q.isEmpty();
        final boolean isList = (q instanceof List);
        final List asList = isList ? (List) q : null;
        check(!q.contains(e));
        try {
            q.add(e);
        } catch (NullPointerException npe) {
            check(e == null);
            return;                     // Null elements not supported
        }
        check(q.contains(e));
        check(q.remove(e));
        check(!q.contains(e));
        equal(new ArrayList<Integer>(q), originalContents);

        if (q instanceof Deque<?>) {
            final Deque<Integer> deq = (Deque<Integer>) q;
            final List<Integer> singleton = Collections.singletonList(e);

            // insert, query, remove element at head
            if (isEmpty) {
                THROWS(NoSuchElementException.class,
                       () -> deq.getFirst(),
                       () -> deq.element(),
                       () -> deq.iterator().next());
                check(deq.peekFirst() == null);
                check(deq.peek() == null);
            } else {
                check(deq.getFirst() != e);
                check(deq.element() != e);
                check(deq.iterator().next() != e);
                check(deq.peekFirst() != e);
                check(deq.peek() != e);
            }
            check(!deq.contains(e));
            check(!deq.removeFirstOccurrence(e));
            check(!deq.removeLastOccurrence(e));
            if (isList) {
                check(asList.indexOf(e) == -1);
                check(asList.lastIndexOf(e) == -1);
            }
            switch (rnd.nextInt(isList ? 4 : 3)) {
            case 0: deq.addFirst(e); break;
            case 1: check(deq.offerFirst(e)); break;
            case 2: deq.push(e); break;
            case 3: asList.add(0, e); break;
            default: throw new AssertionError();
            }
            check(deq.peekFirst() == e);
            check(deq.getFirst() == e);
            check(deq.element() == e);
            check(deq.peek() == e);
            check(deq.iterator().next() == e);
            check(deq.contains(e));
            if (isList) {
                check(asList.get(0) == e);
                check(asList.indexOf(e) == 0);
                check(asList.lastIndexOf(e) == 0);
                check(asList.subList(0, 1).equals(singleton));
            }
            switch (rnd.nextInt(isList ? 11 : 9)) {
            case 0: check(deq.pollFirst() == e); break;
            case 1: check(deq.removeFirst() == e); break;
            case 2: check(deq.remove() == e); break;
            case 3: check(deq.pop() == e); break;
            case 4: check(deq.removeFirstOccurrence(e)); break;
            case 5: check(deq.removeLastOccurrence(e)); break;
            case 6: check(deq.remove(e)); break;
            case 7: check(deq.removeAll(singleton)); break;
            case 8: Iterator it = deq.iterator(); it.next(); it.remove(); break;
            case 9: asList.remove(0); break;
            case 10: asList.subList(0, 1).clear(); break;
            default: throw new AssertionError();
            }
            if (isEmpty) {
                THROWS(NoSuchElementException.class,
                       () -> deq.getFirst(),
                       () -> deq.element(),
                       () -> deq.iterator().next());
                check(deq.peekFirst() == null);
                check(deq.peek() == null);
            } else {
                check(deq.getFirst() != e);
                check(deq.element() != e);
                check(deq.iterator().next() != e);
                check(deq.peekFirst() != e);
                check(deq.peek() != e);
            }
            check(!deq.contains(e));
            check(!deq.removeFirstOccurrence(e));
            check(!deq.removeLastOccurrence(e));
            if (isList) {
                check(isEmpty || asList.get(0) != e);
                check(asList.indexOf(e) == -1);
                check(asList.lastIndexOf(e) == -1);
            }
            equal(new ArrayList<Integer>(deq), originalContents);

            // insert, query, remove element at tail
            if (isEmpty) {
                check(deq.peekLast() == null);
                THROWS(NoSuchElementException.class, () -> deq.getLast());
            } else {
                check(deq.peekLast() != e);
                check(deq.getLast() != e);
            }
            switch (rnd.nextInt(isList ? 6 : 4)) {
            case 0: deq.addLast(e); break;
            case 1: check(deq.offerLast(e)); break;
            case 2: check(deq.add(e)); break;
            case 3: deq.addAll(singleton); break;
            case 4: asList.addAll(deq.size(), singleton); break;
            case 5: asList.add(deq.size(), e); break;
            default: throw new AssertionError();
            }
            check(deq.peekLast() == e);
            check(deq.getLast() == e);
            check(deq.contains(e));
            if (isList) {
                ListIterator it = asList.listIterator(asList.size());
                check(it.previous() == e);
                check(asList.get(asList.size() - 1) == e);
                check(asList.indexOf(e) == asList.size() - 1);
                check(asList.lastIndexOf(e) == asList.size() - 1);
                int size = asList.size();
                check(asList.subList(size - 1, size).equals(singleton));
            }
            switch (rnd.nextInt(isList ? 8 : 6)) {
            case 0: check(deq.pollLast() == e); break;
            case 1: check(deq.removeLast() == e); break;
            case 2: check(deq.removeFirstOccurrence(e)); break;
            case 3: check(deq.removeLastOccurrence(e)); break;
            case 4: check(deq.remove(e)); break;
            case 5: check(deq.removeAll(singleton)); break;
            case 6: asList.remove(asList.size() - 1); break;
            case 7:
                ListIterator it = asList.listIterator(asList.size());
                it.previous();
                it.remove();
                break;
            default: throw new AssertionError();
            }
            if (isEmpty) {
                check(deq.peekLast() == null);
                THROWS(NoSuchElementException.class, () -> deq.getLast());
            } else {
                check(deq.peekLast() != e);
                check(deq.getLast() != e);
            }
            check(!deq.contains(e));
            equal(new ArrayList<Integer>(deq), originalContents);

            // Test operations on empty deque
            switch (rnd.nextInt(isList ? 4 : 2)) {
            case 0: deq.clear(); break;
            case 1:
                Iterator it = deq.iterator();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
                break;
            case 2: asList.subList(0, asList.size()).clear(); break;
            case 3:
                ListIterator lit = asList.listIterator(asList.size());
                while (lit.hasPrevious()) {
                    lit.previous();
                    lit.remove();
                }
                break;
            default: throw new AssertionError();
            }
            testEmptyCollection(deq);
            check(!deq.iterator().hasNext());
            if (isList) {
                check(!asList.listIterator().hasPrevious());
                THROWS(NoSuchElementException.class,
                       () -> asList.listIterator().previous());
            }
            THROWS(NoSuchElementException.class,
                   () -> deq.iterator().next(),
                   () -> deq.element(),
                   () -> deq.getFirst(),
                   () -> deq.getLast(),
                   () -> deq.pop(),
                   () -> deq.remove(),
                   () -> deq.removeFirst(),
                   () -> deq.removeLast());

            check(deq.poll() == null);
            check(deq.pollFirst() == null);
            check(deq.pollLast() == null);
            check(deq.peek() == null);
            check(deq.peekFirst() == null);
            check(deq.peekLast() == null);
            check(!deq.removeFirstOccurrence(e));
            check(!deq.removeLastOccurrence(e));

            check(deq.addAll(originalContents) == !isEmpty);
            equal(new ArrayList<Integer>(deq), originalContents);
            check(!deq.addAll(Collections.<Integer>emptyList()));
            equal(new ArrayList<Integer>(deq), originalContents);
        }
    }

    private static void testQueueIteratorRemove(Queue<Integer> q) {
        System.err.printf("testQueueIteratorRemove %s%n",
                          q.getClass().getSimpleName());
        q.clear();
        for (int i = 0; i < 5; i++)
            q.add(i);
        Iterator<Integer> it = q.iterator();
        check(it.hasNext());
        for (int i = 3; i >= 0; i--)
            q.remove(i);
        equal(it.next(), 0);
        equal(it.next(), 4);

        q.clear();
        for (int i = 0; i < 5; i++)
            q.add(i);
        it = q.iterator();
        equal(it.next(), 0);
        check(it.hasNext());
        for (int i = 1; i < 4; i++)
            q.remove(i);
        equal(it.next(), 1);
        equal(it.next(), 4);
    }

    // for any array of integer values, check that the result of lastIndexOf(1)
    // and indexOf(1) match assumptions for all types of List<Integer> we can
    // construct
    private static void testListIndexOf(final int index,
                                        final int lastIndex,
                                        final Integer ... values) {
        if (values.length == 0) {
            checkListIndexOf(emptyList(), index, lastIndex);
        } else if (values.length == 1) {
            checkListIndexOf(singletonList(values[0]), index, lastIndex);
            checkListIndexOf(nCopies(25, values[0]), index, lastIndex == 0 ? 24 : -1);
        }
        List<Integer> l = List.of(values);
        checkListIndexOf(l, index, lastIndex);
        checkListIndexOf(Arrays.asList(values), index, lastIndex);
        checkListIndexOf(new ArrayList(l), index, lastIndex);
        checkListIndexOf(new LinkedList(l), index, lastIndex);
        checkListIndexOf(new Vector(l), index, lastIndex);
        checkListIndexOf(new CopyOnWriteArrayList(l), index, lastIndex);
    }

    private static void checkListIndexOf(final List<Integer> list,
                                         final int index,
                                         final int lastIndex) {
        String msg = list.getClass().toString();
        equal(list.indexOf(1), index, msg);
        equal(list.lastIndexOf(1), lastIndex, msg);
        equal(list.subList(0, list.size()).indexOf(1), index, msg);
        equal(list.subList(0, list.size()).lastIndexOf(1), lastIndex, msg);
    }

    private static void testList(final List<Integer> l) {
        //----------------------------------------------------------------
        // 4802633: (coll) AbstractList.addAll(-1,emptyCollection)
        // doesn't throw IndexOutOfBoundsException
        //----------------------------------------------------------------
        try {
            l.addAll(-1, Collections.<Integer>emptyList());
            fail("Expected IndexOutOfBoundsException not thrown");
        }
        catch (UnsupportedOperationException ignored) {/* OK */}
        catch (IndexOutOfBoundsException ignored) {/* OK */}
        catch (Throwable t) { unexpected(t); }

//      equal(l instanceof Serializable,
//            l.subList(0,0) instanceof Serializable);
        if (l.subList(0,0) instanceof Serializable)
            check(l instanceof Serializable);

        equal(l instanceof RandomAccess,
              l.subList(0,0) instanceof RandomAccess);

        l.iterator();
        l.listIterator();
        l.listIterator(0);
        l.listIterator(l.size());
        THROWS(IndexOutOfBoundsException.class,
               () -> l.listIterator(-1),
               () -> l.listIterator(l.size() + 1));

        if (l instanceof AbstractList) {
            try {
                int size = l.size();
                AbstractList<Integer> abList = (AbstractList<Integer>) l;
                Method m = AbstractList.class.getDeclaredMethod("removeRange", new Class[] { int.class, int.class });
                m.setAccessible(true);
                m.invoke(abList, new Object[] { 0, 0 });
                m.invoke(abList, new Object[] { size, size });
                equal(size, l.size());
            }
            catch (UnsupportedOperationException ignored) {/* OK */}
            catch (Throwable t) { unexpected(t); }
        }

        int hashCode = 1;
        for (Integer i : l)
            hashCode = 31 * hashCode + (i == null ? 0 : i.hashCode());
        check(l.hashCode() == hashCode);

        var t = new ArrayList<>(l);
        check(t.equals(l));
        check(l.equals(t));
    }

    private static void testCollection(Collection<Integer> c) {
        try { testCollection1(c); }
        catch (Throwable t) { unexpected(t); }
    }

    private static void testCollection1(Collection<Integer> c) {

        System.out.println("\n==> " + c.getClass().getName());

        checkFunctionalInvariants(c);

        if (! supportsAdd(c)) return;
        //System.out.println("add() supported");

        if (c instanceof NavigableSet) {
            System.out.println("NavigableSet tests...");

            NavigableSet<Integer> ns = (NavigableSet<Integer>)c;
            testNavigableSet(ns);
            testNavigableSet(ns.headSet(6, false));
            testNavigableSet(ns.headSet(5, true));
            testNavigableSet(ns.tailSet(0, false));
            testNavigableSet(ns.tailSet(1, true));
            testNavigableSet(ns.subSet(0, false, 5, true));
            testNavigableSet(ns.subSet(1, true, 6, false));
        }

        if (c instanceof Queue)
            testQueue((Queue<Integer>)c);

        if (c instanceof List)
            testList((List<Integer>)c);

        if (c instanceof Set) {
            int hashCode = 0;
            for (Integer i : c)
                hashCode = hashCode + (i == null ? 0 : i.hashCode());
            check(c.hashCode() == hashCode);
        }

        check(supportsRemove(c));

        try {
            oneElement(c);
            checkFunctionalInvariants(c);
        }
        catch (Throwable t) { unexpected(t); }

        clear(c);      testNullElement(c);
        oneElement(c); testNullElement(c);

        clear(c);      testStringElement(c);
        oneElement(c); testStringElement(c);

        if (c.getClass().getName().matches(".*concurrent.*"))
            testConcurrentCollection(c);

        //----------------------------------------------------------------
        // The "all" operations should throw NPE when passed null
        //----------------------------------------------------------------
        {
            clear(c);
            try {
                c.removeAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }

            oneElement(c);
            try {
                c.removeAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }

            clear(c);
            try {
                c.retainAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }

            oneElement(c);
            try {
                c.retainAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }

            oneElement(c);
            try {
                c.addAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }

            oneElement(c);
            try {
                c.containsAll(null);
                fail("Expected NullPointerException");
            }
            catch (NullPointerException e) { pass(); }
            catch (Throwable t) { unexpected(t); }
        }
    }

    //----------------------------------------------------------------
    // Map
    //----------------------------------------------------------------
    private static void checkFunctionalInvariants(Map<Integer,Integer> m) {
        check(m.keySet().size() == m.entrySet().size());
        check(m.keySet().size() == m.size());
        checkFunctionalInvariants(m.keySet());
        checkFunctionalInvariants(m.values());
        check(m.size() != 0 ^ m.isEmpty());
        check(! m.containsKey(ABSENT_VALUE));

        if (m instanceof Serializable) {
            //System.out.printf("Serializing %s%n", m.getClass().getName());
            try {
                Object clone = serialClone(m);
                equal(m instanceof Serializable,
                      clone instanceof Serializable);
                equal(m, clone);
            } catch (Error xxx) {
                if (! (xxx.getCause() instanceof NotSerializableException))
                    throw xxx;
            }
        }
    }

    private static void testMap(Map<Integer,Integer> m) {
        System.out.println("\n==> " + m.getClass().getName());

        int hashCode = 0;
        for (var e : m.entrySet()) {
            int entryHash = (e.getKey() == null ? 0 : e.getKey().hashCode()) ^
                            (e.getValue() == null ? 0 : e.getValue().hashCode());
            check(e.hashCode() == entryHash);
            hashCode += entryHash;
        }
        check(m.hashCode() == hashCode);

        if (m instanceof ConcurrentMap)
            testConcurrentMap((ConcurrentMap<Integer,Integer>) m);

        if (m instanceof NavigableMap) {
            System.out.println("NavigableMap tests...");

            NavigableMap<Integer,Integer> nm =
                (NavigableMap<Integer,Integer>) m;
            testNavigableMapRemovers(nm);
            testNavigableMap(nm);
            testNavigableMap(nm.headMap(6, false));
            testNavigableMap(nm.headMap(5, true));
            testNavigableMap(nm.tailMap(0, false));
            testNavigableMap(nm.tailMap(1, true));
            testNavigableMap(nm.subMap(1, true, 6, false));
            testNavigableMap(nm.subMap(0, false, 5, true));
        }

        checkFunctionalInvariants(m);

        if (supportsClear(m)) {
            try { clear(m); }
            catch (Throwable t) { unexpected(t); }
        }

        if (supportsPut(m)) {
            try {
                check(m.put(3333, 77777) == null);
                check(m.put(9134, 74982) == null);
                check(m.get(9134) == 74982);
                check(m.put(9134, 1382) == 74982);
                check(m.get(9134) == 1382);
                check(m.size() == 2);
                checkFunctionalInvariants(m);
                checkNPEConsistency(m);
            }
            catch (Throwable t) { unexpected(t); }
        }
    }

    private static boolean supportsPut(Map<Integer,Integer> m) {
        // We're asking for .equals(...) semantics
        if (m instanceof IdentityHashMap) return false;

        try { check(m.put(ABSENT_VALUE,12735) == null); }
        catch (UnsupportedOperationException t) { return false; }
        catch (Throwable t) { unexpected(t); }

        try {
            check(m.containsKey(ABSENT_VALUE));
            check(m.remove(ABSENT_VALUE) != null);
        } catch (Throwable t) { unexpected(t); }
        return true;
    }

    private static boolean supportsClear(Map<?,?> m) {
        try { m.clear(); }
        catch (UnsupportedOperationException t) { return false; }
        catch (Throwable t) { unexpected(t); }
        return true;
    }

    //----------------------------------------------------------------
    // ConcurrentMap
    //----------------------------------------------------------------
    private static void testConcurrentMap(ConcurrentMap<Integer,Integer> m) {
        System.out.println("ConcurrentMap tests...");

        try {
            clear(m);

            check(m.putIfAbsent(18357,7346) == null);
            check(m.containsKey(18357));
            check(m.putIfAbsent(18357,8263) == 7346);
            try { m.putIfAbsent(18357,null); fail("NPE"); }
            catch (NullPointerException t) { }
            check(m.containsKey(18357));

            check(! m.replace(18357,8888,7777));
            check(m.containsKey(18357));
            try { m.replace(18357,null,7777); fail("NPE"); }
            catch (NullPointerException t) { }
            check(m.containsKey(18357));
            check(m.get(18357) == 7346);
            check(m.replace(18357,7346,5555));
            check(m.replace(18357,5555,7346));
            check(m.get(18357) == 7346);

            check(m.replace(92347,7834) == null);
            try { m.replace(18357,null); fail("NPE"); }
            catch (NullPointerException t) { }
            check(m.replace(18357,7346) == 7346);
            check(m.replace(18357,5555) == 7346);
            check(m.get(18357) == 5555);
            check(m.replace(18357,7346) == 5555);
            check(m.get(18357) == 7346);

            check(! m.remove(18357,9999));
            check(m.get(18357) == 7346);
            check(m.containsKey(18357));
            check(! m.remove(18357,null)); // 6272521
            check(m.get(18357) == 7346);
            check(m.remove(18357,7346));
            check(m.get(18357) == null);
            check(! m.containsKey(18357));
            check(m.isEmpty());

            m.putIfAbsent(1,2);
            check(m.size() == 1);
            check(! m.remove(1,null));
            check(! m.remove(1,null));
            check(! m.remove(1,1));
            check(m.remove(1,2));
            check(m.isEmpty());

            testEmptyMap(m);
        }
        catch (Throwable t) { unexpected(t); }
    }

    private static void throwsConsistently(Class<? extends Throwable> k,
                                           Iterable<Fun> fs) {
        List<Class<? extends Throwable>> threw = new ArrayList<>();
        for (Fun f : fs)
            try { f.f(); threw.add(null); }
            catch (Throwable t) {
                check(k.isAssignableFrom(t.getClass()));
                threw.add(t.getClass());
            }
        if (new HashSet<Object>(threw).size() != 1)
            fail(threw.toString());
    }

    private static <T> void checkNPEConsistency(final Map<T,Integer> m) {
        m.clear();
        final ConcurrentMap<T,Integer> cm = (m instanceof ConcurrentMap)
            ? (ConcurrentMap<T,Integer>) m
            : null;
        List<Fun> fs = new ArrayList<>();
        fs.add(() -> check(! m.containsKey(null)));
        fs.add(() -> equal(m.remove(null), null));
        fs.add(() -> equal(m.get(null), null));
        if (cm != null)
            fs.add(() -> check(! cm.remove(null,null)));
        throwsConsistently(NullPointerException.class, fs);

        fs.clear();
        final Map<T,Integer> sm = singletonMap(null,1);
        fs.add(() -> { equal(m.put(null,1), null); m.clear();});
        fs.add(() -> { m.putAll(sm); m.clear();});
        if (cm != null) {
            fs.add(() -> check(! cm.remove(null,null)));
            fs.add(() -> equal(cm.putIfAbsent(null,1), 1));
            fs.add(() -> equal(cm.replace(null,1), null));
            fs.add(() -> equal(cm.replace(null,1, 1), 1));
        }
        throwsConsistently(NullPointerException.class, fs);
    }

    //----------------------------------------------------------------
    // NavigableMap
    //----------------------------------------------------------------
    private static void
        checkNavigableMapKeys(NavigableMap<Integer,Integer> m,
                              Integer i,
                              Integer lower,
                              Integer floor,
                              Integer ceiling,
                              Integer higher) {
        equal(m.lowerKey(i),   lower);
        equal(m.floorKey(i),   floor);
        equal(m.ceilingKey(i), ceiling);
        equal(m.higherKey(i),  higher);
    }

    private static void
        checkNavigableSetKeys(NavigableSet<Integer> m,
                              Integer i,
                              Integer lower,
                              Integer floor,
                              Integer ceiling,
                              Integer higher) {
        equal(m.lower(i),   lower);
        equal(m.floor(i),   floor);
        equal(m.ceiling(i), ceiling);
        equal(m.higher(i),  higher);
    }

    static final Random rnd = new Random();
    static void equalNext(final Iterator<?> it, Object expected) {
        if (rnd.nextBoolean())
            check(it.hasNext());
        equal(it.next(), expected);
    }

    static void equalMaps(Map m1, Map m2) {
        equal(m1, m2);
        equal(m2, m1);
        equal(m1.size(), m2.size());
        equal(m1.isEmpty(), m2.isEmpty());
        equal(m1.toString(), m2.toString());
        check(Arrays.equals(m1.entrySet().toArray(), m2.entrySet().toArray()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void testNavigableMapRemovers(NavigableMap m)
    {
        final Map emptyMap = new HashMap();

        final Map singletonMap = new HashMap();
        singletonMap.put(1, 2);

        abstract class NavigableMapView {
            abstract NavigableMap view(NavigableMap m);
        }

        NavigableMapView[] views = {
            new NavigableMapView() { NavigableMap view(NavigableMap m) {
                return m; }},
            new NavigableMapView() { NavigableMap view(NavigableMap m) {
                return m.headMap(99, true); }},
            new NavigableMapView() { NavigableMap view(NavigableMap m) {
                return m.tailMap(-99, false); }},
            new NavigableMapView() { NavigableMap view(NavigableMap m) {
                return m.subMap(-99, true, 99, false); }},
        };

        abstract class Remover {
            abstract void remove(NavigableMap m, Object k, Object v);
        }

        Remover[] removers = {
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.remove(k), v); }},

            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.descendingMap().remove(k), v); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.descendingMap().headMap(-86, false).remove(k), v); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.descendingMap().tailMap(86, true).remove(k), v); }},

            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.headMap(86, true).remove(k), v); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.tailMap(-86, true).remove(k), v); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                equal(m.subMap(-86, false, 86, true).remove(k), v); }},

            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.keySet().remove(k)); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.navigableKeySet().remove(k)); }},

            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.navigableKeySet().headSet(86, true).remove(k)); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.navigableKeySet().tailSet(-86, false).remove(k)); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.navigableKeySet().subSet(-86, true, 86, false)
                      .remove(k)); }},

            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.descendingKeySet().headSet(-86, false).remove(k)); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.descendingKeySet().tailSet(86, true).remove(k)); }},
            new Remover() { void remove(NavigableMap m, Object k, Object v) {
                check(m.descendingKeySet().subSet(86, true, -86, false)
                      .remove(k)); }},
        };

        for (NavigableMapView view : views) {
            for (Remover remover : removers) {
                try {
                    m.clear();
                    equalMaps(m, emptyMap);
                    equal(m.put(1, 2), null);
                    equalMaps(m, singletonMap);
                    NavigableMap v = view.view(m);
                    remover.remove(v, 1, 2);
                    equalMaps(m, emptyMap);
                } catch (Throwable t) { unexpected(t); }
            }
        }
    }

    private static void testNavigableMap(NavigableMap<Integer,Integer> m)
    {
        clear(m);
        checkNavigableMapKeys(m, 1, null, null, null, null);

        equal(m.put(1, 2), null);
        equal(m.put(3, 4), null);
        equal(m.put(5, 9), null);

        equal(m.put(1, 2), 2);
        equal(m.put(3, 4), 4);
        equal(m.put(5, 6), 9);

        checkNavigableMapKeys(m, 0, null, null,    1,    1);
        checkNavigableMapKeys(m, 1, null,    1,    1,    3);
        checkNavigableMapKeys(m, 2,    1,    1,    3,    3);
        checkNavigableMapKeys(m, 3,    1,    3,    3,    5);
        checkNavigableMapKeys(m, 5,    3,    5,    5, null);
        checkNavigableMapKeys(m, 6,    5,    5, null, null);

        for (final Iterator<Integer> it :
                 (Iterator<Integer>[])
                 new Iterator<?>[] {
                     m.descendingKeySet().iterator(),
                     m.navigableKeySet().descendingIterator()}) {
            equalNext(it, 5);
            equalNext(it, 3);
            equalNext(it, 1);
            check(! it.hasNext());
            THROWS(NoSuchElementException.class, () -> it.next());
        }

        {
            final Iterator<Map.Entry<Integer,Integer>> it
                = m.descendingMap().entrySet().iterator();
            check(it.hasNext()); equal(it.next().getKey(), 5);
            check(it.hasNext()); equal(it.next().getKey(), 3);
            check(it.hasNext()); equal(it.next().getKey(), 1);
            check(! it.hasNext());
            THROWS(NoSuchElementException.class, () -> it.next());
        }

        prepMapForDescItrTests(m);
        checkDescItrRmFirst(m.keySet(), m.navigableKeySet().descendingIterator());
        prepMapForDescItrTests(m);
        checkDescItrRmMid(m.keySet(), m.navigableKeySet().descendingIterator());
        prepMapForDescItrTests(m);
        checkDescItrRmLast(m.keySet(), m.navigableKeySet().descendingIterator());

        prepMapForDescItrTests(m);
        checkDescItrRmFirst(m.keySet(), m.descendingMap().keySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmMid(m.keySet(), m.descendingMap().keySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmLast(m.keySet(), m.descendingMap().keySet().iterator());

        prepMapForDescItrTests(m);
        checkDescItrRmFirst(m.keySet(), m.descendingKeySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmMid(m.keySet(), m.descendingKeySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmLast(m.keySet(), m.descendingKeySet().iterator());

        prepMapForDescItrTests(m);
        checkDescItrRmFirst(m.values(), m.descendingMap().values().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmMid(m.values(), m.descendingMap().values().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmLast(m.values(), m.descendingMap().values().iterator());

        prepMapForDescItrTests(m);
        checkDescItrRmFirst((Collection)m.entrySet(),
                            m.descendingMap().entrySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmMid((Collection)m.entrySet(),
                          m.descendingMap().entrySet().iterator());
        prepMapForDescItrTests(m);
        checkDescItrRmLast((Collection)m.entrySet(),
                           m.descendingMap().entrySet().iterator());
    }

    private static void testNavigableSet(NavigableSet<Integer> s) {
        clear(s);
        checkNavigableSetKeys(s, 1, null, null, null, null);

        check(s.add(1));
        check(s.add(3));
        check(s.add(5));

        check(! s.add(1));
        check(! s.add(3));
        check(! s.add(5));

        checkNavigableSetKeys(s, 0, null, null,    1,    1);
        checkNavigableSetKeys(s, 1, null,    1,    1,    3);
        checkNavigableSetKeys(s, 2,    1,    1,    3,    3);
        checkNavigableSetKeys(s, 3,    1,    3,    3,    5);
        checkNavigableSetKeys(s, 5,    3,    5,    5, null);
        checkNavigableSetKeys(s, 6,    5,    5, null, null);

        for (final Iterator<Integer> it :
                 (Iterator<Integer>[])
                 new Iterator<?>[] {
                     s.descendingIterator(),
                     s.descendingSet().iterator()}) {
            equalNext(it, 5);
            equalNext(it, 3);
            equalNext(it, 1);
            check(! it.hasNext());
            THROWS(NoSuchElementException.class, () -> it.next());
        }

        prepSetForDescItrTests(s);
        checkDescItrRmFirst(s, s.descendingIterator());
        prepSetForDescItrTests(s);
        checkDescItrRmMid(s, s.descendingIterator());
        prepSetForDescItrTests(s);
        checkDescItrRmLast(s, s.descendingIterator());

        prepSetForDescItrTests(s);
        checkDescItrRmFirst(s, s.descendingSet().iterator());
        prepSetForDescItrTests(s);
        checkDescItrRmMid(s, s.descendingSet().iterator());
        prepSetForDescItrTests(s);
        checkDescItrRmLast(s, s.descendingSet().iterator());
    }

    private static void prepSetForDescItrTests(Set s) {
        clear(s);
        check(s.add(1));
        check(s.add(3));
        check(s.add(5));
    }

    private static void prepMapForDescItrTests(Map m) {
        clear(m);
        equal(m.put(1, 2), null);
        equal(m.put(3, 4), null);
        equal(m.put(5, 9), null);
    }

    //--------------------------------------------------------------------
    // Check behavior of descending iterator when first element is removed
    //--------------------------------------------------------------------
    private static <T> void checkDescItrRmFirst(Collection<T> ascColl,
                                                Iterator<T> descItr) {
        T[] expected = (T[]) ascColl.toArray();
        int idx = expected.length -1;

        equalNext(descItr, expected[idx--]);
        descItr.remove();
        while (idx >= 0 && descItr.hasNext()) {
            equalNext(descItr, expected[idx--]);
        }
        equal(descItr.hasNext(), false);
        equal(idx, -1);
    }

    //-----------------------------------------------------------------------
    // Check behavior of descending iterator when a middle element is removed
    //-----------------------------------------------------------------------
    private static <T> void checkDescItrRmMid(Collection<T> ascColl,
                                              Iterator<T> descItr) {
        T[] expected = (T[]) ascColl.toArray();
        int idx = expected.length -1;

        while (idx >= expected.length / 2) {
            equalNext(descItr, expected[idx--]);
        }
        descItr.remove();
        while (idx >= 0 && descItr.hasNext()) {
            equalNext(descItr, expected[idx--]);
        }
        equal(descItr.hasNext(), false);
        equal(idx, -1);
    }

    //-----------------------------------------------------------------------
    // Check behavior of descending iterator when the last element is removed
    //-----------------------------------------------------------------------
    private static <T> void checkDescItrRmLast(Collection<T> ascColl,
                                               Iterator<T> descItr) {
        T[] expected = (T[]) ascColl.toArray();
        int idx = expected.length -1;

        while (idx >= 0 && descItr.hasNext()) {
            equalNext(descItr, expected[idx--]);
        }
        equal(idx, -1);
        equal(descItr.hasNext(), false);
        descItr.remove();
        equal(ascColl.contains(expected[0]), false);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() { passed++; }
    static void fail() { failed++; Thread.dumpStack(); }
    static void fail(String msg) { System.out.println(msg); fail(); }
    static void unexpected(Throwable t) { failed++; t.printStackTrace(); }
    static void check(boolean cond) { if (cond) pass(); else fail(); }
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y); fail();}}
    static void equal(Object x, Object y, String msg) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y + " : " + msg); fail();}}
    static void equal2(Object x, Object y) {equal(x, y); equal(y, x);}
    public static void main(String[] args) throws Throwable {
        try { realMain(args); } catch (Throwable t) { unexpected(t); }

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
    interface Fun {void f() throws Throwable;}
    private static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
    static byte[] serializedForm(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) { throw new Error(e); }}
    static Object readObject(byte[] bytes)
        throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(bytes);
        return new ObjectInputStream(is).readObject();}
    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try { return (T) readObject(serializedForm(obj)); }
        catch (Exception e) { throw new Error(e); }}
    private static class NewAbstractCollection<E> extends AbstractCollection<E> {
        ArrayList<E> list = new ArrayList<>();
        public boolean remove(Object obj) {
            return list.remove(obj);
        }
        public boolean add(E e) {
            return list.add(e);
        }
        public Iterator<E> iterator() {
            return list.iterator();
        }
        public int size() {
            return list.size();
        }
    }
    private static class NewAbstractSet<E> extends AbstractSet<E> {
        HashSet<E> set = new HashSet<>();
        public boolean remove(Object obj) {
            return set.remove(obj);
        }
        public boolean add(E e) {
            return set.add(e);
        }
        public Iterator<E> iterator() {
            return set.iterator();
        }
        public int size() {
            return set.size();
        }
    }

}
