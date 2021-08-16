/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008785
 * @summary Ensure toArray() implementations return correct results.
 * @author Mike Duigou
 */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ToArray {

    /**
     * Number of elements per map.
     */
    private static final int TEST_SIZE = 5000;

    private static void realMain(String[] args) throws Throwable {
        Map<Integer, Long>[] maps = (Map<Integer, Long>[]) new Map[]{
                    new HashMap<>(),
                    new Hashtable<>(),
                    new IdentityHashMap<>(),
                    new LinkedHashMap<>(),
                    new TreeMap<>(),
                    new WeakHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentSkipListMap<>()
                };

        // for each map type.
        for (Map<Integer, Long> map : maps) {
             try {
                testMap(map);
             } catch(Exception all) {
                unexpected("Failed for " + map.getClass().getName(), all);
             }
        }
    }

    private static final Integer[] KEYS = new Integer[TEST_SIZE];

    private static final Long[] VALUES = new Long[TEST_SIZE];

    static {
        for (int each = 0; each < TEST_SIZE; each++) {
            KEYS[each]   = Integer.valueOf(each);
            VALUES[each] = Long.valueOf(each + TEST_SIZE);
        }
    }


    private static void testMap(Map<Integer, Long> map) {
        System.out.println("Testing " + map.getClass());
        System.out.flush();

        // Fill the map
        for (int each = 0; each < TEST_SIZE; each++) {
            map.put(KEYS[each], VALUES[each]);
        }

        // check the keys
        Object[] keys = map.keySet().toArray();
        Arrays.sort(keys);

        for(int each = 0; each < TEST_SIZE; each++) {
            check( "unexpected key", keys[each] == KEYS[each]);
        }

        // check the values
        Object[] values = map.values().toArray();
        Arrays.sort(values);

        for(int each = 0; each < TEST_SIZE; each++) {
            check( "unexpected value", values[each] == VALUES[each]);
        }

        // check the entries
        Map.Entry<Integer,Long>[] entries = map.entrySet().toArray(new Map.Entry[TEST_SIZE]);
        Arrays.sort( entries,new Comparator<Map.Entry<Integer,Long>>() {
                public int compare(Map.Entry<Integer,Long> o1, Map.Entry<Integer,Long> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                }});

        for(int each = 0; each < TEST_SIZE; each++) {
            check( "unexpected entry", entries[each].getKey() == KEYS[each] && entries[each].getValue() == VALUES[each]);
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;

    static void pass() {
        passed++;
    }

    static void fail() {
        failed++;
        (new Error("Failure")).printStackTrace(System.err);
    }

    static void fail(String msg) {
        failed++;
        (new Error("Failure: " + msg)).printStackTrace(System.err);
    }

    static void abort() {
        fail();
        System.exit(1);
    }

    static void abort(String msg) {
        fail(msg);
        System.exit(1);
    }

    static void unexpected(String msg, Throwable t) {
        System.err.println("Unexpected: " + msg);
        unexpected(t);
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace(System.err);
    }

    static void check(boolean cond) {
        if (cond) {
            pass();
        } else {
            fail();
        }
    }

    static void check(String desc, boolean cond) {
        if (cond) {
            pass();
        } else {
            fail(desc);
        }
    }

    static void equal(Object x, Object y) {
        if (Objects.equals(x, y)) {
            pass();
        } else {
            fail(x + " not equal to " + y);
        }
    }

    public static void main(String[] args) throws Throwable {
        Thread.currentThread().setName(ToArray.class.getName());
//        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            realMain(args);
        } catch (Throwable t) {
            unexpected(t);
        }

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) {
            throw new Error("Some tests failed");
        }
    }
}
