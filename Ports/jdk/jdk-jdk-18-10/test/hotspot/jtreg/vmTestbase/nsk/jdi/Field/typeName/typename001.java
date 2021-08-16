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


package nsk.jdi.Field.typeName;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class typename001 {
    final static String PRIM_CODE = "primitive";
    final static String REF_CODE = "reference";
    final static int TOTAL_FIELDS = 117;
    final static String FIELDS_TYPE_NAME[][] = {
        {"boolean", "z0", PRIM_CODE},
        {"boolean", "z1", REF_CODE},
        {"boolean", "z2", REF_CODE},
        {"byte", "b0", PRIM_CODE},
        {"byte", "b1", REF_CODE},
        {"byte", "b2", REF_CODE},
        {"char", "c0", PRIM_CODE},
        {"char", "c1", REF_CODE},
        {"char", "c2", REF_CODE},
        {"double", "d0", PRIM_CODE},
        {"double", "d1", REF_CODE},
        {"double", "d2", REF_CODE},
        {"float", "f0", PRIM_CODE},
        {"float", "f1", REF_CODE},
        {"float", "f2", REF_CODE},
        {"int", "i0", PRIM_CODE},
        {"int", "i1", REF_CODE},
        {"int", "i2", REF_CODE},
        {"long", "l0", PRIM_CODE},
        {"long", "l1", REF_CODE},
        {"long", "l2", REF_CODE},

        {"long", "lS0", PRIM_CODE},
        {"long", "lS1", REF_CODE},
        {"long", "lS2", REF_CODE},
        {"long", "lP0", PRIM_CODE},
        {"long", "lP1", REF_CODE},
        {"long", "lP2", REF_CODE},
        {"long", "lU0", PRIM_CODE},
        {"long", "lU1", REF_CODE},
        {"long", "lU2", REF_CODE},
        {"long", "lR0", PRIM_CODE},
        {"long", "lR1", REF_CODE},
        {"long", "lR2", REF_CODE},
        {"long", "lT0", PRIM_CODE},
        {"long", "lT1", REF_CODE},
        {"long", "lT2", REF_CODE},
        {"long", "lV0", PRIM_CODE},
        {"long", "lV1", REF_CODE},
        {"long", "lV2", REF_CODE},
        {"long", "lF0", PRIM_CODE},
        {"long", "lF1", REF_CODE},
        {"long", "lF2", REF_CODE},

        {"Class", "X0", REF_CODE},
        {"Class", "X1", REF_CODE},
        {"Class", "X2", REF_CODE},
        {"Boolean", "Z0", REF_CODE},
        {"Boolean", "Z1", REF_CODE},
        {"Boolean", "Z2", REF_CODE},
        {"Byte", "B0", REF_CODE},
        {"Byte", "B1", REF_CODE},
        {"Byte", "B2", REF_CODE},
        {"Char", "C0", REF_CODE},
        {"Char", "C1", REF_CODE},
        {"Char", "C2", REF_CODE},
        {"Double", "D0", REF_CODE},
        {"Double", "D1", REF_CODE},
        {"Double", "D2", REF_CODE},
        {"Float", "F0", REF_CODE},
        {"Float", "F1", REF_CODE},
        {"Float", "F2", REF_CODE},
        {"Int", "I0", REF_CODE},
        {"Int", "I1", REF_CODE},
        {"Int", "I2", REF_CODE},
        {"Long", "L0", REF_CODE},
        {"Long", "L1", REF_CODE},
        {"Long", "L2", REF_CODE},
        {"String", "S0", REF_CODE},
        {"String", "S1", REF_CODE},
        {"String", "S2", REF_CODE},
        {"Object", "O0", REF_CODE},
        {"Object", "O1", REF_CODE},
        {"Object", "O2", REF_CODE},

        {"Long", "LS0", REF_CODE},
        {"Long", "LS1", REF_CODE},
        {"Long", "LS2", REF_CODE},
        {"Long", "LP0", REF_CODE},
        {"Long", "LP1", REF_CODE},
        {"Long", "LP2", REF_CODE},
        {"Long", "LU0", REF_CODE},
        {"Long", "LU1", REF_CODE},
        {"Long", "LU2", REF_CODE},
        {"Long", "LR0", REF_CODE},
        {"Long", "LR1", REF_CODE},
        {"Long", "LR2", REF_CODE},
        {"Long", "LT0", REF_CODE},
        {"Long", "LT1", REF_CODE},
        {"Long", "LT2", REF_CODE},
        {"Long", "LV0", REF_CODE},
        {"Long", "LV1", REF_CODE},
        {"Long", "LV2", REF_CODE},
        {"Long", "LF0", REF_CODE},
        {"Long", "LF1", REF_CODE},
        {"Long", "LF2", REF_CODE},

        {"Inter", "E0", REF_CODE},
        {"Inter", "E1", REF_CODE},
        {"Inter", "E2", REF_CODE},
        {"Inter", "ES0", REF_CODE},
        {"Inter", "ES1", REF_CODE},
        {"Inter", "ES2", REF_CODE},
        {"Inter", "EP0", REF_CODE},
        {"Inter", "EP1", REF_CODE},
        {"Inter", "EP2", REF_CODE},
        {"Inter", "EU0", REF_CODE},
        {"Inter", "EU1", REF_CODE},
        {"Inter", "EU2", REF_CODE},
        {"Inter", "ER0", REF_CODE},
        {"Inter", "ER1", REF_CODE},
        {"Inter", "ER2", REF_CODE},
        {"Inter", "ET0", REF_CODE},
        {"Inter", "ET1", REF_CODE},
        {"Inter", "ET2", REF_CODE},
        {"Inter", "EV0", REF_CODE},
        {"Inter", "EV1", REF_CODE},
        {"Inter", "EV2", REF_CODE},
        {"Inter", "EF0", REF_CODE},
        {"Inter", "EF1", REF_CODE},
        {"Inter", "EF2", REF_CODE}
    };
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.typeName.";
    private final static String className = "typename001";
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
            Field field = (Field)fieldsIterator.next();
            String typeName = field.typeName();
            String name = field.name();
            String nameStat;

            if (typeName == null) {
                log.complain("debuger FAILURE 1> typeName is null for "
                           + i + " field " + name);
                testFailed = true;
                continue;
            }
            if (name == null) {
                log.complain("debuger FAILURE 2> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field (" + name + ") of " + "type "
                      + typeName + " read.");
            try {
                nameStat = getStatByName(name, 2);
            } catch (Error e) {
                log.complain("debuger FAILURE 3> Unexpected Error " + e);
                return 2;
            }
            if (nameStat.equals(REF_CODE)) {
                // ReferenceType - name of the type should exist in
                // field.typeName() string
                String realTypeName;
                try {
                    realTypeName = getStatByName(name, 0);
                } catch (Error e) {
                    log.complain("debuger FAILURE 4> Unexpected Error " + e);
                    return 2;
                }
                log.display("debuger> ReferenceType = " + name + "; "
                          + "typeName = " + typeName + "; "
                          + "realTypeName = " + realTypeName);
                if (typeName.indexOf(realTypeName) == -1) {
                    log.complain("debuger FAILURE> Type of field " + name
                               + " is " + typeName + ", but " + "should be "
                               + realTypeName);
                    testFailed = true;
                }
            } else {
                // PrimitiveType - name of the type should not be empty
                if (typeName.length() == 0) {
                    log.complain("debuger FAILURE> Empty typeName for " + i
                               + " field " + name);
                    testFailed = true;
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
