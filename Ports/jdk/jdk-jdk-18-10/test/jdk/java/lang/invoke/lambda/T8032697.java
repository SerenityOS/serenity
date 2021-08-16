/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032697
 * @summary Issues with Lambda
 */

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import T8032697_anotherpkg.T8032697_A;

public class T8032697 extends T8032697_A {

    interface I {
        int m();
    }

    interface IA {
        int m(T8032697_A x);
    }

    static MethodHandles.Lookup l;
    static MethodHandle h;
    private static MethodType mt(Class<?> k) { return MethodType.methodType(k); }
    private static MethodType mt(Class<?> k, Class<?> k2) { return MethodType.methodType(k, k2); }
    private static boolean mf(MethodType mti, MethodType mtf) {
        try {
            LambdaMetafactory.metafactory(l, "m", mti,mtf,h,mtf);
        } catch(LambdaConversionException e) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Throwable {
        l = MethodHandles.lookup();
        h = l.findVirtual(T8032697_A.class, "f", mt(int.class));
        if (mf(mt(I.class, T8032697.class), mt(int.class))) throw new AssertionError("Error: Should work");
        if (mf(mt(IA.class), mt(int.class, T8032697.class))) throw new AssertionError("Error: Should work");
        if (!mf(mt(I.class, T8032697_A.class), mt(int.class))) throw new AssertionError("Error: Should fail");
        if (!mf(mt(IA.class), mt(int.class, T8032697_A.class))) throw new AssertionError("Error: Should fail");
    }
}
