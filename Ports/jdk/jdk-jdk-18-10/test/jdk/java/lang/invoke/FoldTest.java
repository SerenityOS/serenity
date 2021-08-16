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
 * @run testng/othervm -ea -esa test.java.lang.invoke.FoldTest
 */

package test.java.lang.invoke;

import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests for the new fold method handle combinator added in JEP 274.
 */
public class FoldTest {

    static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testFold0a() throws Throwable {
        // equivalence to foldArguments(MethodHandle,MethodHandle)
        MethodHandle fold = MethodHandles.foldArguments(Fold.MH_multer, 0, Fold.MH_adder);
        assertEquals(Fold.MT_folded1, fold.type());
        assertEquals(720, (int) fold.invoke(3, 4, 5));
    }

    @Test
    public static void testFold1a() throws Throwable {
        // test foldArguments for folding position 1
        MethodHandle fold = MethodHandles.foldArguments(Fold.MH_multer, 1, Fold.MH_adder1);
        assertEquals(Fold.MT_folded1, fold.type());
        assertEquals(540, (int) fold.invoke(3, 4, 5));
    }

    @Test
    public static void testFold0b() throws Throwable {
        // test foldArguments equivalence with multiple types
        MethodHandle fold = MethodHandles.foldArguments(Fold.MH_str, 0, Fold.MH_comb);
        assertEquals(Fold.MT_folded2, fold.type());
        assertEquals(23, (int) fold.invoke("true", true, 23));
    }

    @Test
    public static void testFold1b() throws Throwable {
        // test folgArguments for folding position 1, with multiple types
        MethodHandle fold = MethodHandles.foldArguments(Fold.MH_str, 1, Fold.MH_comb2);
        assertEquals(Fold.MT_folded3, fold.type());
        assertEquals(1, (int) fold.invoke(true, true, 1));
        assertEquals(-1, (int) fold.invoke(true, false, -1));
    }

    @Test
    public static void testFoldArgumentsExample() throws Throwable {
        // test the JavaDoc foldArguments-with-pos example
        StringWriter swr = new StringWriter();
        MethodHandle trace = LOOKUP.findVirtual(StringWriter.class, "write", methodType(void.class, String.class)).bindTo(swr);
        MethodHandle cat = LOOKUP.findVirtual(String.class, "concat", methodType(String.class, String.class));
        assertEquals("boojum", (String) cat.invokeExact("boo", "jum"));
        MethodHandle catTrace = MethodHandles.foldArguments(cat, 1, trace);
        assertEquals("boojum", (String) catTrace.invokeExact("boo", "jum"));
        assertEquals("jum", swr.toString());
    }

    static class Fold {

        static int adder(int a, int b, int c) {
            return a + b + c;
        }

        static int adder1(int a, int b) {
            return a + b;
        }

        static int multer(int x, int q, int r, int s) {
            return x * q * r * s;
        }

        static int str(boolean b1, String s, boolean b2, int x) {
            return b1 && s.equals(String.valueOf(b2)) ? x : -x;
        }

        static boolean comb(String s, boolean b2) {
            return !s.equals(b2);
        }

        static String comb2(boolean b2, int x) {
            int ib = b2 ? 1 : 0;
            return ib == x ? "true" : "false";
        }

        static final Class<Fold> FOLD = Fold.class;

        static final MethodType MT_adder = methodType(int.class, int.class, int.class, int.class);
        static final MethodType MT_adder1 = methodType(int.class, int.class, int.class);
        static final MethodType MT_multer = methodType(int.class, int.class, int.class, int.class, int.class);
        static final MethodType MT_str = methodType(int.class, boolean.class, String.class, boolean.class, int.class);
        static final MethodType MT_comb = methodType(boolean.class, String.class, boolean.class);
        static final MethodType MT_comb2 = methodType(String.class, boolean.class, int.class);

        static final MethodHandle MH_adder;
        static final MethodHandle MH_adder1;
        static final MethodHandle MH_multer;
        static final MethodHandle MH_str;
        static final MethodHandle MH_comb;
        static final MethodHandle MH_comb2;

        static final MethodType MT_folded1 = methodType(int.class, int.class, int.class, int.class);
        static final MethodType MT_folded2 = methodType(int.class, String.class, boolean.class, int.class);
        static final MethodType MT_folded3 = methodType(int.class, boolean.class, boolean.class, int.class);

        static {
            try {
                MH_adder = LOOKUP.findStatic(FOLD, "adder", MT_adder);
                MH_adder1 = LOOKUP.findStatic(FOLD, "adder1", MT_adder1);
                MH_multer = LOOKUP.findStatic(FOLD, "multer", MT_multer);
                MH_str = LOOKUP.findStatic(FOLD, "str", MT_str);
                MH_comb = LOOKUP.findStatic(FOLD, "comb", MT_comb);
                MH_comb2 = LOOKUP.findStatic(FOLD, "comb2", MT_comb2);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

}
