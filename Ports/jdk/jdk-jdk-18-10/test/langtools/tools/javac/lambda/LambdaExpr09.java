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

/*
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda in array initializers is correctly accepted
 * @compile LambdaExpr09.java
 */

class LambdaExpr09 {

    interface Block<T> {
       void m(T t);
    }

    void apply(Object[] obj_arr) { }

    void test1() {
        Block<?>[] arr1 =  { t -> { }, t -> { } };
        Block<?>[][] arr2 =  { { t -> { }, t -> { } }, { t -> { }, t -> { } } };
    }

    void test2() {
        Block<?>[] arr1 =  new Block<?>[]{ t -> { }, t -> { } };
        Block<?>[][] arr2 =  new Block<?>[][]{ { t -> { }, t -> { } }, { t -> { }, t -> { } } };
    }

    void test3() {
        apply(new Block<?>[]{ t -> { }, t -> { } });
        apply(new Block<?>[][]{ { t -> { }, t -> { } }, { t -> { }, t -> { } } });
    }
}
