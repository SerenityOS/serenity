/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.ArrayReference.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class getvalues001 {
    final static boolean BOOL[] = {true, false};
    final static byte    BYTE[] = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
    final static char    CHAR[] = {Character.MIN_VALUE, '\u00ff', '\uff00',
                                   Character.MAX_VALUE};
    final static double  DOUB[] = {Double.NEGATIVE_INFINITY, Double.MIN_VALUE,
                                   -1, -0, 0, 1,  Double.MAX_VALUE,
                                   Double.POSITIVE_INFINITY,
                                   Double.NaN};
    final static float   FLOAT[] = {Float.NEGATIVE_INFINITY, Float.MIN_VALUE,
                                    -1, -0, 0, 1, Float.MAX_VALUE,
                                    Float.POSITIVE_INFINITY, Float.NaN};
    final static int     INT[] = {Integer.MIN_VALUE, -1, 0, 1,
                                  Integer.MAX_VALUE};
    final static long    LONG[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    final static short   SHORT[] = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};

    final static String FIELD_NAME[][] = {
        {"z1", "boolean"},
        {"b1", "byte"},
        {"c1", "char"},
        {"d1", "double"},
        {"f1", "float"},
        {"i1", "int"},
        {"l1", "long"},
        {"r1", "short"},

        {"lF1", "long"},
        {"lP1", "long"},
        {"lU1", "long"},
        {"lR1", "long"},
        {"lT1", "long"},
        {"lV1", "long"}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.ArrayReference.getValues.";
    private final static String className = "getvalues001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "getvalues001aClassToCheck";

    public static void main(String argv[]) {
        System.exit(95 + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.bindToDebugee(debugeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = debugee.createIOPipe();
        boolean testFailed = false;

        // Connect with debugee and resume it
        debugee.redirectStderr(out);
        debugee.resume();
        String line = pipe.readln();
        if (line == null) {
            log.complain("debuger FAILURE> UNEXPECTED debugee's signal - null");
            return 2;
        }
        if (!line.equals("ready")) {
            log.complain("debuger FAILURE> UNEXPECTED debugee's signal - "
                      + line);
            return 2;
        }
        else {
            log.display("debuger> debugee's \"ready\" signal recieved.");
        }

        ReferenceType refType = debugee.classByName(classToCheckName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + classToCheckName
                      + " not found.");
           return 2;
        }
        log.display("debuger> Total fields in debugee read: "
                  + refType.allFields().size() + " total fields in debuger: "
                  + FIELD_NAME.length + "\n");

        // Check all array fields from debugee
        for (int i = 0; i < FIELD_NAME.length; i++) {
            Field field;
            String name = FIELD_NAME[i][0];
            String realType = FIELD_NAME[i][1];
            Value value;
            Value arrayValue;
            ArrayReference arrayRef;
            List listOfValues;

            // Get field from debuggee by name
            try {
                field = refType.fieldByName(name);
            } catch (ClassNotPreparedException e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + name);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + name);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field " + field + " read.");

            // Get field's value
            try {
                value = refType.getValue(field);
            } catch (IllegalArgumentException e) {
                log.complain("debuger FAILURE 2> Cannot get value for field "
                           + name);
                log.complain("debuger FAILURE 2> Exception: " + e);
                testFailed = true;
                continue;
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 2> Cannot get value for field "
                           + name);
                log.complain("debuger FAILURE 2> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field value is " + value);

            // Cast to ArrayReference. All fields in debugee are
            // arrays, so ClassCastException should not be thrown
            try {
                arrayRef = (ArrayReference)value;
            } catch (ClassCastException e) {
                log.complain("debuger FAILURE 3> Cannot cast value for field "
                           + name + " to ArrayReference.");
                log.complain("debuger FAILURE 3> Exception: " + e);
                testFailed = true;
                continue;
            }

            // Get all components from array
            try {
                listOfValues = arrayRef.getValues();
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 4> Cannot get values from field "
                           + name);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            }

            // Check each element of the list
            for (int j = 0; j < listOfValues.size(); j++) {
                try {
                    arrayValue = (Value)listOfValues.get(j);
                } catch (ClassCastException e) {
                    log.complain("debuger FAILURE 5> Cannot cast to Value "
                               + j + " element of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                }
                log.display("debuger> " + i + " field has " + j + " value "
                          + arrayValue);

                if (realType.equals("boolean")) {

                    ///////////////////// Check boolean[] /////////////////////
                    BooleanValue boolValue;
                    boolean element;

                    try {
                        boolValue = (BooleanValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE Z1> Cannot cast to "
                                   + "boolean " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE Z1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = boolValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != BOOL[j]) {
                        log.complain("debuger FAILURE Z2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + BOOL[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("byte")) {

                    ///////////////////// Check byte[] /////////////////////
                    ByteValue byteValue;
                    byte element;

                    try {
                        byteValue = (ByteValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE B1> Cannot cast to "
                                   + "byte " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE B1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = byteValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != BYTE[j]) {
                        log.complain("debuger FAILURE B2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + BYTE[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("char")) {

                    ///////////////////// Check char[] /////////////////////
                    CharValue charValue;
                    char element;

                    try {
                        charValue = (CharValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE C1> Cannot cast to "
                                   + "char " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE C1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = charValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != CHAR[j]) {
                        log.complain("debuger FAILURE C2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + CHAR[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("double")) {

                    ///////////////////// Check double[] /////////////////////
                    DoubleValue doubleValue;
                    Double element;

                    try {
                        doubleValue = (DoubleValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE D1> Cannot cast to "
                                   + "double " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE D1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = Double.valueOf(doubleValue.value());
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (!element.equals(Double.valueOf(DOUB[j]))) {
                        log.complain("debuger FAILURE D2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + DOUB[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("float")) {

                    ///////////////////// Check float[] /////////////////////
                    FloatValue floatValue;
                    Float element;

                    try {
                        floatValue = (FloatValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE F1> Cannot cast to "
                                   + "float " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE F1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = Float.valueOf(floatValue.value());
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (!element.equals(Float.valueOf(FLOAT[j]))) {
                        log.complain("debuger FAILURE F2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + FLOAT[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("int")) {

                    ///////////////////// Check int[] /////////////////////
                    IntegerValue intValue;
                    int element;

                    try {
                        intValue = (IntegerValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE I1> Cannot cast to "
                                   + "int " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE I1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = intValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != INT[j]) {
                        log.complain("debuger FAILURE I2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + INT[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long")) {

                    ///////////////////// Check long[] /////////////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE L1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE L1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONG[j]) {
                        log.complain("debuger FAILURE L2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONG[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("short")) {

                    ///////////////////// Check short[] /////////////////////
                    ShortValue shortValue;
                    short element;

                    try {
                        shortValue = (ShortValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE R1> Cannot cast to "
                                   + "short " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE R1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = shortValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != SHORT[j]) {
                        log.complain("debuger FAILURE R2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + SHORT[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }

                } else {
                    log.complain("debuger FAILURE 6> Unexpected type: "
                               + realType);
                    testFailed = true;
                    break;
                }
            }
            log.display("debuger> " + i + " field checked.\n");
        }

        pipe.println("quit");
        debugee.waitFor();
        int status = debugee.getStatus();
        if (testFailed) {
            log.complain("debuger FAILURE> TEST FAILED");
            return 2;
        } else {
            if (status == 95) {
                log.display("debuger> expected Debugee's exit "
                          + "status - " + status);
                return 0;
            } else {
                log.complain("debuger FAILURE> UNEXPECTED Debugee's exit "
                           + "status (not 95) - " + status);
                return 2;
            }
        }
    }
}
