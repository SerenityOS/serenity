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


package nsk.jdi.Field.isVolatile;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class isvol001 {
    final static String IS_VOL = "volatile";
    final static String NOT_VOL = "";
    final static int TOTAL_FIELDS = 207;
    final static String FIELDS_TYPE_NAME[][] = {
        {"z0", NOT_VOL},
        {"z1", NOT_VOL},
        {"z2", NOT_VOL},
        {"b0", NOT_VOL},
        {"b1", NOT_VOL},
        {"b2", NOT_VOL},
        {"c0", NOT_VOL},
        {"c1", NOT_VOL},
        {"c2", NOT_VOL},
        {"d0", NOT_VOL},
        {"d1", NOT_VOL},
        {"d2", NOT_VOL},
        {"f0", NOT_VOL},
        {"f1", NOT_VOL},
        {"f2", NOT_VOL},
        {"i0", NOT_VOL},
        {"i1", NOT_VOL},
        {"i2", NOT_VOL},
        {"l0", NOT_VOL},
        {"l1", NOT_VOL},
        {"l2", NOT_VOL},

        {"z0V", IS_VOL},
        {"z1V", IS_VOL},
        {"z2V", IS_VOL},
        {"b0V", IS_VOL},
        {"b1V", IS_VOL},
        {"b2V", IS_VOL},
        {"c0V", IS_VOL},
        {"c1V", IS_VOL},
        {"c2V", IS_VOL},
        {"d0V", IS_VOL},
        {"d1V", IS_VOL},
        {"d2V", IS_VOL},
        {"f0V", IS_VOL},
        {"f1V", IS_VOL},
        {"f2V", IS_VOL},
        {"i0V", IS_VOL},
        {"i1V", IS_VOL},
        {"i2V", IS_VOL},
        {"l0V", IS_VOL},
        {"l1V", IS_VOL},
        {"l2V", IS_VOL},

        {"lS0", NOT_VOL},
        {"lS1", NOT_VOL},
        {"lS2", NOT_VOL},
        {"lP0", NOT_VOL},
        {"lP1", NOT_VOL},
        {"lP2", NOT_VOL},
        {"lU0", NOT_VOL},
        {"lU1", NOT_VOL},
        {"lU2", NOT_VOL},
        {"lR0", NOT_VOL},
        {"lR1", NOT_VOL},
        {"lR2", NOT_VOL},
        {"lT0", NOT_VOL},
        {"lT1", NOT_VOL},
        {"lT2", NOT_VOL},
        {"lF0", NOT_VOL},
        {"lF1", NOT_VOL},
        {"lF2", NOT_VOL},

        {"lS0V", IS_VOL},
        {"lS1V", IS_VOL},
        {"lS2V", IS_VOL},
        {"lP0V", IS_VOL},
        {"lP1V", IS_VOL},
        {"lP2V", IS_VOL},
        {"lU0V", IS_VOL},
        {"lU1V", IS_VOL},
        {"lU2V", IS_VOL},
        {"lR0V", IS_VOL},
        {"lR1V", IS_VOL},
        {"lR2V", IS_VOL},
        {"lT0V", IS_VOL},
        {"lT1V", IS_VOL},
        {"lT2V", IS_VOL},

        {"X0", NOT_VOL},
        {"X1", NOT_VOL},
        {"X2", NOT_VOL},
        {"Z0", NOT_VOL},
        {"Z1", NOT_VOL},
        {"Z2", NOT_VOL},
        {"B0", NOT_VOL},
        {"B1", NOT_VOL},
        {"B2", NOT_VOL},
        {"C0", NOT_VOL},
        {"C1", NOT_VOL},
        {"C2", NOT_VOL},
        {"D0", NOT_VOL},
        {"D1", NOT_VOL},
        {"D2", NOT_VOL},
        {"F0", NOT_VOL},
        {"F1", NOT_VOL},
        {"F2", NOT_VOL},
        {"I0", NOT_VOL},
        {"I1", NOT_VOL},
        {"I2", NOT_VOL},
        {"L0", NOT_VOL},
        {"L1", NOT_VOL},
        {"L2", NOT_VOL},
        {"S0", NOT_VOL},
        {"S1", NOT_VOL},
        {"S2", NOT_VOL},
        {"O0", NOT_VOL},
        {"O1", NOT_VOL},
        {"O2", NOT_VOL},

        {"X0V", IS_VOL},
        {"X1V", IS_VOL},
        {"X2V", IS_VOL},
        {"Z0V", IS_VOL},
        {"Z1V", IS_VOL},
        {"Z2V", IS_VOL},
        {"B0V", IS_VOL},
        {"B1V", IS_VOL},
        {"B2V", IS_VOL},
        {"C0V", IS_VOL},
        {"C1V", IS_VOL},
        {"C2V", IS_VOL},
        {"D0V", IS_VOL},
        {"D1V", IS_VOL},
        {"D2V", IS_VOL},
        {"F0V", IS_VOL},
        {"F1V", IS_VOL},
        {"F2V", IS_VOL},
        {"I0V", IS_VOL},
        {"I1V", IS_VOL},
        {"I2V", IS_VOL},
        {"L0V", IS_VOL},
        {"L1V", IS_VOL},
        {"L2V", IS_VOL},
        {"S0V", IS_VOL},
        {"S1V", IS_VOL},
        {"S2V", IS_VOL},
        {"O0V", IS_VOL},
        {"O1V", IS_VOL},
        {"O2V", IS_VOL},

        {"LS0", NOT_VOL},
        {"LS1", NOT_VOL},
        {"LS2", NOT_VOL},
        {"LP0", NOT_VOL},
        {"LP1", NOT_VOL},
        {"LP2", NOT_VOL},
        {"LU0", NOT_VOL},
        {"LU1", NOT_VOL},
        {"LU2", NOT_VOL},
        {"LR0", NOT_VOL},
        {"LR1", NOT_VOL},
        {"LR2", NOT_VOL},
        {"LT0", NOT_VOL},
        {"LT1", NOT_VOL},
        {"LT2", NOT_VOL},
        {"LF0", NOT_VOL},
        {"LF1", NOT_VOL},
        {"LF2", NOT_VOL},

        {"LS0V", IS_VOL},
        {"LS1V", IS_VOL},
        {"LS2V", IS_VOL},
        {"LP0V", IS_VOL},
        {"LP1V", IS_VOL},
        {"LP2V", IS_VOL},
        {"LU0V", IS_VOL},
        {"LU1V", IS_VOL},
        {"LU2V", IS_VOL},
        {"LR0V", IS_VOL},
        {"LR1V", IS_VOL},
        {"LR2V", IS_VOL},
        {"LT0V", IS_VOL},
        {"LT1V", IS_VOL},
        {"LT2V", IS_VOL},

        {"E0", NOT_VOL},
        {"E1", NOT_VOL},
        {"E2", NOT_VOL},

        {"E0V", IS_VOL},
        {"E1V", IS_VOL},
        {"E2V", IS_VOL},

        {"ES0", NOT_VOL},
        {"ES1", NOT_VOL},
        {"ES2", NOT_VOL},
        {"EP0", NOT_VOL},
        {"EP1", NOT_VOL},
        {"EP2", NOT_VOL},
        {"EU0", NOT_VOL},
        {"EU1", NOT_VOL},
        {"EU2", NOT_VOL},
        {"ER0", NOT_VOL},
        {"ER1", NOT_VOL},
        {"ER2", NOT_VOL},
        {"ET0", NOT_VOL},
        {"ET1", NOT_VOL},
        {"ET2", NOT_VOL},
        {"EF0", NOT_VOL},
        {"EF1", NOT_VOL},
        {"EF2", NOT_VOL},

        {"ES0V", IS_VOL},
        {"ES1V", IS_VOL},
        {"ES2V", IS_VOL},
        {"EP0V", IS_VOL},
        {"EP1V", IS_VOL},
        {"EP2V", IS_VOL},
        {"EU0V", IS_VOL},
        {"EU1V", IS_VOL},
        {"EU2V", IS_VOL},
        {"ER0V", IS_VOL},
        {"ER1V", IS_VOL},
        {"ER2V", IS_VOL},
        {"ET0V", IS_VOL},
        {"ET1V", IS_VOL},
        {"ET2V", IS_VOL}
    };
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.isVolatile.";
    private final static String className = "isvol001";
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
            boolean isVol = field.isVolatile();
            String name = field.name();
            String realIsVol;

            if (name == null) {
                log.complain("debuger FAILURE 1> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field (" + name + "), "
                      + "isVolatile = " + isVol + " read.");
            try {
                realIsVol = getVolByName(name);
            } catch (Error e) {
                log.complain("debuger FAILURE 2> Unexpected Error " + e);
                return 2;
            }
            // isVolatile() returns true if the field is volatile,
            // returns false if the field is not volatile
            if ((isVol && !realIsVol.equals(IS_VOL)) ||
                (!isVol && realIsVol.equals(IS_VOL))
               ) {
                log.complain("debuger FAILURE> " + i + " field " + name
                           + ": read field.isVolatile() = " + isVol
                           + " real isVolatile should be " + realIsVol);
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

    private static String getVolByName(String name)
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
