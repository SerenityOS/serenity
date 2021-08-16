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


package nsk.jdi.ArrayReference.setValues_l;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class setvaluesl001 {
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

    final static boolean BOOL[]  = {true, false};
    final static byte    BYTE[]  = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
    final static char    CHAR[]  = {Character.MIN_VALUE, '\u00ff', '\uff00',
                                   Character.MAX_VALUE};
    final static double  DOUB[]  = {Double.NEGATIVE_INFINITY, Double.MIN_VALUE,
                                    -1, -0, 0, 1, Double.MAX_VALUE,
                                    Double.POSITIVE_INFINITY, Double.NaN};
    final static float   FLOAT[]  = {Float.NEGATIVE_INFINITY, Float.MIN_VALUE,
                                     -1, -0, 0, 1, Float.MAX_VALUE,
                                     Float.POSITIVE_INFINITY, Float.NaN};
    final static int     INT[]  = {Integer.MIN_VALUE, -1, 0, 1,
                                  Integer.MAX_VALUE, Integer.MIN_VALUE + 1};
    final static long    LONG[]  = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    final static short   SHORT[]  = {Short.MIN_VALUE, -1, 0, 1,
                                     Short.MAX_VALUE};

    private static Log log;
    private final static String prefix = "nsk.jdi.ArrayReference.setValues_l.";
    private final static String className = "setvaluesl001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "setvaluesl001aClassToCheck";

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
        Field fieldBoolean = null;
        Field fieldByte = null;
        Field fieldChar = null;
        Field fieldDouble = null;
        Field fieldFloat = null;
        Field fieldInt = null;
        Field fieldLong = null;
        Field fieldShort = null;
        ArrayReference arrayBoolean = null;
        ArrayReference arrayByte = null;
        ArrayReference arrayChar = null;
        ArrayReference arrayDouble = null;
        ArrayReference arrayFloat = null;
        ArrayReference arrayInt = null;
        ArrayReference arrayLong = null;
        ArrayReference arrayShort = null;
        List<? extends com.sun.jdi.Value> listBoolean = null;
        List<? extends com.sun.jdi.Value> listByte = null;
        List<? extends com.sun.jdi.Value> listChar = null;
        List<? extends com.sun.jdi.Value> listDouble = null;
        List<? extends com.sun.jdi.Value> listFloat = null;
        List<? extends com.sun.jdi.Value> listInt = null;
        List<? extends com.sun.jdi.Value> listLong = null;
        List<? extends com.sun.jdi.Value> listShort = null;

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

        // Get all array samples of primitive types to set it to the
        // ArrayReference
        try {
            fieldBoolean = refType.fieldByName("z1S");
            fieldByte    = refType.fieldByName("b1S");
            fieldChar    = refType.fieldByName("c1S");
            fieldDouble  = refType.fieldByName("d1S");
            fieldFloat   = refType.fieldByName("f1S");
            fieldInt     = refType.fieldByName("i1S");
            fieldLong    = refType.fieldByName("l1S");
            fieldShort   = refType.fieldByName("r1S");
        } catch (ClassNotPreparedException e) {
            log.complain("debuger FAILURE> Cannot get field by name.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + fieldBoolean);
            log.complain("debuger FAILURE> byte is " + fieldByte);
            log.complain("debuger FAILURE> char is " + fieldChar);
            log.complain("debuger FAILURE> double is " + fieldDouble);
            log.complain("debuger FAILURE> float is " + fieldFloat);
            log.complain("debuger FAILURE> int is " + fieldInt);
            log.complain("debuger FAILURE> long is " + fieldLong);
            log.complain("debuger FAILURE> short is " + fieldShort);
            return 2;
        } catch (ObjectCollectedException e) {
            log.complain("debuger FAILURE> Cannot get field by name.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + fieldBoolean);
            log.complain("debuger FAILURE> byte is " + fieldByte);
            log.complain("debuger FAILURE> char is " + fieldChar);
            log.complain("debuger FAILURE> double is " + fieldDouble);
            log.complain("debuger FAILURE> float is " + fieldFloat);
            log.complain("debuger FAILURE> int is " + fieldInt);
            log.complain("debuger FAILURE> long is " + fieldLong);
            log.complain("debuger FAILURE> short is " + fieldShort);
            return 2;
        }
        log.display("debuger> Got sample fields for primitive types.");
        log.display("debuger> boolean is " + fieldBoolean);
        log.display("debuger> byte is " + fieldByte);
        log.display("debuger> char is " + fieldChar);
        log.display("debuger> double is " + fieldDouble);
        log.display("debuger> float is " + fieldFloat);
        log.display("debuger> int is " + fieldInt);
        log.display("debuger> long is " + fieldLong);
        log.display("debuger> short is " + fieldShort + "\n");

        if ((fieldBoolean == null) || (fieldByte    == null) ||
            (fieldChar    == null) || (fieldDouble  == null) ||
            (fieldFloat   == null) || (fieldInt     == null) ||
            (fieldLong    == null) || (fieldShort   == null)) {
            log.complain("debuger FAILURE> Cannot find field in debuggee.");
            return 2;
        }

        try {
            arrayBoolean = (ArrayReference)refType.getValue(fieldBoolean);
            arrayByte    = (ArrayReference)refType.getValue(fieldByte);
            arrayChar    = (ArrayReference)refType.getValue(fieldChar);
            arrayDouble  = (ArrayReference)refType.getValue(fieldDouble);
            arrayFloat   = (ArrayReference)refType.getValue(fieldFloat);
            arrayInt     = (ArrayReference)refType.getValue(fieldInt);
            arrayLong    = (ArrayReference)refType.getValue(fieldLong);
            arrayShort   = (ArrayReference)refType.getValue(fieldShort);
        } catch (IllegalArgumentException e) {
            log.complain("debuger FAILURE> Cannot get values for fields.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + arrayBoolean);
            log.complain("debuger FAILURE> byte is " + arrayByte);
            log.complain("debuger FAILURE> char is " + arrayChar);
            log.complain("debuger FAILURE> double is " + arrayDouble);
            log.complain("debuger FAILURE> float is " + arrayFloat);
            log.complain("debuger FAILURE> int is " + arrayInt);
            log.complain("debuger FAILURE> long is " + arrayLong);
            log.complain("debuger FAILURE> short is " + arrayShort);
            return 2;
        } catch (ObjectCollectedException e) {
            log.complain("debuger FAILURE> Cannot get values for fields.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + arrayBoolean);
            log.complain("debuger FAILURE> byte is " + arrayByte);
            log.complain("debuger FAILURE> char is " + arrayChar);
            log.complain("debuger FAILURE> double is " + arrayDouble);
            log.complain("debuger FAILURE> float is " + arrayFloat);
            log.complain("debuger FAILURE> int is " + arrayInt);
            log.complain("debuger FAILURE> long is " + arrayLong);
            log.complain("debuger FAILURE> short is " + arrayShort);
            return 2;
        } catch (ClassCastException e) {
            log.complain("debuger FAILURE> Cannot get array reference for "
                       + "fields.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + arrayBoolean);
            log.complain("debuger FAILURE> byte is " + arrayByte);
            log.complain("debuger FAILURE> char is " + arrayChar);
            log.complain("debuger FAILURE> double is " + arrayDouble);
            log.complain("debuger FAILURE> float is " + arrayFloat);
            log.complain("debuger FAILURE> int is " + arrayInt);
            log.complain("debuger FAILURE> long is " + arrayLong);
            log.complain("debuger FAILURE> short is " + arrayShort);
            return 2;
        }
        log.display("debuger> Got sample array references for primitive "
                  + "types.");
        log.display("debuger> boolean is " + arrayBoolean);
        log.display("debuger> byte is " + arrayByte);
        log.display("debuger> char is " + arrayChar);
        log.display("debuger> double is " + arrayDouble);
        log.display("debuger> float is " + arrayFloat);
        log.display("debuger> int is " + arrayInt);
        log.display("debuger> long is " + arrayLong);
        log.display("debuger> short is " + arrayShort + "\n");

        try {
            listBoolean = arrayBoolean.getValues();
            listByte    = arrayByte.getValues();
            listChar    = arrayChar.getValues();
            listDouble  = arrayDouble.getValues();
            listFloat   = arrayFloat.getValues();
            listInt     = arrayInt.getValues();
            listLong    = arrayLong.getValues();
            listShort   = arrayShort.getValues();
        } catch (ObjectCollectedException e) {
            log.complain("debuger FAILURE> Cannot get list of values for "
                       + "fields.");
            log.complain("debuger FAILURE> Exception: " + e);
            log.complain("debuger FAILURE> boolean is " + listBoolean);
            log.complain("debuger FAILURE> byte is " + listByte);
            log.complain("debuger FAILURE> char is " + listChar);
            log.complain("debuger FAILURE> double is " + listDouble);
            log.complain("debuger FAILURE> float is " + listFloat);
            log.complain("debuger FAILURE> int is " + listInt);
            log.complain("debuger FAILURE> long is " + listLong);
            log.complain("debuger FAILURE> short is " + listShort);
            return 2;
        }
        log.display("debuger> Got list of values for primitive types.");
        log.display("debuger> boolean is " + listBoolean);
        log.display("debuger> byte is " + listByte);
        log.display("debuger> char is " + listChar);
        log.display("debuger> double is " + listDouble);
        log.display("debuger> float is " + listFloat);
        log.display("debuger> int is " + listInt);
        log.display("debuger> long is " + listLong);
        log.display("debuger> short is " + listShort + "\n");

        // Check all array fields from debugee
        for (int i = 0; i < FIELD_NAME.length; i++) {
            Field field;
            String name = FIELD_NAME[i][0];
            String type = FIELD_NAME[i][1];
            Value value;
            ArrayReference arrayRef;
            List<? extends com.sun.jdi.Value> listToSet;
            List listRead;

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

            // Prepare list to set
            if (type.equals("boolean")) {
                listToSet = listBoolean;
            } else if (type.equals("byte")) {
                listToSet = listByte;
            } else if (type.equals("char")) {
                listToSet = listChar;
            } else if (type.equals("double")) {
                listToSet = listDouble;
            } else if (type.equals("float")) {
                listToSet = listFloat;
            } else if (type.equals("int")) {
                listToSet = listInt;
            } else if (type.equals("long")) {
                listToSet = listLong;
            } else if (type.equals("short")) {
                listToSet = listShort;
            } else {
                log.complain("debuger FAILURE 4> Unexpected type: " + type);
                testFailed = true;
                continue;
            }

            // Set the sample list of values
            try {
                arrayRef.setValues(listToSet);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 5> Cannot set list of values "
                           + listToSet + " to the field " + name);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            } catch (IndexOutOfBoundsException e) {
                log.complain("debuger FAILURE 5> Cannot set list of values "
                           + listToSet + " to the field " + name);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            } catch (InvalidTypeException e) {
                log.complain("debuger FAILURE 5> Cannot set list of values "
                           + listToSet + " to the field " + name);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            } catch (ClassNotLoadedException e) {
                log.complain("debuger FAILURE 5> Cannot set list of values "
                           + listToSet + " to the field " + name);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            } catch (VMMismatchException e) {
                log.complain("debuger FAILURE 5> Cannot set list of values "
                           + listToSet + " to the field " + name);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field: list of values set "
                      + listToSet);

            // Get the list of Values and check them
            try {
                  listRead = arrayRef.getValues();
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 6> Cannot get values from field "
                           + name);
                log.complain("debuger FAILURE 6> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field: list of values read "
                     + listRead);

            for (int j = 0; j < listRead.size(); j++) {
                if (type.equals("boolean")) {

                    ///////////////////// Check boolean[] /////////////////////
                    BooleanValue boolValue;
                    boolean element;

                    try {
                        boolValue = (BooleanValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE Z1> Cannot cast to "
                                   + "boolean " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("byte")) {

                    ///////////////////// Check byte[] /////////////////////
                    ByteValue byteValue;
                    byte element;

                    try {
                        byteValue = (ByteValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE B1> Cannot cast to "
                                   + "byte " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("char")) {

                    ///////////////////// Check char[] /////////////////////
                    CharValue charValue;
                    char element;

                    try {
                        charValue = (CharValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE C1> Cannot cast to "
                                   + "char " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("int")) {

                    ///////////////////// Check int[] /////////////////////
                    IntegerValue intValue;
                    int element;

                    try {
                        intValue = (IntegerValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE I1> Cannot cast to "
                                   + "int " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("long")) {

                    ///////////////////// Check long[] /////////////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE L1> Cannot cast to "
                                   + "long " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("short")) {

                    ///////////////////// Check short[] /////////////////////
                    ShortValue shortValue;
                    short element;

                    try {
                        shortValue = (ShortValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE R1> Cannot cast to "
                                   + "short " + j + " value of list "
                                   + listRead);
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
                } else if (type.equals("double")) {

                    ///////////////////// Check double[] /////////////////////
                    DoubleValue doubleValue;
                    Double element;

                    try {
                        doubleValue = (DoubleValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE D1> Cannot cast to "
                                   + "double " + j + " value of list "
                                   + listRead);
                        log.complain("debuger FAILURE D1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }

                    element = Double.valueOf(doubleValue.value());
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    if (!element.equals(Double.valueOf(DOUB[j]))) {
                        log.complain("debuger FAILURE D3> " + j + " element of "
                                   + "array " + name + " was expected "
                                   + DOUB[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else if (type.equals("float")) {

                    ///////////////////// Check float[] /////////////////////
                    FloatValue floatValue;
                    Float element;

                    try {
                        floatValue = (FloatValue)listRead.get(j);
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE F1> Cannot cast to "
                                   + "float " + j + " value of list "
                                   + listRead);
                        log.complain("debuger FAILURE F1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }

                    element = Float.valueOf(floatValue.value());
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    if (!element.equals(Float.valueOf(FLOAT[j]))) {
                        log.complain("debuger FAILURE F3> " + j + " element of "
                                   + "array " + name + " was expected "
                                   + FLOAT[j] + ", but returned " + element);
                        testFailed = true;
                        continue;
                    }
                } else {
                    log.complain("debuger FAILURE 6> Unexpected type: " + type);
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
