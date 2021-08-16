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

/*
 * @test
 * @bug 8010122 8004518 8024331 8024688
 * @summary Test Map default methods
 * @author Mike Duigou
 * @run testng Defaults
 */

import org.testng.Assert.ThrowingRunnable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class Defaults {

    @Test(dataProvider = "Map<IntegerEnum,String> rw=all keys=withNull values=withNull")
    public void testGetOrDefaultNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), description + ": null key absent");
        assertNull(map.get(null), description + ": value not null");
        assertSame(map.get(null), map.getOrDefault(null, EXTRA_VALUE), description + ": values should match");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=all keys=all values=all")
    public void testGetOrDefault(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]), "expected key missing");
        assertSame(map.get(KEYS[1]), map.getOrDefault(KEYS[1], EXTRA_VALUE), "values should match");
        assertFalse(map.containsKey(EXTRA_KEY), "expected absent key");
        assertSame(map.getOrDefault(EXTRA_KEY, EXTRA_VALUE), EXTRA_VALUE, "value not returned as default");
        assertNull(map.getOrDefault(EXTRA_KEY, null), "null not returned as default");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testPutIfAbsentNulls(String description, Map<IntegerEnum, String> map) {
        // null -> null
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertNull(map.putIfAbsent(null, EXTRA_VALUE), "previous not null");
        // null -> EXTRA_VALUE
        assertTrue(map.containsKey(null), "null key absent");
        assertSame(map.get(null), EXTRA_VALUE, "unexpected value");
        assertSame(map.putIfAbsent(null, null), EXTRA_VALUE, "previous not expected value");
        assertTrue(map.containsKey(null), "null key absent");
        assertSame(map.get(null), EXTRA_VALUE, "unexpected value");
        assertSame(map.remove(null), EXTRA_VALUE, "removed unexpected value");
        // null -> <absent>

        assertFalse(map.containsKey(null), description + ": key present after remove");
        assertNull(map.putIfAbsent(null, null), "previous not null");
        // null -> null
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertNull(map.putIfAbsent(null, EXTRA_VALUE), "previous not null");
        assertSame(map.get(null), EXTRA_VALUE, "value not expected");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testPutIfAbsent(String description, Map<IntegerEnum, String> map) {
        // 1 -> 1
        assertTrue(map.containsKey(KEYS[1]));
        Object expected = map.get(KEYS[1]);
        assertTrue(null == expected || expected == VALUES[1]);
        assertSame(map.putIfAbsent(KEYS[1], EXTRA_VALUE), expected);
        assertSame(map.get(KEYS[1]), expected);

        // EXTRA_KEY -> <absent>
        assertFalse(map.containsKey(EXTRA_KEY));
        assertSame(map.putIfAbsent(EXTRA_KEY, EXTRA_VALUE), null);
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
        assertSame(map.putIfAbsent(EXTRA_KEY, VALUES[2]), EXTRA_VALUE);
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=all keys=all values=all")
    public void testForEach(String description, Map<IntegerEnum, String> map) {
        IntegerEnum[] EACH_KEY = new IntegerEnum[map.size()];

        map.forEach((k, v) -> {
            int idx = (null == k) ? 0 : k.ordinal(); // substitute for index.
            assertNull(EACH_KEY[idx]);
            EACH_KEY[idx] = (idx == 0) ? KEYS[0] : k; // substitute for comparison.
            assertSame(v, map.get(k));
        });

        assertEquals(KEYS, EACH_KEY, description);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public static void testReplaceAll(String description, Map<IntegerEnum, String> map) {
        IntegerEnum[] EACH_KEY = new IntegerEnum[map.size()];
        Set<String> EACH_REPLACE = new HashSet<>(map.size());

        map.replaceAll((k,v) -> {
            int idx = (null == k) ? 0 : k.ordinal(); // substitute for index.
            assertNull(EACH_KEY[idx]);
            EACH_KEY[idx] = (idx == 0) ? KEYS[0] : k; // substitute for comparison.
            assertSame(v, map.get(k));
            String replacement = v + " replaced";
            EACH_REPLACE.add(replacement);
            return replacement;
        });

        assertEquals(KEYS, EACH_KEY, description);
        assertEquals(map.values().size(), EACH_REPLACE.size(), description + EACH_REPLACE);
        assertTrue(EACH_REPLACE.containsAll(map.values()), description + " : " + EACH_REPLACE + " != " + map.values());
        assertTrue(map.values().containsAll(EACH_REPLACE), description + " : " + EACH_REPLACE + " != " + map.values());
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=nonNull values=nonNull")
    public static void testReplaceAllNoNullReplacement(String description, Map<IntegerEnum, String> map) {
        assertThrowsNPE(() -> map.replaceAll(null));
        assertThrowsNPE(() -> map.replaceAll((k,v) -> null)); //should not allow replacement with null value
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public static void testRemoveNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertFalse(map.remove(null, EXTRA_VALUE), description);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));
        assertNull(map.get(null));
        assertFalse(map.remove(null, null));
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public static void testRemove(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]));
        Object expected = map.get(KEYS[1]);
        assertTrue(null == expected || expected == VALUES[1]);
        assertFalse(map.remove(KEYS[1], EXTRA_VALUE), description);
        assertSame(map.get(KEYS[1]), expected);
        assertTrue(map.remove(KEYS[1], expected));
        assertNull(map.get(KEYS[1]));
        assertFalse(map.remove(KEYS[1], expected));

        assertFalse(map.containsKey(EXTRA_KEY));
        assertFalse(map.remove(EXTRA_KEY, EXTRA_VALUE));
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testReplaceKVNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertSame(map.replace(null, EXTRA_VALUE), null);
        assertSame(map.get(null), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=nonNull values=nonNull")
    public void testReplaceKVNoNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(FIRST_KEY), "expected key missing");
        assertSame(map.get(FIRST_KEY), FIRST_VALUE, "found wrong value");
        assertThrowsNPE(() -> map.replace(FIRST_KEY, null));
        assertSame(map.replace(FIRST_KEY, EXTRA_VALUE), FIRST_VALUE, description + ": replaced wrong value");
        assertSame(map.get(FIRST_KEY), EXTRA_VALUE, "found wrong value");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testReplaceKV(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]));
        Object expected = map.get(KEYS[1]);
        assertTrue(null == expected || expected == VALUES[1]);
        assertSame(map.replace(KEYS[1], EXTRA_VALUE), expected);
        assertSame(map.get(KEYS[1]), EXTRA_VALUE);

        assertFalse(map.containsKey(EXTRA_KEY));
        assertNull(map.replace(EXTRA_KEY, EXTRA_VALUE));
        assertFalse(map.containsKey(EXTRA_KEY));
        assertNull(map.get(EXTRA_KEY));
        assertNull(map.put(EXTRA_KEY, EXTRA_VALUE));
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
        assertSame(map.replace(EXTRA_KEY, (String)expected), EXTRA_VALUE);
        assertSame(map.get(EXTRA_KEY), expected);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testReplaceKVVNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertFalse(map.replace(null, EXTRA_VALUE, EXTRA_VALUE));
        assertNull(map.get(null));
        assertTrue(map.replace(null, null, EXTRA_VALUE));
        assertSame(map.get(null), EXTRA_VALUE);
        assertTrue(map.replace(null, EXTRA_VALUE, EXTRA_VALUE));
        assertSame(map.get(null), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=nonNull values=nonNull")
    public void testReplaceKVVNoNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(FIRST_KEY), "expected key missing");
        assertSame(map.get(FIRST_KEY), FIRST_VALUE, "found wrong value");
        assertThrowsNPE(() -> map.replace(FIRST_KEY, FIRST_VALUE, null));
        assertThrowsNPE(
                () -> {
                    if (!map.replace(FIRST_KEY, null, EXTRA_VALUE)) {
                        throw new NullPointerException("default returns false rather than throwing");
                    }
                });
        assertTrue(map.replace(FIRST_KEY, FIRST_VALUE, EXTRA_VALUE), description + ": replaced wrong value");
        assertSame(map.get(FIRST_KEY), EXTRA_VALUE, "found wrong value");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testReplaceKVV(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]));
        Object expected = map.get(KEYS[1]);
        assertTrue(null == expected || expected == VALUES[1]);
        assertFalse(map.replace(KEYS[1], EXTRA_VALUE, EXTRA_VALUE));
        assertSame(map.get(KEYS[1]), expected);
        assertTrue(map.replace(KEYS[1], (String)expected, EXTRA_VALUE));
        assertSame(map.get(KEYS[1]), EXTRA_VALUE);
        assertTrue(map.replace(KEYS[1], EXTRA_VALUE, EXTRA_VALUE));
        assertSame(map.get(KEYS[1]), EXTRA_VALUE);

        assertFalse(map.containsKey(EXTRA_KEY));
        assertFalse(map.replace(EXTRA_KEY, EXTRA_VALUE, EXTRA_VALUE));
        assertFalse(map.containsKey(EXTRA_KEY));
        assertNull(map.get(EXTRA_KEY));
        assertNull(map.put(EXTRA_KEY, EXTRA_VALUE));
        assertTrue(map.containsKey(EXTRA_KEY));
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
        assertTrue(map.replace(EXTRA_KEY, EXTRA_VALUE, EXTRA_VALUE));
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testComputeIfAbsentNulls(String description, Map<IntegerEnum, String> map) {
        // null -> null
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertSame(map.computeIfAbsent(null, (k) -> null), null,  "not expected result");
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertSame(map.computeIfAbsent(null, (k) -> EXTRA_VALUE), EXTRA_VALUE, "not mapped to result");
        // null -> EXTRA_VALUE
        assertTrue(map.containsKey(null), "null key absent");
        assertSame(map.get(null), EXTRA_VALUE,  "not expected value");
        assertSame(map.remove(null), EXTRA_VALUE, "removed unexpected value");
        // null -> <absent>
        assertFalse(map.containsKey(null), "null key present");
        assertSame(map.computeIfAbsent(null, (k) -> EXTRA_VALUE), EXTRA_VALUE, "not mapped to result");
        // null -> EXTRA_VALUE
        assertTrue(map.containsKey(null), "null key absent");
        assertSame(map.get(null), EXTRA_VALUE,  "not expected value");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testComputeIfAbsent(String description, Map<IntegerEnum, String> map) {
        // 1 -> 1
        assertTrue(map.containsKey(KEYS[1]));
        Object expected = map.get(KEYS[1]);
        assertTrue(null == expected || expected == VALUES[1], description + String.valueOf(expected));
        expected = (null == expected) ? EXTRA_VALUE : expected;
        assertSame(map.computeIfAbsent(KEYS[1], (k) -> EXTRA_VALUE), expected, description);
        assertSame(map.get(KEYS[1]), expected, description);

        // EXTRA_KEY -> <absent>
        assertFalse(map.containsKey(EXTRA_KEY));
        assertNull(map.computeIfAbsent(EXTRA_KEY, (k) -> null));
        assertFalse(map.containsKey(EXTRA_KEY));
        assertSame(map.computeIfAbsent(EXTRA_KEY, (k) -> EXTRA_VALUE), EXTRA_VALUE);
        // EXTRA_KEY -> EXTRA_VALUE
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testComputeIfAbsentNullFunction(String description, Map<IntegerEnum, String> map) {
        assertThrowsNPE(() -> map.computeIfAbsent(KEYS[1], null));
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testComputeIfPresentNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), description + ": null key absent");
        assertNull(map.get(null), description + ": value not null");
        assertSame(map.computeIfPresent(null, (k, v) -> {
            fail(description + ": null value is not deemed present");
            return EXTRA_VALUE;
        }), null, description);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null), description);
        assertNull(map.remove(EXTRA_KEY), description + ": unexpected mapping");
        assertNull(map.put(EXTRA_KEY, null), description + ": unexpected value");
        assertSame(map.computeIfPresent(EXTRA_KEY, (k, v) -> {
            fail(description + ": null value is not deemed present");
            return EXTRA_VALUE;
        }), null, description);
        assertNull(map.get(EXTRA_KEY), description + ": null mapping gone");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testComputeIfPresent(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]));
        Object value = map.get(KEYS[1]);
        assertTrue(null == value || value == VALUES[1], description + String.valueOf(value));
        Object expected = (null == value) ? null : EXTRA_VALUE;
        assertSame(map.computeIfPresent(KEYS[1], (k, v) -> {
            assertSame(v, value);
            return EXTRA_VALUE;
        }), expected, description);
        assertSame(map.get(KEYS[1]), expected, description);

        assertFalse(map.containsKey(EXTRA_KEY));
        assertSame(map.computeIfPresent(EXTRA_KEY, (k, v) -> {
            fail();
            return EXTRA_VALUE;
        }), null);
        assertFalse(map.containsKey(EXTRA_KEY));
        assertSame(map.get(EXTRA_KEY), null);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testComputeIfPresentNullFunction(String description, Map<IntegerEnum, String> map) {
        assertThrowsNPE(() -> map.computeIfPresent(KEYS[1], null));
    }

     @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull")
    public void testComputeNulls(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(null), "null key absent");
        assertNull(map.get(null), "value not null");
        assertSame(map.compute(null, (k, v) -> {
            assertNull(k);
            assertNull(v);
            return null;
        }), null, description);
        assertFalse(map.containsKey(null), description + ": null key present.");
        assertSame(map.compute(null, (k, v) -> {
            assertSame(k, null);
            assertNull(v);
            return EXTRA_VALUE;
        }), EXTRA_VALUE, description);
        assertTrue(map.containsKey(null));
        assertSame(map.get(null), EXTRA_VALUE, description);
        assertSame(map.remove(null), EXTRA_VALUE, description + ": removed value not expected");
        // no mapping before and after
        assertFalse(map.containsKey(null), description + ": null key present");
        assertSame(map.compute(null, (k, v) -> {
            assertNull(k);
            assertNull(v);
            return null;
        }), null, description + ": expected null result" );
        assertFalse(map.containsKey(null), description + ": null key present");
        // compute with map not containing value
        assertNull(map.remove(EXTRA_KEY),  description + ": unexpected mapping");
        assertFalse(map.containsKey(EXTRA_KEY),  description + ": key present");
        assertSame(map.compute(EXTRA_KEY, (k, v) -> {
            assertSame(k, EXTRA_KEY);
            assertNull(v);
            return null;
        }), null, description);
        assertFalse(map.containsKey(EXTRA_KEY),  description + ": null key present");
        // ensure removal.
        assertNull(map.put(EXTRA_KEY, EXTRA_VALUE));
        assertSame(map.compute(EXTRA_KEY, (k, v) -> {
            assertSame(k, EXTRA_KEY);
            assertSame(v, EXTRA_VALUE);
            return null;
        }), null, description + ": null resulted expected");
        assertFalse(map.containsKey(EXTRA_KEY),  description + ": null key present");
       // compute with map containing null value
        assertNull(map.put(EXTRA_KEY, null),  description + ": unexpected value");
        assertSame(map.compute(EXTRA_KEY, (k, v) -> {
            assertSame(k, EXTRA_KEY);
            assertNull(v);
            return null;
        }), null, description);
        assertFalse(map.containsKey(EXTRA_KEY),  description + ": null key present");
        assertNull(map.put(EXTRA_KEY, null),  description + ": unexpected value");
        assertSame(map.compute(EXTRA_KEY, (k, v) -> {
            assertSame(k, EXTRA_KEY);
            assertNull(v);
            return EXTRA_VALUE;
        }), EXTRA_VALUE, description);
        assertTrue(map.containsKey(EXTRA_KEY), "null key present");
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testCompute(String description, Map<IntegerEnum, String> map) {
        assertTrue(map.containsKey(KEYS[1]));
        Object value = map.get(KEYS[1]);
        assertTrue(null == value || value == VALUES[1], description + String.valueOf(value));
        assertSame(map.compute(KEYS[1], (k, v) -> {
            assertSame(k, KEYS[1]);
            assertSame(v, value);
            return EXTRA_VALUE;
        }), EXTRA_VALUE, description);
        assertSame(map.get(KEYS[1]), EXTRA_VALUE, description);
        assertNull(map.compute(KEYS[1], (k, v) -> {
            assertSame(v, EXTRA_VALUE);
            return null;
        }), description);
        assertFalse(map.containsKey(KEYS[1]));

        assertFalse(map.containsKey(EXTRA_KEY));
        assertSame(map.compute(EXTRA_KEY, (k, v) -> {
            assertNull(v);
            return EXTRA_VALUE;
        }), EXTRA_VALUE);
        assertTrue(map.containsKey(EXTRA_KEY));
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE);
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testComputeNullFunction(String description, Map<IntegerEnum, String> map) {
        assertThrowsNPE(() -> map.compute(KEYS[1], null));
    }

    @Test(dataProvider = "MergeCases")
    private void testMerge(String description, Map<IntegerEnum, String> map, Merging.Value oldValue, Merging.Value newValue, Merging.Merger merger, Merging.Value put, Merging.Value result) {
            // add and check initial conditions.
            switch (oldValue) {
                case ABSENT :
                    map.remove(EXTRA_KEY);
                    assertFalse(map.containsKey(EXTRA_KEY), "key not absent");
                    break;
                case NULL :
                    map.put(EXTRA_KEY, null);
                    assertTrue(map.containsKey(EXTRA_KEY), "key absent");
                    assertNull(map.get(EXTRA_KEY), "wrong value");
                    break;
                case OLDVALUE :
                    map.put(EXTRA_KEY, VALUES[1]);
                    assertTrue(map.containsKey(EXTRA_KEY), "key absent");
                    assertSame(map.get(EXTRA_KEY), VALUES[1], "wrong value");
                    break;
                default:
                    fail("unexpected old value");
            }

            String returned = map.merge(EXTRA_KEY,
                newValue == Merging.Value.NULL ? (String) null : VALUES[2],
                merger
                );

            // check result

            switch (result) {
                case NULL :
                    assertNull(returned, "wrong value");
                    break;
                case NEWVALUE :
                    assertSame(returned, VALUES[2], "wrong value");
                    break;
                case RESULT :
                    assertSame(returned, VALUES[3], "wrong value");
                    break;
                default:
                    fail("unexpected new value");
            }

            // check map
            switch (put) {
                case ABSENT :
                    assertFalse(map.containsKey(EXTRA_KEY), "key not absent");
                    break;
                case NULL :
                    assertTrue(map.containsKey(EXTRA_KEY), "key absent");
                    assertNull(map.get(EXTRA_KEY), "wrong value");
                    break;
                case NEWVALUE :
                    assertTrue(map.containsKey(EXTRA_KEY), "key absent");
                    assertSame(map.get(EXTRA_KEY), VALUES[2], "wrong value");
                    break;
                case RESULT :
                    assertTrue(map.containsKey(EXTRA_KEY), "key absent");
                    assertSame(map.get(EXTRA_KEY), VALUES[3], "wrong value");
                    break;
                default:
                    fail("unexpected new value");
            }
    }

    @Test(dataProvider = "Map<IntegerEnum,String> rw=true keys=all values=all")
    public void testMergeNullMerger(String description, Map<IntegerEnum, String> map) {
        assertThrowsNPE(() -> map.merge(KEYS[1], VALUES[1], null));
    }

    /** A function that flipflops between running two other functions. */
    static <T,U,V> BiFunction<T,U,V> twoStep(AtomicBoolean b,
                                             BiFunction<T,U,V> first,
                                             BiFunction<T,U,V> second) {
        return (t, u) -> {
            boolean bb = b.get();
            try {
                return (b.get() ? first : second).apply(t, u);
            } finally {
                b.set(!bb);
            }};
    }

    /**
     * Simulates races by modifying the map within the mapping function.
     */
    @Test
    public void testConcurrentMap_computeIfAbsent_racy() {
        final ConcurrentMap<Long,Long> map = new ImplementsConcurrentMap<>();
        final Long two = 2L;
        Function<Long,Long> f, g;

        // race not detected if function returns null
        f = (k) -> { map.put(two, 42L); return null; };
        assertNull(map.computeIfAbsent(two, f));
        assertEquals(42L, (long)map.get(two));

        map.clear();
        f = (k) -> { map.put(two, 42L); return 86L; };
        assertEquals(42L, (long)map.computeIfAbsent(two, f));
        assertEquals(42L, (long)map.get(two));

        // mapping function ignored if value already exists
        map.put(two, 99L);
        assertEquals(99L, (long)map.computeIfAbsent(two, f));
        assertEquals(99L, (long)map.get(two));
    }

    /**
     * Simulates races by modifying the map within the remapping function.
     */
    @Test
    public void testConcurrentMap_computeIfPresent_racy() {
        final AtomicBoolean b = new AtomicBoolean(true);
        final ConcurrentMap<Long,Long> map = new ImplementsConcurrentMap<>();
        final Long two = 2L;
        BiFunction<Long,Long,Long> f, g;

        for (Long val : new Long[] { null, 86L }) {
            map.clear();

            // Function not invoked if no mapping exists
            f = (k, v) -> { map.put(two, 42L); return val; };
            assertNull(map.computeIfPresent(two, f));
            assertNull(map.get(two));

            map.put(two, 42L);
            f = (k, v) -> { map.put(two, 86L); return val; };
            g = (k, v) -> {
                assertSame(two, k);
                assertEquals(86L, (long)v);
                return null;
            };
            assertNull(map.computeIfPresent(two, twoStep(b, f, g)));
            assertFalse(map.containsKey(two));
            assertTrue(b.get());

            map.put(two, 42L);
            f = (k, v) -> { map.put(two, 86L); return val; };
            g = (k, v) -> {
                assertSame(two, k);
                assertEquals(86L, (long)v);
                return 99L;
            };
            assertEquals(99L, (long)map.computeIfPresent(two, twoStep(b, f, g)));
            assertTrue(map.containsKey(two));
            assertTrue(b.get());
        }
    }

    @Test
    public void testConcurrentMap_compute_simple() {
        final ConcurrentMap<Long,Long> map = new ImplementsConcurrentMap<>();
        BiFunction<Long,Long,Long> fun = (k, v) -> ((v == null) ? 0L : k + v);
        assertEquals(Long.valueOf(0L), map.compute(3L, fun));
        assertEquals(Long.valueOf(3L), map.compute(3L, fun));
        assertEquals(Long.valueOf(6L), map.compute(3L, fun));
        assertNull(map.compute(3L, (k, v) -> null));
        assertTrue(map.isEmpty());

        assertEquals(Long.valueOf(0L), map.compute(new Long(3L), fun));
        assertEquals(Long.valueOf(3L), map.compute(new Long(3L), fun));
        assertEquals(Long.valueOf(6L), map.compute(new Long(3L), fun));
        assertNull(map.compute(3L, (k, v) -> null));
        assertTrue(map.isEmpty());
    }

    /**
     * Simulates races by modifying the map within the remapping function.
     */
    @Test
    public void testConcurrentMap_compute_racy() {
        final AtomicBoolean b = new AtomicBoolean(true);
        final ConcurrentMap<Long,Long> map = new ImplementsConcurrentMap<>();
        final Long two = 2L;
        BiFunction<Long,Long,Long> f, g;

        // null -> null is a no-op; race not detected
        f = (k, v) -> { map.put(two, 42L); return null; };
        assertNull(map.compute(two, f));
        assertEquals(42L, (long)map.get(two));

        for (Long val : new Long[] { null, 86L }) {
            map.clear();

            f = (k, v) -> { map.put(two, 42L); return 86L; };
            g = (k, v) -> {
                assertSame(two, k);
                assertEquals(42L, (long)v);
                return k + v;
            };
            assertEquals(44L, (long)map.compute(two, twoStep(b, f, g)));
            assertEquals(44L, (long)map.get(two));
            assertTrue(b.get());

            f = (k, v) -> { map.remove(two); return val; };
            g = (k, v) -> {
                assertSame(two, k);
                assertNull(v);
                return 44L;
            };
            assertEquals(44L, (long)map.compute(two, twoStep(b, f, g)));
            assertEquals(44L, (long)map.get(two));
            assertTrue(map.containsKey(two));
            assertTrue(b.get());

            f = (k, v) -> { map.remove(two); return val; };
            g = (k, v) -> {
                assertSame(two, k);
                assertNull(v);
                return null;
            };
            assertNull(map.compute(two, twoStep(b, f, g)));
            assertNull(map.get(two));
            assertFalse(map.containsKey(two));
            assertTrue(b.get());
        }
    }

    /**
     * Simulates races by modifying the map within the remapping function.
     */
    @Test
    public void testConcurrentMap_merge_racy() {
        final AtomicBoolean b = new AtomicBoolean(true);
        final ConcurrentMap<Long,Long> map = new ImplementsConcurrentMap<>();
        final Long two = 2L;
        BiFunction<Long,Long,Long> f, g;

        for (Long val : new Long[] { null, 86L }) {
            map.clear();

            f = (v, w) -> { throw new AssertionError(); };
            assertEquals(99L, (long)map.merge(two, 99L, f));
            assertEquals(99L, (long)map.get(two));

            f = (v, w) -> { map.put(two, 42L); return val; };
            g = (v, w) -> {
                assertEquals(42L, (long)v);
                assertEquals(3L, (long)w);
                return v + w;
            };
            assertEquals(45L, (long)map.merge(two, 3L, twoStep(b, f, g)));
            assertEquals(45L, (long)map.get(two));
            assertTrue(b.get());

            f = (v, w) -> { map.remove(two); return val; };
            g = (k, v) -> { throw new AssertionError(); };
            assertEquals(55L, (long)map.merge(two, 55L, twoStep(b, f, g)));
            assertEquals(55L, (long)map.get(two));
            assertTrue(map.containsKey(two));
            assertFalse(b.get()); b.set(true);
        }
    }

    public enum IntegerEnum {

        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9,
        e10, e11, e12, e13, e14, e15, e16, e17, e18, e19,
        e20, e21, e22, e23, e24, e25, e26, e27, e28, e29,
        e30, e31, e32, e33, e34, e35, e36, e37, e38, e39,
        e40, e41, e42, e43, e44, e45, e46, e47, e48, e49,
        e50, e51, e52, e53, e54, e55, e56, e57, e58, e59,
        e60, e61, e62, e63, e64, e65, e66, e67, e68, e69,
        e70, e71, e72, e73, e74, e75, e76, e77, e78, e79,
        e80, e81, e82, e83, e84, e85, e86, e87, e88, e89,
        e90, e91, e92, e93, e94, e95, e96, e97, e98, e99,
        EXTRA_KEY;
        public static final int SIZE = values().length;
    }
    private static final int TEST_SIZE = IntegerEnum.SIZE - 1;
    /**
     * Realized keys ensure that there is always a hard ref to all test objects.
     */
    private static final IntegerEnum[] KEYS = new IntegerEnum[TEST_SIZE];
    /**
     * Realized values ensure that there is always a hard ref to all test
     * objects.
     */
    private static final String[] VALUES = new String[TEST_SIZE];

    static {
        IntegerEnum[] keys = IntegerEnum.values();
        for (int each = 0; each < TEST_SIZE; each++) {
            KEYS[each] = keys[each];
            VALUES[each] = String.valueOf(each);
        }
    }

    private static final IntegerEnum FIRST_KEY = KEYS[0];
    private static final String FIRST_VALUE = VALUES[0];
    private static final IntegerEnum EXTRA_KEY = IntegerEnum.EXTRA_KEY;
    private static final String EXTRA_VALUE = String.valueOf(TEST_SIZE);

    @DataProvider(name = "Map<IntegerEnum,String> rw=all keys=all values=all", parallel = true)
    public static Iterator<Object[]> allMapProvider() {
        return makeAllMaps().iterator();
    }

    @DataProvider(name = "Map<IntegerEnum,String> rw=all keys=withNull values=withNull", parallel = true)
    public static Iterator<Object[]> allMapWithNullsProvider() {
        return makeAllMapsWithNulls().iterator();
    }

    @DataProvider(name = "Map<IntegerEnum,String> rw=true keys=nonNull values=nonNull", parallel = true)
    public static Iterator<Object[]> rwNonNullMapProvider() {
        return makeRWNoNullsMaps().iterator();
    }

    @DataProvider(name = "Map<IntegerEnum,String> rw=true keys=nonNull values=all", parallel = true)
    public static Iterator<Object[]> rwNonNullKeysMapProvider() {
        return makeRWMapsNoNulls().iterator();
    }

    @DataProvider(name = "Map<IntegerEnum,String> rw=true keys=all values=all", parallel = true)
    public static Iterator<Object[]> rwMapProvider() {
        return makeAllRWMaps().iterator();
    }

    @DataProvider(name = "Map<IntegerEnum,String> rw=true keys=withNull values=withNull", parallel = true)
    public static Iterator<Object[]> rwNullsMapProvider() {
        return makeAllRWMapsWithNulls().iterator();
    }

    private static Collection<Object[]> makeAllRWMapsWithNulls() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeRWMaps(true, true));

        return all;
    }

    private static Collection<Object[]> makeRWMapsNoNulls() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeRWNoNullKeysMaps(false));
        all.addAll(makeRWNoNullsMaps());

        return all;
    }

    private static Collection<Object[]> makeAllROMaps() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeROMaps(false));
        all.addAll(makeROMaps(true));

        return all;
    }

    private static Collection<Object[]> makeAllRWMaps() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeRWNoNullsMaps());
        all.addAll(makeRWMaps(false,true));
        all.addAll(makeRWMaps(true,true));
        all.addAll(makeRWNoNullKeysMaps(true));
        return all;
    }

    private static Collection<Object[]> makeAllMaps() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeAllROMaps());
        all.addAll(makeAllRWMaps());

        return all;
    }

    private static Collection<Object[]> makeAllMapsWithNulls() {
        Collection<Object[]> all = new ArrayList<>();

        all.addAll(makeROMaps(true));
        all.addAll(makeRWMaps(true,true));

        return all;
    }

    /**
     * @param nullKeys include null keys
     * @param nullValues include null values
     * @return
     */
    private static Collection<Object[]> makeRWMaps(boolean nullKeys, boolean nullValues) {
        return Arrays.asList(
            new Object[]{"HashMap", makeMap(HashMap::new, nullKeys, nullValues)},
            new Object[]{"IdentityHashMap", makeMap(IdentityHashMap::new, nullKeys, nullValues)},
            new Object[]{"LinkedHashMap", makeMap(LinkedHashMap::new, nullKeys, nullValues)},
            new Object[]{"WeakHashMap", makeMap(WeakHashMap::new, nullKeys, nullValues)},
            new Object[]{"Collections.checkedMap(HashMap)", Collections.checkedMap(makeMap(HashMap::new, nullKeys, nullValues), IntegerEnum.class, String.class)},
            new Object[]{"Collections.synchronizedMap(HashMap)", Collections.synchronizedMap(makeMap(HashMap::new, nullKeys, nullValues))},
            new Object[]{"ExtendsAbstractMap", makeMap(ExtendsAbstractMap::new, nullKeys, nullValues)});
    }

    /**
     * @param nulls include null values
     * @return
     */
    private static Collection<Object[]> makeRWNoNullKeysMaps(boolean nulls) {
        return Arrays.asList(
                // null key hostile
                new Object[]{"EnumMap", makeMap(() -> new EnumMap(IntegerEnum.class), false, nulls)},
                new Object[]{"TreeMap", makeMap(TreeMap::new, false, nulls)},
                new Object[]{"ExtendsAbstractMap(TreeMap)", makeMap(() -> {return new ExtendsAbstractMap(new TreeMap());}, false, nulls)},
                new Object[]{"Collections.synchronizedMap(EnumMap)", Collections.synchronizedMap(makeMap(() -> new EnumMap(IntegerEnum.class), false, nulls))}
                );
    }

    private static Collection<Object[]> makeRWNoNullsMaps() {
        return Arrays.asList(
            // null key and value hostile
            new Object[]{"Hashtable", makeMap(Hashtable::new, false, false)},
            new Object[]{"ConcurrentHashMap", makeMap(ConcurrentHashMap::new, false, false)},
            new Object[]{"ConcurrentSkipListMap", makeMap(ConcurrentSkipListMap::new, false, false)},
            new Object[]{"Collections.synchronizedMap(ConcurrentHashMap)", Collections.synchronizedMap(makeMap(ConcurrentHashMap::new, false, false))},
            new Object[]{"Collections.checkedMap(ConcurrentHashMap)", Collections.checkedMap(makeMap(ConcurrentHashMap::new, false, false), IntegerEnum.class, String.class)},
            new Object[]{"ExtendsAbstractMap(ConcurrentHashMap)", makeMap(() -> {return new ExtendsAbstractMap(new ConcurrentHashMap());}, false, false)},
            new Object[]{"ImplementsConcurrentMap", makeMap(ImplementsConcurrentMap::new, false, false)}
            );
    }

    /**
     * @param nulls include nulls
     * @return
     */
    private static Collection<Object[]> makeROMaps(boolean nulls) {
        return Arrays.asList(new Object[][]{
            new Object[]{"Collections.unmodifiableMap(HashMap)", Collections.unmodifiableMap(makeMap(HashMap::new, nulls, nulls))}
        });
    }

    /**
     * @param supplier a supplier of mutable map instances.
     *
     * @param nullKeys   include null keys
     * @param nullValues include null values
     * @return
     */
    private static Map<IntegerEnum, String> makeMap(Supplier<Map<IntegerEnum, String>> supplier, boolean nullKeys, boolean nullValues) {
        Map<IntegerEnum, String> result = supplier.get();

        for (int each = 0; each < TEST_SIZE; each++) {
            IntegerEnum key = nullKeys ? (each == 0) ? null : KEYS[each] : KEYS[each];
            String value = nullValues ? (each == 0) ? null : VALUES[each] : VALUES[each];

            result.put(key, value);
        }

        return result;
    }

    static class Merging {
        public enum Value {
            ABSENT,
            NULL,
            OLDVALUE,
            NEWVALUE,
            RESULT
        }

        public enum Merger implements BiFunction<String,String,String> {
            UNUSED {
                public String apply(String oldValue, String newValue) {
                    fail("should not be called");
                    return null;
                }
            },
            NULL {
                public String apply(String oldValue, String newValue) {
                    return null;
                }
            },
            RESULT {
                public String apply(String oldValue, String newValue) {
                    return VALUES[3];
                }
            },
        }
    }

    @DataProvider(name = "MergeCases", parallel = true)
    public Iterator<Object[]> mergeCasesProvider() {
        Collection<Object[]> cases = new ArrayList<>();

        cases.addAll(makeMergeTestCases());

        return cases.iterator();
    }

    static Collection<Object[]> makeMergeTestCases() {
        Collection<Object[]> cases = new ArrayList<>();

        for (Object[] mapParams : makeAllRWMaps() ) {
            cases.add(new Object[] { mapParams[0], mapParams[1], Merging.Value.ABSENT, Merging.Value.NEWVALUE, Merging.Merger.UNUSED, Merging.Value.NEWVALUE, Merging.Value.NEWVALUE });
        }

        for (Object[] mapParams : makeAllRWMaps() ) {
            cases.add(new Object[] { mapParams[0], mapParams[1], Merging.Value.OLDVALUE, Merging.Value.NEWVALUE, Merging.Merger.NULL, Merging.Value.ABSENT, Merging.Value.NULL });
        }

        for (Object[] mapParams : makeAllRWMaps() ) {
            cases.add(new Object[] { mapParams[0], mapParams[1], Merging.Value.OLDVALUE, Merging.Value.NEWVALUE, Merging.Merger.RESULT, Merging.Value.RESULT, Merging.Value.RESULT });
        }

        return cases;
    }

    public static void assertThrowsNPE(ThrowingRunnable r) {
        assertThrows(NullPointerException.class, r);
    }

    /**
     * A simple mutable map implementation that provides only default
     * implementations of all methods. ie. none of the Map interface default
     * methods have overridden implementations.
     *
     * @param <K> Type of keys
     * @param <V> Type of values
     */
    public static class ExtendsAbstractMap<M extends Map<K,V>, K, V> extends AbstractMap<K,V> {

        protected final M map;

        public ExtendsAbstractMap() { this( (M) new HashMap<K,V>()); }

        protected ExtendsAbstractMap(M map) { this.map = map; }

        @Override public Set<Map.Entry<K,V>> entrySet() {
            return new AbstractSet<Map.Entry<K,V>>() {
                @Override public int size() {
                    return map.size();
                }

                @Override public Iterator<Map.Entry<K,V>> iterator() {
                    final Iterator<Map.Entry<K,V>> source = map.entrySet().iterator();
                    return new Iterator<Map.Entry<K,V>>() {
                       public boolean hasNext() { return source.hasNext(); }
                       public Map.Entry<K,V> next() { return source.next(); }
                       public void remove() { source.remove(); }
                    };
                }

                @Override public boolean add(Map.Entry<K,V> e) {
                    return map.entrySet().add(e);
                }
            };
        }

        @Override public V put(K key, V value) {
            return map.put(key, value);
        }
    }

    /**
     * A simple mutable concurrent map implementation that provides only default
     * implementations of all methods, i.e. none of the ConcurrentMap interface
     * default methods have overridden implementations.
     *
     * @param <K> Type of keys
     * @param <V> Type of values
     */
    public static class ImplementsConcurrentMap<K,V> extends ExtendsAbstractMap<ConcurrentMap<K,V>, K, V> implements ConcurrentMap<K,V> {
        public ImplementsConcurrentMap() { super(new ConcurrentHashMap<K,V>()); }

        // ConcurrentMap reabstracts these methods.
        //
        // Unlike ConcurrentHashMap, we have zero tolerance for null values.

        @Override public V replace(K k, V v) {
            return map.replace(requireNonNull(k), requireNonNull(v));
        }

        @Override public boolean replace(K k, V v, V vv) {
            return map.replace(requireNonNull(k),
                               requireNonNull(v),
                               requireNonNull(vv));
        }

        @Override public boolean remove(Object k, Object v) {
            return map.remove(requireNonNull(k), requireNonNull(v));
        }

        @Override public V putIfAbsent(K k, V v) {
            return map.putIfAbsent(requireNonNull(k), requireNonNull(v));
        }
    }
}
