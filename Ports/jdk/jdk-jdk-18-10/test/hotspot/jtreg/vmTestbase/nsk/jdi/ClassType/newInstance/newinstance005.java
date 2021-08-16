/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.newInstance;

import nsk.share.*;
import nsk.share.jdi.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.Array;

/**
 * Test checks up the following assertion:
 *        Primitive arguments must be either assignment compatible with
 *        the argument type or must be convertible to the argument type
 *        without loss of information.
 * for every primitive type
 */

public class newinstance005 extends ValueConversionDebugger {

    private ClassType testedClass;
    private ThreadReference thread;

    static class TestedConstructorData {
        String signature;
        String fieldName;
        ValueType type;

        TestedConstructorData(String signature, String fieldName, ValueType type) {
            this.signature = signature;
            this.fieldName = fieldName;
            this.type = type;
        }
    }

    private static final TestedConstructorData [] testedConstructors = {
                                                    new TestedConstructorData("(B)V", "byteValue", BYTE),
                                                    new TestedConstructorData("(C)V", "charValue", CHAR),
                                                    new TestedConstructorData("(D)V", "doubleValue", DOUBLE),
                                                    new TestedConstructorData("(F)V", "floatValue", FLOAT),
                                                    new TestedConstructorData("(I)V", "intValue", INT),
                                                    new TestedConstructorData("(J)V", "longValue", LONG),
                                                    new TestedConstructorData("(S)V", "shortValue", SHORT)
                                                  };


    private Field field;

    private static byte     [] byteParamValues =
                                    {Byte.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Byte.MAX_VALUE};
    private static char     [] charParamValues =
                                    {Character.MIN_VALUE,
                                    Character.MAX_VALUE};
    private static double   [] doubleParamValues =
                                    {Double.NEGATIVE_INFINITY,
                                    -1.5D,
                                    -1.0D,
                                    -0.0D,
                                    +0.0D,
                                    Double.MIN_VALUE,
                                    1.0D,
                                    1.5D,
                                    Double.MAX_VALUE,
                                    Double.POSITIVE_INFINITY};
    private static float    [] floatParamValues =
                                    {Float.NEGATIVE_INFINITY,
                                    -1.5F,
                                    -1.0F,
                                    -0.0F,
                                    +0.0F,
                                    Float.MIN_VALUE,
                                    1.0F,
                                    1.5F,
                                    Float.MAX_VALUE,
                                    Float.POSITIVE_INFINITY};
    private static int      [] intParamValues =
                                    {Integer.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    1234567890,
                                    Integer.MAX_VALUE};
    private static long     [] longParamValues =
                                    {Long.MIN_VALUE,
                                    -1L,
                                    0L,
                                    1L,
                                    1234567890123456789L,
                                    Long.MAX_VALUE};
    private static short    [] shortParamValues =
                                    {Short.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Short.MAX_VALUE};

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new newinstance005().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return newinstance005a.class.getName();
    }

    protected void doTest() {
        initDefaultBreakpoint();

        BreakpointEvent brkpEvent = forceBreakpoint();

        testedClass = (ClassType )debuggee.classByName(debuggeeClassNameWithoutArgs());

        thread = brkpEvent.thread();

        display("\nTEST BEGINS");
        display("===========");


        PrimitiveValue value = null;
        List<Value> params = new ArrayList<Value>();

        Method method;
        Object arr = null;
        // Type type = null;
        boolean validConversion;
        for (TestedConstructorData testedConstructor : testedConstructors) {
            method = testedClass.concreteMethodByName("<init>", testedConstructor.signature);
            if (method == null) {
                complain("method :<init>" + testedConstructor.signature + " not found");
                setSuccess(false);
                continue;
            }
            display("");
            field = testedClass.fieldByName(testedConstructor.fieldName);
            display("Method      : " + method);
            display("Field       : " + field);
            display("=============");
            for (ValueType type : ValueType.values()) {
                switch (type) {
                case BYTE:
                        arr = byteParamValues;
                        display("\tbyte values");
                        break;
                case CHAR:
                        arr = charParamValues;
                        display("\tchar values");
                        break;
                case DOUBLE:
                        arr = doubleParamValues;
                        display("\tdouble values");
                        break;
                case FLOAT:
                        arr = floatParamValues;
                        display("\tfloat values");
                        break;
                case INT:
                        arr = intParamValues;
                        display("\tinteger values");
                        break;
                case LONG:
                        arr = longParamValues;
                        display("\tlong values");
                        break;
                case SHORT:
                        arr = shortParamValues;
                        display("\tshort values");
                        break;
                default:
                        complain("\t***TEST CASE ERROR***");
                        setSuccess(false);
                        continue;
                }

                display("\t--------------");
                for (int i = 0; i < Array.getLength(arr); i++) {
                    params.clear();
                    value = createValue(arr, i);
                    params.add(value);
                    validConversion = isValidConversion(testedConstructor.type, value);
                    try {
                        newInstance(thread, method, params, value);
                        if (!validConversion) {
                            complain(lastConversion);
                            complain("***InvalidTypeException is not thrown***");
                            setSuccess(false);
                        }
                    } catch(InvalidTypeException e) {
                        if (validConversion) {
                            complain(lastConversion);
                            complain("*** unexpected InvalidTypeException***");
                            setSuccess(false);
                        } else {
                            display(lastConversion);
                            display("!!!expected InvalidTypeException");
                        }
                    }
                    display("");
                }
            }
        }

        display("=============");
        display("TEST FINISHES\n");

        removeDefaultBreakpoint();

        debuggee.resume();
    }

    private void newInstance(ThreadReference thread, Method method, List<Value> params,
                                    PrimitiveValue expectedValue) throws InvalidTypeException {
        ObjectReference objRef = null;
        PrimitiveValue param;
        try {
            for (int i = 0; i < params.size(); i++) {
                param = (PrimitiveValue )params.get(i);
                if (param instanceof CharValue) {
                    display("\tParameters  : " + Integer.toHexString(param.charValue()) + "(" + param.type() + ")");
                } else {
                    display("\tParameters  : " + param + "(" + param.type() + ")");
                }
            }
            objRef = testedClass.newInstance(thread, method, params,
                                                    ClassType.INVOKE_SINGLE_THREADED);
        } catch(ClassNotLoadedException e) {
            complain("exception: " + e);
            setSuccess(false);
        } catch(IncompatibleThreadStateException e) {
            complain("exception: " + e);
            setSuccess(false);
        } catch(InvocationException e) {
            complain("exception: " + e);
            setSuccess(false);
        }

        PrimitiveValue returnedValue = (PrimitiveValue)objRef.getValue(field);

        String retType = returnedValue != null ? returnedValue.type().toString()
                                                  : "";
        display("\tReturn value: " + returnedValue + "(" + retType + ")");

        checkValueConversion(expectedValue, returnedValue);
    }
}
