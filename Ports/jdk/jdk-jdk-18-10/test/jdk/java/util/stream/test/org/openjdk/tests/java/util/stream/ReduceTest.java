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
package org.openjdk.tests.java.util.stream;

import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamTestDataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.TestData;

import static java.util.stream.LambdaTestHelpers.*;

/**
 * ReduceOpTest
 *
 * @author Brian Goetz
 */
@Test
public class ReduceTest extends OpTestCase {
    public void testReduce() {
        List<Integer> list = countTo(10);

        assertEquals(55, (int) list.stream().reduce(rPlus).get());
        assertEquals(55, (int) list.stream().reduce(0, rPlus));
        assertEquals(10, (int) list.stream().reduce(rMax).get());
        assertEquals(1, (int) list.stream().reduce(rMin).get());

        assertEquals(0, (int) countTo(0).stream().reduce(0, rPlus));
        assertTrue(!countTo(0).stream().reduce(rPlus).isPresent());

        assertEquals(110, (int) list.stream().map(mDoubler).reduce(rPlus).get());
        assertEquals(20, (int) list.stream().map(mDoubler).reduce(rMax).get());
        assertEquals(2, (int) list.stream().map(mDoubler).reduce(rMin).get());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        assertEquals(0, (int) exerciseTerminalOps(data, s -> s.filter(pFalse), s -> s.reduce(0, rPlus, rPlus)));

        Optional<Integer> seedless = exerciseTerminalOps(data, s -> s.reduce(rPlus));
        Integer folded = exerciseTerminalOps(data, s -> s.reduce(0, rPlus, rPlus));
        assertEquals(folded, seedless.orElse(0));

        seedless = exerciseTerminalOps(data, s -> s.reduce(rMin));
        folded = exerciseTerminalOps(data, s -> s.reduce(Integer.MAX_VALUE, rMin, rMin));
        assertEquals(folded, seedless.orElse(Integer.MAX_VALUE));

        seedless = exerciseTerminalOps(data, s -> s.reduce(rMax));
        folded = exerciseTerminalOps(data, s -> s.reduce(Integer.MIN_VALUE, rMax, rMax));
        assertEquals(folded, seedless.orElse(Integer.MIN_VALUE));

        seedless = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(rPlus));
        folded = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(0, rPlus, rPlus));
        assertEquals(folded, seedless.orElse(0));

        seedless = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(rMin));
        folded = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(Integer.MAX_VALUE, rMin, rMin));
        assertEquals(folded, seedless.orElse(Integer.MAX_VALUE));

        seedless = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(rMax));
        folded = exerciseTerminalOps(data, s -> s.map(mDoubler), s -> s.reduce(Integer.MIN_VALUE, rMax, rMax));
        assertEquals(folded, seedless.orElse(Integer.MIN_VALUE));
    }
}
