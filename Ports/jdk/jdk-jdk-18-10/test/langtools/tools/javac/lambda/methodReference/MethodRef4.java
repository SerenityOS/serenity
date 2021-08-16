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
 *   Test constructor reference
 * @compile MethodRef4.java
 * @run main MethodRef4
 */

public class MethodRef4 {

    static interface A {Fee<String> m();}

    static interface B {Fee<String> m(String s);}

    static interface C {Object m();}

    static class Fee<T> {

        private T t;

        public Fee() {
            System.out.println("Fee<T> instantiated");
        }

        public Fee(T t) {
            this.t = t;
            System.out.println("Fee<T> instantiated: " + t);
        }

        public void make() {
            System.out.println(this + ": make()");
        }
    }

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) {

        A a = Fee<String>::new; //constructor reference to Fee<T>()
        a.m().make();

        B b = Fee<String>::new; //constructor reference to Fee<T>(String)
        b.m("hi").make();

        C c = MethodRef4::new; //constructor reference to MethodRef4()
        assertTrue( c.m() instanceof MethodRef4 );
        c = MethodRef4::new; //constructor reference to MethodRef4()
        assertTrue( c.m() instanceof MethodRef4 );
        c = Fee<String>::new; //constructor reference to Fee<T>()
        assertTrue( c.m() instanceof Fee );
    }
}
