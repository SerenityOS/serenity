/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 *  test for capture in nested lambda expressions
 * @author  Maurizio Cimadamore
 * @run main LambdaCapture05
 */

public class LambdaCapture05 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface TU<T, U> {
        public T foo(U u);
    }

    public static <T, U> T exec(TU<T, U> lambda, U x) {
        return lambda.foo(x);
    }

    int i = 40;

    void test1(final int a0) {
        exec((final Integer a1) -> {
            final Integer x2 = 10; exec((final Integer a2) -> {
                final Integer x3 = 20;
                exec((final Integer a3) -> { assertTrue(106 == (a0 + a1 + a2 + a3 + x2 + x3 + i)); return null; }, 3);
                return null;
            },2);
            return null;
        },1);
    }

    static void test2(final int a0) {
        exec((final Integer a1) -> {
            final Integer x2 = 10; exec((final Integer a2) -> {
                final Integer x3 = 20;
                exec((final Integer a3) -> { assertTrue(66 == (a0 + a1 + a2 + a3 + x2 + x3)); return null; }, 3);
                return null;
            }, 2);
            return null;
        }, 1);
    }

    public static void main(String[] args) {
        LambdaCapture05 t = new LambdaCapture05();
        t.test1(30);
        test2(30);
        assertTrue(assertionCount == 2);
    }
}
