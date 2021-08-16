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


package nsk.jdi.TypeComponent.signature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class sign001 {
    final static int TOTAL_FIELDS = 96;
    final static String FIELD_NAME[][] = {
        {"z0", "Z"},
        {"z1", "[Z"},
        {"z2", "[[Z"},
        {"b0", "B"},
        {"b1", "[B"},
        {"b2", "[[B"},
        {"c0", "C"},
        {"c1", "[C"},
        {"c2", "[[C"},
        {"d0", "D"},
        {"d1", "[D"},
        {"d2", "[[D"},
        {"f0", "F"},
        {"f1", "[F"},
        {"f2", "[[F"},
        {"i0", "I"},
        {"i1", "[I"},
        {"i2", "[[I"},
        {"l0", "J"},
        {"l1", "[J"},
        {"l2", "[[J"},
        {"r0", "S"},
        {"r1", "[S"},
        {"r2", "[[S"},
        {"lF0", "J"},
        {"lF1", "[J"},
        {"lF2", "[[J"},
        {"lP0", "J"},
        {"lP1", "[J"},
        {"lP2", "[[J"},
        {"lU0", "J"},
        {"lU1", "[J"},
        {"lU2", "[[J"},
        {"lR0", "J"},
        {"lR1", "[J"},
        {"lR2", "[[J"},
        {"l0S", "J"},
        {"l1S", "[J"},
        {"l2S", "[[J"},
        {"lT0", "J"},
        {"lT1", "[J"},
        {"lT2", "[[J"},
        {"lV0", "J"},
        {"lV1", "[J"},
        {"lV2", "[[J"},
        {"X0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Class;"},
        {"X1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Class;"},
        {"X2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Class;"},
        {"O0", "Ljava/lang/Object;"},
        {"O1", "[Ljava/lang/Object;"},
        {"O2", "[[Ljava/lang/Object;"},
        {"LF0", "Ljava/lang/Long;"},
        {"LF1", "[Ljava/lang/Long;"},
        {"LF2", "[[Ljava/lang/Long;"},
        {"LP0", "Ljava/lang/Long;"},
        {"LP1", "[Ljava/lang/Long;"},
        {"LP2", "[[Ljava/lang/Long;"},
        {"LU0", "Ljava/lang/Long;"},
        {"LU1", "[Ljava/lang/Long;"},
        {"LU2", "[[Ljava/lang/Long;"},
        {"LR0", "Ljava/lang/Long;"},
        {"LR1", "[Ljava/lang/Long;"},
        {"LR2", "[[Ljava/lang/Long;"},
        {"L0S", "Ljava/lang/Long;"},
        {"L1S", "[Ljava/lang/Long;"},
        {"L2S", "[[Ljava/lang/Long;"},
        {"LT0", "Ljava/lang/Long;"},
        {"LT1", "[Ljava/lang/Long;"},
        {"LT2", "[[Ljava/lang/Long;"},
        {"LV0", "Ljava/lang/Long;"},
        {"LV1", "[Ljava/lang/Long;"},
        {"LV2", "[[Ljava/lang/Long;"},
        {"E0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"E1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"E2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EF0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EF1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EF2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EP0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EP1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EP2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EU0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EU1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EU2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ER0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ER1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ER2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"E0S", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"E1S", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"E2S", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ET0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ET1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"ET2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EV0", "Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EV1", "[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"},
        {"EV2", "[[Lnsk/jdi/TypeComponent/signature/ClassToCheck$Inter;"}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.signature.";
    private final static String className = "sign001";
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

        log.display("debuger> Total fields in debugee read: "
                  + refType.allFields().size() + " total fields in debuger: "
                  + TOTAL_FIELDS);
        // Check all fields from debugee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String signature;
            String realSign;

            try {
                field = refType.fieldByName(FIELD_NAME[i][0]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + FIELD_NAME[i][0]);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            signature = field.signature();
            realSign = FIELD_NAME[i][1];
            log.display("debuger> " + i + " field (" + FIELD_NAME[i][0]
                      + ") with signature " + signature + " read.");
            if (!realSign.equals(signature)) {
                log.complain("debuger FAILURE 2> Returned signature for field ("
                           + FIELD_NAME[i][0] + ") is " + signature
                           + ", expected " + realSign);
                testFailed = true;
                continue;
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
