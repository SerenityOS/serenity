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


package nsk.jdi.Field.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class equals002 {
    final static int TOTAL_FIELDS = 117;
    final static String FIELDS_TYPE_NAME[][] = {
        {"boolean", "z0"},
        {"boolean", "z1"},
        {"boolean", "z2"},
        {"byte", "b0"},
        {"byte", "b1"},
        {"byte", "b2"},
        {"char", "c0"},
        {"char", "c1"},
        {"char", "c2"},
        {"double", "d0"},
        {"double", "d1"},
        {"double", "d2"},
        {"float", "f0"},
        {"float", "f1"},
        {"float", "f2"},
        {"int", "i0"},
        {"int", "i1"},
        {"int", "i2"},
        {"long", "l0"},
        {"long", "l1"},
        {"long", "l2"},

        {"long", "lS0"},
        {"long", "lS1"},
        {"long", "lS2"},
        {"long", "lP0"},
        {"long", "lP1"},
        {"long", "lP2"},
        {"long", "lU0"},
        {"long", "lU1"},
        {"long", "lU2"},
        {"long", "lR0"},
        {"long", "lR1"},
        {"long", "lR2"},
        {"long", "lT0"},
        {"long", "lT1"},
        {"long", "lT2"},
        {"long", "lV0"},
        {"long", "lV1"},
        {"long", "lV2"},
        {"long", "lF0"},
        {"long", "lF1"},
        {"long", "lF2"},

        {"Class", "X0"},
        {"Class", "X1"},
        {"Class", "X2"},
        {"Boolean", "Z0"},
        {"Boolean", "Z1"},
        {"Boolean", "Z2"},
        {"Byte", "B0"},
        {"Byte", "B1"},
        {"Byte", "B2"},
        {"Char", "C0"},
        {"Char", "C1"},
        {"Char", "C2"},
        {"Double", "D0"},
        {"Double", "D1"},
        {"Double", "D2"},
        {"Float", "F0"},
        {"Float", "F1"},
        {"Float", "F2"},
        {"Int", "I0"},
        {"Int", "I1"},
        {"Int", "I2"},
        {"Long", "L0"},
        {"Long", "L1"},
        {"Long", "L2"},
        {"String", "S0"},
        {"String", "S1"},
        {"String", "S2"},
        {"Object", "O0"},
        {"Object", "O1"},
        {"Object", "O2"},

        {"Long", "LS0"},
        {"Long", "LS1"},
        {"Long", "LS2"},
        {"Long", "LP0"},
        {"Long", "LP1"},
        {"Long", "LP2"},
        {"Long", "LU0"},
        {"Long", "LU1"},
        {"Long", "LU2"},
        {"Long", "LR0"},
        {"Long", "LR1"},
        {"Long", "LR2"},
        {"Long", "LT0"},
        {"Long", "LT1"},
        {"Long", "LT2"},
        {"Long", "LV0"},
        {"Long", "LV1"},
        {"Long", "LV2"},
        {"Long", "LF0"},
        {"Long", "LF1"},
        {"Long", "LF2"},

        {"Inter", "E0"},
        {"Inter", "E1"},
        {"Inter", "E2"},
        {"Inter", "ES0"},
        {"Inter", "ES1"},
        {"Inter", "ES2"},
        {"Inter", "EP0"},
        {"Inter", "EP1"},
        {"Inter", "EP2"},
        {"Inter", "EU0"},
        {"Inter", "EU1"},
        {"Inter", "EU2"},
        {"Inter", "ER0"},
        {"Inter", "ER1"},
        {"Inter", "ER2"},
        {"Inter", "ET0"},
        {"Inter", "ET1"},
        {"Inter", "ET2"},
        {"Inter", "EV0"},
        {"Inter", "EV1"},
        {"Inter", "EV2"},
        {"Inter", "EF0"},
        {"Inter", "EF1"},
        {"Inter", "EF2"}
    };
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.equals.";
    private final static String className = "equals002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";

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
        ReferenceType refType = debugee.classByName(debugeeName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + debugeeName
                      + " not found.");
           return 2;
        }
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
            Field srcField = (Field)fieldsIterator.next();
            String name = srcField.name();

            // Compare all fields with each other but srcField
            if (name == null) {
                log.complain("debuger FAILURE 1> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            for (int j = 0; j < TOTAL_FIELDS; j++) {
                String checkFieldName = FIELDS_TYPE_NAME[j][1];
                Field checkField;

                if (!name.equals(checkFieldName)) {
                    try {
                        checkField = refType.fieldByName(checkFieldName);
                    } catch (Exception e) {
                        log.complain("debuger FAILURE 2> Can't get field "
                                   + "by name " + checkFieldName);
                        log.complain("debuger FAILURE 2> Exception: " + e);
                        testFailed = true;
                        continue;
                    }

                    // Compare two different Fields, result should be false
                    boolean fieldsEqual = srcField.equals(checkField);
                    log.display("debuger> Compared fields " + name + " and "
                              + checkFieldName + ", result is " + fieldsEqual);
                    if (fieldsEqual) {
                        // Fields in the same class that mirror different
                        // fields are not equal
                        log.complain("debuger FAILURE 3> Different fields "
                                   + "(" + name + " and " + checkFieldName + ")"
                                   + " are equal. Expected result: not equal.");
                        testFailed = true;
                        continue;
                    }
                }
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
}
