/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.java.lang.invoke.lib;

import jdk.test.lib.Asserts;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Helper {
    /** Flag for verbose output, true if {@code -Dverbose} specified */
    public static final boolean IS_VERBOSE
            = System.getProperty("verbose") != null;
    /**
     * Flag for thorough testing -- all test will be executed,
     * true if {@code -Dthorough} specified. */
    public static final boolean IS_THOROUGH
            = System.getProperty("thorough") != null;
    /** Random number generator w/ initial seed equal to {@code -Dseed} */
    public static final Random RNG;

    static {
        String str = System.getProperty("seed");
        long seed = str != null ? Long.parseLong(str) : new Random().nextLong();
        RNG = new Random(seed);
        System.out.printf("-Dseed=%d%n", seed);
    }

    public static final long TEST_LIMIT;
    static {
        String str = System.getProperty("testLimit");
        TEST_LIMIT = str != null ? Long.parseUnsignedLong(str) : 2000L;
        System.out.printf("-DtestLimit=%d%n", TEST_LIMIT);
    }

    public static final int MAX_ARITY = 254;
    public static final String MISSING_ARG = "missingArg";
    public static final String MISSING_ARG_2 = "missingArg#2";

    private static final int
            // first int value
            ONE_MILLION = (1000 * 1000),
            // scale factor to reach upper 32 bits
            TEN_BILLION = (10 * 1000 * 1000 * 1000),
            // <<1 makes space for sign bit;
            INITIAL_ARG_VAL = ONE_MILLION << 1;

    public static final MethodHandle AS_LIST;

    static {
        try {
            AS_LIST = MethodHandles.lookup().findStatic(
                    Arrays.class, "asList",
                    MethodType.methodType(List.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    public static boolean isDoubleCost(Class<?> aClass) {
        return aClass == double.class || aClass == long.class;
    }

    private static List<List<Object>> calledLog = new ArrayList<>();
    private static long nextArgVal;

    public static void assertCalled(String name, Object... args) {
        assertCalled(0, name, args);
    }

    public static void assertCalled(int lag, String name, Object... args) {
        Object expected = logEntry(name, args);
        Object actual = getCalled(lag);
        Asserts.assertEQ(expected, actual, "method call w/ lag = " + lag);
    }

    public static Object called(String name, Object... args) {
        List<Object> entry = logEntry(name, args);
        calledLog.add(entry);
        return entry;
    }

    private static List<Object> logEntry(String name, Object... args) {
        return Arrays.asList(name, Arrays.asList(args));
    }

    public static void clear() {
        calledLog.clear();
    }

    public static List<Object> getCalled(int lag) {
        int size = calledLog.size();
        return size <= lag ? null : calledLog.get(size - lag - 1);
    }

    public static List<Class<?>> randomClasses(Class<?>[] classes, int size) {
        List<Class<?>> result = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            result.add(classes[RNG.nextInt(classes.length)]);
        }
        return result;
    }

    public static List<Class<?>> getParams(List<Class<?>> classes,
            boolean isVararg, int argsCount) {
        boolean unmodifiable = true;
        List<Class<?>> result = classes.subList(0,
                Math.min(argsCount, (MAX_ARITY / 2) - 1));
        int extra = 0;
        if (argsCount >= MAX_ARITY / 2) {
            result = new ArrayList<>(result);
            unmodifiable = false;
            extra = (int) result.stream().filter(Helper::isDoubleCost).count();
            int i = result.size();
            while (result.size() + extra < argsCount) {
                Class<?> aClass = classes.get(i);
                if (Helper.isDoubleCost(aClass)) {
                    ++extra;
                    if (result.size() + extra >= argsCount) {
                        break;
                    }
                }
                result.add(aClass);
            }
        }
        if (isVararg && result.size() > 0) {
            if (unmodifiable) {
                result = new ArrayList<>(result);
            }
            int last = result.size() - 1;
            Class<?> aClass = result.get(last);
            aClass = Array.newInstance(aClass, 2).getClass();
            result.set(last, aClass);
        }
        return result;
    }

    public static MethodHandle addTrailingArgs(MethodHandle target, int nargs,
            List<Class<?>> classes) {
        int targetLen = target.type().parameterCount();
        int extra = (nargs - targetLen);
        if (extra <= 0) {
            return target;
        }
        List<Class<?>> fakeArgs = new ArrayList<>(extra);
        for (int i = 0; i < extra; ++i) {
            fakeArgs.add(classes.get(i % classes.size()));
        }
        return MethodHandles.dropArguments(target, targetLen, fakeArgs);
    }

    public static MethodHandle varargsList(int arity) {
        return AS_LIST.asCollector(Object[].class, arity);
    }

    private static long nextArg(boolean moreBits) {
        long val = nextArgVal++;
        long sign = -(val & 1); // alternate signs
        val >>= 1;
        if (moreBits)
        // Guarantee some bits in the high word.
        // In any case keep the decimal representation simple-looking,
        // with lots of zeroes, so as not to make the printed decimal
        // strings unnecessarily noisy.
        {
            val += (val % ONE_MILLION) * TEN_BILLION;
        }
        return val ^ sign;
    }

    private static int nextArg() {
        // Produce a 32-bit result something like ONE_MILLION+(smallint).
        // Example: 1_000_042.
        return (int) nextArg(false);
    }

    private static long nextArg(Class<?> kind) {
        if (kind == long.class || kind == Long.class ||
                kind == double.class || kind == Double.class)
        // produce a 64-bit result something like
        // ((TEN_BILLION+1) * (ONE_MILLION+(smallint)))
        // Example: 10_000_420_001_000_042.
        {
            return nextArg(true);
        }
        return (long) nextArg();
    }

    private static Object randomArg(Class<?> param) {
        Object wrap = castToWrapperOrNull(nextArg(param), param);
        if (wrap != null) {
            return wrap;
        }

        if (param.isInterface()) {
            for (Class<?> c : param.getClasses()) {
                if (param.isAssignableFrom(c) && !c.isInterface()) {
                    param = c;
                    break;
                }
            }
        }
        if (param.isArray()) {
            Class<?> ctype = param.getComponentType();
            Object arg = Array.newInstance(ctype, 2);
            Array.set(arg, 0, randomArg(ctype));
            return arg;
        }
        if (param.isInterface() && param.isAssignableFrom(List.class)) {
            return Arrays.asList("#" + nextArg());
        }
        if (param.isInterface() || param.isAssignableFrom(String.class)) {
            return "#" + nextArg();
        }

        try {
            return param.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
        }
        return null;  // random class not Object, String, Integer, etc.
    }

    public static Object[] randomArgs(Class<?>... params) {
        Object[] args = new Object[params.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = randomArg(params[i]);
        }
        return args;
    }

    public static Object[] randomArgs(int nargs, Class<?> param) {
        Object[] args = new Object[nargs];
        for (int i = 0; i < args.length; i++) {
            args[i] = randomArg(param);
        }
        return args;
    }

    public static Object[] randomArgs(int nargs, Class<?>... params) {
        Object[] args = new Object[nargs];
        for (int i = 0; i < args.length; i++) {
            Class<?> param = params[i % params.length];
            args[i] = randomArg(param);
        }
        return args;
    }

    public static Object[] randomArgs(List<Class<?>> params) {
        return randomArgs(params.toArray(new Class<?>[params.size()]));
    }

    public static Object castToWrapper(Object value, Class<?> dst) {
        Object wrap = null;
        if (value instanceof Number) {
            wrap = castToWrapperOrNull(((Number) value).longValue(), dst);
        }
        if (value instanceof Character) {
            wrap = castToWrapperOrNull((char) (Character) value, dst);
        }
        if (wrap != null) {
            return wrap;
        }
        return dst.cast(value);
    }

    @SuppressWarnings("cast")
    // primitive cast to (long) is part of the pattern
    private static Object castToWrapperOrNull(long value, Class<?> dst) {
        if (dst == int.class || dst == Integer.class) {
            return (int) (value);
        }
        if (dst == long.class || dst == Long.class) {
            return (long) (value);
        }
        if (dst == char.class || dst == Character.class) {
            return (char) (value);
        }
        if (dst == short.class || dst == Short.class) {
            return (short) (value);
        }
        if (dst == float.class || dst == Float.class) {
            return (float) (value);
        }
        if (dst == double.class || dst == Double.class) {
            return (double) (value);
        }
        if (dst == byte.class || dst == Byte.class) {
            return (byte) (value);
        }
        if (dst == boolean.class || dst == Boolean.class) {
            return ((value % 29) & 1) == 0;
        }
        return null;
    }

    /**
     * Routine used to obtain a randomly generated method type.
     *
     * @param arity Arity of returned method type.
     * @return MethodType generated randomly.
     */
    public static MethodType randomMethodTypeGenerator(int arity) {
        final Class<?>[] CLASSES = {
            Object.class,
            int.class,
            boolean.class,
            byte.class,
            short.class,
            char.class,
            long.class,
            float.class,
            double.class
        };
        if (arity > MAX_ARITY) {
            throw new IllegalArgumentException(
                    String.format("Arity should not exceed %d!", MAX_ARITY));
        }
        List<Class<?>> list = randomClasses(CLASSES, arity);
        list = getParams(list, false, arity);
        int i = RNG.nextInt(CLASSES.length + 1);
        Class<?> rtype = i == CLASSES.length ? void.class : CLASSES[i];
        return MethodType.methodType(rtype, list);
    }
}
