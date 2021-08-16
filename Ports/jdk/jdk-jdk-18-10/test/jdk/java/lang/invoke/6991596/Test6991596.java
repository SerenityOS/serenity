/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6991596
 * @summary JSR 292 unimplemented adapter_opt_i2i and adapter_opt_l2i on SPARC
 *
 * @run main/othervm -ea -XX:+UnlockDiagnosticVMOptions -XX:+VerifyMethodHandles Test6991596
 */

import java.lang.invoke.*;

public class Test6991596 {
    private static final Class   CLASS = Test6991596.class;
    private static final String  NAME  = "foo";
    private static final boolean DEBUG = System.getProperty("DEBUG", "false").equals("true");

    public static void main(String[] args) throws Throwable {
        testboolean();
        testbyte();
        testchar();
        testshort();
        testint();
        testlong();
    }

    // Helpers to get various methods.
    static MethodHandle getmh1(Class ret, Class arg) throws ReflectiveOperationException {
        return MethodHandles.lookup().findStatic(CLASS, NAME, MethodType.methodType(ret, arg));
    }
    static MethodHandle getmh2(MethodHandle mh1, Class ret, Class arg) {
        return MethodHandles.explicitCastArguments(mh1, MethodType.methodType(ret, arg));
    }
    static MethodHandle getmh3(MethodHandle mh1, Class ret, Class arg) {
        return MethodHandles.explicitCastArguments(mh1, MethodType.methodType(ret, arg));
    }

