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
 *   Test that parameter types are inferred from SAM descriptor when method parameters are elided,
             with different types of method references
 * @compile MethodRef7.java
 * @run main MethodRef7
 */

public class MethodRef7 {

    static interface A {void m();}

    static interface A2 {void m(int n);}

    static interface B {String m();}

    static interface B2 {String m(int n);}

    static interface C {String m(MethodRef7 mr);}

    static interface C2 {String m(MethodRef7 mr, int n);}

    static interface D {Fee<String> m();}

    static interface D2 {Fee<String> m(String s);}

    static class Fee<T> {

        public Fee() {
            System.out.println("Fee<T> instantiated");
        }

        public Fee(String s) {
            System.out.println("Fee<T> instantiated: " + s);
        }
    }

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    static void bar() {
        System.out.println("MethodRef_neg1.bar()");
    }

    static void bar(int x) {
        System.out.println("MethodRef_neg1.bar(int) " + x);
    }

    String wahoo() {
        return "wahoo";
    }

    String wahoo(int x) {
        return "wahoo " + x;
    }

    public static void main(String[] args) {

        A a = MethodRef7::bar; //static reference to bar()
        a.m();
        A2 a2 = MethodRef7::bar; //static reference to bar(int x)
        a2.m(10);

        MethodRef7 mr = new MethodRef7();
        B b = mr::wahoo; //instance reference to wahoo()
        assertTrue(b.m().equals("wahoo"));
        B2 b2 = mr::wahoo; //instance reference to wahoo(int x)
        assertTrue(b2.m(1).equals("wahoo 1"));

        C c = MethodRef7::wahoo; //unbound reference to wahoo()
        assertTrue(c.m(mr).equals("wahoo"));
        C2 c2 = MethodRef7::wahoo; //unbound reference to wahoo(int x)
        assertTrue(c2.m(mr, 2).equals("wahoo 2"));

        D d = Fee<String>::new; //constructor reference to Fee()
        D2 d2 = Fee<String>::new; //constructor reference to Fee(String s)
    }
}
