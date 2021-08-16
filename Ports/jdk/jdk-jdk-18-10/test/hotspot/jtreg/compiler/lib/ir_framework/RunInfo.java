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
import compiler.lib.ir_framework.shared.TestRunException;
import compiler.lib.ir_framework.test.TestVM;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test info class which provides some useful utility methods and information about a <b>custom run test</b>.
 *
 * @see Run
 */
public class RunInfo extends AbstractInfo {
    private final Method testMethod;
    private final DeclaredTest test;
    private final Map<String, DeclaredTest> tests;
    private final boolean hasMultipleTests;

    public RunInfo(List<DeclaredTest> tests) {
        super(tests.get(0).getTestMethod().getDeclaringClass());
        this.test = tests.get(0);
        this.testMethod = test.getTestMethod();
        this.hasMultipleTests = tests.size() != 1;
        if (hasMultipleTests) {
            this.tests = new HashMap<>();
            for (DeclaredTest test : tests) {
                this.tests.put(test.getTestMethod().getName(), test);
            }
        } else {
            this.tests = null;
        }
    }

    /**
     * Get the associated test method object of this custom run test. This method can only be called if <i>one</i> test
     * method is specified in the custom run test ({@link Run#test()}). Otherwise, use {@link #getTest(String)}.
     *
     * @return the associated test method object.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public Method getTest() {
        checkSingleTest("getTest");
        return testMethod;
    }

    /**
     * Get the associated method object of the test method with the name {@code testName}. This method can only be called
     * if the custom run test specifies more than one test method in ({@link Run#test()}). Otherwise, use {@link #getTest()}.
     *
     * @param testName the test method for which the method object should be returned.
     * @return the associated test method object with the name {@code testName}.
     * @throws TestRunException if there is no test method with the name {@code testName} or if called with only
     *         <i>one</i> associated test method.
     */
    public Method getTest(String testName) {
        checkMultipleTests("getTest");
        return getMethod(testName);
    }

    /**
     * Return a boolean indicating if the framework skipped a compilation of the associated test method after the warm-up
     * due to VM flags not allowing a compilation on the requested level in {@link Test#compLevel()}. This method can only
     * be called if <i>one</i> test method is specified in the custom run test ({@link Run#test()}). Otherwise, use
     * {@link #isCompilationSkipped(String)}.
     *
     * @return {@code true} if the framework skipped compilation of the test;
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isCompilationSkipped() {
        checkSingleTest("isCompilationSkipped");
        return test.getCompLevel() == CompLevel.SKIP;
    }

    /**
     * Return a boolean indicating if the framework skipped a compilation of the associated test method with the name
     * {@code testName} after the warm-up due to VM flags not allowing a compilation on the requested level in
     * {@link Test#compLevel()}. This method can only be called if the custom run test specifies more than one test method
     * in ({@link Run#test()}). Otherwise, use {@link #isCompilationSkipped()}.
     *
     * @param testName the test method for which the method object should be returned.
     * @return {@code true} if the framework skipped compilation of the test;
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName} or if called with only
     *         <i>one</i> associated test method.
     */
    public boolean isCompilationSkipped(String testName) {
        checkMultipleTests("isCompilationSkipped");
        return getDeclaredTest(testName).getCompLevel() == CompLevel.SKIP;
    }

    /**
     * Returns a boolean indicating if the associated test method is C1 compiled. This method can only be called if
     * <i>one</i> test method is specified in the custom run test ({@link Run#test()}). Otherwise, use
     * {@link #isTestC1Compiled(String)}.
     *
     * @return {@code true} if the associated test method is C1 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestC1Compiled() {
        checkSingleTest("isTestC1Compiled");
        return TestVM.isC1Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is C1 compiled.
     * This method can only be called if the custom run test specifies more than one test method in ({@link Run#test()}).
     * Otherwise, use {@link #isTestC1Compiled()}.
     *
     * @param testName the name of the test method.
     * @return {@code true} if the test method with the name {@code testName} is C1 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName} or if called with only
     *         <i>one</i> associated test method.
     */
    public boolean isTestC1Compiled(String testName) {
        checkMultipleTests("isTestC1Compiled");
        return TestVM.isC1Compiled(getMethod(testName));
    }

    /**
     * Returns a boolean indicating if the associated test method is C2 compiled. This method can only be called if
     * <i>one</i> test method is specified in the custom run test ({@link Run#test()}). Otherwise, use
     * {@link #isTestC2Compiled(String)}.
     *
     * @return {@code true} if the associated test method is C2 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestC2Compiled() {
        checkSingleTest("isTestC2Compiled");
        return TestVM.isC2Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is C2 compiled.
     * This method can only be called if the custom run test specifies more than one test method in ({@link Run#test()}).
     * Otherwise, use {@link #isTestC2Compiled()}.
     *
     * @param testName the name of the test method.
     * @return {@code true} if the test method with the name {@code testName} is C2 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName} or if called with only
     *         <i>one</i> associated test method.
     */
    public boolean isTestC2Compiled(String testName) {
        checkMultipleTests("isTestC2Compiled");
        return TestVM.isC2Compiled(getMethod(testName));
    }

    /**
     * Returns a boolean indicating if the associated test method is compiled at {@code compLevel}. This method can only
     * be called if <i>one</i> test method is specified in the custom run test ({@link Run#test()}).
     * Otherwise, use {@link #isTestCompiledAtLevel(String, CompLevel)}.
     *
     * @param compLevel the compilation level
     * @return {@code true} if the associated test method is compiled at {@code compLevel};
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestCompiledAtLevel(CompLevel compLevel) {
        checkSingleTest("isTestCompiledAtLevel");
        return TestVM.isCompiledAtLevel(testMethod, compLevel);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is compiled at
     * {@code compLevel}. This method can only be called if the custom run test specifies more than one test method
     * in ({@link Run#test()}). Otherwise, use {@link #isTestCompiledAtLevel(CompLevel)}.
     *
     * @param testName the name of the test method.
     * @param compLevel the compilation level.
     * @return {@code true} if the test method with the name {@code testName} is compiled at {@code compLevel};
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName} oor if called with only
     *         <i>one</i> associated test method.
     */
    public boolean isTestCompiledAtLevel(String testName, CompLevel compLevel) {
        checkMultipleTests("isTestCompiledAtLevel");
        return TestVM.isCompiledAtLevel(getMethod(testName), compLevel);
    }

    private void checkSingleTest(String calledMethod) {
        if (hasMultipleTests) {
            throw new TestRunException("Use " + calledMethod + "(String) with testName String argument in @Run method " +
                                       "for custom run test that specifies more than one @Test method.");
        }
    }

    private void checkMultipleTests(String calledMethod) {
        if (!hasMultipleTests) {
            throw new TestRunException("Use " + calledMethod + "() without testName String argument in @Run method " +
                                       "for custom run test that specifies exactly one @Test method.");
        }
    }

    private DeclaredTest getDeclaredTest(String testName) {
        DeclaredTest test = tests.get(testName);
        if (test == null) {
            throw new TestRunException("Could not find @Test \"" + testName + "\" in " + testClass + " being associated with" +
                                       " corresponding @Run method.");
        }
        return test;
    }

    private Method getMethod(String testName) {
        return getDeclaredTest(testName).getTestMethod();
    }
}
