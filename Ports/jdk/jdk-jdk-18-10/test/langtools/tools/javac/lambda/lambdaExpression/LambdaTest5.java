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
 *   Test lambda expressions inside lambda expressions
 * @compile LambdaTest5.java
 * @run main LambdaTest5
 */

public class LambdaTest5 {

    interface A {
        int m();
    }

    interface B {
        int make (int i);
    }

    private static int count = 0;

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    static A a;
    static A a2;
    static A a3;
    static A a4;

    public static void main(String[] args) {
        B b = (int i) -> ((A)()-> 5).m();
        assertTrue(b.make(0) == 5);

        a = () -> ((A)()-> { return 6; }).m(); //self reference
        assertTrue(a.m() == 6);

        a2 = ()-> {
                  A an = ()-> { return 7; }; //self reference
                  return an.m();
                };
        assertTrue(a2.m() == 7);

        a3 = () -> a3.m(); //self reference
        try {
            a3.m();
        } catch(StackOverflowError e) {
            count++;
        }
        assertTrue(count==1);

        a4 = ()-> ((B)(int i)-> ((A)()-> 9).m() ).make(0);
        assertTrue(a4.m() == 9);
    }
}
