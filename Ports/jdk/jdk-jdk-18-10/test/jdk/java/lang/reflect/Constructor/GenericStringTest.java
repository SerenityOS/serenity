/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5033583 6316717 6470106 8161500 8162539 6304578
 * @summary Check toGenericString() and toString() methods
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

public class GenericStringTest {
    public static void main(String argv[]) throws Exception{
        int failures = 0;

        for(Class<?> clazz: List.of(TestClass1.class, TestClass2.class))
            for(Constructor<?> ctor: clazz.getDeclaredConstructors()) {
                ExpectedGenericString egs = ctor.getAnnotation(ExpectedGenericString.class);
                String actual = ctor.toGenericString();
                System.out.println(actual);
                failures += checkForFailure(egs.value(), actual);

                if (ctor.isAnnotationPresent(ExpectedString.class)) {
                    failures += checkForFailure(ctor.getAnnotation(ExpectedString.class).value(),
                                                ctor.toString());
                }
            }

        if (failures > 0) {
            System.err.println("Test failed.");
            throw new RuntimeException();
        }
    }

    private static int checkForFailure(String expected, String actual) {
        if (!expected.equals(actual)) {
            System.err.printf("ERROR: Expected ''%s'';%ngot             ''%s''.\n",
                              expected, actual);
            return 1;
        } else
            return 0;
    }
}

class TestClass1 {
    @ExpectedGenericString(
   "TestClass1(int,double)")
    TestClass1(int x, double y) {}

    @ExpectedGenericString(
   "private TestClass1(int,int)")
    private TestClass1(int x, int param2) {}

    @ExpectedGenericString(
   "private TestClass1(java.lang.Object) throws java.lang.RuntimeException")
    @ExpectedString(
   "private TestClass1(java.lang.Object) throws java.lang.RuntimeException")
    private TestClass1(Object o) throws RuntimeException {}

    @ExpectedGenericString(
   "protected <S,T> TestClass1(S,T) throws java.lang.Exception")
    @ExpectedString(
   "protected TestClass1(java.lang.Object,java.lang.Object) throws java.lang.Exception")
    protected <S, T> TestClass1(S s, T t) throws Exception{}

    @ExpectedGenericString(
   "protected <V extends java.lang.Number & java.lang.Runnable> TestClass1(V)")
    @ExpectedString(
   "protected TestClass1(java.lang.Number)")
    protected <V extends Number & Runnable> TestClass1(V v){}

    @ExpectedGenericString(
   "<E extends java.lang.Exception> TestClass1() throws E")
    @ExpectedString(
   "TestClass1() throws java.lang.Exception")
    <E extends Exception> TestClass1() throws E {}

    @ExpectedGenericString(
   "TestClass1(java.lang.Object...)")
    @ExpectedString(
   "TestClass1(java.lang.Object[])")
    TestClass1(Object... o){}
}

class TestClass2<E> {
    @ExpectedGenericString(
   "public <T> TestClass2(E,T)")
    public <T> TestClass2(E e, T t) {}
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedGenericString {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedString {
    String value();
}
