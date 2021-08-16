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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract class for lambda forms caching testing.
 *
 * @author kshefov
 */
public abstract class LFCachingTestCase extends LambdaFormTestCase {

    /**
     * Constructor for lambda forms caching test case.
     *
     * @param testMethod A method from {@code j.l.i.MethodHandles} class that
     * returns a {@code j.l.i.MethodHandle} instance.
     */
    protected LFCachingTestCase(TestMethods testMethod) {
        super(testMethod);
    }

    /**
     * Checks that the lambda forms of the two adapter method handles adapter1
     * and adapter2 are the same.
     *
     * @param adapter1 First method handle.
     * @param adapter2 Second method handle.
     */
    public void checkLFCaching(MethodHandle adapter1, MethodHandle adapter2) {
        try {

            if (!adapter1.type().equals(adapter2.type())) {
                throw new Error("TESTBUG: Types of the two method handles are not the same");
            }

            Object lambdaForm0 = LambdaFormTestCase.INTERNAL_FORM.invoke(adapter1);
            Object lambdaForm1 = LambdaFormTestCase.INTERNAL_FORM.invoke(adapter2);

            if (lambdaForm0 == null || lambdaForm1 == null) {
                throw new Error("Unexpected error: One or both lambda forms of the method handles are null");
            }

            if (lambdaForm0 != lambdaForm1) {
                // Since LambdaForm caches are based on SoftReferences, GC can cause element eviction.
                if (noGCHappened()) {
                    System.err.println("Lambda form 0 toString is:");
                    System.err.println(lambdaForm0);
                    System.err.println("Lambda form 1 toString is:");
                    System.err.println(lambdaForm1);
                    throw new AssertionError("Error: Lambda forms of the two method handles"
                            + " are not the same. LF cahing does not work");
                } else {
                    System.err.println("LambdaForms differ, but there was a GC in between. Ignore the failure.");
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException |
                SecurityException | InvocationTargetException ex) {
            throw new Error("Unexpected exception", ex);
        }
    }
}
