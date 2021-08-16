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

import compiler.lib.ir_framework.shared.TestRunException;
import compiler.lib.ir_framework.test.TestVM;
import jdk.test.lib.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Base info class which provides some useful utility methods and information about a test.
 * <p>
 * <b>Base tests</b> and <b>checked tests</b> use {@link TestInfo} while <b>custom run tests</b> use {@link RunInfo}.
 *
 * @see Test
 * @see Check
 * @see Run
 */
abstract public class AbstractInfo {
    private static final Random RANDOM = Utils.getRandomInstance();

    protected final Class<?> testClass;
    private boolean onWarmUp = true;

    AbstractInfo(Class<?> testClass) {
        this.testClass = testClass;
    }

    /**
     * Get the initialized {@link Random} object.
     *
     * @return the random object.
     */
    public Random getRandom() {
        return RANDOM;
    }

    /**
     * Returns a boolean indicating if the framework is currently warming up the associated test.
     *
     * @return the warm-up status of the associated test.
     *
     * @see Warmup
     */
    public boolean isWarmUp() {
        return onWarmUp;
    }

    /**
     * Get the method object of the method {@code name} of class {@code c} with arguments {@code args}.
     *
     * @param c    the class containing the method.
     * @param name the name of the method.
     * @param args the arguments of the method, leave empty if no arguments.
     *
     * @return the method object of the requested method.
     */
    public Method getMethod(Class<?> c, String name, Class<?>... args) {
        try {
            return c.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            String parameters = args == null || args.length == 0 ? "" :
                    " with arguments [" + Arrays.stream(args).map(Class::getName).collect(Collectors.joining(",")) + "]";
            throw new TestRunException("Could not find method " + name + " in " + c + parameters, e);
        }
    }

    /**
     * Get the method object of the method {@code name} of the test class with arguments {@code args}.
     *
     * @param name the name of the method in the test class.
     * @param args the arguments of the method, leave empty if no arguments.
     *
     * @return the method object of the requested method in the test class.
     */
    public Method getTestClassMethod(String name, Class<?>... args) {
        return getMethod(testClass, name, args);
    }

    /**
     * Returns a boolean indicating if the test VM runs with flags that allow C2 compilations.
     *
     * @return {@code true} if C2 compilations are allowed;
     *         {@code false} otherwise (run with {@code -XX:TieredStopAtLevel={1,2,3}, -XX:-UseCompiler}).
     */
    public boolean isC2CompilationEnabled() {
        return TestVM.USE_COMPILER && !TestVM.TEST_C1;
    }

    /**
     * Called by {@link TestFramework} when the warm-up is finished. Should not be called by user code.
     */
    public void setWarmUpFinished() {
        onWarmUp = false;
    }
}
