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


package nsk.jdi.Field.isTransient;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class istrans001 {
    final static String IS_TRANS = "transient";
    final static String NOT_TRANS = "";
    final static int TOTAL_FIELDS = 216;
    final static String FIELDS_TYPE_NAME[][] = {
        {"z0", NOT_TRANS},
        {"z1", NOT_TRANS},
        {"z2", NOT_TRANS},
        {"b0", NOT_TRANS},
        {"b1", NOT_TRANS},
        {"b2", NOT_TRANS},
        {"c0", NOT_TRANS},
        {"c1", NOT_TRANS},
        {"c2", NOT_TRANS},
        {"d0", NOT_TRANS},
        {"d1", NOT_TRANS},
        {"d2", NOT_TRANS},
        {"f0", NOT_TRANS},
        {"f1", NOT_TRANS},
        {"f2", NOT_TRANS},
        {"i0", NOT_TRANS},
        {"i1", NOT_TRANS},
        {"i2", NOT_TRANS},
        {"l0", NOT_TRANS},
        {"l1", NOT_TRANS},
        {"l2", NOT_TRANS},

        {"z0T", IS_TRANS},
        {"z1T", IS_TRANS},
        {"z2T", IS_TRANS},
        {"b0T", IS_TRANS},
        {"b1T", IS_TRANS},
        {"b2T", IS_TRANS},
        {"c0T", IS_TRANS},
        {"c1T", IS_TRANS},
        {"c2T", IS_TRANS},
        {"d0T", IS_TRANS},
        {"d1T", IS_TRANS},
        {"d2T", IS_TRANS},
        {"f0T", IS_TRANS},
        {"f1T", IS_TRANS},
        {"f2T", IS_TRANS},
        {"i0T", IS_TRANS},
        {"i1T", IS_TRANS},
        {"i2T", IS_TRANS},
        {"l0T", IS_TRANS},
        {"l1T", IS_TRANS},
        {"l2T", IS_TRANS},

        {"lS0", NOT_TRANS},
        {"lS1", NOT_TRANS},
        {"lS2", NOT_TRANS},
        {"lP0", NOT_TRANS},
        {"lP1", NOT_TRANS},
        {"lP2", NOT_TRANS},
        {"lU0", NOT_TRANS},
        {"lU1", NOT_TRANS},
        {"lU2", NOT_TRANS},
        {"lR0", NOT_TRANS},
        {"lR1", NOT_TRANS},
        {"lR2", NOT_TRANS},
        {"lV0", NOT_TRANS},
        {"lV1", NOT_TRANS},
        {"lV2", NOT_TRANS},
        {"lF0", NOT_TRANS},
        {"lF1", NOT_TRANS},
        {"lF2", NOT_TRANS},

        {"lS0T", IS_TRANS},
        {"lS1T", IS_TRANS},
        {"lS2T", IS_TRANS},
        {"lP0T", IS_TRANS},
        {"lP1T", IS_TRANS},
        {"lP2T", IS_TRANS},
        {"lU0T", IS_TRANS},
        {"lU1T", IS_TRANS},
        {"lU2T", IS_TRANS},
        {"lR0T", IS_TRANS},
        {"lR1T", IS_TRANS},
        {"lR2T", IS_TRANS},
        {"lV0T", IS_TRANS},
        {"lV1T", IS_TRANS},
        {"lV2T", IS_TRANS},
        {"lF0T", IS_TRANS},
        {"lF1T", IS_TRANS},
        {"lF2T", IS_TRANS},

        {"X0", NOT_TRANS},
        {"X1", NOT_TRANS},
        {"X2", NOT_TRANS},
        {"Z0", NOT_TRANS},
        {"Z1", NOT_TRANS},
        {"Z2", NOT_TRANS},
        {"B0", NOT_TRANS},
        {"B1", NOT_TRANS},
        {"B2", NOT_TRANS},
        {"C0", NOT_TRANS},
        {"C1", NOT_TRANS},
        {"C2", NOT_TRANS},
        {"D0", NOT_TRANS},
        {"D1", NOT_TRANS},
        {"D2", NOT_TRANS},
        {"F0", NOT_TRANS},
        {"F1", NOT_TRANS},
        {"F2", NOT_TRANS},
        {"I0", NOT_TRANS},
        {"I1", NOT_TRANS},
        {"I2", NOT_TRANS},
        {"L0", NOT_TRANS},
        {"L1", NOT_TRANS},
        {"L2", NOT_TRANS},
        {"S0", NOT_TRANS},
        {"S1", NOT_TRANS},
        {"S2", NOT_TRANS},
        {"O0", NOT_TRANS},
        {"O1", NOT_TRANS},
        {"O2", NOT_TRANS},

        {"X0T", IS_TRANS},
        {"X1T", IS_TRANS},
        {"X2T", IS_TRANS},
        {"Z0T", IS_TRANS},
        {"Z1T", IS_TRANS},
        {"Z2T", IS_TRANS},
        {"B0T", IS_TRANS},
        {"B1T", IS_TRANS},
        {"B2T", IS_TRANS},
        {"C0T", IS_TRANS},
        {"C1T", IS_TRANS},
        {"C2T", IS_TRANS},
        {"D0T", IS_TRANS},
        {"D1T", IS_TRANS},
        {"D2T", IS_TRANS},
        {"F0T", IS_TRANS},
        {"F1T", IS_TRANS},
        {"F2T", IS_TRANS},
        {"I0T", IS_TRANS},
        {"I1T", IS_TRANS},
        {"I2T", IS_TRANS},
        {"L0T", IS_TRANS},
        {"L1T", IS_TRANS},
        {"L2T", IS_TRANS},
        {"S0T", IS_TRANS},
        {"S1T", IS_TRANS},
        {"S2T", IS_TRANS},
        {"O0T", IS_TRANS},
        {"O1T", IS_TRANS},
        {"O2T", IS_TRANS},

        {"LS0", NOT_TRANS},
        {"LS1", NOT_TRANS},
        {"LS2", NOT_TRANS},
        {"LP0", NOT_TRANS},
        {"LP1", NOT_TRANS},
        {"LP2", NOT_TRANS},
        {"LU0", NOT_TRANS},
        {"LU1", NOT_TRANS},
        {"LU2", NOT_TRANS},
        {"LR0", NOT_TRANS},
        {"LR1", NOT_TRANS},
        {"LR2", NOT_TRANS},
        {"LV0", NOT_TRANS},
        {"LV1", NOT_TRANS},
        {"LV2", NOT_TRANS},
        {"LF0", NOT_TRANS},
        {"LF1", NOT_TRANS},
        {"LF2", NOT_TRANS},

        {"LS0T", IS_TRANS},
        {"LS1T", IS_TRANS},
        {"LS2T", IS_TRANS},
        {"LP0T", IS_TRANS},
        {"LP1T", IS_TRANS},
        {"LP2T", IS_TRANS},
        {"LU0T", IS_TRANS},
        {"LU1T", IS_TRANS},
        {"LU2T", IS_TRANS},
        {"LR0T", IS_TRANS},
        {"LR1T", IS_TRANS},
        {"LR2T", IS_TRANS},
        {"LV0T", IS_TRANS},
        {"LV1T", IS_TRANS},
        {"LV2T", IS_TRANS},
        {"LF0T", IS_TRANS},
        {"LF1T", IS_TRANS},
        {"LF2T", IS_TRANS},

        {"E0", NOT_TRANS},
        {"E1", NOT_TRANS},
        {"E2", NOT_TRANS},

        {"E0T", IS_TRANS},
        {"E1T", IS_TRANS},
        {"E2T", IS_TRANS},

        {"ES0", NOT_TRANS},
        {"ES1", NOT_TRANS},
        {"ES2", NOT_TRANS},
        {"EP0", NOT_TRANS},
        {"EP1", NOT_TRANS},
        {"EP2", NOT_TRANS},
        {"EU0", NOT_TRANS},
        {"EU1", NOT_TRANS},
        {"EU2", NOT_TRANS},
        {"ER0", NOT_TRANS},
        {"ER1", NOT_TRANS},
        {"ER2", NOT_TRANS},
        {"EV0", NOT_TRANS},
        {"EV1", NOT_TRANS},
        {"EV2", NOT_TRANS},
        {"EF0", NOT_TRANS},
        {"EF1", NOT_TRANS},
        {"EF2", NOT_TRANS},

        {"ES0T", IS_TRANS},
        {"ES1T", IS_TRANS},
        {"ES2T", IS_TRANS},
        {"EP0T", IS_TRANS},
        {"EP1T", IS_TRANS},
        {"EP2T", IS_TRANS},
        {"EU0T", IS_TRANS},
        {"EU1T", IS_TRANS},
        {"EU2T", IS_TRANS},
        {"ER0T", IS_TRANS},
        {"ER1T", IS_TRANS},
        {"ER2T", IS_TRANS},
        {"EV0T", IS_TRANS},
        {"EV1T", IS_TRANS},
        {"EV2T", IS_TRANS},
        {"EF0T", IS_TRANS},
        {"EF1T", IS_TRANS},
        {"EF2T", IS_TRANS}
    };
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.isTransient.";
    private final static String className = "istrans001";
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
            boolean isTrans = field.isTransient();
            String name = field.name();
            String realIsTrans;

            if (name == null) {
                log.complain("debuger FAILURE 1> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field (" + name + "), "
                      + "isTransient = " + isTrans + " read.");
            try {
                realIsTrans = getTransByName(name);
            } catch (Error e) {
                log.complain("debuger FAILURE 2> Unexpected Error " + e);
                return 2;
            }
            // isTransient() returns true if the field is transient,
            // returns false if the field is not transient
            if ((isTrans && !realIsTrans.equals(IS_TRANS)) ||
                (!isTrans && realIsTrans.equals(IS_TRANS))
               ) {
                log.complain("debuger FAILURE 3> " + i + " field " + name
                           + ": read field.isTransient() = " + isTrans
                           + " real isTransient should be " + realIsTrans);
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

    private static String getTransByName(String name)
        throws Error
    {
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            String fieldName = FIELDS_TYPE_NAME[i][0];
            if (fieldName.equals(name)) {
                return FIELDS_TYPE_NAME[i][1];
            }
        }
        throw new Error("Error in test. Unknown field " + name);
    }
}
