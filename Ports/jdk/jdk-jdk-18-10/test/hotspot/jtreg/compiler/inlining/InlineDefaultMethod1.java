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
 * @bug 8036100
 * @summary Default method returns true for a while, and then returns false
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.inlining.InlineDefaultMethod1::test
 *      -XX:CompileCommand=compileonly,compiler.inlining.InlineDefaultMethod1$I1::m
 *      -XX:CompileCommand=compileonly,compiler.inlining.InlineDefaultMethod1$I2::m
 *      compiler.inlining.InlineDefaultMethod1
 */

package compiler.inlining;

public class InlineDefaultMethod1 {
    interface I1 {
        default public int m() { return 0; }
    }

    interface I2 extends I1 {
        default public int m() { return 1; }
    }

    static abstract class A implements I1 {
    }

    static class B extends A implements I2 {
    }


    public static void test(A obj) {
        int id = obj.m();
        if (id != 1) {
            throw new AssertionError("Called wrong method: 1 != "+id);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        test(new B());
        System.out.println("TEST PASSED");
    }
}
