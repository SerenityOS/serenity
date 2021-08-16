/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8200167 8010319
 * @summary Test direct and MethodHandle access to interface methods using invokespecial semantics
 * @comment This must be compiled so invokespecial is used
 * @compile -XDdisableVirtualizedPrivateInvoke SpecialInterfaceCall.java
 * @compile SpecialInterfaceCallI4.jasm
 * @run main/othervm -Xint SpecialInterfaceCall
 * @run main/othervm -Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=1 SpecialInterfaceCall
 * @run main/othervm -Xbatch -XX:-TieredCompilation SpecialInterfaceCall
 */

import java.lang.invoke.*;

public class SpecialInterfaceCall {
    interface I1 {
        default void pub_m() {};
        private void priv_m() {};
    }
    interface I2 extends I1 {
        // This needs to be a public method to avoid access control issues,
        // but logically we treat it as private and emulate invokespecial
        // using MethodHandles.
        default void pub_m() {};

        private void priv_m() {};

        static void invokeDirect(I2 i) {
            i.priv_m(); // generates invokespecial
        }
        static void invokeSpecialMH(I2 i) throws Throwable {
            // emulates behaviour of invokeDirect
            mh_I2_priv_m_from_I2.invokeExact(i);
        }
        // special case of invoking an Object method via an interface
        static void invokeSpecialObjectMH(I2 i) throws Throwable {
            // emulates invokespecial of I1.toString on i, which resolves
            // to Object.toString
            String s = (String) mh_I1_toString_from_I2.invokeExact(i);
        }
        // special case of invoking a final Object method via an interface
        static void invokeSpecialObjectFinalMH(I2 i) throws Throwable {
            // emulates invokespecial of I1.getClass on i, which resolves
            // to Object.getClass
            Class<?> c = (Class<?>) mh_I1_getClass_from_I2.invokeExact(i);
        }
    }
    interface I3 extends I2 {
        // Must take an I3 here rather than I2 else we get
        // WrongMethodTypeException: expected (I3)void but found (I2)void
        // Statically the receiver type is bounded by the caller type.
        static void invokeSpecialMH(I3 i) throws Throwable {
            // emulates an invokespecial of ((I2)i).pub_m()
            mh_I2_pub_m_from_I3.invokeExact(i);
        }
    }
    // This interface acts like I2 but we define directInvoke* methods
    // that we will rewrite the bytecode of to use invokespecial
    // (see SpecialInterfaceCallI4.jasm).
    interface I4 extends I1 {
        static void invokeDirect(I4 i) {
            // invokeSpecial Object.toString()
            throw new Error("Class file for I4 is not overwritten");
        }
        static void invokeDirectFinal(I4 i) {
            // invokeSpecial Object.getClass() - final method
            throw new Error("Class file for I4 is not overwritten");
        }
    }

    // Concrete classes
    static class C1 implements I1 { }
    static class C2 implements I2 { }
    static class C3 implements I3 { }
    static class C4 implements I4 { }

    // Classes that don't implement I2/I3 but do have a
    // priv_m/pub_m method in their hierarchy
    static class D1 implements I1 { }
    static class E {
        public void pub_m() {}
        private void priv_m() {}
    }

    // This MH acts like the direct invokespecial in I2.invokeDirect
    static final MethodHandle mh_I2_priv_m_from_I2;

    // This MH acts like an invokespecial of I2.pub_m from I3
    static final MethodHandle mh_I2_pub_m_from_I3;

    // This MH acts likes an invokespecial of I1.toString from I2
    static final MethodHandle mh_I1_toString_from_I2;

    // This MH acts likes an invokespecial of I1.getClass from I2
    static final MethodHandle mh_I1_getClass_from_I2;

