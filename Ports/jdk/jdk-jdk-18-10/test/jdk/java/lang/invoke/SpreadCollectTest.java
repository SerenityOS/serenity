/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/* @test
 * @bug 8139885
 * @bug 8143798
 * @run testng/othervm -ea -esa test.java.lang.invoke.SpreadCollectTest
 */

package test.java.lang.invoke;

import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.*;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests for the new asSpreader/asCollector API added in JEP 274.
 */
public class SpreadCollectTest {

    static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testAsSpreader() throws Throwable {
        MethodHandle spreader = SpreadCollect.MH_forSpreading.asSpreader(1, int[].class, 3);
        assertEquals(SpreadCollect.MT_spreader, spreader.type());
        assertEquals("A456B", (String) spreader.invoke("A", new int[]{4, 5, 6}, "B"));
    }

    @Test
    public static void testAsSpreaderExample() throws Throwable {
        // test the JavaDoc asSpreader-with-pos example
        MethodHandle compare = LOOKUP.findStatic(Objects.class, "compare", methodType(int.class, Object.class, Object.class, Comparator.class));
        MethodHandle compare2FromArray = compare.asSpreader(0, Object[].class, 2);
        Object[] ints = new Object[]{3, 9, 7, 7};
        Comparator<Integer> cmp = (a, b) -> a - b;
        assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 0, 2), cmp) < 0);
        assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 1, 3), cmp) > 0);
        assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 2, 4), cmp) == 0);
    }

    @DataProvider
    static Object[][] asSpreaderIllegalPositions() {
        return new Object[][]{{-7}, {3}, {19}};
    }

    @Test(dataProvider = "asSpreaderIllegalPositions")
    public static void testAsSpreaderIllegalPos(int p) throws Throwable {
        boolean caught = false;
        try {
            SpreadCollect.MH_forSpreading.asSpreader(p, Object[].class, 3);
        } catch (IllegalArgumentException iae) {
            assertEquals("bad spread position", iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test(expectedExceptions = {WrongMethodTypeException.class})
    public static void testAsSpreaderIllegalMethodType() {
        MethodHandle h = MethodHandles.dropArguments(MethodHandles.constant(String.class, ""), 0, int.class, int.class);
        MethodHandle s = h.asSpreader(String[].class, 1);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testAsSpreaderNullArrayType() {
        SpreadCollect.MH_forSpreading.asSpreader(null, 0);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testAsSpreaderNullArrayNonZeroLength() {
        SpreadCollect.MH_forSpreading.asSpreader(null, 1);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public static void testAsSpreaderTooManyParams() throws Throwable {
        SpreadCollect.MH_forSpreading.asSpreader(1, int[].class, 6);
    }

    @Test
    public static void testAsCollector() throws Throwable {
        MethodHandle collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 1);
        assertEquals(SpreadCollect.MT_collector1, collector.type());
        assertEquals("A4B", (String) collector.invoke("A", 4, "B"));
        collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 2);
        assertEquals(SpreadCollect.MT_collector2, collector.type());
        assertEquals("A45B", (String) collector.invoke("A", 4, 5, "B"));
        collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 3);
        assertEquals(SpreadCollect.MT_collector3, collector.type());
        assertEquals("A456B", (String) collector.invoke("A", 4, 5, 6, "B"));
    }

    @Test
    public static void testAsCollectorInvokeWithArguments() throws Throwable {
        MethodHandle collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 1);
        assertEquals(SpreadCollect.MT_collector1, collector.type());
        assertEquals("A4B", (String) collector.invokeWithArguments("A", 4, "B"));
        collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 2);
        assertEquals(SpreadCollect.MT_collector2, collector.type());
        assertEquals("A45B", (String) collector.invokeWithArguments("A", 4, 5, "B"));
        collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 3);
        assertEquals(SpreadCollect.MT_collector3, collector.type());
        assertEquals("A456B", (String) collector.invokeWithArguments("A", 4, 5, 6, "B"));
    }

    @Test
    public static void testAsCollectorLeading() throws Throwable {
        MethodHandle collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 1);
        assertEquals(SpreadCollect.MT_collectorLeading1, collector.type());
        assertEquals("7Q", (String) collector.invoke(7, "Q"));
        collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 2);
        assertEquals(SpreadCollect.MT_collectorLeading2, collector.type());
        assertEquals("78Q", (String) collector.invoke(7, 8, "Q"));
        collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 3);
        assertEquals(SpreadCollect.MT_collectorLeading3, collector.type());
        assertEquals("789Q", (String) collector.invoke(7, 8, 9, "Q"));
    }

    @Test
    public static void testAsCollectorLeadingInvokeWithArguments() throws Throwable {
        MethodHandle collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 1);
        assertEquals(SpreadCollect.MT_collectorLeading1, collector.type());
        assertEquals("7Q", (String) collector.invokeWithArguments(7, "Q"));
        collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 2);
        assertEquals(SpreadCollect.MT_collectorLeading2, collector.type());
        assertEquals("78Q", (String) collector.invokeWithArguments(7, 8, "Q"));
        collector = SpreadCollect.MH_forCollectingLeading.asCollector(0, int[].class, 3);
        assertEquals(SpreadCollect.MT_collectorLeading3, collector.type());
        assertEquals("789Q", (String) collector.invokeWithArguments(7, 8, 9, "Q"));
    }

    @Test
    public static void testAsCollectorNone() throws Throwable {
        MethodHandle collector = SpreadCollect.MH_forCollecting.asCollector(1, int[].class, 0);
        assertEquals(SpreadCollect.MT_collector0, collector.type());
        assertEquals("AB", (String) collector.invoke("A", "B"));
    }

    @DataProvider
    static Object[][] asCollectorIllegalPositions() {
        return new Object[][]{{-1}, {17}};
    }

    @Test(dataProvider = "asCollectorIllegalPositions")
    public static void testAsCollectorIllegalPos(int p) {
        boolean caught = false;
        try {
            SpreadCollect.MH_forCollecting.asCollector(p, int[].class, 0);
        } catch (IllegalArgumentException iae) {
            assertEquals("bad collect position", iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testAsCollectorExample() throws Throwable {
        // test the JavaDoc asCollector-with-pos example
        StringWriter swr = new StringWriter();
        MethodHandle swWrite = LOOKUP.
                findVirtual(StringWriter.class, "write", methodType(void.class, char[].class, int.class, int.class)).
                bindTo(swr);
        MethodHandle swWrite4 = swWrite.asCollector(0, char[].class, 4);
        swWrite4.invoke('A', 'B', 'C', 'D', 1, 2);
        assertEquals("BC", swr.toString());
        swWrite4.invoke('P', 'Q', 'R', 'S', 0, 4);
        assertEquals("BCPQRS", swr.toString());
        swWrite4.invoke('W', 'X', 'Y', 'Z', 3, 1);
        assertEquals("BCPQRSZ", swr.toString());
    }

    static class SpreadCollect {

        static String forSpreading(String s1, int i1, int i2, int i3, String s2) {
            return s1 + i1 + i2 + i3 + s2;
        }

        static String forCollecting(String s1, int[] is, String s2) {
            StringBuilder sb = new StringBuilder(s1);
            for (int i : is) {
                sb.append(i);
            }
            return sb.append(s2).toString();
        }

        static String forCollectingLeading(int[] is, String s) {
            return forCollecting("", is, s);
        }

        static final Class<SpreadCollect> SPREAD_COLLECT = SpreadCollect.class;

        static final MethodType MT_forSpreading = methodType(String.class, String.class, int.class, int.class, int.class, String.class);
        static final MethodType MT_forCollecting = methodType(String.class, String.class, int[].class, String.class);
        static final MethodType MT_forCollectingLeading = methodType(String.class, int[].class, String.class);

        static final MethodHandle MH_forSpreading;
        static final MethodHandle MH_forCollecting;
        static final MethodHandle MH_forCollectingLeading;

        static final MethodType MT_spreader = methodType(String.class, String.class, int[].class, String.class);
        static final MethodType MT_collector0 = methodType(String.class, String.class, String.class);
        static final MethodType MT_collector1 = methodType(String.class, String.class, int.class, String.class);
        static final MethodType MT_collector2 = methodType(String.class, String.class, int.class, int.class, String.class);
        static final MethodType MT_collector3 = methodType(String.class, String.class, int.class, int.class, int.class, String.class);
        static final MethodType MT_collectorLeading1 = methodType(String.class, int.class, String.class);
        static final MethodType MT_collectorLeading2 = methodType(String.class, int.class, int.class, String.class);
        static final MethodType MT_collectorLeading3 = methodType(String.class, int.class, int.class, int.class, String.class);

        static final String NONE_ERROR = "zero array length in MethodHandle.asCollector";

        static {
            try {
                MH_forSpreading = LOOKUP.findStatic(SPREAD_COLLECT, "forSpreading", MT_forSpreading);
                MH_forCollecting = LOOKUP.findStatic(SPREAD_COLLECT, "forCollecting", MT_forCollecting);
                MH_forCollectingLeading = LOOKUP.findStatic(SPREAD_COLLECT, "forCollectingLeading", MT_forCollectingLeading);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

}
