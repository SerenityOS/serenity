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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non static selectors in method refs are handled correctly
 * @author  Maurizio Cimadamore
 * @compile MethodReference10.java
 */

import java.util.*;

class MethodReference10 {

    interface Getter<U, T> {
        public U get(T t);
    }

    public static <T, U extends Comparable<? super U>>
        void sortBy(Collection<T> s, Getter<U, T> getter) {};

    static class Foo {
        private String a;
        String getA(Foo f) { return f.a; }
    }

    public static void main(String[] args) {
        Collection<Foo> c = new ArrayList<Foo>();
        sortBy(c, new Foo()::getA);
    }
}
