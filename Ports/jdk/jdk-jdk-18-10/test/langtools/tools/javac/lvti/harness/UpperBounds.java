/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191802
 * @summary Upward projection result is A<? extends Number> instead of A<? super Integer>
 * @modules jdk.compiler/com.sun.source.tree
 *          jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @build LocalVariableInferenceTester
 * @run main LocalVariableInferenceTester UpperBounds.java
 */
class UpperBounds {

    static class A<T extends Number> { }

    static class C<T extends Comparable<T>> { }

    void test1(A<? super Integer> s) {
        //U is not Object, Bi is not fbound and U is not more specific than Bi, use "? super L"
        @InferredType("UpperBounds.A<? super java.lang.Integer>")
        var x = s;
    }

    void test2(C<? super Integer> s) {
        //U is not Object, Bi is fbound, use "? extends L"
        @InferredType("UpperBounds.C<? extends java.lang.Comparable<?>>")
        var x = s;
    }

    void test3(A<? extends Object> s) {
        //U is not Object, Bi is not fbound and U is not more specific than Bi, L is undefined, use "?"
        @InferredType("UpperBounds.A<?>")
        var x = s;
    }

    void test4(A<? extends Integer> s) {
        //U is not Object, Bi is not fbound and U is more specific than Bi, use "? extends U"
        @InferredType("UpperBounds.A<? extends java.lang.Integer>")
        var x = s;
    }

    void test5(A<? extends Object> s) {
        //U is not Object, Bi is not fbound and U is not more specific than Bi, L is undefined, use "?"
        @InferredType("UpperBounds.A<?>")
        var x = s;
    }
}
