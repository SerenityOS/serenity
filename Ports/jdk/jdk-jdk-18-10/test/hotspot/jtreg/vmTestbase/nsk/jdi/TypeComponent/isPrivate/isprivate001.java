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


package nsk.jdi.TypeComponent.isPrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class isprivate001 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPrivate.";
    private final static String debuggerName = prefix + "isprivate001";
    private final static String debuggeeName = debuggerName + "a";

    private static ReferenceType debuggeeClass;

    final static String IS_PRIVATE = "true";
    final static String NOT_PRIVATE = "false";
    final static int TOTAL_FIELDS = 132;

    /** debuggee's fields for check **/
    private final static String checkedFields[][] = {
        {"z0", NOT_PRIVATE}, {"z1", NOT_PRIVATE}, {"z2", NOT_PRIVATE},
        {"b0", NOT_PRIVATE}, {"b1", NOT_PRIVATE}, {"b2", NOT_PRIVATE},
        {"c0", NOT_PRIVATE}, {"c1", NOT_PRIVATE}, {"c2", NOT_PRIVATE},
        {"d0", NOT_PRIVATE}, {"d1", NOT_PRIVATE}, {"d2", NOT_PRIVATE},
        {"f0", NOT_PRIVATE}, {"f1", NOT_PRIVATE}, {"f2", NOT_PRIVATE},
        {"i0", NOT_PRIVATE}, {"i1", NOT_PRIVATE}, {"i2", NOT_PRIVATE},
        {"l0", NOT_PRIVATE}, {"l1", NOT_PRIVATE}, {"l2", NOT_PRIVATE},
        {"r0", NOT_PRIVATE}, {"r1", NOT_PRIVATE}, {"r2", NOT_PRIVATE},

        {"zP0", IS_PRIVATE}, {"zP1", IS_PRIVATE}, {"zP2", IS_PRIVATE},
        {"bP0", IS_PRIVATE}, {"bP1", IS_PRIVATE}, {"bP2", IS_PRIVATE},
        {"cP0", IS_PRIVATE}, {"cP1", IS_PRIVATE}, {"cP2", IS_PRIVATE},
        {"dP0", IS_PRIVATE}, {"dP1", IS_PRIVATE}, {"dP2", IS_PRIVATE},
        {"fP0", IS_PRIVATE}, {"fP1", IS_PRIVATE}, {"fP2", IS_PRIVATE},
        {"iP0", IS_PRIVATE}, {"iP1", IS_PRIVATE}, {"iP2", IS_PRIVATE},
        {"lP0", IS_PRIVATE}, {"lP1", IS_PRIVATE}, {"lP2", IS_PRIVATE},
        {"rP0", IS_PRIVATE}, {"rP1", IS_PRIVATE}, {"rP2", IS_PRIVATE},

        {"Z0", NOT_PRIVATE}, {"Z1", NOT_PRIVATE}, {"Z2", NOT_PRIVATE},
        {"B0", NOT_PRIVATE}, {"B1", NOT_PRIVATE}, {"B2", NOT_PRIVATE},
        {"C0", NOT_PRIVATE}, {"C1", NOT_PRIVATE}, {"C2", NOT_PRIVATE},
        {"D0", NOT_PRIVATE}, {"D1", NOT_PRIVATE}, {"D2", NOT_PRIVATE},
        {"F0", NOT_PRIVATE}, {"F1", NOT_PRIVATE}, {"F2", NOT_PRIVATE},
        {"I0", NOT_PRIVATE}, {"I1", NOT_PRIVATE}, {"I2", NOT_PRIVATE},
        {"L0", NOT_PRIVATE}, {"L1", NOT_PRIVATE}, {"L2", NOT_PRIVATE},
        {"R0", NOT_PRIVATE}, {"R1", NOT_PRIVATE}, {"R2", NOT_PRIVATE},

        {"ZP0", IS_PRIVATE}, {"ZP1", IS_PRIVATE}, {"ZP2", IS_PRIVATE},
        {"BP0", IS_PRIVATE}, {"BP1", IS_PRIVATE}, {"BP2", IS_PRIVATE},
        {"CP0", IS_PRIVATE}, {"CP1", IS_PRIVATE}, {"CP2", IS_PRIVATE},
        {"DP0", IS_PRIVATE}, {"DP1", IS_PRIVATE}, {"DP2", IS_PRIVATE},
        {"FP0", IS_PRIVATE}, {"FP1", IS_PRIVATE}, {"FP2", IS_PRIVATE},
        {"IP0", IS_PRIVATE}, {"IP1", IS_PRIVATE}, {"IP2", IS_PRIVATE},
        {"LP0", IS_PRIVATE}, {"LP1", IS_PRIVATE}, {"LP2", IS_PRIVATE},
        {"RP0", IS_PRIVATE}, {"RP1", IS_PRIVATE}, {"RP2", IS_PRIVATE},

        {"s0", NOT_PRIVATE}, {"s1", NOT_PRIVATE}, {"s2", NOT_PRIVATE},
        {"o0", NOT_PRIVATE}, {"o1", NOT_PRIVATE}, {"o2", NOT_PRIVATE},
        {"S0", IS_PRIVATE},  {"S1", IS_PRIVATE},  {"S2", IS_PRIVATE},
        {"O0", IS_PRIVATE},  {"O1", IS_PRIVATE},  {"O2", IS_PRIVATE},

        {"u0", NOT_PRIVATE}, {"u1", NOT_PRIVATE}, {"u2", NOT_PRIVATE},
        {"v0", IS_PRIVATE},  {"v1", IS_PRIVATE},  {"v2", IS_PRIVATE},
        {"w0", NOT_PRIVATE}, {"w1", NOT_PRIVATE}, {"w2", NOT_PRIVATE},
        {"p0", NOT_PRIVATE}, {"p1", NOT_PRIVATE}, {"p2", NOT_PRIVATE},

        {"h0", NOT_PRIVATE}, {"h1", NOT_PRIVATE}, {"h2", NOT_PRIVATE},
        {"j0", IS_PRIVATE},  {"j1", IS_PRIVATE},  {"j2", IS_PRIVATE},
        {"k0", NOT_PRIVATE}, {"k1", NOT_PRIVATE}, {"k2", NOT_PRIVATE},
        {"m0", NOT_PRIVATE}, {"m1", NOT_PRIVATE}, {"m2", NOT_PRIVATE}
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


        display("Checking isPrivate() method for debuggee's fields...");

        display("Total count of fields read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + TOTAL_FIELDS);

        // Check all fields from debuggee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isPrivate;
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
            isPrivate = ((TypeComponent)field).isPrivate();  // cast to TypeComponent interface
            expectedValue = checkedFields[i][1];
            if ((isPrivate && !expectedValue.equals(IS_PRIVATE)) ||
                (!isPrivate && expectedValue.equals(IS_PRIVATE)) ) {
                complain("isPrivate() returned wrong value: " + isPrivate
                    + " for field " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPrivate() returned expected " + isPrivate
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
