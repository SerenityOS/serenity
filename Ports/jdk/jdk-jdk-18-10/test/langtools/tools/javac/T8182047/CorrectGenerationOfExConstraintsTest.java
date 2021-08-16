/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182047
 * @summary javac compile error on type-parameter-exceptions in lambda expressions
 * @compile CorrectGenerationOfExConstraintsTest.java
 */

public class CorrectGenerationOfExConstraintsTest {
    public static class MyExBase extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static class MyEx1 extends MyExBase {
        private static final long serialVersionUID = 1L;
    }

    public static class MyEx2 extends MyExBase {
        private static final long serialVersionUID = 1L;
    }

    public interface MyLambdaIF1 <E extends Exception> {
        void lambdaFun() throws E, MyEx2;
    }

    public interface MyLambdaIF2 <E extends Exception> {
        void lambdaFun() throws MyEx2, E;
    }

    public <E extends Exception> void fun1(MyLambdaIF1<E> myLambda) throws E, MyEx2 {
        myLambda.lambdaFun();
    }

    public <E extends Exception> void fun2(MyLambdaIF2<E> myLambda) throws E, MyEx2 {
        myLambda.lambdaFun();
    }

    public void useIt1() throws MyEx1, MyEx2 {
        fun1(this::lambda);
    }

    public void useIt1a() throws MyExBase {
        fun1(this::lambda);
    }

    public void useIt2() throws MyEx1, MyEx2 {
        fun2(this::lambda);
    }

    public void lambda() throws MyEx1, MyEx2 {
        if (Math.random() > 0.5)
        throw new MyEx2();
        throw new MyEx1();
    }
}
