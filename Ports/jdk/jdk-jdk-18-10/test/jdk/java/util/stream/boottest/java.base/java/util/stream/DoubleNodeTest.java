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
public class DoubleNodeTest extends OpTestCase {

    @DataProvider(name = "nodes")
    public Object[][] createSizes() {
        List<Object[]> params = new ArrayList<>();

        for (int size : Arrays.asList(0, 1, 4, 15, 16, 17, 127, 128, 129, 1000)) {
            double[] array = new double[size];
            for (int i = 0; i < array.length; i++) {
                array[i] = i;
            }

            List<Node<Double>> nodes = new ArrayList<>();

            nodes.add(Nodes.node(array));
            nodes.add(degenerateTree(Spliterators.iterator(Arrays.spliterator(array))));
            nodes.add(tree(toList(array), l -> Nodes.node(toDoubleArray(l))));
            nodes.add(fill(array, Nodes.doubleBuilder(array.length)));
            nodes.add(fill(array, Nodes.doubleBuilder()));

            for (Node<Double> node : nodes) {
                params.add(new Object[]{array, node});
            }

        }

        return params.toArray(new Object[0][]);
    }

    private static void assertEqualsListDoubleArray(List<Double> list, double[] array) {
        assertEquals(list.size(), array.length);
        for (int i = 0; i < array.length; i++)
            assertEquals(array[i], (double) list.get(i));
    }

    private List<Double> toList(double[] a) {
        List<Double> l = new ArrayList<>();
        for (double i : a) {
            l.add(i);
        }

        return l;
    }

    private double[] toDoubleArray(List<Double> l) {
        double[] a = new double[l.size()];

        int i = 0;
        for (Double e : l) {
            a[i++] = e;
        }
        return a;
    }

    private Node.OfDouble fill(double[] array, Node.Builder.OfDouble nb) {
        nb.begin(array.length);
        for (double i : array)
            nb.accept(i);
        nb.end();
        return nb.build();
    }

    private Node.OfDouble degenerateTree(PrimitiveIterator.OfDouble it) {
        if (!it.hasNext()) {
            return Nodes.node(new double[0]);
        }

        double i = it.nextDouble();
        if (it.hasNext()) {
            return new Nodes.ConcNode.OfDouble(Nodes.node(new double[] {i}), degenerateTree(it));
        }
        else {
            return Nodes.node(new double[] {i});
        }
    }

    private Node.OfDouble tree(List<Double> l, Function<List<Double>, Node.OfDouble> m) {
        if (l.size() < 3) {
            return m.apply(l);
        }
        else {
            return new Nodes.ConcNode.OfDouble(
                    tree(l.subList(0, l.size() / 2), m),
                    tree(l.subList(l.size() / 2, l.size()), m));
        }
    }

    @Test(dataProvider = "nodes")
    public void testAsArray(double[] array, Node.OfDouble n) {
        assertEquals(n.asPrimitiveArray(), array);
    }

    @Test(dataProvider = "nodes")
    public void testFlattenAsArray(double[] array, Node.OfDouble n) {
        assertEquals(Nodes.flattenDouble(n).asPrimitiveArray(), array);
    }

    @Test(dataProvider = "nodes")
    public void testCopyTo(double[] array, Node.OfDouble n) {
        double[] copy = new double[(int) n.count()];
        n.copyInto(copy, 0);

        assertEquals(copy, array);
    }

    @Test(dataProvider = "nodes", groups = { "serialization-hostile" })
    public void testForEach(double[] array, Node.OfDouble n) {
        List<Double> l = new ArrayList<>((int) n.count());
        n.forEach((double e) -> {
            l.add(e);
        });

        assertEqualsListDoubleArray(l, array);
    }

    @Test(dataProvider = "nodes")
    public void testStreams(double[] array, Node.OfDouble n) {
        TestData.OfDouble data = TestData.Factory.ofNode("Node", n);

        exerciseOps(data, s -> s);

        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "nodes", groups={ "serialization-hostile" })
    // throws SOE on serialization of DoubleConcNode[size=1000]
    public void testSpliterator(double[] array, Node.OfDouble n) {
        SpliteratorTestHelper.testDoubleSpliterator(n::spliterator);
    }

    @Test(dataProvider = "nodes")
    public void testTruncate(double[] array, Node.OfDouble n) {
        int[] nums = new int[] { 0, 1, array.length / 2, array.length - 1, array.length };
        for (int start : nums)
            for (int end : nums) {
                if (start < 0 || end < 0 || end < start || end > array.length)
                    continue;
                Node.OfDouble slice = n.truncate(start, end, Double[]::new);
                double[] asArray = slice.asPrimitiveArray();
                for (int k = start; k < end; k++)
                    assertEquals(array[k], asArray[k - start]);
            }
    }
}
