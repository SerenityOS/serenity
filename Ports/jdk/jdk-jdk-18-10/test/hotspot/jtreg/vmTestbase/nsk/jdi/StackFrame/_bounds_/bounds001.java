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
package nsk.jdi.StackFrame._bounds_;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;

import java.io.*;
import java.lang.reflect.Array;

/**
 * The test checks up the methods:                                          <br>
 *     <code>com.sun.jdi.StackFrame.setValue(Var, Object)</code>     <br>
 *     <code>com.sun.jdi.StackFrame.getValue(Var)</code>             <br>
 * for boundry values of primitive types                                    <br>
 *
 * Test checks up the following assertion:                                  <br>
 *      Primitive arguments must be either assignment compatible with       <br>
 *      the Var type or must be convertible to the Var type             <br>
 *      without loss of information.                                        <br>
 * for every primitive type.                                                <br>
 */

public class bounds001 extends ValueConversionDebugger {

    static class TestedVariableData {
        String name;
        ValueType type;

        TestedVariableData(String name, ValueType type) {
            this.name = name;
            this.type = type;
        }
    }

    private static TestedVariableData[] testedVars = {
                                            new TestedVariableData("byteVar", BYTE),
                                            new TestedVariableData("charVar", CHAR),
                                            new TestedVariableData("doubleVar", DOUBLE),
                                            new TestedVariableData("floatVar", FLOAT),
                                            new TestedVariableData("intVar", INT),
                                            new TestedVariableData("longVar", LONG),
                                            new TestedVariableData("shortVar", SHORT)
    };

    private static byte     [] byteVarValues =
                                    {Byte.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Byte.MAX_VALUE};
    private static char     [] charVarValues =
                                    {Character.MIN_VALUE,
                                    Character.MAX_VALUE};
    private static double   [] doubleVarValues =
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
    private static float    [] floatVarValues =
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
    private static int      [] intVarValues =
                                    {Integer.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    1234567890,
                                    Integer.MAX_VALUE};
    private static long     [] longVarValues =
                                    {Long.MIN_VALUE,
                                    -1L,
                                    0L,
                                    1234567890123456789L,
                                    1L,
                                    Long.MAX_VALUE};
    private static short    [] shortVarValues =
                                    {Short.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Short.MAX_VALUE};

    protected String debuggeeClassName() {
        return bounds001a.class.getName();
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        return new bounds001().runIt(argv, out);
    }

    protected void doTest() {
        debuggee.suspend();

        ThreadReference thread = debuggee.threadByName(bounds001a.TEST_THREAD_NAME);
        StackFrame stackFrame = null;
        try {
            for (int i = 0; i < thread.frameCount(); i++) {
                stackFrame = thread.frame(i);
                if (stackFrame.location().method().name().equals("run") ) {
                    break;
                }
            }
        } catch (IncompatibleThreadStateException e) {
            complain("Unexpected " + e);
            setSuccess(false);
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        Location loc = stackFrame.location();
        display("StackFrame: " + loc.declaringType().name());
        display("    method: " + loc.method().name());
        display("");

        PrimitiveValue retValue, value;
        Object arr = null;
        boolean validConversion;

        LocalVariable var = null;
        for (TestedVariableData testedVar : testedVars) {
            try {
                var = stackFrame.visibleVariableByName(testedVar.name);
            } catch (AbsentInformationException e) {
                complain("Unexpected " + e);
                setSuccess(false);
                continue;
            }
            display("LocalVariable: " + var.name());
            display("======================");
            for (ValueType type : ValueType.values()) {
                switch (type) {
                case BYTE:
                        arr = byteVarValues;
                        display("byte values");
                        break;
                case CHAR:
                        arr = charVarValues;
                        display("char values");
                        break;
                case DOUBLE:
                        arr = doubleVarValues;
                        display("double values");
                        break;
                case FLOAT:
                        arr = floatVarValues;
                        display("float values");
                        break;
                case INT:
                        arr = intVarValues;
                        display("integer values");
                        break;
                case LONG:
                        arr = longVarValues;
                        display("long values");
                        break;
                case SHORT:
                        arr = shortVarValues;
                        display("short values");
                        break;
                default:
                        complain("***TEST CASE ERROR***");
                        setSuccess(false);
                        continue;
                }
                display("-----------------");
                for (int i = 0; i < Array.getLength(arr); i++) {
                    value = createValue(arr, i);
                    validConversion = isValidConversion(testedVar.type, value);
                    display(">value = " + value.toString());
                    try {
                        stackFrame.setValue(var, value);
                        if (!validConversion) {
                            complain(lastConversion);
                            complain("***InvalidTypeException is not thrown***");
                            display("");
                            setSuccess(false);
                            continue;
                        }
                        retValue = (PrimitiveValue )stackFrame.getValue(var);

                        checkValueConversion(value, retValue);
                    } catch(InvalidTypeException e) {
                        if (validConversion) {
                            complain(lastConversion);
                            complain("*** unexpected InvalidTypeException***");
                            display("");
                            setSuccess(false);
                        } else {
                            display(lastConversion);
                            display("!!!expected InvalidTypeException");
                            display("");
                        }
                    } catch(Exception e) {
                        complain("unexpected " + e);
                        display("");
                        setSuccess(false);
                    }
                }
                display("");
            }
        }

        display("=============");
        display("TEST FINISHES\n");

        debuggee.resume();

        pipe.println(bounds001a.COMMAND_STOP_TEST_THREAD);
        if (!isDebuggeeReady())
            return;
    }
}
