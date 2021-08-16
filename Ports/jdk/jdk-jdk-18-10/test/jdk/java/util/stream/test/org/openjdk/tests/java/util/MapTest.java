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

package org.openjdk.tests.java.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.LambdaTestHelpers;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for extension methods on Map
 */
public class MapTest {

    private static final Map<Integer, String> EXPECTED = new HashMap<>();

    private Map<Integer, String> map;

    @BeforeClass
    public void setUpClass() {
        EXPECTED.put(0, "zero");
        EXPECTED.put(1, "one");
        EXPECTED.put(2, "two");
        EXPECTED.put(3, "three");
        EXPECTED.put(4, "four");
        EXPECTED.put(5, "five");
        EXPECTED.put(6, "six");
        EXPECTED.put(7, "seven");
        EXPECTED.put(8, "eight");
        EXPECTED.put(9, "nine");
    }

    @AfterClass
    public void tearDownClass() {
        EXPECTED.clear();
    }

    @BeforeMethod
    public void setUp() {
        map = new HashMap<>(EXPECTED);
    }

    @AfterMethod
    public void tearDown() {
        map.clear();
        map = null;
    }

    @Test(groups = { "serialization-hostile" })
    public void testForEach() {
        final Set<String> values = new HashSet<>(EXPECTED.size());
        map.forEach((k, v) -> {values.add(v);});
        LambdaTestHelpers.assertContentsUnordered(values, EXPECTED.values());
    }

    @Test
    public void testReplaceAll() {
        map.replaceAll((k, v) -> {return v.toUpperCase();});
        for (final Map.Entry<Integer, String> entry : map.entrySet()) {
            assertEquals(entry.getValue(), EXPECTED.get(entry.getKey()).toUpperCase());
        }
    }
}
