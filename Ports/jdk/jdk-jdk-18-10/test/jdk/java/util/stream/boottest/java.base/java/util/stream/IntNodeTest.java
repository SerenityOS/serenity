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
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.SpliteratorTestHelper;
import java.util.function.Function;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class IntNodeTest extends OpTestCase {

    @DataProvider(name = "nodes")
    public Object[][] createSizes() {
        List<Object[]> params = new ArrayList<>();

        for (int size : Arrays.asList(0, 1, 4, 15, 16, 17, 127, 128, 129, 1000)) {
            int[] array = new int[size];
            for (int i = 0; i < array.length; i++) {
                array[i] = i;
            }

            List<Node<Integer>> nodes = new ArrayList<>();

            nodes.add(Nodes.node(array));
            nodes.add(degenerateTree(Spliterators.iterator(Arrays.spliterator(array))));
            nodes.add(tree(toList(array), l -> Nodes.node(toIntArray(l))));
            nodes.add(fill(array, Nodes.intBuilder(array.length)));
            nodes.add(fill(array, Nodes.intBuilder()));

            for (Node<Integer> node : nodes) {
                params.add(new Object[]{array, node});
            }

        }

        return params.toArray(new Object[0][]);
    }

    private static void assertEqualsListIntArray(List<Integer> list, int[] array) {
        assertEquals(list.size(), array.length);
        for (int i = 0; i < array.length; i++)
            assertEquals(array[i], (int) list.get(i));
    }

    private List<Integer> toList(int[] a) {
        List<Integer> l = new ArrayList<>();
        for (int i : a) {
            l.add(i);
        }

        return l;
    }

    private int[] toIntArray(List<Integer> l) {
        int[] a = new int[l.size()];

        int i = 0;
        for (Integer e : l) {
            a[i++] = e;
        }
        return a;
    }

    private Node.OfInt fill(int[] array, Node.Builder.OfInt nb) {
        nb.begin(array.length);
        for (int i : array)
            nb.accept(i);
        nb.end();
        return nb.build();
    }

    private Node.OfInt degenerateTree(PrimitiveIterator.OfInt it) {
        if (!it.hasNext()) {
            return Nodes.node(new int[0]);
        }

        int i = it.nextInt();
        if (it.hasNext()) {
            return new Nodes.ConcNode.OfInt(Nodes.node(new int[] {i}), degenerateTree(it));
        }
        else {
            return Nodes.node(new int[] {i});
        }
    }

    private Node.OfInt tree(List<Integer> l, Function<List<Integer>, Node.OfInt> m) {
        if (l.size() < 3) {
            return m.apply(l);
        }
        else {
            return new Nodes.ConcNode.OfInt(
                    tree(l.subList(0, l.size() / 2), m),
                    tree(l.subList(l.size() / 2, l.size()), m));
        }
    }

    @Test(dataProvider = "nodes")
    public void testAsArray(int[] array, Node.OfInt n) {
        assertEquals(n.asPrimitiveArray(), array);
    }

    @Test(dataProvider = "nodes")
    public void testFlattenAsArray(int[] array, Node.OfInt n) {
        assertEquals(Nodes.flattenInt(n).asPrimitiveArray(), array);
    }

    @Test(dataProvider = "nodes")
    public void testCopyTo(int[] array, Node.OfInt n) {
        int[] copy = new int[(int) n.count()];
        n.copyInto(copy, 0);

        assertEquals(copy, array);
    }

    @Test(dataProvider = "nodes", groups = { "serialization-hostile" })
    public void testForEach(int[] array, Node.OfInt n) {
        List<Integer> l = new ArrayList<>((int) n.count());
        n.forEach((int e) -> {
            l.add(e);
        });

        assertEqualsListIntArray(l, array);
    }

    @Test(dataProvider = "nodes")
    public void testStreams(int[] array, Node.OfInt n) {
        TestData.OfInt data = TestData.Factory.ofNode("Node", n);

        exerciseOps(data, s -> s);
        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "nodes")
    public void testSpliterator(int[] array, Node.OfInt n) {
        SpliteratorTestHelper.testIntSpliterator(n::spliterator);
    }

    @Test(dataProvider = "nodes")
    public void testTruncate(int[] array, Node.OfInt n) {
        int[] nums = new int[] { 0, 1, array.length / 2, array.length - 1, array.length };
        for (int start : nums)
            for (int end : nums) {
                if (start < 0 || end < 0 || end < start || end > array.length)
                    continue;
                Node.OfInt slice = n.truncate(start, end, Integer[]::new);
                int[] asArray = slice.asPrimitiveArray();
                for (int k = start; k < end; k++)
                    assertEquals(array[k], asArray[k - start]);
            }
    }
}
