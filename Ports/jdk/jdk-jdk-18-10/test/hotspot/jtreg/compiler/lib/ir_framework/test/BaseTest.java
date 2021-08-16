/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework.test;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.shared.TestRunException;

import java.lang.reflect.Method;

/**
 * A base test only consists of a single @Test method. See {@link Test} for more details and its precise definition.
 */
class BaseTest extends AbstractTest {
    private final DeclaredTest test;
    protected final Method testMethod;
    protected final TestInfo testInfo;
    protected final Object invocationTarget;
    private final boolean shouldCompile;
    private final boolean waitForCompilation;

    public BaseTest(DeclaredTest test, boolean skip) {
        super(test.getWarmupIterations(), skip);
        this.test = test;
        this.testMethod = test.getTestMethod();
        this.testInfo = new TestInfo(testMethod, test.getCompLevel());
        this.invocationTarget = createInvocationTarget(testMethod);
        this.shouldCompile = shouldCompile(test);
        this.waitForCompilation = isWaitForCompilation(test);
    }

    @Override
    public String toString() {
        return "Base Test: @Test " + testMethod.getName();
    }

    @Override
    public String getName() {
        return testMethod.getName();
    }

    @Override
    protected void onStart() {
        test.printFixedRandomArguments();
    }

    @Override
    public void onWarmupFinished() {
        testInfo.setWarmUpFinished();
    }

    @Override
    protected void invokeTest() {
        verify(invokeTestMethod());
    }

    private Object invokeTestMethod() {
        try {
            if (test.hasArguments()) {
                return testMethod.invoke(invocationTarget, test.getArguments());
            } else {
                return testMethod.invoke(invocationTarget);
            }
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod
                                       + ". Used arguments: " + test.getArgumentsString(), e);
        }
    }

    @Override
    protected void compileTest() {
        if (shouldCompile) {
            if (waitForCompilation) {
                waitForCompilation(test);
            } else {
                compileMethod(test);
            }
        }
    }

    /**
     * Verify the result
     */
    public void verify(Object result) { /* no verification in BaseTests */ }
}
