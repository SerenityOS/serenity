/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4254213
 * @summary Verify that member types of classes and interfaces can be inherited.
 *
 * @run compile MemberTypeInheritance.java
 */

class C {
    class D {}
    interface E {}
}

interface I {
    class J {}
    interface K{}
}

class L extends C {}

interface M extends I {}

class X extends C implements I {
    D d;
    E e;
    J j;
    K k;
}

class Y extends L implements M {
    D d;
    E e;
    J j;
    K k;
}

class Outer {

    class C {
        class D {}
        // Inner class cannot have member interface (static member).
    }

    interface I {
        class J {}
        interface K{}
    }

    class L extends C {}

    interface M extends I {}

    class X extends C implements I {
        D d;
        J j;
        K k;
    }

    class Y extends L implements M {
        D d;
        J j;
        K k;
    }

    void test() {

        // Blocks may not contain local interfaces.

        class C {
            class D {}
            // Inner class cannot have member interface (static member).
        }

        class L extends C {}

        class X extends C {
            D d;
        }

        class Y extends L {
            D d;
        }

    }

}
