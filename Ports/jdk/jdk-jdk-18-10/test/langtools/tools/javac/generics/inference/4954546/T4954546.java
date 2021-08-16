/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4954546
 * @summary unverifiable code for method called from ?: expression with inferred
 */

public class T4954546 {
    interface I {
        void f();
    }
    interface J {
        void g();
    }
    static class A implements I, J {
        public void f() {}
        public void g() {}
    }
    static class B implements J, I {
        public void f() {}
        public void g() {}
    }
    public static void main(String[] args) {
        f(true, new A(), new B());
    }
    static void f(boolean cond, A a, B b) {
        (cond?a:b).f();;
        (cond?a:b).g();;
    }
}
