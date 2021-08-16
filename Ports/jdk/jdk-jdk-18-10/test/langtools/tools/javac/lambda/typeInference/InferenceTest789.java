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

import java.io.Serializable;
import java.util.Calendar;

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *  This test is for when lambda return type is inferred from target type
 * @compile InferenceTest789.java
 * @run main InferenceTest789
 */

public class InferenceTest789 {

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    public static void main(String[] args) {
        InferenceTest789 test = new InferenceTest789();
        test.method1(() -> 1);
        SAM1<? extends Number> sam1 = () -> 1.0;
        SAM1<? extends Serializable> sam1_2 = () -> "a";
        SAM1<? extends Comparable<?>> sam1_3 = () -> Calendar.getInstance();
        SAM1<?> sam1_4 = () -> 1.5f;

        SAM2<Number> sam2 = a -> 1;
        SAM2<? extends Serializable> sam2_2 = a -> 1;
    }

    void method1(SAM1<?> s) {
        System.out.println("s.m1()=" + s.m1() + " s.m1().getClass()=" + s.m1().getClass());
          assertTrue(s.m1().equals(new Integer(1)));
    }

    interface SAM1<T> {
        T m1();
    }

    interface SAM2<T extends Serializable> {
        T m2(T t);
    }
}
