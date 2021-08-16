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
 *  use method reference to sort list elements by field
 * @author  Brian Goetz
 * @author  Maurizio Cimadamore
 * @run main MethodReference01
 */

import java.util.*;

public class MethodReference01 {

    interface Getter<U, T> {
        public U get(T t);
    }

    static class Foo {
        private Integer a;
        private String b;

        Foo(Integer a, String b) {
            this.a = a;
            this.b = b;
        }

        static Integer getA(Foo f) { return f.a; }
        static String getB(Foo f) { return f.b; }
    }

    public static <T, U extends Comparable<? super U>>
           void sortBy(List<T> s, final Getter<U, T> getter) {
        Collections.sort(s, new Comparator<T>() {
            public int compare(T t1, T t2) {
                return getter.get(t1).compareTo(getter.get(t2));
            }
        });
    };

    public static void main(String[] args) {
        List<Foo> c = new ArrayList<Foo>();
        c.add(new Foo(2, "Hello3!"));
        c.add(new Foo(3, "Hello1!"));
        c.add(new Foo(1, "Hello2!"));
        checkSortByA(c);
        checkSortByB(c);
    }

    static void checkSortByA(List<Foo> l) {
        sortBy(l, Foo::getA);
        int oldA = -1;
        for (Foo foo : l) {
            if (foo.a.compareTo(oldA) < 1) {
                throw new AssertionError();
            }
        }
    }

    static void checkSortByB(List<Foo> l) {
        sortBy(l, Foo::getB);
        String oldB = "";
        for (Foo foo : l) {
            if (foo.b.compareTo(oldB) < 1) {
                throw new AssertionError();
            }
        }
    }
}
