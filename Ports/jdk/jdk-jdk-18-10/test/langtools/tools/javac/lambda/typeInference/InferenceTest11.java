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
 *   This test is for self referential and recursive lambda expression that have type inference
 * @compile InferenceTest11.java
 * @run main InferenceTest11
 */

public class InferenceTest11 {

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    static Func<Integer, Integer> f1;
    static Func<Integer, ? extends Number> f2;

    public static void main(String[] args) {

        f1 = n -> {
            if(n <= 0)
                return 0;
            if(n == 1)
                return 1;
            return f1.m(n-1) + f1.m(n-2);
        };
        assertTrue(f1.m(-1) == 0);
        assertTrue(f1.m(0) == 0);
        assertTrue(f1.m(10) == 55);

        f2 = n -> {
            if(n <= 1)
            return 1.0;
            return 2 * (Double)f2.m(n-1) + 1;
        };
        assertTrue(f2.m(4).doubleValue() == 15.0);
    }

    interface Func<T, V> {
        V m(T t);
    }
}
