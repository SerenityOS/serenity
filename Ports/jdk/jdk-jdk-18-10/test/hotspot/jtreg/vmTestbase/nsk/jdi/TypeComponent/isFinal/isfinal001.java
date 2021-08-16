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


package nsk.jdi.TypeComponent.isFinal;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class isfinal001 {
    final static String IS_FINAL = "true";
    final static String NOT_FINAL = "false";
    final static int TOTAL_FIELDS = 165;
    final static String FIELD_NAME[][] = {
        {"z0", NOT_FINAL},
        {"z1", NOT_FINAL},
        {"z2", NOT_FINAL},
        {"b0", NOT_FINAL},
        {"b1", NOT_FINAL},
        {"b2", NOT_FINAL},
        {"c0", NOT_FINAL},
        {"c1", NOT_FINAL},
        {"c2", NOT_FINAL},
        {"d0", NOT_FINAL},
        {"d1", NOT_FINAL},
        {"d2", NOT_FINAL},
        {"f0", NOT_FINAL},
        {"f1", NOT_FINAL},
        {"f2", NOT_FINAL},
        {"i0", NOT_FINAL},
        {"i1", NOT_FINAL},
        {"i2", NOT_FINAL},
        {"l0", NOT_FINAL},
        {"l1", NOT_FINAL},
        {"l2", NOT_FINAL},
        {"r0", NOT_FINAL},
        {"r1", NOT_FINAL},
        {"r2", NOT_FINAL},

        {"z0F", IS_FINAL},
        {"z1F", IS_FINAL},
        {"z2F", IS_FINAL},
        {"b0F", IS_FINAL},
        {"b1F", IS_FINAL},
        {"b2F", IS_FINAL},
        {"c0F", IS_FINAL},
        {"c1F", IS_FINAL},
        {"c2F", IS_FINAL},
        {"d0F", IS_FINAL},
        {"d1F", IS_FINAL},
        {"d2F", IS_FINAL},
        {"f0F", IS_FINAL},
        {"f1F", IS_FINAL},
        {"f2F", IS_FINAL},
        {"i0F", IS_FINAL},
        {"i1F", IS_FINAL},
        {"i2F", IS_FINAL},
        {"l0F", IS_FINAL},
        {"l1F", IS_FINAL},
        {"l2F", IS_FINAL},
        {"r0F", IS_FINAL},
        {"r1F", IS_FINAL},
        {"r2F", IS_FINAL},

        {"lS0", NOT_FINAL},
        {"lS1", NOT_FINAL},
        {"lS2", NOT_FINAL},
        {"lP0", NOT_FINAL},
        {"lP1", NOT_FINAL},
        {"lP2", NOT_FINAL},
        {"lU0", NOT_FINAL},
        {"lU1", NOT_FINAL},
        {"lU2", NOT_FINAL},
        {"lR0", NOT_FINAL},
        {"lR1", NOT_FINAL},
        {"lR2", NOT_FINAL},
        {"lT0", NOT_FINAL},
        {"lT1", NOT_FINAL},
        {"lT2", NOT_FINAL},
        {"lV0", NOT_FINAL},
        {"lV1", NOT_FINAL},
        {"lV2", NOT_FINAL},

        {"lS0F", IS_FINAL},
        {"lS1F", IS_FINAL},
        {"lS2F", IS_FINAL},
        {"lP0F", IS_FINAL},
        {"lP1F", IS_FINAL},
        {"lP2F", IS_FINAL},
        {"lU0F", IS_FINAL},
        {"lU1F", IS_FINAL},
        {"lU2F", IS_FINAL},
        {"lR0F", IS_FINAL},
        {"lR1F", IS_FINAL},
        {"lR2F", IS_FINAL},
        {"lT0F", IS_FINAL},
        {"lT1F", IS_FINAL},
        {"lT2F", IS_FINAL},

        {"X0", NOT_FINAL},
        {"X1", NOT_FINAL},
        {"X2", NOT_FINAL},
        {"O0", NOT_FINAL},
        {"O1", NOT_FINAL},
        {"O2", NOT_FINAL},

        {"X0F", IS_FINAL},
        {"X1F", IS_FINAL},
        {"X2F", IS_FINAL},
        {"O0F", IS_FINAL},
        {"O1F", IS_FINAL},
        {"O2F", IS_FINAL},

        {"LS0", NOT_FINAL},
        {"LS1", NOT_FINAL},
        {"LS2", NOT_FINAL},
        {"LP0", NOT_FINAL},
        {"LP1", NOT_FINAL},
        {"LP2", NOT_FINAL},
        {"LU0", NOT_FINAL},
        {"LU1", NOT_FINAL},
        {"LU2", NOT_FINAL},
        {"LR0", NOT_FINAL},
        {"LR1", NOT_FINAL},
        {"LR2", NOT_FINAL},
        {"LT0", NOT_FINAL},
        {"LT1", NOT_FINAL},
        {"LT2", NOT_FINAL},
        {"LV0", NOT_FINAL},
        {"LV1", NOT_FINAL},
        {"LV2", NOT_FINAL},

        {"LS0F", IS_FINAL},
        {"LS1F", IS_FINAL},
        {"LS2F", IS_FINAL},
        {"LP0F", IS_FINAL},
        {"LP1F", IS_FINAL},
        {"LP2F", IS_FINAL},
        {"LU0F", IS_FINAL},
        {"LU1F", IS_FINAL},
        {"LU2F", IS_FINAL},
        {"LR0F", IS_FINAL},
        {"LR1F", IS_FINAL},
        {"LR2F", IS_FINAL},
        {"LT0F", IS_FINAL},
        {"LT1F", IS_FINAL},
        {"LT2F", IS_FINAL},

        {"E0", NOT_FINAL},
        {"E1", NOT_FINAL},
        {"E2", NOT_FINAL},

        {"E0F", IS_FINAL},
        {"E1F", IS_FINAL},
        {"E2F", IS_FINAL},

        {"ES0", NOT_FINAL},
        {"ES1", NOT_FINAL},
        {"ES2", NOT_FINAL},
        {"EP0", NOT_FINAL},
        {"EP1", NOT_FINAL},
        {"EP2", NOT_FINAL},
        {"EU0", NOT_FINAL},
        {"EU1", NOT_FINAL},
        {"EU2", NOT_FINAL},
        {"ER0", NOT_FINAL},
        {"ER1", NOT_FINAL},
        {"ER2", NOT_FINAL},
        {"ET0", NOT_FINAL},
        {"ET1", NOT_FINAL},
        {"ET2", NOT_FINAL},
        {"EV0", NOT_FINAL},
        {"EV1", NOT_FINAL},
        {"EV2", NOT_FINAL},

        {"ES0F", IS_FINAL},
        {"ES1F", IS_FINAL},
        {"ES2F", IS_FINAL},
        {"EP0F", IS_FINAL},
        {"EP1F", IS_FINAL},
        {"EP2F", IS_FINAL},
        {"EU0F", IS_FINAL},
        {"EU1F", IS_FINAL},
        {"EU2F", IS_FINAL},
        {"ER0F", IS_FINAL},
        {"ER1F", IS_FINAL},
        {"ER2F", IS_FINAL},
        {"ET0F", IS_FINAL},
        {"ET1F", IS_FINAL},
        {"ET2F", IS_FINAL}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isFinal.";
    private final static String className = "isfinal001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "isfinal001aClassToCheck";

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

        log.display("debuger> Total fields in debugee read: "
                  + refType.allFields().size() + ", total fields in debuger: "
                  + TOTAL_FIELDS);
        // Check all fields from debugee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isFinal;
            String realIsFinal;

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
            isFinal = field.isFinal();
            realIsFinal = FIELD_NAME[i][1];
            log.display("debuger> " + i + " field (" + name + "), "
                      + "isFinal = " + isFinal + " read.");

            // isFinal() returns true if this type component is declared final,
            // returns false otherwise
            if ((isFinal && !realIsFinal.equals(IS_FINAL)) ||
                (!isFinal && realIsFinal.equals(IS_FINAL))
               ) {
                log.complain("debuger FAILURE 2> " + i + " field " + name
                          + ": read field.isFinal() = " + isFinal
                          + "; real isFinal should be " + realIsFinal);
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
