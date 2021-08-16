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

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *   Test SAM conversion of lambda expressions in context of assignment, method call, return statement and cast.
 * @compile SamConversion.java
 * @run main SamConversion
 */

public class SamConversion {

    static interface Foo {
        Integer m(int i);
    }

    static interface Bar {
        int m(Integer i) throws Exception;
    }

    private static String assertionStr = "";

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    private static void test1(Foo foo) {
        assertTrue(foo.m(1) == 2);
    }

    private static void test2(Bar bar) {
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e){
            assertTrue(false);
        }
    }

    private static Bar test3(int i) {
        switch (i) {
        case 0:
            return n -> n + 1;
        case 1:
            return (Integer n) -> 2 * n;
        case 2:
            return (Integer n) -> {return new Integer(n-1);};
        case 3:
            return n -> {throw new Exception();};
        default:
            return null;
        }
    }

    public static void main(String[] args) {

        //assign:
        Foo foo = (int n) -> n + 1; //explicit type and boxing
        assertTrue(foo.m(1) == 2);

        foo = n -> n + 1; //type inferrred and boxing
        assertTrue(foo.m(1) == 2);

        Bar bar = (Integer n) -> n + 1; //explicit type and unboxing
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e) {
            assertTrue(false);
        }

        bar = (Integer n) -> new Integer(n+1); //explicit type and unboxing twice
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e) {
            assertTrue(false);
        }

        bar = n -> n.intValue() + 1; //type inferred
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e) {
            assertTrue(false);
        }

        bar = n -> n + 1; // type inferred and unboxing
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e) {
            assertTrue(false);
        }

        //cast:
        assertTrue(((Foo)n -> {return n+1;}).m(1) == 2); //statement (instead of expression) in lambda body
        try {
            assertTrue(((Bar)n -> {return n+1;}).m(1) == 2); //statement in lambda body
        } catch (Exception e) {
            assertTrue(false);
        }

        //method parameter:
        test1((int n) -> new Integer(n+1)); //explicit type
        test2((Integer n) -> n.intValue() + 1); //explicit type

        //return statement:
        bar = test3(0);
        try {
            assertTrue(bar.m(1) == 2);
        } catch (Exception e) {
            assertTrue(false);
        }
        bar = test3(1);
        try {
            assertTrue(bar.m(3) == 6);
        } catch (Exception e) {
            assertTrue(false);
        }
        bar = test3(2);
        try {
            assertTrue(bar.m(10) == 9);
        } catch (Exception e) {
            assertTrue(false);
        }
        bar = test3(3);
        try {
            bar.m(10);
            assertTrue(false);
        } catch (Exception e) {}
    }
}
