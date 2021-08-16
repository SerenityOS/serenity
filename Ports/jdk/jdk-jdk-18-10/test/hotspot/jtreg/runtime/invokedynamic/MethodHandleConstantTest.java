/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @bug 8188145
 * @compile MethodHandleConstantHelper.jasm
 * @run main runtime.invokedynamic.MethodHandleConstantTest
 */
package runtime.invokedynamic;

import java.lang.invoke.*;

public class MethodHandleConstantTest {
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    static final MethodType TEST_MT = MethodType.methodType(void.class);
    static final Class<?> TEST_CLASS;

    static {
       try {
          TEST_CLASS = Class.forName("runtime.invokedynamic.MethodHandleConstantHelper");
       } catch (ClassNotFoundException e) {
           throw new Error(e);
       }
    }

    static void test(String testName, Class<? extends Throwable> expectedError) {
        System.out.print(testName);
        try {
            LOOKUP.findStatic(TEST_CLASS, testName, TEST_MT).invokeExact();
        } catch (Throwable e) {
            if (expectedError.isInstance(e)) {
                // expected
            } else {
                e.printStackTrace();
                String msg = String.format("%s: wrong exception: %s, but %s expected",
                                           testName, e.getClass().getName(), expectedError.getName());
                throw new AssertionError(msg);
            }
        }
        System.out.println(": PASSED");
    }

    public static void main(String[] args) throws Throwable {
        test("testMethodSignatureResolutionFailure", NoSuchMethodError.class);
        test("testFieldSignatureResolutionFailure",  NoSuchFieldError.class);
    }
}
