/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8069263
 * @summary Deoptimization between array allocation and arraycopy may result in non initialized array
 *
 * @run main/othervm -XX:-BackgroundCompilation
 *      -XX:CompileCommand=compileonly,compiler.inlining.DefaultMethodsDependencies::test
 *      -XX:CompileCommand=compileonly,compiler.inlining.DefaultMethodsDependencies$I2::m1
 *      compiler.inlining.DefaultMethodsDependencies
 */

package compiler.inlining;

public class DefaultMethodsDependencies {

    interface I1 {
        void m1();
        // triggers processing of default methods in C1
        default void m2() {
        }
    }

    interface I2 extends I1 {
        // added to C2 as default method
        default void m1() {
        }
    }

    static abstract class C1 implements I1 {
    }

    static class C2 extends C1 implements I2 {
    }

    static void test(C1 obj) {
        obj.m1();
    }

    static public void main(String[] args) {
        C2 obj = new C2();
        for (int i = 0; i < 20000; i++) {
            test(obj);
        }
    }
}
