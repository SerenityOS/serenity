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

public class setvalue002 {
    final static int MIN_INDEX = -50;
    final static int MAX_INDEX = 51;
    final static String FIELD_NAME[][] = {
        {"z1", "5", "boolean"},
        {"b1", "5", "byte"},
        {"c1", "6", "char"},
        {"d1", "1", "double"},
        {"f1", "1", "float"},
        {"i1", "10", "int"},
        {"l1", "2", "long"},
        {"r1", "5", "short"},

        {"lF1", "0", "long"},
        {"lP1", "2", "long"},
        {"lU1", "3", "long"},
        {"lR1", "4", "long"},
        {"lT1", "5", "long"},
        {"lV1", "6", "long"}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.ArrayReference.setValue.";
    private final static String className = "setvalue002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "setvalue002aClassToCheck";

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
        Value valueBoolean = null;
        Value valueByte = null;
        Value valueChar = null;
        Value valueDouble = null;
        Value valueFloat = null;
        Value valueInt = null;
        Value valueLong = null;
        Value valueShort = null;

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

        // Get all samples of primitive types Values to set it to the
        // ArrayReference
        try {
            fieldBoolean = refType.fieldByName("z");
            fieldByte    = refType.fieldByName("b");
            fieldChar    = refType.fieldByName("c");
            fieldDouble  = refType.fieldByName("d");
            fieldFloat   = refType.fieldByName("f");
            fieldInt     = refType.fieldByName("i");
            fieldLong    = refType.fieldByName("l");
            fieldShort   = refType.fieldByName("r");
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
            valueBoolean = refType.getValue(fieldBoolean);
            valueByte    = refType.getValue(fieldByte);
            valueChar    = refType.getValue(fieldChar);
            valueDouble  = refType.getValue(fieldDouble);
            valueFloat   = refType.getValue(fieldFloat);
            valueInt     = refType.getValue(fieldInt);
            valueLong    = refType.getValue(fieldLong);
            valueShort   = refType.getValue(fieldShort);
        } catch (IllegalArgumentException e) {
            log.complain("debuger FAILURE> Cannot get values for fields.");
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
            log.complain("debuger FAILURE> Cannot get values for fields.");
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
        log.display("debuger> Got sample values for primitive types.");
        log.display("debuger> boolean is " + valueBoolean);
        log.display("debuger> byte is " + valueByte);
        log.display("debuger> char is " + valueChar);
        log.display("debuger> double is " + valueDouble);
        log.display("debuger> float is " + valueFloat);
        log.display("debuger> int is " + valueInt);
        log.display("debuger> long is " + valueLong);
        log.display("debuger> short is " + valueShort + "\n");

        // Check all array fields from debugee
        for (int i = 0; i < FIELD_NAME.length; i++) {
            Field field;
            String name = FIELD_NAME[i][0];
            Integer totalElements = Integer.valueOf(FIELD_NAME[i][1]);
            String type = FIELD_NAME[i][2];
            int lastElementIndex = totalElements.intValue() - 1;
            Value value;
            ArrayReference arrayRef;
            Value valueToSet;

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

            // Prepare Value to set
            if (type.equals("boolean")) {
                valueToSet = (Value)valueBoolean;
            } else if (type.equals("byte")) {
                valueToSet = (Value)valueByte;
            } else if (type.equals("char")) {
                valueToSet = (Value)valueChar;
            } else if (type.equals("double")) {
                valueToSet = (Value)valueDouble;
            } else if (type.equals("float")) {
                valueToSet = (Value)valueFloat;
            } else if (type.equals("int")) {
                valueToSet = (Value)valueInt;
            } else if (type.equals("long")) {
                valueToSet = (Value)valueLong;
            } else if (type.equals("short")) {
                valueToSet = (Value)valueShort;
            } else {
                log.complain("debuger FAILURE 4> Unexpected type: " + type);
                testFailed = true;
                continue;
            }

            // Try to set value by index from MIN_INDEX to -1 and from
            // arrayRef.length() to MAX_INDEX
            for (int j = MIN_INDEX; j < MAX_INDEX; j++) {
                if ( (j < 0) || (j > lastElementIndex) ) {

                    // Set the Value, IndexOutOfBoundsException is expected
                    try {
                        arrayRef.setValue(j, valueToSet);
                        log.complain("debuger FAILURE 5> IndexOutOfBoundsException"
                                   + "is not thrown. ");
                        testFailed = true;
                    } catch (ObjectCollectedException e) {
                        log.complain("debuger FAILURE 5> Cannot set value "
                                   + valueToSet + " to " + j + " index of "
                                   + "field " + name);
                        log.complain("debuger FAILURE 5> Exception: " + e);
                        testFailed = true;
                    } catch (IndexOutOfBoundsException e) {
                        log.display("debuger> " + i + " field: cannot set "
                                  + "value " + valueToSet + " with index "
                                  + j + ". Expected exception: " + e);
                    } catch (InvalidTypeException e) {
                        log.complain("debuger FAILURE 5> Cannot set value "
                                   + valueToSet + " to " + j + " index of "
                                   + "field " + name);
                        log.complain("debuger FAILURE 5> Exception: " + e);
                        testFailed = true;
                    } catch (ClassNotLoadedException e) {
                        log.complain("debuger FAILURE 5> Cannot set value "
                                   + valueToSet + " to " + j + " index of "
                                   + "field " + name);
                        log.complain("debuger FAILURE 5> Exception: " + e);
                        testFailed = true;
                    } catch (VMMismatchException e) {
                        log.complain("debuger FAILURE 5> Cannot set value "
                                   + valueToSet + " to " + j + " index of "
                                   + "field " + name);
                        log.complain("debuger FAILURE 5> Exception: " + e);
                        testFailed = true;
                    }
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
