/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SpliteratorTestHelper;
import java.util.function.Function;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class NodeTest extends OpTestCase {

    @DataProvider(name = "nodes")
    public Object[][] createSizes() {
        List<Object[]> params = new ArrayList<>();

        for (int size : Arrays.asList(0, 1, 4, 15, 16, 17, 127, 128, 129, 1000)) {
            Integer[] array = new Integer[size];
            for (int i = 0; i < array.length; i++) {
                array[i] = i;
            }

            List<Node<Integer>> nodes = new ArrayList<>();
            nodes.add(Nodes.node(array));
            nodes.add(Nodes.node(Arrays.asList(array)));
            nodes.add(degenerateTree(Arrays.asList(array).iterator()));
            nodes.add(tree(Arrays.asList(array), l -> Nodes.node(l.toArray(new Integer[l.size()]))));
            nodes.add(tree(Arrays.asList(array), l -> Nodes.node(l)));
            nodes.add(fill(array, Nodes.builder(array.length, LambdaTestHelpers.integerArrayGenerator)));
            nodes.add(fill(array, Nodes.builder()));

            for (int i = 0; i < nodes.size(); i++) {
                params.add(new Object[]{array, nodes.get(i)});
            }

        }

        return params.toArray(new Object[0][]);
    }

    Node<Integer> fill(Integer[] array, Node.Builder<Integer> nb) {
        nb.begin(array.length);
        for (Integer i : array) {
            nb.accept(i);
        }
        nb.end();
        return nb.build();
    }

    Node<Integer> degenerateTree(Iterator<Integer> it) {
        if (!it.hasNext()) {
            return Nodes.node(Collections.emptyList());
        }

        Integer i = it.next();
        if (it.hasNext()) {
            return new Nodes.ConcNode<Integer>(Nodes.node(new Integer[] {i}), degenerateTree(it));
        }
        else {
            return Nodes.node(new Integer[]{i});
        }
    }

    Node<Integer> tree(List<Integer> l, Function<List<Integer>, Node<Integer>> m) {
        if (l.size() < 3) {
            return m.apply(l);
        }
        else {
            return new Nodes.ConcNode<>(
                    tree(l.subList(0, l.size() / 2), m),
                    tree(l.subList(l.size() / 2, l.size()), m ));
        }
    }

    @Test(dataProvider = "nodes")
    public void testAsArray(Integer[] array, Node<Integer> n) {
        assertEquals(n.asArray(LambdaTestHelpers.integerArrayGenerator), array);
    }

    @Test(dataProvider = "nodes")
    public void testFlattenAsArray(Integer[] array, Node<Integer> n) {
        assertEquals(Nodes.flatten(n, LambdaTestHelpers.integerArrayGenerator)
                          .asArray(LambdaTestHelpers.integerArrayGenerator), array);
    }

    @Test(dataProvider = "nodes")
    public void testCopyTo(Integer[] array, Node<Integer> n) {
        Integer[] copy = new Integer[(int) n.count()];
        n.copyInto(copy, 0);

        assertEquals(copy, array);
    }

    @Test(dataProvider = "nodes", groups = { "serialization-hostile" })
    public void testForEach(Integer[] array, Node<Integer> n) {
        List<Integer> l = new ArrayList<>((int) n.count());
        n.forEach(e -> l.add(e));

        assertEquals(l.toArray(), array);
    }

    @Test(dataProvider = "nodes")
    public void testStreams(Integer[] array, Node<Integer> n) {
        TestData<Integer, Stream<Integer>> data = TestData.Factory.ofRefNode("Node", n);

        exerciseOps(data, s -> s);

        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "nodes")
    public void testSpliterator(Integer[] array, Node<Integer> n) {
        SpliteratorTestHelper.testSpliterator(n::spliterator);
    }

    @Test(dataProvider = "nodes")
    public void testTruncate(Integer[] array, Node<Integer> n) {
        int[] nums = new int[] { 0, 1, array.length / 2, array.length - 1, array.length };
        for (int start : nums)
            for (int end : nums) {
                if (start < 0 || end < 0 || end < start || end > array.length)
                    continue;
                Node<Integer> slice = n.truncate(start, end, Integer[]::new);
                Integer[] asArray = slice.asArray(Integer[]::new);
                for (int k = start; k < end; k++)
                    assertEquals(array[k], asArray[k - start]);
            }
    }
}
