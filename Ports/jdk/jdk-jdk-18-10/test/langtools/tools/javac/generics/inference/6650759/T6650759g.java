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
 * @compile T6650759g.java
 */

public class T6650759g {

    static abstract class A<X extends A<X>> {}

    static abstract class B<X extends A<X>> {}

    interface C<X, Y> {}

    static abstract class D<X extends A<X>, Y extends B<X>> implements C<X, Y> {}

    static class E extends A<E> {}

    static class F extends B<E> {}

    static void test(Iterable<E> data) {
        m3(m2(data, m1(F.class)));
    }

    static <X extends A<X>, Y extends B<X>> D<X, Y> m1(Class<Y> c) {
        return null;
    }

    static <U, V> Iterable<V> m2(Iterable<U> x1, C<? super U, ? extends V> x2) {
        return null;
    }

    static void m3(Iterable<F> data) {
    }
}
