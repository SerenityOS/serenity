/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008077 8029721
 * @summary new type annotation location: lambda expressions
 * javac crash for annotated parameter type of lambda in a field
 * @compile Lambda.java
 * @author Werner Dietl
 */

import java.lang.annotation.*;

public class Lambda {

    interface LambdaInt {
        <S, T> void generic(S p1, T p2);
    }

    static class LambdaImpl implements LambdaInt {
        <S, T> LambdaImpl(S p1, T p2) {}
        public <S, T> void generic(S p1, T p2) {}
    }

    LambdaInt getMethodRefTA(LambdaImpl r) {
        return r::<@TA Object, @TB Object>generic;
    }

    LambdaInt getConstructorRefTA() {
        return LambdaImpl::<@TA Object, @TB Object>new;
    }

    interface LambdaInt2 {
        void lambda(Object p1, Object p2);
    }

    LambdaInt2 getLambda() {
        return (@TA Object x, @TB Object y) -> { @TA Object l = null; System.out.println("We have: " + (@TB Object) x); };
    }

    java.util.function.IntUnaryOperator x = (@TA int y) -> 1;

    static java.util.function.IntUnaryOperator xx = (@TA int y) -> 1;

    java.util.function.IntUnaryOperator foo() {
        return (@TA int y) -> 2;
    }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TA { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TB { }
