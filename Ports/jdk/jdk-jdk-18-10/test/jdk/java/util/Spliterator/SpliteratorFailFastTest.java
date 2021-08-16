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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Spliterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.testng.Assert.ThrowingRunnable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

/**
 * @test
 * @bug 8148748
 * @summary Spliterator fail-fast tests
 * @run testng SpliteratorFailFastTest
 */

@Test
public class SpliteratorFailFastTest extends SpliteratorLateBindingFailFastHelper {

    static Object[][] spliteratorDataProvider;

    @DataProvider(name = "Source")
    public static Object[][] spliteratorDataProvider() {
        if (spliteratorDataProvider != null) {
            return spliteratorDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        SpliteratorDataBuilder<Integer> db =
                new SpliteratorDataBuilder<>(data, 5, Arrays.asList(1, 2, 3, 4));

        // Collections

        db.addList(ArrayList::new);

        db.addList(LinkedList::new);

        db.addList(Vector::new);

        db.addList(AbstractRandomAccessListImpl::new);

        db.addCollection(HashSet::new);

        db.addCollection(LinkedHashSet::new);

        db.addCollection(TreeSet::new);

        db.addCollection(c -> {
            Stack<Integer> s = new Stack<>();
            s.addAll(c);
            return s;
        });

        db.addCollection(PriorityQueue::new);

        // ArrayDeque fails some tests since its fail-fast support is weaker
        // than other collections and limited to detecting most, but not all,
        // removals.  It probably requires its own test since it is difficult
        // to abstract out the conditions under which it fails-fast.
//        db.addCollection(ArrayDeque::new);

        // Maps

        db.addMap(HashMap::new);

        db.addMap(LinkedHashMap::new);

        // This fails when run through jtreg but passes when run through
        // ant
//        db.addMap(IdentityHashMap::new);

        db.addMap(WeakHashMap::new);

        // @@@  Descending maps etc
        db.addMap(TreeMap::new);

        return spliteratorDataProvider = data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "Source")
    public <T> void testTryAdvance(String description, Supplier<Source<T>> ss) {
        {
            Source<T> source = ss.get();
            Spliterator<T> s = source.spliterator();

            s.tryAdvance(e -> {
            });
            source.update();

            assertThrowsCME(() -> s.tryAdvance(e -> {
            }));
        }

        {
            Source<T> source = ss.get();
            Spliterator<T> s = source.spliterator();

            s.tryAdvance(e -> {
            });
            source.update();

            assertThrowsCME(() -> s.forEachRemaining(e -> {
            }));
        }
    }

    @Test(dataProvider = "Source")
    public <T> void testForEach(String description, Supplier<Source<T>> ss) {
        Source<T> source = ss.get();
        Spliterator<T> s = source.spliterator();

        assertThrowsCME(() -> s.forEachRemaining(e -> {
            source.update();
        }));
    }

    @Test(dataProvider = "Source")
    public <T> void testEstimateSize(String description, Supplier<Source<T>> ss) {
        {
            Source<T> source = ss.get();
            Spliterator<T> s = source.spliterator();

            s.estimateSize();
            source.update();

            assertThrowsCME(() -> s.tryAdvance(e -> {
            }));
        }

        {
            Source<T> source = ss.get();
            Spliterator<T> s = source.spliterator();

            s.estimateSize();
            source.update();

            assertThrowsCME(() -> s.forEachRemaining(e -> {
            }));
        }
    }

    private void assertThrowsCME(ThrowingRunnable r) {
        assertThrows(ConcurrentModificationException.class, r);
    }

}
