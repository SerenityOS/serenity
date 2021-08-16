/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmci
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot:open
 * @library /compiler/jvmci/jdk.vm.ci.hotspot.test/src
 * @run testng/othervm
 *      -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler
 *      jdk.vm.ci.hotspot.test.TestTranslatedException
 */

package jdk.vm.ci.hotspot.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTranslatedException {
    @SuppressWarnings("serial")
    public static class Untranslatable extends RuntimeException {
        public Untranslatable(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void encodeDecodeTest() throws Exception {

        Class<?> translatedExceptionClass = Class.forName("jdk.vm.ci.hotspot.TranslatedException");

        Method encode = translatedExceptionClass.getDeclaredMethod("encodeThrowable", Throwable.class);
        Method decode = translatedExceptionClass.getDeclaredMethod("decodeThrowable", String.class);
        encode.setAccessible(true);
        decode.setAccessible(true);

        Throwable throwable = new ExceptionInInitializerError(new InvocationTargetException(new Untranslatable("test exception", new NullPointerException()), "invoke"));
        for (int i = 0; i < 10; i++) {
            throwable = new ExceptionInInitializerError(new InvocationTargetException(new RuntimeException(String.valueOf(i), throwable), "invoke"));
        }
        String encoding = (String) encode.invoke(null, throwable);
        Throwable decoded = (Throwable) decode.invoke(null, encoding);
        assertThrowableEquals(throwable, decoded);
    }

    private static void assertThrowableEquals(Throwable original, Throwable decoded) {
        try {
            Assert.assertEquals(original == null, decoded == null);
            while (original != null) {
                if (Untranslatable.class.equals(original.getClass())) {
                    Assert.assertEquals("jdk.vm.ci.hotspot.TranslatedException", decoded.getClass().getName());
                    Assert.assertEquals("jdk.vm.ci.hotspot.TranslatedException[jdk.vm.ci.hotspot.test.TestTranslatedException$Untranslatable]: test exception", decoded.toString());
                    Assert.assertEquals("test exception", original.getMessage());
                } else {
                    Assert.assertEquals(original.getClass().getName(), decoded.getClass().getName());
                    Assert.assertEquals(original.getMessage(), decoded.getMessage());
                }
                StackTraceElement[] originalStack = original.getStackTrace();
                StackTraceElement[] decodedStack = decoded.getStackTrace();
                Assert.assertEquals(originalStack.length, decodedStack.length);
                for (int i = 0, n = originalStack.length; i < n; ++i) {
                    StackTraceElement originalStackElement = originalStack[i];
                    StackTraceElement decodedStackElement = decodedStack[i];
                    Assert.assertEquals(originalStackElement.getClassLoaderName(), decodedStackElement.getClassLoaderName());
                    Assert.assertEquals(originalStackElement.getModuleName(), decodedStackElement.getModuleName());
                    Assert.assertEquals(originalStackElement.getClassName(), decodedStackElement.getClassName());
                    Assert.assertEquals(originalStackElement.getMethodName(), decodedStackElement.getMethodName());
                    Assert.assertEquals(originalStackElement.getFileName(), decodedStackElement.getFileName());
                    Assert.assertEquals(originalStackElement.getLineNumber(), decodedStackElement.getLineNumber());
                }
                original = original.getCause();
                decoded = decoded.getCause();
            }
        } catch (AssertionError e) {
            System.err.println("original:[");
            original.printStackTrace(System.err);
            System.err.println("]");
            System.err.println("decoded:[");
            original.printStackTrace(System.err);
            System.err.println("]");
            throw e;
        }
    }
}
