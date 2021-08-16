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
 *   Test that the most specific reference is selected when method parameters are elided
 * @compile MethodRef6.java
 * @run main MethodRef6
 */

public class MethodRef6 {

    static interface A { String make(Integer i); }

    static interface B { String make(Number i); }

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    static String m(Object o) {
        return "Object " + o;
    }

    static String m(Number n) {
        return "Number " + n;
    }

    static String m(Integer i) {
        return "Integer " + i;
    }

    static String m(int i) {
        return "int " + i;
    }

    public static void main(String[] args) {
        A a = MethodRef6::m;
        assertTrue(a.make(1).equals("Integer 1"));//method parameter type inferred from SAM descriptor, boxing applied
        B b = MethodRef6::m;
        assertTrue(b.make(1).equals("Number 1"));//method parameter type inferred from SAM descriptor, boxing and widen applied
    }
}
