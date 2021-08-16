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
 * @bug 8046171
 * @summary Test direct and MethodHandle access to private interface methods using invokeinterface semantics
 *          to ensure all receiver typechecks occur as required.
 * @comment This complements SpecialInterfaceCall which tests invokespecial semantics.
 * @compile PrivateInterfaceCall.java
 * @compile PrivateInterfaceCallI4.jasm
 * @run main/othervm -Xint PrivateInterfaceCall
 * @run main/othervm -Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=1 PrivateInterfaceCall
 * @run main/othervm -Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=2 PrivateInterfaceCall
 * @run main/othervm -Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=3 PrivateInterfaceCall
 * @run main/othervm -Xbatch -XX:-TieredCompilation PrivateInterfaceCall
 */

// This is an adaptation of SpecialInterfaceCall to only use private interface methods and with
// virtual invocation semantics. Because we don't have the same corner cases as for invokespecial
// there's no practical difference between the I3 and I2 cases here. But we do have to ensure the
// correct versions of the methods get executed.
// In addition we add tests that involve calls from nestmates - which also covers the distinction
// between the caller being a class and being an interface.

import java.lang.invoke.*;

public class PrivateInterfaceCall {
    interface I1 {
        private void priv_m() { throw new Error("Should not call this"); };
    }
    interface I2 extends I1 {
        private void priv_m() { };

        static void invokeDirect(I2 i) {
            i.priv_m(); // generates invokeinterface
        }
        static void invokeInterfaceMH(I2 i) throws Throwable {
            // emulates behaviour of invokeDirect
            mh_I2_priv_m_from_I2.invokeExact(i);
        }
        // special case of invoking an Object method via an interface
        static void invokeInterfaceObjectMH(I2 i) throws Throwable {
            // emulates invokeInterface of I2.toString on i, which resolves
            // to Object.toString
            String s = (String) mh_I2_toString_from_I2.invokeExact(i);
        }
        // special case of invoking a final Object method via an interface
        static void invokeInterfaceObjectFinalMH(I2 i) throws Throwable {
            // emulates invokeInterface of I1.getClass on i, which resolves
            // to Object.getClass
            Class<?> c = (Class<?>) mh_I2_getClass_from_I2.invokeExact(i);
        }

        static void init() throws Throwable {
            MethodType mt = MethodType.methodType(void.class);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            mh_I2_priv_m_from_I2 = lookup.findVirtual(I2.class, "priv_m", mt);

            mt = MethodType.methodType(String.class);
            mh_I2_toString_from_I2 = lookup.findVirtual(I2.class, "toString", mt);

            mt = MethodType.methodType(Class.class);
            mh_I2_getClass_from_I2 = lookup.findVirtual(I2.class, "getClass", mt);
        }
    }
    interface I3 extends I2 {
        static void invokeInterfaceMH(I2 i) throws Throwable {
            // emulates behaviour of I2.invokeDirect
            mh_I2_priv_m_from_I3.invokeExact(i);
        }
        static void init() throws Throwable {
            MethodType mt = MethodType.methodType(void.class);
            mh_I2_priv_m_from_I3 = MethodHandles.lookup().findVirtual(I2.class, "priv_m", mt);
        }
    }

    // This interface acts like I2 but we define directInvoke* methods
    // that we will rewrite the bytecode of to use invokeinterface
    // (see PrivateInterfaceCallI4.jasm).
    interface I4 extends I1 {
        static void invokeDirect(I4 i) {
            // invokeinterface I4.toString()
            throw new Error("Class file for I4 is not overwritten");
        }
        static void invokeDirectFinal(I4 i) {
            // invokeinterface I4.getClass() - final method
            throw new Error("Class file for I4 is not overwritten");
        }
    }

    // check invocations from nestmates outside the
    // inheritance hierarchy - and from a class not interface
    static void invokeDirect(I2 i) {
        i.priv_m(); // generates invokeinterface
    }
    static void invokeInterfaceMH(I2 i) throws Throwable {
        mh_I2_priv_m_from_PIC.invokeExact(i);
    }

    // Concrete classes
    static class C2 implements I2 { }
    static class C3 implements I3 { }
    static class C4 implements I4 { }

    // Classes that don't implement I2/I3 but do have a
    // priv_m method in their hierarchy
    static class D1 implements I1 { }
    static class E {
        private void priv_m() { throw new Error("Should not call this"); }
    }

    // This MH acts like the invocation in I2.invokeDirect with caller I2
    static MethodHandle mh_I2_priv_m_from_I2;

    // This MH acts like the invocation in I3.invokeDirect with caller I3
    static MethodHandle mh_I2_priv_m_from_I3;

    // This MH acts like the invocation in PrivateInterfaceCall.invokeDirect
    // with caller PrivateInterfaceCall
    static MethodHandle mh_I2_priv_m_from_PIC;

   // This MH acts likes an invokeinterface of I2.toString from I2
    static MethodHandle mh_I2_toString_from_I2;

