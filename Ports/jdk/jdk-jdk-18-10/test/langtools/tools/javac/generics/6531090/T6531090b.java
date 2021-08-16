/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6531090 6711619
 *
 * @summary Cannot access methods/fields of a captured type belonging to an intersection type
 * @author Maurizio Cimadamore
 *
 */
public class T6531090b {

    static class A {
        public void a1() {}
        protected void a2() {}
        void a3() {}
        public A a1;
        protected A a2;
        A a3;
    }
    static class B extends A {
        public void b1() {}
        protected void b2() {}
        void b3() {}
        public B b1;
        protected B b2;
        B b3;
    }
    static interface I{
        void i();
    }
    static interface I1{
        void i1();
    }
    static class E extends B implements I, I1{
        public void i() {}
        public void i1() {}
    }
    static class C<W extends B & I1, T extends W>{
        T t;
        W w;
        C(W w, T t) {
            this.w = w;
            this.t = t;
        }
    }

    public static void main(String... args) {
        C<E,E> c = new C<E,E>(new E(), new E());
        testMemberMethods(c);
        testMemberFields(c);
    }

    static void testMemberMethods(C<? extends A, ? extends I> arg) {
        arg.t.a1();
        arg.t.a2();
        arg.t.a3();
        arg.t.b1();
        arg.t.b2();
        arg.t.b3();
        arg.t.i1();
        arg.w.a1();
        arg.w.a2();
        arg.w.a3();
        arg.w.b1();
        arg.w.b2();
        arg.w.b3();
        arg.w.i1();
    }

    static void testMemberFields(C<? extends A, ? extends I> arg) {
        A ta; B tb;
        ta = arg.t.a1;
        ta = arg.t.a2;
        ta = arg.t.a3;
        tb = arg.t.b1;
        tb = arg.t.b2;
        tb = arg.t.b3;
        ta = arg.w.a1;
        ta = arg.w.a2;
        ta = arg.w.a3;
        tb = arg.w.b1;
        tb = arg.w.b2;
        tb = arg.w.b3;
    }
}
