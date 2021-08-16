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
 *  This test is for generic methods whose type is the same as the type
            of the generic SAM interface that is taken as the parameter of the
            generic method; the type can be inferred from the value of the other
            type arguments
 * @compile InferenceTest4.java
 * @run main InferenceTest4
 */

import java.util.List;
import java.util.ArrayList;

public class InferenceTest4 {

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    public static void main(String[] args) {
        InferenceTest4 test = new InferenceTest4();
        test.method1(n -> n.size(), "abc", "java.lang.String");
        test.method1(n -> n.size(), 'c', "java.lang.Character");
        test.method1(n -> n.size(), 0, "java.lang.Integer");
        test.method1(n -> n.size(), 0.1, "java.lang.Double");
        test.method1(n -> n.size(), 0.1f, "java.lang.Float");
        test.method1(n -> n.size(), 0L, "java.lang.Long");
        test.method1(n -> n.size(), (short)0, "java.lang.Short");
        test.method1(n -> n.size(), (byte)0, "java.lang.Byte");
        test.method1(n -> n.size(), true, "java.lang.Boolean");
        test.method1(n -> n.size(), new int[]{1, 2, 3}, "[I");
        test.method1(n -> n.size(), new double[]{1.0}, "[D");
        test.method1(n -> n.size(), new String[]{}, "[Ljava.lang.String;");
    }

    <T> void method1(SAM1<T> s, T t, String className) {
        List<T> list = new ArrayList<T>();
        System.out.println(className + "-" + t.getClass().getName());
        assertTrue(t.getClass().getName().equals(className));
        list.add(t);
        assertTrue(s.m1(list) == 1);
    }

    interface SAM1<T> {
        int m1(List<T> x);
    }
}
