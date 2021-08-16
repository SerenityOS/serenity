/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8062280
 * @summary C2: inlining failure due to access checks being too strict
 *
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @run driver compiler.jsr292.MHInlineTest
 */

package compiler.jsr292;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static jdk.test.lib.Asserts.assertEquals;

public class MHInlineTest {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+IgnoreUnrecognizedVMOptions", "-showversion",
                "-XX:-TieredCompilation", "-Xbatch",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                "-XX:CompileCommand=dontinline,compiler.jsr292.MHInlineTest::test*",
                 Launcher.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);

        // The test is applicable only to C2 (present in Server VM).
        if (analyzer.getStderr().contains("Server VM")) {
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$B::public_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$B::protected_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$B::package_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$A::package_final_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$B::private_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$B::private_static_x (3 bytes)   inline (hot)");
            analyzer.shouldContain("compiler.jsr292.MHInlineTest$A::package_static_x (3 bytes)   inline (hot)");
        } else {
            throw new SkippedException("The test is applicable only to C2 (present in Server VM)");
        }
    }

    static class A {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        public Class<?>         public_x() { return A.class; }
        protected Class<?>   protected_x() { return A.class; }
        Class<?>               package_x() { return A.class; }
        final Class<?>   package_final_x() { return A.class; }

        static Class<?> package_static_x() { return A.class; }
    }

    static class B extends A {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        @Override public    Class<?>    public_x() { return B.class; }
        @Override protected Class<?> protected_x() { return B.class; }
        @Override Class<?>             package_x() { return B.class; }

        private   Class<?>             private_x() { return B.class; }
        static    Class<?>      private_static_x() { return B.class; }
    }

    static final MethodHandle A_PUBLIC_X;
    static final MethodHandle A_PROTECTED_X;
    static final MethodHandle A_PACKAGE_X;
    static final MethodHandle A_PACKAGE_STATIC_X;
    static final MethodHandle A_PACKAGE_FINAL_X;

    static final MethodHandle B_PRIVATE_X;
    static final MethodHandle B_PRIVATE_STATIC_X;

    static {
        try {
            MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

            A_PUBLIC_X = LOOKUP.findVirtual(
                    A.class, "public_x", MethodType.methodType(Class.class));
            A_PROTECTED_X = LOOKUP.findVirtual(
                    A.class, "protected_x", MethodType.methodType(Class.class));
            A_PACKAGE_X = LOOKUP.findVirtual(
                    A.class, "package_x", MethodType.methodType(Class.class));
            A_PACKAGE_FINAL_X = LOOKUP.findVirtual(
                    A.class, "package_final_x", MethodType.methodType(Class.class));
            A_PACKAGE_STATIC_X = LOOKUP.findStatic(
                    A.class, "package_static_x", MethodType.methodType(Class.class));

            B_PRIVATE_X = B.LOOKUP.findVirtual(
                    B.class, "private_x", MethodType.methodType(Class.class));
            B_PRIVATE_STATIC_X = B.LOOKUP.findStatic(
                    B.class, "private_static_x", MethodType.methodType(Class.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static final A a = new B();

    private static void testPublicMH() {
        try {
            Class<?> r = (Class<?>)A_PUBLIC_X.invokeExact(a);
            assertEquals(r, B.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testProtectedMH() {
        try {
            Class<?> r = (Class<?>)A_PROTECTED_X.invokeExact(a);
            assertEquals(r, B.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testPackageMH() {
        try {
            Class<?> r = (Class<?>)A_PACKAGE_X.invokeExact(a);
            assertEquals(r, B.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testPackageFinalMH() {
        try {
            Class<?> r = (Class<?>)A_PACKAGE_FINAL_X.invokeExact(a);
            assertEquals(r, A.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testPackageStaticMH() {
        try {
            Class<?> r = (Class<?>)A_PACKAGE_STATIC_X.invokeExact();
            assertEquals(r, A.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testPrivateMH() {
        try {
            Class<?> r = (Class<?>)B_PRIVATE_X.invokeExact((B)a);
            assertEquals(r, B.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    private static void testPrivateStaticMH() {
        try {
            Class<?> r = (Class<?>)B_PRIVATE_STATIC_X.invokeExact();
            assertEquals(r, B.class);
        } catch (Throwable throwable) {
            throw new Error(throwable);
        }
    }

    static class Launcher {
        public static void main(String[] args) throws Exception {
            for (int i = 0; i < 20_000; i++) {
                testPublicMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testProtectedMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testPackageMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testPackageFinalMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testPackageStaticMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testPrivateMH();
            }
            for (int i = 0; i < 20_000; i++) {
                testPrivateStaticMH();
            }
        }
    }
}
