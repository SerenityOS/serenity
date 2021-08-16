/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *  basic test for lambda conversion
 * @author  Brian Goetz
 * @author  Maurizio Cimadamore
 * @run main LambdaConv01
 */

public class LambdaConv01 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface IntToInt {
      public int foo(int x);
    }

    interface IntToVoid {
      public void foo(int x);
    }

    interface VoidToInt {
      public int foo();
    }

    interface TU<T, U> {
      public T foo(U u);
    }

    public static <T, U> T exec(TU<T, U> lambda, U x) {
        return lambda.foo(x);
    }

    static {
        //Assignment conversion:
        VoidToInt f1 = ()-> 3;
        assertTrue(3 == f1.foo());
        //Covariant returns:
        TU<Number, Integer> f2 = (Integer x) -> x;
        assertTrue(3 == f2.foo(3).intValue());
        //Method resolution with boxing:
        int res = LambdaConv01.<Integer,Integer>exec((Integer x) -> x, 3);
        assertTrue(3 == res);
        //Runtime exception transparency:
        try {
            LambdaConv01.<Integer,Object>exec((Object x) -> x.hashCode(), null);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    {
        //Assignment conversion:
        VoidToInt f1 = ()-> 3;
        assertTrue(3 == f1.foo());
        //Covariant returns:
        TU<Number, Integer> f2 = (Integer x) -> x;
        assertTrue(3 == f2.foo(3).intValue());
        //Method resolution with boxing:
        int res = LambdaConv01.<Integer,Integer>exec((Integer x) -> x, 3);
        assertTrue(3 == res);
        //Runtime exception transparency:
        try {
            LambdaConv01.<Integer,Object>exec((Object x) -> x.hashCode(), null);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    public static void test1() {
        //Assignment conversion:
        VoidToInt f1 = ()-> 3;
        assertTrue(3 == f1.foo());
        //Covariant returns:
        TU<Number, Integer> f2 = (Integer x) -> x;
        assertTrue(3 == f2.foo(3).intValue());
        //Method resolution with boxing:
        int res = LambdaConv01.<Integer,Integer>exec((Integer x) -> x, 3);
        assertTrue(3 == res);
        //Runtime exception transparency:
        try {
            LambdaConv01.<Integer,Object>exec((Object x) -> x.hashCode(), null);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    public void test2() {
        //Assignment conversion:
        VoidToInt f1 = ()-> 3;
        assertTrue(3 == f1.foo());
        //Covariant returns:
        TU<Number, Integer> f2 = (Integer x) -> x;
        assertTrue(3 == f2.foo(3).intValue());
        //Method resolution with boxing:
        int res = LambdaConv01.<Integer,Integer>exec((Integer x) -> x, 3);
        assertTrue(3 == res);
        //Runtime exception transparency:
        try {
            LambdaConv01.<Integer,Object>exec((Object x) -> x.hashCode(), null);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    public static void main(String[] args) {
        test1();
        new LambdaConv01().test2();
        assertTrue(assertionCount == 16);
    }
}
