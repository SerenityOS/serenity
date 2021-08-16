/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4327164 8229338
 * @summary Basic test for new RandomAccess interface
 * @run testng Basic
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class Basic {

    /*
     * Lists which implement Random Access interface
     */
    @DataProvider(name = "testLists")
    public Object[][] testData() {
        var intArray = new Integer[100];
        var stack = new Stack<>();
        var random = new Random();
        for (int i = 0; i < 100; i++) {
            var r = random.nextInt(100);
            stack.push(r);
            intArray[i] = r;
        }
        List<Integer> list = Arrays.asList(intArray);
        return new Object[][]{
                {list, true, "Arrays.asList"},
                {stack, true, "Stack"},
                {new ArrayList<>(list), true, "ArrayList"},
                {new LinkedList<>(list), false, "LinkedList"},
                {new Vector<>(list), true, "Vector"},
                {new CopyOnWriteArrayList<>(list), true, "CopyOnWriteArrayList"}
        };
    }

    @Test(dataProvider = "testLists")
    public void testRandomAccess(List<Integer> list, boolean expectedRA, String failMsg) {

        var actualRA = list instanceof RandomAccess;
        assertEquals(actualRA, expectedRA, failMsg);

        List<Integer> unmodList = Collections.unmodifiableList(list);
        List<Integer> syncList = Collections.synchronizedList(list);
        assertEquals((unmodList instanceof RandomAccess), actualRA,
                "Unmodifiable fails to preserve RandomAccess");
        assertEquals((syncList instanceof RandomAccess), actualRA,
                "Synchronized fails to preserve RandomAccess");

        while (list.size() > 0) {
            list = list.subList(0, list.size() - 1);
            assertEquals((list instanceof RandomAccess), actualRA,
                    "SubList fails to preserve RandomAccess: " + list.size());

            unmodList = unmodList.subList(0, unmodList.size() - 1);
            assertEquals((unmodList instanceof RandomAccess), actualRA,
                    "SubList(unmodifiable) fails to preserve RandomAccess: "
                            + unmodList.size());

            syncList = syncList.subList(0, syncList.size() - 1);
            assertEquals((syncList instanceof RandomAccess), actualRA,
                    "SubList(synchronized) fails to preserve RandomAccess: "
                            + syncList.size());
        }
    }

    @Test(dataProvider = "testLists")
    public void testListCopy(List<Integer> list, boolean expectedRA, String failMsg) {
        ArrayList testCollection = new ArrayList<>(Collections.nCopies(100, 0));
        // Test that copy works on random & sequential access
        Collections.copy(list, testCollection);
        assertEquals(list, testCollection, "Copy failed: " + failMsg);
    }

    @Test(dataProvider = "testLists")
    public void testListFill(List<Integer> list, boolean expectedRA, String failMsg) {
        ArrayList testCollection = new ArrayList<>(Collections.nCopies(100, 0));
        // Test that copy works on random & sequential access
        Collections.fill(list, 0);
        assertEquals(list, testCollection, "Fill failed: " + failMsg);
    }

    /*
     * Test that shuffle and binarySearch work the same on random and sequential access lists.
     */
    @DataProvider(name = "testFactoryLists")
    public Object[][] testDataFactory() {
        return new Object[][]{
                {"ArrayList -> LinkedList", supplier(ArrayList::new), copyCtor(LinkedList::new)},
                {"CopyOnWriteArrayList -> Stack", supplier(CopyOnWriteArrayList::new),
                        copyCtor((list) -> { var s = new Stack();s.addAll(list);return s; })}
        };
    }

    private Supplier<List<Integer>> supplier(Supplier<List<Integer>> supplier) {
        return supplier;
    }

    private Function<List<Integer>, List<Integer>> copyCtor(Function<List<Integer>, List<Integer>> ctor) {
        return ctor;
    }

    @Test(dataProvider = "testFactoryLists")
    public void testListShuffle(String description, Supplier<List<Integer>> randomAccessListSupplier,
                                Function<List<Integer>, List<Integer>> otherListFactory) {

        //e.g: ArrayList<Integer> al = new ArrayList<>();
        List<Integer> l1 = randomAccessListSupplier.get();
        for (int j = 0; j < 100; j++) {
            l1.add(Integer.valueOf(2 * j));
        }
        // e.g: List<Integer> ll = new LinkedList<>(al);
        List<Integer> l2 = otherListFactory.apply(l1);
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(l1, new Random(666));
            Collections.shuffle(l2, new Random(666));
            assertEquals(l1, l2, "Shuffle failed: " + description);
        }
    }

    @Test(dataProvider = "testFactoryLists")
    public void testListBinarySearch(String description, Supplier<List<Integer>> randomAccessListSupplier,
                                     Function<List<Integer>, List<Integer>> otherListFactory) {

        //e.g: ArrayList<Integer> al = new ArrayList<>();
        List<Integer> l1 = randomAccessListSupplier.get();
        for (int i = 0; i < 10000; i++) {
            l1.add(Integer.valueOf(2 * i));
        }
        // e.g: List<Integer> ll = new LinkedList<>(al);
        List<Integer> l2 = otherListFactory.apply(l1);
        for (int i = 0; i < 500; i++) {
            Integer key = Integer.valueOf(new Random(666).nextInt(20000));
            assertEquals(Collections.binarySearch(l1, key), Collections
                    .binarySearch(l2, key), "Binary search failed: " + description);
        }
    }
}
