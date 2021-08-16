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

import compiler.lib.ir_framework.Argument;
import compiler.lib.ir_framework.Arguments;
import compiler.lib.ir_framework.shared.TestFrameworkException;
import compiler.lib.ir_framework.shared.TestFormat;
import compiler.lib.ir_framework.shared.TestFormatException;
import jdk.test.lib.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Random;

/**
 * This class represents an argument value specified by {@link Argument} in {@link Arguments}.
 */
class ArgumentValue {
    private static final Random RANDOM = Utils.getRandomInstance();

    private final Object argumentValue;
    private final boolean isRandomEach;
    private final boolean isFixedRandom;
    private final Class<?> randomClass;

    ArgumentValue() {
        this.argumentValue = null;
        this.isRandomEach = false;
        this.randomClass = null;
        this.isFixedRandom = false;
    }

    private ArgumentValue(Object argumentValue, boolean isFixedRandom) {
        this.argumentValue = argumentValue;
        this.isRandomEach = false;
        this.randomClass = null;
        this.isFixedRandom = isFixedRandom;
    }

    private ArgumentValue(Object argumentValue, Class<?> randomClass) {
        this.argumentValue = argumentValue;
        this.isRandomEach = true;
        this.randomClass = randomClass;
        this.isFixedRandom = false;
    }

    /**
     * Return all arguments for the @Arguments annotation.
     *
     * @param m The @Test method.
     * @return Returns an array with Argument objects for each specified argument in the @Arguments annotation of m.
     *         Returns null if method has no @Arguments annotation.
     */
    public static ArgumentValue[] getArguments(Method m) {
        Arguments argumentsAnno = m.getAnnotation(Arguments.class);
        if (argumentsAnno == null) {
            return null;
        }
        Argument[] values = argumentsAnno.value();
        ArgumentValue[] arguments = new ArgumentValue[values.length];
        Class<?>[] declaredParameters = m.getParameterTypes();
        Parameter[] declaredParameterObjects = m.getParameters();
        try {
            TestFormat.check(values.length == declaredParameters.length,
                             "Number of argument values provided in @Arguments does not match the number " +
                             "of actual arguments in " + m);

            for (int i = 0; i < values.length; i++) {
                Argument specifiedArg = values[i];
                Class<?> parameter = declaredParameters[i];
                Parameter parameterObj = declaredParameterObjects[i];
                try {
                    switch (specifiedArg) {
                        case DEFAULT -> {
                            try {
                                arguments[i] = createDefault(parameter);
                            } catch (NoSuchMethodException e) {
                                TestFormat.fail("Cannot create new default instance of " + parameter
                                                + " for " + m + " due to missing default constructor");
                            } catch (Exception e) {
                                TestFormat.fail("Cannot create new default instance of " + parameter
                                                + " for " + m + ": " + e.getCause());
                            }
                        }
                        case NUMBER_42 -> {
                            TestFormat.check(isNumber(parameter), "Provided invalid NUMBER_42 argument " +
                                                                  "for non-number " + parameterObj + " for " + m);
                            arguments[i] = create((byte) 42);
                        }
                        case NUMBER_MINUS_42 -> {
                            TestFormat.check(isNumber(parameter), "Provided invalid NUMBER_MINUS_42 argument " +
                                                                  "for non-number " + parameterObj + " for " + m);
                            arguments[i] = create((byte) -42);
                        }
                        case MIN -> {
                            TestFormat.check(isNumber(parameter) || isChar(parameter),
                                             "Provided invalid MIN argument for non-number "
                                             + parameterObj + " for " + m);
                            arguments[i] = createMin(parameter);
                        }
                        case MAX -> {
                            TestFormat.check(isNumber(parameter) || isChar(parameter),
                                             "Provided invalid MAX argument for non-number "
                                             + parameterObj + " for " + m);
                            arguments[i] = createMax(parameter);
                        }
                        case FALSE -> {
                            TestFormat.check(ArgumentValue.isBoolean(parameter),
                                             "Provided invalid FALSE argument for non-boolean "
                                             + parameterObj + " for " + m);
                            arguments[i] = create(false);
                        }
                        case TRUE -> {
                            TestFormat.check(ArgumentValue.isBoolean(parameter),
                                             "Provided invalid TRUE argument for non-boolean "
                                             + parameterObj + " for " + m);
                            arguments[i] = create(true);
                        }
                        case BOOLEAN_TOGGLE_FIRST_FALSE -> {
                            TestFormat.check(isBoolean(parameter),
                                             "Provided invalid BOOLEAN_TOGGLE_FIRST_FALSE argument for non-boolean "
                                             + parameterObj + " for " + m);
                            arguments[i] = BooleanToggleValue.create(false);
                        }
                        case BOOLEAN_TOGGLE_FIRST_TRUE -> {
                            TestFormat.check(ArgumentValue.isBoolean(parameter),
                                             "Provided invalid BOOLEAN_TOGGLE_FIRST_TRUE argument for non-boolean "
                                             + parameterObj + " for " + m);
                            arguments[i] = BooleanToggleValue.create(true);
                        }
                        case RANDOM_ONCE -> {
                            TestFormat.check(isPrimitiveType(parameter),
                                             "Provided invalid RANDOM_ONCE argument for non-primitive type "
                                             + parameterObj + " for " + m);
                            arguments[i] = createRandom(parameter);
                        }
                        case RANDOM_EACH -> {
                            TestFormat.check(isPrimitiveType(parameter),
                                             "Provided invalid RANDOM_EACH argument for non-primitive type "
                                             + parameterObj + " for " + m);
                            arguments[i] = createRandomEach(parameter);
                        }
                    }
                } catch (TestFormatException e) {
                    // Catch and continue to check arguments.
                }
            }
        } catch (TestFormatException e) {
            // Catch and return empty array to check for additional failures.
            return new ArgumentValue[0];
        }
        return arguments;
    }

