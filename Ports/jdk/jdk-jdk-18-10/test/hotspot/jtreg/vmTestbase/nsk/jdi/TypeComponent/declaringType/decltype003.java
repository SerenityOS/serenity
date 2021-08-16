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


package nsk.jdi.TypeComponent.declaringType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class decltype003 {
    final static String FIELD_NAME_CLASS[] = {
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
        "l0S", "l1S", "l2S",
        "lT0", "lT1", "lT2",
        "lV0", "lV1", "lV2",

        "X0", "X1", "X2",
        "O0", "O1", "O2",

        "LF0", "LF1", "LF2",
        "LP0", "LP1", "LP2",
        "LU0", "LU1", "LU2",
        "LR0", "LR1", "LR2",
        "L0S", "L1S", "L2S",
        "LT0", "LT1", "LT2",
        "LV0", "LV1", "LV2",

        "E0", "E1", "E2",
        "EF0", "EF1", "EF2",
        "EP0", "EP1", "EP2",
        "EU0", "EU1", "EU2",
        "ER0", "ER1", "ER2",
        "E0S", "E1S", "E2S",
        "ET0", "ET1", "ET2",
        "EV0", "EV1", "EV2"
    };
    final static String FIELD_NAME_INTER[] = {
        "z0", "z1", "z2",
        "b0", "b1", "b2",
        "c0", "c1", "c2",
        "d0", "d1", "d2",
        "f0", "f1", "f2",
        "i0", "i1", "i2",
        "l0", "l1", "l2",
        "r0", "r1", "r2",

        "lF0", "lF1", "lF2",
        "lU0", "lU1", "lU2",
        "l0S", "l1S", "l2S",

        "X0", "X1", "X2",
        "O0", "O1", "O2",

        "LF0", "LF1", "LF2",
        "LU0", "LU1", "LU2",
        "L0S", "L1S", "L2S",

        "E0", "E1", "E2",
        "EF0", "EF1", "EF2",
        "EU0", "EU1", "EU2",
        "E0S", "E1S", "E2S",
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.declaringType.";
    private final static String className = "decltype003";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String overClassName = prefix + "decltype003aOverridenClass";
    private final static String overInterName = prefix + "decltype003aOverridenInter";

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
        ReferenceType overClass;
        ReferenceType overInter;

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

        // Get OverridenClass and  OverridenInter from debugee
        overClass = debugee.classByName(overClassName);
        if (overClass == null) {
           log.complain("debuger FAILURE> Class " + overClassName + " not "
                      + "found.");
           return 2;
        }
        overInter = debugee.classByName(overInterName);
        if (overInter == null) {
           log.complain("debuger FAILURE> Interface " + overInterName + " not "
                      + "found.");
           return 2;
        }

        // Check all fields from OverridenClass
        log.display("debuger> Total fields in " + overClassName + ": " +
                  + overClass.fields().size());
        for (int i = 0; i < FIELD_NAME_CLASS.length; i++) {
            Field field;
            String name;
            ReferenceType declType;
            boolean equal;

            try {
                field = overClass.fieldByName(FIELD_NAME_CLASS[i]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + FIELD_NAME_CLASS[i] + " from type "
                           + overClassName);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            name = field.name();
            declType = field.declaringType();
            if (declType == null) {
                log.complain("debuger FAILURE 2> Declaring type is null "
                               + " for field " + name + " in class "
                               + overClassName);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field " + name + " with declaring "
                      + "type " + declType.name() + " from class "
                      + overClassName + " read.");
            try {
                equal = declType.equals(overClass);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 3> Cannot compare reference "
                           + " types " + declType.name() + " and "
                           + overClassName);
                log.complain("debuger FAILURE 3> Exception: " + e);
                testFailed = true;
                continue;
            }
            if (!equal) {
                log.complain("debuger FAILURE 4> Declaring type of field "
                           + name + " is " + declType.name() + ", but should "
                           + "be " + overClassName);
                testFailed = true;
            }
        }

        // Check all fields from OverridenInter
        log.display("debuger> Total fields in " + overInterName + ": " +
                  + overInter.fields().size());
        for (int i = 0; i < FIELD_NAME_INTER.length; i++) {
            Field field;
            String name;
            ReferenceType declType;
            boolean equal;

            try {
                field = overInter.fieldByName(FIELD_NAME_INTER[i]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 5> Can't get field by name "
                           + FIELD_NAME_INTER[i] + " from type "
                           + overInterName);
                log.complain("debuger FAILURE 5> Exception: " + e);
                testFailed = true;
                continue;
            }
            name = field.name();
            declType = field.declaringType();
            if (declType == null) {
                log.complain("debuger FAILURE 6> Declaring type is null "
                               + " for field " + name + " in class "
                               + overInterName);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field " + name + " with declaring "
                      + "type " + declType.name() + " from class "
                      + overInterName + " read.");
            try {
                equal = declType.equals(overInter);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 7> Cannot compare reference "
                           + " types " + declType.name() + " and "
                           + overInterName);
                log.complain("debuger FAILURE 7> Exception: " + e);
                testFailed = true;
                continue;
            }
            if (!equal) {
                log.complain("debuger FAILURE 8> Declaring type of field "
                           + name + " is " + declType.name() + ", but should "
                           + "be " + overInterName);
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
