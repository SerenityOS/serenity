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


package nsk.jdi.TypeComponent.isSynthetic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class issynthetic001 {
    final static String FIELD_NAME[] = {
        "z0", "z1", "z2",
        "b0", "b1", "b2",
        "c0", "c1", "c2",
        "d0", "d1", "d2",
        "f0", "f1", "f2",
        "i0", "i1", "i2",
        "l0", "l1", "l2",
        "r0", "r1", "r2",

        "lF0", "lF1", "lF2",
        "lP0", "lP1", "lP2",
        "lU0", "lU1", "lU2",
        "lR0", "lR1", "lR2",
        "lT0", "lT1", "lT2",
        "lV0", "lV1", "lV2",

        "X0", "X1", "X2",
        "O0", "O1", "O2",

        "LF0", "LF1", "LF2",
        "LP0", "LP1", "LP2",
        "LU0", "LU1", "LU2",
        "LR0", "LR1", "LR2",
        "LT0", "LT1", "LT2",
        "LV0", "LV1", "LV2",

        "E0", "E1", "E2",
        "EF0", "EF1", "EF2",
        "EP0", "EP1", "EP2",
        "EU0", "EU1", "EU2",
        "ER0", "ER1", "ER2",
        "ET0", "ET1", "ET2",
        "EV0", "EV1", "EV2"
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isSynthetic.";
    private final static String className = "issynthetic001";
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
        VirtualMachine vm = debugee.VM();
        boolean canGetSynthetic = vm.canGetSyntheticAttribute();
        IOPipe pipe = new IOPipe(debugee);
        boolean testFailed = false;
        List fields;
        List nestedTypes;
        ReferenceType nestedClass = null;
        int totalSyntheticFields = 0;

        log.display("debuger> Value of canGetSyntheticAttribute in current "
                  + "VM is " + canGetSynthetic);

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

        // Get nested class
        try {
            nestedTypes = refType.nestedTypes();
        } catch (ObjectCollectedException e) {
            log.complain("debuger FAILURE> Cannot get nested types from "
                       + classToCheckName);
            return 2;
        }
        int totalNestedTypes = nestedTypes.size();
        if (totalNestedTypes == 0) {
            log.complain("debuger FAILURE> No nested classes found.");
            return 2;
        }
        nestedClass = (ReferenceType)nestedTypes.get(0);
        if (nestedClass == null) {
           log.complain("debuger FAILURE> Cannot find nested class.");
           return 2;
        }

        // Get all fields and check them
        try {
            fields = nestedClass.fields();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get fields from nested class");
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalFields = fields.size();
        if (totalFields < 1) {
            log.complain("debuger FAILURE> Total number of fields read "
                       + totalFields);
            return 2;
        }
        log.display("debuger> Total fields in debugee read: "
                  + totalFields + " total fields in debuger: "
                  + FIELD_NAME.length);
        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field)fields.get(i);
            String name = field.name();
            boolean isSynthetic;
            boolean isRealSynthetic = true;

            try {
                isSynthetic = field.isSynthetic();

                if (!canGetSynthetic) {
                    log.complain("debuger FAILURE 1> Value of "
                               + "canGetSyntheticAttribute in current VM is "
                               + "false, so UnsupportedOperationException was "
                               + "expected for " + i + " field " + name);
                    testFailed = true;
                    continue;
                } else {
                    log.display("debuger> " + i + " field " + name + " with "
                              + "synthetic value " + isSynthetic + " read "
                              + "without UnsupportedOperationException.");
                }
            } catch (UnsupportedOperationException e) {
                if (canGetSynthetic) {
                    log.complain("debuger FAILURE 2> Value of "
                               + "canGetSyntheticAttribute in current VM is "
                               + "true, but cannot get synthetic for field "
                               + "name.");
                    log.complain("debuger FAILURE 2> Exception: " + e);
                    testFailed = true;
                } else {
                    log.display("debuger> UnsupportedOperationException was "
                              + "thrown while getting isSynthetic for " + i
                              + " field " + name + " because value "
                              + "canGetSynthetic is false.");
                }
                continue;
            }

            // Find out if field exists in list of fields
            for (int j = 0; j < FIELD_NAME.length; j++) {
                String nameFromList = FIELD_NAME[j];

                if (nameFromList.equals(name)) {
                    // Field found in list - is not synthetic

                    isRealSynthetic = false;
                    break;
                }
            }

            if (isRealSynthetic != isSynthetic) {
                log.complain("debuger FAILURE 3> Field's " + name
                           + " synthetic is " + isSynthetic + ", but expected "
                           + "is " + isRealSynthetic);
                testFailed = true;
                continue;
            }

            if (isSynthetic) {
                totalSyntheticFields++;
            }
        }

        if (totalSyntheticFields == 0) {
            log.complain("debuger FAILURE 4> Synthetic fields not found.");
            testFailed = true;
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
