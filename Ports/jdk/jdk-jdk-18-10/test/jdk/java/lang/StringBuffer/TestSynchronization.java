/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6206780
 * @summary Test that all public unsynchronized methods of StringBuffer are either directly or indirectly synchronized
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TestSynchronization tests whether synchronized methods calls on an object
 * result in synchronized calls. Note that this may not test all cases desired.
 * It only tests whether some synchronization has occurred on the object during
 * the call chain, and can't tell whether the object was locked across all
 * operations that have been performed on the object.
 */
public class TestSynchronization {

    /**
     * Define parameters used in methods of StringBuffer - admittedly a bit of
     * hack but 'purpose-built' for StringBuffer. Something more general could
     * probably be developed if the test needs to be more widely adopted.
     * <p/>
     * boolean char char[] int double float long Object CharSequence String
     * StringBuffer StringBuilder
     * <p/>
     */
    private static final boolean BOOLEAN_VAL = true;
    private static final char CHAR_VAL = 'x';
    private static final char[] CHAR_ARRAY_VAL = {'c', 'h', 'a', 'r', 'a', 'r',
        'r', 'a', 'y'};
    private static final int INT_VAL = 1;
    private static final double DOUBLE_VAL = 1.0d;
    private static final float FLOAT_VAL = 1.0f;
    private static final long LONG_VAL = 1L;
    private static final Object OBJECT_VAL = new Object();
    private static final String STRING_VAL = "String value";
    private static final StringBuilder STRING_BUILDER_VAL =
            new StringBuilder("StringBuilder value");
    private static final StringBuffer STRING_BUFFER_VAL =
            new StringBuffer("StringBuffer value");
    private static final CharSequence[] CHAR_SEQUENCE_VAL = {STRING_VAL,
        STRING_BUILDER_VAL, STRING_BUFFER_VAL};

    public static void main(String... args) throws Exception {
        // First, test the tester
        testClass(MyTestClass.class, /*
                 * self-test
                 */ true);
        // Finally, test StringBuffer
        testClass(StringBuffer.class, /*
                 * self-test
                 */ false);
    }

    /**
     * Test all the public, unsynchronized methods of the given class. If
     * isSelfTest is true, this is a self-test to ensure that the test program
     * itself is working correctly. Should help ensure correctness of this
     * program if it changes.
     * <p/>
     * @param aClass - the class to test
     * @param isSelfTest - true if this is the special self-test class
     * @throws SecurityException
     */
    private static void testClass(Class<?> aClass, boolean isSelfTest) throws
            Exception {
        // Get all unsynchronized public methods via reflection.  We don't need
        // to test synchronized methods.  By definition. they are already doing
        // the right thing.
        List<Method> methods = Arrays.asList(aClass.getDeclaredMethods());
        for (Method m : methods) {
            // skip synthetic methods, like default interface methods and lambdas
            if (m.isSynthetic()) {
                continue;
            }
            int modifiers = m.getModifiers();
            if (Modifier.isPublic(modifiers)
                    && !Modifier.isSynchronized(modifiers)) {
                try {
                    testMethod(aClass, m);
                } catch (TestFailedException e) {
                    if (isSelfTest) {
                        String methodName = e.getMethod().getName();
                        switch (methodName) {
                            case "should_pass":
                                throw new RuntimeException(
                                        "Test failed: self-test failed.  The 'should_pass' method did not pass the synchronization test. Check the test code.");
                            case "should_fail":
                                break;
                            default:
                                throw new RuntimeException(
                                        "Test failed: something is amiss with the test. A TestFailedException was generated on a call to "
                                        + methodName + " which we didn't expect to test in the first place.");
                        }
                    } else {
                        throw new RuntimeException("Test failed: the method "
                                + e.getMethod().toString()
                                + " should be synchronized, but isn't.");
                    }
                }
            }
        }
    }

    private static void invokeMethod(Class<?> aClass, final Method m,
            final Object[] args) throws TestFailedException, Exception {
        //System.out.println( "Invoking " + m.toString() + " with parameters " + Arrays.toString(args));
        final Constructor<?> objConstructor;
        Object obj = null;

        objConstructor = aClass.getConstructor(String.class);
        obj = objConstructor.newInstance("LeftPalindrome-emordnilaP-thgiR");

        // test method m for synchronization
        if (!isSynchronized(m, obj, args)) {
            throw new TestFailedException(m);
        }
    }

