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
 *  basic test for simple lambda expressions in multiple scopes
 * @author  Brian Goetz
 * @author  Maurizio Cimadamore
 * @run main LambdaExpr01
 */

public class LambdaExpr02 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface S_int {
        int m();
    }

    interface S_Integer {
        Integer m();
    }

    interface S_int_int {
        int m(int i);
    }

    interface S_Integer_int {
        int m(Integer i);
    }

    interface S_int_Integer {
        Integer m(int i);
    }

    interface S_Integer_Integer {
        Integer m(Integer i);
    }

    static {
        S_int s_i = ()-> { return 3; };
        assertTrue(3 == s_i.m());
        S_Integer s_I = ()-> { return 3; };
        assertTrue(3 == s_I.m());
        S_int_int s_i_i = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_i.m(3));
        S_int_Integer s_i_I = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_I.m(3));
        S_Integer_int s_I_i = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_i.m(3));
        S_Integer_Integer s_I_I = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_I.m(3));
    }

    {
        S_int s_i = ()-> { return 3; };
        assertTrue(3 == s_i.m());
        S_Integer s_I = ()-> { return 3; };
        assertTrue(3 == s_I.m());
        S_int_int s_i_i = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_i.m(3));
        S_int_Integer s_i_I = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_I.m(3));
        S_Integer_int s_I_i = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_i.m(3));
        S_Integer_Integer s_I_I = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_I.m(3));
    }

    static void test1() {
        S_int s_i = ()-> { return 3; };
        assertTrue(3 == s_i.m());
        S_Integer s_I = ()-> { return 3; };
        assertTrue(3 == s_I.m());
        S_int_int s_i_i = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_i.m(3));
        S_int_Integer s_i_I = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_I.m(3));
        S_Integer_int s_I_i = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_i.m(3));
        S_Integer_Integer s_I_I = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_I.m(3));
    }

    void test2() {
        S_int s_i = ()-> { return 3; };
        assertTrue(3 == s_i.m());
        S_Integer s_I = ()-> { return 3; };
        assertTrue(3 == s_I.m());
        S_int_int s_i_i = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_i.m(3));
        S_int_Integer s_i_I = (int x) -> { return x + 1; };
        assertTrue(4 == s_i_I.m(3));
        S_Integer_int s_I_i = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_i.m(3));
        S_Integer_Integer s_I_I = (Integer x) -> { return x.intValue() + 1; };
        assertTrue(4 == s_I_I.m(3));
    }

    public static void main(String[] args) {
        test1();
        new LambdaExpr02().test2();
        assertTrue(assertionCount == 24);
    }
}