    private static ArgumentValue create(Object argumentValue) {
        return new ArgumentValue(argumentValue, false);
    }

    private static ArgumentValue createDefault(Class<?> c) throws Exception {
        if (ArgumentValue.isNumber(c)) {
            return ArgumentValue.create((byte)0);
        } else if (ArgumentValue.isChar(c)) {
            return ArgumentValue.create('\u0000');
        } else if (ArgumentValue.isBoolean(c)) {
            return ArgumentValue.create(false);
        } else {
            // Object
            Constructor<?> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true); // Make sure to have access to private default constructor
            return ArgumentValue.create(constructor.newInstance());
        }
    }

    private static ArgumentValue createMin(Class<?> c) {
        Object argument;
        if (c.equals(byte.class)) {
            argument = Byte.MIN_VALUE;
        } else if (isChar(c)) {
            argument = Character.MIN_VALUE;
        }  else if (c.equals(short.class)) {
            argument = Short.MIN_VALUE;
        } else if (c.equals(int.class)) {
            argument = Integer.MIN_VALUE;
        } else if (c.equals(long.class)) {
            argument = Long.MIN_VALUE;
        } else if (c.equals(float.class)) {
            argument = Float.MIN_VALUE;
        } else if (c.equals(double.class)) {
            argument = Double.MIN_VALUE;
        } else {
            throw new TestFrameworkException("Invalid class passed to createMin()");
        }
        return new ArgumentValue(argument, false);
    }

    private static ArgumentValue createMax(Class<?> c) {
        Object argument;
        if (c.equals(byte.class)) {
            argument = Byte.MAX_VALUE;
        } else if (isChar(c)) {
            argument = Character.MAX_VALUE;
        }  else if (c.equals(short.class)) {
            argument = Short.MAX_VALUE;
        } else if (c.equals(int.class)) {
            argument = Integer.MAX_VALUE;
        } else if (c.equals(long.class)) {
            argument = Long.MAX_VALUE;
        } else if (c.equals(float.class)) {
            argument = Float.MAX_VALUE;
        } else if (c.equals(double.class)) {
            argument = Double.MAX_VALUE;
        } else {
            throw new TestFrameworkException("Invalid class passed to createMax()");
        }
        return new ArgumentValue(argument, false);
    }

    private static ArgumentValue createRandom(Class<?> c) {
        return new ArgumentValue(getRandom(c), true);
    }

    private static ArgumentValue createRandomEach(Class<?> c) {
        return new ArgumentValue(null, c);
    }

    public boolean isFixedRandom() {
        return isFixedRandom;
    }

    public Object getArgument() {
        if (isRandomEach) {
            return getRandom(randomClass);
        } else {
            return argumentValue;
        }
    }

    private static boolean isPrimitiveType(Class<?> c) {
        return isNumber(c) || isBoolean(c) || isChar(c);
    }

    private static boolean isBoolean(Class<?> c) {
        return c.equals(boolean.class);
    }

    private static boolean isChar(Class<?> c) {
        return c.equals(char.class);
    }

    private static boolean isNumber(Class<?> c) {
        return isIntNumber(c) || isFloatNumber(c);
    }

    private static boolean isIntNumber(Class<?> c) {
        return c.equals(byte.class)
                || c.equals(short.class)
                || c.equals(int.class)
                || c.equals(long.class);
    }

    private static boolean isFloatNumber(Class<?> c) {
        return c.equals(float.class) || c.equals(double.class);
    }

    private static Object getRandom(Class<?> c) {
        if (isBoolean(c)) {
            return RANDOM.nextBoolean();
        } else if (c.equals(byte.class)) {
            return (byte) RANDOM.nextInt(256);
        } else if (isChar(c)) {
            return (char) RANDOM.nextInt(65536);
        }  else if (c.equals(short.class)) {
            return (short) RANDOM.nextInt(65536);
        } else if (c.equals(int.class)) {
            return RANDOM.nextInt();
        } else if (c.equals(long.class)) {
            return RANDOM.nextLong();
        } else if (c.equals(float.class)) {
            // Get float between -10000 and 10000.
            return RANDOM.nextFloat() * 20000 - 10000;
        } else if (c.equals(double.class)) {
            // Get double between -10000 and 10000.
            return RANDOM.nextDouble() * 20000 - 10000;
        } else {
            TestFormat.fail("Cannot generate random value for non-primitive type");
            return null;
        }
    }
}

/**
 * Special class to handle boolean toggle argument values.
 */
class BooleanToggleValue extends ArgumentValue {
    private boolean previousBoolean;

    BooleanToggleValue(boolean firstBoolean) {
        this.previousBoolean = !firstBoolean;
    }

    @Override
    public Object getArgument() {
        previousBoolean = !previousBoolean;
        return previousBoolean;
    }

    static BooleanToggleValue create(boolean firstBoolean) {
        return new BooleanToggleValue(firstBoolean);
    }
}
