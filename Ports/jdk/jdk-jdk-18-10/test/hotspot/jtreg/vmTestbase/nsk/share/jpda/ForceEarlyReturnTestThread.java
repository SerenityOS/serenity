/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.share.jpda;

import java.net.*;
import nsk.share.*;

// ForceEarlyReturnTestThread intended for testing ForceEarlyReturn functionality.
//
// ForceEarlyReturnTestThread contains test methods with different return type
// (types corresponds to subclasses of com.sun.jdi.Value or JDWP type identifiers).
// Debugger VM should set breakpoint on line log("...") and when test thread stop at breakpoint call
// forceEarlyReturn(), so instructions after breakpoint shouldn't be executed(unexpectedMethod() shouldn't be called)
// When started thread executes all test methods in order assigned in 'testedTypesNames' array.
// It is possible to run this thread in 'testThread' mode, in this mode thread check that values returned from
// test methods equals to those that should be returned through forceEarlyReturn, and no
// instructions was executed in called method after force return (finally blocks are not executed too).
// In non-testThread mode thread check that values returned from test methods was not changed.
public class ForceEarlyReturnTestThread
extends Thread
{
    void VoidMethod()
    {
        try
        {
            log("in void method"); // breakpointLines[0]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();
    }

    boolean BooleanMethod()
    {
        try
        {
            log("in boolean method"); // breakpointLines[1]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedBooleanValue;
    }

    byte ByteMethod()
    {
        try
        {
            log("in byte method"); // breakpointLines[2]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedByteValue;
    }

    short ShortMethod()
    {
        try
        {
            log("in short method"); // breakpointLines[3]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedShortValue;
    }

    char CharMethod()
    {
        try
        {
            log("in char method"); // breakpointLines[4]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedCharValue;
    }

    int IntMethod()
    {
        try
        {
            log("in int method"); // breakpointLines[5]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedIntValue;
    }

    long LongMethod()
    {
        try
        {
            log("in long method"); // breakpointLines[6]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedLongValue;
    }

    float FloatMethod()
    {
        try
        {
            log("in float method"); // breakpointLines[7]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedFloatValue;
    }

    double DoubleMethod()
    {
        try
        {
            log("in double method"); // breakpointLines[8]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedDoubleValue;
    }

    Object[] ObjectArrayMethod()
    {
        try
        {
            log("in object array method"); // breakpointLines[9]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedObjectArrayValue;
    }

    String StringMethod()
    {
        try
        {
            log("in string method"); // breakpointLines[10]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedStringValue;
    }

    Thread ThreadMethod()
    {
        try
        {
            log("in thread method"); // breakpointLines[11]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedThreadValue;
    }

    ThreadGroup ThreadGroupMethod()
    {
        try
        {
            log("in thread group method"); // breakpointLines[12]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedThreadGroupValue;
    }

    Class<?> ClassObjectMethod()
    {
        try
        {
            log("in class object method"); // breakpointLines[13]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedClassObjectValue;
    }

    ClassLoader ClassLoaderMethod()
    {
        try
        {
            log("in class loader method"); // breakpointLines[14]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedClassLoaderValue;
    }

    Object ObjectMethod()
    {
        try
        {
            log("in object method"); // breakpointLines[15]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedObjectValue;
    }

    Boolean BooleanWrapperMethod()
    {
        try
        {
            log("in boolean wrapper method"); // breakpointLines[16]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedBooleanWrapperValue;
    }

    Byte ByteWrapperMethod()
    {
        try
        {
            log("in byte wrapper method"); // breakpointLines[17]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedByteWrapperValue;
    }

    Short ShortWrapperMethod()
    {
        try
        {
            log("in short wrapper method"); // breakpointLines[18]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedShortWrapperValue;
    }

    Character CharWrapperMethod()
    {
        try
        {
            log("in char wrapper method"); // breakpointLines[19]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedCharWrapperValue;
    }

    Integer IntWrapperMethod()
    {
        try
        {
            log("in int wrapper method"); // breakpointLines[20]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedIntWrapperValue;
    }

    Long LongWrapperMethod()
    {
        try
        {
            log("in long wrapper method"); // breakpointLines[21]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedLongWrapperValue;
    }

    Float FloatWrapperMethod()
    {
        try
        {
            log("in float wrapper method"); // breakpointLines[22]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedFloatWrapperValue;
    }

    Double DoubleWrapperMethod()
    {
        try
        {
            log("in double wrapper method"); // breakpointLines[23]
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedDoubleWrapperValue;
    }

    private void log(String message)
    {
        log.display(currentThread().getName() + ": " + message);
    }

    private void logError(String message)
    {
        log.complain(currentThread().getName() + ": " + message);
    }

    // values which should be passed in forceEarlyReturn():

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
    public static Class<?> expectedClassObjectValue = ForceEarlyReturnTestThread.class;
    public static ClassLoader expectedClassLoaderValue = new URLClassLoader(new URL[]{});
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

    // values which should be returned from test methods without forceEarlyReturn():

    public static boolean unexpectedBooleanValue = Boolean.FALSE;
    public static byte unexpectedByteValue = 0;
    public static char unexpectedCharValue = 0;
    public static short unexpectedShortValue = 0;
    public static int unexpectedIntValue = 0;
    public static long unexpectedLongValue = 0;
    public static float unexpectedFloatValue = 0;
    public static double unexpectedDoubleValue = 0;
    public static Object[] unexpectedObjectArrayValue = new Object[1000];
    public static String unexpectedStringValue = "UNEXPECTED STRING";
    public static Thread unexpectedThreadValue = new Thread();
    public static ThreadGroup unexpectedThreadGroupValue = new ThreadGroup("Unexpected thread group");
    public static Class<?> unexpectedClassObjectValue = Object.class;
    public static ClassLoader unexpectedClassLoaderValue = new URLClassLoader(new URL[]{});
    public static Object unexpectedObjectValue = new Object();
    public static Boolean unexpectedBooleanWrapperValue = Boolean.valueOf(Boolean.FALSE);
    public static Byte unexpectedByteWrapperValue = Byte.valueOf((byte)0);
    public static Character unexpectedCharWrapperValue = Character.valueOf((char)0);
    public static Short unexpectedShortWrapperValue = Short.valueOf((short)0);
    public static Integer unexpectedIntWrapperValue = Integer.valueOf(0);
    public static Long unexpectedLongWrapperValue = Long.valueOf(0);
    public static Float unexpectedFloatWrapperValue = Float.valueOf(0);
    public static Double unexpectedDoubleWrapperValue = Double.valueOf(0);

    public static int[] breakpointLines = {
        49,
        63,
        79,
        95,
        111,
        127,
        143,
        159,
        175,
        191,
        207,
        223,
        239,
        255,
        271,
        287,
        303,
        319,
        335,
        351,
        367,
        383,
        399,
        415};

    /* Invalid data for ForceEarlyReturn, needed to check is ForceEarlyReturn complies with following part of specification:
     * Object values must be assignment compatible with the method return type
     * (This implies that the method return type must be loaded through the enclosing class's class loader).
     * Primitive values must be either assignment compatible with the method return type or must
     * be convertible to the variable type without loss of information.
     */
    public static boolean invalidVoidValue = Boolean.TRUE;
    public static boolean invalidObjectValue = Boolean.TRUE;
    public static byte invalidBooleanValue = Byte.MAX_VALUE;
    public static short invalidByteValue = Short.MAX_VALUE;
    public static char invalidShortValue = Character.MAX_VALUE;
    public static int invalidCharValue = Integer.MAX_VALUE;
    public static long invalidIntValue = Long.MAX_VALUE;
    public static float invalidLongValue = Float.MAX_VALUE;
    public static double invalidFloatValue = Double.MAX_VALUE;
    public static Object[] invalidDoubleValue = new Object[1000];
    public static String invalidObjectArrayValue = "EXPECTED STRING";
    public static Thread invalidStringValue = new Thread("Invalid thread");
    public static ThreadGroup invalidThreadValue = new ThreadGroup("Invalid thread group");
    public static Class<?> invalidThreadGroupValue = ForceEarlyReturnTestThread.class;
    public static ClassLoader invalidClassObjectValue = new URLClassLoader(new URL[]{});
    public static Object invalidClassLoaderValue = new Object();
    public static Byte invalidBooleanWrapperValue = Byte.valueOf(Byte.MAX_VALUE);
    public static Short invalidByteWrapperValue = Short.valueOf(Short.MAX_VALUE);
    public static Character invalidShortWrapperValue = Character.valueOf(Character.MAX_VALUE);
    public static Integer invalidCharWrapperValue = Integer.valueOf(Integer.MAX_VALUE);
    public static Long invalidIntWrapperValue = Long.valueOf(Long.MAX_VALUE);
    public static Float invalidLongWrapperValue = Float.valueOf(Float.MAX_VALUE);
    public static Double invalidFloatWrapperValue = Double.valueOf(Double.MAX_VALUE);
    public static Object[] invalidDoubleWrapperValue = new Object[1000];

    // names of tested types, this names can be used to derive names of tested methods(typeName + 'Method'),
    // names of fields containing predefined data to be returned through ForceEarlyReturn('expected' + typeName + 'Value'),
    // names of fields containing invalid data for ForceEarlyReturn(needed to check is ForceEarlyReturn complies with its specification('invalid' + typeName + 'Value'))
    public static String testedTypesNames[] =
    {
        "Void",
        "Boolean",
        "Byte",
        "Short",
        "Char",
        "Int",
        "Long",
        "Float",
        "Double",
        "ObjectArray",
        "String",
        "Thread",
        "ThreadGroup",
        "ClassObject",
        "ClassLoader",
        "Object",
        "BooleanWrapper",
        "ByteWrapper",
        "ShortWrapper",
        "CharWrapper",
        "IntWrapper",
        "LongWrapper",
        "FloatWrapper",
        "DoubleWrapper",
        };

    private Log log;

    // is forceEarlyReturn would called for this thread
    private boolean isTestThread;

    // how many times call all test methods (zero means infinite execution)
    private int iterationsNumber = 1;

    // test thread wait on 'startExecutionWicket' in beginning of run()
    private Wicket startExecutionWicket = new Wicket();

    private boolean success = true;

    public ForceEarlyReturnTestThread(Log log, boolean isTestThread, int iterationNumber)
    {
        this.log = log;
        this.isTestThread = isTestThread;

        this.iterationsNumber = iterationNumber;
    }

    private volatile boolean stopExecution;

    public void stopExecution()
    {
        stopExecution = true;
    }

    public void startExecuion()
    {
        startExecutionWicket.unlockAll();
    }

    public void run()
    {
        // first, debuggee VM starts and suspends test threads to let debugger initialize breakpoints
        startExecutionWicket.waitFor();

        int iterationCount = 0;

        // test thread executes test methods 'iterationNumber' times
        // non-test thread execute until not interrupted
        // (iterationsNumber = 0 means infinite execution)
        while(!stopExecution && (!isTestThread || ((iterationsNumber == 0) || (iterationCount++ < iterationsNumber))))
        {
            // execute test methods in order given in 'testMethodsNames' array
            for(int i = 0; (i < testedTypesNames.length) && !stopExecution; i++)
            {
                executeMethod(testedTypesNames[i] + "Method");

                /*
                 * Small delay was inserted because of if test starts several ForceEarlyReturnTestThreads
                 * with parameter isTestThread = false, these threads may consume too many CPU time and test
                 * execution will be very slow
                 */
                if (!isTestThread) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        logError("Unexpected exception: " + e);
                        e.printStackTrace(log.getOutStream());
                        success = false;
                    }
                }
            }
        }

        log("Test thread exit");
    }

    // execute test method and check that correct value is returned
    private void executeMethod(String methodName)
    {
        if(methodName.equals("VoidMethod"))
        {
            VoidMethod();
        }
        if(methodName.equals("BooleanMethod"))
        {
            boolean result = BooleanMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            boolean expectedResult;

            expectedResult = isTestThread ? expectedBooleanValue : unexpectedBooleanValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ByteMethod"))
        {
            byte result = ByteMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            byte expectedResult;

            expectedResult = isTestThread ? expectedByteValue : unexpectedByteValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("CharMethod"))
        {
            char result = CharMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            char expectedResult;

            expectedResult = isTestThread ? expectedCharValue : unexpectedCharValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ShortMethod"))
        {
            short result = ShortMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            short expectedResult;

            expectedResult = isTestThread ? expectedShortValue : unexpectedShortValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("IntMethod"))
        {
            int result = IntMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            int expectedResult;

            expectedResult = isTestThread ? expectedIntValue : unexpectedIntValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("LongMethod"))
        {
            long result = LongMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            long expectedResult;

            expectedResult = isTestThread ? expectedLongValue : unexpectedLongValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("FloatMethod"))
        {
            float result = FloatMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            float expectedResult;

            expectedResult = isTestThread ? expectedFloatValue : unexpectedFloatValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("DoubleMethod"))
        {
            double result = DoubleMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            double expectedResult;

            expectedResult = isTestThread ? expectedDoubleValue : unexpectedDoubleValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("StringMethod"))
        {
            String result = StringMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            String expectedResult;

            expectedResult = isTestThread ? expectedStringValue : unexpectedStringValue;

            if(!result.equals(expectedResult))
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ObjectMethod"))
        {
            Object result = ObjectMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Object expectedResult;

            expectedResult = isTestThread ? expectedObjectValue : unexpectedObjectValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ObjectArrayMethod"))
        {
            Object[] result = ObjectArrayMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Object[] expectedResult;

            expectedResult = isTestThread ? expectedObjectArrayValue : unexpectedObjectArrayValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ThreadMethod"))
        {
            Thread result = ThreadMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Thread expectedResult;

            expectedResult = isTestThread ? expectedThreadValue : unexpectedThreadValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ThreadGroupMethod"))
        {
            ThreadGroup result = ThreadGroupMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            ThreadGroup expectedResult;

            expectedResult = isTestThread ? expectedThreadGroupValue : unexpectedThreadGroupValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ClassObjectMethod"))
        {
            Class<?> result = ClassObjectMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Class<?> expectedResult;

            expectedResult = isTestThread ? expectedClassObjectValue : unexpectedClassObjectValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ClassLoaderMethod"))
        {
            ClassLoader result = ClassLoaderMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            ClassLoader expectedResult;

            expectedResult = isTestThread ? expectedClassLoaderValue : unexpectedClassLoaderValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("BooleanWrapperMethod"))
        {
            Boolean result = BooleanWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Boolean expectedResult;

            expectedResult = isTestThread ? expectedBooleanWrapperValue : unexpectedBooleanWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ByteWrapperMethod"))
        {
            Byte result = ByteWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Byte expectedResult;

            expectedResult = isTestThread ? expectedByteWrapperValue : unexpectedByteWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ShortWrapperMethod"))
        {
            Short result = ShortWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Short expectedResult;

            expectedResult = isTestThread ? expectedShortWrapperValue : unexpectedShortWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("CharWrapperMethod"))
        {
            Character result = CharWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Character expectedResult;

            expectedResult = isTestThread ? expectedCharWrapperValue : unexpectedCharWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("IntWrapperMethod"))
        {
            Integer result = IntWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Integer expectedResult;

            expectedResult = isTestThread ? expectedIntWrapperValue : unexpectedIntWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("LongWrapperMethod"))
        {
            Long result = LongWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Long expectedResult;

            expectedResult = isTestThread ? expectedLongWrapperValue : unexpectedLongWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("FloatWrapperMethod"))
        {
            Float result = FloatWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Float expectedResult;

            expectedResult = isTestThread ? expectedFloatWrapperValue : unexpectedFloatWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("DoubleWrapperMethod"))
        {
            Double result = DoubleWrapperMethod();

            // log(Thread.currentThread() + ": result of " + methodName + ": " + result);

            Double expectedResult;

            expectedResult = isTestThread ? expectedDoubleWrapperValue : unexpectedDoubleWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
    }

    // method which shouldn't be executed in test thread
    void unexpectedMethod()
    {
        if(isTestThread)
        {
            success = false;
            logError("unexpected code is executed after forceEarlyReturn");
        }
    }

    public boolean getSuccess()
    {
        return success;
    }
}
