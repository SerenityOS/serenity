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

import compiler.lib.ir_framework.CompLevel;
import compiler.lib.ir_framework.shared.TestRunException;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This class represents a @Test method.
 */
public class DeclaredTest {
    private final Method testMethod;
    private final ArgumentValue[] arguments;
    private final int warmupIterations;
    private final CompLevel compLevel;
    private Method attachedMethod;

    public DeclaredTest(Method testMethod, ArgumentValue[] arguments, CompLevel compLevel, int warmupIterations) {
        // Make sure we can also call non-public or public methods in package private classes
        testMethod.setAccessible(true);
        this.testMethod = testMethod;
        this.compLevel = compLevel;
        this.arguments = arguments;
        this.warmupIterations = warmupIterations;
        this.attachedMethod = null;
    }

    public Method getTestMethod() {
        return testMethod;
    }

    public CompLevel getCompLevel() {
        return compLevel;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public boolean hasArguments() {
        return arguments != null;
    }

    public Object[] getArguments() {
        return Arrays.stream(arguments).map(ArgumentValue::getArgument).toArray();
    }

    public void setAttachedMethod(Method m) {
        attachedMethod = m;
    }

    public Method getAttachedMethod() {
        return attachedMethod;
    }

    public void printFixedRandomArguments() {
        if (hasArguments()) {
            boolean hasRandomArgs = false;
            StringBuilder builder = new StringBuilder("Fixed random arguments for method ").append(testMethod).append(": ");
            for (int i = 0; i < arguments.length; i++) {
                ArgumentValue argument = arguments[i];
                if (argument.isFixedRandom()) {
                    hasRandomArgs = true;
                    Object argumentVal = argument.getArgument();
                    builder.append("arg ").append(i).append(": ").append(argumentVal.toString());
                    if (argumentVal instanceof Character) {
                        builder.append(" (").append((int)(Character)argumentVal).append(")");
                    }
                    builder.append(", ");
                }
            }
            if (hasRandomArgs) {
                // Drop the last comma and space.
                builder.setLength(builder.length() - 2);
                System.out.println(builder.toString());
            }
        }
    }

    public String getArgumentsString() {
        if (hasArguments()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < arguments.length; i++) {
                builder.append("arg ").append(i).append(": ").append(arguments[i].getArgument()).append(", ");
            }
            builder.setLength(builder.length() - 2);
            return builder.toString();
        } else {
            return "<void>";
        }
    }

    public Object invoke(Object obj, Object... args) {
        try {
            return testMethod.invoke(obj, args);
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod, e);
        }
    }
}

