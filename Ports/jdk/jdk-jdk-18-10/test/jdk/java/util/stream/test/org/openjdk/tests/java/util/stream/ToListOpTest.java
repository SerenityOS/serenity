/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.*;

import static java.util.stream.LambdaTestHelpers.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * ToListOpTest
 */
@Test
public class ToListOpTest extends OpTestCase {

    public void testToList() {
        assertCountSum(countTo(0).stream().toList(), 0, 0);
        assertCountSum(countTo(10).stream().toList(), 10, 55);
    }

    private void checkUnmodifiable(List<Integer> list) {
        try {
            list.add(Integer.MIN_VALUE);
            fail("List.add did not throw UnsupportedOperationException");
        } catch (UnsupportedOperationException ignore) { }

        if (list.size() > 0) {
            try {
                list.set(0, Integer.MAX_VALUE);
                fail("List.set did not throw UnsupportedOperationException");
            } catch (UnsupportedOperationException ignore) { }
        }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        List<Integer> objects = exerciseTerminalOps(data, s -> s.toList());
        checkUnmodifiable(objects);
        assertFalse(objects.contains(null));
    }

    @Test(dataProvider = "withNull:StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOpsWithNull(String name, TestData.OfRef<Integer> data) {
        List<Integer> objects = exerciseTerminalOps(data, s -> s.toList());
        checkUnmodifiable(objects);
        assertTrue(objects.contains(null));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testDefaultOps(String name, TestData.OfRef<Integer> data) {
        List<Integer> objects = exerciseTerminalOps(data, s -> DefaultMethodStreams.delegateTo(s).toList());
        checkUnmodifiable(objects);
        assertFalse(objects.contains(null));
    }

    @Test(dataProvider = "withNull:StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testDefaultOpsWithNull(String name, TestData.OfRef<Integer> data) {
        List<Integer> objects = exerciseTerminalOps(data, s -> DefaultMethodStreams.delegateTo(s).toList());
        checkUnmodifiable(objects);
        assertTrue(objects.contains(null));
    }

}
