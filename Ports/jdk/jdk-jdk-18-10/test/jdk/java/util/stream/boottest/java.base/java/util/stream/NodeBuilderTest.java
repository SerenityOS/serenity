/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.stream.LambdaTestHelpers.assertContents;
import static java.util.stream.LambdaTestHelpers.countTo;
import static org.testng.Assert.assertEquals;

@Test
public class NodeBuilderTest {

    List<Integer> sizes = Arrays.asList(0, 1, 4, 16, 256,
                                        1023, 1024, 1025,
                                        2047, 2048, 2049,
                                        1024 * 32 - 1, 1024 * 32, 1024 * 32 + 1);

    @DataProvider(name = "Node.Builder")
    public Object[][] createNodeBuilders() {
        List<List<Integer>> ls = new ArrayList<>();
        for (int size : sizes) {
            ls.add(countTo(size));
        }

        List<Function<Integer, Node.Builder<Integer>>> ms = Arrays.asList(
                s -> Nodes.builder(),
                s -> Nodes.builder(s, LambdaTestHelpers.integerArrayGenerator)
        );

        Object[][] params = new Object[ls.size() * ms.size()][];
        int i = 0;
        for (List<Integer> l : ls) {
            for (Function<Integer, Node.Builder<Integer>> m : ms) {
                params[i++] = new Object[]{l, m};
            }
        }

        return params;
    }

    @Test(dataProvider = "Node.Builder", groups = { "serialization-hostile" })
    public void testIteration(List<Integer> l, Function<Integer, Node.Builder<Integer>> m) {
        Node.Builder<Integer> nb = m.apply(l.size());
        nb.begin(l.size());
        for (Integer i : l) {
            nb.accept(i);
        }
        nb.end();

        Node<Integer> n = nb.build();
        assertEquals(n.count(), l.size());

        {
            List<Integer> _l = new ArrayList<>();
            n.forEach(_l::add);

            assertContents(_l, l);
        }
    }

    // Node.Builder.OfInt

    @DataProvider(name = "Node.Builder<Integer>")
    public Object[][] createIntNodeBuilders() {
        List<List<Integer>> ls = new ArrayList<>();
        for (int size : sizes) {
            ls.add(countTo(size));
        }

        List<Function<Integer, Node.Builder<Integer>>> ms = Arrays.asList(
                s -> Nodes.intBuilder(),
                s -> Nodes.intBuilder(s)
        );

        Object[][] params = new Object[ls.size() * ms.size()][];
        int i = 0;
        for (List<Integer> l : ls) {
            for (Function<Integer, Node.Builder<Integer>> m : ms) {
                params[i++] = new Object[]{l, m};
            }
        }

        return params;
    }

    @Test(dataProvider = "Node.Builder<Integer>", groups = { "serialization-hostile" })
    public void testIntIteration(List<Integer> l, Function<Integer, Node.Builder.OfInt> m) {
        Node.Builder.OfInt nb = m.apply(l.size());
        nb.begin(l.size());
        for (Integer i : l) {
            nb.accept((int) i);
        }
        nb.end();

        Node.OfInt n = nb.build();
        assertEquals(n.count(), l.size());

        {
            List<Integer> _l = new ArrayList<>();
            n.forEach((IntConsumer) _l::add);

            assertContents(_l, l);
        }

    }

    // Node.Builder.OfLong

    @DataProvider(name = "Node.Builder<Long>")
    public Object[][] createLongNodeBuilders() {
        List<List<Long>> ls = new ArrayList<>();
        for (int size : sizes) {
            List<Long> l = new ArrayList<>();
            for (long i = 0; i < size; i++) {
                l.add(i);
            }
            ls.add(l);
        }

        List<Function<Integer, Node.Builder<Long>>> ms = Arrays.asList(
                s -> Nodes.longBuilder(),
                s -> Nodes.longBuilder(s)
        );

        Object[][] params = new Object[ls.size() * ms.size()][];
        int i = 0;
        for (List<Long> l : ls) {
            for (Function<Integer, Node.Builder<Long>> m : ms) {
                params[i++] = new Object[]{l, m};
            }
        }

        return params;
    }

    @Test(dataProvider = "Node.Builder<Long>")
    public void testLongIteration(List<Long> l, Function<Integer, Node.Builder.OfLong> m) {
        Node.Builder.OfLong nb = m.apply(l.size());
        nb.begin(l.size());
        for (Long i : l) {
            nb.accept((long) i);
        }
        nb.end();

        Node.OfLong n = nb.build();
        assertEquals(n.count(), l.size());

        {
            List<Long> _l = new ArrayList<>();
            n.forEach((LongConsumer) _l::add);

            assertContents(_l, l);
        }

    }

    // Node.Builder.OfDouble

    @DataProvider(name = "Node.Builder<Double>")
    public Object[][] createDoubleNodeBuilders() {
        List<List<Double>> ls = new ArrayList<>();
        for (int size : sizes) {
            List<Double> l = new ArrayList<>();
            for (long i = 0; i < size; i++) {
                l.add((double) i);
            }
            ls.add(l);
        }

        List<Function<Integer, Node.Builder<Double>>> ms = Arrays.asList(
                s -> Nodes.doubleBuilder(),
                s -> Nodes.doubleBuilder(s)
        );

        Object[][] params = new Object[ls.size() * ms.size()][];
        int i = 0;
        for (List<Double> l : ls) {
            for (Function<Integer, Node.Builder<Double>> m : ms) {
                params[i++] = new Object[]{l, m};
            }
        }

        return params;
    }

    @Test(dataProvider = "Node.Builder<Double>")
    public void testDoubleIteration(List<Double> l, Function<Integer, Node.Builder.OfDouble> m) {
        Node.Builder.OfDouble nb = m.apply(l.size());
        nb.begin(l.size());
        for (Double i : l) {
            nb.accept((double) i);
        }
        nb.end();

        Node.OfDouble n = nb.build();
        assertEquals(n.count(), l.size());

        {
            List<Double> _l = new ArrayList<>();
            n.forEach((DoubleConsumer) _l::add);

            assertContents(_l, l);
        }

    }
}
