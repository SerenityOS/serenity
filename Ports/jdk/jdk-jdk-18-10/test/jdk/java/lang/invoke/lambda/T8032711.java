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
 * @bug 8032711
 * @summary Issue with Lambda in handling
 */

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class T8032711 {

    interface I {
        void m();
    }

    static void here() {}
    static MethodHandles.Lookup l;
    static MethodHandle h;
    private static MethodType mt(Class<?> k) { return MethodType.methodType(k); }
    private static boolean mf(Class<?> k) {
        try {
            LambdaMetafactory.metafactory(l, "m",
                mt(I.class),mt(k),h,mt(void.class));
        } catch(LambdaConversionException e) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Throwable {
        l = MethodHandles.lookup();
        h = l.findStatic(T8032711.class, "here", mt(void.class));
        if (mf(void.class)) throw new AssertionError("Error: Should work");
        if (!mf(String.class)) throw new AssertionError("Error: Should fail");
    }
}
