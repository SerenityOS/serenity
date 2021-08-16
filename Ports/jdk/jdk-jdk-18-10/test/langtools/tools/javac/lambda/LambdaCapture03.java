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
 *  test for capture of non-mutable locals/outer fields in multiple scopes
 * @author  Maurizio Cimadamore
 * @run main LambdaCapture03
 */

public class LambdaCapture03 {

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

    Integer n1 = 10;

    void test1() {
        final Integer N1 = 1;
        class A {
            Integer n2 = 20;
            void test() {
                  final Integer N2 = 2;
                  class B {
                       void test() {
                           final Integer N3 = 3;
                           int res = LambdaCapture03.exec((Integer x) -> x + n1 + n2 + N1 + N2 + N3, 30);
                           assertTrue(res == 66);
                       }
                  }
                  new B().test();
            }
        }
        new A().test();
    }

    void test2() {
        final Integer N1 = 1;
        new Tester() {
            Integer n2 = 20;
            public void test() {
                final Integer N2 = 2;
                new Tester() {
                    public void test() {
                        final Integer N3 = 3;
                        int res = LambdaCapture03.exec((Integer x) -> x + n1 + n2 + N1 + N2 + N3, 30);
                        assertTrue(res == 66);
                    }
                }.test();
            }
        }.test();
    }

    public static void main(String[] args) {
        LambdaCapture03 t = new LambdaCapture03();
        t.test1();
        t.test2();
        assertTrue(assertionCount == 2);
    }
}
