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
 * @summary  basic test for diamond (generic/non-generic constructors)
 * @author mcimadamore
 * @compile Pos01.java
 * @run main Pos01
 *
 */

public class Pos01<X> {

    Pos01(X x) {}

    <Z> Pos01(X x, Z z) {}

    void test() {
        Pos01<Integer> p1 = new Pos01<>(1);
        Pos01<? extends Integer> p2 = new Pos01<>(1);
        Pos01<?> p3 = new Pos01<>(1);
        Pos01<? super Integer> p4 = new Pos01<>(1);

        Pos01<Integer> p5 = new Pos01<>(1, "");
        Pos01<? extends Integer> p6 = new Pos01<>(1, "");
        Pos01<?> p7 = new Pos01<>(1, "");
        Pos01<? super Integer> p8 = new Pos01<>(1, "");

        Pos01<Integer> p9 = new Pos01<>(1){};
        Pos01<? extends Integer> p10 = new Pos01<>(1){};
        Pos01<?> p11 = new Pos01<>(1){};
        Pos01<? super Integer> p12 = new Pos01<>(1){};

        Pos01<Integer> p13 = new Pos01<>(1, ""){};
        Pos01<? extends Integer> p14= new Pos01<>(1, ""){};
        Pos01<?> p15 = new Pos01<>(1, ""){};
        Pos01<? super Integer> p16 = new Pos01<>(1, ""){};
   }

    public static void main(String[] args) {
        Pos01<String> p1 = new Pos01<>("");
        p1.test();
    }
}
