/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.whitebox;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public enum SimpleTestCase implements CompilerWhiteBoxTest.TestCase {
    /** constructor test case */
    CONSTRUCTOR_TEST(SimpleTestCaseHelper.CONSTRUCTOR, SimpleTestCaseHelper.CONSTRUCTOR_CALLABLE, false),
    /** method test case */
    METHOD_TEST(SimpleTestCaseHelper.METHOD, SimpleTestCaseHelper.METHOD_CALLABLE, false),
    /** static method test case */
    STATIC_TEST(SimpleTestCaseHelper.STATIC, SimpleTestCaseHelper.STATIC_CALLABLE, false),
    /** OSR constructor test case */
    OSR_CONSTRUCTOR_TEST(SimpleTestCaseHelper.OSR_CONSTRUCTOR, SimpleTestCaseHelper.OSR_CONSTRUCTOR_CALLABLE, true),
    /** OSR method test case */
    OSR_METHOD_TEST(SimpleTestCaseHelper.OSR_METHOD, SimpleTestCaseHelper.OSR_METHOD_CALLABLE, true),
    /** OSR static method test case */
    OSR_STATIC_TEST(SimpleTestCaseHelper.OSR_STATIC, SimpleTestCaseHelper.OSR_STATIC_CALLABLE, true);

    private final Executable executable;
    private final Callable<Integer> callable;
    private final boolean isOsr;

    private SimpleTestCase(Executable executable, Callable<Integer> callable,
            boolean isOsr) {
        this.executable = executable;
        this.callable = callable;
        this.isOsr = isOsr;
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public Callable<Integer> getCallable() {
        return callable;
    }

    @Override
    public boolean isOsr() {
        return isOsr;
    }
}

    class SimpleTestCaseHelper {

        public static final Callable<Integer> CONSTRUCTOR_CALLABLE
                = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return new SimpleTestCaseHelper(1337).hashCode();
            }
        };

        public static final Callable<Integer> METHOD_CALLABLE
                = new Callable<Integer>() {
            private final SimpleTestCaseHelper helper = new SimpleTestCaseHelper();

            @Override
            public Integer call() throws Exception {
                return helper.method();
            }
        };

        public static final Callable<Integer> STATIC_CALLABLE
                = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return staticMethod();
            }
        };

        public static final Callable<Integer> OSR_CONSTRUCTOR_CALLABLE
                = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return new SimpleTestCaseHelper(null, CompilerWhiteBoxTest.BACKEDGE_THRESHOLD).hashCode();
            }
        };

        public static final Callable<Integer> OSR_METHOD_CALLABLE
                = new Callable<Integer>() {
            private final SimpleTestCaseHelper helper = new SimpleTestCaseHelper();

            @Override
            public Integer call() throws Exception {
                return helper.osrMethod(CompilerWhiteBoxTest.BACKEDGE_THRESHOLD);
            }
        };

        public static final Callable<Integer> OSR_STATIC_CALLABLE
                = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return osrStaticMethod(CompilerWhiteBoxTest.BACKEDGE_THRESHOLD);
            }
        };

        public static final Constructor CONSTRUCTOR;
        public static final Constructor OSR_CONSTRUCTOR;
        public static final Method METHOD;
        public static final Method STATIC;
        public static final Method OSR_METHOD;
        public static final Method OSR_STATIC;

        static {
            try {
                CONSTRUCTOR = SimpleTestCaseHelper.class.getDeclaredConstructor(int.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(
                        "exception on getting method Helper.<init>(int)", e);
            }
            try {
                OSR_CONSTRUCTOR = SimpleTestCaseHelper.class.getDeclaredConstructor(
                        Object.class, long.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(
                        "exception on getting method Helper.<init>(Object, long)", e);
            }
            METHOD = getMethod("method");
            STATIC = getMethod("staticMethod");
            OSR_METHOD = getMethod("osrMethod", long.class);
            OSR_STATIC = getMethod("osrStaticMethod", long.class);
        }

        private static Method getMethod(String name, Class<?>... parameterTypes) {
            try {
                return SimpleTestCaseHelper.class.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(
                        "exception on getting method Helper." + name, e);
            }
        }

        private static int staticMethod() {
            return 1138;
        }

        private int method() {
            return 42;
        }

        /**
         * Deoptimizes all non-osr versions of the given executable after
         * compilation finished.
         *
         * @param e Executable
         * @throws Exception
         */
        private static void waitAndDeoptimize(Executable e) {
            CompilerWhiteBoxTest.waitBackgroundCompilation(e);
            if (WhiteBox.getWhiteBox().isMethodQueuedForCompilation(e)) {
                throw new RuntimeException(e + " must not be in queue");
            }
            // Deoptimize non-osr versions of executable
            WhiteBox.getWhiteBox().deoptimizeMethod(e, false);
        }

        /**
         * Executes the method multiple times to make sure we have
         * enough profiling information before triggering an OSR
         * compilation. Otherwise the C2 compiler may add uncommon traps.
         *
         * @param m Method to be executed
         * @return Number of times the method was executed
         * @throws Exception
         */
        private static int warmup(Method m) throws Exception {
            waitAndDeoptimize(m);
            SimpleTestCaseHelper helper = new SimpleTestCaseHelper();
            int result = 0;
            for (long i = 0; i < CompilerWhiteBoxTest.THRESHOLD; ++i) {
                result += (int)m.invoke(helper, 1);
            }
            // Wait to make sure OSR compilation is not blocked by
            // non-OSR compilation in the compile queue
            CompilerWhiteBoxTest.waitBackgroundCompilation(m);
            return result;
        }

        /**
         * Executes the constructor multiple times to make sure we
         * have enough profiling information before triggering an OSR
         * compilation. Otherwise the C2 compiler may add uncommon traps.
         *
         * @param c Constructor to be executed
         * @return Number of times the constructor was executed
         * @throws Exception
         */
        private static int warmup(Constructor c) throws Exception {
            waitAndDeoptimize(c);
            int result = 0;
            for (long i = 0; i < CompilerWhiteBoxTest.THRESHOLD; ++i) {
                result += c.newInstance(null, 1).hashCode();
            }
            // Wait to make sure OSR compilation is not blocked by
            // non-OSR compilation in the compile queue
            CompilerWhiteBoxTest.waitBackgroundCompilation(c);
            return result;
        }

        private static int osrStaticMethod(long limit) throws Exception {
            int result = 0;
            if (limit != 1) {
                result = warmup(OSR_STATIC);
            }
            // Trigger osr compilation
            for (long i = 0; i < limit; ++i) {
                result += staticMethod();
            }
            return result;
        }

        private int osrMethod(long limit) throws Exception {
            int result = 0;
            if (limit != 1) {
                result = warmup(OSR_METHOD);
            }
            // Trigger osr compilation
            for (long i = 0; i < limit; ++i) {
                result += method();
            }
            return result;
        }

        private final int x;

        // for method and OSR method test case
        public SimpleTestCaseHelper() {
            x = 0;
        }

        // for OSR constructor test case
        private SimpleTestCaseHelper(Object o, long limit) throws Exception {
            int result = 0;
            if (limit != 1) {
                result = warmup(OSR_CONSTRUCTOR);
            }
            // Trigger osr compilation
            for (long i = 0; i < limit; ++i) {
                result += method();
            }
            x = result;
        }

        // for constructor test case
        private SimpleTestCaseHelper(int x) {
            this.x = x;
        }

        @Override
        public int hashCode() {
            return x;
        }
    }

