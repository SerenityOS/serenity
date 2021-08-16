/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jpda;

import java.net.*;
import nsk.share.*;

/*
 * This thread class executes in loop native methods with different return types
 */
public class NativeMethodsTestThread extends Thread {

    static {
        System.loadLibrary("NativeMethodsTestThread");
    }

    native void VoidMethod(String message);

    native boolean BooleanMethod(String message);

    native byte ByteMethod(String message);

    native short ShortMethod(String message);

    native char CharMethod(String message);

    native int IntMethod(String message);

    native long LongMethod(String message);

    native float FloatMethod(String message);

    native double DoubleMethod(String message);

    native Object[] ObjectArrayMethod(String message);

    native String StringMethod(String message);

    native Thread ThreadMethod(String message);

    native ThreadGroup ThreadGroupMethod(String message);

    native Class ClassObjectMethod(String message);

    native ClassLoader ClassLoaderMethod(String message);

    native Object ObjectMethod(String message);

    native Boolean BooleanWrapperMethod(String message);

    native Byte ByteWrapperMethod(String message);

    native Short ShortWrapperMethod(String message);

    native Character CharWrapperMethod(String message);

    native Integer IntWrapperMethod(String message);

    native Long LongWrapperMethod(String message);

    native Float FloatWrapperMethod(String message);

    native Double DoubleWrapperMethod(String message);

    private void log(String message) {
        log.display(message);
    }

    public static boolean expectedBooleanValue = Boolean.TRUE;

    public static byte expectedByteValue = Byte.MAX_VALUE;

    public static char expectedCharValue = Character.MAX_VALUE;

    public static short expectedShortValue = Short.MAX_VALUE;

    public static int expectedIntValue = Integer.MAX_VALUE;

    public static long expectedLongValue = Long.MAX_VALUE;

    public static float expectedFloatValue = Float.MAX_VALUE;

    public static double expectedDoubleValue = Double.MAX_VALUE;

    public static Object[] expectedObjectArrayValue = new Object[1000];

    public static Thread expectedThreadValue = new Thread();

    public static ThreadGroup expectedThreadGroupValue = new ThreadGroup("Expected thread group");

    public static Class expectedClassObjectValue = NativeMethodsTestThread.class;

    public static ClassLoader expectedClassLoaderValue = new URLClassLoader(new URL[] {});

    public static String expectedStringValue = "EXPECTED STRING";

    public static Object expectedObjectValue = new Object();

    public static Boolean expectedBooleanWrapperValue = Boolean.valueOf(Boolean.TRUE);

    public static Byte expectedByteWrapperValue = Byte.valueOf(Byte.MAX_VALUE);

    public static Character expectedCharWrapperValue = Character.valueOf(Character.MAX_VALUE);

    public static Short expectedShortWrapperValue = Short.valueOf(Short.MAX_VALUE);

    public static Integer expectedIntWrapperValue = Integer.valueOf(Integer.MAX_VALUE);

    public static Long expectedLongWrapperValue = Long.valueOf(Long.MAX_VALUE);

    public static Float expectedFloatWrapperValue = Float.valueOf(Float.MAX_VALUE);

    public static Double expectedDoubleWrapperValue = Double.valueOf(Double.MAX_VALUE);

    // names of tested types, this names can be used to derive names of tested methods(typeName + 'Method'),
    public static String testedTypesNames[] = {"Void", "Boolean", "Byte", "Short", "Char", "Int", "Long", "Float", "Double", "ObjectArray",
            "String", "Thread", "ThreadGroup", "ClassObject", "ClassLoader", "Object", "BooleanWrapper", "ByteWrapper", "ShortWrapper",
            "CharWrapper", "IntWrapper", "LongWrapper", "FloatWrapper", "DoubleWrapper" };

    private Log log;

    // is forceEarlyReturn would called for this thread
    private boolean isTestThread;

    // how many times call all test methods
    private int iterationsNumber = 1;

    // test thread wait on 'startExecutionWicket' in beginning of run()
    private Wicket startExecutionWicket = new Wicket();

    private boolean success = true;

    public NativeMethodsTestThread(Log log, boolean isTestThread, int iterationNumber) {
        this.log = log;
        this.isTestThread = isTestThread;

        this.iterationsNumber = iterationNumber;
    }

    private volatile boolean stopExecution;

    public void stopExecution() {
        stopExecution = true;
    }

    public void startExecuion() {
        startExecutionWicket.unlockAll();
    }

    public void run() {
        // first, debuggee VM starts and suspends test threads to let debugger initialize breakpoints
        startExecutionWicket.waitFor();

        int iterationCount = 0;

        // test thread executes test methods 'iterationNumber' times
        // non-test thread execute until not interrupted
        while ((iterationCount++ < iterationsNumber) || (!isTestThread && !stopExecution)) {
            // execute test methods in order given in 'testMethodsNames' array
            for (int i = 0; i < testedTypesNames.length; i++) {
                executeMethod(testedTypesNames[i] + "Method");
            }
        }

        log("Test thread exit");
    }

    // execute test method and check that correct value is returned
    private void executeMethod(String methodName) {
        String message = Thread.currentThread() + " in " + methodName;
        if (methodName.equals("VoidMethod")) {
            VoidMethod(message);
        }
        if (methodName.equals("BooleanMethod")) {
            boolean result = BooleanMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ByteMethod")) {
            byte result = ByteMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("CharMethod")) {
            char result = CharMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ShortMethod")) {
            short result = ShortMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("IntMethod")) {
            int result = IntMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("LongMethod")) {
            long result = LongMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("FloatMethod")) {
            float result = FloatMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("DoubleMethod")) {
            double result = DoubleMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("StringMethod")) {
            String result = StringMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ObjectMethod")) {
            Object result = ObjectMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ObjectArrayMethod")) {
            Object[] result = ObjectArrayMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ThreadMethod")) {
            Thread result = ThreadMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ThreadGroupMethod")) {
            ThreadGroup result = ThreadGroupMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ClassObjectMethod")) {
            Class result = ClassObjectMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ClassLoaderMethod")) {
            ClassLoader result = ClassLoaderMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("BooleanWrapperMethod")) {
            Boolean result = BooleanWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ByteWrapperMethod")) {
            Byte result = ByteWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("ShortWrapperMethod")) {
            Short result = ShortWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("CharWrapperMethod")) {
            Character result = CharWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("IntWrapperMethod")) {
            Integer result = IntWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("LongWrapperMethod")) {
            Long result = LongWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("FloatWrapperMethod")) {
            Float result = FloatWrapperMethod(message);

            log("Result: " + result);
        }
        if (methodName.equals("DoubleWrapperMethod")) {
            Double result = DoubleWrapperMethod(message);

            log("Result: " + result);
        }
    }

    public boolean getSuccess() {
        return success;
    }
}
