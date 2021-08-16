/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 *  basic test for capture of non-mutable locals
 * @author  Brian Goetz
 * @author  Maurizio Cimadamore
 * @run main LambdaCapture01
 */

public class LambdaCapture01 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface Tester {
        void test();
    }

    interface TU<T, U> {
        public T foo(U u);
    }

    public static <T, U> T exec(TU<T, U> lambda, U x) {
        return lambda.foo(x);
    }

    public int n = 5;

    //Simple local capture
    void test1() {
        final int N = 1;
        int res = LambdaCapture01.<Integer,Integer>exec((Integer x) -> x + N, 3);
        assertTrue(4 == res);
    }

    //Local capture with multiple scopes (anon class)
    void test2() {
        final int N = 1;
        new Tester() {
            public void test() {
                final int M = 2;
                int res = LambdaCapture01.<Integer,Integer>exec((Integer x) -> x + N + M, 3);
                assertTrue(6 == res);
            }
        }.test();
    }

    //Local capture with multiple scopes (local class)
    void test3() {
        final int N = 1;
        class MyTester implements Tester {
            public void test() {
                final int M = 2;
                int res = LambdaCapture01.<Integer,Integer>exec((Integer x) -> x + N + M, 3);
                assertTrue(6 == res);
            }
        }
        new MyTester().test();
    }

    //access to field from enclosing scope
    void test4() {
        final int N = 4;
        int res1 = LambdaCapture01.<Integer,Integer>exec((Integer x) -> x + n + N, 3);
        assertTrue(12 == res1);
        int res2 = LambdaCapture01.<Integer,Integer>exec((Integer x) -> x + LambdaCapture01.this.n + N, 3);
        assertTrue(12 == res2);
    }

    public static void main(String[] args) {
        LambdaCapture01 t = new LambdaCapture01();
        t.test1();
        t.test2();
        t.test3();
        t.test4();
        assertTrue(assertionCount == 5);
    }
}
