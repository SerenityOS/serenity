/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225056
 */

// Test that a method in a sealed class or interface can be overridden.
public class OverrideSealedTest {

    sealed class Rectangle permits Square {
        public String draw() { return "rectangle"; }
    }

    final class Square extends Rectangle {
        public String draw() { return "square"; }
    }

    Rectangle r = new Rectangle();
    Rectangle rs = new Square();
    Square s = new Square();


    public sealed interface Shape permits Circle {
        default String name() { return "shape"; }
    }

    final class Circle implements Shape {
        public String name() { return "circle"; }
    }

    Shape sc = new Circle();
    Circle c = new Circle();


    public static void main(String... args) {
        OverrideSealedTest ost = new OverrideSealedTest();
        if (ost.r.draw() != "rectangle")
            throw new RuntimeException("Bad value returned by draw(): " + ost.r.draw());
        if (ost.rs.draw() != "square")
            throw new RuntimeException("Bad value returned by draw(): " + ost.rs.draw());
        if (ost.s.draw() != "square")
            throw new RuntimeException("Bad value returned by draw(): " + ost.s.draw());

        if (ost.sc.name() != "circle")
            throw new RuntimeException("Bad value returned by name(): " + ost.sc.name());
        if (ost.c.name() != "circle")
            throw new RuntimeException("Bad value returned by name(): " + ost.c.name());
    }
}
