/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072008
 * @library /test/lib / ../patches
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 *
 * @build java.base/java.lang.invoke.MethodHandleHelper
 *        sun.hotspot.WhiteBox
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                                 -Xbatch -XX:-TieredCompilation -XX:CICompilerCount=1
 *                                 compiler.jsr292.NonInlinedCall.InvokeTest
 */

package compiler.jsr292.NonInlinedCall;

import jdk.internal.vm.annotation.DontInline;
import sun.hotspot.WhiteBox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleHelper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static jdk.test.lib.Asserts.assertEquals;

public class InvokeTest {
    static MethodHandles.Lookup LOOKUP = MethodHandleHelper.IMPL_LOOKUP;

    static final MethodHandle virtualMH; // invokevirtual   T.f1
    static final MethodHandle staticMH;  // invokestatic    T.f2
    static final MethodHandle intfMH;    // invokeinterface I.f3
    static final MethodHandle defaultMH; // invokevirtual   T.f3
    static final MethodHandle specialMH; // invokespecial   T.f4 T
    static final MethodHandle privateMH; // invokespecial   I.f4 T

    static final MethodHandle intrinsicMH; // invokevirtual Object.hashCode

    static final WhiteBox WB = WhiteBox.getWhiteBox();

    static volatile boolean doDeopt = false;

    static {
        try {
            MethodType mtype = MethodType.methodType(Class.class);

            virtualMH  = LOOKUP.findVirtual(T.class, "f1", mtype);
            staticMH   = LOOKUP.findStatic (T.class, "f2", mtype);
            intfMH     = LOOKUP.findVirtual(I.class, "f3", mtype);
            defaultMH  = LOOKUP.findVirtual(T.class, "f3", mtype);
            specialMH  = LOOKUP.findSpecial(T.class, "f4", mtype, T.class);
            privateMH  = LOOKUP.findSpecial(I.class, "f4", mtype, I.class);
            intrinsicMH = LOOKUP.findVirtual(Object.class, "hashCode", MethodType.methodType(int.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static class T implements I {
        @DontInline public        Class<?> f1() { if (doDeopt) WB.deoptimizeAll(); return T.class; }
        @DontInline public static Class<?> f2() { if (doDeopt) WB.deoptimizeAll(); return T.class; }
        @DontInline private       Class<?> f4() { if (doDeopt) WB.deoptimizeAll(); return T.class; }
    }

    static class P1 extends T {
        @DontInline public Class<?> f1() { if (doDeopt) WB.deoptimizeAll(); return P1.class; }
        @DontInline public Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return P1.class; }
    }

    static class P2 extends T {
        @DontInline public Class<?> f1() { if (doDeopt) WB.deoptimizeAll(); return P2.class; }
        @DontInline public Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return P2.class; }
    }

    interface I {
        @DontInline default Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return I.class; }
        @DontInline private Class<?> f4() { if (doDeopt) WB.deoptimizeAll(); return I.class; }
    }

    interface J1 extends I {
        @DontInline default Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return J1.class; }
    }

    interface J2 extends I {
        @DontInline default Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return J2.class; }
    }

    interface J3 extends I {
        @DontInline default Class<?> f3() { if (doDeopt) WB.deoptimizeAll(); return J3.class; }
    }

    static class Q1 extends T implements J1 {}
    static class Q2 extends T implements J2 {}
    static class Q3 extends T implements J3 {}

    static class H {
        public int hashCode() { return 0; }
    }

    @DontInline
    static void linkToVirtual(T recv, Class<?> expected) {
        try {
            Class<?> cls = (Class<?>)virtualMH.invokeExact(recv);
            assertEquals(cls, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToVirtualDefault(T recv, Class<?> expected) {
        try {
            Class<?> cls = (Class<?>)defaultMH.invokeExact(recv);
            assertEquals(cls, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToVirtualIntrinsic(Object recv, int expected) {
        try {
            int v = (int)intrinsicMH.invokeExact(recv);
            assertEquals(v, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToInterface(I recv, Class<?> expected) {
        try {
            Class<?> cls = (Class<?>)intfMH.invokeExact(recv);
            assertEquals(cls, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToStatic() {
        try {
            Class<?> cls = (Class<?>)staticMH.invokeExact();
            assertEquals(cls, T.class);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToSpecial(T recv, Class<?> expected) {
        try {
            Class<?> cls = (Class<?>)specialMH.invokeExact(recv);
            assertEquals(cls, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @DontInline
    static void linkToSpecialIntf(I recv, Class<?> expected) {
        try {
            Class<?> cls = (Class<?>)privateMH.invokeExact(recv);
            assertEquals(cls, expected);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static void run(Runnable r) {
        for (int i = 0; i < 20_000; i++) {
            r.run();
        }

        doDeopt = true;
        r.run();
        doDeopt = false;

        WB.clearInlineCaches();

        for (int i = 0; i < 20_000; i++) {
            r.run();
        }

        doDeopt = true;
        r.run();
        doDeopt = false;
    }

    static void testVirtual() {
        System.out.println("linkToVirtual");

        // Monomorphic case (optimized virtual call)
        run(() -> linkToVirtual(new T(), T.class));
        run(() -> linkToVirtualDefault(new T(), I.class));

        run(() -> linkToVirtualIntrinsic(new H(), 0));

        // Megamorphic case (optimized virtual call)
        run(() -> {
            linkToVirtual(new T() {}, T.class);
            linkToVirtual(new T() {}, T.class);
            linkToVirtual(new T() {}, T.class);
        });

        run(() -> {
            linkToVirtualDefault(new T(){}, I.class);
            linkToVirtualDefault(new T(){}, I.class);
            linkToVirtualDefault(new T(){}, I.class);
        });

        // Megamorphic case (virtual call), multiple implementations
        run(() -> {
            linkToVirtual(new T(),  T.class);
            linkToVirtual(new P1(), P1.class);
            linkToVirtual(new P2(), P2.class);
        });

        run(() -> {
            linkToVirtualDefault(new Q1(), J1.class);
            linkToVirtualDefault(new Q2(), J2.class);
            linkToVirtualDefault(new Q3(), J3.class);
        });
    }

    static void testInterface() {
        System.out.println("linkToInterface");

        // Monomorphic case (optimized virtual call), concrete target method
        run(() -> linkToInterface(new P1(), P1.class));

        // Monomorphic case (optimized virtual call), default target method
        run(() -> linkToInterface(new T(), I.class));

        // Megamorphic case (virtual call)
        run(() -> {
            linkToInterface(new T(),  I.class);
            linkToInterface(new P1(), P1.class);
            linkToInterface(new P2(), P2.class);
        });
    }

    static void testSpecial() {
        System.out.println("linkToSpecial");
        // Monomorphic case (optimized virtual call)
        run(() -> linkToSpecial(new T(), T.class));
        run(() -> linkToSpecialIntf(new T(), I.class));
    }

    static void testStatic() {
        System.out.println("linkToStatic");
        // static call
        run(() -> linkToStatic());
    }

    public static void main(String[] args) {
        testVirtual();
        testInterface();
        testSpecial();
        testStatic();
    }
}
