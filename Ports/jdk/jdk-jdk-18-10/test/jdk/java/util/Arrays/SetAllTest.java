/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8012650
 * @summary Unit test for setAll, parallelSetAll variants
 * @run testng SetAllTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

@Test
public class SetAllTest {
    private static final IntFunction<String> toString = i -> "N" + Integer.valueOf(i);
    private static final IntFunction<String> fillString = i -> "X";
    private static final String[] r0 = {};
    private static final String[] r1 = { "N0" };
    private static final String[] r10 = { "N0", "N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8", "N9" };

    private Object[][] stringData = new Object[][] {
        { "empty", 0, toString, r0 },
        { "one", 1, toString, r1 },
        { "ten", 10, toString, r10 },
        { "fill", 3, fillString, new String[] { "X", "X", "X" }}
    };

    private static final IntUnaryOperator toInt = i -> i << 1;
    private static final IntUnaryOperator fillInt = i -> 99;
    private static final int[] ir0 = {};
    private static final int[] ir1 = { 0 };
    private static final int[] ir10 = { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18 };
    private Object[][] intData = new Object[][] {
        { "empty", 0, toInt, ir0 },
        { "one", 1, toInt, ir1 },
        { "ten", 10, toInt, ir10 },
        { "fill", 3, fillInt, new int[] { 99, 99, 99 }}
    };

    private static final IntToLongFunction toLong = i -> i << 1;
    private static final IntToLongFunction fillLong = i -> 9999L;
    private static final long[] lr0 = {};
    private static final long[] lr1 = { 0L };
    private static final long[] lr10 = { 0L, 2L, 4L, 6L, 8L, 10L, 12L, 14L, 16L, 18L };
    private Object[][] longData = new Object[][] {
        { "empty", 0, toLong, lr0 },
        { "one", 1, toLong, lr1 },
        { "ten", 10, toLong, lr10 },
        { "fill", 3, fillLong, new long[] { 9999L, 9999L, 9999L }}
    };

    private static final IntToDoubleFunction toDouble = i -> i * 1.1;
    private static final IntToDoubleFunction fillDouble = i -> 3.14;
    private static final double[] dr0 = {};
    private static final double[] dr1 = { 0.0 };
    private static final double[] dr10 = { 0.0, 1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9 };
    private Object[][] doubleData = new Object[][] {
        { "empty", 0, toDouble, dr0 },
        { "one", 1, toDouble, dr1 },
        { "ten", 10, toDouble, dr10 },
        { "fill", 3, fillDouble, new double[] { 3.14, 3.14, 3.14 }}
    };

    @DataProvider(name="string")
    public Object[][] stringTests() { return stringData; }

    @DataProvider(name="int")
    public Object[][] intTests() { return intData; }

    @DataProvider(name="long")
    public Object[][] longTests() { return longData; }

    @DataProvider(name="double")
    public Object[][] doubleTests() { return doubleData; }

    @Test(dataProvider = "string")
    public void testSetAllString(String name, int size, IntFunction<String> generator, String[] expected) {
        String[] result = new String[size];
        Arrays.setAll(result, generator);
        assertEquals(result, expected, "setAll(String[], IntFunction<String>) case " + name + " failed.");

        // ensure fresh array
        result = new String[size];
        Arrays.parallelSetAll(result, generator);
        assertEquals(result, expected, "parallelSetAll(String[], IntFunction<String>) case " + name + " failed.");
    }

    @Test(dataProvider = "int")
    public void testSetAllInt(String name, int size, IntUnaryOperator generator, int[] expected) {
        int[] result = new int[size];
        Arrays.setAll(result, generator);
        assertEquals(result, expected, "setAll(int[], IntUnaryOperator) case " + name + " failed.");

        // ensure fresh array
        result = new int[size];
        Arrays.parallelSetAll(result, generator);
        assertEquals(result, expected, "parallelSetAll(int[], IntUnaryOperator) case " + name + " failed.");
    }

    @Test(dataProvider = "long")
    public void testSetAllLong(String name, int size, IntToLongFunction generator, long[] expected) {
        long[] result = new long[size];
        Arrays.setAll(result, generator);
        assertEquals(result, expected, "setAll(long[], IntToLongFunction) case " + name + " failed.");

        // ensure fresh array
        result = new long[size];
        Arrays.parallelSetAll(result, generator);
        assertEquals(result, expected, "parallelSetAll(long[], IntToLongFunction) case " + name + " failed.");
    }

    private void assertDoubleArrayEquals(double[] actual, double[] expected, double delta, String msg) {
        if (actual.length != expected.length) {
            fail(msg + ": length mismatch, expected " + expected.length + ", got " + actual.length);
        }

        for (int i = 0; i < actual.length; i++) {
            assertEquals(actual[i], expected[i], delta, msg + "(mismatch at index " + i + ")");
        }
    }

    @Test(dataProvider = "double")
    public void testSetAllDouble(String name, int size, IntToDoubleFunction generator, double[] expected) {
        double[] result = new double[size];
        Arrays.setAll(result, generator);
        assertDoubleArrayEquals(result, expected, 0.05, "setAll(double[], IntToDoubleFunction) case " + name + " failed.");

        // ensure fresh array
        result = new double[size];
        Arrays.parallelSetAll(result, generator);
        assertDoubleArrayEquals(result, expected, 0.05, "setAll(double[], IntToDoubleFunction) case " + name + " failed.");
    }

    @Test
    public void testStringSetNulls() {
        String[] ar = new String[2];
        try {
            Arrays.setAll(null, (IntFunction<String>) i -> "X");
            fail("Arrays.setAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(null, (IntFunction<String>) i -> "X");
            fail("Arrays.parallelSetAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.setAll(ar, null);
            fail("Arrays.setAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(ar, null);
            fail("Arrays.parallelSetAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void testIntSetNulls() {
        int[] ar = new int[2];
        try {
            Arrays.setAll(null, (IntUnaryOperator) i -> i);
            fail("Arrays.setAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(null, (IntUnaryOperator) i -> i);
            fail("Arrays.parallelSetAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.setAll(ar, null);
            fail("Arrays.setAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(ar, null);
            fail("Arrays.parallelSetAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void testLongSetNulls() {
        long[] ar = new long[2];
        try {
            Arrays.setAll(null, (IntToLongFunction) i -> Long.MAX_VALUE);
            fail("Arrays.setAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(null, (IntToLongFunction) i -> Long.MAX_VALUE);
            fail("Arrays.parallelSetAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.setAll(ar, null);
            fail("Arrays.setAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(ar, null);
            fail("Arrays.parallelSetAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void testDoubleSetNulls() {
        double[] ar = new double[2];
        try {
            Arrays.setAll(null, (IntToDoubleFunction) i -> Math.E);
            fail("Arrays.setAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(null, (IntToDoubleFunction) i -> Math.E);
            fail("Arrays.parallelSetAll(null, foo) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.setAll(ar, null);
            fail("Arrays.setAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Arrays.parallelSetAll(ar, null);
            fail("Arrays.parallelSetAll(array, null) should throw NPE");
        } catch (NullPointerException npe) {
            // expected
        }
    }
}
