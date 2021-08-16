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
 * @run main LambdaScope01
 */

public class LambdaScope01 {

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

    public int n = 5;

    public int hashCode() {
        throw new RuntimeException();
    }

    public void test1() {
        try {
            int res = LambdaScope01.<Integer,Integer>exec((Integer x) -> x * hashCode(), 3);
        }
        catch (RuntimeException e) {
            assertTrue(true); //should throw
        }
    }

    public void test2() {
        final int n = 10;
        int res = LambdaScope01.<Integer,Integer>exec((Integer x) -> x + n, 3);
        assertTrue(13 == res);
    }

    public static void main(String[] args) {
        LambdaScope01 t = new LambdaScope01();
        t.test1();
        t.test2();
        assertTrue(assertionCount == 2);
    }
}
