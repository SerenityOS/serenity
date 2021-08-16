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
 * @bug 8041458
 * @summary profiling of arguments in C1 at MethodHandle invoke of intrinsic tries to profile popped argument.
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:TieredStopAtLevel=3
 *                   compiler.profiling.TestMethodHandleInvokesIntrinsic
 *
 */

package compiler.profiling;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestMethodHandleInvokesIntrinsic {

    static final MethodHandle mh_nanoTime;
    static final MethodHandle mh_getClass;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(long.class);
        MethodHandle MH = null;
        try {
            MH = lookup.findStatic(System.class, "nanoTime", mt);
        } catch(NoSuchMethodException nsme) {
            nsme.printStackTrace();
            throw new RuntimeException("TEST FAILED", nsme);
        } catch(IllegalAccessException iae) {
            iae.printStackTrace();
            throw new RuntimeException("TEST FAILED", iae);
        }
        mh_nanoTime = MH;

        mt = MethodType.methodType(Class.class);
        MH = null;
        try {
            MH = lookup.findVirtual(Object.class, "getClass", mt);
        } catch(NoSuchMethodException nsme) {
            nsme.printStackTrace();
            throw new RuntimeException("TEST FAILED", nsme);
        } catch(IllegalAccessException iae) {
            iae.printStackTrace();
            throw new RuntimeException("TEST FAILED", iae);
        }
        mh_getClass = MH;
    }

    static long m1() throws Throwable {
        return (long)mh_nanoTime.invokeExact();
    }

    static Class m2(Object o) throws Throwable {
        return (Class)mh_getClass.invokeExact(o);
    }

    static public void main(String[] args) {
        try {
            for (int i = 0; i < 20000; i++) {
                m1();
            }
            TestMethodHandleInvokesIntrinsic o = new TestMethodHandleInvokesIntrinsic();
            for (int i = 0; i < 20000; i++) {
                m2(o);
            }
        } catch(Throwable t) {
            System.out.println("Unexpected exception");
            t.printStackTrace();
            throw new RuntimeException("TEST FAILED", t);
        }

        System.out.println("TEST PASSED");
    }
}
