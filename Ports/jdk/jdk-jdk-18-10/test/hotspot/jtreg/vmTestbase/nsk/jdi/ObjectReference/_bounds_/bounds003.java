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

package nsk.jdi.ObjectReference._bounds_;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;

import java.io.*;
import java.lang.reflect.Array;

/**
 * The test checks up the methods:                                          <br>
 *     <code>com.sun.jdi.ObjectReference.setValue(Field, Object)</code>     <br>
 *     <code>com.sun.jdi.ObjectReference.getValue(Field)</code>             <br>
 * for boundary values of primitive types                                    <br>
 *
 * Test checks up the following assertion:                                  <br>
 *      Primitive arguments must be either assignment compatible with       <br>
 *      the field type or must be convertible to the field type             <br>
 *      without loss of information.                                        <br>
 * for every primitive type.                                                <br>
 */

public class bounds003 extends ValueConversionDebugger {

    private static class TestedFieldData {
        public String fieldName;
        public ValueType fieldType;

        public TestedFieldData(String fieldName, ValueType fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }
    }

    private static TestedFieldData[] testedFields = {
                                            new TestedFieldData("byteField", BYTE),
                                            new TestedFieldData("charField", CHAR),
                                            new TestedFieldData("doubleField", DOUBLE),
                                            new TestedFieldData("floatField", FLOAT),
                                            new TestedFieldData("intField", INT),
                                            new TestedFieldData("longField", LONG),
                                            new TestedFieldData("shortField", SHORT)
    };

    private static byte     [] byteFieldValues =
                                    {Byte.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Byte.MAX_VALUE};
    private static char     [] charFieldValues =
                                    {Character.MIN_VALUE,
                                    Character.MAX_VALUE};
    private static double   [] doubleFieldValues =
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
    private static float    [] floatFieldValues =
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
    private static int      [] intFieldValues =
                                    {Integer.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    1234567890,
                                    Integer.MAX_VALUE};
    private static long     [] longFieldValues =
                                    {Long.MIN_VALUE,
                                    -1L,
                                    0L,
                                    1L,
                                    1234567890123456789L,
                                    Long.MAX_VALUE};
    private static short    [] shortFieldValues =
                                    {Short.MIN_VALUE,
                                    -1,
                                    0,
                                    1,
                                    Short.MAX_VALUE};

    protected String debuggeeClassName() {
        return bounds003a.class.getName();
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        return new bounds003().runIt(argv, out);
    }

    protected void doTest() {
        debuggee.suspend();

        ReferenceType debugeeClass = debuggee.classByName(debuggeeClassNameWithoutArgs());

        Field field = debugeeClass.fieldByName(bounds003a.testedFieldName);
        ObjectReference objRef = (ObjectReference )debugeeClass.getValue(field);
        ReferenceType testedClass = objRef.referenceType();

        display("\nTEST BEGINS");
        display("===========");

        PrimitiveValue retValue, value;
        Object arr = null;
        boolean validConversion;

        for (TestedFieldData testedField : testedFields) {
            field = testedClass.fieldByName(testedField.fieldName);
            display("Field      : " + field);
            display("======================");
            for (ValueType type : ValueType.values()) {
                switch (type) {
                case BYTE:
                        arr = byteFieldValues;
                        display("byte values");
                        break;
                case CHAR:
                        arr = charFieldValues;
                        display("char values");
                        break;
                case DOUBLE:
                        arr = doubleFieldValues;
                        display("double values");
                        break;
                case FLOAT:
                        arr = floatFieldValues;
                        display("float values");
                        break;
                case INT:
                        arr = intFieldValues;
                        display("integer values");
                        break;
                case LONG:
                        arr = longFieldValues;
                        display("long values");
                        break;
                case SHORT:
                        arr = shortFieldValues;
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
                    validConversion = isValidConversion(testedField.fieldType, value);
                    display(">value = " + value.toString());
                    try {
                        objRef.setValue(field, value);
                        if (!validConversion) {
                            complain(lastConversion);
                            complain("***InvalidTypeException is not thrown***");
                            display("");
                            setSuccess(false);
                            continue;
                        }

                        retValue = (PrimitiveValue) objRef.getValue(field);

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
    }
}