    private static void testMethod(Class<?> aClass, Method m) throws
            Exception {
        /*
         * Construct call with arguments of the correct type. Note that the
         * values are somewhat irrelevant. If the call actually succeeds, it
         * means we aren't synchronized and the test has failed.
         */
        Class<?>[] pTypes = m.getParameterTypes();
        List<Integer> charSequenceArgs = new ArrayList<>();
        Object[] args = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            // determine the type and create the corresponding actual argument
            Class<?> pType = pTypes[i];
            if (pType.equals(boolean.class)) {
                args[i] = BOOLEAN_VAL;
            } else if (pType.equals(char.class)) {
                args[i] = CHAR_VAL;
            } else if (pType.equals(int.class)) {
                args[i] = INT_VAL;
            } else if (pType.equals(double.class)) {
                args[i] = DOUBLE_VAL;
            } else if (pType.equals(float.class)) {
                args[i] = FLOAT_VAL;
            } else if (pType.equals(long.class)) {
                args[i] = LONG_VAL;
            } else if (pType.equals(Object.class)) {
                args[i] = OBJECT_VAL;
            } else if (pType.equals(StringBuilder.class)) {
                args[i] = STRING_BUILDER_VAL;
            } else if (pType.equals(StringBuffer.class)) {
                args[i] = STRING_BUFFER_VAL;
            } else if (pType.equals(String.class)) {
                args[i] = STRING_VAL;
            } else if (pType.isArray() && pType.getComponentType().equals(char.class)) {
                args[i] = CHAR_ARRAY_VAL;
            } else if (pType.equals(CharSequence.class)) {
                charSequenceArgs.add(new Integer(i));
            } else {
                throw new RuntimeException("Test Failed: not accounting for method call with parameter type of " + pType.getName() + " You must update the test.");
            }
        }
        /*
         * If there are no CharSequence args, we can simply invoke our method
         * and test it
         */
        if (charSequenceArgs.isEmpty()) {
            invokeMethod(aClass, m, args);
        } else {
            /*
             * Iterate through the different CharSequence types and invoke the
             * method for each type.
             */
            if (charSequenceArgs.size() > 1) {
                throw new RuntimeException("Test Failed: the test cannot handle a method with multiple CharSequence arguments.  You must update the test to handle the method "
                        + m.toString());
            }
            for (int j = 0; j < CHAR_SEQUENCE_VAL.length; j++) {
                args[charSequenceArgs.get(0)] = CHAR_SEQUENCE_VAL[j];
                invokeMethod(aClass, m, args);
            }
        }
    }

    @SuppressWarnings("serial")
    private static class TestFailedException extends Exception {

        final Method m;

        public Method getMethod() {
            return m;
        }

        public TestFailedException(Method m) {
            this.m = m;
        }
    }

    static class InvokeTask implements Runnable {

        private final Method m;
        private final Object target;
        private final Object[] args;

        InvokeTask(Method m, Object target, Object... args) {
            this.m = m;
            this.target = target;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                m.invoke(target, args);
            } catch (IllegalAccessException | IllegalArgumentException |
                    InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * isSynchronized tests whether the given method is synchronized or not by
     * invoking it in a thread and testing the thread state after starting the
     * thread
     * <p/>
     * @param m the method to test
     * @param target the object the method is executed on
     * @param args the arguments passed to the method
     * @return true iff the method is synchronized
     */
    private static boolean isSynchronized(Method m, Object target,
            Object... args) {
        Thread t = new Thread(new InvokeTask(m, target, args));

        Boolean isSynchronized = null;

        synchronized (target) {
            t.start();

            while (isSynchronized == null) {
                switch (t.getState()) {
                    case NEW:
                    case RUNNABLE:
                    case WAITING:
                    case TIMED_WAITING:
                        Thread.yield();
                        break;
                    case BLOCKED:
                        isSynchronized = true;
                        break;
                    case TERMINATED:
                        isSynchronized = false;
                        break;
                }
            }
        }

        try {
            t.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        return isSynchronized;
    }

    /*
     * This class is used to test the synchronization tester above. It has a
     * method, should_pass, that is unsynchronized but calls a synchronized
     * method. It has another method, should_fail, which isn't synchronized and
     * doesn't call a synchronized method. The former should pass and the latter
     * should fail.
     */
    private static class MyTestClass {

        @SuppressWarnings("unused")
        public MyTestClass(String s) {
        }

        @SuppressWarnings("unused")
        public void should_pass() {
            // call sync method
            sync_shouldnt_be_tested();
        }

        @SuppressWarnings("unused")
        public void should_fail() {
        }

        public synchronized void sync_shouldnt_be_tested() {
        }
    }
}
