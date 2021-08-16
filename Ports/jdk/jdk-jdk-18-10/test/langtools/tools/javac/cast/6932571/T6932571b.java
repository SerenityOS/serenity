/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6932571
 * @summary Compiling Generics causing Inconvertible types
 * @compile T6932571b.java
 */

class T6932571b {

    interface A1<T extends B<? super T>> {
        public T getT();
    }

    interface A2<T extends B<? extends T>> {
        public T getT();
    }

    class B<T extends B<T>> {}

    class C extends B<C> {}

    void test1(A1<?> a) {
        Object o = (C)a.getT();
    }

    void test2(A2<?> a) {
        Object o = (C)a.getT();
    }
}
