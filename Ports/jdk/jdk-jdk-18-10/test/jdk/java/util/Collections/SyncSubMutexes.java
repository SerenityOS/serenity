/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048209
 * @summary Check that Collections.synchronizedNavigableSet().tailSet() is using
 * the same lock object as it's source.
 * @modules java.base/java.util:open
 * @run testng SyncSubMutexes
 */
import java.lang.reflect.Field;
import java.util.*;
import java.util.Set;
import java.util.Arrays;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.assertSame;

public class SyncSubMutexes {

    @Test(dataProvider = "Collections")
    public void testCollections(Collection<String> instance) {
        // nothing to test, no subset methods
    }

    @Test(dataProvider = "Lists")
    public void testLists(List<String> instance) {
         assertSame(getSyncCollectionMutex(instance.subList(0, 1)), getSyncCollectionMutex(instance));
    }

    @Test(dataProvider = "Sets")
    public void testSets(Set<String> instance) {
        // nothing to test, no subset methods

    }

    @Test(dataProvider = "SortedSets")
    public void testSortedSets(SortedSet<String> instance) {
         assertSame(getSyncCollectionMutex(instance.headSet("Echo")), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.tailSet("Charlie")), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.subSet("Charlie", "Echo")), getSyncCollectionMutex(instance));

    }

    @Test(dataProvider = "NavigableSets")
    public void testNavigableSets(NavigableSet<String> instance) {
         assertSame(getSyncCollectionMutex(instance.descendingSet()), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.headSet("Echo")), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.headSet("Echo", true)), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.tailSet("Charlie")), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.tailSet("Charlie", true)), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.subSet("Charlie", "Echo")), getSyncCollectionMutex(instance));
         assertSame(getSyncCollectionMutex(instance.subSet("Charlie", true, "Echo", true)), getSyncCollectionMutex(instance));
    }

    @Test(dataProvider = "Maps")
    public void testMaps(Map<String, String> instance) {
         assertSame(getSyncCollectionMutex(instance.entrySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.keySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.values()), getSyncMapMutex(instance));
    }

    @Test(dataProvider = "SortedMaps")
    public void testSortedMaps(SortedMap<String, String> instance) {
         assertSame(getSyncCollectionMutex(instance.entrySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.keySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.values()), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.headMap("Echo")), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.tailMap("Charlie")), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.subMap("Charlie", "Echo")), getSyncMapMutex(instance));
    }

    @Test(dataProvider = "NavigableMaps")
    public void testNavigableMaps(NavigableMap<String, String> instance) {
         assertSame(getSyncMapMutex(instance.descendingMap()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.entrySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.keySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.descendingKeySet()), getSyncMapMutex(instance));
         assertSame(getSyncCollectionMutex(instance.values()), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.headMap("Echo")), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.headMap("Echo", true)), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.tailMap("Charlie")), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.tailMap("Charlie", true)), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.subMap("Charlie", true, "Echo", true)), getSyncMapMutex(instance));
         assertSame(getSyncMapMutex(instance.subMap("Charlie", true, "Echo", true)), getSyncMapMutex(instance));
    }

    @DataProvider(name = "Collections", parallel = true)
    public static Iterator<Object[]> collectionProvider() {
        return makeCollections().iterator();
    }

    @DataProvider(name = "Lists", parallel = true)
    public static Iterator<Object[]> listProvider() {
        return makeLists().iterator();
    }

    @DataProvider(name = "Sets", parallel = true)
    public static Iterator<Object[]> setProvider() {
        return makeSets().iterator();
    }

    @DataProvider(name = "SortedSets", parallel = true)
    public static Iterator<Object[]> sortedsetProvider() {
        return makeSortedSets().iterator();
    }

    @DataProvider(name = "NavigableSets", parallel = true)
    public static Iterator<Object[]> navigablesetProvider() {
        return makeNavigableSets().iterator();
    }

    @DataProvider(name = "Maps", parallel = true)
    public static Iterator<Object[]> mapProvider() {
        return makeMaps().iterator();
    }

    @DataProvider(name = "SortedMaps", parallel = true)
    public static Iterator<Object[]> sortedmapProvider() {
        return makeSortedMaps().iterator();
    }

    @DataProvider(name = "NavigableMaps", parallel = true)
    public static Iterator<Object[]> navigablemapProvider() {
        return makeNavigableMaps().iterator();
    }

    private static final Collection<String> BASE_COLLECTION = Collections.unmodifiableCollection(
            Arrays.asList("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf")
    );
    private static final Map<String, String> BASE_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        for(String each : BASE_COLLECTION) {
            map.put(each, "*" + each + "*");
        }
        BASE_MAP = Collections.unmodifiableMap(map);
    }

    public static Collection<Object[]> makeCollections() {
        Collection<Object[]> instances = new ArrayList<>();
        instances.add(new Object[] {Collections.synchronizedCollection(new ArrayList<>(BASE_COLLECTION))});
        instances.addAll(makeLists());

        return instances;
    }

    public static Collection<Object[]> makeLists() {
        Collection<Object[]> instances = new ArrayList<>();
        instances.add(new Object[] {Collections.synchronizedList(new ArrayList<>(BASE_COLLECTION))});
        instances.add(new Object[] {Collections.synchronizedList(new ArrayList<>(BASE_COLLECTION)).subList(1, 2)});

        return instances;
    }

     public static Collection<Object[]> makeSets() {
        Collection<Object[]> instances = new ArrayList<>();

        instances.add(new Object[] {Collections.synchronizedSet(new TreeSet<>(BASE_COLLECTION))});
        instances.addAll(makeSortedSets());
        return instances;
     }

    public static Collection<Object[]> makeSortedSets() {
        Collection<Object[]> instances = new ArrayList<>();
        instances.add(new Object[] {Collections.synchronizedSortedSet(new TreeSet<>(BASE_COLLECTION))});
        instances.add(new Object[] {Collections.synchronizedSortedSet(new TreeSet<>(BASE_COLLECTION)).headSet("Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedSortedSet(new TreeSet<>(BASE_COLLECTION)).tailSet("Bravo")});
        instances.add(new Object[] {Collections.synchronizedSortedSet(new TreeSet<>(BASE_COLLECTION)).subSet("Bravo", "Foxtrot")});
        instances.addAll(makeNavigableSets());

        return instances;
     }

    public static Collection<Object[]> makeNavigableSets() {
        Collection<Object[]> instances = new ArrayList<>();

        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION))});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).descendingSet().descendingSet()});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).headSet("Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).headSet("Foxtrot", true)});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).tailSet("Bravo")});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).tailSet("Bravo", true)});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).subSet("Bravo", "Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedNavigableSet(new TreeSet<>(BASE_COLLECTION)).subSet("Bravo", true, "Foxtrot", true)});

        return instances;
    }

    public static Collection<Object[]> makeMaps() {
        Collection<Object[]> instances = new ArrayList<>();

        instances.add(new Object[] {Collections.synchronizedMap(new HashMap<>(BASE_MAP))});
        instances.addAll(makeSortedMaps());

        return instances;
    }

    public static Collection<Object[]> makeSortedMaps() {
        Collection<Object[]> instances = new ArrayList<>();

        instances.add(new Object[] {Collections.synchronizedSortedMap(new TreeMap<>(BASE_MAP))});
        instances.add(new Object[] {Collections.synchronizedSortedMap(new TreeMap<>(BASE_MAP)).headMap("Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedSortedMap(new TreeMap<>(BASE_MAP)).tailMap("Bravo")});
        instances.add(new Object[] {Collections.synchronizedSortedMap(new TreeMap<>(BASE_MAP)).subMap("Bravo", "Foxtrot")});
        instances.addAll(makeNavigableMaps());

        return instances;
    }

    public static Collection<Object[]> makeNavigableMaps() {
        Collection<Object[]> instances = new ArrayList<>();

        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP))});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP).descendingMap().descendingMap())});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).headMap("Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).headMap("Foxtrot", true)});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).tailMap("Bravo")});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).tailMap("Bravo", true)});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).subMap("Bravo", "Foxtrot")});
        instances.add(new Object[] {Collections.synchronizedNavigableMap(new TreeMap<>(BASE_MAP)).subMap("Bravo", true, "Foxtrot", true)});

        return instances;
    }

    private static Object getSyncCollectionMutex(Collection<?> from) {
        try {
            Class<?> synchronizedCollectionClazz = Class.forName("java.util.Collections$SynchronizedCollection");
            Field f = synchronizedCollectionClazz.getDeclaredField("mutex");
            f.setAccessible(true);
            return f.get(from);
        } catch ( ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to get mutex field.", e);
        }
    }

    private static Object getSyncMapMutex(Map<?,?> from) {
        try {
            Class<?> synchronizedMapClazz = Class.forName("java.util.Collections$SynchronizedMap");
            Field f = synchronizedMapClazz.getDeclaredField("mutex");
            f.setAccessible(true);
            return f.get(from);
        } catch ( ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to get mutex field.", e);
        }
    }

}
