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

import java.util.stream.LambdaTestHelpers;
import java.util.stream.OpTestCase;
import java.util.stream.StreamTestDataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.TestData;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * SequentialOpTest
 *
 * @author Brian Goetz
 */
public class SequentialOpTest extends OpTestCase {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testLazy(String name, TestData.OfRef<Integer> data) {
        Function<Integer, Integer> id = LambdaTestHelpers.identity();
        AtomicInteger counter = new AtomicInteger();
        Supplier<Stream<Integer>>[] suppliers = new Supplier[] { () -> data.stream(), () -> data.parallelStream() };
        UnaryOperator<Stream<Integer>>[] configs
                = new UnaryOperator[] {
                    (UnaryOperator<Stream<Integer>>) s -> s.peek(e -> { counter.incrementAndGet(); }),
                    (UnaryOperator<Stream<Integer>>) s -> s.map(id).peek(e -> { counter.incrementAndGet(); }).sequential().map(id),
                    (UnaryOperator<Stream<Integer>>) s -> s.map(id).peek(e -> { counter.incrementAndGet(); }).parallel().map(id),
                    (UnaryOperator<Stream<Integer>>) s -> s.sequential().map(id).peek(e -> {
                        counter.incrementAndGet();
                    }).map(id),
                    (UnaryOperator<Stream<Integer>>) s -> s.parallel().map(id).peek(e -> { counter.incrementAndGet(); }).map(id)
        };

        for (int i = 0; i < suppliers.length; i++) {
            setContext("supplierIndex", i);
            Supplier<Stream<Integer>> supp = suppliers[i];
            for (int j = 0; j < configs.length; j++) {
                setContext("configIndex", j);
                UnaryOperator<Stream<Integer>> config = configs[j];
                counter.set(0);
                Stream<Integer> stream = config.apply(supp.get());
                assertEquals(0, counter.get());

                Iterator<Integer> iterator = stream.iterator();
                assertEquals(0, counter.get());

                if (iterator.hasNext())
                    iterator.next();
                assertTrue(data.size() == 0 || counter.get() > 0);

                counter.set(0);
                stream = config.apply(supp.get());
                Spliterator<Integer> spliterator = stream.spliterator();
                assertEquals(0, counter.get());

                spliterator.forEachRemaining(e -> {
                });
                assertTrue(data.size() == 0 || counter.get() > 0);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(dataProvider = "StreamTestData<Integer>.mini", dataProviderClass = StreamTestDataProvider.class)
    public void testMixedSeqPar(String name, TestData.OfRef<Integer> data) {
        Function<Integer, Integer> id = LambdaTestHelpers.identity();
        UnaryOperator<Stream<Integer>>[] changers
                = new UnaryOperator[] {
                (UnaryOperator<Stream<Integer>>) s -> s,
                (UnaryOperator<Stream<Integer>>) s -> s.sequential(),
                (UnaryOperator<Stream<Integer>>) s -> s.parallel(),
                (UnaryOperator<Stream<Integer>>) s -> s.unordered()
        };
        UnaryOperator<Stream<Integer>>[] stuff
                = new UnaryOperator[] {
                (UnaryOperator<Stream<Integer>>) s -> s,
                (UnaryOperator<Stream<Integer>>) s -> s.map(id),
                (UnaryOperator<Stream<Integer>>) s -> s.sorted(Comparator.naturalOrder()),
                (UnaryOperator<Stream<Integer>>) s -> s.map(id).sorted(Comparator.naturalOrder()).map(id),
                (UnaryOperator<Stream<Integer>>) s -> s.filter(LambdaTestHelpers.pEven).sorted(Comparator.naturalOrder()).map(id),
        };

        for (int c1Index = 0; c1Index < changers.length; c1Index++) {
            setContext("c1Index", c1Index);
            UnaryOperator<Stream<Integer>> c1 = changers[c1Index];
            for (int s1Index = 0; s1Index < stuff.length; s1Index++) {
                setContext("s1Index", s1Index);
                UnaryOperator<Stream<Integer>> s1 = stuff[s1Index];
                for (int c2Index = 0; c2Index < changers.length; c2Index++) {
                    setContext("c2Index", c2Index);
                    UnaryOperator<Stream<Integer>> c2 = changers[c2Index];
                    for (int s2Index = 0; s2Index < stuff.length; s2Index++) {
                        setContext("s2Index", s2Index);
                        UnaryOperator<Stream<Integer>> s2 = stuff[s2Index];
                        UnaryOperator<Stream<Integer>> composed = s -> s2.apply(c2.apply(s1.apply(c1.apply(s))));
                        exerciseOps(data, composed);
                    }
                }
            }
        }
    }
}
