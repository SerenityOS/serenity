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

import java.util.*;

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *   Parameter types inferred from target type in generics with wildcard
 * @compile InferenceTest2b.java
 * @run main InferenceTest2b
 */

public class InferenceTest2b {

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) {

        InferenceTest2b test = new InferenceTest2b();

        test.m1((a, b) -> {return a;});
        test.m2((a, b) -> {return a;});
        test.m3((a, b) -> a);
    }

    interface SAM6<T> {
        T m6(T a, T b);
    }

    void m1(SAM6<? super List<?>> s) {
        System.out.println("m1()");
        Stack<String> a = new Stack<String>();
        ArrayList<String> b = new ArrayList<String>();
        assertTrue(s.m6(a, b) == a);

        Vector<?> c = null;
        assertTrue(s.m6(c, b) == c);
    }

    void m2(SAM6<? super Integer> s) {
        System.out.println("m2()");
        assertTrue(s.m6(1, 2).equals(Integer.valueOf(1)));
    }

    void m3(SAM6<? super Calendar> s) {
        System.out.println("m3()");
        Calendar gc = Calendar.getInstance();
        GregorianCalendar gc2 = new GregorianCalendar();
        assertTrue(s.m6(gc, gc2) == gc);
    }
}
