/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/*
 * @test
 * @bug 8000955
 * @summary Map.Entry implementations need to comply with Map.Entry.hashCode() defined behaviour.
 * @author ngmr
 */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class EntryHashCode {
    private static final int TEST_SIZE = 100;

    static final Object[][] entryData = {
        new Object[TEST_SIZE],
        new Object[TEST_SIZE]
    };

    @SuppressWarnings("unchecked")
    static final Map<Object,Object>[] maps = (Map<Object,Object>[])new Map[] {
        new HashMap<>(),
        new Hashtable<>(),
        new IdentityHashMap<>(),
        new LinkedHashMap<>(),
        new TreeMap<>(),
        new WeakHashMap<>(),
        new ConcurrentHashMap<>(),
        new ConcurrentSkipListMap<>()
    };

    static {
        for (int i = 0; i < entryData[0].length; i++) {
            // key objects need to be Comparable for use in TreeMap
            entryData[0][i] = new Comparable<Object>() {
                public int compareTo(Object o) {
                    return (hashCode() - o.hashCode());
                }
            };
            entryData[1][i] = new Object();
        }
    }

    private static void addTestData(Map<Object,Object> map) {
        for (int i = 0; i < entryData[0].length; i++) {
            map.put(entryData[0][i], entryData[1][i]);
        }
    }

    public static void main(String[] args) throws Exception {
        Exception failure = null;
        for (Map<Object,Object> map: maps) {
            addTestData(map);

            try {
                for (Map.Entry<Object,Object> e: map.entrySet()) {
                    Object key = e.getKey();
                    Object value = e.getValue();
                    int expectedEntryHashCode =
                        (Objects.hashCode(key) ^ Objects.hashCode(value));

                    if (e.hashCode() != expectedEntryHashCode) {
                        throw new Exception("FAILURE: " +
                                e.getClass().getName() +
                                ".hashCode() does not conform to defined" +
                                " behaviour of java.util.Map.Entry.hashCode()");
                    }
                }
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            } finally {
                map.clear();
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
