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


package nsk.jdi.ArrayReference.getValues_ii;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class getvaluesii002 {
    final static String FIELD_NAME[][] = {
        {"z1", "boolean"},
        {"b1", "byte"},
        {"c1", "char"},
        {"i1", "int"},
        {"l1", "long"},
        {"r1", "short"},
        {"d1", "double"},
        {"f1", "float"},

        {"lF1", "long_final"},
        {"lP1", "long_private"},
        {"lU1", "long_public"},
        {"lR1", "long_protected"},
        {"lT1", "long_transient"},
        {"lV1", "long_volatile"}
    };

    static boolean BOOL[] = {true};
    static byte    BYTE[] = {Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
    static char    CHAR[] = {Character.MIN_VALUE, '\u00ff', '\uff00',
                             Character.MAX_VALUE, Character.MAX_VALUE};
    static int     INT[] = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE,
                            Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1};
    static long    LONG[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE,
                             Long.MIN_VALUE + 2, -2, 2, Long.MAX_VALUE - 2};
    static short   SHORT[] = {Short.MIN_VALUE, -1, 0, 0, 1, Short.MAX_VALUE,
                              Short.MAX_VALUE - 1, 1, 1, 0, Short.MIN_VALUE};
    static double  DOUB[] = {Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1, -0,
                             0, 1,  Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                             Double.NaN, Double.NaN, -0, 0, 1};
    static float   FLOAT[] = {Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -1, -0,
                              0, 1, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                              Float.NaN, Float.NaN, 123456, 0, -123456,
                              Float.NaN, Float.NEGATIVE_INFINITY};

    static final     long LONGF[] = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE,
                                     Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE,
                                     Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE,
                                     Long.MIN_VALUE, -1, 0};
    static private   long LONGP[] = {Long.MAX_VALUE, -1, 0, 1, Long.MIN_VALUE,
                                     Long.MAX_VALUE, -1, 0, 1, Long.MIN_VALUE,
                                     Long.MAX_VALUE, -1, 0, 1, Long.MIN_VALUE,
                                     Long.MAX_VALUE, -1, 0, 1, Long.MIN_VALUE};
    static public    long LONGU[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                     -1, -2, -3, -4, -5, -6, -7, -8, -9, -10,
                                   Long.MAX_VALUE, Long.MIN_VALUE};
    static protected long LONGR[] = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10,
                                     1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                     Long.MAX_VALUE, Long.MIN_VALUE, 0, 0};
    static transient long LONGT[] = {Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MIN_VALUE, Long.MIN_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE - 1};
    static volatile  long LONGV[] = {Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MAX_VALUE, Long.MAX_VALUE,
                                     Long.MIN_VALUE};

    private static Log log;
    private final static String prefix = "nsk.jdi.ArrayReference.getValues_ii.";
    private final static String className = "getvaluesii002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "getvaluesii002aClassToCheck";

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

            // Get i + 1 components from index i. Each array has enough
            // elements, so that IndexOutOfBoundsException should never be
            // thrown
            try {
                listOfValues = arrayRef.getValues(i, i + 1);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 4> Cannot get values from field "
                           + name + " from index " + i);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            } catch (IndexOutOfBoundsException e) {
                log.complain("debuger FAILURE 4> Cannot get values from field "
                           + name + " from index " + i);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field read: " + listOfValues);

            // Check each element from the list
            for (int j = 0; j < listOfValues.size(); j++) {
                Value arrayValue;

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
                    if (element != BOOL[j + i]) {
                        log.complain("debuger FAILURE Z2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + BOOL[j + i] + ", but returned "
                                   + element);
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
                    if (element != BYTE[j + i]) {
                        log.complain("debuger FAILURE B2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + BYTE[j + i] + ", but returned "
                                   + element);
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
                    if (element != CHAR[j + i]) {
                        Character c = Character.valueOf('c');
                         Integer  n = Integer.valueOf(0);
                        String sReal = n.toHexString(
                                           c.getNumericValue(CHAR[j + i])
                                       );
                        String sRead = n.toHexString(
                                           c.getNumericValue(element)
                                        );

                        log.complain("debuger FAILURE C2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + sReal + " but returned " + sRead);
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
                    if (!element.equals(Double.valueOf(DOUB[j + i]))) {
                        log.complain("debuger FAILURE D2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + DOUB[j + i] + ", but returned " + element);
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
                    if (!element.equals(Float.valueOf(FLOAT[j + i]))) {
                        log.complain("debuger FAILURE F2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + FLOAT[j + i] + ", but returned "
                                   + element);
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
                    if (element != INT[j + i]) {
                        log.complain("debuger FAILURE I2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + INT[j + i] + ", but returned " + element);
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
                    if (element != LONG[j + i]) {
                        log.complain("debuger FAILURE L2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONG[j + i] + ", but returned "
                                   + element);
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
                    if (element != SHORT[j + i]) {
                        log.complain("debuger FAILURE R2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + SHORT[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_final")) {

                    //////////////////// Check final long[] /////////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LF1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LF1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGF[j + i]) {
                        log.complain("debuger FAILURE LF2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGF[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_private")) {

                    //////////////////// Check private long[] ////////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LP1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LP1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGP[j + i]) {
                        log.complain("debuger FAILURE LP2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGP[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_public")) {

                    //////////////////// Check public long[] ////////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LU1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LU1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGU[j + i]) {
                        log.complain("debuger FAILURE LU2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGU[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_protected")) {

                    ////////////////// Check protected long[] //////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LR1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LR1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGR[j + i]) {
                        log.complain("debuger FAILURE LR2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGR[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_transient")) {

                    ////////////////// Check transient long[] //////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LT1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LT1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGT[j + i]) {
                        log.complain("debuger FAILURE LT2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGT[j + i] + ", but returned "
                                   + element);
                        testFailed = true;
                        continue;
                    }
                } else if (realType.equals("long_volatile")) {

                    ////////////////// Check volatile long[] //////////////
                    LongValue longValue;
                    long element;

                    try {
                        longValue = (LongValue)arrayValue;
                    } catch (ClassCastException e) {
                        log.complain("debuger FAILURE LV1> Cannot cast to "
                                   + "long " + j + " value of field "
                                   + name);
                        log.complain("debuger FAILURE LV1> Exception: " + e);
                        testFailed = true;
                        continue;
                    }
                    element = longValue.value();
                    log.display("debuger> " + i + " field has " + j
                              + " element " + element);

                    // Check element's value
                    if (element != LONGV[j + i]) {
                        log.complain("debuger FAILURE LV2> " + j + " element "
                                   + "of array " + name + " was expected "
                                   + LONGV[j + i] + ", but returned "
                                   + element);
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
