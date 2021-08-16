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

package p1;

import p2.T3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.concurrent.Callable;

class T1 {
    protected        void m1() {}
    protected static void m2() {}
}

public class T2 extends T1 {
    public static void main(String[] args) throws Throwable {
        Lookup LOOKUP = T3.lookup();
        Class<IllegalAccessException> IAE = IllegalAccessException.class;

        assertFailure(IAE, () -> LOOKUP.findVirtual(T1.class, "m1", MethodType.methodType(void.class)));
        assertFailure(IAE, () -> LOOKUP.findStatic(T1.class, "m2", MethodType.methodType(void.class)));

        assertSuccess(() -> LOOKUP.findVirtual(T2.class, "m1", MethodType.methodType(void.class)));
        assertSuccess(() -> LOOKUP.findVirtual(T3.class, "m1", MethodType.methodType(void.class)));

        assertSuccess(() -> LOOKUP.findStatic(T2.class, "m2", MethodType.methodType(void.class)));
        assertSuccess(() -> LOOKUP.findStatic(T3.class, "m2", MethodType.methodType(void.class)));

        assertFailure(IAE, () -> LOOKUP.unreflect(T1.class.getDeclaredMethod("m1")));
        assertFailure(IAE, () -> LOOKUP.unreflect(T1.class.getDeclaredMethod("m2")));

        System.out.println("TEST PASSED");
    }

    public static void assertFailure(Class<? extends Throwable> expectedError, Callable r) {
        try {
            r.call();
        } catch(Throwable e) {
            if (expectedError.isAssignableFrom(e.getClass())) {
                return; // expected error
            } else {
                throw new Error("Unexpected error type: "+e.getClass()+"; expected type: "+expectedError, e);
            }
        }
        throw new Error("No error");
    }

    public static void assertSuccess(Callable r) {
        try {
            r.call();
        } catch(Throwable e) {
            throw new Error("Unexpected error", e);
        }
    }
}
