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

/**
 * @test
 * @run testng Wrappers
 * @summary Ensure Collections wrapping classes provide non-default implementations
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static org.testng.Assert.assertFalse;

@Test(groups = "unit")
public class Wrappers {
    static Object[][] collections;

    @DataProvider(name="collections")
    public static Object[][] collectionCases() {
        if (collections != null) {
            return collections;
        }

        List<Object[]> cases = new ArrayList<>();
        LinkedList<Integer> seedList = new LinkedList<>();
        ArrayList<Integer> seedRandomAccess = new ArrayList<>();
        TreeSet<Integer> seedSet = new TreeSet<>();
        TreeMap<Integer, Integer> seedMap = new TreeMap<>();

        for (int i = 1; i <= 10; i++) {
            seedList.add(i);
            seedRandomAccess.add(i);
            seedSet.add(i);
            seedMap.put(i, i);
        }

        cases.add(new Object[] { Collections.unmodifiableCollection(seedList) });
        cases.add(new Object[] { Collections.unmodifiableList(seedList) });
        cases.add(new Object[] { Collections.unmodifiableList(seedRandomAccess) });
        cases.add(new Object[] { Collections.unmodifiableSet(seedSet) });
        cases.add(new Object[] { Collections.unmodifiableSortedSet(seedSet) });
        cases.add(new Object[] { Collections.unmodifiableNavigableSet(seedSet) });

        // As sets from map also need to be unmodifiable, thus a wrapping
        // layer exist and should not have default methods
        cases.add(new Object[] { Collections.unmodifiableMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.unmodifiableMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.unmodifiableMap(seedMap).values() });
        cases.add(new Object[] { Collections.unmodifiableSortedMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.unmodifiableSortedMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.unmodifiableSortedMap(seedMap).values() });
        cases.add(new Object[] { Collections.unmodifiableNavigableMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.unmodifiableNavigableMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.unmodifiableNavigableMap(seedMap).values() });

        // Synchronized
        cases.add(new Object[] { Collections.synchronizedCollection(seedList) });
        cases.add(new Object[] { Collections.synchronizedList(seedList) });
        cases.add(new Object[] { Collections.synchronizedList(seedRandomAccess) });
        cases.add(new Object[] { Collections.synchronizedSet(seedSet) });
        cases.add(new Object[] { Collections.synchronizedSortedSet(seedSet) });
        cases.add(new Object[] { Collections.synchronizedNavigableSet(seedSet) });

        // As sets from map also need to be synchronized on the map, thus a
        // wrapping layer exist and should not have default methods
        cases.add(new Object[] { Collections.synchronizedMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.synchronizedMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.synchronizedMap(seedMap).values() });
        cases.add(new Object[] { Collections.synchronizedSortedMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.synchronizedSortedMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.synchronizedSortedMap(seedMap).values() });
        cases.add(new Object[] { Collections.synchronizedNavigableMap(seedMap).entrySet() });
        cases.add(new Object[] { Collections.synchronizedNavigableMap(seedMap).keySet() });
        cases.add(new Object[] { Collections.synchronizedNavigableMap(seedMap).values() });

        // Checked
        cases.add(new Object[] { Collections.checkedCollection(seedList, Integer.class) });
        cases.add(new Object[] { Collections.checkedList(seedList, Integer.class) });
        cases.add(new Object[] { Collections.checkedList(seedRandomAccess, Integer.class) });
        cases.add(new Object[] { Collections.checkedSet(seedSet, Integer.class) });
        cases.add(new Object[] { Collections.checkedSortedSet(seedSet, Integer.class) });
        cases.add(new Object[] { Collections.checkedNavigableSet(seedSet, Integer.class) });
        cases.add(new Object[] { Collections.checkedQueue(seedList, Integer.class) });

        // asLifoQueue is another wrapper
        cases.add(new Object[] { Collections.asLifoQueue(seedList) });

        collections = cases.toArray(new Object[0][]);
        return collections;
    }

    static Method[] defaultMethods;

    static {
        List<Method> list = new ArrayList<>();
        Method[] methods = Collection.class.getMethods();
        for (Method m: methods) {
            if (m.isDefault()) {
                list.add(m);
            }
        }
        defaultMethods = list.toArray(new Method[0]);
    }

    @Test(dataProvider = "collections")
    public static void testAllDefaultMethodsOverridden(Collection c) throws NoSuchMethodException {
        Class cls = c.getClass();
        for (Method m: defaultMethods) {
            Method m2 = cls.getMethod(m.getName(), m.getParameterTypes());
            // default had been override
            assertFalse(m2.isDefault(), cls.getCanonicalName());
        }
    }
}

