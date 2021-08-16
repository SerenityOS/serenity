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

import compiler.lib.ir_framework.Check;
import compiler.lib.ir_framework.CheckAt;
import compiler.lib.ir_framework.shared.TestRunException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A checked test is an extension of a base test with additional verification done in a @Check method.
 * See {@link Check} for more details and its precise definition.
 */
class CheckedTest extends BaseTest {
    private final Method checkMethod;
    private final CheckAt checkAt;
    private final Parameter parameter;
    private final Object checkInvocationTarget;

    enum Parameter {
        NONE, RETURN_ONLY, TEST_INFO_ONLY, BOTH
    }

    public CheckedTest(DeclaredTest test, Method checkMethod, Check checkSpecification, Parameter parameter, boolean excludedByUser) {
        super(test, excludedByUser);
        // Make sure we can also call non-public or public methods in package private classes
        checkMethod.setAccessible(true);
        this.checkMethod = checkMethod;
        this.checkAt = checkSpecification.when();
        this.parameter = parameter;
        // Use the same invocation target
        if (Modifier.isStatic(checkMethod.getModifiers())) {
            this.checkInvocationTarget = null;
        } else {
            // Use the same invocation target as the test method if check method is non-static.
            this.checkInvocationTarget = this.invocationTarget != null ? this.invocationTarget : createInvocationTarget(checkMethod);
        }
    }

    @Override
    public String toString() {
        return "Checked Test: @Check " + checkMethod.getName() + " - @Test: " + testMethod.getName();
    }

    @Override
    public String getName() {
        return checkMethod.getName();
    }

    @Override
    public void verify(Object result) {
        boolean shouldVerify = false;
        switch (checkAt) {
            case EACH_INVOCATION -> shouldVerify = true;
            case COMPILED -> shouldVerify = !testInfo.isWarmUp();
        }
        if (shouldVerify) {
            try {
                switch (parameter) {
                    case NONE -> checkMethod.invoke(checkInvocationTarget);
                    case RETURN_ONLY -> checkMethod.invoke(checkInvocationTarget, result);
                    case TEST_INFO_ONLY -> checkMethod.invoke(checkInvocationTarget, testInfo);
                    case BOTH -> checkMethod.invoke(checkInvocationTarget, result, testInfo);
                }
            } catch (Exception e) {
                throw new TestRunException("There was an error while invoking @Check method " + checkMethod, e);
            }
        }
    }
}
