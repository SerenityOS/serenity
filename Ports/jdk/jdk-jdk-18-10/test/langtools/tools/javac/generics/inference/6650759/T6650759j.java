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
 * @compile T6650759j.java
 */

public class T6650759j {

    static abstract class A<X extends A<X>> {}

    static abstract class B<X extends B<X, Y>, Y> extends A<X> {}

    static abstract class C<X extends C<X, Y>, Y> extends B<X, Y> {}

    interface D {}

    static class E extends C<E, D> {}

    static abstract class F<X extends F<X, Y>, Y extends A<Y>> extends A<X> {}

    static class G extends F<G, E> {}

    static <X extends F<X, Y>, Y extends A<Y>> X m(Iterable<X> it) {
        return null;
    }

    static G test(Iterable<G> c) {
        return m(c);
    }
}
