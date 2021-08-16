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
 * @summary  basic test for diamond (simple/qualified type-expressions, member inner)
 * @author mcimadamore
 * @compile Pos03.java
 * @run main Pos03
 *
 */

public class Pos03<U> {

    class Foo<V> {
        Foo(V x) {}
        <Z> Foo(V x, Z z) {}
    }

    void testSimple() {
        Foo<Integer> f1 = new Foo<>(1);
        Foo<? extends Integer> f2 = new Foo<>(1);
        Foo<?> f3 = new Foo<>(1);
        Foo<? super Integer> f4 = new Foo<>(1);

        Foo<Integer> f5 = new Foo<>(1, "");
        Foo<? extends Integer> f6 = new Foo<>(1, "");
        Foo<?> f7 = new Foo<>(1, "");
        Foo<? super Integer> f8 = new Foo<>(1, "");

        Foo<Integer> f9 = new Foo<>(1){};
        Foo<? extends Integer> f10 = new Foo<>(1){};
        Foo<?> f11 = new Foo<>(1){};
        Foo<? super Integer> f12 = new Foo<>(1){};

        Foo<Integer> f13 = new Foo<>(1, ""){};
        Foo<? extends Integer> f14 = new Foo<>(1, ""){};
        Foo<?> f15 = new Foo<>(1, ""){};
        Foo<? super Integer> f16 = new Foo<>(1, ""){};
    }

    void testQualified_1() {
        Foo<Integer> f1 = new Pos03<U>.Foo<>(1);
        Foo<? extends Integer> f2 = new Pos03<U>.Foo<>(1);
        Foo<?> f3 = new Pos03<U>.Foo<>(1);
        Foo<? super Integer> f4 = new Pos03<U>.Foo<>(1);

        Foo<Integer> f5 = new Pos03<U>.Foo<>(1, "");
        Foo<? extends Integer> f6 = new Pos03<U>.Foo<>(1, "");
        Foo<?> f7 = new Pos03<U>.Foo<>(1, "");
        Foo<? super Integer> f8 = new Pos03<U>.Foo<>(1, "");

        Foo<Integer> f9 = new Pos03<U>.Foo<>(1){};
        Foo<? extends Integer> f10 = new Pos03<U>.Foo<>(1){};
        Foo<?> f11 = new Pos03<U>.Foo<>(1){};
        Foo<? super Integer> f12 = new Pos03<U>.Foo<>(1){};

        Foo<Integer> f13 = new Pos03<U>.Foo<>(1, ""){};
        Foo<? extends Integer> f14 = new Pos03<U>.Foo<>(1, ""){};
        Foo<?> f15 = new Pos03<U>.Foo<>(1, ""){};
        Foo<? super Integer> f16 = new Pos03<U>.Foo<>(1, ""){};
    }

    void testQualified_2(Pos03<U> p) {
        Foo<Integer> f1 = p.new Foo<>(1);
        Foo<? extends Integer> f2 = p.new Foo<>(1);
        Foo<?> f3 = p.new Foo<>(1);
        Foo<? super Integer> f4 = p.new Foo<>(1);

        Foo<Integer> f5 = p.new Foo<>(1, "");
        Foo<? extends Integer> f6 = p.new Foo<>(1, "");
        Foo<?> f7 = p.new Foo<>(1, "");
        Foo<? super Integer> f8 = p.new Foo<>(1, "");

        Foo<Integer> f9 = p.new Foo<>(1){};
        Foo<? extends Integer> f10 = p.new Foo<>(1){};
        Foo<?> f11 = p.new Foo<>(1){};
        Foo<? super Integer> f12 = p.new Foo<>(1){};

        Foo<Integer> f13 = p.new Foo<>(1, ""){};
        Foo<? extends Integer> f14 = p.new Foo<>(1, ""){};
        Foo<?> f15 = p.new Foo<>(1, ""){};
        Foo<? super Integer> f16 = p.new Foo<>(1, ""){};
    }

    public static void main(String[] args) {
        Pos03<String> p3 = new Pos03<>();
        p3.testSimple();
        p3.testQualified_1();
        p3.testQualified_2(p3);
    }
}
