/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020149
 * @summary Graph inference: wrong logic for picking best variable to solve
 * @compile T8020149.java
 */
class T8020149 {
    static class TestData<X,Y> { }

    interface Foo<X, Y extends Foo<X, Y>> { }

    interface IntFoo extends Foo<Integer, IntFoo> { }

    interface Function<X, Y> {
        Y apply(X x);
    }

    void test(TestData<Integer, IntFoo> data) {
       m1(data, s->s);
       m2(data, s->s);
    }

    <E, E_OUT extends Foo<E, E_OUT>, W, W_IN extends Foo<W, W_IN>> void m1(TestData<W, W_IN> data, Function<W_IN, E_OUT> m) {  }
    <W, W_IN extends Foo<W, W_IN>, E, E_OUT extends Foo<E, E_OUT>> void m2(TestData<W, W_IN> data, Function<W_IN, E_OUT> m) {  }
}
