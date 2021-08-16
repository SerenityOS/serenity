/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010117
 * @summary Test if the VM enforces Reflection.getCallerClass
 *          be called by methods annotated with CallerSensitive
 * @modules java.base/jdk.internal.reflect
 * @build SetupGetCallerClass boot.GetCallerClass
 * @run driver SetupGetCallerClass
 * @run main/othervm -Xbootclasspath/a:bcp GetCallerClassTest
 */

import boot.GetCallerClass;
import java.lang.reflect.*;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

public class GetCallerClassTest {
    private final GetCallerClass gcc;   // boot.GetCallerClass is in bootclasspath
    GetCallerClassTest() {
        this.gcc = new GetCallerClass();
    }

    public static void main(String[] args) throws Exception {
        GetCallerClassTest gcct = new GetCallerClassTest();
        // ensure methods are annotated with @CallerSensitive
        ensureAnnotationPresent(boot.GetCallerClass.class, "getCallerLoader", true);
        ensureAnnotationPresent(GetCallerClassTest.class, "testNonSystemMethod", false);
        // call Reflection.getCallerClass from bootclasspath with and without @CS
        gcct.testCallerSensitiveMethods();
        // call Reflection.getCallerClass from classpath with @CS
        gcct.testNonSystemMethod();
    }

    private static void ensureAnnotationPresent(Class<?> c, String name, boolean cs)
        throws NoSuchMethodException
    {
        Method m = c.getDeclaredMethod(name);
        if (!m.isAnnotationPresent(CallerSensitive.class)) {
            throw new RuntimeException("@CallerSensitive not present in method " + m);
        }
        if (Reflection.isCallerSensitive(m) != cs) {
            throw new RuntimeException("Unexpected: isCallerSensitive returns " +
                Reflection.isCallerSensitive(m));
        }
    }

    private void testCallerSensitiveMethods() {
        try {
            ClassLoader cl = gcc.getCallerLoader();
            if (cl != GetCallerClassTest.class.getClassLoader()) {
                throw new RuntimeException("mismatched class loader");
            }
            gcc.missingCallerSensitiveAnnotation();
            throw new RuntimeException("getCallerLoader not marked with @CallerSensitive");
        } catch (InternalError e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            checkStackTrace(stackTrace, e);
            if (!stackTrace[1].getClassName().equals(GetCallerClass.class.getName()) ||
                !stackTrace[1].getMethodName().equals("missingCallerSensitiveAnnotation")) {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
            if (!stackTrace[2].getClassName().equals(GetCallerClassTest.class.getName()) ||
                !stackTrace[2].getMethodName().equals("testCallerSensitiveMethods")) {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    @CallerSensitive
    private void testNonSystemMethod() {
        try {
            Class<?> c = Reflection.getCallerClass();
            throw new RuntimeException("@CallerSensitive testNonSystemMethods not supported");
        } catch (InternalError e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            checkStackTrace(stackTrace, e);
            if (!stackTrace[1].getClassName().equals(GetCallerClassTest.class.getName()) ||
                !stackTrace[1].getMethodName().equals("testNonSystemMethod")) {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
            if (!stackTrace[2].getClassName().equals(GetCallerClassTest.class.getName()) ||
                !stackTrace[2].getMethodName().equals("main")) {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    private void checkStackTrace(StackTraceElement[] stackTrace, Error e) {
        if (stackTrace.length < 3) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }

        if (!stackTrace[0].getClassName().equals("jdk.internal.reflect.Reflection") ||
            !stackTrace[0].getMethodName().equals("getCallerClass")) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }

    }
}
