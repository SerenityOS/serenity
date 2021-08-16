/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.testng.annotations.DataProvider;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class MapWithCollisionsProviders {

    private static final int TEST_SIZE
            = Boolean.valueOf(System.getProperty("test.map.collisions.shortrun"))
            ? 2500
            : 5000;

    private static final IntKey EXTRA_INTKEY_VAL
            = new IntKey(TEST_SIZE, Integer.MAX_VALUE);

    private static final String EXTRA_STRING_VAL = "Extra Value";

    public static final class IntKey implements Comparable<IntKey> {

        private final int value;
        private final int hashmask; //yes duplication

        IntKey(int value, int hashmask) {
            this.value = value;
            this.hashmask = hashmask;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IntKey) {
                IntKey other = (IntKey) obj;

                return other.value == value;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return value % hashmask;
        }

        @Override
        public int compareTo(IntKey o) {
            return value - o.value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        public int getValue() {
            return value;
        }
    }

    private static Object[] createUniqueObjectKeys() {
        IntKey UNIQUE_OBJECTS[] = new IntKey[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            UNIQUE_OBJECTS[i] = new IntKey(i, Integer.MAX_VALUE);
        }
        return UNIQUE_OBJECTS;
    }

    private static Object[] createUniqueStringKeys() {
        String UNIQUE_STRINGS[] = new String[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            UNIQUE_STRINGS[i] = unhash(i);
        }
        return UNIQUE_STRINGS;
    }

    private static Object[] createCollidingObjectKeys() {
        IntKey COLLIDING_OBJECTS[] = new IntKey[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            COLLIDING_OBJECTS[i] = new IntKey(i, 10);
        }
        return COLLIDING_OBJECTS;
    }

    private static Object[] createCollidingStringKeys() {
        String COLLIDING_STRINGS[] = new String[TEST_SIZE];
        String UNIQUE_STRINGS[] = new String[TEST_SIZE];
        for (int i = 0; i < TEST_SIZE; i++) {
            UNIQUE_STRINGS[i] = unhash(i);
            COLLIDING_STRINGS[i] = (0 == i % 2)
                    ? UNIQUE_STRINGS[i / 2]
                    : "\u0000\u0000\u0000\u0000\u0000" + COLLIDING_STRINGS[i - 1];
        }
        return COLLIDING_STRINGS;
    }

    /**
     * Returns a string with a hash equal to the argument.
     *
     * @return string with a hash equal to the argument.
     */
    private static String unhash(int target) {
        StringBuilder answer = new StringBuilder();
        if (target < 0) {
            // String with hash of Integer.MIN_VALUE, 0x80000000
            answer.append("\\u0915\\u0009\\u001e\\u000c\\u0002");

            if (target == Integer.MIN_VALUE) {
                return answer.toString();
            }
            // Find target without sign bit set
            target = target & Integer.MAX_VALUE;
        }

        unhash0(answer, target);
        return answer.toString();
    }

    private static void unhash0(StringBuilder partial, int target) {
        int div = target / 31;
        int rem = target % 31;

        if (div <= Character.MAX_VALUE) {
            if (div != 0) {
                partial.append((char) div);
            }
            partial.append((char) rem);
        } else {
            unhash0(partial, div);
            partial.append((char) rem);
        }
    }

    private static <T> Map<T, T> fillMap(Map<T, T> m, T[] keys) {
        for (T k : keys) {
            m.put(k, k);
            assertTrue(m.containsKey(k));
            assertTrue(m.containsValue(k));
        }
        assertEquals(m.size(), keys.length);
        return m;
    }

    private static <T> Supplier<Map<T, T>> createMap(Map<T, T> m, T[] keys) {
        return () -> fillMap(m, keys);
    }

    private static <T> Object[] createCase(String desc, Map<T, T> m, T[] keys, T val) {
        return new Object[]{desc, createMap(m, keys), val};
    }

    private static <T> Collection<Object[]> makeMapsMoreTypes(String desc,
                                                              T[] keys,
                                                              T val) {
        Collection<Object[]> cases = new ArrayList<>();
        cases.add(createCase("Hashtable with " + desc,
                             new Hashtable<>(), keys, val));
        cases.add(createCase("IdentityHashMap with " + desc,
                             new IdentityHashMap<>(), keys, val));
        cases.add(createCase("TreeMap with " + desc,
                             new TreeMap<>(), keys, val));
        cases.add(createCase("Descending TreeMap with " + desc,
                             new TreeMap<>().descendingMap(), keys, val));
        cases.add(createCase("WeakHashMap with " + desc,
                             new WeakHashMap<>(), keys, val));
        cases.add(createCase("ConcurrentHashMap with " + desc,
                             new ConcurrentHashMap<>(), keys, val));
        cases.add(createCase("ConcurrentSkipListMap with " + desc,
                             new ConcurrentSkipListMap<>(), keys, val));
        return cases;
    }

    private static <T> Collection<Object[]> makeMapsHashMap(String desc,
                                                            T[] keys,
                                                            T val) {
        Collection<Object[]> cases = new ArrayList<>();
        cases.add(createCase("HashMap with " + desc,
                             new HashMap<>(), keys, val));
        cases.add(createCase("LinkedHashMap with " + desc,
                             new LinkedHashMap<>(), keys, val));
        return cases;
    }

    private static <T> Collection<Object[]> makeMaps(String desc, T[] keys, T val) {
        Collection<Object[]> cases = new ArrayList<>();
        cases.addAll(makeMapsHashMap(desc, keys, val));
        cases.addAll(makeMapsMoreTypes(desc, keys, val));
        return cases;
    }

    private static <T> Collection<Object[]> makeObjectsCases(String desc, T[] keys) {
        return makeMaps(desc, keys, EXTRA_INTKEY_VAL);
    }

    private static <T> Collection<Object[]> makeStringsCases(String desc,
            T[] keys) {
        return makeMaps(desc, keys, EXTRA_STRING_VAL);
    }

    private static final Collection<Object[]> mapsWithObjectsCases
            = new ArrayList<>() {
        {
            addAll(makeObjectsCases("unique objects", createUniqueObjectKeys()));
            addAll(makeObjectsCases("colliding objects", createCollidingObjectKeys()));
        }
    };

    private static final Collection<Object[]> mapsWithStringsCases
            = new ArrayList<>() {
        {
            addAll(makeStringsCases("unique strings", createUniqueStringKeys()));
            addAll(makeStringsCases("colliding strings", createCollidingStringKeys()));
        }
    };

    private static final Collection<Object[]> mapsWithObjectsAndStringsCases
            = new ArrayList<>() {
        {
            addAll(mapsWithObjectsCases);
            addAll(mapsWithStringsCases);
        }
    };

    private static final Collection<Object[]> hashMapsWithObjectsCases
            = new ArrayList<>() {
        {
            addAll(makeMapsHashMap("unique objects",
                createUniqueObjectKeys(), EXTRA_INTKEY_VAL));
            addAll(makeMapsHashMap("collisions objects",
                createCollidingObjectKeys(), EXTRA_INTKEY_VAL));
        }
    };

    @DataProvider
    public Iterator<Object[]> mapsWithObjects() {
        return mapsWithObjectsCases.iterator();
    }

    @DataProvider
    public Iterator<Object[]> mapsWithStrings() {
        return mapsWithStringsCases.iterator();
    }

    @DataProvider
    public Iterator<Object[]> mapsWithObjectsAndStrings() {
        return mapsWithObjectsAndStringsCases.iterator();
    }

    @DataProvider
    public Iterator<Object[]> hashMapsWithObjects() {
        return hashMapsWithObjectsCases.iterator();
    }

}
