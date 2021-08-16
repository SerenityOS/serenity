/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.TypeComponent.isStatic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class isstatic001 {
    final static String IS_STATIC = "true";
    final static String NOT_STATIC = "false";
    final static int TOTAL_FIELDS = 174;
    final static String FIELD_NAME[][] = {
        {"z0", NOT_STATIC},
        {"z1", NOT_STATIC},
        {"z2", NOT_STATIC},
        {"b0", NOT_STATIC},
        {"b1", NOT_STATIC},
        {"b2", NOT_STATIC},
        {"c0", NOT_STATIC},
        {"c1", NOT_STATIC},
        {"c2", NOT_STATIC},
        {"d0", NOT_STATIC},
        {"d1", NOT_STATIC},
        {"d2", NOT_STATIC},
        {"f0", NOT_STATIC},
        {"f1", NOT_STATIC},
        {"f2", NOT_STATIC},
        {"i0", NOT_STATIC},
        {"i1", NOT_STATIC},
        {"i2", NOT_STATIC},
        {"l0", NOT_STATIC},
        {"l1", NOT_STATIC},
        {"l2", NOT_STATIC},
        {"r0", NOT_STATIC},
        {"r1", NOT_STATIC},
        {"r2", NOT_STATIC},

        {"z0S", IS_STATIC},
        {"z1S", IS_STATIC},
        {"z2S", IS_STATIC},
        {"b0S", IS_STATIC},
        {"b1S", IS_STATIC},
        {"b2S", IS_STATIC},
        {"c0S", IS_STATIC},
        {"c1S", IS_STATIC},
        {"c2S", IS_STATIC},
        {"d0S", IS_STATIC},
        {"d1S", IS_STATIC},
        {"d2S", IS_STATIC},
        {"f0S", IS_STATIC},
        {"f1S", IS_STATIC},
        {"f2S", IS_STATIC},
        {"i0S", IS_STATIC},
        {"i1S", IS_STATIC},
        {"i2S", IS_STATIC},
        {"l0S", IS_STATIC},
        {"l1S", IS_STATIC},
        {"l2S", IS_STATIC},
        {"r0S", IS_STATIC},
        {"r1S", IS_STATIC},
        {"r2S", IS_STATIC},

        {"lF0", NOT_STATIC},
        {"lF1", NOT_STATIC},
        {"lF2", NOT_STATIC},
        {"lP0", NOT_STATIC},
        {"lP1", NOT_STATIC},
        {"lP2", NOT_STATIC},
        {"lU0", NOT_STATIC},
        {"lU1", NOT_STATIC},
        {"lU2", NOT_STATIC},
        {"lR0", NOT_STATIC},
        {"lR1", NOT_STATIC},
        {"lR2", NOT_STATIC},
        {"lT0", NOT_STATIC},
        {"lT1", NOT_STATIC},
        {"lT2", NOT_STATIC},
        {"lV0", NOT_STATIC},
        {"lV1", NOT_STATIC},
        {"lV2", NOT_STATIC},

        {"lF0S", IS_STATIC},
        {"lF1S", IS_STATIC},
        {"lF2S", IS_STATIC},
        {"lP0S", IS_STATIC},
        {"lP1S", IS_STATIC},
        {"lP2S", IS_STATIC},
        {"lU0S", IS_STATIC},
        {"lU1S", IS_STATIC},
        {"lU2S", IS_STATIC},
        {"lR0S", IS_STATIC},
        {"lR1S", IS_STATIC},
        {"lR2S", IS_STATIC},
        {"lT0S", IS_STATIC},
        {"lT1S", IS_STATIC},
        {"lT2S", IS_STATIC},
        {"lV0S", IS_STATIC},
        {"lV1S", IS_STATIC},
        {"lV2S", IS_STATIC},

        {"X0", NOT_STATIC},
        {"X1", NOT_STATIC},
        {"X2", NOT_STATIC},
        {"O0", NOT_STATIC},
        {"O1", NOT_STATIC},
        {"O2", NOT_STATIC},

        {"X0S", IS_STATIC},
        {"X1S", IS_STATIC},
        {"X2S", IS_STATIC},
        {"O0S", IS_STATIC},
        {"O1S", IS_STATIC},
        {"O2S", IS_STATIC},

        {"LF0", NOT_STATIC},
        {"LF1", NOT_STATIC},
        {"LF2", NOT_STATIC},
        {"LP0", NOT_STATIC},
        {"LP1", NOT_STATIC},
        {"LP2", NOT_STATIC},
        {"LU0", NOT_STATIC},
        {"LU1", NOT_STATIC},
        {"LU2", NOT_STATIC},
        {"LR0", NOT_STATIC},
        {"LR1", NOT_STATIC},
        {"LR2", NOT_STATIC},
        {"LT0", NOT_STATIC},
        {"LT1", NOT_STATIC},
        {"LT2", NOT_STATIC},
        {"LV0", NOT_STATIC},
        {"LV1", NOT_STATIC},
        {"LV2", NOT_STATIC},

        {"LF0S", IS_STATIC},
        {"LF1S", IS_STATIC},
        {"LF2S", IS_STATIC},
        {"LP0S", IS_STATIC},
        {"LP1S", IS_STATIC},
        {"LP2S", IS_STATIC},
        {"LU0S", IS_STATIC},
        {"LU1S", IS_STATIC},
        {"LU2S", IS_STATIC},
        {"LR0S", IS_STATIC},
        {"LR1S", IS_STATIC},
        {"LR2S", IS_STATIC},
        {"LT0S", IS_STATIC},
        {"LT1S", IS_STATIC},
        {"LT2S", IS_STATIC},
        {"LV0S", IS_STATIC},
        {"LV1S", IS_STATIC},
        {"LV2S", IS_STATIC},

        {"E0", NOT_STATIC},
        {"E1", NOT_STATIC},
        {"E2", NOT_STATIC},

        {"E0S", IS_STATIC},
        {"E1S", IS_STATIC},
        {"E2S", IS_STATIC},

        {"EF0", NOT_STATIC},
        {"EF1", NOT_STATIC},
        {"EF2", NOT_STATIC},
        {"EP0", NOT_STATIC},
        {"EP1", NOT_STATIC},
        {"EP2", NOT_STATIC},
        {"EU0", NOT_STATIC},
        {"EU1", NOT_STATIC},
        {"EU2", NOT_STATIC},
        {"ER0", NOT_STATIC},
        {"ER1", NOT_STATIC},
        {"ER2", NOT_STATIC},
        {"ET0", NOT_STATIC},
        {"ET1", NOT_STATIC},
        {"ET2", NOT_STATIC},
        {"EV0", NOT_STATIC},
        {"EV1", NOT_STATIC},
        {"EV2", NOT_STATIC},

        {"EF0S", IS_STATIC},
        {"EF1S", IS_STATIC},
        {"EF2S", IS_STATIC},
        {"EP0S", IS_STATIC},
        {"EP1S", IS_STATIC},
        {"EP2S", IS_STATIC},
        {"EU0S", IS_STATIC},
        {"EU1S", IS_STATIC},
        {"EU2S", IS_STATIC},
        {"ER0S", IS_STATIC},
        {"ER1S", IS_STATIC},
        {"ER2S", IS_STATIC},
        {"ET0S", IS_STATIC},
        {"ET1S", IS_STATIC},
        {"ET2S", IS_STATIC},
        {"EV0S", IS_STATIC},
        {"EV1S", IS_STATIC},
        {"EV2S", IS_STATIC}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isStatic.";
    private final static String className = "isstatic001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "ClassToCheck";

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

        log.complain("debuger> Total fields in debugee read: "
                  + refType.allFields().size() + ", total fields in debuger: "
                  + TOTAL_FIELDS);
        // Check all fields from debugee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isStatic;
            String realIsStatic;

            try {
                field = refType.fieldByName(FIELD_NAME[i][0]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + FIELD_NAME[i][0]);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            name = field.name();
            isStatic = field.isStatic();
            realIsStatic = FIELD_NAME[i][1];
            log.display("debuger> " + i + " field (" + name + "), "
                      + "isStatic = " + isStatic + " read.");

            // isStatic() returns true if this type component is declared
            // static, returns false otherwise
            if ((isStatic && !realIsStatic.equals(IS_STATIC)) ||
                (!isStatic && realIsStatic.equals(IS_STATIC))
               ) {
                log.complain("debuger FAILURE 2> " + i + " field " + name
                          + ": read field.isStatic() = " + isStatic
                          + "; real isStatic should be " + realIsStatic);
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
}