    // This MH acts likes an invokeinterface of I2.getClass from I2
    static MethodHandle mh_I2_getClass_from_I2;

    static {
        try {
            MethodType mt = MethodType.methodType(void.class);
            mh_I2_priv_m_from_PIC = MethodHandles.lookup().findVirtual(I2.class, "priv_m", mt);
            I2.init();
            I3.init();
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static void runPositiveTests() {
        shouldNotThrow(() -> PrivateInterfaceCall.invokeDirect(new C2()));
        shouldNotThrow(() -> PrivateInterfaceCall.invokeDirect(new C3()));
        shouldNotThrow(() -> PrivateInterfaceCall.invokeInterfaceMH(new C2()));
        shouldNotThrow(() -> PrivateInterfaceCall.invokeInterfaceMH(new C3()));

        shouldNotThrow(() -> I2.invokeDirect(new C2()));
        shouldNotThrow(() -> I2.invokeDirect(new C3()));
        shouldNotThrow(() -> I2.invokeInterfaceMH(new C2()));
        shouldNotThrow(() -> I2.invokeInterfaceMH(new C3()));
        shouldNotThrow(() -> I2.invokeInterfaceObjectMH(new C2()));
        shouldNotThrow(() -> I2.invokeInterfaceObjectMH(new C3()));
        shouldNotThrow(() -> I2.invokeInterfaceObjectFinalMH(new C2()));
        shouldNotThrow(() -> I2.invokeInterfaceObjectFinalMH(new C3()));

        // This looks odd but at runtime the only constraint is that the
        // receiver is an I2. In contrast in the invokespecial case the
        // receiver must be an I3.
        shouldNotThrow(() -> I3.invokeInterfaceMH(unsafeCastI3(new C2())));
        shouldNotThrow(() -> I3.invokeInterfaceMH(new C3()));

        shouldNotThrow(() -> I4.invokeDirect(new C4()));
        shouldNotThrow(() -> I4.invokeDirectFinal(new C4()));
    }

    static void runNegativeTests() {
        System.out.println("ICCE PrivateInterfaceCall.invokeDirect D1");
        shouldThrowICCE(() -> PrivateInterfaceCall.invokeDirect(unsafeCastI2(new D1())));
        System.out.println("ICCE PrivateInterfaceCall.invokeDirect E");
        shouldThrowICCE(() -> PrivateInterfaceCall.invokeDirect(unsafeCastI2(new E())));
        System.out.println("ICCE PrivateInterfaceCall.invokeInterfaceMH D1");
        shouldThrowICCE(() -> PrivateInterfaceCall.invokeInterfaceMH(unsafeCastI2(new D1())));
        System.out.println("ICCE PrivateInterfaceCall.invokeInterfaceMH E");
        shouldThrowICCE(() -> PrivateInterfaceCall.invokeInterfaceMH(unsafeCastI2(new E())));


        System.out.println("ICCE I2.invokeInterfaceMH D1");
        shouldThrowICCE(() -> I2.invokeInterfaceMH(unsafeCastI2(new D1())));
        System.out.println("ICCE I2.invokeInterfaceMH E");
        shouldThrowICCE(() -> I2.invokeInterfaceMH(unsafeCastI2(new E())));

        System.out.println("ICCE I2.invokeInterfaceObjectFinalMH D1");
        shouldThrowICCE(() -> I2.invokeInterfaceObjectFinalMH(unsafeCastI2(new D1())));
        System.out.println("ICCE I2.invokeInterfaceObjectFinalMH E");
        shouldThrowICCE(() -> I2.invokeInterfaceObjectFinalMH(unsafeCastI2(new E())));

        System.out.println("ICCE I3.invokeInterfaceMH D1");
        shouldThrowICCE(() -> I3.invokeInterfaceMH(unsafeCastI3(new D1())));
        System.out.println("ICCE I3.invokeInterfaceMH E");
        shouldThrowICCE(() -> I3.invokeInterfaceMH(unsafeCastI3(new E())));

        System.out.println("ICCE I4.invokeDirect D1");
        shouldThrowICCE(() -> I4.invokeDirect(unsafeCastI4(new D1())));
        System.out.println("ICCE I4.invokeDirect E");
        shouldThrowICCE(() -> I4.invokeDirect(unsafeCastI4(new E())));
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
                    "does not implement the requested interface", t);
    }

    // Depending on whether the exception originates in the linkResolver, the interpreter
    // or the compiler, the message can be different - which is unfortunate and could be
    // fixed. So we allow the listed reason or else a null message.
    static void shouldThrow(Class<?> expectedError, String reason, Test t) {
        try {
            t.run();
        } catch (Throwable e) {
            // Don't accept subclasses as they can hide unexpected failure modes
            if (expectedError == e.getClass()) {
                String msg = e.getMessage();
                if ((msg != null && msg.contains(reason)) || msg == null) {
                    // passed
                    System.out.println("Threw expected: " + e);
                    return;
                }
                else {
                    throw new AssertionError("Wrong exception reason: expected '" + reason
                                             + "', got '" + msg + "'", e);
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
