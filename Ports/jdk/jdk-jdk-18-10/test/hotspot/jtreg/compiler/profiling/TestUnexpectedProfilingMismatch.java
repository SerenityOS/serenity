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
 * @bug 8027631
 * @summary profiling of arguments at calls cannot rely on signature of callee for types
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:TieredStopAtLevel=3 -XX:TypeProfileLevel=111
 *                   -XX:Tier3InvocationThreshold=200 -XX:Tier0InvokeNotifyFreqLog=7
 *                   compiler.profiling.TestUnexpectedProfilingMismatch
 */

package compiler.profiling;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestUnexpectedProfilingMismatch {

    static class A {
    }

    static class B {
    }

    static void mA(A a) {
    }

    static void mB(B b) {
    }

    static final MethodHandle mhA;
    static final MethodHandle mhB;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class, A.class);
        MethodHandle res = null;
        try {
            res = lookup.findStatic(TestUnexpectedProfilingMismatch.class, "mA", mt);
        } catch(NoSuchMethodException ex) {
        } catch(IllegalAccessException ex) {
        }
        mhA = res;
        mt = MethodType.methodType(void.class, B.class);
        try {
            res = lookup.findStatic(TestUnexpectedProfilingMismatch.class, "mB", mt);
        } catch(NoSuchMethodException ex) {
        } catch(IllegalAccessException ex) {
        }
        mhB = res;
    }

    void m1(A a, boolean doit) throws Throwable {
        if (doit) {
            mhA.invoke(a);
        }
    }

    void m2(B b) throws Throwable {
        mhB.invoke(b);
    }

    static public void main(String[] args) {
        TestUnexpectedProfilingMismatch tih = new TestUnexpectedProfilingMismatch();
        A a = new A();
        B b = new B();
        try {
            for (int i = 0; i < 256 - 1; i++) {
                tih.m1(a, true);
            }
            // Will trigger the compilation but will also run once
            // more interpreted with a non null MDO which it will
            // update. Make it skip the body of the method.
            tih.m1(a, false);
            // Compile this one as well and do the profiling
            for (int i = 0; i < 256; i++) {
                tih.m2(b);
            }
            // Will run and see a conflict
            tih.m1(a, true);
        } catch(Throwable ex) {
            ex.printStackTrace();
        }
        System.out.println("TEST PASSED");
    }
}
