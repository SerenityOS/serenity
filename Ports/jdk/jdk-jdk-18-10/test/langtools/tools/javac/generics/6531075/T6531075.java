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
 * @bug 6531075
 *
 * @summary Missing synthetic casts when accessing fields/methods of intersection types including type variables
 * @author Maurizio Cimadamore
 *
 */


public class T6531075 {

    static class A {
        void a() {}
    }

    static interface I{
        void i();
    }

    static class E extends A implements I{
        public void i() {}
    }

    static class C<W extends A & I, T extends W>{
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
    }

    static void testMemberMethods(C<?, ?> arg) {
        arg.t.a();
        arg.t.i();
    }
}
