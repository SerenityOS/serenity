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

package compiler.lib.ir_framework;

import compiler.lib.ir_framework.test.DeclaredTest;
import compiler.lib.ir_framework.test.TestVM;

import java.lang.reflect.Method;

/**
 * Test info class which provides some useful utility methods and information about a <b>checked test</b>.
 *
 * @see Test
 * @see Check
 */
public class TestInfo extends AbstractInfo {
    private final Method testMethod;
    private final boolean compilationSkipped;

    public TestInfo(Method testMethod, CompLevel testCmpLevel) {
        super(testMethod.getDeclaringClass());
        this.testMethod = testMethod;
        this.compilationSkipped = testCmpLevel == CompLevel.SKIP;
    }

    /**
     * Get the associated test method object.
     *
     * @return the associated test method object.
     */
    public Method getTest() {
        return testMethod;
    }

    /**
     * Return a boolean indicating if the framework skipped a compilation after the warm-up due to VM flags not
     * allowing a compilation on the requested level in {@link Test#compLevel()}.
     *
     * @return {@code true} if the framework skipped compilation of the test;
     *         {@code false} otherwise.
     */
    public boolean isCompilationSkipped() {
        return compilationSkipped;
    }

    /**
     * Returns a boolean indicating if the associated test method is C1 compiled.
     *
     * @return {@code true} if the test method is C1 compiled;
     *         {@code false} otherwise.
     */
    public boolean isC1Compiled() {
        return TestVM.isC1Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method is C2 compiled.
     *
     * @return {@code true} if the test method is C2 compiled;
     *         {@code false} otherwise.
     */
    public boolean isC2Compiled() {
        return TestVM.isC2Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method is compiled at {@code compLevel}.
     *
     * @param compLevel the compilation level.
     * @return {@code true} if the test method is compiled at {@code compLevel};
     *         {@code false} otherwise.
     */
    public boolean isCompiledAtLevel(CompLevel compLevel) {
        return TestVM.isCompiledAtLevel(testMethod, compLevel);
    }
}