    static {
        try {
            MethodType mt = MethodType.methodType(void.class);
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            mh_I2_priv_m_from_I2 = lookup.findSpecial(I2.class, "priv_m", mt, I2.class);
            mh_I2_pub_m_from_I3 = lookup.findSpecial(I2.class, "pub_m", mt, I3.class);

            mt = MethodType.methodType(String.class);
            mh_I1_toString_from_I2 = lookup.findSpecial(I1.class, "toString", mt, I2.class);

            mt = MethodType.methodType(Class.class);
            mh_I1_getClass_from_I2 = lookup.findSpecial(I1.class, "getClass", mt, I2.class);

        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static void runPositiveTests() {
        shouldNotThrow(() -> I2.invokeDirect(new C2()));
        shouldNotThrow(() -> I2.invokeDirect(new C3()));
        shouldNotThrow(() -> I2.invokeSpecialMH(new C2()));
        shouldNotThrow(() -> I2.invokeSpecialMH(new C3()));
        shouldNotThrow(() -> I2.invokeSpecialObjectMH(new C2()));
        shouldNotThrow(() -> I2.invokeSpecialObjectMH(new C3()));
        shouldNotThrow(() -> I2.invokeSpecialObjectFinalMH(new C2()));
        shouldNotThrow(() -> I2.invokeSpecialObjectFinalMH(new C3()));

        shouldNotThrow(() -> I3.invokeSpecialMH(new C3()));

        shouldNotThrow(() -> I4.invokeDirect(new C4()));
        shouldNotThrow(() -> I4.invokeDirectFinal(new C4()));
    }

    static void runNegativeTests() {
        System.out.println("IAE I2.invokeDirect D1");
        shouldThrowIAE(() -> I2.invokeDirect(unsafeCastI2(new D1())));
        System.out.println("IAE I2.invokeDirect E");
        shouldThrowIAE(() -> I2.invokeDirect(unsafeCastI2(new E())));
        System.out.println("ICCE I2.invokeMH D1");
        shouldThrowICCE(() -> I2.invokeSpecialMH(unsafeCastI2(new D1())));
        System.out.println("ICCE I2.invokeMH E");
        shouldThrowICCE(() -> I2.invokeSpecialMH(unsafeCastI2(new E())));
        System.out.println("ICCE I3.invokeMH D1");
        shouldThrowICCE(() -> I3.invokeSpecialMH(unsafeCastI3(new D1())));
        System.out.println("ICCE I3.invokeMH E");
        shouldThrowICCE(() -> I3.invokeSpecialMH(unsafeCastI3(new E())));
        System.out.println("ICCE I3.invokeMH C2");
        shouldThrowICCE(() -> I3.invokeSpecialMH(unsafeCastI3(new C2())));
        System.out.println("ICCE I4.invokeDirect C1");
        shouldThrowIAE(() -> I4.invokeDirect(unsafeCastI4(new C1())));
        System.out.println("ICCE I4.invokeDirectFinal C1");
        shouldThrowIAE(() -> I4.invokeDirectFinal(unsafeCastI4(new C1())));
        System.out.println("ICCE I2.invokeObjectMH C1");
        shouldThrowICCE(() -> I2.invokeSpecialObjectMH(unsafeCastI2(new C1())));
        System.out.println("ICCE I2.invokeObjectFinalMH C1");
        shouldThrowICCE(() -> I2.invokeSpecialObjectFinalMH(unsafeCastI2(new C1())));

    }

    static void warmup() {
        for (int i = 0; i < 20_000; i++) {
            runPositiveTests();
        }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("UNRESOLVED:");
        runNegativeTests();
        runPositiveTests();

        System.out.println("RESOLVED:");
        runNegativeTests();

        System.out.println("WARMUP:");
        warmup();

        System.out.println("COMPILED:");
        runNegativeTests();
        runPositiveTests();
    }

    static interface Test {
        void run() throws Throwable;
    }

    static void shouldThrowICCE(Test t) {
        shouldThrow(IncompatibleClassChangeError.class,
                    "is not a subclass of caller class", t);
    }

    static void shouldThrowIAE(Test t) {
        shouldThrow(IllegalAccessError.class,
                    "must be the current class or a subtype of interface", t);
    }

    static void shouldThrow(Class<?> expectedError, String reason, Test t) {
        try {
            t.run();
        } catch (Throwable e) {
            if (expectedError.isInstance(e)) {
                if (e.getMessage().contains(reason)) {
                    // passed
                    System.out.println("Threw expected: " + e);
                    return;
                }
                else {
                    throw new AssertionError("Wrong exception reason: expected '" + reason
                                             + "', got '" + e.getMessage() + "'", e);
                }
            } else {
                String msg = String.format("Wrong exception thrown: expected=%s; thrown=%s",
                                           expectedError.getName(), e.getClass().getName());
                throw new AssertionError(msg, e);
            }
        }
        throw new AssertionError("No exception thrown: expected " + expectedError.getName());
    }

    static void shouldNotThrow(Test t) {
        try {
            t.run();
            // passed
        } catch (Throwable e) {
            throw new AssertionError("Exception was thrown: ", e);
        }
    }

    // Note: these unsafe casts are only possible for interface types

    static I2 unsafeCastI2(Object obj) {
        try {
            MethodHandle mh = MethodHandles.identity(Object.class);
            mh = MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(I2.class));
            return (I2)mh.invokeExact((Object) obj);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static I3 unsafeCastI3(Object obj) {
        try {
            MethodHandle mh = MethodHandles.identity(Object.class);
            mh = MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(I3.class));
            return (I3)mh.invokeExact((Object) obj);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static I4 unsafeCastI4(Object obj) {
        try {
            MethodHandle mh = MethodHandles.identity(Object.class);
            mh = MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(I4.class));
            return (I4)mh.invokeExact((Object) obj);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }
}
