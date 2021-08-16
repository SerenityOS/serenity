/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4490824
 * @summary JDI: provide arguments when no debug attributes present
 *
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile ArgumentValuesTest.java
 * @run driver ArgumentValuesTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

 /********** target program **********/

class ArgumentValuesTarg {
    static char s_char1 = 'a';
    static byte s_byte1 = (byte) 146;
    static short s_short1 = (short) 28123;
    static int s_int1 = 3101246;
    static long s_long1 = 0x0123456789ABCDEFL;
    static float s_float1 = 2.3145f;
    static double s_double1 = 1.469d;
    static int s_iarray1[] = {1, 2, 3};
    static int s_marray1[][] = {{1, 2, 3}, {3, 4, 5}, null, {6, 7}};
    static String s_sarray1[] = {"abc", null, "def", "ghi"};
    static String s_string1 = "abcdef";

    static String s_string2 = "xy";
    static String s_string3 = "wz";
    static List<Integer> intList;

    public static void noArgs() {
        int index = 0;     // line NO_ARGS_LINE_1
    }

    public static void allArgs(char p_char, byte p_byte, short p_short,
                               int p_int, long p_long, float p_float,
                               double p_double, int p_iarray[], int p_marray[][],
                               String p_sarray1[], String p_string) {
        int index = 0;      // line ALL_ARGS_LINE_1
    }

    public static void varArgs(String ... p1) {
        int index = 0;     // line VAR_ARGS_LINE_1
    }

    public static void genericArgs(List<Integer> p1) {
        int index = 0;     // line GENERIC_ARGS_LINE_1
    }

    public void instanceMethod(char p_char, byte p_byte) {
        int index = 0;     // line INSTANCE_METHOD_LINE_1
    }

    public static void main(String[] args) {
        System.out.println("Howdy!");
        allArgs(
                s_char1,   s_byte1,   s_short1,  s_int1,
                s_long1,   s_float1,  s_double1, s_iarray1,
                s_marray1, s_sarray1, s_string1);

        noArgs();
        varArgs(s_string1, s_string2, s_string3);
        ArgumentValuesTarg avt = new ArgumentValuesTarg();
        intList = new ArrayList<Integer>(10);
        intList.add(10);
        intList.add(20);
        genericArgs(intList);

        avt.instanceMethod(s_char1, s_byte1);

        System.out.println("Goodbye from ArgumentValuesTarg!");
    }
}

 /********** test program **********/

public class ArgumentValuesTest extends TestScaffold {
    static final int NO_ARGS_LINE_1 = 63;
    static final int ALL_ARGS_LINE_1 = 70;
    static final int VAR_ARGS_LINE_1 = 74;
    static final int GENERIC_ARGS_LINE_1 = 78;
    static final int INSTANCE_METHOD_LINE_1 = 82;

    // Must be in same order as args to allArgs(....)
    String fieldNames[] = {"s_char1",   "s_byte1",   "s_short1",  "s_int1",
                           "s_long1",   "s_float1",  "s_double1", "s_iarray1",
                           "s_marray1", "s_sarray1", "s_string1"};

    String fieldNamesVarArgs[] = {"s_string1", "s_string2", "s_string3"};
    String fieldNamesInstance[] = {"s_char1",   "s_byte1"};

    ReferenceType targetClass;
    ThreadReference mainThread;

    ArgumentValuesTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new ArgumentValuesTest (args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {
        /*
         * Get to the top of main() to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("ArgumentValuesTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();


        {
            System.out.println("----- Testing each type of arg");
            bpe = resumeTo("ArgumentValuesTarg", ALL_ARGS_LINE_1);
            StackFrame frame = bpe.thread().frame(0);

            Method mmm = frame.location().method();
            System.out.println("Arg types are: " + mmm.argumentTypeNames());

            List<Value> argVals = frame.getArgumentValues();

            if (argVals.size() != fieldNames.length) {
                failure("failure: Varargs: expected length " + fieldNames.length +
                        " args, got: " + argVals);
            }
            for (int ii = 0; ii < argVals.size(); ii++) {
                Value gotVal = argVals.get(ii);

                Field theField = targetClass.fieldByName(fieldNames[ii]);
                Value expectedVal = targetClass.getValue(theField);
                System.out.println(fieldNames[ii] + ": gotVal = " + gotVal +
                                   ", expected = " + expectedVal);
                //System.out.println(gotVal.getClass() + ", " + expectedVal.getClass());
                if (!gotVal.equals(expectedVal)) {
                    failure("     failure: gotVal != expected");
                }
            }
        }

        // a method with no params
        {
            System.out.println("----- Testing no args");
            bpe = resumeTo("ArgumentValuesTarg", NO_ARGS_LINE_1);
            StackFrame frame = bpe.thread().frame(0);

            Method mmm = frame.location().method();
            System.out.println("Arg types are: " + mmm.argumentTypeNames());

            List<Value> argVals = frame.getArgumentValues();
            if (argVals.size() == 0) {
                System.out.println("Empty arg list ok");
            } else {
                failure("failure: Expected empty val list, got: " + argVals);
            }
        }

        // var args.  3 Strings are passed in and they appear
        // as a String[3] in the method.
        {
            System.out.println("----- Testing var args");
            bpe = resumeTo("ArgumentValuesTarg", VAR_ARGS_LINE_1);
            StackFrame frame = bpe.thread().frame(0);

            Method mmm = frame.location().method();
            System.out.println("Arg types are: " + mmm.argumentTypeNames());

            List<Value> argVals = frame.getArgumentValues();
            if (argVals.size() != 1) {
                failure("failure: Varargs: expected one arg, got: " + argVals);
            }
            argVals = ((ArrayReference)argVals.get(0)).getValues();

            if (argVals.size() != fieldNamesVarArgs.length) {
                failure("failure: Varargs: expected length " + fieldNamesVarArgs.length +
                        " array elements, got: " + argVals);
            }

            for (int ii = 0; ii < argVals.size(); ii++) {
                Value gotVal = argVals.get(ii);

                Field theField = targetClass.fieldByName(fieldNamesVarArgs[ii]);
                Value expectedVal = targetClass.getValue(theField);
                System.out.println(fieldNamesVarArgs[ii] + ": gotVal = " + gotVal +
                                   ", expected = " + expectedVal);
                //System.out.println(gotVal.getClass() + ", " + expectedVal.getClass());
                if (!gotVal.equals(expectedVal)) {
                    failure("     failure: gotVal != expected");
                }
            }
        }

        // a method with with one generic param
        {
            System.out.println("----- Testing generic args");
            bpe = resumeTo("ArgumentValuesTarg", GENERIC_ARGS_LINE_1);
            StackFrame frame = bpe.thread().frame(0);

            Method mmm = frame.location().method();
            System.out.println("Arg types are: " + mmm.argumentTypeNames());

            List<Value> argVals = frame.getArgumentValues();
            if (argVals.size() != 1) {
                failure("failure: Expected one arg, got: " + argVals);
            } else {
                Value gotVal = argVals.get(0);

                Field theField = targetClass.fieldByName("intList");
                Value expectedVal = targetClass.getValue(theField);
                System.out.println("intList " + ": gotVal = " + gotVal +
                                   ", expected = " + expectedVal);
                if (!gotVal.equals(expectedVal)) {
                    failure("failure: gotVal != expected");
                }
            }
        }

        // test instance method call
        {
            System.out.println("----- Testing instance method call");
            bpe = resumeTo("ArgumentValuesTarg", INSTANCE_METHOD_LINE_1);
            StackFrame frame = bpe.thread().frame(0);

            Method mmm = frame.location().method();
            System.out.println("Arg types are: " + mmm.argumentTypeNames());

            List<Value> argVals = frame.getArgumentValues();

            if (argVals.size() != fieldNamesInstance.length) {
                failure("failure: Varargs: expected length " + fieldNamesInstance.length +
                        " args, got: " + argVals);
            }
            for (int ii = 0; ii < argVals.size(); ii++) {
                Value gotVal = argVals.get(ii);

                Field theField = targetClass.fieldByName(fieldNamesInstance[ii]);
                Value expectedVal = targetClass.getValue(theField);
                System.out.println(fieldNamesInstance[ii] + ": gotVal = " + gotVal +
                                   ", expected = " + expectedVal);
                //System.out.println(gotVal.getClass() + ", " + expectedVal.getClass());
                if (!gotVal.equals(expectedVal)) {
                    failure("     failure: gotVal != expected");
                }
            }
        }


        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test if anything has called failure("foo")
         * testFailed will be true
         */
        if (!testFailed) {
            println("ArgumentValuesTest: passed");
        } else {
            throw new Exception("ArgumentValuesTest: failed");
        }
    }
}
