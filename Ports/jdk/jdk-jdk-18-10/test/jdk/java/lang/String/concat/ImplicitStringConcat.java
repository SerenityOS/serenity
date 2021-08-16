/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test implicit String concatenations
 *
 * @compile ImplicitStringConcat.java
 * @run main/othervm -Xverify:all ImplicitStringConcat
 *
 * @compile -XDstringConcat=inline ImplicitStringConcat.java
 * @run main/othervm -Xverify:all ImplicitStringConcat
 *
 * @compile -XDstringConcat=indy ImplicitStringConcat.java
 * @run main/othervm -Xverify:all ImplicitStringConcat
 *
 * @compile -XDstringConcat=indyWithConstants ImplicitStringConcat.java
 * @run main/othervm -Xverify:all ImplicitStringConcat
*/
import java.lang.StringBuilder;

public class ImplicitStringConcat {

    static boolean b = true;
    static byte by = 42;
    static short sh = 42;
    static char ch = 'a';
    static int i = 42;
    static float fl = 42.0f;
    static long l = 42;
    static double d = 42.0d;
    static String s = "foo";
    static String sNull = null;
    static Object o = "bar";
    static Object oNull = null;
    static CharSequence cs = "bar";
    static char[] chars = new char[] {'a'};

    static MyClass myCl = new MyClass();
    static MyClassNull myClNull = new MyClassNull();
    static Object  myCl2 = new MyClass();
    static Object[] myArr = new Object[] { myCl };
    static final Object[] s_myArr = new Object[] { myCl };

    static StringBuffer sb = new StringBuffer("a");

    public static void main(String[] args) throws Exception {

        test("footrue", s + b);
        test("foo42",   s + by);
        test("foo42",   s + sh);
        test("fooa",    s + ch);
        test("foo42",   s + i);
        test("foo42",   s + l);
        test("foo42.0", s + fl);
        test("foo42.0", s + d);
        test("foofoo",  s + s);
        test("foonull", s + sNull);
        test("foobar",  s + o);
        test("foonull", s + oNull);
        test("foobar",  s + cs);

        {
            StringBuilder sb = new StringBuilder();
            sb.append("foo");
            sb.append(myArr.toString());
            test(sb.toString(), s + myArr);
        }

        {
            StringBuilder sb = new StringBuilder();
            sb.append("foo");
            sb.append(s_myArr.toString());
            test(sb.toString(), s + s_myArr);
        }

        {
            StringBuilder sb = new StringBuilder();
            sb.append("foo[C@");
            sb.append(Integer.toHexString(System.identityHashCode(chars)));
            test(sb.toString(), s + chars);
        }

        test("fooa",    s + ImplicitStringConcat.sb);
        test("foonull", s + null);
        test("fooMyClass", s + myCl);
        test("foonull",    s + myClNull);
        test("fooMyClass", s + myCl2);

        s = "foo";  s += b;     test("footrue", s);
        s = "foo";  s += by;    test("foo42", s);
        s = "foo";  s += sh;    test("foo42", s);
        s = "foo";  s += ch;    test("fooa", s);
        s = "foo";  s += i;     test("foo42", s);
        s = "foo";  s += l;     test("foo42", s);
        s = "foo";  s += fl;    test("foo42.0", s);
        s = "foo";  s += d;     test("foo42.0", s);
        s = "foo";  s += s;     test("foofoo", s);
        s = "foo";  s += sNull; test("foonull", s);
        s = "foo";  s += o;     test("foobar", s);
        s = "foo";  s += oNull; test("foonull", s);
        s = "foo";  s += cs;    test("foobar", s);

        {
            StringBuilder sb = new StringBuilder();
            sb.append("foo[C@");
            sb.append(Integer.toHexString(System.identityHashCode(chars)));
            s = "foo";
            s += chars;
            test(sb.toString(), s);
        }

        s = "foo";  s += ImplicitStringConcat.sb;    test("fooa", s);
        s = "foo";  s += null;  test("foonull", s);
        s = "foo";  s += myCl;  test("fooMyClass", s);
        s = "foo";  s += myCl2; test("fooMyClass", s);
    }

    public static void test(String expected, String actual) {
       // Fingers crossed: String concat should work.
       if (!expected.equals(actual)) {
           StringBuilder sb = new StringBuilder();
           sb.append("Expected = ");
           sb.append(expected);
           sb.append(", actual = ");
           sb.append(actual);
           throw new IllegalStateException(sb.toString());
       }
    }

    static class MyClass {
        public String toString() {
            return "MyClass";
        }
    }

    static class MyClassNull {
        public String toString() {
            return null;
        }
    }
}
