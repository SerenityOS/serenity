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


package nsk.jdi.ArrayReference.setValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class setvalue001 {
    final static int HALF = 9;
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
    private final static String prefix = "nsk.jdi.ArrayReference.setValue.";
    private final static String className = "setvalue001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "setvalue001aClassToCheck";

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
            ArrayReference arrayRef;

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

            for (int j = 0;  j < HALF; j++) {
                int indexSample = j + HALF;
                 Value sample;
                 Value valueNew;

                // Get Value from sample-area (9..17 elements)
                try {
                      sample = arrayRef.getValue(indexSample);
                } catch (ObjectCollectedException e) {
                    log.complain("debuger FAILURE 4> Cannot get " + indexSample
                               + " value from field " + name);
                    log.complain("debuger FAILURE 4> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (IndexOutOfBoundsException e) {
                    log.complain("debuger FAILURE 4> Cannot get " + indexSample
                               + " value from field " + name);
                    log.complain("debuger FAILURE 4> Exception: " + e);
                    testFailed = true;
                    continue;
                }
                log.display("debuger> " + i + " field: sample from index "
                          + indexSample  + " is " + sample);

                // Set the Sample to the correspondent index
                // Any exception means that the test fails
                try {
                      arrayRef.setValue(j, sample);
                } catch (ObjectCollectedException e) {
                    log.complain("debuger FAILURE 5> Cannot set value " + sample
                               + " to " + j + " index of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (IndexOutOfBoundsException e) {
                    log.complain("debuger FAILURE 5> Cannot set value " + sample
                               + " to " + j + " index of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (InvalidTypeException e) {
                    log.complain("debuger FAILURE 5> Cannot set value " + sample
                               + " to " + j + " index of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (ClassNotLoadedException e) {
                    log.complain("debuger FAILURE 5> Cannot set value " + sample
                               + " to " + j + " index of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (VMMismatchException e) {
                    log.complain("debuger FAILURE 5> Cannot set value " + sample
                               + " to " + j + " index of field " + name);
                    log.complain("debuger FAILURE 5> Exception: " + e);
                    testFailed = true;
                    continue;
                }
                log.display("debuger> " + i + " field: sample " + sample
                          + " has been set to index " + j);


                // Get the Value from the correspondent index
                try {
                      valueNew = arrayRef.getValue(j);
                } catch (ObjectCollectedException e) {
                    log.complain("debuger FAILURE 6> Cannot get " + j + " value"
                               + " from field " + name);
                    log.complain("debuger FAILURE 6> Exception: " + e);
                    testFailed = true;
                    continue;
                } catch (IndexOutOfBoundsException e) {
                    log.complain("debuger FAILURE 6> Cannot get " + j + " value"
                               + " from field " + name);
                    log.complain("debuger FAILURE 6> Exception: " + e);
                    testFailed = true;
                    continue;
                }
                log.display("debuger> " + i + " field: element from index " + j
                          + " is " + valueNew);

                // Check the Value that has been read with the sample
                if (realType.equals("boolean")) {

                    ///////////////////// Check boolean[] /////////////////////
                    BooleanValue boolValue;
                    boolean boolSample;
                    boolean boolNew;

                    // Cast sample and read Value to primitive type
                    try {
                        boolValue = (BooleanValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE Z1> Cannot cast to "
                                   + "boolean sample.");
                        log.complain("debuger FAILURE Z1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    boolSample = boolValue.value();
                    try {
                        boolValue = (BooleanValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE Z2> Cannot cast to "
                                   + "boolean read value.");
                        log.complain("debuger FAILURE Z2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    boolNew = boolValue.value();

                    // Check two primitive values
                    if (boolNew != boolSample) {
                        log.complain("debuger FAILURE Z3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + boolSample + ", but returned " + boolNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + boolSample + ", primitive "
                                  + " read value is " + boolNew);
                    }
                } else if (realType.equals("byte")) {

                    ///////////////////// Check byte[] /////////////////////
                    ByteValue byteValue;
                    byte byteSample;
                    byte byteNew;

                    // Cast sample and read Value to primitive type
                    try {
                        byteValue = (ByteValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE B1> Cannot cast to "
                                   + "byte sample.");
                        log.complain("debuger FAILURE B1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    byteSample = byteValue.value();
                    try {
                        byteValue = (ByteValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE B2> Cannot cast to "
                                   + "byte read value.");
                        log.complain("debuger FAILURE B2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    byteNew = byteValue.value();

                    // Check two primitive values
                    if (byteNew != byteSample) {
                        log.complain("debuger FAILURE B3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + byteSample + ", but returned " + byteNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + byteSample + ", primitive "
                                  + "read value is " + byteNew);
                    }
                } else if (realType.equals("char")) {

                    ///////////////////// Check char[] /////////////////////
                    CharValue charValue;
                    char charSample;
                    char charNew;

                    // Cast sample and read Value to primitive type
                    try {
                        charValue = (CharValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE C1> Cannot cast to "
                                   + "char sample.");
                        log.complain("debuger FAILURE C1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    charSample = charValue.value();
                    try {
                        charValue = (CharValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE C2> Cannot cast to "
                                   + "char read value.");
                        log.complain("debuger FAILURE C2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    charNew = charValue.value();

                    // Check two primitive values
                    if (charNew != charSample) {
                        log.complain("debuger FAILURE C3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + charSample + ", but returned " + charNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + charSample + ", primitive "
                                  + "read value is " + charNew);
                    }
                } else if (realType.equals("int")) {

                    ///////////////////// Check int[] /////////////////////
                    IntegerValue intValue;
                    int intSample;
                    int intNew;

                    // Cast sample and read Value to primitive type
                    try {
                        intValue = (IntegerValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE I1> Cannot cast to "
                                   + "int sample.");
                        log.complain("debuger FAILURE I1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    intSample = intValue.value();
                    try {
                        intValue = (IntegerValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE I2> Cannot cast to "
                                   + "int read value.");
                        log.complain("debuger FAILURE I2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    intNew = intValue.value();

                    // Check two primitive values
                    if (intNew != intSample) {
                        log.complain("debuger FAILURE I3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + intSample + ", but returned " + intNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + intSample + ", primitive "
                                  + "read value is " + intNew);
                    }
                } else if (realType.equals("long")) {

                    ///////////////////// Check long[] /////////////////////
                    LongValue longValue;
                    long longSample;
                    long longNew;

                    // Cast sample and read Value to primitive type
                    try {
                        longValue = (LongValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE L1> Cannot cast to "
                                   + "long sample.");
                        log.complain("debuger FAILURE L1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    longSample = longValue.value();
                    try {
                        longValue = (LongValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE L2> Cannot cast to "
                                   + "long read value.");
                        log.complain("debuger FAILURE L2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    longNew = longValue.value();

                    // Check two primitive values
                    if (longNew != longSample) {
                        log.complain("debuger FAILURE L3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + longSample + ", but returned " + longNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + longSample + ", primitive "
                                  + "read value is " + longNew);
                    }
                } else if (realType.equals("short")) {

                    ///////////////////// Check short[] /////////////////////
                    ShortValue shortValue;
                    short shortSample;
                    short shortNew;

                    // Cast sample and read Value to primitive type
                    try {
                        shortValue = (ShortValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE R1> Cannot cast to "
                                   + "short sample.");
                        log.complain("debuger FAILURE R1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    shortSample = shortValue.value();
                    try {
                        shortValue = (ShortValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE R2> Cannot cast to "
                                   + "short read value.");
                        log.complain("debuger FAILURE R2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    shortNew = shortValue.value();

                    // Check two primitive values
                    if (shortNew != shortSample) {
                        log.complain("debuger FAILURE R3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + shortSample + ", but returned "
                                   + shortNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + shortSample + ", primitive "
                                  + "read value is " + shortNew);
                    }
                } else if (realType.equals("double")) {

                    ///////////////////// Check double[] /////////////////////
                    DoubleValue dblValue;
                    Double dblSample;
                    Double dblNew;

                    // Cast sample and read Value to primitive type
                    try {
                        dblValue = (DoubleValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE D1> Cannot cast to "
                                   + "double sample.");
                        log.complain("debuger FAILURE D1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    dblSample = Double.valueOf(dblValue.value());
                    try {
                        dblValue = (DoubleValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE D2> Cannot cast to "
                                   + "double read value.");
                        log.complain("debuger FAILURE D2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    dblNew = Double.valueOf(dblValue.value());

                    // Check two primitive values
                    if (!dblNew.equals(dblSample)) {
                        log.complain("debuger FAILURE D3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + dblSample + ", but returned " + dblNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + dblSample + ", primitive "
                                  + "read value is " + dblNew);
                    }
                } else if (realType.equals("float")) {

                    ///////////////////// Check float[] /////////////////////
                    FloatValue fltValue;
                    Float fltSample;
                    Float fltNew;

                    // Cast sample and read Value to primitive type
                    try {
                        fltValue = (FloatValue)sample;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE F1> Cannot cast to "
                                   + "float sample.");
                        log.complain("debuger FAILURE F1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    fltSample = Float.valueOf(fltValue.value());
                    try {
                        fltValue = (FloatValue)valueNew;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE F2> Cannot cast to "
                                   + "float read value.");
                        log.complain("debuger FAILURE F2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    fltNew = Float.valueOf(fltValue.value());

                    // Check two primitive values
                    if (!fltNew.equals(fltSample)) {
                        log.complain("debuger FAILURE F3> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + fltSample + ", but returned " + fltNew);
                        testFailed = true;
                    } else {
                        log.display("debuger> " + i + " field: primitive "
                                  + "sample is " + fltSample + ", primitive "
                                  + "read value is " + fltNew);
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
