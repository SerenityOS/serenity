/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194554
 * @run testng/othervm test.java.lang.invoke.FilterArgumentsTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class FilterArgumentsTest {

    @Test
    public static void testFilterA_B_C() throws Throwable {
        FilterTest test = new FilterTest(
            filterArguments(MH_TEST, 0, MH_FILTER_A, MH_FILTER_B, MH_FILTER_C));
        test.run(List.of("A", "B", "C"));
    }

    @Test
    public static void testFilterA_B() throws Throwable {
        FilterTest test = new FilterTest(
            filterArguments(MH_TEST, 0, MH_FILTER_A, MH_FILTER_B));
        test.run(List.of("A", "B"));
    }

    @Test
    public static void testFilterB_C() throws Throwable {
        FilterTest test = new FilterTest(
            filterArguments(MH_TEST, 1, MH_FILTER_B, MH_FILTER_C));
        test.run(List.of("B", "C"));
    }

    @Test
    public static void testFilterB() throws Throwable {
        FilterTest test = new FilterTest(filterArguments(MH_TEST, 1, MH_FILTER_B));
        test.run(List.of("B"));
    }

    @Test
    public static void testFilterC() throws Throwable {
        FilterTest test = new FilterTest(filterArguments(MH_TEST, 2, MH_FILTER_C));
        test.run(List.of("C"));
    }

    static class FilterTest {
        static List<String> filters = new ArrayList<>();

        final MethodHandle mh;
        FilterTest(MethodHandle mh) {
            this.mh = mh;
        }

        void run(List<String> expected) throws Throwable {
            filters.clear();
            assertEquals((String)mh.invokeExact("x", 0, 'z'), "x-0-z");
            assertEquals(filters, expected);
        }

        static String filterA(String s) {
            filters.add("A");
            return s;
        }

        static int filterB(int value) {
            filters.add("B");
            return value;
        }

        static char filterC(char c) {
            filters.add("C");
            return c;
        }

        static String test(String s, int i, char c) {
            return s + "-" + i + "-" + c;
        }
    }

    static final MethodHandle MH_TEST;
    static final MethodHandle MH_FILTER_A;
    static final MethodHandle MH_FILTER_B;
    static final MethodHandle MH_FILTER_C;
    static final Lookup LOOKUP = MethodHandles.lookup();

    static {
        try {
            MH_TEST = LOOKUP.findStatic(FilterTest.class, "test",
                methodType(String.class, String.class, int.class, char.class));
            MH_FILTER_A = LOOKUP.findStatic(FilterTest.class, "filterA",
                methodType(String.class, String.class));
            MH_FILTER_B = LOOKUP.findStatic(FilterTest.class, "filterB",
                methodType(int.class, int.class));
            MH_FILTER_C = LOOKUP.findStatic(FilterTest.class, "filterC",
                methodType(char.class, char.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
