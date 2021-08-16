/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6650759
 * @summary Inference of formal type parameter (unused in formal parameters) is not performed
 * @compile T6650759i.java
 */
public class T6650759i {

    static class A<X extends A, Y extends B> {}

    static class B<X extends B> {}

    static class C<X extends A<X, Y>, Y extends B<Y>> {}

    static <U extends A<U, V>, V extends B<V>> Class<U> m1(U x) {
        return null;
    }

    static <U extends A<U, V>, V extends B<V>> U m2(Class<U> c) {
        return null;
    }

    static <W, U extends A<U, V>, V extends B<V>> W m3(Class<W> c1, C<U, V> c2) {
        return null;
    }

    static <U extends A<U, V>, V extends B<V>> void test(U u, C<U, V> c) {
        m2(m1(u));
        U res = m3(m1(u), c);
    }
}
