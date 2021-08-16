/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test ServiceLoader caches
 * @run testng CachingTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Spliterator;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CachingTest {

    // service type
    public static interface S { }

    // service provider implementations
    public static class S1 implements S { }

    public static class S2 implements S { }

    private ClassLoader testClassLoader;

    // creates the services configuration file and sets the ClassLoader
    @BeforeClass
    void setup() throws Exception {
        String classes = System.getProperty("test.classes");
        Path dir = Paths.get(classes, "META-INF", "services");
        Files.createDirectories(dir);
        Path config = dir.resolve(S.class.getName());
        Files.write(config, List.of(S1.class.getName(), S2.class.getName()));
        this.testClassLoader = CachingTest.class.getClassLoader();
    }

    private void checkLists(List<?> list1, List<?> list2) {
        assertTrue(list1.size() == 2);
        assertTrue(list2.size() == 2);
        Iterator<?> iterator1 = list1.iterator();
        Iterator<?> iterator2 = list2.iterator();
        while (iterator1.hasNext()) {
            assertTrue(iterator1.next() == iterator2.next());
        }
    }

    @Test
    public void testIterator1() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        List<S> list1 = new ArrayList<>();
        List<S> list2 = new ArrayList<>();

        // 1-1-2-2
        sl.forEach(list1::add);
        sl.forEach(list2::add);

        checkLists(list1, list2);
    }

    // interleaved
    @Test
    public void testIterator2() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        List<S> list1 = new ArrayList<>();
        List<S> list2 = new ArrayList<>();

        Iterator<S> iterator1 = sl.iterator();
        Iterator<S> iterator2 = sl.iterator();

        // 1-2-1-2
        list1.add(iterator1.next());
        list2.add(iterator2.next());
        list1.add(iterator1.next());
        list2.add(iterator2.next());

        checkLists(list1, list2);
    }

    // interleaved
    @Test
    public void testIterator3() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        List<S> list1 = new ArrayList<>();
        List<S> list2 = new ArrayList<>();

        Iterator<S> iterator1 = sl.iterator();
        Iterator<S> iterator2 = sl.iterator();

        // 1-2-2-1
        list1.add(iterator1.next());
        list2.add(iterator2.next());
        list2.add(iterator2.next());
        list1.add(iterator1.next());

        checkLists(list1, list2);
    }

    @Test
    public void testStream1() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        // 1-1-2-2
        List<Provider<S>> list1 = sl.stream().collect(Collectors.toList());
        List<Provider<S>> list2 = sl.stream().collect(Collectors.toList());
        checkLists(list1, list2);
    }

    // interleaved
    @Test
    public void testStream2() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        List<Provider<S>> list1 = new ArrayList<>();
        List<Provider<S>> list2 = new ArrayList<>();

        Spliterator<Provider<S>> spliterator1 = sl.stream().spliterator();
        Spliterator<Provider<S>> spliterator2 = sl.stream().spliterator();

        // 1-2-1-2
        spliterator1.tryAdvance(list1::add);
        spliterator2.tryAdvance(list2::add);
        spliterator1.tryAdvance(list1::add);
        spliterator2.tryAdvance(list2::add);

        assertFalse(spliterator1.tryAdvance(e -> assertTrue(false)));
        assertFalse(spliterator2.tryAdvance(e -> assertTrue(false)));

        checkLists(list1, list2);
    }

    // interleaved
    @Test
    public void testStream3() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        List<Provider<S>> list1 = new ArrayList<>();
        List<Provider<S>> list2 = new ArrayList<>();

        Spliterator<Provider<S>> spliterator1 = sl.stream().spliterator();
        Spliterator<Provider<S>> spliterator2 = sl.stream().spliterator();

        // 1-2-2-1
        spliterator1.tryAdvance(list1::add);
        spliterator2.tryAdvance(list2::add);
        spliterator2.tryAdvance(list2::add);
        spliterator1.tryAdvance(list1::add);

        assertFalse(spliterator1.tryAdvance(e -> assertTrue(false)));
        assertFalse(spliterator2.tryAdvance(e -> assertTrue(false)));

        checkLists(list1, list2);
    }
}
