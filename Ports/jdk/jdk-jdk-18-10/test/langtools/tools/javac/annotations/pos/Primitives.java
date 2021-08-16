/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5034991 5040842 5040853
 * @summary Modify class-file representation of Class-valued annotation elements
 * @author gafter
 */

public class Primitives {
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface A {
        Class value() default void.class;
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface B {
        Class[] value() default { void.class };
    }

    @A
    @B
    static class T1 {}

    @A(int.class)
    @B({void.class, byte.class, char.class, short.class, int.class, long.class,
        boolean.class, float.class, double.class, A[].class, int[].class})
    static class T2 {}

    static void check(Object actual, Object expected) {
        if (actual != expected)
            throw new Error("expected: " + expected + "; actual = " + actual);
    }

    public static void main(String[] args) {
        check(T1.class.getAnnotation(A.class).value(), void.class);
        check(T1.class.getAnnotation(B.class).value().length, 1);
        check(T1.class.getAnnotation(B.class).value()[0], void.class);

        check(T2.class.getAnnotation(A.class).value(), int.class);
        check(T2.class.getAnnotation(B.class).value().length, 11);
        check(T2.class.getAnnotation(B.class).value()[0], void.class);
        check(T2.class.getAnnotation(B.class).value()[1], byte.class);
        check(T2.class.getAnnotation(B.class).value()[2], char.class);
        check(T2.class.getAnnotation(B.class).value()[3], short.class);
        check(T2.class.getAnnotation(B.class).value()[4], int.class);
        check(T2.class.getAnnotation(B.class).value()[5], long.class);
        check(T2.class.getAnnotation(B.class).value()[6], boolean.class);
        check(T2.class.getAnnotation(B.class).value()[7], float.class);
        check(T2.class.getAnnotation(B.class).value()[8], double.class);
        check(T2.class.getAnnotation(B.class).value()[9], A[].class);
        check(T2.class.getAnnotation(B.class).value()[10], int[].class);
    }
}
