/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027422
 * @summary type methods shouldn't always operate on speculative part
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:TypeProfileLevel=222
 *                   -XX:+UseTypeSpeculation -XX:-BackgroundCompilation
 *                   compiler.types.TestSpeculationFailedHigherEqual
 */

package compiler.types;

public class TestSpeculationFailedHigherEqual {

    static class A {
        void m() {}
        int i;
    }

    static class C extends A {
    }

    static C c;

    static A m1(A a, boolean cond) {
        // speculative type for a is C not null
        if (cond ) {
            a = c;
        }
        // speculative type for a is C (may be null)
        int i = a.i;
        return a;
    }

    static public void main(String[] args) {
        C c = new C();
        TestSpeculationFailedHigherEqual.c = c;
        for (int i = 0; i < 20000; i++) {
            m1(c, i%2 == 0);
        }

        System.out.println("TEST PASSED");
    }
}