    // test adapter_opt_i2i
    static void testboolean() throws Throwable {
        boolean[] a = new boolean[] {
            true,
            false
        };
        for (int i = 0; i < a.length; i++) {
            doboolean(a[i]);
        }
    }
    static void doboolean(boolean x) throws Throwable {
        if (DEBUG)  System.out.println("boolean=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, boolean.class);
            // TODO add this for all cases when the bugs are fixed.
            //MethodHandle mh3 = getmh3(mh1, boolean.class, boolean.class);
            boolean a = (boolean) mh1.invokeExact((boolean) x);
            boolean b = (boolean) mh2.invokeExact(x);
            //boolean c = mh3.<boolean>invokeExact((boolean) x);
            check(x, a, b);
            //check(x, c, x);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class,    byte.class   );
            MethodHandle mh2 = getmh2(mh1, byte.class,    boolean.class);
            byte a = (byte) mh1.invokeExact((byte) (x ? 1 : 0));
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, boolean.class);
            char a = (char) mh1.invokeExact((char) (x ? 1 : 0));
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, boolean.class);
            short a = (short) mh1.invokeExact((short) (x ? 1 : 0));
            short b = (short) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    static void testbyte() throws Throwable {
        byte[] a = new byte[] {
            Byte.MIN_VALUE,
            Byte.MIN_VALUE + 1,
            -0x0F,
            -1,
            0,
            1,
            0x0F,
            Byte.MAX_VALUE - 1,
            Byte.MAX_VALUE
        };
        for (int i = 0; i < a.length; i++) {
            dobyte(a[i]);
        }
    }
    static void dobyte(byte x) throws Throwable {
        if (DEBUG)  System.out.println("byte=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, byte.class);
            boolean a = (boolean) mh1.invokeExact((x & 1) == 1);
            boolean b = (boolean) mh2.invokeExact(x);
            check(x, a, b);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class, byte.class);
            MethodHandle mh2 = getmh2(mh1, byte.class, byte.class);
            byte a = (byte) mh1.invokeExact((byte) x);
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, byte.class);
            char a = (char) mh1.invokeExact((char) x);
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, byte.class);
            short a = (short) mh1.invokeExact((short) x);
            short b = (short) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    static void testchar() throws Throwable {
        char[] a = new char[] {
            Character.MIN_VALUE,
            Character.MIN_VALUE + 1,
            0x000F,
            0x00FF,
            0x0FFF,
            Character.MAX_VALUE - 1,
            Character.MAX_VALUE
        };
        for (int i = 0; i < a.length; i++) {
            dochar(a[i]);
        }
    }
    static void dochar(char x) throws Throwable {
        if (DEBUG)  System.out.println("char=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, char.class);
            boolean a = (boolean) mh1.invokeExact((x & 1) == 1);
            boolean b = (boolean) mh2.invokeExact(x);
            check(x, a, b);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class, byte.class);
            MethodHandle mh2 = getmh2(mh1, byte.class, char.class);
            byte a = (byte) mh1.invokeExact((byte) x);
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, char.class);
            char a = (char) mh1.invokeExact((char) x);
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, char.class);
            short a = (short) mh1.invokeExact((short) x);
            short b = (short) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    static void testshort() throws Throwable {
        short[] a = new short[] {
            Short.MIN_VALUE,
            Short.MIN_VALUE + 1,
            -0x0FFF,
            -0x00FF,
            -0x000F,
            -1,
            0,
            1,
            0x000F,
            0x00FF,
            0x0FFF,
            Short.MAX_VALUE - 1,
            Short.MAX_VALUE
        };
        for (int i = 0; i < a.length; i++) {
            doshort(a[i]);
        }
    }
    static void doshort(short x) throws Throwable {
        if (DEBUG)  System.out.println("short=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, short.class);
            boolean a = (boolean) mh1.invokeExact((x & 1) == 1);
            boolean b = (boolean) mh2.invokeExact(x);
            check(x, a, b);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class, byte.class);
            MethodHandle mh2 = getmh2(mh1, byte.class, short.class);
            byte a = (byte) mh1.invokeExact((byte) x);
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, short.class);
            char a = (char) mh1.invokeExact((char) x);
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, short.class);
            short a = (short) mh1.invokeExact((short) x);
            short b = (short) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    static void testint() throws Throwable {
        int[] a = new int[] {
            Integer.MIN_VALUE,
            Integer.MIN_VALUE + 1,
            -0x0FFFFFFF,
            -0x00FFFFFF,
            -0x000FFFFF,
            -0x0000FFFF,
            -0x00000FFF,
            -0x000000FF,
            -0x0000000F,
            -1,
            0,
            1,
            0x0000000F,
            0x000000FF,
            0x00000FFF,
            0x0000FFFF,
            0x000FFFFF,
            0x00FFFFFF,
            0x0FFFFFFF,
            Integer.MAX_VALUE - 1,
            Integer.MAX_VALUE
        };
        for (int i = 0; i < a.length; i++) {
            doint(a[i]);
        }
    }
    static void doint(int x) throws Throwable {
        if (DEBUG)  System.out.println("int=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, int.class);
            boolean a = (boolean) mh1.invokeExact((x & 1) == 1);
            boolean b = (boolean) mh2.invokeExact(x);
            check(x, a, b);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class, byte.class);
            MethodHandle mh2 = getmh2(mh1, byte.class, int.class);
            byte a = (byte) mh1.invokeExact((byte) x);
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, int.class);
            char a = (char) mh1.invokeExact((char) x);
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, int.class);
            short a = (short) mh1.invokeExact((short) x);
            short b = (short) mh2.invokeExact(x);
            assert a == b : a + " != " + b;
            check(x, a, b);
        }

        // int
        {
            MethodHandle mh1 = getmh1(     int.class, int.class);
            MethodHandle mh2 = getmh2(mh1, int.class, int.class);
            int a = (int) mh1.invokeExact((int) x);
            int b = (int) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    // test adapter_opt_l2i
    static void testlong() throws Throwable {
        long[] a = new long[] {
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            -0x000000000FFFFFFFL,
            -0x0000000000FFFFFFL,
            -0x00000000000FFFFFL,
            -0x000000000000FFFFL,
            -0x0000000000000FFFL,
            -0x00000000000000FFL,
            -0x000000000000000FL,
            -1L,
            0L,
            1L,
            0x000000000000000FL,
            0x00000000000000FFL,
            0x0000000000000FFFL,
            0x0000000000000FFFL,
            0x000000000000FFFFL,
            0x00000000000FFFFFL,
            0x0000000000FFFFFFL,
            0x000000000FFFFFFFL,
            Long.MAX_VALUE - 1,
            Long.MAX_VALUE
        };
        for (int i = 0; i < a.length; i++) {
            dolong(a[i]);
        }
    }
    static void dolong(long x) throws Throwable {
        if (DEBUG)  System.out.println("long=" + x);

        // boolean
        {
            MethodHandle mh1 = getmh1(     boolean.class, boolean.class);
            MethodHandle mh2 = getmh2(mh1, boolean.class, long.class);
            boolean a = (boolean) mh1.invokeExact((x & 1L) == 1L);
            boolean b = (boolean) mh2.invokeExact(x);
            check(x, a, b);
        }

        // byte
        {
            MethodHandle mh1 = getmh1(     byte.class, byte.class);
            MethodHandle mh2 = getmh2(mh1, byte.class, long.class);
            byte a = (byte) mh1.invokeExact((byte) x);
            byte b = (byte) mh2.invokeExact(x);
            check(x, a, b);
        }

        // char
        {
            MethodHandle mh1 = getmh1(     char.class, char.class);
            MethodHandle mh2 = getmh2(mh1, char.class, long.class);
            char a = (char) mh1.invokeExact((char) x);
            char b = (char) mh2.invokeExact(x);
            check(x, a, b);
        }

        // short
        {
            MethodHandle mh1 = getmh1(     short.class, short.class);
            MethodHandle mh2 = getmh2(mh1, short.class, long.class);
            short a = (short) mh1.invokeExact((short) x);
            short b = (short) mh2.invokeExact(x);
            check(x, a, b);
        }

        // int
        {
            MethodHandle mh1 = getmh1(     int.class, int.class);
            MethodHandle mh2 = getmh2(mh1, int.class, long.class);
            int a = (int) mh1.invokeExact((int) x);
            int b = (int) mh2.invokeExact(x);
            check(x, a, b);
        }
    }

    static void check(boolean x, boolean e, boolean a) { p(z2h(x), z2h(e), z2h(a)); assert e == a : z2h(x) + ": " + z2h(e) + " != " + z2h(a); }
    static void check(boolean x, byte    e, byte    a) { p(z2h(x), i2h(e), i2h(a)); assert e == a : z2h(x) + ": " + i2h(e) + " != " + i2h(a); }
    static void check(boolean x, int     e, int     a) { p(z2h(x), i2h(e), i2h(a)); assert e == a : z2h(x) + ": " + i2h(e) + " != " + i2h(a); }

    static void check(int     x, boolean e, boolean a) { p(i2h(x), z2h(e), z2h(a)); assert e == a : i2h(x) + ": " + z2h(e) + " != " + z2h(a); }
    static void check(int     x, byte    e, byte    a) { p(i2h(x), i2h(e), i2h(a)); assert e == a : i2h(x) + ": " + i2h(e) + " != " + i2h(a); }
    static void check(int     x, int     e, int     a) { p(i2h(x), i2h(e), i2h(a)); assert e == a : i2h(x) + ": " + i2h(e) + " != " + i2h(a); }

    static void check(long    x, boolean e, boolean a) { p(l2h(x), z2h(e), z2h(a)); assert e == a : l2h(x) + ": " + z2h(e) + " != " + z2h(a); }
    static void check(long    x, byte    e, byte    a) { p(l2h(x), i2h(e), i2h(a)); assert e == a : l2h(x) + ": " + i2h(e) + " != " + i2h(a); }
    static void check(long    x, int     e, int     a) { p(l2h(x), i2h(e), i2h(a)); assert e == a : l2h(x) + ": " + i2h(e) + " != " + i2h(a); }

    static void p(String x, String e, String a) { if (DEBUG)  System.out.println(x + ": expected: " + e + ", actual: " + a); }

    static String z2h(boolean x) { return x ? "1" : "0"; }
    static String i2h(int     x) { return Integer.toHexString(x); }
    static String l2h(long    x) { return Long.toHexString(x); }

    // to int
    public static boolean foo(boolean i) { return i; }
    public static byte    foo(byte    i) { return i; }
    public static char    foo(char    i) { return i; }
    public static short   foo(short   i) { return i; }
    public static int     foo(int     i) { return i; }
}
