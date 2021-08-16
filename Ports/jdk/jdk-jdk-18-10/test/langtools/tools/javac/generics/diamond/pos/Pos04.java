/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6939620 7020044 8062373
 *
 * @summary  basic test for diamond (simple/qualified type-expressions, local class)
 * @author mcimadamore
 * @compile Pos04.java
 * @run main Pos04
 *
 */

public class Pos04<U> {

    void test() {
        class Foo<V> {
            Foo(V x) {}
            <Z> Foo(V x, Z z) {}
        }
        Foo<Integer> p1 = new Foo<>(1);
        Foo<? extends Integer> p2 = new Foo<>(1);
        Foo<?> p3 = new Foo<>(1);
        Foo<? super Integer> p4 = new Foo<>(1);

        Foo<Integer> p5 = new Foo<>(1, "");
        Foo<? extends Integer> p6 = new Foo<>(1, "");
        Foo<?> p7 = new Foo<>(1, "");
        Foo<? super Integer> p8 = new Foo<>(1, "");

        Foo<Integer> p9 = new Foo<>(1){};
        Foo<? extends Integer> p10 = new Foo<>(1){};
        Foo<?> p11 = new Foo<>(1){};
        Foo<? super Integer> p12 = new Foo<>(1){};

        Foo<Integer> p13 = new Foo<>(1, ""){};
        Foo<? extends Integer> p14 = new Foo<>(1, ""){};
        Foo<?> p15 = new Foo<>(1, ""){};
        Foo<? super Integer> p16 = new Foo<>(1, ""){};
    }

    public static void main(String[] args) {
        Pos04<String> p4 = new Pos04<>();
        p4.test();
    }
}
