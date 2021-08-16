/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test static method reference
 * @compile MethodRef1.java
 * @run main MethodRef1
 */

public class MethodRef1 {

    static interface A {void m();}

    static interface B {void m(int i);}

    static interface C {String m(String s);}

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    static void foo(int x) {
        System.out.println("MethodRef1.foo(int) " + x);
    }

    static void bar() {
        System.out.println("MethodRef1.bar()");
    }

    static void bar(int x) {
        System.out.println("MethodRef1.bar(int) " + x);
    }

    static String bar(String s) {
        return "MethodRef1.bar(String) " + s;
    }

    public static void main(String[] args) {

        A a = MethodRef1::bar; //static reference to bar()
        a.m();

        B b = MethodRef1::foo; //static reference to foo(int), (int) omitted because method foo is not overloaded
        b.m(1);

        b = MethodRef1::foo; //static reference to foo(int)
        b.m(1);

        b = MethodRef1::bar; //static reference to bar(int)
        b.m(2);

        C c = MethodRef1::bar; //static reference to bar(String)
        assertTrue( c.m("hi").equals("MethodRef1.bar(String) hi") );
    }
}
