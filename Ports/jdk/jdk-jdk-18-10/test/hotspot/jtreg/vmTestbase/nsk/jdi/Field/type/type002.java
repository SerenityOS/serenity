/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.Field.type;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class type002 {
    final static int TOTAL_FIELDS = 25;
    // final static String ARRAY_TYPE = "A";
    // Not used yet because of bug
    // 4404195: JDI: spec: Field.type() is undefined for array fields
    final static String CLASS_TYPE = "C";
    final static String INTERFACE_TYPE = "I";

    final static String FIELDS_TYPE_NAME[][] = {
        {"Class", "X0", CLASS_TYPE},
        {"Boolean", "Z0", CLASS_TYPE},
        {"Byte", "B0", CLASS_TYPE},
        {"Character", "C0", CLASS_TYPE},
        {"Double", "D0", CLASS_TYPE},
        {"Float", "F0", CLASS_TYPE},
        {"Integer", "I0", CLASS_TYPE},
        {"Long", "L0", CLASS_TYPE},
        {"String", "S0", CLASS_TYPE},
        {"Object", "O0", CLASS_TYPE},

        {"Long", "LS0", CLASS_TYPE},
        {"Long", "LP0", CLASS_TYPE},
        {"Long", "LU0", CLASS_TYPE},
        {"Long", "LR0", CLASS_TYPE},
        {"Long", "LT0", CLASS_TYPE},
        {"Long", "LV0", CLASS_TYPE},
        {"Long", "LF0", CLASS_TYPE},

        {"Inter", "E0", INTERFACE_TYPE},
        {"Inter", "ES0", INTERFACE_TYPE},
        {"Inter", "EP0", INTERFACE_TYPE},
        {"Inter", "EU0", INTERFACE_TYPE},
        {"Inter", "ER0", INTERFACE_TYPE},
        {"Inter", "ET0", INTERFACE_TYPE},
        {"Inter", "EV0", INTERFACE_TYPE},
        {"Inter", "EF0", INTERFACE_TYPE},
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.Field.type.";
    private final static String className = "type002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToChekName = prefix + "type002aClassToCheck";

    public static void main(String argv[]) {
        System.exit(95 + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.bindToDebugee(debugeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = new IOPipe(debugee);
        boolean testFailed = false;
        List fields;

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

        // Get all fields from debugee
        ReferenceType refType = debugee.classByName(classToChekName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + classToChekName
                      + " not found.");
           return 2;
        }
        log.display("debuger> Class loaded with loader "
                  + refType.classLoader());
        try {
            fields = refType.allFields();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get fields from class");
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalFields = fields.size();
        if (totalFields < 1) {
            log.complain("debuger FAILURE> Total number of fields read "
                       + totalFields + ", should be " + TOTAL_FIELDS);
            return 2;
        }
        log.display("debuger> Total fields found: " + totalFields);
        Iterator fieldsIterator = fields.iterator();

        for (int i = 0; fieldsIterator.hasNext(); i++) {
            Field field = (Field)fieldsIterator.next();
            String name = field.name();
            String realType;
            String realSign;
            Type type;
            ArrayType typeArray;
            ClassType typeClass;
            InterfaceType typeInterface;

            try {
                type = field.type();
            } catch (ClassNotLoadedException e) {
                // All classes are loaded, so ClassNotLoadedException is
                // not expected
                log.complain("debuger FAILURE 1> Can't get type of field "
                           + name);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            if (type == null) {
                log.complain("debuger FAILURE 2> Type is null for " + i
                           + " field " + name);
                testFailed = true;
                continue;
            }
            if (name == null) {
                log.display("debuger FAILURE 3> Name is null for " + i
                          + " field");
                testFailed = true;
                continue;
            }
            try {
                realType = getStatByName(name, 0);
            } catch (Error e) {
                log.complain("debuger FAILURE 4> Unexpected Error " + e);
                return 2;
            }
            try {
                realSign = getStatByName(name, 2);
            } catch (Error e) {
                log.complain("debuger FAILURE 5> Unexpected Error " + e);
                return 2;
            }
            String signature = type.signature();
            log.display("debuger> " + i + " field (" + name + ") of type "
                      + type + " signature " + signature + " read.");

            char signJNI = realSign.charAt(0);
            try {
                switch (signJNI) {
//                    case 'A':
//                        typeArray = (ArrayType)type;
//                        break;
//                        Not used yet because of bug
//         4404195: JDI: spec: Field.type() is undefined for array fields
                    case 'C':
                        typeClass = (ClassType)type;
                        break;
                    case 'I':
                        typeInterface = (InterfaceType)type;
                        break;
                    default:
                        log.complain("debuger FAILURE 6> Error in test. "
                                   + "Unknown JNI signature " + signJNI);
                        return 2;
                }
            } catch (ClassCastException e) {
                log.complain("debuger FAILURE 7> Can't convert field " + name
                           + " to ReferenceType");
                log.complain("debuger FAILURE 7> Exception: " + e);
                testFailed = true;
            }

            if (signature.indexOf(realType) == -1) {
                // field.signature() does not exist in realType
                log.complain("debuger FAILURE 8> Signature of field " + name
                           + " is " + signature + ", but " + "should be "
                           + realType);
                testFailed = true;
            }
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

    private static String getStatByName(String name, int field)
        throws Error
    {
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            String fieldName = FIELDS_TYPE_NAME[i][1];
            if (fieldName.equals(name)) {
                return FIELDS_TYPE_NAME[i][field];
            }
        }
        throw new Error("Error in test. Unknown field " + name);
    }
}
