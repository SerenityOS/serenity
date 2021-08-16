/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.util.stream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.LambdaTestHelpers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests laziness of stream operations -- mutations to the source after the stream() but prior to terminal operations
 * are reflected in the stream contents.
 */
@Test
public class CollectionAndMapModifyStreamTest {

    @DataProvider(name = "collections")
    public Object[][] createCollections() {
        List<Integer> content = LambdaTestHelpers.countTo(10);

        List<Collection<Integer>> collections = new ArrayList<>();
        collections.add(new ArrayList<>(content));
        collections.add(new LinkedList<>(content));
        collections.add(new Vector<>(content));

        collections.add(new HashSet<>(content));
        collections.add(new LinkedHashSet<>(content));
        collections.add(new TreeSet<>(content));

        Stack<Integer> stack = new Stack<>();
        stack.addAll(content);
        collections.add(stack);
        collections.add(new PriorityQueue<>(content));
        collections.add(new ArrayDeque<>(content));

        // Concurrent collections

        collections.add(new ConcurrentSkipListSet<>(content));

        ArrayBlockingQueue<Integer> arrayBlockingQueue = new ArrayBlockingQueue<>(content.size());
        for (Integer i : content)
            arrayBlockingQueue.add(i);
        collections.add(arrayBlockingQueue);
        collections.add(new PriorityBlockingQueue<>(content));
        collections.add(new LinkedBlockingQueue<>(content));
        collections.add(new LinkedTransferQueue<>(content));
        collections.add(new ConcurrentLinkedQueue<>(content));
        collections.add(new LinkedBlockingDeque<>(content));
        collections.add(new ConcurrentLinkedDeque<>(content));

        Object[][] params = new Object[collections.size()][];
        for (int i = 0; i < collections.size(); i++) {
            params[i] = new Object[]{collections.get(i).getClass().getName(), collections.get(i)};
        }

        return params;
    }

    @Test(dataProvider = "collections")
    public void testCollectionSizeRemove(String name, Collection<Integer> c) {
        assertTrue(c.remove(1));
        Stream<Integer> s = c.stream();
        assertTrue(c.remove(2));
        Object[] result = s.toArray();
        assertEquals(result.length, c.size());
    }

    @DataProvider(name = "maps")
    public Object[][] createMaps() {
        Map<Integer, Integer> content = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            content.put(i, i);
        }

        Map<String, Supplier<Map<Integer, Integer>>> maps = new HashMap<>();

        maps.put(HashMap.class.getName(), () -> new HashMap<>(content));
        maps.put(LinkedHashMap.class.getName(), () -> new LinkedHashMap<>(content));
        maps.put(IdentityHashMap.class.getName(), () -> new IdentityHashMap<>(content));
        maps.put(WeakHashMap.class.getName(), () -> new WeakHashMap<>(content));

        maps.put(TreeMap.class.getName(), () -> new TreeMap<>(content));
        maps.put(TreeMap.class.getName() + ".descendingMap()", () -> new TreeMap<>(content).descendingMap());

        // The following are not lazy
//        maps.put(TreeMap.class.getName() + ".descendingMap().descendingMap()", () -> new TreeMap<>(content).descendingMap().descendingMap());
//        maps.put(TreeMap.class.getName() + ".headMap()", () -> new TreeMap<>(content).headMap(content.size() - 1));
//        maps.put(TreeMap.class.getName() + ".descendingMap().headMap()", () -> new TreeMap<>(content).descendingMap().tailMap(content.size() - 1, false));

        // Concurrent collections

        maps.put(ConcurrentHashMap.class.getName(), () -> new ConcurrentHashMap<>(content));
        maps.put(ConcurrentSkipListMap.class.getName(), () -> new ConcurrentSkipListMap<>(content));

        Object[][] params = new Object[maps.size()][];
        int i = 0;
        for (Map.Entry<String, Supplier<Map<Integer, Integer>>> e : maps.entrySet()) {
            params[i++] = new Object[]{e.getKey(), e.getValue()};

        }

        return params;
    }

    @Test(dataProvider = "maps", groups = { "serialization-hostile" })
    public void testMapKeysSizeRemove(String name, Supplier<Map<Integer, Integer>> c) {
        testCollectionSizeRemove(name + " key set", c.get().keySet());
    }

    @Test(dataProvider = "maps", groups = { "serialization-hostile" })
    public void testMapValuesSizeRemove(String name, Supplier<Map<Integer, Integer>> c) {
        testCollectionSizeRemove(name + " value set", c.get().values());
    }

    @Test(dataProvider = "maps")
    public void testMapEntriesSizeRemove(String name, Supplier<Map<Integer, Integer>> c) {
        testEntrySetSizeRemove(name + " entry set", c.get().entrySet());
    }

    private void testEntrySetSizeRemove(String name, Set<Map.Entry<Integer, Integer>> c) {
        Map.Entry<Integer, Integer> first = c.iterator().next();
        assertTrue(c.remove(first));
        Stream<Map.Entry<Integer, Integer>> s = c.stream();
        Map.Entry<Integer, Integer> second = c.iterator().next();
        assertTrue(c.remove(second));
        Object[] result = s.toArray();
        assertEquals(result.length, c.size());
    }
}
