/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.TypeComponent.isPublic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class ispublic001 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPublic.";
    private final static String debuggerName = prefix + "ispublic001";
    private final static String debuggeeName = debuggerName + "a";

    private static ReferenceType debuggeeClass;

    final static String IS_PUBLIC = "true";
    final static String NOT_PUBLIC = "false";
    final static int TOTAL_FIELDS = 132;

    /** debuggee's fields for check **/
    private final static String checkedFields[][] = {
        {"z0", NOT_PUBLIC}, {"z1", NOT_PUBLIC}, {"z2", NOT_PUBLIC},
        {"b0", NOT_PUBLIC}, {"b1", NOT_PUBLIC}, {"b2", NOT_PUBLIC},
        {"c0", NOT_PUBLIC}, {"c1", NOT_PUBLIC}, {"c2", NOT_PUBLIC},
        {"d0", NOT_PUBLIC}, {"d1", NOT_PUBLIC}, {"d2", NOT_PUBLIC},
        {"f0", NOT_PUBLIC}, {"f1", NOT_PUBLIC}, {"f2", NOT_PUBLIC},
        {"i0", NOT_PUBLIC}, {"i1", NOT_PUBLIC}, {"i2", NOT_PUBLIC},
        {"l0", NOT_PUBLIC}, {"l1", NOT_PUBLIC}, {"l2", NOT_PUBLIC},
        {"r0", NOT_PUBLIC}, {"r1", NOT_PUBLIC}, {"r2", NOT_PUBLIC},

        {"zP0", IS_PUBLIC}, {"zP1", IS_PUBLIC}, {"zP2", IS_PUBLIC},
        {"bP0", IS_PUBLIC}, {"bP1", IS_PUBLIC}, {"bP2", IS_PUBLIC},
        {"cP0", IS_PUBLIC}, {"cP1", IS_PUBLIC}, {"cP2", IS_PUBLIC},
        {"dP0", IS_PUBLIC}, {"dP1", IS_PUBLIC}, {"dP2", IS_PUBLIC},
        {"fP0", IS_PUBLIC}, {"fP1", IS_PUBLIC}, {"fP2", IS_PUBLIC},
        {"iP0", IS_PUBLIC}, {"iP1", IS_PUBLIC}, {"iP2", IS_PUBLIC},
        {"lP0", IS_PUBLIC}, {"lP1", IS_PUBLIC}, {"lP2", IS_PUBLIC},
        {"rP0", IS_PUBLIC}, {"rP1", IS_PUBLIC}, {"rP2", IS_PUBLIC},

        {"Z0", NOT_PUBLIC}, {"Z1", NOT_PUBLIC}, {"Z2", NOT_PUBLIC},
        {"B0", NOT_PUBLIC}, {"B1", NOT_PUBLIC}, {"B2", NOT_PUBLIC},
        {"C0", NOT_PUBLIC}, {"C1", NOT_PUBLIC}, {"C2", NOT_PUBLIC},
        {"D0", NOT_PUBLIC}, {"D1", NOT_PUBLIC}, {"D2", NOT_PUBLIC},
        {"F0", NOT_PUBLIC}, {"F1", NOT_PUBLIC}, {"F2", NOT_PUBLIC},
        {"I0", NOT_PUBLIC}, {"I1", NOT_PUBLIC}, {"I2", NOT_PUBLIC},
        {"L0", NOT_PUBLIC}, {"L1", NOT_PUBLIC}, {"L2", NOT_PUBLIC},
        {"R0", NOT_PUBLIC}, {"R1", NOT_PUBLIC}, {"R2", NOT_PUBLIC},

        {"ZP0", IS_PUBLIC}, {"ZP1", IS_PUBLIC}, {"ZP2", IS_PUBLIC},
        {"BP0", IS_PUBLIC}, {"BP1", IS_PUBLIC}, {"BP2", IS_PUBLIC},
        {"CP0", IS_PUBLIC}, {"CP1", IS_PUBLIC}, {"CP2", IS_PUBLIC},
        {"DP0", IS_PUBLIC}, {"DP1", IS_PUBLIC}, {"DP2", IS_PUBLIC},
        {"FP0", IS_PUBLIC}, {"FP1", IS_PUBLIC}, {"FP2", IS_PUBLIC},
        {"IP0", IS_PUBLIC}, {"IP1", IS_PUBLIC}, {"IP2", IS_PUBLIC},
        {"LP0", IS_PUBLIC}, {"LP1", IS_PUBLIC}, {"LP2", IS_PUBLIC},
        {"RP0", IS_PUBLIC}, {"RP1", IS_PUBLIC}, {"RP2", IS_PUBLIC},

        {"s0", NOT_PUBLIC}, {"s1", NOT_PUBLIC}, {"s2", NOT_PUBLIC},
        {"o0", NOT_PUBLIC}, {"o1", NOT_PUBLIC}, {"o2", NOT_PUBLIC},
        {"S0", IS_PUBLIC},  {"S1", IS_PUBLIC},  {"S2", IS_PUBLIC},
        {"O0", IS_PUBLIC},  {"O1", IS_PUBLIC},  {"O2", IS_PUBLIC},

        {"u0", NOT_PUBLIC}, {"u1", NOT_PUBLIC}, {"u2", NOT_PUBLIC},
        {"v0", NOT_PUBLIC}, {"v1", NOT_PUBLIC}, {"v2", NOT_PUBLIC},
        {"w0", IS_PUBLIC},  {"w1", IS_PUBLIC},  {"w2", IS_PUBLIC},
        {"p0", NOT_PUBLIC}, {"p1", NOT_PUBLIC}, {"p2", NOT_PUBLIC},

        {"h0", NOT_PUBLIC}, {"h1", NOT_PUBLIC}, {"h2", NOT_PUBLIC},
        {"j0", NOT_PUBLIC}, {"j1", NOT_PUBLIC}, {"j2", NOT_PUBLIC},
        {"k0", IS_PUBLIC},  {"k1", IS_PUBLIC},  {"k2", IS_PUBLIC},
        {"m0", NOT_PUBLIC}, {"m1", NOT_PUBLIC}, {"m2", NOT_PUBLIC}
    };

    /**
     * Re-call to <code>run(args,out)</code>, and exit with
     * either status 95 or 97 (JCK-like exit status).
     */
    public static void main (String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    /**
     * JCK-like entry point to the test: perform testing, and
     * return exit code 0 (Consts.TEST_PASSED) or either 2 (Consts.TEST_FAILED).
     */
    public static int run (String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        Binder binder = new Binder(argHandler, log);
        Debugee debuggee = binder.bindToDebugee(debuggeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "debugger > ");

        debuggee.resume();
        display("Waiting debuggee's \"ready\" signal...");
        String line = pipe.readln();

        if (line == null) {
            complain("UNEXPECTED debuggee's signal - null");
            return Consts.TEST_FAILED;
        }
        if (!line.equals("ready")) {
            complain("UNEXPECTED debuggee's signal - " + line);
            return Consts.TEST_FAILED;
        } else {
            display("debuggee's \"ready\" signal recieved.");
        }

        int exitStatus = Consts.TEST_PASSED;

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            return Consts.TEST_FAILED;
        }


        display("Checking isPublic() method for debuggee's fields...");

        display("Total count of fields read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + TOTAL_FIELDS);

        // Check all fields from debuggee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isPublic;
            String expectedValue;

            try {
                field = debuggeeClass.fieldByName(checkedFields[i][0]);
            } catch (Exception e) {
                complain("Can't get field by name "  + checkedFields[i][0]);
                complain("Unexpected Exception: " + e);
                exitStatus = Consts.TEST_FAILED;
                continue;
            }

            name = field.name();
            isPublic = ((TypeComponent)field).isPublic();  // cast to TypeComponent interface
            expectedValue = checkedFields[i][1];
            if ((isPublic && !expectedValue.equals(IS_PUBLIC)) ||
                (!isPublic && expectedValue.equals(IS_PUBLIC)) ) {
                complain("isPublic() returned wrong value: " + isPublic
                    + " for field " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPublic() returned expected " + isPublic
                    + " for field " + name);
            }
        }

        display("Checking debuggee's fields completed!");
        display("Waiting for debuggee's finish...");
        pipe.println("quit");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != Consts.TEST_PASSED + Consts.JCK_STATUS_BASE) {
            complain("UNEXPECTED debuggee's exit status (not 95) - " + status);
            exitStatus = Consts.TEST_FAILED;
        }  else {
            display("Got expected debuggee's exit status - " + status);
        }

        return exitStatus;
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }
}
